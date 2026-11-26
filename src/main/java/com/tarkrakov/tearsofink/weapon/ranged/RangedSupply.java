package com.tarkrakov.tearsofink.weapon.ranged;

import com.tarkrakov.tearsofink.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import java.util.List;

/** Count is independent of durability; empty containers and daggers are refillable. */
public final class RangedSupply extends Item {
    private static final String COUNT = "toi_ranged_charges";
    public final int max;
    public RangedSupply(int max) { super(new Properties().stacksTo(1)); this.max=max; }
    public static int capacity(ItemStack s) {
        if (s.getItem() instanceof RangedSupply supply) return supply.max;
        return RangedItem.kind(s)==RangedItem.Kind.DAGGER ? KnifeTuning.CAPACITY : 0;
    }
    public static int amount(ItemStack s) {
        int max=capacity(s);
        if (max==0) return 0;
        CustomData d=s.get(DataComponents.CUSTOM_DATA);
        if (d==null || !d.copyTag().contains(COUNT)) return RangedItem.kind(s)==RangedItem.Kind.DAGGER ? max : 0;
        return Math.clamp(d.copyTag().getInt(COUNT),0,max);
    }
    public static void set(ItemStack s,int n) {
        int clamped=Math.clamp(n,0,capacity(s));
        CustomData.update(DataComponents.CUSTOM_DATA,s,t->t.putInt(COUNT,clamped));
    }
    public static Item supply(RangedItem.Kind kind) {
        return kind==RangedItem.Kind.CROSSBOW ? ModItems.INK_QUIVER : ModItems.HOLY_BANDOLIER;
    }
    public static int available(Player p,RangedItem.Kind kind) {
        int total=0;
        for(int i=0;i<36;i++) { ItemStack s=p.getInventory().getItem(i); if(s.is(supply(kind))) total+=amount(s); }
        return total;
    }
    public static boolean take(Player p,RangedItem.Kind kind,int count) {
        if(p.getAbilities().instabuild) return true;
        if(available(p,kind)<count) return false;
        for(int i=0;i<36 && count>0;i++) {
            ItemStack s=p.getInventory().getItem(i);
            if(!s.is(supply(kind))) continue;
            int use=Math.min(count,amount(s));
            if(use>0){ set(s,amount(s)-use); count-=use; }
        }
        p.getInventory().setChanged();
        return true;
    }
    public static final int FULL_REFILL_ITEMS=12;
    public record Refill(ItemStack result,int[] consume) {}
    /** Stacked ammo tops up proportionally; stacked blood ingots add a fixed number of knife charges. */
    public static Refill refillPlan(Container grid) {
        int size=grid.getContainerSize(),receiverSlot=-1,units=0;
        ItemStack receiver=ItemStack.EMPTY;
        Item material=null;
        for(int i=0;i<size;i++){
            ItemStack s=grid.getItem(i);
            if(s.isEmpty())continue;
            if(capacity(s)>0){
                if(!receiver.isEmpty()||s.getCount()!=1)return null;
                receiver=s;receiverSlot=i;
            }else{
                if(material!=null&&material!=s.getItem())return null;
                material=s.getItem();units+=s.getCount();
            }
        }
        if(receiver.isEmpty()||units==0||amount(receiver)>=capacity(receiver))return null;
        boolean ammo=receiver.is(ModItems.INK_QUIVER)&&material==Items.ARROW
            ||receiver.is(ModItems.HOLY_BANDOLIER)&&material==ModItems.PALE_BULLET;
        boolean dagger=RangedItem.kind(receiver)==RangedItem.Kind.DAGGER&&material==ModItems.BLOOD_INGOT;
        if(!ammo&&!dagger)return null;
        int max=capacity(receiver),missing=max-amount(receiver);
        int needed=ammo?(missing*FULL_REFILL_ITEMS+max-1)/max:
            Math.min(units,(missing+KnifeTuning.REFILL_PER_INGOT-1)/KnifeTuning.REFILL_PER_INGOT);
        if(units<needed)return null;
        int[] consume=new int[size];consume[receiverSlot]=1;
        int left=needed;
        for(int i=0;i<size&&left>0;i++){
            if(i==receiverSlot||grid.getItem(i).isEmpty())continue;
            int n=Math.min(left,grid.getItem(i).getCount());
            consume[i]=n;left-=n;
        }
        if(left!=0)return null;
        ItemStack out=receiver.copyWithCount(1);
        set(out,ammo?max:amount(out)+KnifeTuning.REFILL_PER_INGOT*needed);
        return new Refill(out,consume);
    }
    public static ItemStack refill(Container grid){
        Refill plan=refillPlan(grid);
        return plan==null?ItemStack.EMPTY:plan.result();
    }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13f*amount(stack)/max); }
    @Override public int getBarColor(ItemStack stack) { return 0x9caa70; }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.translatable("ranged.tears-of-ink.charges",amount(stack),max));
        int needed=((max-amount(stack))*FULL_REFILL_ITEMS+max-1)/max;
        tooltip.add(Component.translatable(stack.is(ModItems.INK_QUIVER)?"ranged.tears-of-ink.refill_arrows":"ranged.tears-of-ink.refill_bullets",needed));
    }
}
