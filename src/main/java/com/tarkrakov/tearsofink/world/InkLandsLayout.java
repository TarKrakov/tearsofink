package com.tarkrakov.tearsofink.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.function.IntBinaryOperator;

/**
 * Раскладка Чернильных Земель — своё измерение (tears-of-ink:ink_lands), центр в 0,0.
 * От края к центру: Бледнолесье → (круглые пятна Кроволесья) → Чернолесье.
 * Одно крупное Смоляное Озеро на стыке Бледнолесья и Чернолесья: чёрная вода, глубже к середине,
 * пепельный пляж вокруг (деревня рыболюдов будет плавучей). Дороги его огибают.
 * Магистральные дороги идут от края к центру.
 * Центр выбирается автоматически (на суше) и одинаков для одного и того же сида мира.
 */
public final class InkLandsLayout {
	// ===== Размеры (в блоках) =====
	/** Радиус всей области (до края-петли). Каждая зона на +100 шире, чем было в обычном мире (800). */
	public static final int RADIUS = 2200; // 16 ноя: было 1100 — мир вдвое шире (по дороге ~6–7 мин бегом)
	/** Радиус ядра — Чернолесья (16 ноя: ×1,5, было 424; шире стало в основном Бледнолесье). */
	public static final int CORE_RADIUS = 640;
	/** Насколько рвутся края области (±). */
	public static final int EDGE_WOBBLE = 160;
	/** Масштаб «рваности» краёв: больше — плавнее изгибы. */
	public static final double EDGE_SCALE = 420.0;

	// ===== Кроволесье: круглые пятна (позже — вокруг Багряных Вестников) =====
	/** Размер ячейки сетки: в каждой ячейке может быть одно пятно. */
	public static final int BLOOD_CELL = 240;
	/** Шанс, что в ячейке есть пятно. */
	public static final double BLOOD_CHANCE = 0.6;
	/** Радиус пятна: от MIN до MAX. */
	public static final int BLOOD_MIN_RADIUS = 60, BLOOD_MAX_RADIUS = 98;
	/** Лёгкая неровность края пятна (±). */
	public static final int BLOOD_WOBBLE = 7;
	/** Пятна только в средней полосе: от ядра + запас до этой доли радиуса области. */
	public static final int BLOOD_INNER_MARGIN = 15;
	public static final double BLOOD_OUTER_FRACTION = 0.92;

	// ===== Смоляное Озеро (одно на область) =====
	/** Расстояние центра Озера от центра области: на стыке Бледнолесья и Чернолесья, ближе к Чернолесью. */
	public static final int LAKE_DISTANCE = 720;
	/** Радиус водной глади (до пляжа). */
	public static final int LAKE_WATER_RADIUS = 130;
	/** Ширина пляжа вокруг воды. */
	public static final int LAKE_BEACH = 14;
	/** Плавный склон от пляжа к обычной земле (там уже лес). */
	public static final int LAKE_SLOPE = 45;
	/** Зона «Смоляное озеро» (чёрная вода, туман Озера, без деревьев): вода + пляж + полоса берега. */
	public static final int LAKE_ZONE_RADIUS = LAKE_WATER_RADIUS + LAKE_BEACH + 20;
	/** Глубина у самого берега и наибольшая — в середине. */
	public static final int LAKE_SHORE_DEPTH = 2, LAKE_DEPTH = 16;
	/** Неровность берега (±) и её масштаб (больше — плавнее изгибы). */
	public static final int LAKE_WOBBLE = 12;
	public static final double LAKE_WOBBLE_SCALE = 90.0;
	/** Дороги огибают Озеро на таком расстоянии от его центра (идут вдоль берега). */
	public static final int LAKE_ROAD_AVOID = LAKE_WATER_RADIUS + LAKE_BEACH + LAKE_WOBBLE + 6;

	// ===== Низина Кроволесья =====
	/** Насколько дно долины ниже окружающей земли. */
	public static final int VALLEY_DEPTH = 11;
	/** Ширина плавного склона вокруг пятна (за его краем). */
	public static final int VALLEY_SLOPE = 40;

	// ===== Дороги =====
	/** Сколько магистралей ведёт к центру. */
	public static final int ROADS = 5;
	/** Полуширина дороги (дорога шириной ~2×этого). */
	public static final double ROAD_HALF_WIDTH = 2.6;
	/**
	 * Насколько дорога петляет вбок (блоки) и масштаб изгибов (блоки пути). 16 ноя: раньше петляние было в радианах —
	 * вбок оно росло с расстоянием от центра, и на удвоенном мире дорога у края шла почти поперёк (рвалась на куски,
	 * висела и проваливалась). Теперь вбок не больше ROAD_MEANDER блоков, уклон изгиба ~0,4 блока вбок на блок вперёд.
	 */
	public static final double ROAD_MEANDER = 70;
	public static final double ROAD_MEANDER_SCALE = 260.0;
	/** Дороги начинаются чуть за краем области и доходят почти до центра. */
	public static final double ROAD_START_FRACTION = 1.08;
	/** 16 ноя: дороги обрываются у Чернолесья разрушенным мостом — дальше только лесом. */
	public static final int ROAD_END_RADIUS = CORE_RADIUS - 4;
	/** Разрушенный мост: столько блоков от конца дороги идёт подъём на мост и обломки; подъём над землёй. */
	public static final int BROKEN_BRIDGE_LEN = 48, BROKEN_BRIDGE_RISE = 9;

	// ===== Заставы Шпиля на дорогах (16 ноя) =====
	/** На какой доле пути от Чернолесья к краю стоят заставы (по одной на каждую долю, на каждой дороге). */
	public static final double[] OUTPOSTS = {0.3, 0.65};

