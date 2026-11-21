package com.tarkrakov.tearsofink.command;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.Heightmap;

/** Operator-only showcase of real authored patrol gear, without any worldgen. */
public final class BattlefieldDecorCommands {
    private BattlefieldDecorCommands() {}

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("tearsofink")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("battlefield")
                                .then(Commands.literal("preview").executes(BattlefieldDecorCommands::preview))
                                .then(Commands.literal("banner").executes(BattlefieldDecorCommands::banner)))));
    }

    private static int preview(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Block[] blocks = {ModBlocks.BATTLEFIELD_SPEAR, ModBlocks.BATTLEFIELD_SHIELD, ModBlocks.BATTLEFIELD_CROSSBOW};
        String[] names = {"копьё", "щит", "арбалет"};
        StringBuilder placed = new StringBuilder();
        int count = 0;
        // Three independent spots, four blocks in front of the player.
        // Never replace existing structures, vegetation or player blocks.
        Direction forward = player.getDirection();
        Direction side = forward.getClockWise();
        BlockPos origin = player.blockPosition().relative(forward, 4);
        for (int i = 0; i < blocks.length; i++) {
            BlockPos xz = origin.relative(side, (i - 1) * 3);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz.getX(), xz.getZ());
            BlockPos ground = new BlockPos(xz.getX(), y, xz.getZ());
            // The doubled shield spans 2.5 blocks: bake it one block higher so
            // its lower edge touches terrain without exceeding JSON model bounds.
            boolean largeShield = blocks[i] == ModBlocks.BATTLEFIELD_SHIELD;
            BlockPos spot = largeShield ? ground.above() : ground;
            if (!level.getWorldBorder().isWithinBounds(spot)
                    || Math.abs(spot.getY() - player.getBlockY()) > 5
                    || !level.getBlockState(ground).isAir()
                    || !level.getBlockState(spot).isAir()
                    || !level.getBlockState(ground.below()).isFaceSturdy(level, ground.below(), Direction.UP)) {
                continue;
            }
            // Avoid covering vegetation or structures with the tall shield.
            if (largeShield && (!level.getBlockState(spot.above()).isAir()
                    || !level.getBlockState(spot.above(2)).isAir())) {
                continue;
            }
            var state = blocks[i].defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, forward.getOpposite());
            if (level.setBlock(spot, state, Block.UPDATE_CLIENTS)) {
                count++;
                placed.append(" ").append(names[i]).append(" ").append(spot.toShortString()).append(";");
            }
        }
        final int done = count;
        final String positions = placed.toString();
        context.getSource().sendSuccess(
                () -> Component.literal("Поле боя: размещено " + done + "/3 декораций." + positions
                        + (done < 3 ? " Остальные места заняты или неровные — перейдите на открытую площадку." : "")),
                false);
        return count;
    }

    /**
     * Place the authored 4-block-high banner like a real world decoration:
     * find a solid ground anchor near the player and test the actual pole/cloth
     * footprint. No artificial flat 7x7 clearing, no vegetation removal, no
     * overwriting structures and no automatic worldgen in the preview command.
     *
     * Keep this local-only preview search separate from the future deterministic
     * BattlefieldPlan / zone placement required by D-114–D-116.
     */
    private static int banner(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Direction forward = player.getDirection();
        Direction preferredFacing = forward.getOpposite();
        Direction side = forward.getClockWise();
        BlockPos target = player.blockPosition().relative(forward, 5);

        // Search candidate anchors around the requested position, nearest first.
        // This adapts to irregular ground and patches of grass without modifying
        // the terrain. At radius 4 every sampled anchor remains ahead of the player.
        for (int radius = 0; radius <= 4; radius++) {
            for (int f = -radius; f <= radius; f++) {
                for (int s = -radius; s <= radius; s++) {
                    if (Math.max(Math.abs(f), Math.abs(s)) != radius) continue;
                    BlockPos xz = target.relative(forward, f).relative(side, s);
                    if (!level.hasChunkAt(xz) || !level.getWorldBorder().isWithinBounds(xz)) continue;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz.getX(), xz.getZ());
                    if (Math.abs(y - player.getBlockY()) > 5) continue;
                    BlockPos base = new BlockPos(xz.getX(), y, xz.getZ());

                    // Preserve the player's requested orientation when it fits.
                    // Rotating the flag is less intrusive than requiring terrain
                    // changes or wiping a huge empty rectangle.
                    for (int turn = 0; turn < 4; turn++) {
                        Direction facing = preferredFacing;
                        for (int j = 0; j < turn; j++) facing = facing.getClockWise();
                        if (!canPlaceBanner(level, base, facing)) continue;

                        var state = ModBlocks.BATTLEFIELD_BANNER.defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, facing);
                        if (!level.setBlock(base, state, Block.UPDATE_CLIENTS)) continue;
                        BlockPos placed = base;
                        context.getSource().sendSuccess(
                                () -> Component.literal("Первое знамя установлено на " + placed.toShortString()
                                        + ". Рельеф и растения сохранены; полотно движется только на клиенте."),
                                false);
                        return 1;
                    }
                }
            }
        }
        context.getSource().sendFailure(Component.literal(
                "Рядом не нашлось безопасного основания для знамени. Попробуйте отойти от стен или деревьев; "
                        + "ничего не было срублено или заменено."));
        return 0;
    }

    /**
     * Source model first_flag.bbmodel: pole Y 0..64, cloth X -35..-1,
     * Y 40..63, Z 0..1 (units /16). Rendering centers the pole in the
     * anchor block, then rotates the cloth with FACING.
     *
     * Therefore the pole needs just one column of air, and the high cloth
     * occupies two more columns on the counter-clockwise side. Low grass
     * elsewhere (including beneath the cloth) is perfectly fine.
     * Checking only that real footprint avoids the erroneous 7x7x5 veto.
     */
    private static boolean canPlaceBanner(ServerLevel level, BlockPos base, Direction facing) {
        if (base.getY() <= level.getMinBuildHeight()
                || base.getY() + 4 >= level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(base)
                || !level.hasChunkAt(base)
                || !level.getBlockState(base).isAir()
                || !level.getBlockState(base.below()).isFaceSturdy(level, base.below(), Direction.UP)) {
            return false;
        }

        // The pole spans four blocks, but only its anchor replaces a block.
        // Leaves/solid buildings stop placement; harmless zero-collision
        // vegetation higher up does not need to be cleared or destroyed.
        for (int dy = 1; dy <= 3; dy++) {
            if (hasSolidObstruction(level, base.above(dy))) return false;
        }

        // At the authored scale the cloth extends 35/16 from the centered
        // pole, reaching only two *other* columns. The animation amplitudes
        // are small enough that the wave stays inside those columns.
        Direction clothSide = facing.getCounterClockWise();
        for (int distance = 1; distance <= 2; distance++) {
            BlockPos column = base.relative(clothSide, distance);
            for (int dy = 2; dy <= 3; dy++) {
                if (hasSolidObstruction(level, column.above(dy))) return false;
            }
        }
        return true;
    }

    private static boolean hasSolidObstruction(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) return true;
        // A zero-collision plant does not block the visible cloth; a wall,
        // log, structure block, banner or other physical obstacle does.
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }
}
