package com.tarkrakov.tearsofink.prologue;

import com.tarkrakov.tearsofink.world.ModDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Reusable item type for future authored letters, notes and world relics.
 * Right-click consumes one physical copy and unlocks a persistent diary page.
 * There is intentionally no new collectible ID or placeholder art in this slice.
 */
public class DiaryCollectibleItem extends Item {
    private final String page;

    public DiaryCollectibleItem(String page, Properties properties) {
        super(properties);
        if (!page.startsWith("collection_")) throw new IllegalArgumentException("Not a collection page: " + page);
        this.page = page;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!ModDimensions.isLands(level)) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer sp && !sp.isSpectator()) {
            // Persist first; consume only one copy when the server accepted the use.
            DiaryEntries.unlock(sp, page);
            stack.consume(1, sp);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
