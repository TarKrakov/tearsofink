package com.tarkrakov.tearsofink.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Buried worldgen-only proof that a battlefield's center chunk was generated
 * with the new feature. Prevents the runtime installer from altering old fields
 * or visited chunks that predate the feature.
 */
public final class BattlefieldFieldMarkerBlock extends Block {
    public static final MapCodec<BattlefieldFieldMarkerBlock> CODEC = simpleCodec(BattlefieldFieldMarkerBlock::new);
    public BattlefieldFieldMarkerBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
}
