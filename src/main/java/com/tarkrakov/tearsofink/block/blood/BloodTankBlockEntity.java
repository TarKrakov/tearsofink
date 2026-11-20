package com.tarkrakov.tearsofink.block.blood;

import com.tarkrakov.tearsofink.block.lantern.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/** Накопитель: хранит кровь и ранее полученные Кристаллы крови (совместимость сохранений). */
public class BloodTankBlockEntity extends BlockEntity {
	private int blood;
	private int crystals;

	public BloodTankBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BLOOD_TANK, pos, state);
	}

	public int blood() {
		return blood;
	}

	public int crystals() {
		return crystals;
	}

	/** Удар помпы: +1 крови. Кристаллы больше не образуются пассивно. */
	public void pumpIn(RandomSource random) {
		if (blood < BloodTuning.TANK_CAPACITY) blood++;
		setChanged();
	}

	/** Можно ли гусенице отпить. */
	public boolean canSip() {
		return blood > BloodTuning.CATERPILLAR_MIN;
	}

	/** Гусеница отпивает, не опуская ниже минимума. */
	public boolean sip() {
		if (!canSip()) return false;
		blood = Math.max(BloodTuning.CATERPILLAR_MIN, blood - BloodTuning.CATERPILLAR_SIP);
		setChanged();
		return true;
	}

	/** Игрок забирает кровь целыми Сгустками. Возвращает число Сгустков. */
	public int takeClots() {
		int clots = blood / BloodTuning.BLOOD_PER_CLOT;
		blood -= clots * BloodTuning.BLOOD_PER_CLOT;
		if (clots > 0) setChanged();
		return clots;
	}

	public int takeCrystals() {
		int c = crystals;
		crystals = 0;
		if (c > 0) setChanged();
		return c;
	}

	/** Ближайший Накопитель, из которого можно отпить (поиск по блок-сущностям — быстро). */
	@Nullable
	public static BlockPos findSippable(Level level, BlockPos from, int radius) {
		BlockPos best = null;
		double bestDist = (double) radius * radius;
		ChunkPos min = new ChunkPos(from.offset(-radius, 0, -radius));
		ChunkPos max = new ChunkPos(from.offset(radius, 0, radius));
		for (int cx = min.x; cx <= max.x; cx++) {
			for (int cz = min.z; cz <= max.z; cz++) {
				if (!level.hasChunk(cx, cz)) continue;
				LevelChunk chunk = level.getChunk(cx, cz);
				for (BlockPos p : chunk.getBlockEntitiesPos()) {
					if (!(chunk.getBlockEntity(p) instanceof BloodTankBlockEntity tank) || !tank.canSip()) continue;
					double d = p.distSqr(from);
					if (d <= bestDist) {
						bestDist = d;
						best = p;
					}
				}
			}
		}
		return best;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt("Blood", blood);
		tag.putInt("Crystals", crystals);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		blood = tag.getInt("Blood");
		crystals = tag.getInt("Crystals");
	}
}
