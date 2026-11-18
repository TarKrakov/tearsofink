package com.tarkrakov.tearsofink.travel;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class FountainTravelPackets {
	public static final int PAGE_SIZE = 5;

	private FountainTravelPackets() {}

	public record Menu(UUID session, BlockPos source, Optional<BlockPos> last, int page, int pages, List<BlockPos> entries)
			implements CustomPacketPayload {
		public static final Type<Menu> TYPE = new Type<>(TearsOfInk.id("fountain_menu"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Menu> CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, Menu::session, BlockPos.STREAM_CODEC, Menu::source,
				BlockPos.STREAM_CODEC.apply(ByteBufCodecs::optional), Menu::last,
				ByteBufCodecs.VAR_INT, Menu::page, ByteBufCodecs.VAR_INT, Menu::pages,
				BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(PAGE_SIZE)), Menu::entries, Menu::new);
		@Override public Type<Menu> type() { return TYPE; }
	}

	/** A row on the current page, or -1 to request a different page. */
	public record Select(UUID session, int page, int row) implements CustomPacketPayload {
		public static final Type<Select> TYPE = new Type<>(TearsOfInk.id("fountain_select"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Select> CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, Select::session, ByteBufCodecs.VAR_INT, Select::page,
				ByteBufCodecs.VAR_INT, Select::row, Select::new);
		@Override public Type<Select> type() { return TYPE; }
	}

	public record Close(UUID session) implements CustomPacketPayload {
		public static final Type<Close> TYPE = new Type<>(TearsOfInk.id("fountain_close"));
		public static final StreamCodec<io.netty.buffer.ByteBuf, Close> CODEC = UUIDUtil.STREAM_CODEC.map(Close::new, Close::session);
		@Override public Type<Close> type() { return TYPE; }
	}
}
