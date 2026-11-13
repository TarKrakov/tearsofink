package com.tarkrakov.tearsofink.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Сохранение построек автора в файл — без ограничения 48×48×48 структурного блока.
 * /tearsofink structure save <имя> <угол1> <угол2> — сохранить (воздух не сохраняется: при установке
 *     постройка не вырезает землю вокруг себя);
 * /tearsofink structure place <имя> — поставить сохранённое у себя под ногами (проверка).
 * Файл появляется в папке мира: generated/tears-of-ink/structure/<имя>.nbt — его кладём в мод.
 */
public final class StructureCommands {
	private StructureCommands() {}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("tearsofink")
						.requires(source -> source.hasPermission(2))
						.then(Commands.literal("structure")
								.then(Commands.literal("save")
										.then(Commands.argument("name", StringArgumentType.word())
												.then(Commands.argument("from", BlockPosArgument.blockPos())
														.then(Commands.argument("to", BlockPosArgument.blockPos())
																.executes(ctx -> save(ctx, null))
																// + блок, который НЕ сохранять (например, землю суперплоского мира)
																.then(Commands.argument("ignore", net.minecraft.commands.arguments.ResourceLocationArgument.id())
																		.suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggestResource(
																				net.minecraft.core.registries.BuiltInRegistries.BLOCK.keySet(), b))
																		.executes(ctx -> save(ctx, net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "ignore"))))))))
								.then(Commands.literal("place")
										.then(Commands.argument("name", StringArgumentType.word())
												.executes(StructureCommands::place))))));
	}

	private static ResourceLocation id(CommandContext<CommandSourceStack> ctx) {
		String name = StringArgumentType.getString(ctx, "name");
		return ResourceLocation.isValidPath(name) ? TearsOfInk.id(name) : null;
	}

	private static int save(CommandContext<CommandSourceStack> ctx, ResourceLocation ignoreId) throws CommandSyntaxException {
		ResourceLocation id = id(ctx);
		if (id == null) {
			ctx.getSource().sendFailure(Component.literal("Имя — только строчные латинские буквы, цифры и _"));
			return 0;
		}
		ServerLevel level = ctx.getSource().getLevel();
		BoundingBox box = BoundingBox.fromCorners(
				BlockPosArgument.getLoadedBlockPos(ctx, "from"), BlockPosArgument.getLoadedBlockPos(ctx, "to"));
		StructureTemplateManager manager = level.getStructureManager();
		StructureTemplate template = manager.getOrCreate(id);
		// «не сохранять» блок: на время сохранения он становится воздухом, потом возвращается на место
		java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> hidden = new java.util.HashMap<>();
		if (ignoreId != null) {
			var ignore = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(ignoreId).orElse(null);
			if (ignore == null) {
				ctx.getSource().sendFailure(Component.literal("Нет такого блока: " + ignoreId));
				return 0;
			}
			for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
				var st = level.getBlockState(p);
				if (st.is(ignore)) {
					hidden.put(p.immutable(), st);
					level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
				}
			}
		}
		template.fillFromWorld(level, new BlockPos(box.minX(), box.minY(), box.minZ()),
				new Vec3i(box.getXSpan(), box.getYSpan(), box.getZSpan()), false, Blocks.AIR);
		hidden.forEach((p, st) -> level.setBlock(p, st, 2 | 16));
		template.setAuthor(ctx.getSource().getTextName());
		if (!manager.save(id)) {
			ctx.getSource().sendFailure(Component.literal("Не удалось сохранить файл (см. лог)."));
			return 0;
		}
		Path file = level.getServer().getWorldPath(LevelResource.GENERATED_DIR).normalize()
				.resolve(id.getNamespace()).resolve("structure").resolve(id.getPath() + ".nbt");
		Vec3i size = template.getSize();
		ctx.getSource().sendSuccess(() -> Component.literal("Сохранено «" + id.getPath() + "» "
				+ size.getX() + "×" + size.getY() + "×" + size.getZ() + ": " + file.toAbsolutePath()), false);
		return 1;
	}

	private static int place(CommandContext<CommandSourceStack> ctx) {
		ResourceLocation id = id(ctx);
		ServerLevel level = ctx.getSource().getLevel();
		Optional<StructureTemplate> template = id == null ? Optional.empty() : level.getStructureManager().get(id);
		if (template.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("Нет такой постройки."));
			return 0;
		}
		BlockPos at = BlockPos.containing(ctx.getSource().getPosition());
		template.get().placeInWorld(level, at, at, new StructurePlaceSettings(), level.getRandom(), 2);
		ctx.getSource().sendSuccess(() -> Component.literal("Поставлено «" + id.getPath() + "» от " + at.toShortString()), false);
		return 1;
	}
}
