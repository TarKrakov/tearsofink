package com.tarkrakov.tearsofink.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Prop;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Scene;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldSceneRecipes.Slot;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * D-122: non-destructive operator preview of TWO parametric battlefield zone
 * recipes. No generation, global seed quotas, mobs, ticks, new art or loot.
 *
 * Required anchors are validated as a unit BEFORE any blocks are written.
 * Optional details can be skipped, but an incomplete core is never placed.
 * Terrain is sampled per prop and blocks are never removed/replaced.
 */
public final class BattlefieldZonePreview {
    private BattlefieldZonePreview() {}

    static LiteralArgumentBuilder<CommandSourceStack> literal() {
        return Commands.literal("zone")
                .then(Commands.literal("last_stand").executes(ctx -> preview(ctx, Scene.LAST_STAND)))
                .then(Commands.literal("banner_line").executes(ctx -> preview(ctx, Scene.BANNER_LINE)));
    }

    private record Placement(Prop prop, BlockPos pos, BlockState state, Set<BlockPos> envelope) {}

    // Root search is intentionally local to the operator preview. The final
    // BattlefieldPlan must search and reserve large regions *before* worldgen.
    private static int preview(CommandContext<CommandSourceStack> context, Scene scene)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Direction forward = player.getDirection();
        Direction right = forward.getClockWise();
        BlockPos target = player.blockPosition().relative(forward, 8);

