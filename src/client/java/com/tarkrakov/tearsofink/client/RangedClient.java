package com.tarkrakov.tearsofink.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tarkrakov.tearsofink.weapon.ranged.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class RangedClient {
    private static KeyMapping off;
    private RangedClient(){}
    public static void init(){
        off=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.tears-of-ink.ranged_offhand", InputConstants.Type.MOUSE,GLFW.GLFW_MOUSE_BUTTON_4,SkillKeys.CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(mc->{while(off.consumeClick()){if(mc.screen==null)fire(true);}});
        HudRenderCallback.EVENT.register((g,d)->{
            Minecraft mc=Minecraft.getInstance();
            if(mc.player==null||mc.screen!=null||mc.options.hideGui||mc.player.isSpectator())return;
            int y=g.guiHeight()/2+14;
            for(ItemStack s:new ItemStack[]{mc.player.getMainHandItem(),mc.player.getOffhandItem()}){
                RangedItem.Kind kind=RangedItem.kind(s);
                if(kind==null)continue;
                if(s==mc.player.getOffhandItem()&&RangedCombat.blocksOffhand(mc.player))continue;
                int n=kind==RangedItem.Kind.DAGGER?RangedSupply.amount(s):RangedSupply.available(mc.player,kind);
                String txt=kind==RangedItem.Kind.DAGGER?String.valueOf(n):
                    Component.translatable(RangedCombat.loaded(s)?"ranged.tears-of-ink.loaded":"ranged.tears-of-ink.unloaded").getString()+"  "+n;
                if(RangedCombat.empowered(s))txt+=" +";
                g.drawString(mc.font,txt,g.guiWidth()/2-mc.font.width(txt)/2,y,0xffe3d9bd,true);
                y+=11;
            }
        });
    }
    public static void fire(boolean secondary){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.player.isSpectator()||!ClientPlayNetworking.canSend(RangedCombat.Fire.TYPE))return;
        if(RangedItem.kind(secondary?mc.player.getOffhandItem():mc.player.getMainHandItem())==null)return;
        ClientPlayNetworking.send(new RangedCombat.Fire(secondary,!secondary&&mc.options.keyUse.isDown()));
    }
}
