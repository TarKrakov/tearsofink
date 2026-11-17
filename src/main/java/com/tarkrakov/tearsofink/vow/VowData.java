package com.tarkrakov.tearsofink.vow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Обет и рост Души игрока (хранится на игроке, переживает смерть).
 *
 * @param vow        выбранный путь
 * @param changes    историческое число смен обета в старых сохранениях (теперь не меняется)
 * @param exchanged  сколько Души получено обменом Чернил у Торговки (до {@link Vows#EXCHANGE_LIMIT})
 * @param awakenings пробуждения Души — каждое раз за игру (id: contract_0…2, miniboss, vanguard, final, diary, tempering, mercy)
 */
public record VowData(Vow vow, int changes, int exchanged, List<String> awakenings) {
	public static final Codec<VowData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.optionalFieldOf("vow", "none").forGetter(d -> d.vow().id),
			Codec.INT.optionalFieldOf("changes", 0).forGetter(VowData::changes),
			Codec.INT.optionalFieldOf("exchanged", 0).forGetter(VowData::exchanged),
			Codec.STRING.listOf().optionalFieldOf("awakenings", List.of()).forGetter(VowData::awakenings)
	).apply(i, (vow, changes, exchanged, awakenings) -> new VowData(parse(vow), changes, exchanged, List.copyOf(awakenings))));

	public static VowData fresh() {
		return new VowData(Vow.NONE, 0, 0, List.of());
	}

	private static Vow parse(String id) {
		for (Vow v : Vow.values()) if (v.id.equals(id)) return v;
		return Vow.NONE;
	}

	public VowData withVow(Vow v) {
		return new VowData(v, changes, exchanged, awakenings);
	}

	public VowData withExchanged(int e) {
		return new VowData(vow, changes, Math.max(0, e), awakenings);
	}

	public VowData withAwakening(String id) {
		List<String> list = new ArrayList<>(awakenings);
		list.add(id);
		return new VowData(vow, changes, exchanged, List.copyOf(list));
	}
}