        for (int radius = 0; radius <= 5; radius++) {
            for (int f = -radius; f <= radius; f++) {
                for (int s = -radius; s <= radius; s++) {
                    if (Math.max(Math.abs(f), Math.abs(s)) != radius) continue;
                    BlockPos root = target.relative(forward, f).relative(right, s);
                    if (!level.hasChunkAt(root) || !level.getWorldBorder().isWithinBounds(root)) continue;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, root.getX(), root.getZ());
                    if (Math.abs(y - player.getBlockY()) > 5) continue;

                    // Same level seed and candidate anchor -> same relationships.
                    long seed = level.getSeed() ^ (root.getX() * 341873128712L)
                            ^ (root.getZ() * 132897987541L) ^ scene.ordinal();
                    List<Placement> plan = assemble(level, root, y, forward, right,
                            BattlefieldSceneRecipes.plan(scene, seed));
                    if (plan == null) continue;

                    int placed = commit(level, plan);
                    if (placed == 0) continue;
                    BlockPos location = root;
                    int count = placed;
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Пробная зона «" + scene.label() + "»: " + count
                                    + " связанных декораций у " + location.toShortString()
                                    + ". Рельеф и постройки не изменены; это ещё не генерация поля."),
                            false);
                    return placed;
                }
            }
        }
        context.getSource().sendFailure(Component.literal(
                "Поблизости не удалось безопасно разместить все обязательные элементы зоны «"
                        + scene.label() + "». Ничего не изменено; попробуйте участок без стен и густых деревьев."));
        return 0;
    }

    private static List<Placement> assemble(ServerLevel level, BlockPos origin, int rootY,
                                             Direction forward, Direction right, List<Slot> slots) {
        List<Placement> planned = new ArrayList<>();
        Set<BlockPos> occupied = new HashSet<>();

        // Required core must be resolved before spending space on optional props.
        for (int pass = 0; pass < 2; pass++) {
            boolean required = pass == 0;
            for (Slot slot : slots) {
                if (slot.required() != required) continue;
                Placement selected = findSlot(level, origin, rootY, forward, right, slot, occupied);
                if (selected == null) {
                    if (required) return null;
                    continue;
                }
                planned.add(selected);
                occupied.addAll(selected.envelope());
            }
        }
        return planned;
    }

    private static Placement findSlot(ServerLevel level, BlockPos origin, int rootY,
                                      Direction forward, Direction right, Slot slot,
                                      Set<BlockPos> occupied) {
        // Small adaptive offsets keep every prop connected to its episode.
        // They are not random standalone spawn locations.
        int[][] offsets = {{0,0}, {1,0}, {-1,0}, {0,1}, {0,-1}};
        Direction facing = forward.getOpposite();
        for (int n = 0; n < slot.facingTurns(); n++) facing = facing.getClockWise();

        for (int[] offset : offsets) {
            BlockPos column = origin.relative(forward, slot.forward() + offset[0])
                    .relative(right, slot.right() + offset[1]);
            if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    column.getX(), column.getZ());
            if (Math.abs(y - rootY) > 3 || y < level.getMinBuildHeight() + 1
                    || y + 5 >= level.getMaxBuildHeight()) continue;
            BlockPos surface = new BlockPos(column.getX(), y, column.getZ());
            if (!level.getBlockState(surface).isAir()
                    || !level.getBlockState(surface.below()).isFaceSturdy(level, surface.below(), Direction.UP)
                    || !isNaturalGround(level.getBlockState(surface.below()))) continue;

            BlockPos anchor = slot.prop() == Prop.SHIELD ? surface.above() : surface;
            if (!clearForProp(level, slot.prop(), surface, anchor, facing)) continue;
            Set<BlockPos> envelope = envelope(slot.prop(), anchor, facing);
            boolean overlaps = false;
            for (BlockPos p : envelope) {
                if (occupied.contains(p)) {
                    overlaps = true;
                    break;
                }
            }
            if (overlaps) continue;

            Block block = switch (slot.prop()) {
                case BANNER -> ModBlocks.BATTLEFIELD_BANNER;
                case SHIELD -> ModBlocks.BATTLEFIELD_SHIELD;
                case SPEAR -> ModBlocks.BATTLEFIELD_SPEAR;
                case CROSSBOW -> ModBlocks.BATTLEFIELD_CROSSBOW;
            };
            BlockState state = block.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, facing);
            return new Placement(slot.prop(), anchor, state, envelope);
        }
        return null;
    }

    private static boolean isNaturalGround(BlockState state) {
        // Preview must not plant monuments on a roadway, ruin floor,
        // bridge or construction. Do not turn this whitelist into a global
        // protected-area mechanism for the future worldgen planner.
        return state.is(BlockTags.DIRT) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND) || state.is(Blocks.CLAY)
                || state.is(ModBlocks.ASHEN_SOIL) || state.is(ModBlocks.BLOOD_SOIL)
                || state.is(ModBlocks.TAR_SOIL) || state.is(ModBlocks.ASPHODELITE_ROCK)
                || state.is(ModBlocks.ASH);
    }

    private static boolean clearForProp(ServerLevel level, Prop prop,
                                        BlockPos surface, BlockPos anchor, Direction facing) {
        if (prop == Prop.BANNER) {
            return BattlefieldDecorCommands.canPlaceBanner(level, anchor, facing);
        }
        // The existing static shield is offset one block upward and flipped;
        // it occupies the actual ground level up to roughly 2.5 blocks.
        if (!level.getBlockState(anchor).isAir()) return false;
        if (prop == Prop.SHIELD) {
            return !hasSolidObstacle(level, anchor.above())
                    && !hasSolidObstacle(level, anchor.above(2));
        }
        // The spear rises ~1.6 blocks; the crossbow lies near the surface.
        return !hasSolidObstacle(level, surface.above());
    }

    private static boolean hasSolidObstacle(ServerLevel level, BlockPos pos) {
        return !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private static Set<BlockPos> envelope(Prop prop, BlockPos anchor, Direction facing) {
        Set<BlockPos> area = new HashSet<>();
        if (prop == Prop.BANNER) {
            for (int y = 0; y <= 3; y++) area.add(anchor.above(y));
            // Reuse D-120's *real* authored cloth footprint, not a 7x7 volume.
            Direction clothSide = facing.getCounterClockWise();
            for (int x = 1; x <= 2; x++) {
                for (int y = 2; y <= 3; y++) {
                    area.add(anchor.relative(clothSide, x).above(y));
                }
            }
        } else if (prop == Prop.SHIELD) {
            area.add(anchor.below());
            for (int y = 0; y <= 2; y++) area.add(anchor.above(y));
        } else {
            area.add(anchor);
            area.add(anchor.above());
        }
        return area;
    }

    private static int commit(ServerLevel level, List<Placement> plan) {
        // Failure is exceptional on the single server thread, but rollback
        // avoids leaving a half-built "story" if a placement ever fails.
        List<Placement> written = new ArrayList<>();
        for (Placement p : plan) {
            if (!level.getBlockState(p.pos()).isAir()
                    || !level.setBlock(p.pos(), p.state(), Block.UPDATE_CLIENTS)) {
                for (Placement placed : written) {
                    if (level.getBlockState(placed.pos()).is(placed.state().getBlock())) {
                        level.setBlock(placed.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                return 0;
            }
            written.add(p);
        }
        return written.size();
    }
}
