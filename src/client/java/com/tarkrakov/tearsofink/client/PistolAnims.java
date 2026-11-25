package com.tarkrakov.tearsofink.client;

import com.tarkrakov.tearsofink.client.combat.WeaponAnims;
import com.tarkrakov.tearsofink.weapon.ranged.RangedCombat;
import com.tarkrakov.tearsofink.weapon.ranged.RangedItem;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.Map;

/** Author clips are optional; no fake authored poses and no melee combo registration.
 * Every hand has its own accepted-shot and reload timeline. Aim holds the last authored key. */
public final class PistolAnims {
    private static final String ID="holy_pistol", PREFIX="bb_author_";
    private static final Map<Integer,Hands> PLAYERS=new HashMap<>();
    private static final class Hands {
        final Hand main=new Hand(),off=new Hand();
        long touched;
        Hand hand(boolean second){return second?off:main;}
    }
    private static final class Hand {
        boolean aiming,hadAim,dualAim;
        long aimAt,shotAt,reloadAt,transitionAt;
        String shot;
        int reloadTicks,reloadMax;
        Sample shown,previous;
    }
    private record Sample(WeaponAnims.Clip clip,float time,boolean mirrorWrist){}
    public record Motion(WeaponAnims.Clip clip,float time,boolean mirrorWrist,
                         WeaponAnims.Clip previous,float previousTime,boolean previousMirrorWrist,float blend){}
    private PistolAnims(){}
    public static boolean isPistol(ItemStack s){return RangedItem.kind(s)==RangedItem.Kind.PISTOL;}
    private static long now(){return System.nanoTime();}
    private static float elapsed(long then,long now){return Math.max(0,(now-then)/1e9f);}
    private static Hands state(int id,long time){Hands h=PLAYERS.computeIfAbsent(id,k->new Hands());h.touched=time;return h;}
    private static void shot(Hand h,String name,long time){h.shot=name;h.shotAt=time;}
    private static void reload(Hand h,int ticks,int max,long time){
        h.reloadTicks=ticks;h.reloadMax=max;h.reloadAt=time;
    }
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(RangedCombat.PistolShot.TYPE,(v,ctx)->ctx.client().execute(()->{
            long t=now();Hands h=state(v.entity(),t);
            String name=v.wild()?"wild_shot":(v.pair()?"dual_":"")+(v.aimed()?"shoot_aimed":"shoot");
            shot(h.hand(v.off()),name,t);
            if(v.pair())shot(h.off,name,t);
        }));
        ClientPlayNetworking.registerGlobalReceiver(RangedCombat.PistolReload.TYPE,(v,ctx)->ctx.client().execute(()->{
            long t=now();Hands h=state(v.entity(),t);
            reload(h.main,v.main(),v.mainMax(),t);reload(h.off,v.off(),v.offMax(),t);
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->PLAYERS.clear());
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            if(mc.level==null){PLAYERS.clear();return;}
            if(mc.player!=null&&mc.player.tickCount%20==0){
                long t=now();
                PLAYERS.entrySet().removeIf(e->elapsed(e.getValue().touched,t)>10||mc.level.getEntity(e.getKey())==null);
            }
            if(mc.player!=null){
                Hands h=PLAYERS.get(mc.player.getId());
                if(h!=null){
                    if(!isPistol(mc.player.getMainHandItem())) reset(h.main);
                    if(!isPistol(mc.player.getOffhandItem())||RangedCombat.blocksOffhand(mc.player))reset(h.off);
                }
            }
        });
    }
    private static void reset(Hand h){
        h.aiming=h.hadAim=false;h.shot=null;h.reloadTicks=h.reloadMax=0;h.shown=h.previous=null;
    }
    private static Sample sample(String name,boolean second,HumanoidArm physical,float seconds,boolean fraction,boolean hold){
        if(name==null)return null;
        String key=PREFIX+name;
        WeaponAnims.Clip c=WeaponAnims.clip(ID,key+(second?"_off":""));
        boolean authoredLeft=second&&c!=null;
        if(c==null)c=WeaponAnims.clip(ID,key);
        if(c==null)return null;
        float t=fraction?seconds*c.length():seconds;
        if(!hold&&t>c.length())return null;
        // Main clip is authored for right; the off variant is authored for left.
        boolean mirrorWrist=(physical==HumanoidArm.LEFT)!=authoredLeft;
        return new Sample(c,Math.clamp(t,0,c.length()),mirrorWrist);
    }
    public static Motion motion(Player p,HumanoidArm side){
        if(!ModDimensions.hasLandsRules(p.level()))return null;
        boolean second=side!=p.getMainArm();
        if(!isPistol(second?p.getOffhandItem():p.getMainHandItem())||second&&RangedCombat.blocksOffhand(p))return null;
        long t=now();Hands both=state(p.getId(),t);Hand h=both.hand(second);
        boolean pair=isPistol(p.getMainHandItem())&&isPistol(p.getOffhandItem());
        boolean aim=RangedClient.aimsHand(p,second);
        if(aim!=h.aiming||aim&&pair!=h.dualAim){
            h.aimAt=t;h.hadAim=h.aiming||aim;h.aiming=aim;h.dualAim=pair;
        }
        Sample next=sample(h.shot,second,side,elapsed(h.shotAt,t),false,false);
        if(next==null)h.shot=null;
        boolean reloading=h.reloadMax>0&&elapsed(h.reloadAt,t)<.6f&&!RangedCombat.loaded(second?p.getOffhandItem():p.getMainHandItem());
        if(next==null&&reloading){
            float progress=Math.clamp((h.reloadTicks+20*elapsed(h.reloadAt,t))/h.reloadMax,0f,1f);
            next=sample(pair?"dual_reload":"reload",second,side,progress,true,true);
        }
        if(next==null&&aim)next=sample(pair?"dual_aim_enter":"aim_enter",second,side,elapsed(h.aimAt,t),false,true);
        if(next==null&&!aim&&h.hadAim)next=sample(h.dualAim?"dual_aim_exit":"aim_exit",second,side,elapsed(h.aimAt,t),false,false);
        if(next==null){h.shown=h.previous=null;return null;}
        if(h.shown!=null&&h.shown.clip!=next.clip){h.previous=h.shown;h.transitionAt=t;}
        h.shown=next;
        float w=h.previous==null?1:Math.clamp(elapsed(h.transitionAt,t)/.09f,0f,1f);
        if(w>=1)h.previous=null;
        return new Motion(next.clip,next.time,next.mirrorWrist,h.previous==null?null:h.previous.clip,
            h.previous==null?0:h.previous.time,h.previous!=null&&h.previous.mirrorWrist,w);
    }
}
