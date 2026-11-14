package com.lach.createswatches;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Paper card with a palette typed on it. Use it on a typewriter to open it with that palette. */
public class SwatchCardItem extends Item {
    public SwatchCardItem(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        SwatchCardData d = stack.get(CreateSwatches.CARD_DATA.get());
        return d == null || d.name().isBlank() ? super.getName(stack) : Component.translatable("item.createswatches.swatch_card.named", d.name());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        SwatchCardData d = stack.get(CreateSwatches.CARD_DATA.get());
        if (d == null) {
            tip.add(Component.translatable("item.createswatches.swatch_card.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        var chips = Component.empty();
        for (int c : d.palette()) chips.append(Component.literal("█").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(c))));
        tip.add(chips);
        var hex = new StringBuilder();
        for (int c : d.palette()) hex.append(hex.isEmpty() ? "" : " ").append(String.format("#%06X", c & 0xFFFFFF));
        tip.add(Component.literal(hex.toString()).withStyle(ChatFormatting.DARK_GRAY));
        if (!d.blocks().isEmpty())
            tip.add(Component.translatable("item.createswatches.swatch_card.blocks", d.blocks().size()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.createswatches.swatch_card.use").withStyle(ChatFormatting.DARK_AQUA));
    }
}
