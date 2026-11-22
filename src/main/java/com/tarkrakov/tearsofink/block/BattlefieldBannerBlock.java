package com.tarkrakov.tearsofink.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.util.StringRepresentable;

/**
 * Rare, non-colliding authored banner preview. One quiet block entity, no
 * physics, no server animation and no item/loot. Worldgen is deliberately separate.
 */
public final class BattlefieldBannerBlock extends BaseEntityBlock {
    public static final MapCodec<BattlefieldBannerBlock> CODEC = simpleCodec(BattlefieldBannerBlock::new);
    // Missing property in older saves defaults to false: existing animated banner.
    public static final BooleanProperty BARE = BooleanProperty.create("bare");
    /** Defaults to REGULAR in worlds saved before the faction property existed. */
    public enum Faction implements StringRepresentable {
        REGULAR("regular"), SOUL("soul"), HUNTER("hunter");
        private final String id;
        Faction(String id) { this.id = id; }
        @Override public String getSerializedName() { return id; }
    }
    public static final EnumProperty<Faction> FACTION = EnumProperty.create("faction", Faction.class);
    /** 0 is the unchanged saved-world upright stance; 1..7 are fallen poles. */
    public static final IntegerProperty LEAN = IntegerProperty.create("lean", 0, 7);
    private static final float[] LEAN_X = {0f, 36f,-41f, 27f,-39f, 46f,-31f, 34f};
    private static final float[] LEAN_Z = {0f,-27f, 22f, 37f,-25f,-18f, 40f,-35f};
    public static float leanX(int value) { return LEAN_X[value]; }
    public static float leanZ(int value) { return LEAN_Z[value]; }

    public BattlefieldBannerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(BARE, false).setValue(FACTION, Faction.REGULAR)
                .setValue(LEAN, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BattlefieldBannerBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HorizontalDirectionalBlock.FACING, rotation.rotate(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, BARE, FACTION, LEAN);
    }
}
