package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.item.ModItems;
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

/** First repeatable non-combat trial. All transitions use the existing scene journal. */
public final class PantheonAltar {
    static final BlockPos POS = PantheonTemple.world(new BlockPos(62, 1, 11));
    private static final BlockPos CHEST_A = PantheonTemple.world(new BlockPos(62, 1, 13));
    private static final BlockPos CHEST_B = PantheonTemple.world(new BlockPos(62, 1, 14));
    private PantheonAltar() {}

    public static boolean isRelic(ItemStack stack) {
        return stack.is(ModItems.PANTHEON_TEST_RELIC);
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
            if (!PantheonTemple.ready(temple) || PantheonArena.preparing()) {
                tell(sp, "busy");
                return InteractionResult.CONSUME;
            }

            var state = temple.getBlockState(POS);
            boolean occupied = state.getValue(PantheonAltarBlock.RELIC);
            ItemStack held = sp.getMainHandItem();

            if (!held.isEmpty()) {
                if (!isRelic(held) || !ItemStack.isSameItemSameComponents(held, new ItemStack(ModItems.PANTHEON_TEST_RELIC))) {
                    tell(sp, "empty_hand");
                } else if (!PantheonArena.neutralReady()) {
                    tell(sp, "active");
                } else if (occupied) {
                    tell(sp, "occupied");
                } else if (PantheonTemple.altarRelic(temple, state, true)) {
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
                } else if (occupied) {
                    int free = sp.getInventory().getFreeSlot();
                    if (free < 0) {
                        tell(sp, "inventory_full");
                    } else if (PantheonTemple.altarRelic(temple, state, false)) {
                        sp.getInventory().setItem(free, new ItemStack(ModItems.PANTHEON_TEST_RELIC));
                        sp.getInventory().setChanged();
                        tell(sp, "taken");
                    }
                } else tell(sp, "missing");
            } else if (!PantheonArena.neutralReady()) {
                tell(sp, "active");
            } else if (!occupied) {
                tell(sp, "missing");
            } else if (PantheonArena.request(sp.createCommandSourceStack(), PantheonArenaScenes.Scene.FOREST) != 0) {
                tell(sp, "started");
            }
            return InteractionResult.CONSUME;
        });
    }

    private static void tell(ServerPlayer player, String id) {
        player.displayClientMessage(Component.translatable("message.tears-of-ink.pantheon.altar." + id), false);
    }
}
