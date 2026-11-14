package com.lach.createswatches.net;

import com.lach.createswatches.CreateSwatches;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The address network orders are sent to. */
public record AddressPayload(String address) implements CustomPacketPayload {
    public static final Type<AddressPayload> TYPE = new Type<>(CreateSwatches.id("address"));
    public static final StreamCodec<ByteBuf, AddressPayload> STREAM_CODEC = ByteBufCodecs.stringUtf8(64).map(AddressPayload::new, AddressPayload::address);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
