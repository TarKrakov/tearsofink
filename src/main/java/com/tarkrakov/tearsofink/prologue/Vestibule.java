package com.tarkrakov.tearsofink.prologue;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Преддверие — крошечное туманное измерение (пустота) с постройкой автора и воротами.
 * Постройка: data/tears-of-ink/structure/vestibule.nbt. Метки (ванильные блоки, код их убирает):
 * золотой блок — где появляется игрок, алмазный — где стоит Г-н Проводник, изумрудные — проём ворот.
 * Нет файла — ставится простая заглушка.
 */
public final class Vestibule extends SavedData {
	public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION, TearsOfInk.id("vestibule"));
	private static final int BASE_Y = 64;

	boolean placed;
	BlockPos arrival = new BlockPos(0, BASE_Y + 1, 6);
	BlockPos guide = new BlockPos(0, BASE_Y + 1, -4);
	BlockPos gateMin = new BlockPos(-1, BASE_Y + 1, -8), gateMax = new BlockPos(1, BASE_Y + 4, -8);

	public static boolean is(Level level) {
		return level.dimension() == KEY;
	}

	@Nullable
	public static ServerLevel level(MinecraftServer server) {
		return server.getLevel(KEY);
	}

	public static Vestibule get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Vestibule::new, Vestibule::load, null), "tears_of_ink_vestibule");
	}

	public AABB gate() {
		return new AABB(gateMin.getX(), gateMin.getY(), gateMin.getZ(), gateMax.getX() + 1, gateMax.getY() + 1, gateMax.getZ() + 1);
	}

	public BlockPos arrival() { return arrival; }
	public BlockPos guidePos() { return guide; }

	/** Поставить постройку (один раз на мир). */
	public void ensurePlaced(ServerLevel level) {
		if (placed) return;
		Optional<StructureTemplate> template = level.getStructureManager().get(TearsOfInk.id("vestibule"));
		if (template.isPresent()) placeTemplate(level, template.get());
		else placePlaceholder(level);
		placed = true;
		setDirty();
	}

	private void placeTemplate(ServerLevel level, StructureTemplate t) {
		Vec3i size = t.getSize();
		BlockPos origin = new BlockPos(-size.getX() / 2, BASE_Y, -size.getZ() / 2);
		for (int cx = (origin.getX() >> 4) - 1; cx <= ((origin.getX() + size.getX()) >> 4) + 1; cx++)
			for (int cz = (origin.getZ() >> 4) - 1; cz <= ((origin.getZ() + size.getZ()) >> 4) + 1; cz++) level.getChunk(cx, cz);
		t.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.random, 2);
		BlockPos gMin = null, gMax = null, arr = null, gd = null;
		for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
			BlockState s = level.getBlockState(p);
			if (s.is(Blocks.EMERALD_BLOCK)) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
				gMin = gMin == null ? p.immutable() : new BlockPos(Math.min(gMin.getX(), p.getX()), Math.min(gMin.getY(), p.getY()), Math.min(gMin.getZ(), p.getZ()));
				gMax = gMax == null ? p.immutable() : new BlockPos(Math.max(gMax.getX(), p.getX()), Math.max(gMax.getY(), p.getY()), Math.max(gMax.getZ(), p.getZ()));
			} else if (s.is(Blocks.GOLD_BLOCK)) {
				arr = p.immutable().above();
				level.setBlock(p, floorNear(level, p), 2);
			} else if (s.is(Blocks.DIAMOND_BLOCK)) {
				gd = p.immutable().above();
				level.setBlock(p, floorNear(level, p), 2);
			}
		}
		if (arr != null) arrival = arr;
		if (gd != null) guide = gd;
		if (gMin != null) { gateMin = gMin; gateMax = gMax; }
		fillGate(level);
		if (arr == null || gd == null || gMin == null) {
			TearsOfInk.LOGGER.warn("Преддверие: не найдены метки (золото — игрок, алмаз — Проводник, изумруды — ворота). Беру места по умолчанию.");
		}
	}

	/** Чем заделать место метки: блок пола рядом. */
	private static BlockState floorNear(ServerLevel level, BlockPos p) {
		for (BlockPos n : new BlockPos[]{p.north(), p.south(), p.east(), p.west()}) {
			BlockState s = level.getBlockState(n);
			if (!s.isAir() && !s.is(Blocks.GOLD_BLOCK) && !s.is(Blocks.DIAMOND_BLOCK) && !s.is(Blocks.EMERALD_BLOCK)) return s;
		}
		return ModBlocks.ASPHODELITE_ROCK.defaultBlockState();
	}

	/** Заглушка: каменный островок, бледные ворота и два Вечных фонаря. */
	private void placePlaceholder(ServerLevel level) {
		BlockState rock = ModBlocks.ASPHODELITE_ROCK.defaultBlockState(), path = ModBlocks.ASHEN_PATH.defaultBlockState();
		BlockState log = ModBlocks.PALE_LOG.defaultBlockState();
		for (int x = -9; x <= 9; x++) {
			for (int z = -10; z <= 9; z++) {
				if (x * x + z * z > 100) continue;
				for (int y = BASE_Y - 3; y <= BASE_Y; y++) level.setBlock(new BlockPos(x, y, z), rock, 2);
				if (Math.abs(x) <= 1) level.setBlock(new BlockPos(x, BASE_Y, z), path, 2);
			}
		}
		for (int y = BASE_Y + 1; y <= BASE_Y + 5; y++) {
			level.setBlock(new BlockPos(-2, y, -8), log, 2);
			level.setBlock(new BlockPos(2, y, -8), log, 2);
		}
		for (int x = -2; x <= 2; x++) level.setBlock(new BlockPos(x, BASE_Y + 5, -8), log, 2);
		BlockState lantern = ModBlocks.ETERNAL_LANTERN.defaultBlockState();
		level.setBlock(new BlockPos(-3, BASE_Y + 1, -7), lantern, 2);
		level.setBlock(new BlockPos(3, BASE_Y + 1, -7), lantern, 2);
		gateMin = new BlockPos(-1, BASE_Y + 1, -8);
		gateMax = new BlockPos(1, BASE_Y + 4, -8);
		arrival = new BlockPos(0, BASE_Y + 1, 6);
		guide = new BlockPos(0, BASE_Y + 1, -5);
		fillGate(level);
	}

	/** Пелена в проёме ворот (где были изумруды). Ворота шире по X — пелена вдоль X, иначе вдоль Z. */
	private void fillGate(ServerLevel level) {
		net.minecraft.core.Direction.Axis axis = (gateMax.getX() - gateMin.getX()) >= (gateMax.getZ() - gateMin.getZ())
				? net.minecraft.core.Direction.Axis.X : net.minecraft.core.Direction.Axis.Z;
		BlockState veil = ModBlocks.VESTIBULE_GATE.defaultBlockState().setValue(VestibuleGateBlock.AXIS, axis);
		for (BlockPos p : BlockPos.betweenClosed(gateMin, gateMax)) {
			if (level.getBlockState(p).isAir()) level.setBlock(p, veil, 2);
		}
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putBoolean("placed", placed);
		tag.put("arrival", NbtUtils.writeBlockPos(arrival));
		tag.put("guide", NbtUtils.writeBlockPos(guide));
		tag.put("gate_min", NbtUtils.writeBlockPos(gateMin));
		tag.put("gate_max", NbtUtils.writeBlockPos(gateMax));
		return tag;
	}

	private static Vestibule load(CompoundTag tag, HolderLookup.Provider registries) {
		Vestibule v = new Vestibule();
		v.placed = tag.getBoolean("placed");
		NbtUtils.readBlockPos(tag, "arrival").ifPresent(p -> v.arrival = p);
		NbtUtils.readBlockPos(tag, "guide").ifPresent(p -> v.guide = p);
		NbtUtils.readBlockPos(tag, "gate_min").ifPresent(p -> v.gateMin = p);
		NbtUtils.readBlockPos(tag, "gate_max").ifPresent(p -> v.gateMax = p);
		return v;
	}

}
