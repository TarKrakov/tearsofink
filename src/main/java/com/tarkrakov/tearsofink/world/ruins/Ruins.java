package com.tarkrakov.tearsofink.world.ruins;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModBiomes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Древние руины в Бледнолесье и Чернолесье (постройки автора: data/tears-of-ink/structure/ruin_*.nbt).
 * Метки в постройках: лазурит — «здесь может быть сундук»; сундуки и фонари автора остаются как есть;
 * ванильная земля в нижнем слое — заменяется почвой того места.
 */
public final class Ruins {
	// ===== Постройки =====
	public static final ResourceLocation TEMPLE = TearsOfInk.id("ruin_temple");
	public static final ResourceLocation HOUSE = TearsOfInk.id("ruin_house");
	public static final ResourceLocation REMAINS = TearsOfInk.id("ruin_remains");
	public static final ResourceLocation MONUMENT = TearsOfInk.id("ruin_monument");

	/** Обычные руины: какие и с каким весом (храмы — отдельно, ровно TEMPLES штук). */
	public static final ResourceLocation[] SCATTERED = {HOUSE, REMAINS, MONUMENT};
	public static final int[] SCATTERED_WEIGHTS = {40, 40, 20};
	/** Частота обычных руин — в structure_set/ruin_scattered.json (spacing: раз в N×N чанков). В Чернолесье часть попыток пропускается. */
	public static final float INKWOOD_SKIP = 0.5f;
	/** Наибольший перепад земли под руиной (блоки) — круче не ставим, чтобы не висела над обрывом. */
	public static final int MAX_SLOPE = 7;
	/** Насколько глубоко подсыпается фундамент под руиной. */
	public static final int FOUNDATION_DEPTH = 40;

	// ===== Храмы (в каждом — Малый чернильный фонтан: +5 к Чернилам навсегда) =====
	/** Сколько храмов на всё Бледнолесье. 7 фонтанов = +35 к запасу Чернил за все. */
	public static final int TEMPLES = 13; // 16 ноя: мир вчетверо больше по площади (было 7)
	/** На какой глубине от края стоят храмы. */
	public static final int TEMPLE_MIN_DEPTH = 150, TEMPLE_MAX_DEPTH = 1350;

	// ===== Стартовый храм: новички попадают в Земли на дорогу рядом с ним =====
	/** От осевой дороги до середины стартового храма (блоки). */
	public static final int START_SIDE = 26;
	/** Насколько место прибытия сдвинуто с дороги в сторону храма (храм — в START_SIDE − это блоков от игрока). */
	public static final int START_ARRIVAL_SHIFT = 10;
	/** Шанс, что лазурит стартового храма станет сундуком (отдельный стартовый лут). */
	public static final float START_CHEST_CHANCE = 0.7f;
	public static final ResourceLocation START_LOOT = TearsOfInk.id("chests/ruins_start");
	public static final ResourceLocation INKWOOD_LOOT = TearsOfInk.id("chests/ruins_inkwood");
	public static final ResourceLocation BLOOD_TEMPLE_LOOT = TearsOfInk.id("chests/temple_blood_supply");
	public static final ResourceLocation PALE_TEMPLE_REWARD = TearsOfInk.id("chests/temple_pale_reward");
	public static final ResourceLocation BLOOD_TEMPLE_REWARD = TearsOfInk.id("chests/temple_blood_reward");
	/** Радиус вокруг места прибытия, где не появляются патрули. */
	public static final int START_CALM_RADIUS = 64;

	// ===== Монумент — ориентир у дорог =====
	/** Шанс, что монумент встанет у дороги (если она есть в пределах MONUMENT_ROAD_SEARCH блоков). */
	public static final float MONUMENT_NEAR_ROAD = 0.6f;
	public static final int MONUMENT_ROAD_SEARCH = 64;
	/** Насколько от края полотна (блоки). */
	public static final int MONUMENT_ROAD_GAP_MIN = 5, MONUMENT_ROAD_GAP_MAX = 10;

