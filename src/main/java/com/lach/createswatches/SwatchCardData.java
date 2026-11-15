package com.lach.createswatches;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

import java.util.List;

/** A typed palette: its name, colours (0xRRGGBB) and the blocks picked for it. */
public record SwatchCardData(String name, List<Integer> palette, List<Item> blocks) {
    public static final Codec<SwatchCardData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(SwatchCardData::name),
            Codec.INT.listOf().fieldOf("palette").forGetter(SwatchCardData::palette),
            BuiltInRegistries.ITEM.byNameCodec().listOf().optionalFieldOf("blocks", List.of()).forGetter(SwatchCardData::blocks)
    ).apply(i, SwatchCardData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwatchCardData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SwatchCardData::name,
            ByteBufCodecs.INT.apply(ByteBufCodecs.list(CarouselBlockEntity.MAX_PALETTE)), SwatchCardData::palette,
            ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list(CarouselBlockEntity.SLOTS)), SwatchCardData::blocks,
            SwatchCardData::new);
}
