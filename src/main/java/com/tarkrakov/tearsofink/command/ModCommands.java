package com.tarkrakov.tearsofink.command;

import com.tarkrakov.tearsofink.block.fountain.InkFountainBlock;
import com.tarkrakov.tearsofink.ink.InkData;
import com.tarkrakov.tearsofink.ink.InkPowers;
import com.tarkrakov.tearsofink.skill.Skills;
import com.tarkrakov.tearsofink.world.InkLandsAccess;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Команды для тестов (нужны права оператора):
 * /tearsofink where  — где центр Чернильных Земель и сколько до него
 * /tearsofink center — телепорт в центр
 * /tearsofink edge [1-5] — телепорт к краю, в начало одной из дорог (смотришь в сторону центра)
 * /tearsofink blood — телепорт в центр ближайшего пятна Кроволесья + проверка высот низины
 * /tearsofink powers — показать Душу и Чернила
 * /tearsofink powers reset — вернуть стартовые значения
 * /tearsofink soul max <50-100> — максимум Души (как будто продал душу)
 * /tearsofink soul set <число> — текущая Душа
 * /tearsofink ink bonus <число> — прибавка к запасу Чернил (как от фонтанов и сердец)
 * /tearsofink ink set <число> — текущие Чернила
 * /tearsofink skills — какие навыки выучены
 * /tearsofink fountains forget — забыть испитые фонтаны (запас Чернил не меняется)
 * /tearsofink skills forget — забыть все навыки (чтобы снова проверить книги)
 */
