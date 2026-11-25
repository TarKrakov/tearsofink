package com.tarkrakov.tearsofink.client.hud;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.client.SkillKeys;
import com.tarkrakov.tearsofink.skill.CombatCooldowns;
import com.tarkrakov.tearsofink.skill.CombatCooldowns.Entry;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Temporary pixel glyphs; a matching 32x32 resource can replace each without code changes. */
public final class CombatCooldownHud {
    private static CombatCooldowns.Snapshot view = CombatCooldowns.Snapshot.EMPTY;
    private CombatCooldownHud() {}
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(CombatCooldowns.Snapshot.TYPE,(p,c)->c.client().execute(()->view=p));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->view=CombatCooldowns.Snapshot.EMPTY);
        HudRenderCallback.EVENT.register(CombatCooldownHud::render);
    }
    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.options.hideGui ||
                mc.getDebugOverlay().showDebugScreen() || !mc.player.isAlive() || mc.player.isSpectator() ||
                mc.gameMode == null || !mc.gameMode.canHurtPlayer() || !ModDimensions.hasLandsRules(mc.level)) return;
        Entry[] slots = {view.move(),view.r(),view.rmb(),view.shiftR(),view.loadout()};
        String r = SkillKeys.weaponKeyLabel().getString();
        String shift = mc.options.keyShift.getTranslatedKeyMessage().getString();
        String[] keys = {SkillKeys.mobilityKeyLabel().getString(),r,mc.options.keyUse.getTranslatedKeyMessage().getString(),shift+"+"+r,SkillKeys.loadoutKeyLabel().getString()};
        int count=0; for (Entry e:slots) if (!e.id().isEmpty()) count++;
        if(count==0) return;
        // Draw in the original logical coordinates, then halve the whole HUD.
        // Its right edge aligns with the left-hand slot instead of the health bar.
        final float scale = 0.5f;
        final int rowHeight = 19;
        int edge = g.guiWidth()/2-100;
        int available = edge-3;
        // With insufficient room beside the hotbar, keep clear of the heart rows.
        boolean narrow = available < 27;
        int width = narrow ? 54 : Math.min(76,available*2);
        int x = narrow ? 3 : edge-(width+1)/2;
        int bottom = g.guiHeight() - (narrow ? 80 : 25);
        int top = Math.max(3,bottom-(count*rowHeight+1)/2);
        g.pose().pushPose();
        g.pose().translate(x,top,0);
        g.pose().scale(scale,scale,1f);
        int y=0;
        for(int i=0;i<slots.length;i++) {
            Entry e=slots[i]; if(e.id().isEmpty())continue;
            draw(g,mc,e,keys[i],0,y,width); y+=rowHeight;
        }
        g.pose().popPose();
    }
    private static void draw(GuiGraphics g,Minecraft mc,Entry e,String key,int x,int y,int width) {
        boolean active=e.state()==CombatCooldowns.ACTIVE;
        boolean ready=e.remaining()==0 && e.state()==CombatCooldowns.READY;
        int tint=active?0xffe9d29a:ready?color(e.id()):e.state()==CombatCooldowns.NO_RESOURCE?0xff9d6670:0xff85858c;
        for(int dy=0;dy<16;dy++) {
            int inset=dy==0||dy==15?5:dy==1||dy==14?3:dy==2||dy==13?2:dy==3||dy==12?1:0;
            g.fill(x+inset,y+dy,x+16-inset,y+dy+1,active?0xffc3a56a:0xff77727c);
            if(dy>0&&dy<15)g.fill(x+inset+1,y+dy,x+15-inset,y+dy+1,0xff252329);
        }
        String icon=icon(e.id());
        var texture=TearsOfInk.id("textures/gui/combat/"+icon+".png");
        if(mc.getResourceManager().getResource(texture).isPresent()) {
            g.setColor(((tint>>16)&255)/255f,((tint>>8)&255)/255f,(tint&255)/255f,1f);
            g.blit(texture,x+2,y+2,12,12,0f,0f,32,32,32,32);
            g.setColor(1f,1f,1f,1f);
        } else {
            String[] pixels=glyph(icon);
            for(int row=0;row<pixels.length;row++)for(int col=0;col<pixels[row].length();col++)
                if(pixels[row].charAt(col)=='#')g.fill(x+4+col,y+4+row,x+5+col,y+5+row,tint);
        }
        int barX=x+19, barW=Math.max(8,width-20);
        // Shorten long localized key names to the actual configured chord where possible.
        String display=key;
        if(mc.font.width(display)>barW && display.contains("+")) display="⇧+"+display.substring(display.lastIndexOf('+')+1);
        if(mc.font.width(display)>barW) display=mc.font.plainSubstrByWidth(display,barW);
        g.drawString(mc.font,display,barX,y,0xffded9df,true);
        g.fill(barX,y+10,barX+barW,y+15,0xff77727c);
        g.fill(barX+1,y+11,barX+barW-1,y+14,0xff18171c);
        float progress=active?1f:Math.max(0f,Math.min(1f,1f-e.remaining()/(float)e.total()));
        int filled=Math.round((barW-2)*progress);
        if(filled>0)g.fill(barX+1,y+11,barX+1+filled,y+14,tint);
    }
    private static int color(String id) {
        return id.equals("mighty_stride")||id.equals("shield_resonance")||id.equals("shield_ram")||id.equals("mercy")||id.equals("radiance")||id.equals("repel") ? 0xffe9d29a :
                id.startsWith("blood")||id.contains("dussak")||id.contains("talwar")?0xffe49090:0xffc6b6e2;
    }
    private static String icon(String id) {
        if(id.equals("loadout"))return "swap";
        if(id.equals("dash")||id.equals("mighty_stride"))return "boot";
        if(id.equals("shield_resonance"))return "resonance";
        if(id.equals("mercy"))return "heal";
        if(id.equals("radiance"))return "sun";
        if(id.equals("pale_guard"))return "guard";
        if(id.contains("parry"))return "parry";
        if(id.contains("wave")||id.equals("repel"))return "wave";
        if(id.contains("rush")||id.contains("charge")||id.equals("shield_ram"))return "rush";
        if(id.contains("blot")||id.contains("blood")||id.contains("talwar"))return "drop";
        if(id.contains("cleave")||id.contains("guillotine")||id.contains("execution"))return "heavy";
        if(id.equals("hilt"))return "hilt";
        if(id.contains("flourish"))return "parry";
        return "sword";
    }
    private static String[] glyph(String icon) {
        return switch(icon) {
            case "swap" -> new String[]{"....#...",".....#..","#######.",".....#..","..#.....",".#######","..#.....","...#...."};
            case "boot" -> new String[]{"..###...","..###...","..###...","..####..","..######","..######","........","........"};
            case "wave" -> new String[]{"......#.",".....##.","....###.","...###..","..###...","#####...","###.....","........"};
            case "parry" -> new String[]{"#......#",".#....#.","..#..#..","...##...","..####..",".#....#.","##....##","........"};
            case "guard" -> new String[]{".######.",".#....#.",".#....#.",".#....#.","..#..#..","...##...","........","........"};
            case "rush" -> new String[]{"....#...",".....#..","######..",".......#","######..",".....#..","....#...","........"};
            case "drop" -> new String[]{"...##...","...##...","..####..",".######.",".######.","..####..","...##...","........"};
            case "resonance","sun" -> new String[]{"...##...","#..##..#",".######.","########",".######.","#..##..#","...##...","........"};
            case "heal" -> new String[]{"...##...","...##...",".######.",".######.","...##...","...##...","........","........"};
            case "heavy" -> new String[]{"..####..","..####..","..####..","...##...","...##...",".######.","...##...","........"};
            case "hilt" -> new String[]{"......#.",".....#..","....#...","...#....",".####...",".##.....","#.......","........"};
            default -> new String[]{"......##",".....##.","....##..","...##...","..##....",".###....","##.#....","........"};
        };
    }
}
