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
    public static final int KNIFE_RELEASE=5, KNIFE_RETURN=20, KNIFE_BLOOD_RETURN=10, KNIFE_DURATION=25, KNIFE_BLOOD_DURATION=15;
    public static final int KNIFE_START=0, KNIFE_FLY=1, KNIFE_REFORM=2, KNIFE_CANCEL=3;
    public static final int CROSSBOW_AIM_TICKS=5, CROSSBOW_SHOT_TICKS=7, CROSSBOW_RELOAD_TICKS=46;
    private static final String LOADED="toi_ranged_loaded", EMPOWERED="toi_ranged_empowered";
    private static final net.minecraft.resources.ResourceLocation SLOW=TearsOfInk.id("ranged_reload_slow");
    private static final Map<UUID,State> STATES=new HashMap<>();
    private static final class Job { ItemStack stack=ItemStack.EMPTY; int ticks, duration; }
    private static final class State { final Job main=new Job(), off=new Job(); long nextMain, nextOff; double slow; boolean aim, hadPistol; long aimSince, crossbowShotUntil; ItemStack crossbowShotStack=ItemStack.EMPTY, aimStack=ItemStack.EMPTY; PendingKnife mainKnife, offKnife; PendingCrossbow crossbowPending; }
    private record PendingCrossbow(net.minecraft.server.level.ServerLevel level,ItemStack stack,long release) {}
    private static final class PendingKnife {
        final net.minecraft.server.level.ServerLevel level; final ItemStack first, second;
        final boolean off, pair, special, aimed; final long start; boolean released, returned;
        PendingKnife(ServerPlayer p, ItemStack first, ItemStack second, boolean off, boolean pair, boolean special, boolean aimed, long start){
            this.level=p.serverLevel();this.first=first;this.second=second;this.off=off;this.pair=pair;this.special=special;this.aimed=aimed;this.start=start;
        }
    }
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
    public record KnifePose(int entity,boolean off,boolean pair,boolean special,int phase) implements CustomPacketPayload {
        public static final Type<KnifePose> TYPE=new Type<>(TearsOfInk.id("knife_throw_pose"));
        public static final StreamCodec<ByteBuf,KnifePose> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,KnifePose::entity,ByteBufCodecs.BOOL,KnifePose::off,ByteBufCodecs.BOOL,KnifePose::pair,ByteBufCodecs.BOOL,KnifePose::special,ByteBufCodecs.VAR_INT,KnifePose::phase,KnifePose::new);
        @Override public Type<KnifePose> type(){return TYPE;}
    }
    private static void knifePose(ServerPlayer p,PendingKnife k,int phase){
        var packet=new KnifePose(p.getId(),k.off,k.pair,k.special,phase);
        for(ServerPlayer viewer:k.level.players())if(viewer.distanceToSqr(p)<128*128&&ServerPlayNetworking.canSend(viewer,KnifePose.TYPE))ServerPlayNetworking.send(viewer,packet);
        if(p.serverLevel()!=k.level&&ServerPlayNetworking.canSend(p,KnifePose.TYPE))ServerPlayNetworking.send(p,packet);
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
    /** Server-authoritative phase for the authored FP/F5 crossbow clip. */
    public record CrossbowPose(int entity,int aimRemaining,int shotRemaining,int reloadTicks,int reloadMax) implements CustomPacketPayload {
        public static final Type<CrossbowPose> TYPE=new Type<>(TearsOfInk.id("crossbow_pose"));
        public static final StreamCodec<ByteBuf,CrossbowPose> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,CrossbowPose::entity,ByteBufCodecs.VAR_INT,CrossbowPose::aimRemaining,ByteBufCodecs.VAR_INT,CrossbowPose::shotRemaining,ByteBufCodecs.VAR_INT,CrossbowPose::reloadTicks,ByteBufCodecs.VAR_INT,CrossbowPose::reloadMax,CrossbowPose::new);
        @Override public Type<CrossbowPose> type(){return TYPE;}
    }
    private static void sendCrossbow(ServerPlayer p,State s){
        if(!blocksOffhand(p))return;
        int shot=s.crossbowShotStack==p.getMainHandItem()?(int)Math.clamp(s.crossbowShotUntil-p.serverLevel().getGameTime(),0,CROSSBOW_SHOT_TICKS):0;
        int aim=s.crossbowPending!=null&&s.crossbowPending.stack==p.getMainHandItem()&&s.crossbowPending.level==p.serverLevel()
            ?(int)Math.clamp(s.crossbowPending.release-p.serverLevel().getGameTime(),0,CROSSBOW_AIM_TICKS):0;
        boolean reloading=s.main.stack==p.getMainHandItem()&&!loaded(p.getMainHandItem());
        var packet=new CrossbowPose(p.getId(),aim,shot,reloading?s.main.ticks:0,reloading?s.main.duration:0);
        for(ServerPlayer viewer:p.serverLevel().players())if(viewer.distanceToSqr(p)<128*128&&ServerPlayNetworking.canSend(viewer,CrossbowPose.TYPE))ServerPlayNetworking.send(viewer,packet);
    }
    public static boolean canAim(Player p){
        Kind k=RangedItem.kind(p.getMainHandItem());
        return k==Kind.CROSSBOW||k==Kind.PISTOL||k==Kind.DAGGER&&ModDimensions.hasLandsRules(p.level());
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
        if(!s.aim||s.aimStack!=p.getMainHandItem())s.aimSince=p.serverLevel().getGameTime();
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
        PayloadTypeRegistry.playS2C().register(KnifePose.TYPE,KnifePose.CODEC);
        PayloadTypeRegistry.playS2C().register(PistolReload.TYPE,PistolReload.CODEC);
        PayloadTypeRegistry.playS2C().register(CrossbowPose.TYPE,CrossbowPose.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Aim.TYPE,(packet,ctx)->ctx.player().server.execute(()->aim(ctx.player(),packet.active())));
        ServerPlayNetworking.registerGlobalReceiver(Fire.TYPE,(packet,ctx)->ctx.player().server.execute(()->requestFire(ctx.player(),packet.off(),packet.aimed())));
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
            if(s.mainKnife!=null)knifePose(p,s.mainKnife,KNIFE_CANCEL);
            if(s.offKnife!=null)knifePose(p,s.offKnife,KNIFE_CANCEL);
            s.mainKnife=s.offKnife=null;s.crossbowShotStack=ItemStack.EMPTY;s.crossbowShotUntil=0;s.crossbowPending=null;
            aim(p,false);s.main.stack=ItemStack.EMPTY;s.main.ticks=s.main.duration=0;
            s.off.stack=ItemStack.EMPTY;s.off.ticks=s.off.duration=0;slow(p,s,0);sendReload(p,s);sendCrossbow(p,s);
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
        s.mainKnife=tickKnife(p,s.mainKnife);
        s.offKnife=tickKnife(p,s.offKnife);
        tickCrossbow(p,s);
        if(s.aim&&(!canAim(p)||s.aimStack!=p.getMainHandItem()))aim(p,false);
        boolean blocked=blocksOffhand(p);
        if(blocked&&p.isUsingItem()&&p.getUsedItemHand()==InteractionHand.OFF_HAND)p.stopUsingItem();
        boolean pair=paired(p,Kind.PISTOL);
        boolean m=reload(p,s.main,p.getMainHandItem(),false,pair);
        boolean o=reload(p,s.off,p.getOffhandItem(),true,pair);
        slow(p,s,m&&blocked?.50:m||o?PistolTuning.RELOAD_SLOW:0);
        if(p.tickCount%4==0||!has){sendReload(p,s);sendCrossbow(p,s);}
        if(!has)STATES.remove(p.getUUID());
    }
    private static boolean reload(ServerPlayer p,Job job,ItemStack stack,boolean off,boolean pair){
        Kind kind=RangedItem.kind(stack);
        if(job.stack!=stack){job.stack=stack;job.ticks=0;}
        job.duration=0;
        if(kind==null||kind==Kind.DAGGER||loaded(stack)||(off&&(blocksOffhand(p)||kind==Kind.CROSSBOW))){
            job.ticks=0;return false;
        }
        if(!p.getAbilities().instabuild&&RangedSupply.available(p,kind)<(pair?2:1)*(kind==Kind.PISTOL?PistolTuning.SHOT_COST:1)){job.ticks=0;return false;}
        State state=STATES.get(p.getUUID());
        if(kind==Kind.CROSSBOW&&state!=null&&state.crossbowShotStack==stack&&p.serverLevel().getGameTime()<state.crossbowShotUntil){job.ticks=0;return false;}
        int duration=kind==Kind.CROSSBOW?CROSSBOW_RELOAD_TICKS:pair?PistolTuning.DUAL_RELOAD:PistolTuning.RELOAD;
        job.duration=duration;
        if(++job.ticks==1&&kind==Kind.CROSSBOW&&state!=null)sendCrossbow(p,state);
        if(job.ticks>=duration){
            flag(stack,LOADED,true);job.ticks=job.duration=0;
            if(kind==Kind.CROSSBOW&&state!=null){state.aimSince=p.serverLevel().getGameTime();sendCrossbow(p,state);}
            p.level().playSound(null,p.blockPosition(),SoundEvents.CROSSBOW_LOADING_END.value(),SoundSource.PLAYERS,.5f,1f);
            return false;
        }
        return true;
    }
    /** A single LMB schedules a server-owned aim then shot. RMB remains manual aim. */
    private static void requestFire(ServerPlayer p,boolean off,boolean aimed){
        if(off||!blocksOffhand(p)){fire(p,off,aimed,false);return;}
        if(!valid(p))return;
        ItemStack stack=p.getMainHandItem();
        State s=STATES.computeIfAbsent(p.getUUID(),k->new State());
        long now=p.serverLevel().getGameTime();
        if(s.crossbowPending!=null||now<s.nextMain||!loaded(stack)
            ||!p.getAbilities().instabuild&&RangedSupply.available(p,Kind.CROSSBOW)<1)return;
        long held=aimed&&s.aim&&s.aimStack==stack?Math.clamp(now-s.aimSince,0,CROSSBOW_AIM_TICKS):0;
        if(held>=CROSSBOW_AIM_TICKS){fire(p,false,true,false,true);return;}
        s.crossbowPending=new PendingCrossbow(p.serverLevel(),stack,now+CROSSBOW_AIM_TICKS-held);
        sendCrossbow(p,s);
    }
    private static void tickCrossbow(ServerPlayer p,State s){
        PendingCrossbow pending=s.crossbowPending;
        if(pending==null)return;
        if(p.serverLevel()!=pending.level||p.getMainHandItem()!=pending.stack||!loaded(pending.stack)
            ||!p.getAbilities().instabuild&&RangedSupply.available(p,Kind.CROSSBOW)<1){
            s.crossbowPending=null;sendCrossbow(p,s);return;
        }
        if(p.serverLevel().getGameTime()<pending.release)return;
        s.crossbowPending=null;
        if(!fire(p,false,true,false,true))sendCrossbow(p,s);
    }
    private static boolean fire(ServerPlayer p,boolean off,boolean aimed,boolean special){
        return fire(p,off,aimed,special,false);
    }
    private static boolean fire(ServerPlayer p,boolean off,boolean aimed,boolean special,boolean crossbowAimed){
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
        aimed=kind==Kind.CROSSBOW?crossbowAimed:aimed&&s.aim&&s.aimStack==p.getMainHandItem();
        if(kind==Kind.CROSSBOW&&!aimed)return false;
        if(now<(off?s.nextOff:s.nextMain)||pair&&now<s.nextOff)return false;
        int n=(pair?2:1)*(kind==Kind.PISTOL?PistolTuning.SHOT_COST:1);
        if(kind==Kind.DAGGER){
            int cost=special?KnifeTuning.BLOOD_COST:KnifeTuning.COST;
            if(!p.getAbilities().instabuild&&(RangedSupply.amount(first)<cost||pair&&RangedSupply.amount(second)<cost))return false;
            PendingKnife k=new PendingKnife(p,first,second,off,pair,special,aimed&&!off,now);
            if(off)s.offKnife=k;else s.mainKnife=k;
            int duration=special?KNIFE_BLOOD_DURATION:KNIFE_DURATION;
            if(off)s.nextOff=now+duration;else s.nextMain=now+duration;
            if(pair)s.nextOff=now+duration;
            knifePose(p,k,KNIFE_START);
            return true;
        }else{
            if(!loaded(first)||pair&&!loaded(second)||!RangedSupply.take(p,kind,n))return false;
            flag(first,LOADED,false);if(pair)flag(second,LOADED,false);
        }
        Map<UUID,Long> hits=pair||kind==Kind.PISTOL?RangedProjectile.pair():null;
        launch(p,first,kind,off,aimed&&!off,special,hits);
        if(pair)launch(p,second,kind,true,aimed,special,hits);
        if(kind==Kind.PISTOL)pistolShot(p,off,pair,aimed&&!off,false);
        int gap=kind==Kind.CROSSBOW?CROSSBOW_SHOT_TICKS:5;
        if(kind==Kind.CROSSBOW){s.crossbowShotStack=first;s.crossbowShotUntil=now+CROSSBOW_SHOT_TICKS;sendCrossbow(p,s);}
        if(off)s.nextOff=now+gap;else s.nextMain=now+gap;
        if(pair)s.nextOff=now+gap;
        p.getInventory().setChanged();
        if(kind==Kind.PISTOL){
            p.level().playSound(null,p.blockPosition(),SoundEvents.GENERIC_EXPLODE.value(),SoundSource.PLAYERS,pair?1.15f:.85f,1.45f);
            p.level().playSound(null,p.blockPosition(),SoundEvents.CROSSBOW_SHOOT,SoundSource.PLAYERS,.8f,.75f);
        }else p.level().playSound(null,p.blockPosition(),kind==Kind.DAGGER?SoundEvents.TRIDENT_THROW.value():SoundEvents.CROSSBOW_SHOOT,SoundSource.PLAYERS,.85f,1f);
        return true;
    }
    private static PendingKnife tickKnife(ServerPlayer p,PendingKnife k){
        if(k==null)return null;
        ItemStack held=k.off?p.getOffhandItem():p.getMainHandItem();
        if(k.level!=p.serverLevel()||held!=k.first||k.off&&blocksOffhand(p)||k.pair&&p.getOffhandItem()!=k.second){knifePose(p,k,KNIFE_CANCEL);return null;}
        long elapsed=p.serverLevel().getGameTime()-k.start;
        if(!k.released&&elapsed>=KNIFE_RELEASE){
            int cost=k.special?KnifeTuning.BLOOD_COST:KnifeTuning.COST;
            if(!p.getAbilities().instabuild&&(RangedSupply.amount(k.first)<cost||k.pair&&RangedSupply.amount(k.second)<cost)){knifePose(p,k,KNIFE_CANCEL);return null;}
            if(!p.getAbilities().instabuild){RangedSupply.set(k.first,RangedSupply.amount(k.first)-cost);if(k.pair)RangedSupply.set(k.second,RangedSupply.amount(k.second)-cost);}
            Map<UUID,Long> hits=k.pair?RangedProjectile.pair():null;
            launch(p,k.first,Kind.DAGGER,k.off,k.aimed,k.special,hits);
            if(k.pair)launch(p,k.second,Kind.DAGGER,true,k.aimed,k.special,hits);
            k.released=true;p.getInventory().setChanged();
            knifePose(p,k,KNIFE_FLY);
            p.level().playSound(null,p.blockPosition(),SoundEvents.TRIDENT_THROW.value(),SoundSource.PLAYERS,.85f,1f);
        }
        if(!k.returned&&elapsed>=(k.special?KNIFE_BLOOD_RETURN:KNIFE_RETURN)){
            k.returned=true;knifePose(p,k,KNIFE_REFORM);knifeReform(p,k.off);if(k.pair)knifeReform(p,true);
        }
        return elapsed>=(k.special?KNIFE_BLOOD_DURATION:KNIFE_DURATION)?null:k;
    }
    private static void knifeReform(ServerPlayer p,boolean off){
        Vec3 at=shotStart(p,off,false).add(p.getLookAngle().scale(.25)).add(0,-.35,0);
        p.serverLevel().sendParticles(RangedProjectile.KNIFE_TRACE,at.x,at.y,at.z,7,.09,.12,.09,.01);
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
        RangedShotPerks perks=RangedShotPerks.capture(stack,ink||special);
        float damage=kind==Kind.CROSSBOW?(ink?CrossbowTuning.INK_DAMAGE:CrossbowTuning.DAMAGE):pistol?PistolTuning.DAMAGE/PistolTuning.PELLETS:KnifeTuning.DAMAGE;
        float bleed=kind==Kind.DAGGER?Math.min(1f,(special?KnifeTuning.BLOOD_BLEED_CHANCE:KnifeTuning.BLEED_CHANCE)+perks.bleed()):0;
        Vec3 look=p.getLookAngle(),start=kind==Kind.CROSSBOW?p.getEyePosition().add(0,-.15,0):shotStart(p,off,pistol);
        // Do not place a projectile across an adjacent wall.
        var obstruction=p.level().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),start,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
        if(obstruction.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)start=p.getEyePosition();
        float spread=kind==Kind.CROSSBOW?(aimed?.5f:3f):pistol?(aimed?PistolTuning.AIM_SPREAD:PistolTuning.HIP_SPREAD):(aimed?KnifeTuning.AIM_SPREAD:KnifeTuning.SPREAD);
        spread*=1-perks.precision();
        Vec3 direction=aimed?(kind==Kind.DAGGER?aimedKnifeDirection(p,start,look):p.getEyePosition().add(look.scale(pistol?PistolTuning.AIM_DISTANCE:24)).subtract(start).normalize()):look;
        int count=pistol?PistolTuning.PELLETS:1;
        for(int i=0;i<count;i++){
            RangedProjectile shot=new RangedProjectile(p.serverLevel(),p,damage,bleed,ink,hits);
            shot.setPerks(perks);
            shot.setItem(new ItemStack(kind==Kind.DAGGER?ModItems.BLOOD_THROWING_DAGGER:pistol?ModItems.PALE_BULLET:ink?ModItems.INK_CROSSBOW_BOLT_EMPOWERED_VISUAL:Items.ARROW));
            shot.setPos(start.x,start.y,start.z);
            if(pistol)shot.limitLifetime(PistolTuning.PELLET_LIFETIME);
            else if(kind==Kind.DAGGER)shot.limitLifetime(aimed?KnifeTuning.AIM_LIFETIME:KnifeTuning.LIFETIME);
            shot.shoot(direction.x,direction.y,direction.z,kind==Kind.DAGGER?(aimed?KnifeTuning.AIM_SPEED:KnifeTuning.SPEED):pistol?PistolTuning.SPEED:2.7f,spread);
            p.serverLevel().addFreshEntity(shot);
        }
        Gear.wear(p,stack,special?Gear.WEAR_ABILITY:Gear.WEAR_HIT);
        p.getInventory().setChanged();
        if(pistol)muzzleFlash(p,off,false);
    }
    /** Converge on the visible crosshair point, with a small ballistic correction.
     * Collision, gravity and damage still belong to the real projectile. */
    private static Vec3 aimedKnifeDirection(ServerPlayer p,Vec3 start,Vec3 look){
        Vec3 eye=p.getEyePosition(),end=eye.add(look.scale(KnifeTuning.AIM_DISTANCE));
        var block=p.level().clip(new net.minecraft.world.level.ClipContext(eye,end,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
        if(block.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)end=block.getLocation();
        Vec3 ray=end.subtract(eye);
        var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(p,eye,end,
            p.getBoundingBox().expandTowards(ray).inflate(1),e->e instanceof LivingEntity&&e.isPickable()&&!e.isSpectator(),eye.distanceToSqr(end));
        if(hit!=null)end=hit.getLocation();
        Vec3 delta=end.subtract(start);
        if(delta.lengthSqr()<.04||delta.dot(look)<=0)return look;
        double time=Math.min(KnifeTuning.AIM_LIFETIME,delta.length()/KnifeTuning.AIM_SPEED);
        return delta.add(0,KnifeTuning.AIM_GRAVITY_COMPENSATION*time*time*.5,0).normalize();
    }
    private static ItemStack held(ServerPlayer p,SkillInput input){return input.sneaking()?p.getOffhandItem():p.getMainHandItem();}
    public static final Skill INK_SHOT=new Skill(TearsOfInk.id("ranged_ink_shot"),8,0,60){
        @Override public boolean activate(ServerPlayer p,SkillInput input){
            ItemStack s=held(p,input);
            if(input.sneaking()||RangedItem.kind(s)!=Kind.CROSSBOW||empowered(s))return false;
            flag(s,EMPOWERED,true);p.getInventory().setChanged();return true;
        }
    };
    public static final Skill BLOOD_SHOT=new Skill(TearsOfInk.id("ranged_blood_shot"),0,0,KnifeTuning.BLOOD_COOLDOWN){
        @Override public boolean activate(ServerPlayer p,SkillInput input){return RangedItem.kind(held(p,input))==Kind.DAGGER&&fire(p,input.sneaking(),!input.sneaking(),true);}
    };
    public static final Skill WILD_SHOT=new Skill(TearsOfInk.id("ranged_wild_shot"),0,PistolTuning.WILD_SOUL,PistolTuning.WILD_COOLDOWN){
        @Override public boolean activate(ServerPlayer p,SkillInput input){
            boolean off=input.sneaking(),pair=!off&&paired(p,Kind.PISTOL);
            ItemStack s=held(p,input),other=p.getOffhandItem();
            if(!valid(p)||off&&blocksOffhand(p)||RangedItem.kind(s)!=Kind.PISTOL||!loaded(s)
                ||pair&&!loaded(other)||!RangedSupply.take(p,Kind.PISTOL,(pair?2:1)*PistolTuning.WILD_COST))return false;
            RangedShotPerks firstPerks=RangedShotPerks.capture(s,true),secondPerks=pair?RangedShotPerks.capture(other,true):RangedShotPerks.NONE;
            flag(s,LOADED,false);
            if(pair)flag(other,LOADED,false);
            pistolShot(p,off,pair,false,true);
            p.getInventory().setChanged();
            Vec3 look=p.getLookAngle().multiply(1,0,1).normalize();
            if(look.lengthSqr()<1e-5)look=Vec3.directionFromRotation(0,p.getYRot());
            for(LivingEntity t:p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(PistolTuning.WILD_RANGE),e->e!=p&&e.isAlive())){
                Vec3 d=t.position().subtract(p.position()).multiply(1,0,1);
                if(d.lengthSqr()>PistolTuning.WILD_RANGE_SQUARED||d.lengthSqr()<1e-5||look.dot(d.normalize())<PistolTuning.WILD_FORWARD_DOT||!p.hasLineOfSight(t)
                    ||t.isAlliedTo(p)||!PantheonCombat.permitsInteraction(t,p))continue;
                float damage=firstPerks.damage(p,t,PistolTuning.WILD_DAMAGE)+(pair?secondPerks.damage(p,t,PistolTuning.WILD_DAMAGE):0);
                if(Gear.hurtRanged(t,p.damageSources().playerAttack(p),damage))t.knockback(PistolTuning.WILD_KNOCKBACK,p.getX()-t.getX(),p.getZ()-t.getZ());
            }
            Gear.wear(p,s,Gear.WEAR_ABILITY);if(pair)Gear.wear(p,other,Gear.WEAR_ABILITY);
            p.getInventory().setChanged();
            p.setDeltaMovement(p.getDeltaMovement().add(look.scale(-PistolTuning.WILD_RECOIL)).add(0,PistolTuning.WILD_LIFT,0));p.hurtMarked=true;
            wildFlash(p,off);
            if(pair)wildFlash(p,true);
            p.level().playSound(null,p.blockPosition(),SoundEvents.GENERIC_EXPLODE.value(),SoundSource.PLAYERS,pair?1.8f:1.45f,.85f);
            p.level().playSound(null,p.blockPosition(),SoundEvents.CROSSBOW_SHOOT,SoundSource.PLAYERS,1.0f,.55f);
            return true;
        }
    };
}
