package com.tarkrakov.tearsofink.client.battlefield;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.client.fog.BattlefieldClarity;
import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.world.ModBiomes;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * D-142: a calm upward counterpart to the Pale Forest's vanilla white ash.
 * Only runs client-side on the already server-confirmed battlefield and only
 * samples loaded chunks. No world modifications, packets or new mob spawns.
 */
public final class BattlefieldMemoryMotes {
    private static final int ATTEMPTS_PER_TICK = 6;
    private BattlefieldMemoryMotes() {}

    public static void tick(Minecraft mc) {
        var level = mc.level;
        var player = mc.player;
        if (level == null || player == null || mc.isPaused() || !ModDimensions.isLands(level))
            return;
        // Prepare the updraft on the VISIBLE side of the boundary even when
        // the player is still just outside; prevents an abrupt empty strip.
        if (!BattlefieldClarity.nearField(player.getX(), player.getZ(), 25d))
            return;

        RandomSource rng = level.random;
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int i = 0; i < ATTEMPTS_PER_TICK; i++) {
            double angle = rng.nextDouble() * (Math.PI * 2d);
            double radius = 2d + Math.sqrt(rng.nextDouble()) * 22d;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * radius);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * radius);
            float field = BattlefieldClarity.opennessAt(x + .5d, z + .5d);
            // The same smooth mask as falling white_ash, so their relative
            // densities cross-fade on ENTRY and EXIT rather than switching.
            if (field <= 0f || rng.nextFloat() >= field * .38f) continue;
            probe.set(x, player.getBlockY(), z);
            if (!level.hasChunkAt(probe)
                    || !level.getBiome(probe).is(ModBiomes.PALE_FOREST)) continue;
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(surfaceY - player.getY()) > 12d
                    || surfaceY <= level.getMinBuildHeight() + 1) continue;
            probe.set(x, surfaceY - 1, z);
            var floor = level.getBlockState(probe);
            if (!floor.isFaceSturdy(level, probe, Direction.UP)
                    || !floor.getFluidState().isEmpty()
                    || !(floor.is(ModBlocks.ASHEN_SOIL) || floor.is(ModBlocks.ASPHODELITE_ROCK)
                    || floor.is(ModBlocks.ASHEN_PATH) || floor.is(ModBlocks.ASH)
                    || floor.is(BlockTags.DIRT) || floor.is(BlockTags.BASE_STONE_OVERWORLD))) continue;
            probe.setY(surfaceY);
            if (!level.getBlockState(probe).isAir()) continue;
            double px = x + .16d + rng.nextDouble() * .68d;
            double pz = z + .16d + rng.nextDouble() * .68d;
            double py = surfaceY + .045d + rng.nextDouble() * .14d;
            level.addParticle(ModParticles.BATTLEFIELD_MEMORY_MOTE,
                    px, py, pz,
                    (rng.nextDouble() - .5d) * .012d,
                    .012d + rng.nextDouble() * .016d,
                    (rng.nextDouble() - .5d) * .012d);
        }
    }
}
