package com.lach.createswatches.net;

import com.lach.createswatches.CreateSwatches;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Ask the server for a fresh {@link CandidatesPayload}. */
public record RefreshPayload() implements CustomPacketPayload {
    public static final RefreshPayload INSTANCE = new RefreshPayload();
    public static final Type<RefreshPayload> TYPE = new Type<>(CreateSwatches.id("refresh"));
    public static final StreamCodec<ByteBuf, RefreshPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
