package com.tarkrakov.tearsofink.client;

import com.tarkrakov.tearsofink.client.combat.WeaponAnims;
import com.tarkrakov.tearsofink.weapon.ranged.RangedCombat;
import com.tarkrakov.tearsofink.weapon.ranged.RangedItem;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.Map;

/** Release and reappearance are confirmed by the server, even under tick lag. */
public final class KnifeAnims {
    public static final String ID="blood_throwing_dagger";
    private static final Map<Integer,Hands> ACTIVE=new HashMap<>();
    private record Throw(long anchor,boolean special,int phase){}
    private static final class Hands { Throw main,off; }
    public record Motion(WeaponAnims.Clip clip,float time,boolean hidden){}
    private KnifeAnims(){}
    public static boolean isKnife(ItemStack s){return RangedItem.kind(s)==RangedItem.Kind.DAGGER;}
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(RangedCombat.KnifePose.TYPE,(v,ctx)->ctx.client().execute(()->{
            if(ctx.client().level==null)return;
            Hands h=ACTIVE.computeIfAbsent(v.entity(),id->new Hands());
            Throw k=v.phase()==RangedCombat.KNIFE_CANCEL?null:new Throw(System.nanoTime(),v.special(),v.phase());
            if(v.off())h.off=k;else h.main=k;
            if(v.pair())h.off=k;
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->ACTIVE.clear());
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            if(mc.level==null){ACTIVE.clear();return;}
            ACTIVE.entrySet().removeIf(e->{
                if(!(mc.level.getEntity(e.getKey()) instanceof Player p)||!p.isAlive())return true;
                Hands h=e.getValue();
                if(expired(h.main)||!isKnife(p.getMainHandItem()))h.main=null;
                if(expired(h.off)||!isKnife(p.getOffhandItem())||RangedCombat.blocksOffhand(p))h.off=null;
                return h.main==null&&h.off==null;
            });
        });
    }
    private static float elapsed(Throw k){return Math.max(0,(System.nanoTime()-k.anchor())/1e9f);}
    private static float release(){return RangedCombat.KNIFE_RELEASE/20f;}
    private static float reform(Throw k){return (k.special()?RangedCombat.KNIFE_BLOOD_RETURN:RangedCombat.KNIFE_RETURN)/20f;}
    private static float duration(Throw k){return (k.special()?RangedCombat.KNIFE_BLOOD_DURATION:RangedCombat.KNIFE_DURATION)/20f;}
    private static boolean expired(Throw k){return k==null||elapsed(k)>5||k.phase()==RangedCombat.KNIFE_REFORM&&elapsed(k)>=duration(k)-reform(k);}
    private static float time(Throw k){
        float t=elapsed(k);
        if(k.phase()==RangedCombat.KNIFE_START)return Math.min(t,release()-.0001f);
        if(k.phase()==RangedCombat.KNIFE_FLY)return Math.min(release()+t,reform(k)-.0001f);
        return Math.min(reform(k)+t,duration(k));
    }
    public static Motion motion(Player p,HumanoidArm side){
        if(!ModDimensions.hasLandsRules(p.level()))return null;
        Hands h=ACTIVE.get(p.getId());if(h==null)return null;
        boolean second=side!=p.getMainArm();
        if(!isKnife(second?p.getOffhandItem():p.getMainHandItem())||second&&RangedCombat.blocksOffhand(p))return null;
        Throw k=second?h.off:h.main;if(expired(k))return null;
        var clip=WeaponAnims.clip(ID,"bb_author_"+(k.special()?"blood_throw":"throw")+(side==HumanoidArm.LEFT?"_off":""));
        if(clip==null)return null;
        return new Motion(clip,Math.min(time(k),clip.length()),k.phase()==RangedCombat.KNIFE_FLY);
    }
    public static boolean hidden(Player p,HumanoidArm side){Motion m=motion(p,side);return m!=null&&m.hidden();}
}
