package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Registered relic, scene, spawn and target lifecycle for a single trial. */
record PantheonTrial(ResourceLocation id, Supplier<Item> relic, PantheonArenaScenes.Scene scene,
                     BlockPos spawn, Function<ServerLevel, Mob> create, Consumer<Mob> activate,
                     Consumer<UUID> defeated, Consumer<Mob> cleanup) {
    private static final PantheonTrial FOREST = new PantheonTrial(TearsOfInk.id("forest_trial"),
            () -> ModItems.PANTHEON_TEST_RELIC, PantheonArenaScenes.Scene.FOREST,
            PantheonTemple.world(new BlockPos(58, 2, 86)),
            level -> {
                Mob target = EntityType.VINDICATOR.create(level);
                if (target != null) target.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
                return target;
            }, target -> target.setNoAi(false), id -> {}, Mob::discard);
    private static final List<PantheonTrial> REGISTERED = List.of(FOREST);

    static PantheonTrial forRelic(ItemStack stack) {
        for (PantheonTrial trial : REGISTERED) {
            if (ItemStack.isSameItemSameComponents(stack, new ItemStack(trial.relic().get()))) return trial;
        }
        return null;
    }

    static PantheonTrial lookup(ResourceLocation id) {
        for (PantheonTrial trial : REGISTERED) {
            if (trial.id().equals(id)) return trial;
        }
        return null;
    }

    static PantheonTrial legacyRelic() { return FOREST; }

    ItemStack relicStack() { return new ItemStack(relic.get()); }
}
