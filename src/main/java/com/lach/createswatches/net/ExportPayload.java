package com.lach.createswatches.net;

import com.lach.createswatches.CreateSwatches;
import com.lach.createswatches.TypewriterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;

import java.util.List;

/** Pull (or order) {@code perItem} of each item; {@code palette} goes on the typewriter's paper. */
public record ExportPayload(List<Item> items, int perItem, List<Integer> palette) implements CustomPacketPayload {
    public static final Type<ExportPayload> TYPE = new Type<>(CreateSwatches.id("export"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExportPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list(TypewriterBlockEntity.SLOTS)), ExportPayload::items,
            ByteBufCodecs.VAR_INT.map(v -> Math.clamp(v, 1, 64 * TypewriterBlockEntity.SLOTS), v -> v), ExportPayload::perItem,
            ByteBufCodecs.INT.apply(ByteBufCodecs.list(TypewriterBlockEntity.MAX_PALETTE)), ExportPayload::palette,
            ExportPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
