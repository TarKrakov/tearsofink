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

/** Rare monument plus optional grounded relic tableau, no server ticker. */
public final class BattlefieldMonumentBlockEntity extends BlockEntity {
    public static final int MISSING = -9999;
    private int[] contacts = new int[0];

    public BattlefieldMonumentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTLEFIELD_MONUMENT, pos, state);
    }
    public int offsetAt(int i) {
        return i >= 0 && i < contacts.length ? contacts[i] : MISSING;
    }
    public void configure(int[] offsets) {
        contacts = Arrays.copyOf(offsets, offsets.length);
        setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("Contacts", contacts);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] saved = tag.getIntArray("Contacts");
        contacts = saved.length <= 6 ? saved : new int[0];
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