	// ===== Сундуки на метках-лазуритах: по поясам глубины (как у патрулей) =====
	/** Шанс, что лазурит станет сундуком: окраина / лес / глубь (и Чернолесье). Хотя бы один сундук — всегда. */
	public static final float[] CHEST_CHANCE = {0.25f, 0.45f, 0.65f};
	public static final ResourceLocation[] CHEST_LOOT = {
			TearsOfInk.id("chests/ruins_poor"), TearsOfInk.id("chests/ruins_common"), TearsOfInk.id("chests/ruins_rich")};
	// Legacy gate_poor/common/rich tables are retained for unopened old chests.
	public static final ResourceLocation[] GATE_LOOT = {TearsOfInk.id("chests/gate_cache_poor"), TearsOfInk.id("chests/gate_cache_common"), TearsOfInk.id("chests/gate_cache_rich")};
	public static final ResourceLocation[] CAMP_LOOT = {TearsOfInk.id("chests/camp_cache_poor"), TearsOfInk.id("chests/camp_cache_common"), TearsOfInk.id("chests/camp_cache_rich")};
	public static final ResourceLocation[] WATCHTOWER_LOOT = {TearsOfInk.id("chests/watchtower_cache_poor"), TearsOfInk.id("chests/watchtower_cache_common"), TearsOfInk.id("chests/watchtower_cache_rich")};
	public static final ResourceLocation[] PALE_TEMPLE_SUPPLIES = {TearsOfInk.id("chests/temple_pale_supply_poor"), TearsOfInk.id("chests/temple_pale_supply_common"), TearsOfInk.id("chests/temple_pale_supply_rich")};
	public static final ResourceLocation GATE_SUPPLIES = TearsOfInk.id("chests/gate_supplies");
	public static final ResourceLocation CAMP_SUPPLIES = TearsOfInk.id("chests/camp_supplies");

	// ===== Ряска (только в Бледнолесье) =====
	/** Шанс, что у открытой стены руины повиснет ряска; и сколько ещё блоков она свисает вниз. */
	public static final float DUCKWEED_CHANCE = 0.12f;
	public static final int DUCKWEED_MAX_HANG = 2;

	// ===== Где нельзя =====
	public static final int ROAD_GAP = 8, LAKE_GAP = 30, HERALD_GAP = 50, TEMPLE_GAP = 60;

	public static final StructureType<RuinStructure> STRUCTURE_TYPE = Registry.register(
			BuiltInRegistries.STRUCTURE_TYPE, TearsOfInk.id("ruin"), () -> RuinStructure.CODEC);
	public static final StructurePieceType PIECE_TYPE = Registry.register(
			BuiltInRegistries.STRUCTURE_PIECE, TearsOfInk.id("ruin"), (StructurePieceType.StructureTemplateType) RuinPiece::new);

	private Ruins() {}

	public static void init() {
	}

	/** Пояс глубины (0 окраина, 1 лес, 2 глубь/Чернолесье) — как у патрулей. */
	public static int tier(InkLandsLayout layout, int x, int z) {
		double depth = layout.depthInside(x, z);
		return depth < com.tarkrakov.tearsofink.entity.patrol.SquadTuning.TIER1_DEPTH ? 0
				: depth < com.tarkrakov.tearsofink.entity.patrol.SquadTuning.TIER2_DEPTH ? 1 : 2;
	}

	private static final Map<InkLandsLayout, List<int[]>> TEMPLE_CACHE = new WeakHashMap<>();
	private static final Map<InkLandsLayout, Start> START_CACHE = new WeakHashMap<>();

	private record Start(double[] coordinates, int road) {}

	/**
	 * Стартовый храм и место прибытия (один раз на мир): {храм x, храм z, прибытие x, прибытие z}.
	 * Прибытие — на дороге у края Бледнолесья (как раньше), храм — сбоку от дороги.
	 */
	public static double[] start(InkLandsLayout layout) {
		return startSite(layout).coordinates();
	}

	public static int startRoad(InkLandsLayout layout) {
		return startSite(layout).road();
	}

	private static synchronized Start startSite(InkLandsLayout layout) {
		Start cached = START_CACHE.get(layout);
		if (cached != null) return cached;
		RandomSource random = RandomSource.create(layout.seed() ^ 0x51A27L);
		int first = random.nextInt(InkLandsLayout.ROADS);
		double firstSide = random.nextBoolean() ? 1 : -1;
		double[] result = null;
		int selectedRoad = first;
		for (int i = 0; i < InkLandsLayout.ROADS && result == null; i++) {
			int road = (first + i) % InkLandsLayout.ROADS;
			double[] a = layout.arrivalPoint(road);
			double along = Math.hypot(a[0] - layout.center().getX(), a[1] - layout.center().getZ());
			double[] dir = layout.roadDirection(road, along);
			for (double side : new double[]{firstSide, -firstSide}) {
				double tx = a[0] - dir[1] * side * START_SIDE, tz = a[1] + dir[0] * side * START_SIDE;
				int x = (int) Math.floor(tx), z = (int) Math.floor(tz);
				if (layout.pick(x, z, null) != ModBiomes.PALE_FOREST) continue;
				if (layout.roadNear(x, z, 18) != null || layout.nearLake(x, z, LAKE_GAP + 20) || layout.nearHerald(x, z, HERALD_GAP + 20)) continue;
				selectedRoad = road;
				result = new double[]{x, z, a[0] - dir[1] * side * START_ARRIVAL_SHIFT, a[1] + dir[0] * side * START_ARRIVAL_SHIFT};
				break;
			}
		}
		if (result == null) { // запасной вариант: первая дорога, как есть
			double[] a = layout.arrivalPoint(first);
			double along = Math.hypot(a[0] - layout.center().getX(), a[1] - layout.center().getZ());
			double[] dir = layout.roadDirection(first, along);
			result = new double[]{Math.floor(a[0] - dir[1] * firstSide * START_SIDE), Math.floor(a[1] + dir[0] * firstSide * START_SIDE), a[0], a[1]};
		}
		Start site = new Start(result, selectedRoad);
		START_CACHE.put(layout, site);
		return site;
	}