	// ===== Арена Храбрецов: одна низина Кроволесья крупнее прочих (16 ноя, место под постройку автора) =====
	public static final int ARENA_VALLEY_RADIUS = 140;
	/** Насколько дорога может подниматься над рельефом насыпью (вниз — режет без ограничений). */
	public static final int ROAD_MAX_FILL = 6;
	/** Максимальный уклон дороги: подъём в блоках на 1 блок пути. */
	public static final double ROAD_MAX_GRADE = 0.35; // 16 ноя: было 0,2 — в холмах дорога резала выемки до 40 блоков
	/** Мост идёт на столько блоков выше воды. */
	public static final int BRIDGE_CLEARANCE = 2;
	/** Шаг выборки высоты вдоль дороги и ширина сглаживания (в шагах). */
	public static final int ROAD_PROFILE_STEP = 8;
	public static final int ROAD_SMOOTHING = 5; // 16 ноя: было 8 — дорога точнее идёт по рельефу, меньше глубоких выемок
	/** Если полотно выше земли больше чем на столько — вместо насыпи эстакада (мост на опорах). */
	public static final int ROAD_VIADUCT_HEIGHT = 6;
	/** Обочина за краем полотна: на ней перила мостов, фонарные столбы, откосы насыпи. */
	public static final double ROAD_SHOULDER = 1.4;
	/** Фонарные столбы вдоль дорог — каждые столько блоков, по очереди слева и справа. */
	public static final int ROAD_LANTERN_SPACING = 64;

	private final Climate.Sampler sampler;
	private final int templeLayoutVersion;
	private final Object templePlanLock = new Object();
	private java.nio.file.Path templePlanFile;
	// A failed frozen-plan read/write is not a reason to retry costly planning on every chunk.
	private RuntimeException templePlanFailure;
	private volatile TempleNetwork templeNetwork;
	private final BlockPos center;
	private final long seed;
	/** Шум рваного края — один для всех миров, чтобы клиент сам знал, где край (туман сгущается). */
	private static final SimplexNoise EDGE_NOISE = new SimplexNoise(RandomSource.create(0x7E4125L));
	private final SimplexNoise detailNoise;
	private final SimplexNoise roadNoise;
	private final double[] roadAngles = new double[ROADS];
	/** Сглаженная высота каждой дороги вдоль её длины (считается один раз). */
	private volatile double[][] roadProfiles;
	private volatile boolean[][] roadBridges;
	/** Центр Смоляного Озера. */
	private final BlockPos lakeCenter;
	/** Уровень воды Озера (считается один раз). */
	private volatile Double lakeWaterLevel;
	/** Высота дна для каждого пятна Кроволесья (считается один раз на пятно). */
	private final java.util.concurrent.ConcurrentHashMap<Long, Double> valleyFloors = new java.util.concurrent.ConcurrentHashMap<>();

	private InkLandsLayout(Climate.Sampler sampler, long seed, int templeLayoutVersion) {
		this.sampler = sampler;
		this.templeLayoutVersion = templeLayoutVersion;
		this.center = BlockPos.ZERO;
		this.seed = seed;
		RandomSource random = RandomSource.create(seed);
		this.detailNoise = new SimplexNoise(random);
		this.roadNoise = new SimplexNoise(random);
		double start = random.nextDouble() * Mth.TWO_PI;
		for (int i = 0; i < ROADS; i++) {
			roadAngles[i] = start + i * Mth.TWO_PI / ROADS + (random.nextDouble() - 0.5) * 0.5;
		}
		this.lakeCenter = placeLake();
		this.arenaCenter = placeArena();
	}

	/** Центр низины Арены: середина второго по ширине промежутка между дорогами, посередине Бледнолесья. */
	private final BlockPos arenaCenter;

	private BlockPos placeArena() {
		double[] sorted = new double[ROADS];
		for (int i = 0; i < ROADS; i++) sorted[i] = Mth.positiveModulo(roadAngles[i], Mth.TWO_PI);
		java.util.Arrays.sort(sorted);
		double[] gaps = new double[ROADS], mids = new double[ROADS];
		for (int i = 0; i < ROADS; i++) {
			double a = sorted[i], b = i + 1 < ROADS ? sorted[i + 1] : sorted[0] + Mth.TWO_PI;
			gaps[i] = b - a;
			mids[i] = (a + b) / 2;
		}
		int best = 0, second = -1;
		for (int i = 1; i < ROADS; i++) if (gaps[i] > gaps[best]) best = i;
		for (int i = 0; i < ROADS; i++) if (i != best && (second < 0 || gaps[i] > gaps[second])) second = i;
		double d = (CORE_RADIUS + RADIUS) / 2.0;
		return new BlockPos(center.getX() + (int) Math.round(Math.cos(mids[second]) * d), 0,
				center.getZ() + (int) Math.round(Math.sin(mids[second]) * d));
	}

	/** Центр низины Арены Храбрецов (под её Вестником встанет Арена). */
	public BlockPos arenaCenter() {
		return arenaCenter;
	}

	/** Где на дороге road стоит застава number: расстояние от центра. */
	public double outpostDistance(int number) {
		return ROAD_END_RADIUS + BROKEN_BRIDGE_LEN + OUTPOSTS[number] * (RADIUS - ROAD_END_RADIUS - BROKEN_BRIDGE_LEN);
	}

	/** Озеро — посередине самого широкого промежутка между соседними дорогами. */
	private BlockPos placeLake() {
		double[] sorted = new double[ROADS];
		for (int i = 0; i < ROADS; i++) sorted[i] = Mth.positiveModulo(roadAngles[i], Mth.TWO_PI);
		java.util.Arrays.sort(sorted);
		double bestGap = -1, bestMid = 0;
		for (int i = 0; i < ROADS; i++) {
			double a = sorted[i], b = i + 1 < ROADS ? sorted[i + 1] : sorted[0] + Mth.TWO_PI;
			if (b - a > bestGap) {
				bestGap = b - a;
				bestMid = (a + b) / 2;
			}
		}
		return new BlockPos(center.getX() + (int) Math.round(Math.cos(bestMid) * LAKE_DISTANCE), 0,
				center.getZ() + (int) Math.round(Math.sin(bestMid) * LAKE_DISTANCE));
	}

