package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
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

    static void init() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!PantheonTemple.isTemple(level) || !POS.equals(hit.getBlockPos())
                    || !level.getBlockState(POS).is(ModBlocks.PANTHEON_ALTAR)
                    || player.isSpectator()) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel temple)
                    || !sp.isAlive()) return InteractionResult.FAIL;
            if (PantheonCombat.active()) {
                PantheonCombat.interact(sp, temple, sp.isShiftKeyDown() && sp.getMainHandItem().isEmpty());
                return InteractionResult.CONSUME;
            }
            if (!PantheonTemple.ready(temple) || PantheonArena.preparing()) {
                tell(sp, "busy");
                return InteractionResult.CONSUME;
            }

            var state = temple.getBlockState(POS);
            boolean occupied = state.getValue(PantheonAltarBlock.RELIC);
            var chunk = temple.getChunkAt(POS);
            var relicId = chunk.getAttached(RELIC_ID);
            PantheonTrial placed = !occupied ? null
                    : relicId == null ? PantheonTrial.legacyRelic() : PantheonTrial.lookup(relicId);
            ItemStack held = sp.getMainHandItem();

            if (!held.isEmpty()) {
                PantheonTrial heldTrial = PantheonTrial.forRelic(held);
                if (heldTrial == null) {
                    tell(sp, "empty_hand");
                } else if (!PantheonArena.neutralReady()) {
                    tell(sp, "active");
                } else if (occupied) {
                    tell(sp, "occupied");
                } else if (PantheonTemple.altarRelic(temple, state, true)) {
                    chunk.setAttached(RELIC_ID, heldTrial.id());
                    held.shrink(1);
                    sp.getInventory().setChanged();
                    tell(sp, "placed");
                }
                return InteractionResult.CONSUME;
            }

            if (sp.isShiftKeyDown()) {
                if (!PantheonArena.neutralReady()) {
                    if (PantheonArena.request(sp.createCommandSourceStack(), PantheonArenaScenes.Scene.NEUTRAL) != 0)
                        tell(sp, "reset");
                } else if (placed != null) {
                    int free = sp.getInventory().getFreeSlot();
                    if (free < 0) {
                        tell(sp, "inventory_full");
                    } else if (PantheonTemple.altarRelic(temple, state, false)) {
                        chunk.setAttached(RELIC_ID, null);
                        sp.getInventory().setItem(free, placed.relicStack());
                        sp.getInventory().setChanged();
                        tell(sp, "taken");
                    }
                } else tell(sp, "missing");
            } else if (!PantheonArena.neutralReady()) {
                tell(sp, "active");
            } else if (placed == null) {
                tell(sp, "missing");
            } else {
                PantheonCombat.start(sp, temple, placed);
            }
            return InteractionResult.CONSUME;
        });
    }

    private static void tell(ServerPlayer player, String id) {
        player.displayClientMessage(Component.translatable("message.tears-of-ink.pantheon.altar." + id), false);
    }
}
