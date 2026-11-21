package com.tarkrakov.tearsofink.block;

import com.tarkrakov.tearsofink.block.lantern.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Only stores sampled terrain contact offsets, set once at placement. No
 * ticker, physics, item drops, entity AI or continuous network traffic.
 * Explicit initial chunk data + update packet keep both clients consistent.
 */
public final class BattlefieldSceneBlockEntity extends BlockEntity {
    public static final int MISSING = -9999;
    private int[] contactHeights = new int[0];

    public BattlefieldSceneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTLEFIELD_SCENE, pos, state);
    }

    public int offsetAt(int index) {
        return index >= 0 && index < contactHeights.length ? contactHeights[index] : MISSING;
    }

    public void configure(int[] offsets) {
        this.contactHeights = Arrays.copyOf(offsets, offsets.length);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("Contacts", contactHeights);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] offsets = tag.getIntArray("Contacts");
        contactHeights = offsets.length <= 16 ? offsets : new int[0];
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Contacts", contactHeights);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
