package com.tarkrakov.tearsofink.world.herald;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Багряный Вестник — огромное дерево автора (data/tears-of-ink/structure/blood_herald.nbt)
 * в центре каждого пятна Кроволесья. У его корней — 4 Кровосборника (помпа + Накопитель).
 * Где у дерева ствол и где встанут Кровосборники, считается по самому файлу — автор может пересохранить дерево.
 */
public final class BloodHerald {
	public static final ResourceLocation TEMPLATE = TearsOfInk.id("blood_herald");

	// ===== Настройки =====
	/** Сколько Кровосборников у корней. */
	public static final int COLLECTORS = 4;
	/** Отступ Кровосборника от последнего корня (блоков). */
	public static final int COLLECTOR_GAP = 2;
	/** В этом радиусе от ствола обычные деревья не растут (чтобы не врастали в Вестника). */
	public static final int CLEAR_RADIUS = 75;
	/** Дороги обходят ствол на таком расстоянии. */
	public static final int ROAD_AVOID = 42;

	public static final StructureType<BloodHeraldStructure> STRUCTURE_TYPE = Registry.register(
			BuiltInRegistries.STRUCTURE_TYPE, TEMPLATE, () -> BloodHeraldStructure.CODEC);
	public static final StructurePieceType PIECE_TYPE = Registry.register(
			BuiltInRegistries.STRUCTURE_PIECE, TEMPLATE, (StructurePieceType.StructureTemplateType) BloodHeraldPiece::new);

	private BloodHerald() {}

	public static void init() {
	}

	public static StructurePlaceSettings settings() {
		return new StructurePlaceSettings();
	}

	/** Разметка дерева: где ствол и где Кровосборники (в координатах файла). */
	public record Layout(BlockPos trunk, List<BlockPos> pumps, List<Direction> facings) {}

	private static volatile Layout cached;

	public static Layout layout(StructureTemplateManager manager) {
		Layout l = cached;
		if (l != null) return l;
		StructureTemplate template = manager.getOrCreate(TEMPLATE);
		l = compute(template);
		cached = l;
		return l;
	}

	private static Layout compute(StructureTemplate template) {
		StructurePlaceSettings s = settings();
		Set<Long> ground = new HashSet<>(); // занято у земли (y 0..3)
		long sx = 0, sz = 0, n = 0;
		for (Block b : new Block[]{ModBlocks.SCARLET.wood(), ModBlocks.SCARLET.log(), ModBlocks.SCARLET.strippedLog(),
				ModBlocks.SCARLET.strippedWood(), net.minecraft.world.level.block.Blocks.GLOWSTONE}) {
			for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, s, b)) {
				if (info.pos().getY() > 3) continue;
				ground.add(key(info.pos().getX(), info.pos().getZ()));
				if (info.pos().getY() == 0) {
					sx += info.pos().getX();
					sz += info.pos().getZ();
					n++;
				}
			}
		}
		var size = template.getSize();
		BlockPos trunk = n == 0 ? new BlockPos(size.getX() / 2, 0, size.getZ() / 2)
				: new BlockPos((int) (sx / n), 0, (int) (sz / n));

		List<BlockPos> pumps = new ArrayList<>();
		List<Direction> facings = new ArrayList<>();
		for (int i = 0; i < COLLECTORS; i++) {
			double angle = Math.PI / 4 + i * (Math.PI * 2 / COLLECTORS); // по диагоналям
			double dx = Math.cos(angle), dz = Math.sin(angle);
			Direction facing = Direction.getNearest(dx, 0, dz); // лицом от ствола
			Direction tankSide = com.tarkrakov.tearsofink.block.blood.BloodPumpBlock.tankSide(facing);
			int lastBlocked = 0;
			for (int r = 4; r < 90; r++) {
				int x = trunk.getX() + (int) Math.round(dx * r), z = trunk.getZ() + (int) Math.round(dz * r);
				if (blocked(ground, x, z) || blocked(ground, x + tankSide.getStepX(), z + tankSide.getStepZ())) {
					lastBlocked = r;
				} else if (r - lastBlocked > COLLECTOR_GAP) {
					pumps.add(new BlockPos(x, 0, z));
					facings.add(facing);
					break;
				}
			}
		}
		return new Layout(trunk, pumps, facings);
	}

	private static boolean blocked(Set<Long> ground, int x, int z) {
		for (int ox = -1; ox <= 1; ox++) {
			for (int oz = -1; oz <= 1; oz++) {
				if (ground.contains(key(x + ox, z + oz))) return true;
			}
		}
		return false;
	}

	private static long key(int x, int z) {
		return ((long) x << 32) ^ (z & 0xffffffffL);
	}
}
