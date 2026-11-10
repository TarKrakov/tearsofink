package com.tarkrakov.tearsofink.ink;

import com.tarkrakov.tearsofink.TearsOfInk;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Сервер → клиент: текущие значения Души и Чернил (для полосок). */
public record InkSyncPayload(int soulMax, float soul, int inkMax, float ink, boolean inside) implements CustomPacketPayload {
	public static final Type<InkSyncPayload> TYPE = new Type<>(TearsOfInk.id("ink_sync"));

	public static final StreamCodec<ByteBuf, InkSyncPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, InkSyncPayload::soulMax,
			ByteBufCodecs.FLOAT, InkSyncPayload::soul,
			ByteBufCodecs.VAR_INT, InkSyncPayload::inkMax,
			ByteBufCodecs.FLOAT, InkSyncPayload::ink,
			ByteBufCodecs.BOOL, InkSyncPayload::inside,
			InkSyncPayload::new);

	public static InkSyncPayload of(InkData data) {
		return new InkSyncPayload(data.soulMax(), data.soul(), data.inkMax(), data.ink(), data.inside());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