	public BlockPos lakeCenter() {
		return lakeCenter;
	}

	/** Быстрая проверка без шума: может ли точка быть ближе radius к Озеру (с учётом неровности берега). */
	public boolean nearLake(int x, int z, double radius) {
		double dx = x - lakeCenter.getX(), dz = z - lakeCenter.getZ();
		double r = radius + LAKE_WOBBLE + 1;
		return dx * dx + dz * dz < r * r;
	}

	/** Расстояние до центра Озера с учётом неровного берега. */
	public double lakeDistance(int x, int z) {
		double dx = x - lakeCenter.getX(), dz = z - lakeCenter.getZ();
		return Math.sqrt(dx * dx + dz * dz) + detailNoise.getValue(x / LAKE_WOBBLE_SCALE + 100, z / LAKE_WOBBLE_SCALE) * LAKE_WOBBLE;
	}

	/**
	 * Рельеф Озера в точке: {высота верхнего твёрдого блока, 1 если над ним вода (до уровня воды), тип: 0 дно, 2 пляж, 3 склон}
	 * или null, если точка вне влияния Озера.
	 */
	public double[] lakeShape(int x, int z, double natural, double waterLevel) {
		double r = lakeDistance(x, z);
		double shore = LAKE_WATER_RADIUS, beachEnd = shore + LAKE_BEACH, slopeEnd = beachEnd + LAKE_SLOPE;
		if (r >= slopeEnd) return null;
		if (r < shore) {
			// у берега мелко, к середине всё глубже; середина — ровное глубокое дно
			double u = Mth.clamp((shore - r) / (shore * 0.75), 0, 1);
			u = u * u * (3 - 2 * u);
			double depth = LAKE_SHORE_DEPTH + (LAKE_DEPTH - LAKE_SHORE_DEPTH) * u;
			return new double[]{waterLevel - Math.round(depth), 1, 0};
		}
		if (r < beachEnd) {
			double rise = (r - shore) / LAKE_BEACH < 0.5 ? 1 : 2;
			return new double[]{waterLevel + rise, 0, 2};
		}
		double t = (r - beachEnd) / LAKE_SLOPE;
		t = t * t * (3 - 2 * t);
		return new double[]{Mth.lerp(t, waterLevel + 2, natural), 0, 3};
	}

	/** Уровень воды Озера: средняя высота земли вокруг, минус немного; не ниже уровня моря. */
	public double lakeWaterLevel(IntBinaryOperator baseHeight, int seaLevel) {
		Double cached = lakeWaterLevel;
		if (cached != null) return cached;
		double sum = 0;
		int n = 0;
		for (int a = 0; a < 12; a++) {
			double angle = a * Mth.TWO_PI / 12;
			for (double f : new double[]{0.5, 1.0, 1.3}) {
				sum += baseHeight.applyAsInt((int) (lakeCenter.getX() + Math.cos(angle) * LAKE_WATER_RADIUS * f),
						(int) (lakeCenter.getZ() + Math.sin(angle) * LAKE_WATER_RADIUS * f));
				n++;
			}
		}
		double level = Math.max(seaLevel, Math.round(sum / n) - 4);
		lakeWaterLevel = level;
		return level;
	}

	public static InkLandsLayout create(Climate.Sampler sampler) {
		return create(sampler, 1);
	}

	static InkLandsLayout create(Climate.Sampler sampler, int templeLayoutVersion) {
		return new InkLandsLayout(sampler, seedOf(sampler), templeLayoutVersion);
	}

	public boolean hasTempleNetwork() {
		return templeLayoutVersion == 2 || templeLayoutVersion == 3;
	}

	int templeLayoutVersion() { return templeLayoutVersion; }

	/** Attached to the canonical world layout before generation starts. */
	void attachTemplePlanFile(java.nio.file.Path file) {
		synchronized (templePlanLock) {
			if (templePlanFile != null && templePlanFile.equals(file)) return;
			if (templeNetwork != null || templePlanFile != null) {
				throw new IllegalStateException("Temple plan storage bound after planning");
			}
			templePlanFile = file;
		}
	}

	/** Restore only an existing plan before world generation/gameplay starts. */
	void restoreTempleNetworkIfPresent(TempleNetwork.Inputs inputs) {
		synchronized (templePlanLock) {
			if (templePlanFailure != null) throw templePlanFailure;
			if (templeNetwork != null || templePlanFile == null) return;
			try {
				templeNetwork = TemplePlanStore.restoreIfPresent(templePlanFile, this, inputs);
			} catch (RuntimeException failure) {
				templePlanFailure = failure;
				throw failure;
			}
		}
	}

	public TempleNetwork templeNetwork() {
		return templeNetwork;
	}

	public TempleNetwork prepareTempleNetwork(net.minecraft.world.level.levelgen.structure.Structure.GenerationContext context) {
		var plan = templeNetwork;
		return plan != null ? plan : prepareTempleNetwork(new TempleNetwork.Inputs(
				new RoadTerrain(context.chunkGenerator(), context.heightAccessor(), context.randomState()),
				context.registryAccess(), context.structureTemplateManager(), context.seed()));
	}

