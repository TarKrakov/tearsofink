package com.tarkrakov.tearsofink.command;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.block.BattlefieldBannerBlock;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldBannerGeometry;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldMonumentGeometry;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldFactionBannerGeometry;
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
                                .then(Commands.literal("monument")
                                        .then(monumentCommand("linear", BattlefieldMonumentGeometry.Blade.LINEAR))
                                        .then(monumentCommand("cleaving", BattlefieldMonumentGeometry.Blade.CLEAVING)))
                                .then(Commands.literal("banner")
                                        .executes(ctx -> banner(ctx, BattlefieldBannerBlock.Faction.REGULAR, false))
                                        .then(Commands.literal("regular_standard")
                                                .executes(ctx -> banner(ctx, BattlefieldBannerBlock.Faction.REGULAR, false)))
                                        .then(Commands.literal("regular_bare")
                                                .executes(ctx -> banner(ctx, BattlefieldBannerBlock.Faction.REGULAR, true)))
                                        .then(Commands.literal("paladin_standard")
                                                .executes(ctx -> banner(ctx, BattlefieldBannerBlock.Faction.SOUL, false)))
                                        .then(Commands.literal("paladin_bare")
                                                .executes(ctx -> banner(ctx, BattlefieldBannerBlock.Faction.SOUL, true)))
                                        .then(Commands.literal("hunter_standard")
                                                .executes(ctx -> banner(ctx, BattlefieldBannerBlock.Faction.HUNTER, false)))
                                        .then(Commands.literal("hunter_bare")
                                                .executes(ctx -> banner(ctx, BattlefieldBannerBlock.Faction.HUNTER, true))))
                                .then(BattlefieldZonePreview.literal()))));
    }


    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> monumentCommand(
            String id, BattlefieldMonumentGeometry.Blade blade) {
        return Commands.literal(id)
                .executes(ctx -> monument(ctx, blade, 0, false))
                .then(Commands.argument("variant", IntegerArgumentType.integer(0, 7))
                        .executes(ctx -> monument(ctx, blade, IntegerArgumentType.getInteger(ctx, "variant"), false)))
                .then(Commands.literal("tableau")
                        .executes(ctx -> monument(ctx, blade, 0, true))
                        .then(Commands.argument("variant", IntegerArgumentType.integer(0, 7))
                                .executes(ctx -> monument(ctx, blade, IntegerArgumentType.getInteger(ctx, "variant"), true))));
    }

    /** Non-destructive operator-only placement of a giant weapon on prepared natural ground. */
    private static int monument(CommandContext<CommandSourceStack> context,
                                BattlefieldMonumentGeometry.Blade blade, int variant, boolean tableau)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Direction forward = player.getDirection(), side = forward.getClockWise();
        BlockPos target = player.blockPosition().relative(forward, 14);
        for (int radius = 0; radius <= 8; radius++) {
            for (int df = -radius; df <= radius; df++) for (int ds = -radius; ds <= radius; ds++) {
                if (Math.max(Math.abs(df), Math.abs(ds)) != radius) continue;
                BlockPos xz = target.relative(forward, df).relative(side, ds);
                if (!level.hasChunkAt(xz) || !level.getWorldBorder().isWithinBounds(xz)) continue;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz.getX(), xz.getZ());
                if (Math.abs(y - player.getBlockY()) > 6) continue;
                BlockPos base = new BlockPos(xz.getX(), y, xz.getZ());
                for (int turn = 0; turn < 4; turn++) {
                    Direction facing = forward.getOpposite();
                    for (int k = 0; k < turn; k++) facing = facing.getClockWise();
                    if (!canPlaceMonument(level, base, blade, variant, facing)) continue;
                    int[] relicContacts = tableau ? tableauContacts(level, base, facing, blade, variant) : new int[0];
                    if (relicContacts == null) continue;
                    var state = ModBlocks.BATTLEFIELD_MONUMENT.defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, facing)
                            .setValue(com.tarkrakov.tearsofink.block.BattlefieldMonumentBlock.BLADE, blade.ordinal())
                            .setValue(com.tarkrakov.tearsofink.block.BattlefieldMonumentBlock.VARIANT, variant)
                            .setValue(com.tarkrakov.tearsofink.block.BattlefieldMonumentBlock.TABLEAU, tableau);
                    if (!level.setBlock(base, state, Block.UPDATE_CLIENTS)) continue;
                    if (tableau) {
                        if (level.getBlockEntity(base) instanceof com.tarkrakov.tearsofink.block.BattlefieldMonumentBlockEntity monumentEntity) {
                            monumentEntity.configure(relicContacts);
                            level.sendBlockUpdated(base, state, state, Block.UPDATE_CLIENTS);
                        } else {
                            level.setBlock(base, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                            continue;
                        }
                    }
                    BlockPos placed = base;
                    var style = BattlefieldMonumentGeometry.style(variant);
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Монументальный " + blade.label + (tableau ? " с боевой композицией" : "") + " установлен: " + placed.toShortString()
                            + " (масштаб x" + style.scale() + ", наклон " + style.tilt()
                            + "°, поворот " + style.yaw() + "°)."), false);
                    return 1;
                }
            }
        }
        context.getSource().sendFailure(Component.literal(
                "Для огромного клинка нет свободной площадки в загруженных чанках рядом. "
                + "Строения и деревья сохранены; попробуйте открытую местность."));
        return 0;
    }

    /** Shared automation/preview placement. No chunk loads, no destructive writes. */
    public static boolean placePlannedMonument(ServerLevel level, BlockPos rootXZ, Direction facing,
                                              BattlefieldMonumentGeometry.Blade blade,
                                              int variant, boolean tableau) {
        if (!level.hasChunkAt(rootXZ)) return false;
        for (int radius = 0; radius <= 3; radius++) {
            for (int f = -radius; f <= radius; f++) for (int r = -radius; r <= radius; r++) {
                if (Math.max(Math.abs(f), Math.abs(r)) != radius) continue;
                BlockPos xz = rootXZ.relative(facing, f).relative(facing.getClockWise(), r);
                if (!level.hasChunkAt(xz) || (xz.getX() >> 4) != (rootXZ.getX() >> 4)
                        || (xz.getZ() >> 4) != (rootXZ.getZ() >> 4)) continue;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz.getX(), xz.getZ());
                BlockPos base = new BlockPos(xz.getX(), y, xz.getZ());
                if (!canPlaceMonument(level, base, blade, variant, facing)) continue;
                int[] contacts = tableau ? tableauContacts(level, base, facing, blade, variant) : new int[0];
                if (contacts == null) continue;
                var state = ModBlocks.BATTLEFIELD_MONUMENT.defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, facing)
                        .setValue(com.tarkrakov.tearsofink.block.BattlefieldMonumentBlock.BLADE, blade.ordinal())
                        .setValue(com.tarkrakov.tearsofink.block.BattlefieldMonumentBlock.VARIANT, variant)
                        .setValue(com.tarkrakov.tearsofink.block.BattlefieldMonumentBlock.TABLEAU, tableau);
                if (!level.setBlock(base, state, Block.UPDATE_CLIENTS)) continue;
                if (tableau) {
                    if (level.getBlockEntity(base) instanceof com.tarkrakov.tearsofink.block.BattlefieldMonumentBlockEntity entity) {
                        entity.configure(contacts);
                        level.sendBlockUpdated(base, state, state, Block.UPDATE_CLIENTS);
                    } else {
                        level.setBlock(base, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        continue;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /** Find natural ground contacts for the small relics without overwriting anything. */
    private static int[] tableauContacts(ServerLevel level, BlockPos base, Direction facing,
                                         BattlefieldMonumentGeometry.Blade blade, int variant) {
        var relics = BattlefieldMonumentGeometry.tableau(blade, variant);
        int[] contacts = new int[relics.length];
        java.util.Arrays.fill(contacts, com.tarkrakov.tearsofink.block.BattlefieldMonumentBlockEntity.MISSING);
        Direction right = facing.getClockWise();
        for (int i = 0; i < relics.length; i++) {
            var relic = relics[i];
            BlockPos point = base.relative(facing, Math.round(relic.forward()))
                    .relative(right, Math.round(relic.right()));
            if (!level.hasChunkAt(point)) {
                if (relic.required()) return null;
                continue;
            }
            int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, point.getX(), point.getZ());
            BlockPos at = new BlockPos(point.getX(), height, point.getZ());
            var ground = level.getBlockState(at.below());
            boolean good = Math.abs(height - base.getY()) <= 3
                    && level.getWorldBorder().isWithinBounds(at)
                    && level.getBlockState(at).isAir()
                    && ground.isFaceSturdy(level, at.below(), Direction.UP)
                    && (ground.is(net.minecraft.tags.BlockTags.DIRT)
                    || ground.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
                    || ground.is(ModBlocks.ASHEN_SOIL) || ground.is(ModBlocks.BLOOD_SOIL)
                    || ground.is(ModBlocks.TAR_SOIL) || ground.is(ModBlocks.ASPHODELITE_ROCK)
                    || ground.is(ModBlocks.ASH))
                    && level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty();
            if (!good) {
                if (relic.required()) return null;
                continue;
            }
            contacts[i] = height - base.getY();
        }
        return contacts;
    }

    private static boolean canPlaceMonument(ServerLevel level, BlockPos base,
                                            BattlefieldMonumentGeometry.Blade blade,
                                            int variant, Direction facing) {
        if (!level.getBlockState(base).isAir() ||
                !level.getBlockState(base.below()).isFaceSturdy(level, base.below(), Direction.UP)) return false;
        var below = level.getBlockState(base.below());
        if (!(below.is(net.minecraft.tags.BlockTags.DIRT)
                || below.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
                || below.is(net.minecraft.world.level.block.Blocks.GRAVEL)
                || below.is(ModBlocks.ASHEN_SOIL) || below.is(ModBlocks.BLOOD_SOIL)
                || below.is(ModBlocks.TAR_SOIL) || below.is(ModBlocks.ASPHODELITE_ROCK)
                || below.is(ModBlocks.ASH))) return false;
        var lands = com.tarkrakov.tearsofink.world.InkLandsLayout.of(level);
        for (BlockPos delta : BattlefieldMonumentGeometry.occupiedCells(blade, variant, facing)) {
            BlockPos pos = base.offset(delta);
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || pos.getY() < level.getMinBuildHeight()
                    || pos.getY() >= level.getMaxBuildHeight()) return false;
            if (lands != null && lands.nearRoad(pos.getX(), pos.getZ(), 12)) return false;
            // Below-ground contact is intentional. Above-ground parts must not clip solid obstacles.
            if (pos.getY() > base.getY() + 1
                    && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return false;
        }
        return true;
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
     * Place the authored 7-block-high regular standard on natural terrain:
     * find a solid ground anchor near the player and test the actual animated-cloth envelope
     * footprint. No artificial flat 7x7 clearing, no vegetation removal, no
     * overwriting structures and no automatic worldgen in the preview command.
     *
     * Keep this local-only preview search separate from the future deterministic
     * BattlefieldPlan / zone placement required by D-114–D-116.
     */
    private static int banner(CommandContext<CommandSourceStack> context,
                              BattlefieldBannerBlock.Faction faction, boolean bare)
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
                        if (!canPlaceBanner(level, base, facing, bare, faction)) continue;

                        var state = ModBlocks.BATTLEFIELD_BANNER.defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, facing)
                                .setValue(BattlefieldBannerBlock.BARE, bare)
                                .setValue(BattlefieldBannerBlock.FACTION, faction);
                        if (!level.setBlock(base, state, Block.UPDATE_CLIENTS)) continue;
                        BlockPos placed = base;
                        context.getSource().sendSuccess(
                                () -> Component.literal((switch (faction) {
                                    case REGULAR -> "Регулярный штандарт";
                                    case SOUL -> "Штандарт паладинов Угасающей Звезды";
                                    case HUNTER -> "Штандарт Чернильных Охотников";
                                }) + (bare ? " без полотна" : " с полотном")
                                        + " установлен на " + placed.toShortString()
                                        + ". Рельеф и растения сохранены."),
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

    /** Conservative footprint for all 31 source cuboids, including the five moving cloth sections. */
    static boolean canPlaceBanner(ServerLevel level, BlockPos base, Direction facing) {
        return canPlaceBanner(level, base, facing, false);
    }

    static boolean canPlaceBanner(ServerLevel level, BlockPos base, Direction facing, boolean bare) {
        return canPlaceBanner(level, base, facing, bare, BattlefieldBannerBlock.Faction.REGULAR);
    }

    static boolean canPlaceBanner(ServerLevel level, BlockPos base, Direction facing,
                                  boolean bare, BattlefieldBannerBlock.Faction faction) {
        if (base.getY() <= level.getMinBuildHeight()
                || !level.getWorldBorder().isWithinBounds(base)
                || !level.hasChunkAt(base)
                || !level.getBlockState(base).isAir()
                || !level.getBlockState(base.below()).isFaceSturdy(level, base.below(), Direction.UP)) {
            return false;
        }
        for (BlockPos offset : BattlefieldFactionBannerGeometry.footprint(faction, bare, facing)) {
            BlockPos pos = base.offset(offset);
            if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()
                    || hasSolidObstruction(level, pos)) return false;
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
