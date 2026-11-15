package com.lach.createswatches;

import com.lach.createswatches.net.CandidatesPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CarouselMenu extends AbstractContainerMenu {
    /** Most block types sent to the client in one listing. */
    public static final int MAX_CANDIDATES = 32768;
    public static final int OUT_X = 188, OUT_Y = 144, INV_X = 8, INV_Y = 144;

    private final @Nullable CarouselBlockEntity be;
    private final Player player;
    private final ContainerLevelAccess access;

    /** Client side. */
    public CarouselMenu(int id, Inventory inv) {
        this(id, inv, null, new ItemStackHandler(CarouselBlockEntity.SLOTS), ContainerLevelAccess.NULL);
    }

    /** Server side. */
    public CarouselMenu(int id, Inventory inv, CarouselBlockEntity be) {
        this(id, inv, be, be.output(), ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()));
    }

    private CarouselMenu(int id, Inventory inv, @Nullable CarouselBlockEntity be, ItemStackHandler output, ContainerLevelAccess access) {
        super(CreateSwatches.CAROUSEL_MENU.get(), id);
        this.be = be;
        this.player = inv.player;
        this.access = access;
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new SlotItemHandler(output, c + r * 9, OUT_X + c * 18, OUT_Y + r * 18));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, INV_X + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, INV_X + c * 18, INV_Y + 58));
    }

    public @Nullable CarouselBlockEntity carousel() {
        return be;
    }

    /** Where the carousel is, for linking an applicator; null on the client. */
    public @Nullable GlobalPos carouselPos() {
        return be == null || be.getLevel() == null ? null : GlobalPos.of(be.getLevel().dimension(), be.getBlockPos());
    }

    /** Sends the block items the carousel can see to this menu's player. */
    public void sendCandidates() {
        if (!(player instanceof ServerPlayer sp) || be == null) return;
        List<Item> items = new ArrayList<>();
        List<Long> amounts = new ArrayList<>();
        boolean running = be.running();
        if (running) for (Map.Entry<Item, Long> e : be.stock().entrySet()) {
            if (items.size() >= MAX_CANDIDATES) break;
            items.add(e.getKey());
            amounts.add(e.getValue());
        }
        PacketDistributor.sendToPlayer(sp, new CandidatesPayload(be.source().ordinal(), running, be.address(), items,
                amounts.stream().mapToLong(Long::longValue).toArray()));
    }

    public void export(List<Item> items, int perItem, List<Integer> palette) {
        if (be == null) return;
        be.setPalette(palette.stream().mapToInt(Integer::intValue).toArray());
        Component msg = be.export(items, perItem, player);
        if (!msg.getString().isEmpty()) player.displayClientMessage(msg, true);
    }

    public void returnAll() {
        if (be != null) be.returnAll();
    }

    public void setAddress(String address) {
        if (be != null) be.setAddress(address.trim());
    }

    /** Types a Swatch Card into the player's inventory, using a paper if the config asks for one. */
    public void printCard(String name, List<Integer> palette, List<Item> blocks) {
        if (be == null || palette.isEmpty()) return;
        if (!be.running()) {
            player.displayClientMessage(Component.translatable("gui.createswatches.no_rotation").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (Config.CARD_COSTS_PAPER.get() && !player.getAbilities().instabuild) {
            int slot = player.getInventory().findSlotMatchingItem(new ItemStack(Items.PAPER));
            if (slot < 0) {
                player.displayClientMessage(Component.translatable("gui.createswatches.no_paper").withStyle(ChatFormatting.RED), true);
                return;
            }
            player.getInventory().removeItem(slot, 1);
        }
        ItemStack card = new ItemStack(CreateSwatches.SWATCH_CARD.get());
        card.set(CreateSwatches.CARD_DATA.get(), new SwatchCardData(name, List.copyOf(palette), List.copyOf(blocks)));
        if (!player.getInventory().add(card)) player.drop(card, false);
        be.setPalette(palette.stream().mapToInt(Integer::intValue).toArray());
        be.startTyping();
        player.displayClientMessage(Component.translatable("gui.createswatches.card_typed", name).withStyle(ChatFormatting.GREEN), true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        int out = CarouselBlockEntity.SLOTS;
        if (index < out) {
            if (!moveItemStackTo(stack, out, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, out, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof CarouselBlock
                && player.canInteractWithBlock(pos, 4.0), true);
    }
}
