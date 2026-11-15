package com.lach.createswatches;

import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Right-click a Stock Link to tune the carousel to its logistics network, like a Stock Ticker. Right-click air to clear. */
public class CarouselItem extends BlockItem {
    public CarouselItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return LogisticallyLinkedBlockItem.isTuned(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!LogisticallyLinkedBlockItem.isTuned(stack)) return super.use(level, player, hand);
        if (!level.isClientSide) {
            stack.remove(DataComponents.BLOCK_ENTITY_DATA);
            player.displayClientMessage(Component.translatable("item.createswatches.carousel.cleared"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player != null && !player.isShiftKeyDown()) {
            LogisticallyLinkedBehaviour link = BlockEntityBehaviour.get(ctx.getLevel(), ctx.getClickedPos(), LogisticallyLinkedBehaviour.TYPE);
            if (link != null && !(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof CarouselBlockEntity)) {
                if (!ctx.getLevel().isClientSide && link.mayInteractMessage(player))
                    LogisticallyLinkedBlockItem.assignFrequency(ctx.getItemInHand(), player, link.freqId);
                return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
            }
        }
        return super.useOn(ctx);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tip, flag);
        tip.add(Component.translatable(LogisticallyLinkedBlockItem.isTuned(stack) ? "item.createswatches.carousel.tuned"
                : "item.createswatches.carousel.tip").withStyle(ChatFormatting.GRAY));
    }
}
