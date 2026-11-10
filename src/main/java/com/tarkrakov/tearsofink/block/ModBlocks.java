package com.tarkrakov.tearsofink.block;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import java.util.Optional;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Все блоки мода. */
public final class ModBlocks {
	/** Пепельная земля: верхний слой Чернильных пустошей (как трава, но мёртвая). */
	public static final Block ASHEN_SOIL = register("ashen_soil", new Block(
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_GRAY)
					.strength(0.6f)
					.sound(SoundType.GRAVEL)));

	/** Пепел: сыпучий, падает как песок. */
	public static final Block ASH = register("ash", new ColoredFallingBlock(
			new ColorRGBA(0x4A4652FF),
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_LIGHT_GRAY)
					.strength(0.5f)
					.sound(SoundType.SAND)));

	// ===== Чернильное дерево =====
	public static final Block INK_LOG = register("ink_log", new RotatedPillarBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LOG).mapColor(MapColor.COLOR_BLACK)));
	public static final Block STRIPPED_INK_LOG = register("stripped_ink_log", new RotatedPillarBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_DARK_OAK_LOG).mapColor(MapColor.COLOR_BLACK)));
	public static final Block INK_WOOD = register("ink_wood", new RotatedPillarBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_WOOD).mapColor(MapColor.COLOR_BLACK)));
	public static final Block STRIPPED_INK_WOOD = register("stripped_ink_wood", new RotatedPillarBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_DARK_OAK_WOOD).mapColor(MapColor.COLOR_BLACK)));
	public static final Block INK_PLANKS = register("ink_planks", new Block(
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS).mapColor(MapColor.COLOR_BLACK)));
	public static final Block INK_LEAVES = register("ink_leaves", new InkLeavesBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LEAVES).mapColor(MapColor.COLOR_PURPLE)));

	/** Как растёт саженец: описание дерева лежит в data/tears-of-ink/worldgen/configured_feature/ink_tree.json */
	public static final ResourceKey<ConfiguredFeature<?, ?>> INK_TREE_FEATURE =
			ResourceKey.create(Registries.CONFIGURED_FEATURE, TearsOfInk.id("ink_tree"));
	public static final Block INK_SAPLING = register("ink_sapling", new SaplingBlock(
			new TreeGrower("ink", Optional.empty(), Optional.of(INK_TREE_FEATURE), Optional.empty()),
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_SAPLING)));

	private ModBlocks() {}

	private static Block register(String name, Block block) {
		Block registered = Registry.register(BuiltInRegistries.BLOCK, TearsOfInk.id(name), block);
		Registry.register(BuiltInRegistries.ITEM, TearsOfInk.id(name), new BlockItem(registered, new Item.Properties()));
		return registered;
	}

	/** Вызывается при запуске мода, чтобы блоки зарегистрировались. */
	public static void init() {
		// Обтёсывание топором
		StrippableBlockRegistry.register(INK_LOG, STRIPPED_INK_LOG);
		StrippableBlockRegistry.register(INK_WOOD, STRIPPED_INK_WOOD);
		// Горючесть (как у обычного дерева)
		FlammableBlockRegistry fire = FlammableBlockRegistry.getDefaultInstance();
		for (Block log : new Block[]{INK_LOG, STRIPPED_INK_LOG, INK_WOOD, STRIPPED_INK_WOOD}) fire.add(log, 5, 5);
		fire.add(INK_PLANKS, 5, 20);
		fire.add(INK_LEAVES, 30, 60);
		// Компостер
		CompostingChanceRegistry.INSTANCE.add(INK_LEAVES, 0.3f);
		CompostingChanceRegistry.INSTANCE.add(INK_SAPLING, 0.3f);
	}
}
