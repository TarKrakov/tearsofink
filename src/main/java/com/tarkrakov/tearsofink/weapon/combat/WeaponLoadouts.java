package com.tarkrakov.tearsofink.weapon.combat;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.pantheon.PantheonTemple;
import com.tarkrakov.tearsofink.prologue.PersonalSlots;
import com.tarkrakov.tearsofink.skill.CombatCooldowns;
import com.tarkrakov.tearsofink.weapon.AbilityWeapon;
import com.tarkrakov.tearsofink.weapon.WeaponAbilities;
import com.tarkrakov.tearsofink.weapon.shield.ShieldStance;
import com.tarkrakov.tearsofink.world.ModDimensions;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;

import java.util.List;

public final class WeaponLoadouts {
    public static final int COOLDOWN = 40;
    public static final int MENU_START = PersonalSlots.INVENTORY_MENU_START + PersonalSlots.COUNT;
    public static final int MAIN_X = 125, OFF_X = 150, SLOT_Y = 173;
    private static final List<ItemStack> EMPTY = List.of(ItemStack.EMPTY, ItemStack.EMPTY);
    private static final AttachmentType<List<ItemStack>> RESERVE = AttachmentRegistry.<List<ItemStack>>builder()
            .persistent(ItemStack.OPTIONAL_CODEC.listOf(2, 2))
            .buildAndRegister(TearsOfInk.id("weapon_loadout"));
    private static final AttachmentType<Long> READY_AT = AttachmentRegistry.<Long>builder()
            .persistent(Codec.LONG).copyOnDeath()
            .buildAndRegister(TearsOfInk.id("weapon_loadout_ready_at"));

    private WeaponLoadouts() {}

    public static void init() {
        PayloadTypeRegistry.playC2S().register(Swap.TYPE, Swap.CODEC);
        PayloadTypeRegistry.playS2C().register(Swapped.TYPE, Swapped.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Swap.TYPE, (request, context) -> swap(context.player()));
        ServerPlayerEvents.COPY_FROM.register((previous, player, alive) -> {
            if (alive || PantheonTemple.isTemple(previous.level())
                    || previous.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)
                    || previous.isSpectator()) {
                List<ItemStack> reserve = previous.removeAttached(RESERVE);
                player.setAttached(RESERVE, reserve);
            }
        });
    }

    public record Swap() implements CustomPacketPayload {
        public static final Swap INSTANCE = new Swap();
        public static final Type<Swap> TYPE = new Type<>(TearsOfInk.id("swap_weapon_loadout"));
        public static final StreamCodec<ByteBuf, Swap> CODEC = StreamCodec.unit(INSTANCE);
        @Override public Type<Swap> type() { return TYPE; }
    }

    public record Swapped() implements CustomPacketPayload {
        public static final Swapped INSTANCE = new Swapped();
        public static final Type<Swapped> TYPE = new Type<>(TearsOfInk.id("weapon_loadout_swapped"));
        public static final StreamCodec<ByteBuf, Swapped> CODEC = StreamCodec.unit(INSTANCE);
        @Override public Type<Swapped> type() { return TYPE; }
    }

    public static boolean accepts(ItemStack stack) {
        return stack.isEmpty() || (stack.getCount() == 1 && stack.getItem() instanceof AbilityWeapon);
    }

    private static List<ItemStack> reserve(Player player) {
        return player.getAttachedOrElse(RESERVE, EMPTY);
    }

    private static void setReserve(Player player, ItemStack main, ItemStack off) {
        player.setAttached(RESERVE, main.isEmpty() && off.isEmpty() ? null : List.of(main, off));
    }

    private static int remaining(ServerPlayer player) {
        long now = player.server.overworld().getGameTime();
        return Math.clamp(player.getAttachedOrElse(READY_AT, now) - now, 0, COOLDOWN);
    }

    public static CombatCooldowns.Entry hud(ServerPlayer player) {
        int left = remaining(player);
        if (left == 0 && reserve(player).stream().allMatch(ItemStack::isEmpty)) return CombatCooldowns.Entry.EMPTY;
        boolean ready = accepts(player.getMainHandItem()) && accepts(player.getOffhandItem())
                && reserve(player).stream().allMatch(WeaponLoadouts::accepts)
                && reserve(player).stream().anyMatch(stack -> !stack.isEmpty());
        return CombatCooldowns.Entry.of("loadout", left, COOLDOWN,
                ready ? CombatCooldowns.READY : CombatCooldowns.BLOCKED);
    }

    private static void swap(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.isCreative() || player.isSleeping()
                || !ModDimensions.hasLandsRules(player.level()) || player.containerMenu != player.inventoryMenu
                || !player.inventoryMenu.getCarried().isEmpty() || remaining(player) > 0) return;

        List<ItemStack> reserve = reserve(player);
        if (reserve.stream().allMatch(ItemStack::isEmpty)) return;
        var inventory = player.getInventory();
        int selected = inventory.selected;
        if (selected < 0 || selected >= Inventory.getSelectionSize()) return;
        ItemStack main = inventory.getItem(selected);
        ItemStack off = player.getOffhandItem();
        if (!accepts(main) || !accepts(off) || !reserve.stream().allMatch(WeaponLoadouts::accepts)) {
            player.displayClientMessage(Component.translatable("message.tears-of-ink.loadout.invalid"), true);
            return;
        }

        CombatTransitions.begin(player, false);
        com.tarkrakov.tearsofink.weapon.ranged.RangedCombat.resetReload(player);
        setReserve(player, main, off);
        inventory.setItem(selected, reserve.get(0));
        inventory.setItem(Inventory.SLOT_OFFHAND, reserve.get(1));
        inventory.setChanged();
        ShieldStance.equipmentChanged(player);
        WeaponAbilities.MERCY.equipmentChanged(player);
        player.setAttached(READY_AT, player.server.overworld().getGameTime() + COOLDOWN);
        if (ServerPlayNetworking.canSend(player, Swapped.TYPE)) ServerPlayNetworking.send(player, Swapped.INSTANCE);
        player.inventoryMenu.broadcastChanges();
    }

    /** Runs with vanilla's inventory drop, after its keep-inventory and spectator checks. */
    public static void drop(ServerPlayer player) {
        List<ItemStack> stacks = player.removeAttached(RESERVE);
        if (stacks == null) return;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                player.drop(stack, true, false);
            }
        }
    }

    /** The two additional InventoryMenu slots use vanilla clicks and slot synchronization. */
    public record Items(Player owner) implements Container {
        @Override public int getContainerSize() { return 2; }
        @Override public boolean isEmpty() { return reserve(owner).stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack getItem(int index) { return reserve(owner).get(index); }
        @Override public ItemStack removeItem(int index, int amount) {
            return amount > 0 ? removeItemNoUpdate(index) : ItemStack.EMPTY;
        }
        @Override public ItemStack removeItemNoUpdate(int index) {
            ItemStack stack = getItem(index);
            setItem(index, ItemStack.EMPTY);
            return stack;
        }
        @Override public void setItem(int index, ItemStack stack) {
            List<ItemStack> stacks = reserve(owner);
            setReserve(owner, index == 0 ? stack : stacks.get(0), index == 1 ? stack : stacks.get(1));
        }
        @Override public void setChanged() {}
        @Override public boolean stillValid(Player player) { return player == owner; }
        @Override public boolean canPlaceItem(int index, ItemStack stack) { return PersonalSlots.fullPanel(owner) && accepts(stack); }
        @Override public void clearContent() { owner.removeAttached(RESERVE); }
    }
}
