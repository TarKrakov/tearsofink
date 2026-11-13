package com.tarkrakov.tearsofink.world.ruins;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tarkrakov.tearsofink.world.InkLandsAccess;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
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

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkGenerator generator = context.chunkGenerator();
		if (!(generator.getBiomeSource() instanceof InkLandsAccess access)) return Optional.empty();
		InkLandsLayout layout = access.tearsofink$layout(context.randomState().sampler());
		if (layout == null) return Optional.empty();
		ChunkPos chunk = context.chunkPos();
		RandomSource random = context.random();
		ResourceLocation template;
		int x, z;

		if (kind.equals("temple")) {
			int[] at = null;
			for (int[] p : Ruins.templePoints(layout)) {
				if ((p[0] >> 4) == chunk.x && (p[1] >> 4) == chunk.z) { at = p; break; }
			}
			if (at == null) return Optional.empty();
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
		if (size.getX() == 0) return Optional.empty();
		int half = Math.max(size.getX(), size.getZ()) / 2 + 1;
		boolean monumentByRoad = false;
		if (template.equals(Ruins.MONUMENT) && random.nextFloat() < Ruins.MONUMENT_NEAR_ROAD) {
			int[] side = besideRoad(layout, x, z, random, half);
			if (side != null) { x = side[0]; z = side[1]; monumentByRoad = true; }
		}
		if (!kind.equals("temple") && Ruins.nearTemple(layout, x, z)) return Optional.empty();
		if (!kind.equals("temple") && !monumentByRoad && !Ruins.clear(layout, x, z, half)) return Optional.empty();
		if (monumentByRoad && (layout.roadNear(x, z, half - InkLandsLayout.ROAD_HALF_WIDTH + 1) != null
				|| layout.nearLake(x, z, Ruins.LAKE_GAP) || layout.nearHerald(x, z, Ruins.HERALD_GAP))) return Optional.empty();
		if (layout.depthInside(x, z) < 12) return Optional.empty();

		Rotation rotation = Rotation.getRandom(random);
		// высота: «нижняя середина» земли под постройкой (чтобы не висела, но и не тонула)
		int[] ys = new int[5];
		int k = 0;
		for (int[] o : new int[][]{{0, 0}, {-half, -half}, {half, -half}, {-half, half}, {half, half}}) {
			ys[k++] = generator.getBaseHeight(x + o[0], z + o[1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()) - 1;
		}
		Arrays.sort(ys);
		int y = ys[1];
		if (y <= generator.getSeaLevel()) return Optional.empty(); // не в воде

		BlockPos center = new BlockPos(x, y, z);
		ResourceLocation chosen = template;
		int tier = Ruins.tier(layout, x, z);
		boolean start = kind.equals("temple") && Ruins.isStartTemple(layout, x, z);
		boolean pale = layout.pick(x, z, null) == com.tarkrakov.tearsofink.world.ModBiomes.PALE_FOREST;
		return Optional.of(new GenerationStub(center, builder ->
				builder.addPiece(new RuinPiece(context.structureTemplateManager(), chosen, center, rotation, tier, start, pale))));
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
