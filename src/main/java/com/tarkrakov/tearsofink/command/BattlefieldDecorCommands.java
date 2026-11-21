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
                                .then(Commands.literal("preview").executes(BattlefieldDecorCommands::preview)))));
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
}