	public TempleNetwork prepareTempleNetwork(TempleNetwork.Inputs inputs) {
		TempleNetwork plan = templeNetwork;
		if (plan == null) {
			synchronized (templePlanLock) {
				plan = templeNetwork;
				if (plan == null) {
					if (templePlanFailure != null) throw templePlanFailure;
					// Diagnostic planning without a world path remains pure.
					try {
						plan = templePlanFile == null ? TempleNetwork.plan(this, inputs)
								: TemplePlanStore.loadOrCreate(templePlanFile, this, inputs);
						templeNetwork = plan;
					} catch (RuntimeException failure) {
						if (templePlanFile != null) templePlanFailure = failure;
						throw failure;
					}
				}
			}
		}
		return plan;
	}

	/** Сид раскладки из сида мира: берём климат в нескольких точках (он зависит только от сида). */
	private static long seedOf(Climate.Sampler sampler) {
		long h = 0x7E4125L;
		for (int i = 0; i < 4; i++) {
			Climate.TargetPoint p = sampler.sample(i * 97, 16, i * -131);
			h = h * 31 + p.temperature();
			h = h * 31 + p.humidity();
			h = h * 31 + p.weirdness();
			h = h * 31 + p.erosion();
		}
		return h;
	}

	/** Сид раскладки (свой у каждого мира). */
	public long seed() {
		return seed;
	}

	public Climate.Sampler sampler() {
		return sampler;
	}

	public BlockPos center() {
		return center;
	}

	/** Расстояние до центра с учётом рваного края. */
	private double edgeDistance(int x, int z) {
		return Math.sqrt((double) x * x + (double) z * z) + EDGE_NOISE.getValue(x / EDGE_SCALE, z / EDGE_SCALE) * EDGE_WOBBLE;
	}

	/** Глубина внутри Земель без раскладки (центр всегда 0,0) — для клиента: туман у края. */
	public static double edgeDepth(double x, double z) {
		return RADIUS - (Math.sqrt(x * x + z * z) + EDGE_NOISE.getValue(x / EDGE_SCALE, z / EDGE_SCALE) * EDGE_WOBBLE);
	}

	/** На сколько блоков внутрь от края попадает новичок (чтобы бой у места прибытия не уводил за край, в петлю). */
	public static final int ARRIVAL_DEPTH = 100;

	/** Где игрок попадает в Земли: на дороге road, чуть внутри края Бледнолесья. {x, z} */
	public double[] arrivalPoint(int road) {
		road = Math.floorMod(road, ROADS);
		for (double d = RADIUS * ROAD_START_FRACTION; d > CORE_RADIUS; d -= 4) {
			double[] p = roadPoint(road, d);
			if (depthInside((int) p[0], (int) p[1]) >= ARRIVAL_DEPTH) return p;
		}
		return roadPoint(road, RADIUS * 0.9);
	}

	/**
	 * Насколько глубоко точка внутри Чернильных Земель (в блоках, с учётом рваного края).
	 * Больше нуля — внутри, меньше нуля — снаружи.
	 */
	public double depthInside(int x, int z) {
		return RADIUS - edgeDistance(x, z);
	}

	/**
	 * Какой биом поставить в точке (координаты в блоках).
	 * @return наш биом или {@code null}, если точка вне Чернильных Земель.
	 */
	public ResourceKey<Biome> pick(int x, int z, Holder<Biome> original) {
		double dx = x - center.getX();
		double dz = z - center.getZ();
		if (dx * dx + dz * dz > (double) (RADIUS + EDGE_WOBBLE) * (RADIUS + EDGE_WOBBLE)) return null;
		double d = edgeDistance(x, z);
		if (d > RADIUS) return null;

		// Смоляное Озеро — одно, крупное; прочая вода внутри области — просто вода своей зоны
		if (nearLake(x, z, LAKE_ZONE_RADIUS) && lakeDistance(x, z) < LAKE_ZONE_RADIUS) return ModBiomes.TAR_LAKE;
		if (d < CORE_RADIUS) return ModBiomes.INKWOOD;
		if (isBlood(x, z)) return ModBiomes.BLOODWOOD;
		return ModBiomes.PALE_FOREST;
	}

