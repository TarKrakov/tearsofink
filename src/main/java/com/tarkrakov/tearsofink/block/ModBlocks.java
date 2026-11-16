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
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import java.util.Optional;
import java.util.function.Function;
import com.tarkrakov.tearsofink.particle.ModParticles;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import com.tarkrakov.tearsofink.block.lantern.StoneLanternBlock;
import com.tarkrakov.tearsofink.block.fountain.InkFountainBlock;
import com.tarkrakov.tearsofink.alchemy.AlchemyTableBlock;
import com.tarkrakov.tearsofink.block.lantern.FogClearing;
import com.tarkrakov.tearsofink.block.lantern.InkLanternBlock;

/** Все блоки мода. */
public final class ModBlocks {
	/** Пепельная земля: верхний слой Чернильных пустошей (как трава, но мёртвая). */
	public static final Block ASHEN_SOIL = register("ashen_soil", new ZoneSoilBlock(
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_GRAY)
					.strength(0.6f)
					.sound(SoundType.GRAVEL), () -> ModBlocks.PALE_GRASS, () -> ModBlocks.TALL_PALE_GRASS));

	/** Пепельная дорога: утоптанный пепел магистралей. */
	public static final Block ASHEN_PATH = register("ashen_path", new Block(
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_GRAY)
					.strength(0.7f)
					.sound(SoundType.GRAVEL)));

	/** Земля Кроволесья — пропитана кровью. */
	public static final Block BLOOD_SOIL = register("blood_soil", new ZoneSoilBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.6f).sound(SoundType.GRAVEL),
			() -> ModBlocks.SCARLET_GRASS, () -> ModBlocks.TALL_SCARLET_GRASS));
	/** Земля Чернолесья — смоляная. */
	public static final Block TAR_SOIL = register("tar_soil", new ZoneSoilBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.6f).sound(SoundType.GRAVEL),
			() -> ModBlocks.INK_GRASS, () -> ModBlocks.TALL_INK_GRASS));

	/** Пепел: сыпучий, падает как песок. */
	public static final Block ASH = register("ash", new ColoredFallingBlock(
			new ColorRGBA(0x4A4652FF),
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_LIGHT_GRAY)
					.strength(0.5f)
					.sound(SoundType.SAND)));

	// ===== Бледное дерево =====
	public static final Block PALE_LOG = register("pale_log", new RotatedPillarBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LOG).mapColor(MapColor.COLOR_CYAN)));
	public static final Block STRIPPED_PALE_LOG = register("stripped_pale_log", new RotatedPillarBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_DARK_OAK_LOG).mapColor(MapColor.COLOR_CYAN)));
	public static final Block PALE_WOOD = register("pale_wood", new RotatedPillarBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_WOOD).mapColor(MapColor.COLOR_CYAN)));
	public static final Block STRIPPED_PALE_WOOD = register("stripped_pale_wood", new RotatedPillarBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_DARK_OAK_WOOD).mapColor(MapColor.COLOR_CYAN)));
	public static final Block PALE_PLANKS = register("pale_planks", new Block(
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS).mapColor(MapColor.COLOR_CYAN)));
	public static final Block PALE_LEAVES = register("pale_leaves", new LeavesBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LEAVES).mapColor(MapColor.COLOR_CYAN)));

	/** Как растёт саженец: описание дерева лежит в data/tears-of-ink/worldgen/configured_feature/pale_tree.json */
	public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_TREE_FEATURE =
			ResourceKey.create(Registries.CONFIGURED_FEATURE, TearsOfInk.id("pale_tree"));
	public static final Block PALE_SAPLING = register("pale_sapling", new SaplingBlock(
			new TreeGrower("pale", Optional.empty(), Optional.of(PALE_TREE_FEATURE), Optional.empty()),
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_SAPLING)));

	// ===== Столярка из бледного дерева =====
	/** Звуки дверей/кнопок и т. п. — как у тёмного дуба. */
	private static final BlockSetType PALE_SET = BlockSetType.DARK_OAK;
	private static final WoodType PALE_WOOD_TYPE = WoodType.DARK_OAK;

	public static final Block PALE_STAIRS = register("pale_stairs", new StairBlock(PALE_PLANKS.defaultBlockState(),
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_STAIRS).mapColor(MapColor.COLOR_CYAN)));
	public static final Block PALE_SLAB = register("pale_slab", new SlabBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_SLAB).mapColor(MapColor.COLOR_CYAN)));
	public static final Block PALE_FENCE = register("pale_fence", new FenceBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_FENCE).mapColor(MapColor.COLOR_CYAN)));
	public static final Block PALE_FENCE_GATE = register("pale_fence_gate", new FenceGateBlock(PALE_WOOD_TYPE,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_FENCE_GATE).mapColor(MapColor.COLOR_CYAN)));
	public static final Block PALE_DOOR = register("pale_door", new DoorBlock(PALE_SET,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_DOOR).mapColor(MapColor.COLOR_CYAN)));
	public static final Block PALE_TRAPDOOR = register("pale_trapdoor", new TrapDoorBlock(PALE_SET,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_TRAPDOOR).mapColor(MapColor.COLOR_CYAN)));
	public static final Block PALE_BUTTON = register("pale_button", new ButtonBlock(PALE_SET, 30,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_BUTTON)));
	public static final Block PALE_PRESSURE_PLATE = register("pale_pressure_plate", new PressurePlateBlock(PALE_SET,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PRESSURE_PLATE).mapColor(MapColor.COLOR_CYAN)));

	// ===== Багряное дерево (Кроволесье) и Смоляное дерево (Чернолесье) =====
	public static final WoodSet SCARLET = woodSet("scarlet", MapColor.COLOR_RED,
			props -> new DrippingLeavesBlock(() -> ModParticles.BLOOD_DRIP_HANG, props));
	/** Вторая листва Багряного дерева: золотисто-оранжевая (растёт пятнами вместе с красной). */
	public static final Block GOLDEN_SCARLET_LEAVES = register("golden_scarlet_leaves", new DrippingLeavesBlock(
			() -> ModParticles.BLOOD_DRIP_HANG,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LEAVES).mapColor(MapColor.GOLD)));
	public static final WoodSet INK = woodSet("ink", MapColor.COLOR_BLACK,
			props -> new DrippingLeavesBlock(() -> ModParticles.INK_DRIP_HANG, props));

	// ===== Растения и руды пустошей =====
	/** Яркость Слёзницы (0–15, факел = 14). */
	private static final int TEARBLOOM_LIGHT = 10;

	/** Слёзница: бледный цветок, выросший из слёз святой. Слабо светится. */
	public static final Block TEARBLOOM = register("tearbloom", new TearbloomBlock(MobEffects.REGENERATION, 5f,
			BlockBehaviour.Properties.ofFullCopy(Blocks.LILY_OF_THE_VALLEY).mapColor(MapColor.SNOW).lightLevel(s -> TEARBLOOM_LIGHT)));
	public static final Block POTTED_TEARBLOOM = registerNoItem("potted_tearbloom", new FlowerPotBlock(TEARBLOOM,
			BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_LILY_OF_THE_VALLEY).lightLevel(s -> TEARBLOOM_LIGHT)));

	/** Насколько ярко светится Асфоделит (0–15; факел = 14, светокамень = 15). */
	private static final int ASPHODELITE_LIGHT = 5;

	// ===== Трава зон (короткая + высокая) =====
	public static final Block TALL_PALE_GRASS = register("tall_pale_grass", tallGrass(MapColor.COLOR_CYAN));
	public static final Block PALE_GRASS = register("pale_grass", grass(MapColor.COLOR_CYAN, () -> ModBlocks.TALL_PALE_GRASS));
	public static final Block TALL_SCARLET_GRASS = register("tall_scarlet_grass", tallGrass(MapColor.COLOR_RED));
	public static final Block SCARLET_GRASS = register("scarlet_grass", grass(MapColor.COLOR_RED, () -> ModBlocks.TALL_SCARLET_GRASS));
	public static final Block TALL_INK_GRASS = register("tall_ink_grass", tallGrass(MapColor.COLOR_PURPLE));
	public static final Block INK_GRASS = register("ink_grass", grass(MapColor.COLOR_PURPLE, () -> ModBlocks.TALL_INK_GRASS));
	/** Тростник берегов (растёт только у воды). */
	public static final Block TALL_SHORE_REED = register("tall_shore_reed", tallGrass(MapColor.COLOR_GRAY));
	public static final Block SHORE_REED = register("shore_reed", grass(MapColor.COLOR_GRAY, () -> ModBlocks.TALL_SHORE_REED));

	private static Block grass(MapColor color, java.util.function.Supplier<Block> tall) {
		return new ZoneGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(color), tall);
	}

	private static Block tallGrass(MapColor color) {
		return new DoublePlantBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_GRASS).mapColor(color));
	}

	// ===== Травы Чернильных Земель =====
	/** Где ещё может расти Смолька (кроме земли): камень и кладка руин. */
	public static final TagKey<Block> TARWEED_GROWS_ON = TagKey.create(Registries.BLOCK, TearsOfInk.id("tarweed_grows_on"));

	/** Кровавик — Кроволесье: низкий круглый алый цветок на местах старых битв. */
	public static final Block BLOODROOT = register("bloodroot", herb(MobEffects.DAMAGE_BOOST, MapColor.COLOR_RED, 0, null));
	/** Плотеягода — Кроволесье: куст с большой круглой ягодой. */
	public static final Block FLESHBERRY = register("fleshberry", herb(MobEffects.SATURATION, MapColor.COLOR_RED, 0, null));
	/** Чернильник — у корней смоляных деревьев. */
	public static final Block INKROOT = register("inkroot", herb(MobEffects.BLINDNESS, MapColor.COLOR_BLACK, 0, null));
	/** Смолька — растёт в старых зданиях, на камне и кладке. */
	public static final Block TARWEED = register("tarweed", herb(MobEffects.POISON, MapColor.COLOR_BLACK, 0, TARWEED_GROWS_ON));
	/** Водяница — кустик с бледным светлым плодом на берегах. Плод слабо светится. */
	public static final Block WATERBLOOM = register("waterbloom", herb(MobEffects.WATER_BREATHING, MapColor.COLOR_LIGHT_BLUE, 4, null));
	/** Трясинник — плавает на воде Смоляного озера. Сам блок и есть материал («кувшинка Трясинника»). */
	public static final Block BOGLEAF = registerNoItem("bogleaf", new BogleafBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.LILY_PAD).mapColor(MapColor.COLOR_GREEN)));
	/** Ряска — лоза на деревьях и постройках. */
	public static final Block DUCKWEED = register("duckweed", new DuckweedVineBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.VINE).mapColor(MapColor.COLOR_CYAN)));

	private static Block herb(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, MapColor color, int light, TagKey<Block> extraSoil) {
		return new HerbBlock(effect, 5f, BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY).mapColor(color).lightLevel(s -> light), extraSoil);
	}

	/** Асфоделитовая порода: камень под Чернильными пустошами (вместо обычного камня выше y=0). */
	public static final Block ASPHODELITE_ROCK = register("asphodelite_rock", new Block(
			BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.COLOR_LIGHT_BLUE)));
	// ===== Древние руины (заглушки текстур — автор нарисует) =====
	public static final Block RUIN_BRICKS = register("ruin_bricks", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block CRACKED_RUIN_BRICKS = register("cracked_ruin_bricks", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block OVERGROWN_RUIN_BRICKS = register("overgrown_ruin_bricks", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block RUIN_TILES = register("ruin_tiles", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block CRACKED_RUIN_TILES = register("cracked_ruin_tiles", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block RUIN_PATTERN = register("ruin_pattern", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block CRACKED_RUIN_PATTERN = register("cracked_ruin_pattern", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block RUIN_PILLAR = register("ruin_pillar", new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block CRACKED_RUIN_PILLAR = register("cracked_ruin_pillar", new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block RUIN_BRICK_STAIRS = register("ruin_brick_stairs", new StairBlock(RUIN_BRICKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block RUIN_BRICK_SLAB = register("ruin_brick_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block RUIN_BRICK_WALL = register("ruin_brick_wall", new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY).forceSolidOn()));
	public static final Block CRACKED_RUIN_BRICK_STAIRS = register("cracked_ruin_brick_stairs", new StairBlock(CRACKED_RUIN_BRICKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block CRACKED_RUIN_BRICK_SLAB = register("cracked_ruin_brick_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block CRACKED_RUIN_BRICK_WALL = register("cracked_ruin_brick_wall", new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY).forceSolidOn()));
	public static final Block OVERGROWN_RUIN_BRICK_STAIRS = register("overgrown_ruin_brick_stairs", new StairBlock(OVERGROWN_RUIN_BRICKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block OVERGROWN_RUIN_BRICK_SLAB = register("overgrown_ruin_brick_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block OVERGROWN_RUIN_BRICK_WALL = register("overgrown_ruin_brick_wall", new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY).forceSolidOn()));
	public static final Block RUIN_TILE_SLAB = register("ruin_tile_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	public static final Block CRACKED_RUIN_TILE_SLAB = register("cracked_ruin_tile_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_LIGHT_GRAY)));

	/** Асфоделит: руда в асфоделитовой породе и глубинном сланце. Роняет Асфоду — материал для снаряжения. */
	public static final Block ASPHODELITE_ORE = register("asphodelite_ore", new DropExperienceBlock(UniformInt.of(2, 5),
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> ASPHODELITE_LIGHT)));
	public static final Block DEEPSLATE_ASPHODELITE_ORE = register("deepslate_asphodelite_ore", new DropExperienceBlock(UniformInt.of(2, 5),
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE).lightLevel(s -> ASPHODELITE_LIGHT)));

	// ===== Руды линий (16 ноя): копаешь — падает металл, металл в печь — слиток =====
	/** Бледная руда — под землёй Бледнолесья (железная кирка не нужна — каменной хватит). */
	public static final Block PALE_ORE = register("pale_ore", new DropExperienceBlock(UniformInt.of(0, 2),
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).mapColor(MapColor.COLOR_LIGHT_GRAY)));
	/** Кровавая руда — под низинами Кроволесья, неглубоко и гуще. */
	public static final Block BLOOD_ORE = register("blood_ore", new DropExperienceBlock(UniformInt.of(0, 2),
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).mapColor(MapColor.COLOR_RED)));
	/** Чернильная наковальня — ковка оружия (раскладка 3×3) и заточка за Пигмент. */
	public static final Block INK_ANVIL = register("ink_anvil", new com.tarkrakov.tearsofink.weapon.forge.InkAnvilBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE).mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(3.5f)));

	/** Проклятый алтарь — пробуждения оружия кровью и чернилами. */
	public static final Block CURSED_ALTAR = register("cursed_altar", new com.tarkrakov.tearsofink.weapon.forge.CursedAltarBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE).mapColor(MapColor.COLOR_RED).lightLevel(s -> 5)));

	// ===== Ванильные руды в асфоделитовой породе (лут — ванильный) =====
	public static final Block ASPHODELITE_COAL_ORE = register("asphodelite_coal_ore", new DropExperienceBlock(UniformInt.of(0, 2),
			BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_ORE).mapColor(MapColor.COLOR_LIGHT_BLUE)));
	public static final Block ASPHODELITE_IRON_ORE = register("asphodelite_iron_ore", new DropExperienceBlock(ConstantInt.of(0),
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).mapColor(MapColor.COLOR_LIGHT_BLUE)));
	public static final Block ASPHODELITE_COPPER_ORE = register("asphodelite_copper_ore", new DropExperienceBlock(ConstantInt.of(0),
			BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_ORE).mapColor(MapColor.COLOR_LIGHT_BLUE)));
	public static final Block ASPHODELITE_GOLD_ORE = register("asphodelite_gold_ore", new DropExperienceBlock(ConstantInt.of(0),
			BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_ORE).mapColor(MapColor.COLOR_LIGHT_BLUE)));
	public static final Block ASPHODELITE_REDSTONE_ORE = register("asphodelite_redstone_ore", new RedStoneOreBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_ORE).mapColor(MapColor.COLOR_LIGHT_BLUE)));
	public static final Block ASPHODELITE_LAPIS_ORE = register("asphodelite_lapis_ore", new DropExperienceBlock(UniformInt.of(2, 5),
			BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_ORE).mapColor(MapColor.COLOR_LIGHT_BLUE)));
	public static final Block ASPHODELITE_DIAMOND_ORE = register("asphodelite_diamond_ore", new DropExperienceBlock(UniformInt.of(3, 7),
			BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).mapColor(MapColor.COLOR_LIGHT_BLUE)));
	public static final Block ASPHODELITE_EMERALD_ORE = register("asphodelite_emerald_ore", new DropExperienceBlock(UniformInt.of(3, 7),
			BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_ORE).mapColor(MapColor.COLOR_LIGHT_BLUE)));

	/** Чернильная лужа: тонкий слой чернил, замедляет. */
	public static final Block INK_PUDDLE = register("ink_puddle", new InkPuddleBlock(
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_BLACK)
					.strength(0.2f)
					.noCollission()
					.noOcclusion()
					.replaceable()
					.speedFactor(0.55f)
					.jumpFactor(0.7f)
					.sound(SoundType.HONEY_BLOCK)
					.pushReaction(PushReaction.DESTROY)));
	/** Лужица из следа чернильной гусеницы — высыхает через 1–2 минуты (без предмета, в инвентаре не бывает). */
	public static final Block DRYING_INK_PUDDLE = Registry.register(BuiltInRegistries.BLOCK, TearsOfInk.id("drying_ink_puddle"),
			new DryingInkPuddleBlock(BlockBehaviour.Properties.ofFullCopy(INK_PUDDLE).noLootTable()));

	private ModBlocks() {}

	/** Создаёт и регистрирует полный набор блоков дерева с префиксом name (бревно, доски, дверь…). */
	private static WoodSet woodSet(String name, MapColor color, Function<BlockBehaviour.Properties, Block> leaves) {
		ResourceKey<ConfiguredFeature<?, ?>> tree = ResourceKey.create(Registries.CONFIGURED_FEATURE, TearsOfInk.id(name + "_tree"));
		Block planks = register(name + "_planks", new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS).mapColor(color)));
		return new WoodSet(
				register(name + "_log", new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LOG).mapColor(color))),
				register("stripped_" + name + "_log", new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_DARK_OAK_LOG).mapColor(color))),
				register(name + "_wood", new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_WOOD).mapColor(color))),
				register("stripped_" + name + "_wood", new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_DARK_OAK_WOOD).mapColor(color))),
				planks,
				register(name + "_leaves", leaves.apply(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LEAVES).mapColor(color))),
				register(name + "_sapling", new SaplingBlock(new TreeGrower(name, Optional.empty(), Optional.of(tree), Optional.empty()),
						BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_SAPLING))),
				register(name + "_stairs", new StairBlock(planks.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_STAIRS).mapColor(color))),
				register(name + "_slab", new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_SLAB).mapColor(color))),
				register(name + "_fence", new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_FENCE).mapColor(color))),
				register(name + "_fence_gate", new FenceGateBlock(WoodType.DARK_OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_FENCE_GATE).mapColor(color))),
				register(name + "_door", new DoorBlock(BlockSetType.DARK_OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_DOOR).mapColor(color))),
				register(name + "_trapdoor", new TrapDoorBlock(BlockSetType.DARK_OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_TRAPDOOR).mapColor(color))),
				register(name + "_button", new ButtonBlock(BlockSetType.DARK_OAK, 30, BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_BUTTON))),
				register(name + "_pressure_plate", new PressurePlateBlock(BlockSetType.DARK_OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PRESSURE_PLATE).mapColor(color))));
	}

	private static void woodSetBehaviour(WoodSet set) {
		StrippableBlockRegistry.register(set.log(), set.strippedLog());
		StrippableBlockRegistry.register(set.wood(), set.strippedWood());
		FlammableBlockRegistry fire = FlammableBlockRegistry.getDefaultInstance();
		for (Block log : new Block[]{set.log(), set.strippedLog(), set.wood(), set.strippedWood()}) fire.add(log, 5, 5);
		for (Block b : new Block[]{set.planks(), set.stairs(), set.slab(), set.fence(), set.fenceGate()}) fire.add(b, 5, 20);
		fire.add(set.leaves(), 30, 60);
		CompostingChanceRegistry.INSTANCE.add(set.leaves(), 0.3f);
		CompostingChanceRegistry.INSTANCE.add(set.sapling(), 0.3f);
	}

	// ===== Фонари: разгоняют мглу, светят, не дают появляться нашим врагам =====
	/** Радиус ясности (блоков) у каждого вида фонаря. */
	public static final int INK_LANTERN_RADIUS = 10, STONE_LANTERN_RADIUS = 18, ETERNAL_LANTERN_RADIUS = 14;

	private static BlockBehaviour.Properties lanternProps(int light) {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
				.lightLevel(s -> !s.hasProperty(FogClearing.LIT) || s.getValue(FogClearing.LIT) ? light : 0)
				.explosionResistance(1200f)
				.pushReaction(PushReaction.BLOCK);
	}

	/** Чернильный фонарь: малый, ставится на землю/забор или подвешивается. Можно скрафтить. */
	public static final Block INK_LANTERN = register("ink_lantern", new InkLanternBlock(INK_LANTERN_RADIUS, false, lanternProps(14)));
	/** Каменный фонарь (модель автора): большой, в два блока; огонь — танцующая частица. Можно скрафтить. */
	public static final Block STONE_LANTERN = register("stone_lantern", new StoneLanternBlock(STONE_LANTERN_RADIUS,
			lanternProps(15).sound(SoundType.STONE).noOcclusion()
					.lightLevel(s -> s.getValue(StoneLanternBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER
							&& s.getValue(FogClearing.LIT) ? 15 : 0)));
	/** Вечный фонарь: сюжетные места; горит всегда, не ломается, не крафтится. */
	public static final Block ETERNAL_LANTERN = register("eternal_lantern", new InkLanternBlock(ETERNAL_LANTERN_RADIUS, true,
			lanternProps(15).strength(-1f, 3600000f)));

	/** Малый чернильный фонтан: новый фонтан навсегда даёт +5 к запасу Чернил. Мировой, не ломается. */
	public static final Block INK_FOUNTAIN = register("ink_fountain", new InkFountainBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.COLOR_BLACK)
					.strength(-1f, 3600000f).noOcclusion().lightLevel(s -> 5).pushReaction(PushReaction.BLOCK)));

	/** Алхимический стол: варка зелий мода (модель автора — позже). */
	public static final Block ALCHEMY_TABLE = register("alchemy_table", new AlchemyTableBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).mapColor(MapColor.COLOR_GRAY)));

	// ===== Кровосборник (у корней Багряного Вестника) =====
	/** Помпа: два блока, модель автора с анимацией; качает кровь в Накопитель. Мировая, не ломается. */
	public static final Block BLOOD_PUMP = register("blood_pump", new com.tarkrakov.tearsofink.block.blood.BloodPumpBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_RED)
					.strength(-1f, 3600000f).noOcclusion().pushReaction(PushReaction.BLOCK)));
	/** Накопитель: копит кровь; ПКМ — забрать Сгустки. Мировой, не ломается. */
	public static final Block BLOOD_TANK = register("blood_tank", new com.tarkrakov.tearsofink.block.blood.BloodTankBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_RED)
					.strength(-1f, 3600000f).noOcclusion().pushReaction(PushReaction.BLOCK)));

	/** Сердце Багряного Вестника: невидимый маячок в стволе (туман реже, кровь с кроны). Ставит генерация. */
	public static final Block HERALD_HEART = registerNoItem("herald_heart", new com.tarkrakov.tearsofink.world.herald.HeraldHeartBlock(
			BlockBehaviour.Properties.of().strength(-1f, 3600000f).noCollission().noLootTable().noOcclusion()
					.pushReaction(PushReaction.BLOCK)));

	/** Предмет-блок для Трясинника (ставится на воду, как кувшинка). Вызывается из ModItems. */
	public static Item bogleafItem() {
		return Registry.register(BuiltInRegistries.ITEM, TearsOfInk.id("bogleaf"), new PlaceOnWaterBlockItem(BOGLEAF, new Item.Properties()));
	}

	/** Пелена в воротах Преддверия. */
	public static final Block VESTIBULE_GATE = registerNoItem("vestibule_gate", new com.tarkrakov.tearsofink.prologue.VestibuleGateBlock(
			BlockBehaviour.Properties.of().strength(-1f, 3600000f).noCollission().noLootTable().noOcclusion()
					.lightLevel(s -> 11).sound(net.minecraft.world.level.block.SoundType.GLASS).pushReaction(PushReaction.BLOCK)));

	private static Block registerNoItem(String name, Block block) {
		return Registry.register(BuiltInRegistries.BLOCK, TearsOfInk.id(name), block);
	}

	private static Block register(String name, Block block) {
		Block registered = Registry.register(BuiltInRegistries.BLOCK, TearsOfInk.id(name), block);
		Registry.register(BuiltInRegistries.ITEM, TearsOfInk.id(name), new BlockItem(registered, new Item.Properties()));
		return registered;
	}

	/** Вызывается при запуске мода, чтобы блоки зарегистрировались. */
	public static void init() {
		// Обтёсывание топором
		StrippableBlockRegistry.register(PALE_LOG, STRIPPED_PALE_LOG);
		StrippableBlockRegistry.register(PALE_WOOD, STRIPPED_PALE_WOOD);
		// Горючесть (как у обычного дерева)
		FlammableBlockRegistry fire = FlammableBlockRegistry.getDefaultInstance();
		for (Block log : new Block[]{PALE_LOG, STRIPPED_PALE_LOG, PALE_WOOD, STRIPPED_PALE_WOOD}) fire.add(log, 5, 5);
		fire.add(PALE_PLANKS, 5, 20);
		for (Block b : new Block[]{PALE_STAIRS, PALE_SLAB, PALE_FENCE, PALE_FENCE_GATE}) fire.add(b, 5, 20);
		fire.add(PALE_LEAVES, 30, 60);
		// Компостер
		CompostingChanceRegistry.INSTANCE.add(PALE_LEAVES, 0.3f);
		CompostingChanceRegistry.INSTANCE.add(PALE_SAPLING, 0.3f);
		CompostingChanceRegistry.INSTANCE.add(TEARBLOOM, 0.65f);
		woodSetBehaviour(SCARLET);
		FlammableBlockRegistry.getDefaultInstance().add(GOLDEN_SCARLET_LEAVES, 30, 60);
		CompostingChanceRegistry.INSTANCE.add(GOLDEN_SCARLET_LEAVES, 0.3f);
		woodSetBehaviour(INK);
		for (Block herb : new Block[]{BLOODROOT, FLESHBERRY, INKROOT, TARWEED, WATERBLOOM, BOGLEAF, DUCKWEED}) {
			CompostingChanceRegistry.INSTANCE.add(herb, 0.65f);
		}
		FlammableBlockRegistry.getDefaultInstance().add(DUCKWEED, 15, 100);
		for (Block g : new Block[]{PALE_GRASS, TALL_PALE_GRASS, SCARLET_GRASS, TALL_SCARLET_GRASS, INK_GRASS, TALL_INK_GRASS, SHORE_REED, TALL_SHORE_REED}) {
			FlammableBlockRegistry.getDefaultInstance().add(g, 60, 100);
			CompostingChanceRegistry.INSTANCE.add(g, 0.3f);
		}
	}
}
