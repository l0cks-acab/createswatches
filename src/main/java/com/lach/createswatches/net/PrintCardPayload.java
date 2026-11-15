package com.lach.createswatches.net;

import com.lach.createswatches.CreateSwatches;
import com.lach.createswatches.CarouselBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;

import java.util.List;

/** Type a Swatch Card holding this palette and these blocks. */
public record PrintCardPayload(String name, List<Integer> palette, List<Item> blocks) implements CustomPacketPayload {
    public static final Type<PrintCardPayload> TYPE = new Type<>(CreateSwatches.id("print_card"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PrintCardPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), PrintCardPayload::name,
            ByteBufCodecs.INT.apply(ByteBufCodecs.list(CarouselBlockEntity.MAX_PALETTE)), PrintCardPayload::palette,
            ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list(CarouselBlockEntity.SLOTS)), PrintCardPayload::blocks,
            PrintCardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
