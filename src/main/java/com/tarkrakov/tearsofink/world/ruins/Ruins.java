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

	// ===== Храмы (в каждом — Малый чернильный фонтан: +5 к Чернилам навсегда) =====
	/** Сколько храмов на всё Бледнолесье. 7 фонтанов = +35 к запасу Чернил за все. */
	public static final int TEMPLES = 7;
	/** На какой глубине от края стоят храмы. */
	public static final int TEMPLE_MIN_DEPTH = 150, TEMPLE_MAX_DEPTH = 560;

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
		return depth < 250 ? 0 : depth < 450 ? 1 : 2;
	}

	private static final Map<InkLandsLayout, List<int[]>> TEMPLE_CACHE = new WeakHashMap<>();

	/** Места храмов (один раз на мир): по кругу, на разной глубине, в Бледнолесье, не у дорог, Озера и Вестников. */
	public static synchronized List<int[]> templePoints(InkLandsLayout layout) {
		List<int[]> cached = TEMPLE_CACHE.get(layout);
		if (cached != null) return cached;
		RandomSource random = RandomSource.create(layout.seed() ^ 0x7E3B1E5L);
		List<int[]> points = new ArrayList<>();
		double offset = random.nextDouble() * Math.PI * 2;
		for (int i = 0; i < TEMPLES; i++) {
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
		for (int[] p : templePoints(layout)) {
			if (Mth.square(p[0] - x) + Mth.square(p[1] - z) < TEMPLE_GAP * TEMPLE_GAP) return true;
		}
		return false;
	}
}
