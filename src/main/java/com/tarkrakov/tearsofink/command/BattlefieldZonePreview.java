package com.tarkrakov.tearsofink.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tarkrakov.tearsofink.block.BattlefieldSceneBlock;
import com.tarkrakov.tearsofink.block.BattlefieldSceneBlockEntity;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Pose;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Prop;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldPosedScenes.Scenario;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldWeaponCatalog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * D-123 visual composition preview: a single inert anchor renders arbitrary
 * ground-pivot rotations of existing baked, authored weapon models. The actual
 * two animated banners still use the approved original banner block.
 *
 * Important: NO worldgen, no server tick, no mass prop BlockEntities, no
 * destruction of terrain, no placement on roads or buildings. Failed required
 * anchors reject the WHOLE scene before world writes. These are explicitly
 * hand-requested technical test recipes, not an approved final battlefield.
 */
public final class BattlefieldZonePreview {
    private BattlefieldZonePreview() {}

    static LiteralArgumentBuilder<CommandSourceStack> literal() {
        var root = Commands.literal("zone");
        for (Scenario scene : Scenario.values()) {
            root.then(Commands.literal(scene.id())
                    .executes(ctx -> preview(ctx, scene, -1))
                    .then(Commands.argument("variant", IntegerArgumentType.integer(0, 7))
                            .executes(ctx -> preview(ctx, scene,
                                    IntegerArgumentType.getInteger(ctx, "variant")))));
        }
        return root;
    }

    private record Flag(BlockPos pos, BlockState state, Set<BlockPos> envelope) {}
    private record Layout(BlockPos root, BlockState state, int[] heights, List<Flag> flags, int visibleProps) {}

    private static int preview(CommandContext<CommandSourceStack> context, Scenario scene, int requestedVariant)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Direction forward = player.getDirection();
        Direction right = forward.getClockWise();
        BlockPos target = player.blockPosition().relative(forward, 8);

