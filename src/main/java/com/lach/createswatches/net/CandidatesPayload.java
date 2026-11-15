package com.lach.createswatches.net;

import com.lach.createswatches.CreateSwatches;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/** Block items the carousel can see, with amounts, plus where they come from and whether it is turning. */
public record CandidatesPayload(int source, boolean running, String address, List<Item> items, long[] amounts) implements CustomPacketPayload {
    public static final Type<CandidatesPayload> TYPE = new Type<>(CreateSwatches.id("candidates"));
    private static final StreamCodec<RegistryFriendlyByteBuf, Item> ITEM = ByteBufCodecs.registry(Registries.ITEM);
    public static final StreamCodec<RegistryFriendlyByteBuf, CandidatesPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        buf.writeVarInt(p.source);
        buf.writeBoolean(p.running);
        buf.writeUtf(p.address, 64);
        buf.writeVarInt(p.items.size());
        for (int i = 0; i < p.items.size(); i++) {
            ITEM.encode(buf, p.items.get(i));
            buf.writeVarLong(p.amounts[i]);
        }
    }, buf -> {
        int source = buf.readVarInt();
        boolean running = buf.readBoolean();
        String address = buf.readUtf(64);
        int n = Math.min(buf.readVarInt(), 65536);
        List<Item> items = new ArrayList<>(n);
        long[] amounts = new long[n];
        for (int i = 0; i < n; i++) {
            items.add(ITEM.decode(buf));
            amounts[i] = buf.readVarLong();
        }
        return new CandidatesPayload(source, running, address, items, amounts);
    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
