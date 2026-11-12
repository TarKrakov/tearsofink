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
						.then(Commands.literal("terrain").executes(ctx -> checkTerrain(ctx.getSource()))))));
	}

	private static void say(CommandSourceStack s, String msg) {
		TearsOfInk.LOGGER.info("[check] " + msg);
		s.sendSuccess(() -> Component.literal(msg), false);
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
			for (int yy = top; yy > top - 48; yy--) {
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
