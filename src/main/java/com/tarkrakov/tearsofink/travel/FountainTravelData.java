package com.tarkrakov.tearsofink.travel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.block.fountain.InkFountainBlock;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

record FountainTravelData(Set<Long> opened, Optional<BlockPos> last, Optional<BlockPos> legacyBinding, long cooldownUntil) {
	static final Codec<FountainTravelData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.listOf().xmap(Set::copyOf, set -> set.stream().toList()).optionalFieldOf("opened", Set.of()).forGetter(FountainTravelData::opened),
			BlockPos.CODEC.optionalFieldOf("last").forGetter(FountainTravelData::last),
			BlockPos.CODEC.optionalFieldOf("legacy_binding").forGetter(FountainTravelData::legacyBinding),
			Codec.LONG.optionalFieldOf("cooldown_until", 0L).forGetter(FountainTravelData::cooldownUntil)
	).apply(instance, FountainTravelData::new));

	private static final AttachmentType<FountainTravelData> DATA = AttachmentRegistry.<FountainTravelData>builder()
			.persistent(CODEC).copyOnDeath().buildAndRegister(TearsOfInk.id("fountain_travel"));

	// Initialize the attachment type before the server reads any player NBT.
	static void init() {}

	static FountainTravelData get(ServerPlayer player) {
		FountainTravelData data = player.getAttached(DATA);
		if (data == null) {
			Set<Long> drank = Optional.ofNullable(player.getAttached(InkFountainBlock.VISITED)).orElse(Set.of());
			Optional<BlockPos> binding = Optional.ofNullable(player.getAttached(Travel.DATA))
					.filter(travel -> !travel.respawnBed()).flatMap(LandsTravel::respawn)
					.filter(pos -> drank.contains(pos.asLong()));
			data = new FountainTravelData(drank, Optional.empty(), binding, 0L);
			save(player, data);
		}
		if (data.legacyBinding().isPresent()) {
			BlockPos pos = data.legacyBinding().get();
			var level = ModDimensions.lands(player.server);
			// An unloaded binding stays a candidate until the return request can check it.
			if (level != null) {
				if (!FountainLanding.inside(level, pos)) return data.resolveBinding(player, false);
				var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
				if (chunk != null) return data.resolveBinding(player, chunk.getBlockState(pos).is(ModBlocks.INK_FOUNTAIN));
			}
		}
		return data;
	}

	void activate(ServerPlayer player, BlockPos pos) {
		if (last.filter(pos::equals).isPresent() && opened.contains(pos.asLong())) return;
		Set<Long> points = new HashSet<>(opened);
		points.add(pos.asLong());
		save(player, new FountainTravelData(Set.copyOf(points), Optional.of(pos.immutable()), Optional.empty(), cooldownUntil));
	}

	FountainTravelData resolveBinding(ServerPlayer player, boolean valid) {
		FountainTravelData resolved = new FountainTravelData(opened, valid ? legacyBinding : last, Optional.empty(), cooldownUntil);
		save(player, resolved);
		return resolved;
	}

	void cooldown(ServerPlayer player, long until) {
		save(player, new FountainTravelData(opened, last, legacyBinding, until));
	}

	private static void save(ServerPlayer player, FountainTravelData data) {
		player.setAttached(DATA, data);
	}
}