	// ----- Кроволесье -----
	private boolean isBlood(int x, int z) {
		int cellX = Math.floorDiv(x, BLOOD_CELL);
		int cellZ = Math.floorDiv(z, BLOOD_CELL);
		double wobble = Double.NaN; // шум считаем, только если рядом есть пятно
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				long[] blob = bloodBlob(cellX + i, cellZ + j);
				if (blob == null) continue;
				double bx = x - blob[0], bz = z - blob[1];
				double dist = Math.sqrt(bx * bx + bz * bz);
				if (dist - BLOOD_WOBBLE >= blob[2]) continue;
				if (Double.isNaN(wobble)) wobble = detailNoise.getValue(x / 30.0, z / 30.0) * BLOOD_WOBBLE;
				if (dist + wobble < blob[2]) return true;
			}
		}
		return false;
	}

	/** Пятна по ячейкам (считаются один раз — выбор зоны спрашивает их очень часто). */
	private final java.util.concurrent.ConcurrentHashMap<Long, long[]> bloodBlobs = new java.util.concurrent.ConcurrentHashMap<>();
	private static final long[] NO_BLOB = new long[0];

	/** Пятно в ячейке: {x, z, радиус} или null. */
	private long[] bloodBlob(int cellX, int cellZ) {
		long[] blob = bloodBlobs.computeIfAbsent(((long) cellX << 32) ^ (cellZ & 0xffffffffL), k -> {
			long[] b = computeBloodBlob(cellX, cellZ);
			return b == null ? NO_BLOB : b;
		});
		return blob == NO_BLOB ? null : blob;
	}

	private long[] computeBloodBlob(int cellX, int cellZ) {
		// низина Арены — в своей ячейке всегда, крупнее прочих; соседние пятна на неё не наезжают
		if (Math.floorDiv(arenaCenter.getX(), BLOOD_CELL) == cellX && Math.floorDiv(arenaCenter.getZ(), BLOOD_CELL) == cellZ)
			return new long[]{arenaCenter.getX(), arenaCenter.getZ(), ARENA_VALLEY_RADIUS};
		RandomSource r = RandomSource.create(seed * 31 + cellX * 341873128712L + cellZ * 132897987541L);
		if (r.nextDouble() > BLOOD_CHANCE) return null;
		long bx = (long) ((cellX + 0.2 + r.nextDouble() * 0.6) * BLOOD_CELL);
		long bz = (long) ((cellZ + 0.2 + r.nextDouble() * 0.6) * BLOOD_CELL);
		long radius = BLOOD_MIN_RADIUS + r.nextInt(BLOOD_MAX_RADIUS - BLOOD_MIN_RADIUS + 1);
		double dx = bx - center.getX(), dz = bz - center.getZ();
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d < CORE_RADIUS + BLOOD_INNER_MARGIN + radius || d > RADIUS * BLOOD_OUTER_FRACTION - radius) return null;
		double lx = bx - lakeCenter.getX(), lz = bz - lakeCenter.getZ();
		if (Math.sqrt(lx * lx + lz * lz) < LAKE_ZONE_RADIUS + radius + VALLEY_SLOPE) return null; // не наезжать на Озеро
		double ax = bx - arenaCenter.getX(), az = bz - arenaCenter.getZ();
		if (Math.sqrt(ax * ax + az * az) < ARENA_VALLEY_RADIUS + radius + VALLEY_SLOPE * 2) return null; // и на Арену
		return new long[]{bx, bz, radius};
	}

	// ----- Низина Кроволесья -----
	/**
	 * Высота земли с учётом низин Кроволесья: плоское дно внутри пятна и плавный склон вокруг.
	 * @param natural естественная высота в этой точке
	 * @param baseHeight высота рельефа (x, z) → y — нужна, чтобы одинаково посчитать дно для всех чанков
	 * @return новая высота или {@code natural}, если точка вне влияния низин
	 */
	public double valleyHeight(int x, int z, double natural, IntBinaryOperator baseHeight) {
		int cellX = Math.floorDiv(x, BLOOD_CELL);
		int cellZ = Math.floorDiv(z, BLOOD_CELL);
		double result = natural;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				long[] blob = bloodBlob(cellX + i, cellZ + j);
				if (blob == null) continue;
				double bx = x - blob[0], bz = z - blob[1];
				double dist = Math.sqrt(bx * bx + bz * bz);
				if (dist > blob[2] + VALLEY_SLOPE) continue;
				double floor = valleyFloor(cellX + i, cellZ + j, blob, baseHeight);
				double t = Mth.clamp((dist - blob[2]) / VALLEY_SLOPE, 0, 1);
				t = t * t * (3 - 2 * t); // плавный (smoothstep) склон
				result = Math.min(result, Mth.lerp(t, floor, natural));
			}
		}
		return result;
	}

	private double valleyFloor(int cellX, int cellZ, long[] blob, IntBinaryOperator baseHeight) {
		return valleyFloors.computeIfAbsent(((long) cellX << 32) ^ (cellZ & 0xffffffffL), k -> {
			double sum = 0;
			int n = 0;
			for (int a = 0; a < 8; a++) {
				double angle = a * Mth.TWO_PI / 8;
				for (double f : new double[]{0, 0.5, 1.0}) {
					sum += baseHeight.applyAsInt((int) (blob[0] + Math.cos(angle) * blob[2] * f),
							(int) (blob[1] + Math.sin(angle) * blob[2] * f));
					n++;
				}
			}
			return sum / n - VALLEY_DEPTH;
		});
	}

	// ----- Дороги -----
	/** Точка, где начинается дорога номер index (0..ROADS-1) — чуть за краем области. */
	public BlockPos roadStart(int index) {
		double d = RADIUS * 1.04;
		double angle = roadAngle(Math.floorMod(index, ROADS), d);
		return new BlockPos(center.getX() + (int) Math.round(Math.cos(angle) * d), 0,
				center.getZ() + (int) Math.round(Math.sin(angle) * d));
	}

	/** Боковой сдвиг дороги (блоки) для обхода Озера и Вестников — по 1 блоку вдоль дороги, считается один раз. */
	private volatile double[][] roadShift;
	/** Как круто дорога может уходить вбок при обходе: блоков вбок на блок вперёд. */
	public static final double ROAD_SHIFT_SLOPE = 0.55;
	/** Запас при обходе (сверх радиусов Озера/ствола), чтобы сглаживание не «срезало» угол. */
	private static final double ROAD_SHIFT_MARGIN = 5;

	/** Угол дороги без обходов: направление + плавное петляние. */
	private double baseRoadAngle(int road, double d) {
		return roadAngles[road] + roadNoise.getValue(d / ROAD_MEANDER_SCALE, road * 10.0) * ROAD_MEANDER / Math.max(d, 200.0);
	}

	private double roadAngle(int road, double d) {
		double a = baseRoadAngle(road, d);
		if (d < 1) return a;
		double[] shift = roadShifts()[road];
		double t = Mth.clamp(d, 0, shift.length - 2);
		int i = (int) t;
		return a + Mth.lerp(t - i, shift[i], shift[i + 1]) / d;
	}

	private double[][] roadShifts() {
		double[][] s = roadShift;
		if (s == null) {
			synchronized (this) {
				s = roadShift;
				if (s == null) {
					s = buildRoadShifts();
					roadShift = s;
				}
			}
		}
		return s;
	}

	/**
	 * Сдвиг вбок: где дорога должна обойти круг — нужный сдвиг; к нему плавные съезды с наклоном не круче
	 * {@link #ROAD_SHIFT_SLOPE}, потом сглаживание. Так дорога не рвётся и не уходит поперёк пути.
	 */
	private double[][] buildRoadShifts() {
		int n = (int) (RADIUS * ROAD_START_FRACTION) + 40;
		double[][] out = new double[ROADS][];
		for (int road = 0; road < ROADS; road++) {
			// 16 ноя: сторона обхода — одна на препятствие (с той, где дорога и так ближе проходит). Раньше сторона
			// решалась на каждом шаге: если дорога шла почти через центр, сдвиги «влево» и «вправо» гасили друг друга
			// и дорога шла прямо сквозь ствол Вестника.
			java.util.Map<Long, double[]> sides = new java.util.HashMap<>(); // ключ препятствия → {мин. расстояние, сторона}
			for (int i = 1; i < n; i++) {
				double a0 = baseRoadAngle(road, i);
				double px = center.getX() + Math.cos(a0) * i, pz = center.getZ() + Math.sin(a0) * i;
				noteSide(sides, -1L, a0, i, px, pz, lakeCenter.getX(), lakeCenter.getZ());
				int cellX = Math.floorDiv((int) px, BLOOD_CELL), cellZ = Math.floorDiv((int) pz, BLOOD_CELL);
				for (int ci = -1; ci <= 1; ci++) for (int cj = -1; cj <= 1; cj++) {
					long[] blob = bloodBlob(cellX + ci, cellZ + cj);
					if (blob != null) noteSide(sides, blob[0] * 1_000_003L + blob[1], a0, i, px, pz, blob[0], blob[1]);
				}
			}
			sideMap = sides;
			double[] need = new double[n];
			for (int i = 1; i < n; i++) {
				double a0 = baseRoadAngle(road, i);
				double a = avoidObstacles(a0, i);
				need[i] = Mth.wrapDegrees((a - a0) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD * i;
			}
			double[] pos = new double[n], neg = new double[n];
			for (int i = 0; i < n; i++) {
				double v = need[i];
				if (v == 0) continue;
				double mag = Math.abs(v) + ROAD_SHIFT_MARGIN;
				int reach = (int) Math.ceil(mag / ROAD_SHIFT_SLOPE);
				double[] env = v > 0 ? pos : neg;
				for (int j = Math.max(0, i - reach); j <= Math.min(n - 1, i + reach); j++) {
					env[j] = Math.max(env[j], mag - ROAD_SHIFT_SLOPE * Math.abs(i - j));
				}
			}
			double[] raw = new double[n];
			for (int i = 0; i < n; i++) raw[i] = pos[i] - neg[i];
			double[] smooth = new double[n];
			int w = 6;
			for (int i = 0; i < n; i++) {
				double sum = 0;
				int c = 0;
				for (int k = -w; k <= w; k++) {
					int j = i + k;
					if (j < 0 || j >= n) continue;
					sum += raw[j];
					c++;
				}
				smooth[i] = sum / c;
			}
			out[road] = smooth;
		}
		return out;
	}

	/** Стороны обхода для дороги, которую сейчас считаем (buildRoadShifts). */
	private java.util.Map<Long, double[]> sideMap;

	/** Запомнить, с какой стороны дорога ближе всего проходит мимо препятствия. */
	private void noteSide(java.util.Map<Long, double[]> sides, long key, double a0, double d, double px, double pz, double cx, double cz) {
		double dist = Math.hypot(px - cx, pz - cz);
		double[] cur = sides.get(key);
		if (cur != null && cur[0] <= dist) return;
		double phi = Math.atan2(cz - center.getZ(), cx - center.getX());
		double diff = Mth.wrapDegrees((a0 - phi) * Mth.RAD_TO_DEG);
		sides.put(key, new double[]{dist, diff >= 0 ? 1 : -1});
	}

	private int side(long key) {
		java.util.Map<Long, double[]> m = sideMap;
		double[] v = m == null ? null : m.get(key);
		return v == null ? 0 : (int) v[1];
	}

	/** Жёсткий обход: Озеро и стволы Вестников (без сглаживания — только чтобы узнать, сколько надо сдвинуть). */
	private double avoidObstacles(double a, double d) {
		a = avoidLake(a, d);
		// и стволы Багряных Вестников (в центрах пятен Кроволесья)
		int px = center.getX() + (int) Math.round(Math.cos(a) * d), pz = center.getZ() + (int) Math.round(Math.sin(a) * d);
		int cellX = Math.floorDiv(px, BLOOD_CELL), cellZ = Math.floorDiv(pz, BLOOD_CELL);
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				long[] blob = bloodBlob(cellX + i, cellZ + j);
				if (blob != null) a = avoidCircle(a, d, blob[0], blob[1], com.tarkrakov.tearsofink.world.herald.BloodHerald.ROAD_AVOID,
						side(blob[0] * 1_000_003L + blob[1]));
			}
		}
		return a;
	}


	/** Если дорога на этом радиусе попадает в Озеро — сдвигаем её вбок, на берег (в ту сторону, где она и так шла). */
	private double avoidLake(double a, double d) {
		if (lakeCenter == null) return a;
		return avoidCircle(a, d, lakeCenter.getX(), lakeCenter.getZ(), LAKE_ROAD_AVOID, side(-1L));
	}

	/** Сдвинуть дорогу (угол a на радиусе d от центра области) так, чтобы она обошла круг (cx, cz, r). */
	private double avoidCircle(double a, double d, double cx, double cz, double r, int forcedSide) {
		double lx = cx - center.getX(), lz = cz - center.getZ();
		double dist = Math.sqrt(lx * lx + lz * lz), phi = Math.atan2(lz, lx);
		double c = (d * d + dist * dist - r * r) / (2 * d * dist);
		if (c >= 1 || c <= -1) return a; // на этом радиусе круг Озера дорогу не задевает
		double theta = Math.acos(c);
		double diff = Mth.wrapDegrees((a - phi) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
		if (Math.abs(diff) >= theta) return a;
		int s = forcedSide != 0 ? forcedSide : (diff >= 0 ? 1 : -1);
		return phi + s * theta;
	}

	/**
	 * Где стоит k-й фонарный столб дороги (на краю полотна).
	 * @return {x, z} или null, если столб вне Чернильных Земель
	 */
	public int[] roadLanternPost(int road, int k) {
		double t = (double) k * ROAD_LANTERN_SPACING;
		if (t < ROAD_END_RADIUS + 10 || t > RADIUS * ROAD_START_FRACTION) return null;
		double[] p = roadPoint(road, t);
		double[] dir = roadDirection(road, t);
		double side = (k % 2 == 0) ? 1 : -1;
		// на обочине, поперёк НАСТОЯЩЕГО направления дороги (на изгибах и обходах — тоже не на проходе)
		for (double offset = ROAD_HALF_WIDTH + 0.6; offset <= ROAD_HALF_WIDTH + 2.6; offset += 0.5) {
			int x = (int) Math.floor(p[0] - dir[1] * offset * side);
			int z = (int) Math.floor(p[1] + dir[0] * offset * side);
			if (depthInside(x, z) < 0) return null;
			double[] hit = roadAt(x, z, ROAD_HALF_WIDTH + ROAD_SHOULDER);
			if (hit == null || hit[2] > ROAD_HALF_WIDTH + 0.3) return new int[]{x, z};
		}
		return null;
	}

	/** Дорога рядом с точкой (полуширина + extra): {номер, расстояние от центра области вдоль дороги, до осевой} или null. */
	public double[] roadNear(int x, int z, double extra) {
		return roadAt(x, z, ROAD_HALF_WIDTH + extra);
	}

	/** Точка на осевой дороги road на расстоянии d от центра области: {x, z}. */
	public double[] roadPoint(int road, double d) {
		double a = roadAngle(road, d);
		return new double[]{center.getX() + Math.cos(a) * d, center.getZ() + Math.sin(a) * d};
	}

	/** Раскладка Земель для мира (или null, если их нет). */
	public static InkLandsLayout of(net.minecraft.server.level.ServerLevel level) {
		if (level.getChunkSource().getGenerator().getBiomeSource() instanceof InkLandsAccess access) {
			return access.tearsofink$layout(level.getChunkSource().randomState().sampler());
		}
		return null;
	}

	/** Лежит ли точка на одной из магистралей. */
	public boolean isRoad(int x, int z) {
		return roadAt(x, z) != null;
	}

	/** Если точка на дороге — {номер дороги, расстояние вдоль дороги от центра, расстояние до осевой}, иначе null. */
	public double[] roadAt(int x, int z) {
		return roadAt(x, z, ROAD_HALF_WIDTH);
	}

	/** Ближе ли точка к осевой дороги, чем полуширина + extra (для запрета деревьев у обочины). */
	public boolean nearRoad(int x, int z, double extra) {
		return roadAt(x, z, ROAD_HALF_WIDTH + extra) != null;
	}

	private double[] roadAt(int x, int z, double halfWidth) {
		double dx = x - center.getX();
		double dz = z - center.getZ();
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d < ROAD_END_RADIUS || d > RADIUS * ROAD_START_FRACTION) return null;
		double angle = Math.atan2(dz, dx);
		for (int i = 0; i < ROADS; i++) {
			double diff = Mth.wrapDegrees((angle - roadAngle(i, d)) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
			if (Math.abs(diff) > Math.PI / 2 || Math.abs(Math.sin(diff)) * d > halfWidth + 14) continue;
			// настоящее расстояние до осевой линии (с учётом изгибов)
			double best = Double.MAX_VALUE, bestT = d;
			for (double t = d - 12; t <= d + 12; t += 0.5) {
				if (t < ROAD_END_RADIUS) continue;
				double a = roadAngle(i, t);
				double cx = center.getX() + Math.cos(a) * t - x;
				double cz = center.getZ() + Math.sin(a) * t - z;
				double dist2 = cx * cx + cz * cz;
				if (dist2 < best) { best = dist2; bestT = t; }
			}
			double dist = Math.sqrt(best);
			if (dist <= halfWidth) return new double[]{i, bestT, dist};
		}
		return null;
	}

	/**
	 * Высота полотна дороги в этом месте.
	 * @param terrain исходные высоты генератора
	 */
	public double roadHeight(int road, double d, RoadTerrain terrain) {
		double[][] profiles = roadProfiles;
		if (profiles == null) {
			synchronized (this) {
				profiles = roadProfiles;
				if (profiles == null) {
					boolean[][] bridges = new boolean[ROADS][];
					profiles = buildRoadProfiles(terrain, bridges);
					roadBridges = bridges;
					roadProfiles = profiles;
				}
			}
		}
		double[] profile = profiles[road];
		double t = (d - ROAD_END_RADIUS) / ROAD_PROFILE_STEP;
		int i = Mth.clamp((int) Math.floor(t), 0, profile.length - 2);
		return Mth.lerp(Mth.clamp(t - i, 0, 1), profile[i], profile[i + 1]);
	}

	/**
	 * Идёт ли дорога здесь мостом (над водой или эстакадой над низиной). Решается целыми отрезками,
	 * чтобы мост не рвался на куски. Вызывать после {@link #roadHeight}.
	 */
	public boolean roadBridge(int road, double d) {
		boolean[][] bridges = roadBridges;
		if (bridges == null) return false;
		boolean[] b = bridges[road];
		double t = (d - ROAD_END_RADIUS) / ROAD_PROFILE_STEP;
		int i = Mth.clamp((int) Math.floor(t), 0, b.length - 2);
		return b[i] || b[i + 1];
	}

	/** Направление дороги в этом месте: {dx, dz}, единичный вектор (от центра наружу). */
	public double[] roadDirection(int road, double d) {
		double[] a = roadPoint(road, d - 1), b = roadPoint(road, d + 1);
		double dx = b[0] - a[0], dz = b[1] - a[1], len = Math.sqrt(dx * dx + dz * dz);
		return len < 1.0E-6 ? new double[]{1, 0} : new double[]{dx / len, dz / len};
	}

	private double[][] buildRoadProfiles(RoadTerrain terrain, boolean[][] bridgesOut) {
		var heights = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
		IntBinaryOperator surface = (x, z) -> heights.computeIfAbsent(ChunkPos.asLong(x, z), key -> terrain.surface(x, z));
		int samples = (int) Math.ceil((RADIUS * ROAD_START_FRACTION - ROAD_END_RADIUS) / ROAD_PROFILE_STEP) + 2;
		double maxStep = ROAD_MAX_GRADE * ROAD_PROFILE_STEP;
		double[][] result = new double[ROADS][];
		for (int road = 0; road < ROADS; road++) {
			double[] raw = new double[samples];
			boolean[] water = new boolean[samples];
			for (int i = 0; i < samples; i++) {
				double d = ROAD_END_RADIUS + i * ROAD_PROFILE_STEP;
				double a = roadAngle(road, d);
				int px = center.getX() + (int) Math.round(Math.cos(a) * d);
				int pz = center.getZ() + (int) Math.round(Math.sin(a) * d);
				RoadTerrain.Sample sample = terrain.sample(px, pz);
				int top = sample.surface();
				heights.put(ChunkPos.asLong(px, pz), top);
				water[i] = sample.water();
				raw[i] = water[i] ? top + BRIDGE_CLEARANCE : valleyHeight(px, pz, top, surface);
			}
			// сглаживание
			double[] p = new double[samples];
			for (int i = 0; i < samples; i++) {
				double sum = 0;
				int n = 0;
				for (int k = -ROAD_SMOOTHING; k <= ROAD_SMOOTHING; k++) {
					int j = i + k;
					if (j < 0 || j >= samples) continue;
					sum += raw[j];
					n++;
				}
				p[i] = water[i] ? Math.max(sum / n, raw[i]) : Math.min(sum / n, raw[i] + ROAD_MAX_FILL);
			}
			// пологий уклон: срезаем слишком крутые подъёмы (дорога режет горы)
			for (int pass = 0; pass < 2; pass++) {
				for (int i = 1; i < samples; i++) if (!water[i]) p[i] = Math.min(p[i], p[i - 1] + maxStep);
				for (int i = samples - 2; i >= 0; i--) if (!water[i]) p[i] = Math.min(p[i], p[i + 1] + maxStep);
			}
			// у Чернолесья дорога поднимается на мост, который обрывается (разрушенный мост)
			int brokenSamples = BROKEN_BRIDGE_LEN / ROAD_PROFILE_STEP;
			for (int i = 0; i <= brokenSamples && i < samples; i++) {
				double rise = BROKEN_BRIDGE_RISE * (1 - (double) i / (brokenSamples + 1));
				p[i] = Math.max(p[i], raw[i] + rise);
			}
			// плавный подъём к мостам
			for (int i = 1; i < samples; i++) p[i] = Math.max(p[i], p[i - 1] - maxStep);
			for (int i = samples - 2; i >= 0; i--) p[i] = Math.max(p[i], p[i + 1] - maxStep);
			result[road] = p;
			// где мост: вода или полотно высоко над землёй; + по отрезку с каждой стороны (заходы на берег)
			boolean[] raw2 = new boolean[samples], br = new boolean[samples];
			for (int i = 0; i < samples; i++) raw2[i] = water[i] || p[i] - raw[i] > ROAD_VIADUCT_HEIGHT || i <= brokenSamples;
			for (int i = 0; i < samples; i++) br[i] = raw2[i] || (i > 0 && raw2[i - 1]) || (i + 1 < samples && raw2[i + 1]);
			// короткие разрывы между мостами (1–2 отрезка земли) — тоже мост, без ступенек туда-сюда
			for (int i = 1; i < samples - 2; i++) {
				if (!br[i] && br[i - 1] && (br[i + 1] || br[i + 2])) br[i] = true;
			}
			bridgesOut[road] = br;
		}
		return result;
	}

	/** Ближе ли точка radius блоков к центру какого-нибудь пятна (там стоит Багряный Вестник). */
	public boolean nearHerald(int x, int z, double radius) {
		int cellX = Math.floorDiv(x, BLOOD_CELL), cellZ = Math.floorDiv(z, BLOOD_CELL);
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				long[] blob = bloodBlob(cellX + i, cellZ + j);
				if (blob == null) continue;
				double dx = x - blob[0], dz = z - blob[1];
				if (dx * dx + dz * dz < radius * radius) return true;
			}
		}
		return false;
	}

	/** Ближайшее к точке пятно Кроволесья: {x, z, радиус} или null. */
	public long[] nearestBloodBlob(int x, int z) {
		int cellX = Math.floorDiv(x, BLOOD_CELL), cellZ = Math.floorDiv(z, BLOOD_CELL);
		long[] best = null;
		double bestD = Double.MAX_VALUE;
		for (int i = -6; i <= 6; i++) {
			for (int j = -6; j <= 6; j++) {
				long[] blob = bloodBlob(cellX + i, cellZ + j);
				if (blob == null) continue;
				double dd = (blob[0] - x) * (double) (blob[0] - x) + (blob[1] - z) * (double) (blob[1] - z);
				if (dd < bestD) { bestD = dd; best = blob; }
			}
		}
		return best;
	}
}