        // Small local, non-destructive operator search. Terrain is sampled
        // separately per prop; no "flat empty square" is required.
        for (int radius = 0; radius <= 6; radius++) {
            for (int f = -radius; f <= radius; f++) {
                for (int s = -radius; s <= radius; s++) {
                    if (Math.max(Math.abs(f), Math.abs(s)) != radius) continue;
                    BlockPos column = target.relative(forward, f).relative(right, s);
                    if (!validColumn(level, column)) continue;
                    int y = surfaceY(level, column);
                    if (Math.abs(y - player.getBlockY()) > 5) continue;
                    BlockPos root = new BlockPos(column.getX(), y, column.getZ());
                    if (!freeSurface(level, root) || !naturalSupport(level, root)) continue;

                    long seed = level.getSeed()
                            ^ ((long) root.getX() * 341873128712L)
                            ^ ((long) root.getZ() * 132897987541L) ^ scene.ordinal();
                    int variant = requestedVariant >= 0 ? requestedVariant
                            : (int) ((seed ^ seed >>> 29) & 7L);
                    List<Pose> poses = BattlefieldPosedScenes.plan(scene, variant);

                    // First retain the player's orientation, then try other
                    // axes to find a composition that fits without tree felling.
                    for (int turn = 0; turn < 4; turn++) {
                        Direction axis = forward;
                        for (int j = 0; j < turn; j++) axis = axis.getClockWise();
                        Layout layout = plan(level, root, axis, scene, variant, poses);
                        if (layout == null) continue;
                        int placed = commit(level, layout);
                        if (placed == 0) continue;
                        int count = placed;
                        BlockPos at = root;
                        context.getSource().sendSuccess(
                                () -> Component.literal("Пробная сцена «" + scene.label() + "»: "
                                        + count + " деталей у " + at.toShortString()
                                        + ". Связанные предметы имеют разные силуэты и 3D-позы. "
                                        + "Это не генерация поля."), false);
                        return placed;
                    }
                }
            }
        }
        context.getSource().sendFailure(Component.literal(
                "Нет безопасного места для ВСЕХ обязательных элементов сцены «"
                        + scene.label() + "». Рельеф и постройки не изменены."));
        return 0;
    }

    private static Layout plan(ServerLevel level, BlockPos root, Direction axis,
                               Scenario scene, int variant, List<Pose> poses) {
        int[] heights = new int[poses.size()];
        Arrays.fill(heights, BattlefieldSceneBlockEntity.MISSING);
        List<Flag> flags = new ArrayList<>();
        Set<BlockPos> reserved = new HashSet<>();
        reserved.add(root);

        // A scene is committed only when all essential parts fit. Optional
        // details can be absent without changing the underlying story.
        for (int pass = 0; pass < 2; pass++) {
            boolean mandatory = pass == 0;
            for (int i = 0; i < poses.size(); i++) {
                Pose pose = poses.get(i);
                if (pose.required() != mandatory) continue;
                if (pose.banner()) {
                    Flag flag = findFlag(level, root, axis, pose, reserved);
                    if (flag == null) {
                        if (mandatory) return null;
                        continue;
                    }
                    flags.add(flag);
                    reserved.addAll(flag.envelope());
                } else {
                    BlockPos column = footprintColumn(root, axis, pose);
                    if (!validColumn(level, column)) {
                        if (mandatory) return null;
                        continue;
                    }
                    int height = surfaceY(level, column);
                    BlockPos surface = new BlockPos(column.getX(), height, column.getZ());
                    if (Math.abs(height - root.getY()) > 3 || !freeSurface(level, surface)
                            || !naturalSupport(level, surface)
                            || !modelFits(level, root, pose, axis, height)) {
                        if (mandatory) return null;
                        continue;
                    }
                    heights[i] = height - root.getY();
                }
            }
        }

        BlockState state = ModBlocks.BATTLEFIELD_SCENE.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, axis)
                .setValue(BattlefieldSceneBlock.SCENE, scene.ordinal())
                .setValue(BattlefieldSceneBlock.VARIANT, variant)
                .setValue(BattlefieldSceneBlock.LAYOUT, 1);
        int rendered = (int) Arrays.stream(heights).filter(h -> h != BattlefieldSceneBlockEntity.MISSING).count();
        return new Layout(root, state, heights, flags, rendered);
    }

    private static Flag findFlag(ServerLevel level, BlockPos root, Direction axis,
                                 Pose pose, Set<BlockPos> reserved) {
        BlockPos center = footprintColumn(root, axis, pose);
        Direction side = axis.getClockWise();
        int[][] shifts = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        Direction facing = axis.getOpposite();

        for (int[] shift : shifts) {
            BlockPos column = center.relative(axis, shift[0]).relative(side, shift[1]);
            if (!validColumn(level, column)) continue;
            int y = surfaceY(level, column);
            BlockPos base = new BlockPos(column.getX(), y, column.getZ());
            if (Math.abs(y - root.getY()) > 3 || !naturalSupport(level, base)
                    || !BattlefieldDecorCommands.canPlaceBanner(level, base, facing)) continue;
            Set<BlockPos> envelope = bannerEnvelope(base, facing);
            boolean overlap = false;
            for (BlockPos position : envelope) {
                if (reserved.contains(position)) { overlap = true; break; }
            }
            if (overlap) continue;
            return new Flag(base, ModBlocks.BATTLEFIELD_BANNER.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, facing), envelope);
        }
        return null;
    }

    private static Set<BlockPos> bannerEnvelope(BlockPos base, Direction facing) {
        Set<BlockPos> cells = new HashSet<>();
        for (int y = 0; y <= 3; y++) cells.add(base.above(y));
        Direction clothSide = facing.getCounterClockWise();
        for (int x = 1; x <= 2; x++) {
            for (int y = 2; y <= 3; y++) cells.add(base.relative(clothSide, x).above(y));
        }
        return cells;
    }

    private static BlockPos footprintColumn(BlockPos root, Direction forward, Pose pose) {
        Direction right = forward.getClockWise();
        int x = Math.round(root.getX() + pose.forward() * forward.getStepX()
                + pose.right() * right.getStepX());
        int z = Math.round(root.getZ() + pose.forward() * forward.getStepZ()
                + pose.right() * right.getStepZ());
        return new BlockPos(x, root.getY(), z);
    }

    private static boolean validColumn(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos);
    }

    private static int surfaceY(ServerLevel level, BlockPos column) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                column.getX(), column.getZ());
    }

    private static boolean freeSurface(ServerLevel level, BlockPos base) {
        return validColumn(level, base) && level.getBlockState(base).isAir();
    }

    private static boolean naturalSupport(ServerLevel level, BlockPos base) {
        BlockPos below = base.below();
        if (!validColumn(level, below)) return false;
        BlockState ground = level.getBlockState(below);
        return ground.isFaceSturdy(level, below, Direction.UP)
                && (ground.is(BlockTags.DIRT) || ground.is(BlockTags.BASE_STONE_OVERWORLD)
                || ground.is(Blocks.GRAVEL) || ground.is(Blocks.SAND)
                || ground.is(Blocks.RED_SAND) || ground.is(Blocks.CLAY)
                || ground.is(ModBlocks.ASHEN_SOIL) || ground.is(ModBlocks.BLOOD_SOIL)
                || ground.is(ModBlocks.TAR_SOIL) || ground.is(ModBlocks.ASPHODELITE_ROCK)
                || ground.is(ModBlocks.ASH));
    }

    /**
     * Conservative transform of the authored model's own visible dimensions,
     * not a 7x7 clearing. The values account for D-113 shield X=180 plus
     * its 1-block raised anchor. Check only exposed parts above the local
     * support: intentional partial burial and rough natural ground are okay.
     */
    private static boolean modelFits(ServerLevel level, BlockPos root,
                                     Pose pose, Direction axis, int surfaceY) {
        float x0, x1, y0, y1, z0, z1;
        if (pose.prop() == Prop.SHIELD) {
            x0 = -0.30f; x1 = 0.30f;
            y0 = 0f; y1 = 2.50f;
            z0 = -0.75f; z1 = 0.75f;
        } else if (pose.prop() == Prop.SPEAR) {
            x0 = -0.07f; x1 = 0.07f;
            y0 = -0.07f; y1 = 1.60f;
            z0 = -0.04f; z1 = 0.04f;
        } else if (pose.prop() == Prop.CROSSBOW) {
            x0 = -0.32f; x1 = 0.32f;
            y0 = -0.07f; y1 = 0.32f;
            z0 = -0.32f; z1 = 0.32f;
        } else {
            // The eight unmodified item-mesh sources sit diagonally in XY.
            // Contact is at the high diagonal: blade/head DOWN, grip UP.
            // See the same -135° Z normalization in BattlefieldSceneRenderer.
            BattlefieldWeaponCatalog.Source mesh = BattlefieldWeaponCatalog.fromProp(pose.prop());
            x0 = mesh.minX; x1 = mesh.maxX;
            y0 = mesh.minY; y1 = mesh.maxY;
            z0 = mesh.minZ; z1 = mesh.maxZ;
        }
        BattlefieldWeaponCatalog.Source mesh = BattlefieldWeaponCatalog.isWeapon(pose.prop())
                ? BattlefieldWeaponCatalog.fromProp(pose.prop()) : null;

        double pitch = Math.toRadians(pose.pitch()), roll = Math.toRadians(pose.roll());
        double yaw = Math.toRadians(pose.yaw());
        double cx = Math.cos(pitch), sx = Math.sin(pitch);
        double cz = Math.cos(roll), sz = Math.sin(roll);
        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        Direction right = axis.getClockWise();
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (float x : new float[]{x0, x1}) {
            for (float y : new float[]{y0, y1}) {
                for (float z : new float[]{z0, z1}) {
                    // PoseStack order Y, Z, X (matrix applies X, then Z, then Y).
                    // Match the renderer's high-end pivot and -135° Z exactly:
                    // source low diagonal = grip in the AIR, high = blade
                    // tip / axe head in the GROUND.
                    double lx = x, ly = y, lz = z;
                    if (mesh != null) {
                        lx = mesh.tipDownX(x, y);
                        ly = mesh.tipDownY(x, y);
                        lz = (z - mesh.centerZ()) / 16d;
                    }
                    double px = lx, py = ly * cx - lz * sx, pz = ly * sx + lz * cx;
                    double qx = px * cz - py * sz, qy = px * sz + py * cz;
                    double rx = qx * cy + pz * sy, rz = -qx * sy + pz * cy;
                    double lateral = pose.right() + rx;
                    double ahead = pose.forward() - rz;
                    double wx = root.getX() + 0.5 + lateral * right.getStepX() + ahead * axis.getStepX();
                    double wz = root.getZ() + 0.5 + lateral * right.getStepZ() + ahead * axis.getStepZ();
                    double wy = surfaceY + pose.sink() + qy;
                    minX = Math.min(minX, wx); maxX = Math.max(maxX, wx);
                    minY = Math.min(minY, wy); maxY = Math.max(maxY, wy);
                    minZ = Math.min(minZ, wz); maxZ = Math.max(maxZ, wz);
                }
            }
        }
        int startY = Math.max(surfaceY + 1, (int) Math.floor(minY));
        int endY = (int) Math.ceil(maxY) - 1;
        if (endY < startY) return true;
        for (int x = (int) Math.floor(minX); x < Math.ceil(maxX); x++) {
            for (int z = (int) Math.floor(minZ); z < Math.ceil(maxZ); z++) {
                for (int y = startY; y <= endY; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!validColumn(level, p)
                            || !level.getBlockState(p).getCollisionShape(level, p).isEmpty()) return false;
                }
            }
        }
        return true;
    }

    private static int commit(ServerLevel level, Layout layout) {
        List<BlockPos> written = new ArrayList<>();
        BlockPos root = layout.root();
        if (!freeSurface(level, root)
                || !level.setBlock(root, layout.state(), Block.UPDATE_CLIENTS)) return 0;
        written.add(root);
        if (!(level.getBlockEntity(root) instanceof BattlefieldSceneBlockEntity entity)) {
            rollback(level, written);
            return 0;
        }
        entity.configure(layout.heights());
        level.sendBlockUpdated(root, layout.state(), layout.state(), Block.UPDATE_CLIENTS);

        for (Flag flag : layout.flags()) {
            if (!freeSurface(level, flag.pos())
                    || !level.setBlock(flag.pos(), flag.state(), Block.UPDATE_CLIENTS)) {
                rollback(level, written);
                return 0;
            }
            written.add(flag.pos());
        }
        return layout.visibleProps() + layout.flags().size();
    }

    private static void rollback(ServerLevel level, List<BlockPos> written) {
        for (int i = written.size() - 1; i >= 0; i--) {
            BlockPos p = written.get(i);
            Block block = level.getBlockState(p).getBlock();
            if (block == ModBlocks.BATTLEFIELD_BANNER || block == ModBlocks.BATTLEFIELD_SCENE) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }
}
