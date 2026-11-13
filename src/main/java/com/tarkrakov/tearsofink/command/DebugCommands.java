package com.tarkrakov.tearsofink.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/** Проверки генерации (для автотеста на сервере): /tearsofink check roads | terrain. */
public final class DebugCommands {
	private DebugCommands() {}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> dispatcher.register(Commands.literal("tearsofink")
				.requires(s -> s.hasPermission(2))
				.then(Commands.literal("check")
						.then(Commands.literal("roads").then(Commands.argument("road", IntegerArgumentType.integer(1, InkLandsLayout.ROADS))
								.executes(ctx -> checkRoad(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "road") - 1, 100000))
								.then(Commands.argument("length", IntegerArgumentType.integer(16, 100000))
										.executes(ctx -> checkRoad(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "road") - 1,
												IntegerArgumentType.getInteger(ctx, "length"))))))
						.then(Commands.literal("terrain").executes(ctx -> checkTerrain(ctx.getSource())))
						.then(Commands.literal("start").executes(ctx -> checkStart(ctx.getSource()))))));
	}

	private static void say(CommandSourceStack s, String msg) {
		TearsOfInk.LOGGER.info("[check] " + msg);
		s.sendSuccess(() -> Component.literal(msg), false);
	}

	/** Стартовый храм: где должен стоять, что о нём думает генерация и что реально построено. */
	private static int checkStart(CommandSourceStack src) {
		ServerLevel level = ModDimensions.lands(src.getServer());
		InkLandsLayout layout = level == null ? null : InkLandsLayout.of(level);
		if (layout == null) { say(src, "нет измерения"); return 0; }
		double[] s = com.tarkrakov.tearsofink.world.ruins.Ruins.start(layout);
		int tx = (int) s[0], tz = (int) s[1];
		say(src, "Старт: храм X " + tx + " Z " + tz + ", прибытие X " + (int) s[2] + " Z " + (int) s[3]
				+ "; биом по раскладке " + layout.pick(tx, tz, null) + ", глубина " + (int) layout.depthInside(tx, tz)
				+ ", дорога рядом " + (layout.roadNear(tx, tz, 18) != null) + ", первый в списке храмов "
				+ java.util.Arrays.toString(com.tarkrakov.tearsofink.world.ruins.Ruins.templePoints(layout).get(0)));
		for (int cx = -2; cx <= 2; cx++) for (int cz = -2; cz <= 2; cz++) level.getChunk((tx >> 4) + cx, (tz >> 4) + cz);
		var registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
		var temple = registry.get(TearsOfInk.id("ruin_temple"));
		var start = temple == null ? null : level.structureManager().getStructureAt(new BlockPos(tx, 100, tz), temple);
		boolean startFound = false;
		for (int cx = -1; cx <= 1 && !startFound; cx++) for (int cz = -1; cz <= 1 && !startFound; cz++) {
			var chunk = level.getChunk((tx >> 4) + cx, (tz >> 4) + cz);
			if (temple != null && chunk.getStartForStructure(temple) != null && chunk.getStartForStructure(temple).isValid()) startFound = true;
		}
		int fountains = 0, ruin = 0, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		for (int x = tx - 24; x <= tx + 24; x++) for (int z = tz - 24; z <= tz + 24; z++) for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
			BlockState b = level.getBlockState(m.set(x, y, z));
			if (b.is(ModBlocks.INK_FOUNTAIN)) fountains++;
			var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b.getBlock());
			if (id.getNamespace().equals(TearsOfInk.MOD_ID) && id.getPath().contains("ruin")) { ruin++; minY = Math.min(minY, y); maxY = Math.max(maxY, y); }
		}
		say(src, "Старт: структура " + (temple != null) + ", начало в чанке храма " + startFound + ", кусок в точке " + (start != null && start.isValid())
				+ "; блоков руин " + ruin + (ruin > 0 ? " (Y " + minY + "…" + maxY + ")" : "") + ", фонтанов " + fountains
				+ ", земля в точке Y " + level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz));
		return 1;
	}

	private static int checkRoad(CommandSourceStack src, int road, int length) {
		ServerLevel level = ModDimensions.lands(src.getServer());
		InkLandsLayout layout = level == null ? null : InkLandsLayout.of(level);
		if (layout == null) { say(src, "нет измерения"); return 0; }
		int missing = 0, steps = 0, blocked = 0, bridge = 0, n = 0, prevY = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
		List<String> examples = new ArrayList<>();
		double[] start = layout.arrivalPoint(road);
		double dStart = Math.sqrt(start[0] * start[0] + start[1] * start[1]);
		long t0 = System.currentTimeMillis();
		for (double d = dStart; d > InkLandsLayout.ROAD_END_RADIUS + 4 && dStart - d < length; d -= 1) {
			if (((int) (dStart - d)) % 64 == 0) TearsOfInk.LOGGER.info("[check] … дорога " + (road + 1) + ": " + (int) (dStart - d) + " блоков, " + (System.currentTimeMillis() - t0) / 1000 + " с");
			double[] p = layout.roadPoint(road, d);
			int x = (int) Math.floor(p[0]), z = (int) Math.floor(p[1]);
			level.getChunk(x >> 4, z >> 4);
			int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1;
			int y = Integer.MIN_VALUE;
			boolean planks = false;
			for (int yy = top; yy > level.getMinBuildHeight(); yy--) {
				BlockState s = level.getBlockState(new BlockPos(x, yy, z));
				if (s.is(ModBlocks.ASHEN_PATH) || s.is(ModBlocks.PALE_PLANKS)) { y = yy; planks = s.is(ModBlocks.PALE_PLANKS); break; }
			}
			n++;
			if (y == Integer.MIN_VALUE) {
				missing++;
				if (examples.size() < 12) examples.add("нет полотна " + x + " " + z);
				prevY = Integer.MIN_VALUE;
				continue;
			}
			if (planks) bridge++;
			minY = Math.min(minY, y); maxY = Math.max(maxY, y);
			if (prevY != Integer.MIN_VALUE && Math.abs(y - prevY) > 1) {
				steps++;
				if (examples.size() < 12) examples.add("ступень " + prevY + "→" + y + " у " + x + " " + z);
			}
			for (int h = 1; h <= 2; h++) {
				BlockPos above = new BlockPos(x, y + h, z);
				if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
					blocked++;
					if (examples.size() < 12) examples.add("завал над полотном " + x + " " + (y + h) + " " + z + " " + level.getBlockState(above).getBlock());
					break;
				}
			}
			prevY = y;
		}
		say(src, "Дорога " + (road + 1) + ": точек " + n + ", без полотна " + missing + ", ступеней >1 " + steps
				+ ", завалов " + blocked + ", мостом " + bridge + ", высота " + minY + "…" + maxY);
		for (String e : examples) say(src, "  " + e);
		return 1;
	}

	private static int checkTerrain(CommandSourceStack src) {
		ServerLevel level = ModDimensions.lands(src.getServer());
		InkLandsLayout layout = level == null ? null : InkLandsLayout.of(level);
		if (layout == null) { say(src, "нет измерения"); return 0; }
		var gen = level.getChunkSource().getGenerator();
		var rs = level.getChunkSource().randomState();
		int total = 0, deep = 0, pale = 0, blood = 0, ink = 0, lake = 0;
		int step = 40, r = InkLandsLayout.RADIUS;
		for (int x = -r; x <= r; x += step) {
			for (int z = -r; z <= r; z += step) {
				if (layout.depthInside(x, z) < 0) continue;
				total++;
				int floor = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
				if (floor < gen.getSeaLevel() - 3) deep++;
				var key = layout.pick(x, z, null);
				if (key == com.tarkrakov.tearsofink.world.ModBiomes.PALE_FOREST) pale++;
				else if (key == com.tarkrakov.tearsofink.world.ModBiomes.BLOODWOOD) blood++;
				else if (key == com.tarkrakov.tearsofink.world.ModBiomes.INKWOOD) ink++;
				else if (key == com.tarkrakov.tearsofink.world.ModBiomes.TAR_LAKE) lake++;
			}
		}
		say(src, "Рельеф: точек " + total + ", глубже 3 под уровнем моря (до Озера) " + deep
				+ "; Бледнолесье " + pale + ", Кроволесье " + blood + ", Чернолесье " + ink + ", Озеро " + lake
				+ "; Озеро в " + layout.lakeCenter().getX() + " " + layout.lakeCenter().getZ());
		return 1;
	}
}
