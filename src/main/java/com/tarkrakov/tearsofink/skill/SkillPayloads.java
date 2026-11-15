package com.tarkrakov.tearsofink.skill;

import com.tarkrakov.tearsofink.TearsOfInk;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Сетевые сообщения навыков. */
public final class SkillPayloads {
	private SkillPayloads() {}

	/** Клиент → сервер: игрок нажал клавишу навыка. */
	public record Use(ResourceLocation skill, float strafe, float forward, boolean sneaking) implements CustomPacketPayload {
		public static final Type<Use> TYPE = new Type<>(TearsOfInk.id("use_skill"));
		public static final StreamCodec<ByteBuf, Use> CODEC = StreamCodec.composite(
				ResourceLocation.STREAM_CODEC, Use::skill,
				ByteBufCodecs.FLOAT, Use::strafe,
				ByteBufCodecs.FLOAT, Use::forward,
				ByteBufCodecs.BOOL, Use::sneaking,
				Use::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Клиент → сервер: игрок нажал клавишу абилки оружия (offhand — Shift: щит в левой руке). */
	public record UseWeapon(boolean offhand, float strafe, float forward) implements CustomPacketPayload {
		public static final Type<UseWeapon> TYPE = new Type<>(TearsOfInk.id("use_weapon"));
		public static final StreamCodec<ByteBuf, UseWeapon> CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, UseWeapon::offhand,
				ByteBufCodecs.FLOAT, UseWeapon::strafe,
				ByteBufCodecs.FLOAT, UseWeapon::forward,
				UseWeapon::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Сервер → клиент: подбросить вверх (прыжки путей) — скорость по горизонтали клиент сохраняет свою. */
	public record Boost(float up) implements CustomPacketPayload {
		public static final Type<Boost> TYPE = new Type<>(TearsOfInk.id("skill_boost"));
		public static final StreamCodec<ByteBuf, Boost> CODEC = StreamCodec.composite(ByteBufCodecs.FLOAT, Boost::up, Boost::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Сервер → клиент: на навык не хватило Чернил или Души — полоска пульсирует. */
	public record NotEnough(boolean soul) implements CustomPacketPayload {
		public static final Type<NotEnough> TYPE = new Type<>(TearsOfInk.id("not_enough"));
		public static final StreamCodec<ByteBuf, NotEnough> CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, NotEnough::soul,
				NotEnough::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
