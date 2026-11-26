package com.tarkrakov.tearsofink.weapon.ranged;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.skill.Skill;
import com.tarkrakov.tearsofink.skill.SkillInput;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.weapon.forge.Gear;
import com.tarkrakov.tearsofink.weapon.ranged.RangedItem.Kind;
import com.tarkrakov.tearsofink.pantheon.PantheonCombat;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import java.util.*;

public final class RangedCombat {
    // Game balance: one cartridge pays for a short-range spread, not six full hits.
    private static final int PELLETS=6, PELLET_LIFETIME=6;
    private static final float PISTOL_DAMAGE=8f, PISTOL_WILD_DAMAGE=10f; // HP per barrel, before target protection
    private static final String LOADED="toi_ranged_loaded", EMPOWERED="toi_ranged_empowered";
    private static final net.minecraft.resources.ResourceLocation SLOW=TearsOfInk.id("ranged_reload_slow");
    private static final Map<UUID,State> STATES=new HashMap<>();
    private static final class Job { ItemStack stack=ItemStack.EMPTY; int ticks, duration; }
    private static final class State { final Job main=new Job(), off=new Job(); long nextMain, nextOff; double slow; boolean aim, hadPistol; ItemStack aimStack=ItemStack.EMPTY; }
    private RangedCombat(){}
    public record Fire(boolean off,boolean aimed) implements CustomPacketPayload {
        public static final Type<Fire> TYPE=new Type<>(TearsOfInk.id("ranged_fire"));
        public static final StreamCodec<ByteBuf,Fire> CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,Fire::off,ByteBufCodecs.BOOL,Fire::aimed,Fire::new);
        @Override public Type<Fire> type(){return TYPE;}
    }
    public record Aim(boolean active) implements CustomPacketPayload {
        public static final Type<Aim> TYPE=new Type<>(TearsOfInk.id("ranged_aim"));
        public static final StreamCodec<ByteBuf,Aim> CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,Aim::active,Aim::new);
        @Override public Type<Aim> type(){return TYPE;}
    }
    public record AimPose(int entity,boolean active) implements CustomPacketPayload {
        public static final Type<AimPose> TYPE=new Type<>(TearsOfInk.id("ranged_aim_pose"));
        public static final StreamCodec<ByteBuf,AimPose> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,AimPose::entity,ByteBufCodecs.BOOL,AimPose::active,AimPose::new);
        @Override public Type<AimPose> type(){return TYPE;}
    }
    /** Accepted shot/ability, for visual playback only. */
    public record PistolShot(int entity,boolean off,boolean pair,boolean aimed,boolean wild) implements CustomPacketPayload {
        public static final Type<PistolShot> TYPE=new Type<>(TearsOfInk.id("pistol_shot_pose"));
        public static final StreamCodec<ByteBuf,PistolShot> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,PistolShot::entity,ByteBufCodecs.BOOL,PistolShot::off,ByteBufCodecs.BOOL,PistolShot::pair,ByteBufCodecs.BOOL,PistolShot::aimed,ByteBufCodecs.BOOL,PistolShot::wild,PistolShot::new);
        @Override public Type<PistolShot> type(){return TYPE;}
    }
    public record PistolReload(int entity,int main,int mainMax,int off,int offMax) implements CustomPacketPayload {
        public static final Type<PistolReload> TYPE=new Type<>(TearsOfInk.id("pistol_reload_pose"));
        public static final StreamCodec<ByteBuf,PistolReload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,PistolReload::entity,ByteBufCodecs.VAR_INT,PistolReload::main,ByteBufCodecs.VAR_INT,PistolReload::mainMax,ByteBufCodecs.VAR_INT,PistolReload::off,ByteBufCodecs.VAR_INT,PistolReload::offMax,PistolReload::new);
        @Override public Type<PistolReload> type(){return TYPE;}
    }
    private static void pistolShot(ServerPlayer p,boolean off,boolean pair,boolean aimed,boolean wild){
        var packet=new PistolShot(p.getId(),off,pair,aimed,wild);
        for(ServerPlayer viewer:p.serverLevel().players())
            if(viewer.distanceToSqr(p)<128*128&&ServerPlayNetworking.canSend(viewer,PistolShot.TYPE))ServerPlayNetworking.send(viewer,packet);
    }
    public record ReloadView(int main,int mainMax,int off,int offMax) implements CustomPacketPayload {
        public static final Type<ReloadView> TYPE=new Type<>(TearsOfInk.id("ranged_reload_view"));
        public static final StreamCodec<ByteBuf,ReloadView> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,ReloadView::main,ByteBufCodecs.VAR_INT,ReloadView::mainMax,ByteBufCodecs.VAR_INT,ReloadView::off,ByteBufCodecs.VAR_INT,ReloadView::offMax,ReloadView::new);
        @Override public Type<ReloadView> type(){return TYPE;}
    }
    public static boolean canAim(Player p){
        Kind k=RangedItem.kind(p.getMainHandItem());
        return k==Kind.CROSSBOW||k==Kind.PISTOL;
    }
    private static void broadcastAim(ServerPlayer p,boolean active){
        AimPose packet=new AimPose(p.getId(),active);
        for(ServerPlayer viewer:p.serverLevel().players())
            if(viewer.distanceToSqr(p)<128*128&&ServerPlayNetworking.canSend(viewer,AimPose.TYPE))ServerPlayNetworking.send(viewer,packet);
    }
    private static void aim(ServerPlayer p,boolean active){
        State s=STATES.get(p.getUUID());
        if(!active||!valid(p)||!canAim(p)){
            if(s!=null&&s.aim){s.aim=false;s.aimStack=ItemStack.EMPTY;broadcastAim(p,false);}
            return;
        }
        if(s==null){s=new State();STATES.put(p.getUUID(),s);}
        s.aim=true;s.aimStack=p.getMainHandItem();broadcastAim(p,true);
    }
    private static void sendReload(ServerPlayer p,State s){
        if(ServerPlayNetworking.canSend(p,ReloadView.TYPE))
            ServerPlayNetworking.send(p,new ReloadView(s.main.ticks,s.main.duration,s.off.ticks,s.off.duration));
        boolean main=RangedItem.kind(p.getMainHandItem())==Kind.PISTOL,off=RangedItem.kind(p.getOffhandItem())==Kind.PISTOL&&!blocksOffhand(p);
        if(main||off||s.hadPistol){
            var packet=new PistolReload(p.getId(),main?s.main.ticks:0,main?s.main.duration:0,off?s.off.ticks:0,off?s.off.duration:0);
            for(ServerPlayer viewer:p.serverLevel().players())if(viewer.distanceToSqr(p)<128*128&&ServerPlayNetworking.canSend(viewer,PistolReload.TYPE))ServerPlayNetworking.send(viewer,packet);
        }
        s.hadPistol=main||off;
    }
    public static boolean loaded(ItemStack s){return flag(s,LOADED);}
    public static boolean empowered(ItemStack s){return flag(s,EMPOWERED);}
    private static boolean flag(ItemStack s,String key){CustomData d=s.get(DataComponents.CUSTOM_DATA);return d!=null && d.copyTag().getBoolean(key);}
    private static void flag(ItemStack s,String key,boolean value){CustomData.update(DataComponents.CUSTOM_DATA,s,t->t.putBoolean(key,value));}
    public static boolean blocksOffhand(Player p){return RangedItem.kind(p.getMainHandItem())==Kind.CROSSBOW;}
    private static boolean paired(Player p,Kind kind){return kind!=Kind.CROSSBOW && RangedItem.kind(p.getMainHandItem())==kind && RangedItem.kind(p.getOffhandItem())==kind;}
    private static boolean valid(ServerPlayer p){return p.isAlive()&&!p.isSpectator()&&!p.isSleeping()&&!p.isPassenger()
        &&p.containerMenu==p.inventoryMenu&&ModDimensions.hasLandsRules(p.level());}
    public static void init(){
        PayloadTypeRegistry.playC2S().register(Fire.TYPE,Fire.CODEC);
        PayloadTypeRegistry.playC2S().register(Aim.TYPE,Aim.CODEC);
        PayloadTypeRegistry.playS2C().register(AimPose.TYPE,AimPose.CODEC);
        PayloadTypeRegistry.playS2C().register(ReloadView.TYPE,ReloadView.CODEC);
        PayloadTypeRegistry.playS2C().register(PistolShot.TYPE,PistolShot.CODEC);
        PayloadTypeRegistry.playS2C().register(PistolReload.TYPE,PistolReload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Aim.TYPE,(packet,ctx)->ctx.player().server.execute(()->aim(ctx.player(),packet.active())));
        ServerPlayNetworking.registerGlobalReceiver(Fire.TYPE,(packet,ctx)->ctx.player().server.execute(()->fire(ctx.player(),packet.off(),packet.aimed(),false)));
        ServerTickEvents.END_SERVER_TICK.register(server->{for(ServerPlayer p:server.getPlayerList().getPlayers()) tick(p);});
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->STATES.remove(h.getPlayer().getUUID()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->STATES.clear());
        AttackEntityCallback.EVENT.register((p,level,hand,target,hit)->RangedItem.kind(p.getMainHandItem())!=null?InteractionResult.FAIL:InteractionResult.PASS);
        UseItemCallback.EVENT.register((p,level,hand)->blocked(p,hand)
            ?InteractionResultHolder.fail(p.getItemInHand(hand)):InteractionResultHolder.pass(p.getItemInHand(hand)));
        UseBlockCallback.EVENT.register((p,level,hand,hit)->hand==InteractionHand.OFF_HAND&&blocksOffhand(p)?InteractionResult.FAIL:InteractionResult.PASS);
        UseEntityCallback.EVENT.register((p,level,hand,target,hit)->hand==InteractionHand.OFF_HAND&&blocksOffhand(p)?InteractionResult.FAIL:InteractionResult.PASS);
    }
    private static boolean blocked(Player p,InteractionHand hand){return hand==InteractionHand.OFF_HAND&&blocksOffhand(p)
        || RangedItem.kind(p.getItemInHand(hand))!=null;}
    public static void resetReload(ServerPlayer p){
        State s=STATES.get(p.getUUID());
        if(s!=null){
            aim(p,false);s.main.stack=ItemStack.EMPTY;s.main.ticks=s.main.duration=0;
            s.off.stack=ItemStack.EMPTY;s.off.ticks=s.off.duration=0;slow(p,s,0);sendReload(p,s);
        }
    }
    private static void slow(ServerPlayer p,State s,double value){
        var a=p.getAttribute(Attributes.MOVEMENT_SPEED);
        if(a==null)return;
        if(s.slow==value && (value==0 || a.getModifier(SLOW)!=null))return;
        a.removeModifier(SLOW);
        if(value>0)a.addTransientModifier(new AttributeModifier(SLOW,-value,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        s.slow=value;
    }
    private static void tick(ServerPlayer p){
        State s=STATES.get(p.getUUID());
        boolean has=RangedItem.kind(p.getMainHandItem())!=null||RangedItem.kind(p.getOffhandItem())!=null;
        if(!has&&s==null)return;
        if(s==null){s=new State();STATES.put(p.getUUID(),s);}
        if(!valid(p)){resetReload(p);return;}
        if(s.aim&&(!canAim(p)||s.aimStack!=p.getMainHandItem()))aim(p,false);
        boolean blocked=blocksOffhand(p);
        if(blocked&&p.isUsingItem()&&p.getUsedItemHand()==InteractionHand.OFF_HAND)p.stopUsingItem();
        boolean pair=paired(p,Kind.PISTOL);
        boolean m=reload(p,s.main,p.getMainHandItem(),false,pair);
        boolean o=reload(p,s.off,p.getOffhandItem(),true,pair);
        slow(p,s,m&&blocked?.50:m||o?.18:0);
        if(p.tickCount%4==0||!has)sendReload(p,s);
        if(!has)STATES.remove(p.getUUID());
    }
    private static boolean reload(ServerPlayer p,Job job,ItemStack stack,boolean off,boolean pair){
        Kind kind=RangedItem.kind(stack);
        if(job.stack!=stack){job.stack=stack;job.ticks=0;}
        job.duration=0;
        if(kind==null||kind==Kind.DAGGER||loaded(stack)||(off&&(blocksOffhand(p)||kind==Kind.CROSSBOW))){
            job.ticks=0;return false;
        }
        if(!p.getAbilities().instabuild&&RangedSupply.available(p,kind)<(pair?2:1)){job.ticks=0;return false;}
        int duration=kind==Kind.CROSSBOW?46:pair?60:38;
        job.duration=duration;
        if(++job.ticks>=duration){
            flag(stack,LOADED,true);job.ticks=job.duration=0;
            p.level().playSound(null,p.blockPosition(),SoundEvents.CROSSBOW_LOADING_END.value(),SoundSource.PLAYERS,.5f,1f);
            return false;
        }
        return true;
    }
    private static boolean fire(ServerPlayer p,boolean off,boolean aimed,boolean special){
        if(!valid(p)||off&&blocksOffhand(p))return false;
        ItemStack first=off?p.getOffhandItem():p.getMainHandItem();
        Kind kind=RangedItem.kind(first);
        if(kind==null||off&&kind==Kind.CROSSBOW)return false;
        boolean pair=!off&&paired(p,kind);
        // Mouse 4 is auxiliary only; matching pairs fire from primary input.
        if(off&&!special&&paired(p,kind))return false;
        ItemStack second=pair?p.getOffhandItem():ItemStack.EMPTY;
        State s=STATES.computeIfAbsent(p.getUUID(),k->new State());
        long now=p.serverLevel().getGameTime();
        aimed=aimed&&s.aim&&s.aimStack==p.getMainHandItem();
        if(now<(off?s.nextOff:s.nextMain)||pair&&now<s.nextOff)return false;
        int n=pair?2:1;
        if(kind==Kind.DAGGER){
            int cost=special?2:1;
            if(!p.getAbilities().instabuild&&(RangedSupply.amount(first)<cost||pair&&RangedSupply.amount(second)<cost))return false;
            if(!p.getAbilities().instabuild){RangedSupply.set(first,RangedSupply.amount(first)-cost);if(pair)RangedSupply.set(second,RangedSupply.amount(second)-cost);}
        }else{
            if(!loaded(first)||pair&&!loaded(second)||!RangedSupply.take(p,kind,n))return false;
            flag(first,LOADED,false);if(pair)flag(second,LOADED,false);
        }
        Map<UUID,Long> hits=pair||kind==Kind.PISTOL?RangedProjectile.pair():null;
        launch(p,first,kind,off,aimed&&!off,special,hits);
        if(pair)launch(p,second,kind,true,aimed,special,hits);
        if(kind==Kind.PISTOL)pistolShot(p,off,pair,aimed&&!off,false);
        int gap=kind==Kind.DAGGER?12:5;
        if(off)s.nextOff=now+gap;else s.nextMain=now+gap;
        if(pair)s.nextOff=now+gap;
        p.getInventory().setChanged();
        if(kind==Kind.PISTOL){
            p.level().playSound(null,p.blockPosition(),SoundEvents.GENERIC_EXPLODE.value(),SoundSource.PLAYERS,pair?1.15f:.85f,1.45f);
            p.level().playSound(null,p.blockPosition(),SoundEvents.CROSSBOW_SHOOT,SoundSource.PLAYERS,.8f,.75f);
        }else p.level().playSound(null,p.blockPosition(),kind==Kind.DAGGER?SoundEvents.TRIDENT_THROW.value():SoundEvents.CROSSBOW_SHOOT,SoundSource.PLAYERS,.85f,1f);
        return true;
    }
    private static Vec3 shotStart(ServerPlayer p,boolean off,boolean pistol){
        Vec3 right=Vec3.directionFromRotation(0,p.getYRot()+90);
        // Minecraft yaw+90 points to the player's physical right.
        double side=(off?-1:1)*(p.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT?1:-1);
        return p.getEyePosition().add(right.scale(side*(pistol?.36:.22))).add(0,pistol?-.24:-.15,0);
    }
    private static Vec3 visibleMuzzle(ServerPlayer p,Vec3 start,Vec3 look){
        Vec3 end=start.add(look.scale(.65));
        var hit=p.level().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),end,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
        return hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS?end:hit.getLocation().subtract(look.scale(.08));
    }
    /** Server particle bursts; no fire blocks, explosions or destructive world changes. */
    private static void muzzleFlash(ServerPlayer p,boolean off,boolean wild){
        Vec3 look=p.getLookAngle(),m=visibleMuzzle(p,shotStart(p,off,true),look);
        var level=p.serverLevel();
        level.sendParticles(ParticleTypes.FLASH,m.x,m.y,m.z,1,0,0,0,0);
        level.sendParticles(ParticleTypes.FLAME,m.x,m.y,m.z,wild?34:16,.09,.09,.09,wild?.22:.15);
        level.sendParticles(ParticleTypes.LAVA,m.x,m.y,m.z,wild?7:3,.1,.1,.1,.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,m.x,m.y,m.z,wild?16:6,.13,.12,.13,.07);
        Vec3 right=Vec3.directionFromRotation(0,p.getYRot()+90),up=right.cross(look).normalize();
        int rays=wild?20:8;
        for(int i=0;i<rays;i++){
            double a=p.getRandom().nextDouble()*Math.PI*2;
            double spread=(wild?.48:.25)*Math.sqrt(p.getRandom().nextDouble());
            Vec3 v=look.scale(wild?.75:.45).add(right.scale(Math.cos(a)*spread)).add(up.scale(Math.sin(a)*spread));
            level.sendParticles(ParticleTypes.FLAME,m.x,m.y,m.z,0,v.x,v.y,v.z,1);
        }
    }
    private static void wildFlash(ServerPlayer p,boolean off){
        muzzleFlash(p,off,true);
        Vec3 look=p.getLookAngle(),m=visibleMuzzle(p,shotStart(p,off,true),look);
        var level=p.serverLevel();
        // A widening fire/smoke front, stopped by intervening solid blocks.
        for(int i=1;i<=3;i++){
            double distance=i*.72;
            Vec3 center=m.add(look.scale(distance));
            var hit=p.level().clip(new net.minecraft.world.level.ClipContext(m,center,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
            if(hit.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)break;
            double radius=.18+i*.18;
            level.sendParticles(ParticleTypes.FLAME,center.x,center.y,center.z,22,radius,radius,radius,.09);
            level.sendParticles(ParticleTypes.SMOKE,center.x,center.y,center.z,10,radius,radius,radius,.06);
            if(i==2)level.sendParticles(ParticleTypes.EXPLOSION,center.x,center.y,center.z,1,0,0,0,0);
        }
    }
    private static void launch(ServerPlayer p,ItemStack stack,Kind kind,boolean off,boolean aimed,boolean special,Map<UUID,Long> hits){
        boolean ink=kind==Kind.CROSSBOW&&empowered(stack);
        if(ink)flag(stack,EMPOWERED,false);
        boolean pistol=kind==Kind.PISTOL;
        float damage=kind==Kind.CROSSBOW?(ink?15:11):pistol?PISTOL_DAMAGE/PELLETS:4.5f;
        float bleed=kind==Kind.DAGGER?(special?.80f:.25f):0;
        Vec3 look=p.getLookAngle(),start=shotStart(p,off,pistol);
        // Do not place a projectile across an adjacent wall.
        var obstruction=p.level().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),start,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
        if(obstruction.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)start=p.getEyePosition();
        float spread=kind==Kind.CROSSBOW?(aimed?.5f:3f):pistol?(aimed?10f:18f):1.7f;
        Vec3 direction=aimed&&kind!=Kind.DAGGER?p.getEyePosition().add(look.scale(pistol?8:24)).subtract(start).normalize():look;
        int count=pistol?PELLETS:1;
        for(int i=0;i<count;i++){
            RangedProjectile shot=new RangedProjectile(p.serverLevel(),p,damage,bleed,ink,hits);
            shot.setItem(new ItemStack(kind==Kind.DAGGER?ModItems.BLOOD_THROWING_DAGGER:pistol?ModItems.PALE_BULLET:Items.ARROW));
            shot.setPos(start.x,start.y,start.z);
            if(pistol)shot.limitLifetime(PELLET_LIFETIME);
            shot.shoot(direction.x,direction.y,direction.z,kind==Kind.DAGGER?1.7f:pistol?3.2f:2.7f,spread);
            p.serverLevel().addFreshEntity(shot);
        }
        if(pistol)muzzleFlash(p,off,false);
    }
    private static ItemStack held(ServerPlayer p,SkillInput input){return input.sneaking()?p.getOffhandItem():p.getMainHandItem();}
    public static final Skill INK_SHOT=new Skill(TearsOfInk.id("ranged_ink_shot"),8,0,60){
        @Override public boolean activate(ServerPlayer p,SkillInput input){
            ItemStack s=held(p,input);
            if(input.sneaking()||RangedItem.kind(s)!=Kind.CROSSBOW||empowered(s))return false;
            flag(s,EMPOWERED,true);return true;
        }
    };
    public static final Skill BLOOD_SHOT=new Skill(TearsOfInk.id("ranged_blood_shot"),0,0,40){
        @Override public boolean activate(ServerPlayer p,SkillInput input){return RangedItem.kind(held(p,input))==Kind.DAGGER&&fire(p,input.sneaking(),false,true);}
    };
    public static final Skill WILD_SHOT=new Skill(TearsOfInk.id("ranged_wild_shot"),0,12,100){
        @Override public boolean activate(ServerPlayer p,SkillInput input){
            boolean off=input.sneaking(),pair=!off&&paired(p,Kind.PISTOL);
            ItemStack s=held(p,input),other=p.getOffhandItem();
            if(!valid(p)||off&&blocksOffhand(p)||RangedItem.kind(s)!=Kind.PISTOL||!loaded(s)
                ||pair&&!loaded(other)||!RangedSupply.take(p,Kind.PISTOL,pair?4:2))return false;
            flag(s,LOADED,false);
            if(pair)flag(other,LOADED,false);
            pistolShot(p,off,pair,false,true);
            p.getInventory().setChanged();
            Vec3 look=p.getLookAngle().multiply(1,0,1).normalize();
            if(look.lengthSqr()<1e-5)look=Vec3.directionFromRotation(0,p.getYRot());
            for(LivingEntity t:p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(3.3),e->e!=p&&e.isAlive())){
                Vec3 d=t.position().subtract(p.position()).multiply(1,0,1);
                if(d.lengthSqr()>10.89||d.lengthSqr()<1e-5||look.dot(d.normalize())<.45||!p.hasLineOfSight(t)
                    ||t.isAlliedTo(p)||!PantheonCombat.permitsInteraction(t,p))continue;
                if(Gear.hurtModWave(t,p.damageSources().playerAttack(p),PISTOL_WILD_DAMAGE*(pair?2:1)))t.knockback(1.1,p.getX()-t.getX(),p.getZ()-t.getZ());
            }
            p.setDeltaMovement(p.getDeltaMovement().add(look.scale(-.75)).add(0,.14,0));p.hurtMarked=true;
            wildFlash(p,off);
            if(pair)wildFlash(p,true);
            p.level().playSound(null,p.blockPosition(),SoundEvents.GENERIC_EXPLODE.value(),SoundSource.PLAYERS,pair?1.8f:1.45f,.85f);
            p.level().playSound(null,p.blockPosition(),SoundEvents.CROSSBOW_SHOOT,SoundSource.PLAYERS,1.0f,.55f);
            return true;
        }
    };
}
