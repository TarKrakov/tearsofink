package com.tarkrakov.tearsofink.world.ruins;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tarkrakov.tearsofink.world.InkLandsAccess;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.TempleNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Arrays;
import java.util.Optional;

/** Руина: «temple» — храмы в заранее выбранных местах; «scattered» — дома, остовы и монументы где попало. */
public class RuinStructure extends Structure {
	public static final MapCodec<RuinStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			settingsCodec(i),
			Codec.STRING.fieldOf("kind").forGetter(s -> s.kind)
	).apply(i, RuinStructure::new));

	private final String kind;

	public RuinStructure(StructureSettings settings, String kind) {
		super(settings);
		this.kind = kind;
	}

	private record Site(InkLandsLayout layout, ResourceLocation template, int x, int z, int half,
						Rotation rotation, boolean temple, boolean pale, Integer plannedY) {}

	/** Быстрый прогноз горизонтального объёма, включая запас terrain_adaptation и фундамент. */
	public boolean mayOverlap(GenerationContext context, ChunkPos target) {
		Site site = findSite(context);
		if (site == null) return false;
		int radius = site.half() + 12;
		return target.getMinBlockX() <= site.x() + radius && target.getMaxBlockX() >= site.x() - radius
				&& target.getMinBlockZ() <= site.z() + radius && target.getMaxBlockZ() >= site.z() - radius;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		Site site = findSite(context);
		if (site == null) return Optional.empty();
		ChunkGenerator generator = context.chunkGenerator();
		InkLandsLayout layout = site.layout();
		int x = site.x(), z = site.z(), half = site.half();
		boolean temple = site.temple();
		if (site.plannedY() != null) return generationStub(context, site, site.plannedY());
		// высота: «нижняя середина» земли под постройкой (чтобы не висела, но и не тонула)
		// (с учётом низин Кроволесья — их выкапывает генерация позже, по рельефу их не видно)
		java.util.function.IntBinaryOperator base = (bx, bz) ->
				generator.getBaseHeight(bx, bz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()) - 1;
		int[] ys = new int[9];
		int k = 0;
		for (int[] o : new int[][]{{0, 0}, {-half, -half}, {half, -half}, {-half, half}, {half, half}, {0, -half}, {0, half}, {-half, 0}, {half, 0}}) {
			int px = x + o[0], pz = z + o[1];
			ys[k++] = (int) Math.floor(layout.valleyHeight(px, pz, base.applyAsInt(px, pz), base));
		}
		Arrays.sort(ys);
		int y = temple ? ys[4] : ys[2]; // храму рельеф подгоняется (terrain_adaptation), ставим по середине
		if (!temple) {
			if (y <= generator.getSeaLevel()) return Optional.empty(); // не в воде
			if (ys[8] - ys[0] > Ruins.MAX_SLOPE) return Optional.empty(); // не на обрыве
		}

		return generationStub(context, site, y);
	}

	private Optional<GenerationStub> generationStub(GenerationContext context, Site site, int y) {
		int x = site.x(), z = site.z();
		BlockPos center = new BlockPos(x, y, z);
		ResourceLocation chosen = site.template();
		int tier = Ruins.tier(site.layout(), x, z);
		boolean start = site.temple() && Ruins.isStartTemple(site.layout(), x, z);
		boolean bloodTemple=site.temple() && !start && site.plannedY()!=null
				&& site.layout().templeLayoutVersion()==3
				&& site.layout().pick(x,z,null)==com.tarkrakov.tearsofink.world.ModBiomes.BLOODWOOD;
		// The shared biome tag now also allows bloodwood. Keep the previous
		// biome gate for every legacy temple and old pale-only frozen plan.
		if (site.temple() && !bloodTemple && !context.chunkGenerator().getBiomeSource()
				.getNoiseBiome(x>>2,y>>2,z>>2,context.randomState().sampler())
				.is(com.tarkrakov.tearsofink.world.ModBiomes.PALE_FOREST)) return Optional.empty();
		return Optional.of(new GenerationStub(center, builder ->
				builder.addPiece(new RuinPiece(context.structureTemplateManager(), chosen, center, site.rotation(), tier, start, site.pale(),bloodTemple))));
	}

	/** Conservatively reserves the original scattered ruin, including its foundation and terrain adaptation. */
	public BoundingBox scatteredFootprint(GenerationContext context) {
		if (kind.equals("temple")) throw new IllegalStateException("Expected scattered ruin");
		Site site = findSite(context);
		if (site == null) return null;
		int r = site.half() + 12;
		return new BoundingBox(site.x() - r, 0, site.z() - r, site.x() + r, 0, site.z() + r);
	}

	private Site findSite(GenerationContext context) {
		ChunkGenerator generator = context.chunkGenerator();
		if (!(generator.getBiomeSource() instanceof InkLandsAccess access)) return null;
		InkLandsLayout layout = access.tearsofink$layout(context.randomState().sampler());
		if (layout == null) return null;
		ChunkPos chunk = context.chunkPos();
		RandomSource random = context.random();
		ResourceLocation template;
		int x, z;
		TempleNetwork network = null;
		if (kind.equals("temple") && layout.hasTempleNetwork()
				&& layout.depthInside(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ()) >= 170) {
			network = layout.prepareTempleNetwork(context);
		}

		if (kind.equals("temple")) {
			if (network != null) {
				var plan = network.at(chunk);
				if (plan == null) return null;
				if (!plan.starter()) {
					var size = context.structureTemplateManager().getOrCreate(Ruins.TEMPLE).getSize();
					int half = Math.max(size.getX(), size.getZ()) / 2 + 1;
					boolean pale=layout.pick(plan.x(),plan.z(),null)!=com.tarkrakov.tearsofink.world.ModBiomes.BLOODWOOD;
					return new Site(layout, Ruins.TEMPLE, plan.x(), plan.z(), half, plan.rotation(), true, pale, plan.y());
				}
			}
			int[] at = null;
			for (int[] p : layout.hasTempleNetwork() ? Ruins.startTemplePoints(layout) : Ruins.templePoints(layout)) {
				if ((p[0] >> 4) == chunk.x && (p[1] >> 4) == chunk.z) { at = p; break; }
			}
			if (at == null) return null;
			template = Ruins.TEMPLE;
			x = at[0];
			z = at[1];
		} else {
			template = pick(random);
			x = chunk.getMinBlockX() + random.nextInt(16);
			z = chunk.getMinBlockZ() + random.nextInt(16);
		}

		StructureTemplate t = context.structureTemplateManager().getOrCreate(template);
		Vec3i size = t.getSize();
		if (size.getX() == 0) return null;
		int half = Math.max(size.getX(), size.getZ()) / 2 + 1;
		boolean monumentByRoad = false;
		if (template.equals(Ruins.MONUMENT) && random.nextFloat() < Ruins.MONUMENT_NEAR_ROAD) {
			int[] side = besideRoad(layout, x, z, random, half);
			if (side != null) { x = side[0]; z = side[1]; monumentByRoad = true; }
		}
		if (!kind.equals("temple") && Ruins.nearLegacyTemple(layout, x, z)) return null;
		var battlefieldPlan = com.tarkrakov.tearsofink.world.battlefield.BattlefieldWorldPlan.of(layout);
		if (!kind.equals("temple") && battlefieldPlan != null
				&& battlefieldPlan.reserved(x, z, half + 12)) return null;
		if (!kind.equals("temple") && !monumentByRoad && !Ruins.clear(layout, x, z, half)) return null;
		if (monumentByRoad && (layout.roadNear(x, z, half - InkLandsLayout.ROAD_HALF_WIDTH + 1) != null
				|| layout.nearLake(x, z, Ruins.LAKE_GAP) || layout.nearHerald(x, z, Ruins.HERALD_GAP))) return null;
		if (layout.depthInside(x, z) < 12) return null;
		boolean temple = kind.equals("temple");
		boolean pale = layout.pick(x, z, null) == com.tarkrakov.tearsofink.world.ModBiomes.PALE_FOREST;
		if (!temple && !pale && random.nextFloat() < Ruins.INKWOOD_SKIP) return null; // в Чернолесье реже

		Rotation rotation = Rotation.getRandom(random);
		return new Site(layout, template, x, z, half, rotation, temple, pale, null);
	}

	private static ResourceLocation pick(RandomSource random) {
		int total = 0;
		for (int w : Ruins.SCATTERED_WEIGHTS) total += w;
		int r = random.nextInt(total);
		for (int i = 0; i < Ruins.SCATTERED.length; i++) {
			r -= Ruins.SCATTERED_WEIGHTS[i];
			if (r < 0) return Ruins.SCATTERED[i];
		}
		return Ruins.SCATTERED[0];
	}

	/** Место сбоку от ближайшей дороги (если она рядом), на обочине в нескольких блоках. */
	private static int[] besideRoad(InkLandsLayout layout, int x, int z, RandomSource random, int half) {
		double[] hit = layout.roadNear(x, z, Ruins.MONUMENT_ROAD_SEARCH);
		if (hit == null) return null;
		int road = (int) hit[0];
		double along = hit[1];
		double[] p = layout.roadPoint(road, along);
		double[] dir = layout.roadDirection(road, along);
		double side = random.nextBoolean() ? 1 : -1;
		double off = InkLandsLayout.ROAD_HALF_WIDTH + InkLandsLayout.ROAD_SHOULDER + half
				+ Ruins.MONUMENT_ROAD_GAP_MIN + random.nextInt(Ruins.MONUMENT_ROAD_GAP_MAX - Ruins.MONUMENT_ROAD_GAP_MIN + 1);
		return new int[]{(int) Math.floor(p[0] - dir[1] * off * side), (int) Math.floor(p[1] + dir[0] * off * side)};
	}

	@Override
	public StructureType<?> type() {
		return Ruins.STRUCTURE_TYPE;
	}
}
