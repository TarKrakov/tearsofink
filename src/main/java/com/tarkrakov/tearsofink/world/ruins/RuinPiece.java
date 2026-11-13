package com.tarkrakov.tearsofink.world.ruins;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Одна руина: постройка автора + сундуки на метках-лазуритах (по поясу глубины), лут в сундуках автора,
 * почва вместо ванильной земли, фундамент на склонах, ряска на стенах (только в Бледнолесье).
 * Всё «случайное» здесь зависит только от места руины — одинаково в каждом чанке, через который она проходит.
 */
public class RuinPiece extends TemplateStructurePiece {
	private final Rotation rotation;
	private final int tier;

	public RuinPiece(StructureTemplateManager manager, ResourceLocation template, BlockPos center, Rotation rotation, int tier) {
		super(Ruins.PIECE_TYPE, 0, manager, template, template.toString(), settings(rotation, manager, template), corner(manager, template, center, rotation));
		this.rotation = rotation;
		this.tier = tier;
	}

	public RuinPiece(StructureTemplateManager manager, CompoundTag tag) {
		super(Ruins.PIECE_TYPE, tag, manager, id -> settings(Rotation.valueOf(tag.getString("Rot")), manager, id));
		this.rotation = Rotation.valueOf(tag.getString("Rot"));
		this.tier = tag.getInt("Tier");
	}

	private static StructurePlaceSettings settings(Rotation rotation, StructureTemplateManager manager, ResourceLocation id) {
		var size = manager.getOrCreate(id).getSize();
		return new StructurePlaceSettings().setRotation(rotation)
				.setRotationPivot(new BlockPos(size.getX() / 2, 0, size.getZ() / 2))
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
	}

	/** Угол постройки так, чтобы её середина встала в center (поворот — вокруг середины). */
	private static BlockPos corner(StructureTemplateManager manager, ResourceLocation id, BlockPos center, Rotation rotation) {
		var size = manager.getOrCreate(id).getSize();
		return center.offset(-size.getX() / 2, 0, -size.getZ() / 2);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putString("Rot", rotation.name());
		tag.putInt("Tier", tier);
	}

	@Override
	protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
							BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
		super.postProcess(level, structures, generator, random, box, chunkPos, pivot);
		RandomSource fixed = RandomSource.create(templatePosition.asLong() * 31 + 0x5EED);
		ResourceKey<LootTable> loot = ResourceKey.create(Registries.LOOT_TABLE, Ruins.CHEST_LOOT[tier]);

		// --- сундуки: метки-лазуриты (решение одинаковое для всех чанков руины)
		List<StructureTemplate.StructureBlockInfo> marks = template.filterBlocks(templatePosition, placeSettings, Blocks.LAPIS_BLOCK);
		List<StructureTemplate.StructureBlockInfo> authorChests = template.filterBlocks(templatePosition, placeSettings, Blocks.CHEST);
		boolean[] chest = new boolean[marks.size()];
		boolean any = !authorChests.isEmpty();
		for (int i = 0; i < marks.size(); i++) {
			chest[i] = fixed.nextFloat() < Ruins.CHEST_CHANCE[tier];
			any |= chest[i];
		}
		if (!any && !marks.isEmpty()) chest[fixed.nextInt(marks.size())] = true; // хотя бы один сундук
		for (int i = 0; i < marks.size(); i++) {
			BlockPos pos = marks.get(i).pos();
			Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(fixed);
			if (!box.isInside(pos)) continue;
			if (chest[i]) {
				level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
				net.minecraft.world.RandomizableContainer.setBlockEntityLootTable(level, fixed, pos, loot);
			} else {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
			}
		}
		// сундуки автора — с тем же лутом
		for (StructureTemplate.StructureBlockInfo info : authorChests) {
			if (box.isInside(info.pos())) net.minecraft.world.RandomizableContainer.setBlockEntityLootTable(level, fixed, info.pos(), loot);
		}

		// --- почва вместо ванильной земли + фундамент на склонах
		for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(templatePosition, placeSettings, Blocks.DIRT)) {
			BlockPos pos = info.pos();
			if (!box.isInside(pos)) continue;
			BlockState soil = soilAt(level, pos);
			level.setBlock(pos, soil, 2);
		}
		BoundingBox bb = getBoundingBox();
		int baseY = templatePosition.getY();
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		for (int x = Math.max(bb.minX(), box.minX()); x <= Math.min(bb.maxX(), box.maxX()); x++) {
			for (int z = Math.max(bb.minZ(), box.minZ()); z <= Math.min(bb.maxZ(), box.maxZ()); z++) {
				BlockState floor = level.getBlockState(m.set(x, baseY, z));
				if (floor.isAir() || !floor.isSolid()) continue;
				BlockState soil = soilAt(level, m);
				for (int y = baseY - 1, n = 0; n < 16; y--, n++) {
					BlockState below = level.getBlockState(m.set(x, y, z));
					if (!below.canBeReplaced() && below.getFluidState().isEmpty()) break;
					level.setBlock(m, n < 2 ? soil : ModBlocks.ASPHODELITE_ROCK.defaultBlockState(), 2);
				}
			}
		}

		// --- ряска по стенам (только Бледнолесье)
		if (level.getBiome(templatePosition).is(ModBiomes.PALE_FOREST)) {
			List<BlockPos> hang = new ArrayList<>();
			for (int x = Math.max(bb.minX(), box.minX()); x <= Math.min(bb.maxX(), box.maxX()); x++) {
				for (int z = Math.max(bb.minZ(), box.minZ()); z <= Math.min(bb.maxZ(), box.maxZ()); z++) {
					for (int y = baseY + 1; y <= bb.maxY(); y++) {
						m.set(x, y, z);
						if (!level.getBlockState(m).isAir()) continue;
						RandomSource r = RandomSource.create(m.asLong() ^ 0xD0C3EEDL);
						if (r.nextFloat() >= Ruins.DUCKWEED_CHANCE) continue;
						BlockState vine = vineFor(level, m);
						if (vine == null) continue;
						level.setBlock(m, vine, 2);
						int extra = r.nextInt(Ruins.DUCKWEED_MAX_HANG + 1);
						for (int k = 1; k <= extra; k++) {
							BlockPos down = m.below(k);
							if (!box.isInside(down) || !level.getBlockState(down).isAir()) break;
							level.setBlock(down, vine, 2);
						}
					}
				}
			}
		}
	}

	/** Ряска, прилипшая к соседней стене руины (или null, если стены рядом нет). */
	private static BlockState vineFor(WorldGenLevel level, BlockPos pos) {
		BlockState vine = ModBlocks.DUCKWEED.defaultBlockState();
		boolean any = false;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockPos n = pos.relative(d);
			BlockState s = level.getBlockState(n);
			if (isRuin(s) && s.isFaceSturdy(level, n, d.getOpposite())) {
				vine = vine.setValue(VineBlock.getPropertyForFace(d), true);
				any = true;
			}
		}
		return any ? vine : null;
	}

	private static boolean isRuin(BlockState s) {
		var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock());
		return id.getNamespace().equals("tears-of-ink") && id.getPath().contains("ruin");
	}

	private static BlockState soilAt(WorldGenLevel level, BlockPos pos) {
		var biome = level.getBiome(pos);
		Block b = biome.is(ModBiomes.INKWOOD) ? ModBlocks.TAR_SOIL : ModBlocks.ASHEN_SOIL;
		return b.defaultBlockState();
	}
}
