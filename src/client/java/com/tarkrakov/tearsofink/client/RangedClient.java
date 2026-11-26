package com.tarkrakov.tearsofink.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tarkrakov.tearsofink.weapon.ranged.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import java.util.HashMap;
import java.util.Map;

public final class RangedClient {
    private static KeyMapping off;
    private static boolean sentAim, aimRequested;
    private static ItemStack lastMain=ItemStack.EMPTY;
    private static final Map<Integer,Long> REMOTE_AIM=new HashMap<>();
    private static RangedCombat.ReloadView reload=new RangedCombat.ReloadView(0,0,0,0);
    private RangedClient(){}
    public static void beginUseAttempt(){aimRequested=false;}
    public static void aimAfterInteraction(Minecraft mc){aimRequested=mc.player!=null&&RangedCombat.canAim(mc.player);}
    private static boolean localAim(Minecraft mc){
        return aimRequested&&mc.player!=null&&mc.screen==null&&!mc.player.isSpectator()&&RangedCombat.canAim(mc.player)&&mc.options.keyUse.isDown()&&!SkillKeys.suppressHeldUse(mc);
    }
    public static boolean aiming(Player player){
        Minecraft mc=Minecraft.getInstance();
        if(player==mc.player)return localAim(mc);
        return RangedCombat.canAim(player)&&REMOTE_AIM.getOrDefault(player.getId(),Long.MIN_VALUE)>=player.level().getGameTime();
    }
    private static void syncAim(Minecraft mc,boolean periodic){
        boolean now=localAim(mc);
        ItemStack main=mc.player==null?ItemStack.EMPTY:mc.player.getMainHandItem();
        if((now!=sentAim||now&&(periodic||!ItemStack.isSameItem(main,lastMain)))&&ClientPlayNetworking.canSend(RangedCombat.Aim.TYPE))
            ClientPlayNetworking.send(new RangedCombat.Aim(now));
        sentAim=now;lastMain=main;
    }
    public static boolean aimsHand(Player p,boolean offHand){
        return aiming(p)&&(!offHand||RangedItem.kind(p.getMainHandItem())==RangedItem.Kind.PISTOL&&RangedItem.kind(p.getOffhandItem())==RangedItem.Kind.PISTOL);
    }
    /** Temporary F5 stance only; the final art uses authored clips. */
    public static void pose(HumanoidModel<?> model,Player p){
        if(aiming(p))fallbackPose(model,p);
        com.tarkrakov.tearsofink.client.combat.ThirdPerson.pistol(model,p);
    }
    private static void fallbackPose(HumanoidModel<?> model,Player p){
        boolean right=p.getMainArm()==HumanoidArm.RIGHT;
        var main=right?model.rightArm:model.leftArm;
        var second=right?model.leftArm:model.rightArm;
        main.xRot=-(float)Math.PI/2+model.head.xRot;
        main.yRot=model.head.yRot+(right?-.14f:.14f);
        main.zRot=0;
        if(RangedCombat.blocksOffhand(p)||aimsHand(p,true)){
            second.xRot=main.xRot;
            second.yRot=model.head.yRot+(right?.32f:-.32f);
            second.zRot=0;
        }
    }
    public static void init(){
        PistolAnims.init();
        off=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.tears-of-ink.ranged_offhand", InputConstants.Type.MOUSE,GLFW.GLFW_MOUSE_BUTTON_4,SkillKeys.CATEGORY));
        ClientPlayNetworking.registerGlobalReceiver(RangedCombat.AimPose.TYPE,(v,ctx)->ctx.client().execute(()->{
            if(v.active()&&ctx.client().level!=null)REMOTE_AIM.put(v.entity(),ctx.client().level.getGameTime()+45);
            else REMOTE_AIM.remove(v.entity());
        }));
        ClientPlayNetworking.registerGlobalReceiver(RangedCombat.ReloadView.TYPE,(v,ctx)->ctx.client().execute(()->reload=v));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->{REMOTE_AIM.clear();sentAim=false;aimRequested=false;lastMain=ItemStack.EMPTY;reload=new RangedCombat.ReloadView(0,0,0,0);});
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            if(!mc.options.keyUse.isDown()||mc.screen!=null||mc.player==null||!RangedCombat.canAim(mc.player)||SkillKeys.suppressHeldUse(mc))aimRequested=false;
            if(mc.player!=null){syncAim(mc,mc.player.tickCount%20==0);if(mc.player.tickCount%40==0)REMOTE_AIM.values().removeIf(t->t<mc.player.level().getGameTime());}
            while(off.consumeClick()){if(mc.screen==null)fire(true);}
        });
        HudRenderCallback.EVENT.register((g,d)->{
            Minecraft mc=Minecraft.getInstance();
            if(mc.player==null||mc.screen!=null||mc.options.hideGui||mc.player.isSpectator())return;
            int y=g.guiHeight()/2+14;
            for(boolean second:new boolean[]{false,true}){
                ItemStack s=second?mc.player.getOffhandItem():mc.player.getMainHandItem();
                RangedItem.Kind kind=RangedItem.kind(s);
                if(kind==null||second&&RangedCombat.blocksOffhand(mc.player))continue;
                int n=kind==RangedItem.Kind.DAGGER?RangedSupply.amount(s):RangedSupply.available(mc.player,kind);
                int progress=second?reload.off():reload.main(),max=second?reload.offMax():reload.mainMax();
                String txt=kind==RangedItem.Kind.DAGGER?String.valueOf(n):
                    Component.translatable(RangedCombat.loaded(s)?"ranged.tears-of-ink.loaded":"ranged.tears-of-ink.unloaded").getString()+"  "+n;
                if(RangedCombat.empowered(s))txt+=" +";
                int center=g.guiWidth()/2;
                g.drawString(mc.font,txt,center-mc.font.width(txt)/2,y,0xffe3d9bd,true);
                y+=11;
                if(max>0&&!RangedCombat.loaded(s)&&kind!=RangedItem.Kind.DAGGER){
                    g.fill(center-30,y,center+30,y+3,0x99413d39);
                    g.fill(center-30,y,center-30+Math.clamp(60*progress/max,0,60),y+3,0xffd7b980);
                    y+=5;
                }
            }
        });
    }
    public static void fire(boolean secondary){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.player.isSpectator()||!ClientPlayNetworking.canSend(RangedCombat.Fire.TYPE))return;
        if(RangedItem.kind(secondary?mc.player.getOffhandItem():mc.player.getMainHandItem())==null)return;
        syncAim(mc,false);
        ClientPlayNetworking.send(new RangedCombat.Fire(secondary,!secondary&&localAim(mc)));
    }
}
