package com.tarkrakov.tearsofink.world.battlefield;

import com.tarkrakov.tearsofink.block.BattlefieldRelicPatchBlock;
import com.tarkrakov.tearsofink.block.BattlefieldRelicPatchBlockEntity;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.Arrays;

/** Small terrain-following relics; no block destruction, no per-weapon entity. */
public final class BattlefieldRelicPlacement {
    private BattlefieldRelicPlacement() {}
    public static boolean place(ServerLevel level, BlockPos target, Direction facing, int style, int variant) {
        var layout = InkLandsLayout.of(level);
        Direction side = facing.getClockWise();
        for (int radius = 0; radius <= 4; radius++) for (int f = -radius; f <= radius; f++)
            for (int r = -radius; r <= radius; r++) {
                if (Math.max(Math.abs(f), Math.abs(r)) != radius) continue;
                BlockPos xz = target.relative(facing, f).relative(side, r);
                if (xz.getX() >> 4 != target.getX() >> 4 || xz.getZ() >> 4 != target.getZ() >> 4)
                    continue;
                if (level.getChunkSource().getChunkNow(xz.getX() >> 4, xz.getZ() >> 4) == null)
                    continue;
                int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz.getX(), xz.getZ());
                BlockPos base = new BlockPos(xz.getX(), groundY, xz.getZ());
                if (!level.getBlockState(base).isAir()
                        || !level.getWorldBorder().isWithinBounds(base)
                        || layout.roadNear(base.getX(), base.getZ(), 10) != null) continue;
                var earth = level.getBlockState(base.below());
                if (!earth.isFaceSturdy(level, base.below(), Direction.UP)
                        || !(earth.is(ModBlocks.ASHEN_SOIL) || earth.is(ModBlocks.ASPHODELITE_ROCK)
                        || earth.is(ModBlocks.ASHEN_PATH) || earth.is(ModBlocks.ASH)
                        || earth.is(BlockTags.DIRT) || earth.is(BlockTags.BASE_STONE_OVERWORLD))) continue;
                var poses = BattlefieldRelicRecipes.generate(base, style, variant);
                int[] heights = new int[poses.size()];
                Arrays.fill(heights, BattlefieldRelicPatchBlockEntity.MISSING);
                int good = 0;
                for (int i = 0; i < poses.size(); i++) {
                    var pose = poses.get(i);
                    BlockPos at = base.relative(facing, Math.round(pose.forward()))
                            .relative(side, Math.round(pose.right()));
                    if (level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4) == null) continue;
                    int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
                    if (Math.abs(h - groundY) > 6) continue;
                    BlockPos surface = new BlockPos(at.getX(), h, at.getZ());
                    var below = level.getBlockState(surface.below());
                    if (!level.getBlockState(surface).isAir()
                            || !level.getBlockState(surface.above()).getCollisionShape(level, surface.above()).isEmpty()
                            || !below.isFaceSturdy(level, surface.below(), Direction.UP)
                            || !(below.is(ModBlocks.ASHEN_SOIL) || below.is(ModBlocks.ASPHODELITE_ROCK)
                            || below.is(ModBlocks.ASHEN_PATH) || below.is(ModBlocks.ASH)
                            || below.is(BlockTags.DIRT) || below.is(BlockTags.BASE_STONE_OVERWORLD))) continue;
                    if (style >= 8 && !barePoleFits(level, at, h, facing, pose)) continue;
                    heights[i] = h - groundY; good++;
                }
                // Buried standard patches have just 2..3 author-made meshes;
                // preserve the former 4+ minimum for all old weapon/bolt beds.
                if (good < (style >= 8 ? 2 : Math.max(4, poses.size() / 2))) continue;
                var state = ModBlocks.BATTLEFIELD_RELIC_PATCH.defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, facing)
                        .setValue(BattlefieldRelicPatchBlock.STYLE, style)
                        .setValue(BattlefieldRelicPatchBlock.VARIANT, variant);
                if (!level.setBlock(base, state, Block.UPDATE_CLIENTS)) continue;
                if (level.getBlockEntity(base) instanceof BattlefieldRelicPatchBlockEntity entity) {
                    entity.configure(heights);
                    level.sendBlockUpdated(base, state, state, Block.UPDATE_CLIENTS);
                    return true;
                }
                level.setBlock(base, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
            }
        return false;
    }

    /**
     * No forced neighbor loads and no collision removal. Sample the exposed
     * tilted pole along its true Y -> X/Z direction after Y/Z/X rotations.
     * The buried section may intersect natural earth; above ground cannot
     * intersect solid terrain or players' builds.
     */
    private static boolean barePoleFits(ServerLevel level, BlockPos planted, int surfaceY,
                                        Direction facing, BattlefieldRelicRecipes.Relic pose) {
        double pitch = Math.toRadians(pose.pitch()), roll = Math.toRadians(pose.roll());
        double yaw = Math.toRadians(pose.yaw());
        double outerYaw = Math.toRadians(180f - facing.toYRot());
        double sinP = Math.sin(pitch), cosP = Math.cos(pitch);
        double sinR = Math.sin(roll), cosR = Math.cos(roll);
        double sx = -cosP * sinR, sy = cosP * cosR, sz = sinP;
        double lx = sx * Math.cos(yaw) + sz * Math.sin(yaw);
        double lz = -sx * Math.sin(yaw) + sz * Math.cos(yaw);
        double dx = lx * Math.cos(outerYaw) + lz * Math.sin(outerYaw);
        double dz = -lx * Math.sin(outerYaw) + lz * Math.cos(outerYaw);
        for (int sample = 1; sample <= 14; sample++) {
            double t = sample * .47d;
            double y = surfaceY + pose.sink() + sy * t;
            if (y < surfaceY + .1d) continue; // deliberately sunk in soil
            BlockPos column = BlockPos.containing(planted.getX() + .5d + dx * t, y,
                    planted.getZ() + .5d + dz * t);
            if (level.getChunkSource().getChunkNow(column.getX() >> 4, column.getZ() >> 4) == null
                    || !level.getWorldBorder().isWithinBounds(column)
                    || !level.getBlockState(column).getCollisionShape(level, column).isEmpty())
                return false;
        }
        return true;
    }
}
