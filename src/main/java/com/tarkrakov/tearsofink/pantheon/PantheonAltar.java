package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleMenuProvider;
import com.tarkrakov.tearsofink.mixin.PantheonCompoundContainerAccessor;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** The existing altar and filtered reliquary share the registered trial relics. */
public final class PantheonAltar {
    static final BlockPos POS = PantheonTemple.world(new BlockPos(62, 1, 11));
    private static final BlockPos CHEST_A = PantheonTemple.world(new BlockPos(62, 1, 13));
    private static final BlockPos CHEST_B = PantheonTemple.world(new BlockPos(62, 1, 14));
    private static final AttachmentType<ResourceLocation> RELIC_ID = AttachmentRegistry.<ResourceLocation>builder()
            .persistent(ResourceLocation.CODEC).buildAndRegister(TearsOfInk.id("pantheon_altar_relic"));

    public static final MenuType<PantheonAltarMenu> MENU = Registry.register(BuiltInRegistries.MENU,
            TearsOfInk.id("pantheon_altar"), new MenuType<>(PantheonAltarMenu::new, FeatureFlags.VANILLA_SET));

    private PantheonAltar() {}

    public static boolean isRelic(ItemStack stack) {
        return PantheonTrial.forRelic(stack) != null;
    }
    public static boolean isRelicChest(Container container) {
        if (container instanceof ChestBlockEntity chest)
            return chest.getLevel() != null && PantheonTemple.isTemple(chest.getLevel())
                    && (CHEST_A.equals(chest.getBlockPos()) || CHEST_B.equals(chest.getBlockPos()));
        if (container instanceof CompoundContainer doubleChest) {
            PantheonCompoundContainerAccessor parts = (PantheonCompoundContainerAccessor) doubleChest;
            return isRelicChest(parts.tearsofink$first()) || isRelicChest(parts.tearsofink$second());
        }
        return false;
    }

    /** Legacy occupied block with no ID still contains the original starter relic. */
    static PantheonTrial placed(ServerLevel level) {
        var state = level.getBlockState(POS);
        if (!state.is(ModBlocks.PANTHEON_ALTAR) || !state.getValue(PantheonAltarBlock.RELIC)) return null;
        var id = level.getChunkAt(POS).getAttached(RELIC_ID);
        return id == null ? PantheonTrial.legacyRelic() : PantheonTrial.lookup(id);
    }

    static boolean editable(ServerLevel level) {
        if (!PantheonTemple.ready(level) || PantheonCombat.active() || !PantheonArena.neutralReady()) return false;
        var state = level.getBlockState(POS);
        return state.is(ModBlocks.PANTHEON_ALTAR)
                && (!state.getValue(PantheonAltarBlock.RELIC) || placed(level) != null);
    }

    /** Read/write the existing canonical chunk attachment, never a per-menu inventory copy. */
    static boolean setRelic(ServerLevel level, ItemStack stack) {
        if (!editable(level) || (!stack.isEmpty() && (stack.getCount() != 1 || !isRelic(stack)))) return false;
        PantheonTrial next = stack.isEmpty() ? null : PantheonTrial.forRelic(stack);
        var state = level.getBlockState(POS);
        boolean occupied = next != null;
        if (state.getValue(PantheonAltarBlock.RELIC) != occupied
                && !PantheonTemple.altarRelic(level, state, occupied)) return false;
        level.getChunkAt(POS).setAttached(RELIC_ID, next == null ? null : next.id());
        return true;
    }

    static void init() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!PantheonTemple.isTemple(level) || !POS.equals(hit.getBlockPos())
                    || !level.getBlockState(POS).is(ModBlocks.PANTHEON_ALTAR)
                    || player.isSpectator()) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel temple)
                    || !sp.isAlive()) return InteractionResult.FAIL;
            sp.openMenu(new SimpleMenuProvider((id, inventory, owner) ->
                    new PantheonAltarMenu(id, inventory, temple),
                    Component.translatable("block.tears-of-ink.pantheon_altar")));
            return InteractionResult.CONSUME;
        });
    }
}