public final class ModCommands {
	private ModCommands() {}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("tearsofink")
						.requires(source -> source.hasPermission(2))
						.then(Commands.literal("where").executes(ModCommands::where))
						.then(Commands.literal("hunt")
								.then(Commands.literal("start").executes(ctx -> {
									var lands = com.tarkrakov.tearsofink.world.ModDimensions.lands(ctx.getSource().getServer());
									var p = ctx.getSource().getPlayer();
									if (lands == null || p == null || p.level() != lands) {
										ctx.getSource().sendFailure(Component.literal("Охота начинается только в Землях, рядом с вами."));
										return 0;
									}
									if (com.tarkrakov.tearsofink.hunt.Hunt.running()) {
										ctx.getSource().sendFailure(Component.literal("Охота уже идёт (/tearsofink hunt stop)."));
										return 0;
									}
									boolean ok = com.tarkrakov.tearsofink.hunt.Hunt.start(lands, p);
									if (!ok) ctx.getSource().sendFailure(Component.literal("Не нашлось места для цели — отойдите от храма или воды."));
									return ok ? 1 : 0;
								}))
								.then(com.tarkrakov.tearsofink.hunt.HuntTestCommand.build())
								.then(Commands.literal("stop").executes(ctx -> {
									var lands = com.tarkrakov.tearsofink.world.ModDimensions.lands(ctx.getSource().getServer());
									if (lands != null) com.tarkrakov.tearsofink.hunt.Hunt.stop(lands);
									ctx.getSource().sendSuccess(() -> Component.literal("Охота остановлена. Следующая — через 15 минут."), false);
									return 1;
								}))
								.then(Commands.literal("duel")
										.then(Commands.argument("per", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100)).executes(ctx -> {
											var lands = com.tarkrakov.tearsofink.world.ModDimensions.lands(ctx.getSource().getServer());
											if (lands == null) return 0;
											int n = com.tarkrakov.tearsofink.hunt.HuntDuels.start(lands,
													com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "per"));
											ctx.getSource().sendSuccess(() -> Component.literal("Испытательные бои: " + n + " (итог — в логе, [duel])."), false);
											return n;
										})))
								.then(Commands.literal("next").executes(ctx -> {
									long t = com.tarkrakov.tearsofink.hunt.Hunt.ticksUntilNext(ctx.getSource().getServer());
									ctx.getSource().sendSuccess(() -> Component.literal(com.tarkrakov.tearsofink.hunt.Hunt.running()
											? "Охота идёт сейчас." : "Следующая Охота примерно через " + (t / 20 / 60) + " мин " + (t / 20 % 60) + " с."), false);
									return 1;
								})))
						.then(Commands.literal("temples").executes(ctx -> {
							var lands = com.tarkrakov.tearsofink.world.ModDimensions.lands(ctx.getSource().getServer());
							InkLandsLayout layout = lands == null ? null : InkLandsLayout.of(lands);
							if (layout == null) return 0;
							StringBuilder sb = new StringBuilder("Храмы (первый — стартовый):");
							for (int[] t : com.tarkrakov.tearsofink.world.ruins.Ruins.templePoints(layout)) {
								sb.append("\n  X ").append(t[0]).append(", Z ").append(t[1]);
							}
							double[] a = com.tarkrakov.tearsofink.world.ModDimensions.arrivalXZ(layout);
							sb.append("\nМесто прибытия: X ").append((int) a[0]).append(", Z ").append((int) a[1]);
							ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
							return 1;
						}))
						.then(Commands.literal("enter").executes(ctx -> {
							ServerPlayer p = ctx.getSource().getPlayerOrException();
							com.tarkrakov.tearsofink.travel.Travel.enterTemple(p);
							return 1;
						}))
						.then(Commands.literal("leave").executes(ctx -> {
							ServerPlayer p = ctx.getSource().getPlayerOrException();
							var ow = ctx.getSource().getServer().overworld();
							var s = ow.getSharedSpawnPos();
							p.teleportTo(ow, s.getX() + 0.5, ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, s.getX(), s.getZ()), s.getZ() + 0.5, p.getYRot(), 0f);
							return 1;
						}))
						.then(Commands.literal("center").executes(ModCommands::center))
						.then(Commands.literal("blood").executes(ModCommands::blood))
						.then(Commands.literal("lake").executes(ModCommands::lake))
						.then(Commands.literal("replace")
								.then(Commands.argument("from", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
										.then(Commands.argument("to", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
												.then(Commands.argument("old", net.minecraft.commands.arguments.blocks.BlockStateArgument.block(registryAccess))
														.then(Commands.argument("new", net.minecraft.commands.arguments.blocks.BlockStateArgument.block(registryAccess))
																.executes(ModCommands::replace))))))
						.then(Commands.literal("edge")
								.executes(ctx -> edge(ctx, 1))
								.then(Commands.argument("road", IntegerArgumentType.integer(1, InkLandsLayout.ROADS))
										.executes(ctx -> edge(ctx, IntegerArgumentType.getInteger(ctx, "road")))))
						.then(Commands.literal("powers")
								.executes(ModCommands::powers)
								.then(Commands.literal("reset").executes(ctx -> change(ctx, d -> InkData.fresh().withInside(d.inside())))))
						.then(Commands.literal("fountains").then(Commands.literal("forget").executes(ctx -> {
							ctx.getSource().getPlayerOrException().setAttached(InkFountainBlock.VISITED, java.util.Set.of());
							ctx.getSource().sendSuccess(() -> Component.literal("Фонтаны забыты — их снова можно испить."), false);
							return 1;
						})))
						.then(Commands.literal("skills")
								.executes(ModCommands::skills)
								.then(Commands.literal("forget").executes(ctx -> {
									Skills.forgetAll(ctx.getSource().getPlayerOrException());
									return skills(ctx);
								})))
						.then(Commands.literal("vow")
								.executes(ModCommands::vow)
								.then(Commands.literal("ink").executes(ctx -> setVow(ctx, com.tarkrakov.tearsofink.vow.Vow.INK)))
								.then(Commands.literal("soul").executes(ctx -> setVow(ctx, com.tarkrakov.tearsofink.vow.Vow.SOUL)))
								.then(Commands.literal("reset").executes(ctx -> {
									com.tarkrakov.tearsofink.vow.Vows.reset(ctx.getSource().getPlayerOrException());
									com.tarkrakov.tearsofink.vow.Vows.sync(ctx.getSource().getPlayerOrException());
									return vow(ctx);
								}))
								.then(Commands.literal("choose").executes(ctx -> { // открыть окно выбора (сбрось обет перед этим)
									com.tarkrakov.tearsofink.vow.Vows.prompt(ctx.getSource().getPlayerOrException());
									return 1;
								}))
								.then(Commands.literal("awaken").then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word())
										.executes(ctx -> {
											boolean ok = com.tarkrakov.tearsofink.vow.Vows.awaken(ctx.getSource().getPlayerOrException(),
													com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "id"), com.tarkrakov.tearsofink.vow.Vows.AWAKENING);
											if (!ok) ctx.getSource().sendFailure(Component.literal("Не пробудилось: не Путь Души или это пробуждение уже было."));
											return vow(ctx);
										}))))
						.then(Commands.literal("soul")
								.then(Commands.literal("max").then(Commands.argument("value", IntegerArgumentType.integer(InkData.SOUL_MIN, InkData.SOUL_START))
										.executes(ctx -> change(ctx, d -> d.withSoulMax(IntegerArgumentType.getInteger(ctx, "value"))))))
								.then(Commands.literal("set").then(Commands.argument("value", IntegerArgumentType.integer(0, InkData.SOUL_START))
										.executes(ctx -> change(ctx, d -> d.withSoul(IntegerArgumentType.getInteger(ctx, "value")))))))
						.then(Commands.literal("ink")
								.then(Commands.literal("bonus").then(Commands.argument("value", IntegerArgumentType.integer(0, InkData.INK_CAP))
										.executes(ctx -> change(ctx, d -> d.withInkBonus(IntegerArgumentType.getInteger(ctx, "value"))))))
								.then(Commands.literal("set").then(Commands.argument("value", IntegerArgumentType.integer(0, InkData.INK_CAP))
										.executes(ctx -> change(ctx, d -> d.withInk(IntegerArgumentType.getInteger(ctx, "value")))))))));
	}

	private static int change(CommandContext<CommandSourceStack> ctx, java.util.function.UnaryOperator<InkData> fn) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		InkPowers.update(player, fn.apply(InkPowers.get(player)));
		return powers(ctx);
	}

	private static int setVow(CommandContext<CommandSourceStack> ctx, com.tarkrakov.tearsofink.vow.Vow v) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		p.setAttached(com.tarkrakov.tearsofink.vow.Vows.DATA, com.tarkrakov.tearsofink.vow.Vows.get(p).withVow(v));
		com.tarkrakov.tearsofink.vow.Vows.sync(p);
		return vow(ctx);
	}

	private static int vow(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		var v = com.tarkrakov.tearsofink.vow.Vows.get(p);
		InkData d = InkPowers.get(p);
		ctx.getSource().sendSuccess(() -> Component.literal("Обет: " + v.vow().id + " (смен: " + v.changes() + ")"
				+ "\nДуша: " + d.soulMax() + " (продано шагов " + d.sold() + ", сверх 100: " + d.extra() + ", из них обменом " + v.exchanged() + ")"
				+ "\nПробуждения: " + (v.awakenings().isEmpty() ? "—" : String.join(", ", v.awakenings()))), false);
		return 1;
	}

	private static int skills(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		var learned = Skills.learned(ctx.getSource().getPlayerOrException());
		ctx.getSource().sendSuccess(() -> learned.isEmpty()
				? Component.literal("Навыков пока нет.")
				: Component.literal("Выучено: " + String.join(", ", learned.stream().map(Object::toString).sorted().toList())), false);
		return 1;
	}

	private static int powers(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		InkData d = InkPowers.get(ctx.getSource().getPlayerOrException());
		ctx.getSource().sendSuccess(() -> Component.literal("Душа: " + (int) d.soul() + " / " + d.soulMax()
				+ " (продано шагов: " + d.soldSteps() + ", сверх 100: " + d.extra() + ")"
				+ "\nЧернила: " + (int) d.ink() + " / " + d.inkMax()
				+ " (прибавка от фонтанов/сердец: " + d.inkBonus() + ", потолок " + InkData.INK_CAP + ")"
				+ "\nСилы чернил: " + (d.inside() ? "пробуждены (ты в Чернильных Землях)" : "спят (ты снаружи)")), false);
		return 1;
	}

	private static InkLandsLayout findLayout(CommandSourceStack source) {
		ServerLevel overworld = com.tarkrakov.tearsofink.world.ModDimensions.lands(source.getServer());
		if (overworld.getChunkSource().getGenerator().getBiomeSource() instanceof InkLandsAccess access) {
			return access.tearsofink$layout(overworld.getChunkSource().randomState().sampler());
		}
		return null;
	}

	private static int edge(CommandContext<CommandSourceStack> ctx, int road) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		InkLandsLayout layout = findLayout(ctx.getSource());
		if (layout == null) {
			ctx.getSource().sendFailure(Component.literal("В этом мире нет Чернильных Земель."));
			return 0;
		}
		double[] arr = layout.arrivalPoint(road - 1);
		BlockPos start = BlockPos.containing(arr[0], 0, arr[1]);
		BlockPos c = layout.center();
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel overworld = com.tarkrakov.tearsofink.world.ModDimensions.lands(ctx.getSource().getServer());
		overworld.getChunk(start.getX() >> 4, start.getZ() >> 4);
		int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, start.getX(), start.getZ()) + 1;
		float yaw = (float) (Math.atan2(c.getZ() - start.getZ(), c.getX() - start.getX()) * 180.0 / Math.PI) - 90f;
		player.teleportTo(overworld, start.getX() + 0.5, y, start.getZ() + 0.5, yaw, 0f);
		ctx.getSource().sendSuccess(() -> Component.literal("Край Чернильных Земель, дорога " + road + ". Центр — прямо перед тобой."), false);
		return 1;
	}

	/**
	 * Замена одного блока на другой в коробке любого размера (для строительства; у /fill предел 32 768).
	 * Поворот (ось бревна и т.п.) переносится, если у нового блока он тоже есть.
	 */
	private static int replace(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerLevel level = ctx.getSource().getLevel();
		BlockPos a = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "from");
		BlockPos b = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "to");
		net.minecraft.world.level.block.Block oldBlock = net.minecraft.commands.arguments.blocks.BlockStateArgument.getBlock(ctx, "old").getState().getBlock();
		net.minecraft.world.level.block.state.BlockState newState = net.minecraft.commands.arguments.blocks.BlockStateArgument.getBlock(ctx, "new").getState();
		int count = 0;
		for (BlockPos p : BlockPos.betweenClosed(a, b)) {
			var state = level.getBlockState(p);
			if (!state.is(oldBlock)) continue;
			var placed = newState;
			for (var prop : state.getProperties()) placed = copyProperty(state, placed, prop);
			level.setBlock(p, placed, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
			count++;
		}
		int done = count;
		ctx.getSource().sendSuccess(() -> Component.literal("Заменено блоков: " + done), true);
		return done;
	}

	private static <T extends Comparable<T>> net.minecraft.world.level.block.state.BlockState copyProperty(
			net.minecraft.world.level.block.state.BlockState from, net.minecraft.world.level.block.state.BlockState to,
			net.minecraft.world.level.block.state.properties.Property<T> prop) {
		return to.hasProperty(prop) ? to.setValue(prop, from.getValue(prop)) : to;
	}

	/** Телепорт в центр Смоляного Озера (на воду). */
	private static int lake(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		InkLandsLayout layout = findLayout(ctx.getSource());
		if (layout == null) {
			ctx.getSource().sendFailure(Component.literal("В этом мире нет Чернильных Земель."));
			return 0;
		}
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel overworld = com.tarkrakov.tearsofink.world.ModDimensions.lands(ctx.getSource().getServer());
		int lx = layout.lakeCenter().getX(), lz = layout.lakeCenter().getZ();
		overworld.getChunk(lx >> 4, lz >> 4);
		int y = groundY(overworld, lx, lz) + 1;
		player.teleportTo(overworld, lx + 0.5, y, lz + 0.5, player.getYRot(), player.getXRot());
		ctx.getSource().sendSuccess(() -> Component.literal("Смоляное Озеро: " + lx + " " + y + " " + lz), false);
		return 1;
	}

	private static int blood(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		InkLandsLayout layout = findLayout(ctx.getSource());
		if (layout == null) {
			ctx.getSource().sendFailure(Component.literal("В этом мире нет Чернильных Земель."));
			return 0;
		}
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		long[] blob = layout.nearestBloodBlob(player.getBlockX(), player.getBlockZ());
		if (blob == null) {
			ctx.getSource().sendFailure(Component.literal("Рядом нет пятен Кроволесья."));
			return 0;
		}
		ServerLevel overworld = com.tarkrakov.tearsofink.world.ModDimensions.lands(ctx.getSource().getServer());
		int bx = (int) blob[0], bz = (int) blob[1];
		var generator = overworld.getChunkSource().getGenerator();
		var randomState = overworld.getChunkSource().randomState();
		java.util.function.IntBinaryOperator base = (x, z) ->
				generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, overworld, randomState) - 1;
		int natural = base.applyAsInt(bx, bz);
		int valley = (int) Math.floor(layout.valleyHeight(bx, bz, natural, base));
		overworld.getChunk(bx >> 4, bz >> 4);
		int actual = groundY(overworld, bx, bz);
		// для сравнения — земля за краем склона
		int outX = bx + (int) blob[2] + InkLandsLayout.VALLEY_SLOPE + 5;
		overworld.getChunk(outX >> 4, bz >> 4);
		int outside = groundY(overworld, outX, bz);
		player.teleportTo(overworld, bx + 0.5, actual + 1, bz + 0.5, player.getYRot(), player.getXRot());
		ctx.getSource().sendSuccess(() -> Component.literal("Кроволесье: центр X " + bx + ", Z " + bz + ", радиус " + blob[2]
				+ "\nВысота рельефа без низины: " + natural
				+ "\nВысота, которую задаёт низина: " + valley
				+ "\nРеальная земля в центре: " + actual
				+ "\nЗемля за краем склона: " + outside), false);
		return 1;
	}

	/** Высота настоящей земли: пропускаем стволы, листву, траву и прочее сверху. */
	private static int groundY(ServerLevel level, int x, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
		while (pos.getY() > level.getMinBuildHeight()) {
			pos.move(0, -1, 0);
			var state = level.getBlockState(pos);
			if (state.is(net.minecraft.tags.BlockTags.DIRT) || state.is(com.tarkrakov.tearsofink.block.ModBlocks.ASH)
					|| state.is(com.tarkrakov.tearsofink.block.ModBlocks.ASPHODELITE_ROCK)
					|| state.is(com.tarkrakov.tearsofink.block.ModBlocks.ASHEN_PATH) || !state.getFluidState().isEmpty()) {
				return pos.getY();
			}
		}
		return pos.getY();
	}

	private static BlockPos findCenter(CommandSourceStack source) {
		ServerLevel overworld = com.tarkrakov.tearsofink.world.ModDimensions.lands(source.getServer());
		if (overworld.getChunkSource().getGenerator().getBiomeSource() instanceof InkLandsAccess access) {
			InkLandsLayout layout = access.tearsofink$layout(overworld.getChunkSource().randomState().sampler());
			if (layout != null) return layout.center();
		}
		return null;
	}

	private static int where(CommandContext<CommandSourceStack> ctx) {
		BlockPos c = findCenter(ctx.getSource());
		if (c == null) {
			ctx.getSource().sendFailure(Component.literal("В этом мире нет Чернильных Земель."));
			return 0;
		}
		double dist = Math.sqrt(ctx.getSource().getPosition().distanceToSqr(c.getX(), ctx.getSource().getPosition().y, c.getZ()));
		var pos = ctx.getSource().getPosition();
		int edge = (int) InkLandsLayout.edgeDepth(pos.x, pos.z);
		ctx.getSource().sendSuccess(() -> Component.literal("Центр Чернильных Земель: X 0, Z 0 (до него ~" + (int) dist
				+ " блоков, радиус области ~" + InkLandsLayout.RADIUS + ", край рваный ±" + InkLandsLayout.EDGE_WOBBLE + ")"
				+ "\nДо края отсюда: " + (edge >= 0 ? "~" + edge + " блоков" : "ты за краем на " + (-edge) + " (петля на " + com.tarkrakov.tearsofink.world.InkLandsBorder.LOOP_DEPTH + ")")), false);
		return 1;
	}

	private static int center(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		BlockPos c = findCenter(ctx.getSource());
		if (c == null) {
			ctx.getSource().sendFailure(Component.literal("В этом мире нет Чернильных Земель."));
			return 0;
		}
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel overworld = com.tarkrakov.tearsofink.world.ModDimensions.lands(ctx.getSource().getServer());
		overworld.getChunk(c.getX() >> 4, c.getZ() >> 4);
		int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ()) + 1;
		player.teleportTo(overworld, c.getX() + 0.5, y, c.getZ() + 0.5, player.getYRot(), player.getXRot());
		ctx.getSource().sendSuccess(() -> Component.literal("Ты в сердце Чернильных Земель."), false);
		return 1;
	}
}