	public static boolean isStartTemple(InkLandsLayout layout, int x, int z) {
		double[] s = start(layout);
		return (int) s[0] == x && (int) s[1] == z;
	}

	/** Рядом с местом прибытия (там патрули не появляются). */
	public static boolean nearStart(InkLandsLayout layout, double x, double z) {
		double[] s = start(layout);
		return Mth.square(s[2] - x) + Mth.square(s[3] - z) < START_CALM_RADIUS * START_CALM_RADIUS
				|| Mth.square(s[0] - x) + Mth.square(s[1] - z) < START_CALM_RADIUS * START_CALM_RADIUS;
	}

	/**
	 * Места храмов (один раз на мир): первый — стартовый у места прибытия, остальные по кругу,
	 * на разной глубине, в Бледнолесье, не у дорог, Озера и Вестников.
	 */
	public static List<int[]> templePoints(InkLandsLayout layout) {
		if (!layout.hasTempleNetwork()) return legacyTemplePoints(layout);
		var network = layout.templeNetwork();
		if (network != null) return network.points();
		return startTemplePoints(layout);
	}

	static List<int[]> startTemplePoints(InkLandsLayout layout) {
		var starter = start(layout);
		return List.of(new int[]{(int) starter[0], (int) starter[1]});
	}

	private static synchronized List<int[]> legacyTemplePoints(InkLandsLayout layout) {
		List<int[]> cached = TEMPLE_CACHE.get(layout);
		if (cached != null) return cached;
		RandomSource random = RandomSource.create(layout.seed() ^ 0x7E3B1E5L);
		List<int[]> points = new ArrayList<>();
		double[] s = start(layout);
		points.add(new int[]{(int) s[0], (int) s[1]});
		double offset = Math.atan2(s[1] - layout.center().getZ(), s[0] - layout.center().getX());
		for (int i = 1; i < TEMPLES; i++) {
			for (int attempt = 0; attempt < 40; attempt++) {
				double angle = offset + (i + (random.nextDouble() - 0.5) * 0.7) * Math.PI * 2 / TEMPLES;
				double depth = Mth.lerp(random.nextDouble(), TEMPLE_MIN_DEPTH, TEMPLE_MAX_DEPTH);
				double r = InkLandsLayout.RADIUS - depth;
				for (int k = 0; k < 4; k++) { // край рваный — уточняем радиус по месту
					double d = InkLandsLayout.edgeDepth(Math.cos(angle) * r, Math.sin(angle) * r);
					r += d - depth;
				}
				int x = (int) Math.floor(Math.cos(angle) * r), z = (int) Math.floor(Math.sin(angle) * r);
				if (layout.pick(x, z, null) != ModBiomes.PALE_FOREST) continue;
				if (!clear(layout, x, z, 20)) continue;
				points.add(new int[]{x, z});
				break;
			}
		}
		TEMPLE_CACHE.put(layout, points);
		return points;
	}

	/** Свободно ли место для руины: не на дороге, не у Озера и Вестников. */
	public static boolean clear(InkLandsLayout layout, int x, int z, int halfSize) {
		return layout.roadNear(x, z, ROAD_GAP + halfSize) == null
				&& !layout.nearLake(x, z, LAKE_GAP + halfSize)
				&& !layout.nearHerald(x, z, HERALD_GAP + halfSize);
	}

	public static boolean nearTemple(InkLandsLayout layout, int x, int z) {
		return nearTemple(templePoints(layout), x, z);
	}

	static boolean nearLegacyTemple(InkLandsLayout layout, int x, int z) {
		return nearTemple(legacyTemplePoints(layout), x, z);
	}

	private static boolean nearTemple(List<int[]> points, int x, int z) {
		for (int[] p : points) {
			if (Mth.square(p[0] - x) + Mth.square(p[1] - z) < TEMPLE_GAP * TEMPLE_GAP) return true;
		}
		return false;
	}
}
