package com.tarkrakov.tearsofink.compat.jaber;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Optional compatibility with the inspected JABER 1.0.0 binary; no dependency on its Java classes. */
public final class JaberCompat {
    private static final String MOD_ID = "chaos_duck_jaber";

    private JaberCompat() {}

    public static boolean supported() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(mod -> "1.0.0".equals(mod.getMetadata().getVersion().getFriendlyString()))
                .orElse(false);
    }

    public static boolean active(Level level) {
        return level instanceof ServerLevel && ModDimensions.isLands(level);
    }

    /** Avoid the fog loop; this is the same noisy edge as InkLandsBorder, not a square/radial substitute. */
    public static boolean inside(ServerLevel level, int x, int z) {
        // The noise amplitude is bounded around the radius. Only evaluate it near the edge.
        double depth = InkLandsLayout.RADIUS - Math.hypot(x + 0.5, z + 0.5);
        return level.getWorldBorder().isWithinBounds(new BlockPos(x, level.getMinBuildHeight(), z))
                && (depth > InkLandsLayout.EDGE_WOBBLE * 2.0
                    || InkLandsLayout.edgeDepth(x + 0.5, z + 0.5) > 0);
    }

    /** Do not getChunk(), getHeight() or getBlockState() on a not-yet-loaded column. */
    public static boolean columnReady(Level level, int x, int z) {
        if (!(level instanceof ServerLevel server)) return false;
        return inside(server, x, z) && server.getChunkSource().getChunkNow(x >> 4, z >> 4) != null;
    }

    public static boolean readable(Level level, int x, int y, int z) {
        return y >= level.getMinBuildHeight() && y < level.getMaxBuildHeight()
                && columnReady(level, x, z);
    }

    /** Collision shapes can consult adjacent blocks, especially fences and walls. */
    public static boolean shapeReady(Level level, int x, int y, int z) {
        if (!readable(level, x, y, z)) return false;
        ServerLevel server = (ServerLevel) level;
        for (int cx = (x - 1) >> 4; cx <= (x + 1) >> 4; cx++) {
            for (int cz = (z - 1) >> 4; cz <= (z + 1) >> 4; cz++) {
                if (server.getChunkSource().getChunkNow(cx, cz) == null) return false;
            }
        }
        return true;
    }

    /** Actual supporting block, not a heightmap or hypothetical road. A missing
     * deck is never passable just because the generator has a road at that X/Z. */
    public static double offRoadCost(Level level, int x, int y, int z, boolean swimming) {
        if (swimming || !readable(level, x, y - 1, z)) return 0.0;
        var floor = level.getBlockState(new BlockPos(x, y - 1, z));
        if (floor.is(ModBlocks.ASHEN_PATH)) return 0.0;
        // The road generator uses pale planks for the deck. Treat actual planked
        // floors as comfortable too, including player-built bridges.
        if (floor.is(ModBlocks.PALE_PLANKS)) return 0.0;
        return 1.4;
    }

    public static void init() {
        if (!supported()) return;
        TearsOfInk.LOGGER.info("JABER 1.0.0: Ink Lands compatibility enabled");
        // JABER's call item places the frog at exactly player.x + 4, player.y, player.z.
        // It does not look for free space. Reject unsafe calls before it creates/repositions the frog.
        final ResourceLocation call = ResourceLocation.fromNamespaceAndPath(MOD_ID, "frog_call");
        UseItemCallback.EVENT.register((player, level, hand) -> {
            var stack = player.getItemInHand(hand);
            if (!active(level) || !call.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                return InteractionResultHolder.pass(stack);
            }
            if (player.isSpectator()) return InteractionResultHolder.fail(stack);
            ServerLevel server = (ServerLevel) level;
            double x = player.getX() + 4.0, y = player.getY(), z = player.getZ();
            // Allow room for the temporarily scale-3 frog and its rider. Check loaded chunks before collision.
            AABB space = new AABB(x - 0.8, y, z - 0.8, x + 0.8, y + 3.0, z + 0.8);
            boolean safe = y >= server.getMinBuildHeight() + 1 && y + 3.0 < server.getMaxBuildHeight();
            for (int bx = Mth.floor(space.minX) - 1; safe && bx <= Mth.floor(space.maxX) + 1; bx++) {
                for (int bz = Mth.floor(space.minZ) - 1; safe && bz <= Mth.floor(space.maxZ) + 1; bz++) {
                    safe = columnReady(server, bx, bz);
                }
            }
            if (safe) {
                BlockPos feet = BlockPos.containing(x, y, z);
                BlockPos below = feet.below();
                var fluid = server.getFluidState(feet);
                var support = server.getBlockState(below);
                boolean water = fluid.is(net.minecraft.tags.FluidTags.WATER);
                safe = !fluid.is(net.minecraft.tags.FluidTags.LAVA)
                        && (water || support.isFaceSturdy(server, below, net.minecraft.core.Direction.UP))
                        && server.noCollision(null, space);
            }
            if (safe) return InteractionResultHolder.pass(stack);
            player.displayClientMessage(Component.translatable("message.tears-of-ink.jaber.unsafe_call"), true);
            return InteractionResultHolder.fail(stack);
        });
    }
}
