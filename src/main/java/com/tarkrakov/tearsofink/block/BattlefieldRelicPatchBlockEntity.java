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

/** Height contacts are recorded once; all poses are reproducible from anchor+style. */
public final class BattlefieldRelicPatchBlockEntity extends BlockEntity {
    public static final int MISSING = -9999;
    private int[] contacts = new int[0];

    public BattlefieldRelicPatchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTLEFIELD_RELIC_PATCH, pos, state);
    }
    public int offsetAt(int i) {
        return i >= 0 && i < contacts.length ? contacts[i] : MISSING;
    }
    public void configure(int[] heights) {
        if (heights.length > 24) throw new IllegalArgumentException("Too many scenic relics");
        contacts = Arrays.copyOf(heights, heights.length);
        setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("Contacts", contacts);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] saved = tag.getIntArray("Contacts");
        contacts = saved.length <= 24 ? saved : new int[0];
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Contacts", contacts);
        return tag;
    }
    @Nullable @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
