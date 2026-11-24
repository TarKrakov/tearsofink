package com.tarkrakov.tearsofink.compat.jaber;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.frog.Frog;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;

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
        return level instanceof ServerLevel && ModDimensions.hasLandsRules(level);
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

    public static void init() {
        if (!supported()) return;
        TearsOfInk.LOGGER.info("JABER 1.0.0: Ink Lands road router enabled");
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (active(level) && entity instanceof Frog frog && frog.getTags().stream().anyMatch(t -> t.startsWith("jaber_owner_"))) JaberRouter.forget(frog);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> JaberRouter.reset());
        final ResourceLocation call = ResourceLocation.fromNamespaceAndPath(MOD_ID, "frog_call");
        UseItemCallback.EVENT.register((player, level, hand) -> {
            var stack = player.getItemInHand(hand);
            if (!active(level) || !call.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                return InteractionResultHolder.pass(stack);
            }
            if (!(player instanceof ServerPlayer rider) || player.isSpectator()) return InteractionResultHolder.fail(stack);
            return JaberSummon.call(rider) ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
        });
    }
}
