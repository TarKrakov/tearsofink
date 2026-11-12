package com.tarkrakov.tearsofink.travel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.Optional;

/**
 * Путешествия игрока между мирами (хранится на игроке, переживает смерть).
 *
 * @param respawn     где очнуться после смерти в Землях (кровать в Землях или фонтан)
 * @param respawnBed  respawn — это кровать (иначе фонтан)
 * @param lastPos     где игрок был в Землях, когда последний раз ушёл через зеркальце
 * @param lastYaw     куда смотрел
 * @param keepMirror  при смерти зеркальце забрано из инвентаря — вернуть после возрождения
 * @param tosses      сколько раз подряд выбрасывал зеркальце (для скрытой реплики)
 * @param stage       пролог: 0 — ничего, 1 — получил письмо, 2 — подписал контракт, 3 — прошёл ворота Преддверия
 */
public record LandsTravel(Optional<BlockPos> respawn, boolean respawnBed, Optional<BlockPos> lastPos, float lastYaw,
						  int keepMirror, int tosses, int stage) {
	public static final int STAGE_LETTER = 1, STAGE_SIGNED = 2, STAGE_ENTERED = 3;

	public static final Codec<LandsTravel> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.optionalFieldOf("respawn").forGetter(LandsTravel::respawn),
			Codec.BOOL.optionalFieldOf("respawn_bed", false).forGetter(LandsTravel::respawnBed),
			BlockPos.CODEC.optionalFieldOf("last_pos").forGetter(LandsTravel::lastPos),
			Codec.FLOAT.optionalFieldOf("last_yaw", 0f).forGetter(LandsTravel::lastYaw),
			Codec.INT.optionalFieldOf("keep_mirror", 0).forGetter(LandsTravel::keepMirror),
			Codec.INT.optionalFieldOf("tosses", 0).forGetter(LandsTravel::tosses),
			Codec.INT.optionalFieldOf("stage", 0).forGetter(LandsTravel::stage)
	).apply(i, LandsTravel::new));

	public static LandsTravel fresh() {
		return new LandsTravel(Optional.empty(), false, Optional.empty(), 0f, 0, 0, 0);
	}

	public LandsTravel withRespawn(BlockPos pos, boolean bed) {
		return new LandsTravel(Optional.of(pos), bed, lastPos, lastYaw, keepMirror, tosses, stage);
	}

	public LandsTravel withoutRespawn() {
		return new LandsTravel(Optional.empty(), false, lastPos, lastYaw, keepMirror, tosses, stage);
	}

	public LandsTravel withLast(BlockPos pos, float yaw) {
		return new LandsTravel(respawn, respawnBed, Optional.of(pos), yaw, keepMirror, tosses, stage);
	}

	public LandsTravel withKeepMirror(int count) {
		return new LandsTravel(respawn, respawnBed, lastPos, lastYaw, count, tosses, stage);
	}

	public LandsTravel withTosses(int count) {
		return new LandsTravel(respawn, respawnBed, lastPos, lastYaw, keepMirror, count, stage);
	}

	public LandsTravel withStage(int value) {
		return new LandsTravel(respawn, respawnBed, lastPos, lastYaw, keepMirror, tosses, value);
	}
}
