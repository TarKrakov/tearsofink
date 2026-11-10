package com.tarkrakov.tearsofink.command;

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
 */
public final class ModCommands {
	private ModCommands() {}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("tearsofink")
						.requires(source -> source.hasPermission(2))
						.then(Commands.literal("where").executes(ModCommands::where))
						.then(Commands.literal("center").executes(ModCommands::center))
						.then(Commands.literal("edge")
								.executes(ctx -> edge(ctx, 1))
								.then(Commands.argument("road", IntegerArgumentType.integer(1, InkLandsLayout.ROADS))
										.executes(ctx -> edge(ctx, IntegerArgumentType.getInteger(ctx, "road")))))));
	}

	private static InkLandsLayout findLayout(CommandSourceStack source) {
		ServerLevel overworld = source.getServer().overworld();
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
		BlockPos start = layout.roadStart(road - 1);
		BlockPos c = layout.center();
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel overworld = ctx.getSource().getServer().overworld();
		overworld.getChunk(start.getX() >> 4, start.getZ() >> 4);
		int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, start.getX(), start.getZ()) + 1;
		float yaw = (float) (Math.atan2(c.getZ() - start.getZ(), c.getX() - start.getX()) * 180.0 / Math.PI) - 90f;
		player.teleportTo(overworld, start.getX() + 0.5, y, start.getZ() + 0.5, yaw, 0f);
		ctx.getSource().sendSuccess(() -> Component.literal("Край Чернильных Земель, дорога " + road + ". Центр — прямо перед тобой."), false);
		return 1;
	}

	private static BlockPos findCenter(CommandSourceStack source) {
		ServerLevel overworld = source.getServer().overworld();
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
		ctx.getSource().sendSuccess(() -> Component.literal("Центр Чернильных Земель: X " + c.getX() + ", Z " + c.getZ()
				+ " (до него ~" + (int) dist + " блоков, радиус области ~" + InkLandsLayout.RADIUS + ")"), false);
		return 1;
	}

	private static int center(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		BlockPos c = findCenter(ctx.getSource());
		if (c == null) {
			ctx.getSource().sendFailure(Component.literal("В этом мире нет Чернильных Земель."));
			return 0;
		}
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel overworld = ctx.getSource().getServer().overworld();
		overworld.getChunk(c.getX() >> 4, c.getZ() >> 4);
		int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ()) + 1;
		player.teleportTo(overworld, c.getX() + 0.5, y, c.getZ() + 0.5, player.getYRot(), player.getXRot());
		ctx.getSource().sendSuccess(() -> Component.literal("Ты в сердце Чернильных Земель."), false);
		return 1;
	}
}
