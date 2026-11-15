package com.lach.createswatches;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CarouselBlockEntity extends KineticBlockEntity implements MenuProvider {
    public static final int SLOTS = 27;
    public static final int MAX_PALETTE = 8;
    private static final int TYPE_TICKS = 36;

    /** Where the carousel reads blocks from. */
    public enum Source {
        NONE, INVENTORY, NETWORK
    }

    public LogisticallyLinkedBehaviour link;
    private boolean tuned;
    private String address = "";
    private int[] palette = new int[0];
    /** Most-stocked blocks, synced so the ring can show what the storage holds when no palette is set. */
    private List<Item> showcase = List.of();
    public static final int RING = 28;
    private int typing;

    private final ItemStackHandler output = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** Seen by pipes, funnels and chutes: takes packages (and unpacks them), gives out the output slots. */
    private final IItemHandler external = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return output.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!PackageItem.isPackage(stack)) return stack;
            return unpack(stack, simulate) ? ItemStack.EMPTY : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return output.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return output.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return PackageItem.isPackage(stack);
        }
    };

    public CarouselBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(link = new LogisticallyLinkedBehaviour(this, false));
    }

    public ItemStackHandler output() {
        return output;
    }

    public IItemHandler external() {
        return external;
    }

    public String address() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address.length() > 64 ? address.substring(0, 64) : address;
        setChanged();
        sendData();
    }

    public int[] palette() {
        return palette;
    }

    public List<Item> showcase() {
        return showcase;
    }

    private void refreshShowcase() {
        List<Item> top = stock().entrySet().stream().sorted(Map.Entry.<Item, Long>comparingByValue().reversed())
                .limit(RING).map(Map.Entry::getKey).toList();
        if (!top.equals(showcase)) {
            showcase = top;
            sendData();
        }
    }

    public int typing() {
        return typing;
    }

    /** Puts every package item's contents into the output, or nothing when they don't all fit. */
    private boolean unpack(ItemStack box, boolean simulate) {
        ItemStackHandler contents = PackageItem.getContents(box);
        ItemStackHandler test = new ItemStackHandler(SLOTS);
        for (int i = 0; i < SLOTS; i++) test.setStackInSlot(i, output.getStackInSlot(i).copy());
        for (int i = 0; i < contents.getSlots(); i++)
            if (!ItemHandlerHelper.insertItemStacked(test, contents.getStackInSlot(i).copy(), false).isEmpty()) return false;
        if (!simulate) for (int i = 0; i < contents.getSlots(); i++)
            ItemHandlerHelper.insertItemStacked(output, contents.getStackInSlot(i).copy(), false);
        return true;
    }

    /** Set when placed from an item tuned on a Stock Link. */
    public void setTuned(boolean tuned) {
        this.tuned = tuned;
        setChanged();
        sendData();
    }

    public Source source() {
        if (tuned) return Source.NETWORK;
        return attached() != null ? Source.INVENTORY : Source.NONE;
    }

    /** The inventory the carousel sits on, if any. */
    public @Nullable IItemHandler attached() {
        if (level == null) return null;
        BlockPos below = worldPosition.below();
        if (level.getBlockEntity(below) instanceof CarouselBlockEntity) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, below, Direction.UP);
    }

    /** Turning fast enough (or rotation is switched off in the config). */
    public boolean running() {
        return !Config.NEEDS_ROTATION.get() || getSpeed() != 0 && !isOverStressed();
    }

    private static boolean plainBlock(ItemStack s) {
        return !s.isEmpty() && s.getItem() instanceof BlockItem && s.getComponentsPatch().isEmpty();
    }

    /** Plain block items the source holds, with amounts, in the order found. */
    public Map<Item, Long> stock() {
        Map<Item, Long> out = new LinkedHashMap<>();
        switch (source()) {
            case INVENTORY -> {
                IItemHandler inv = attached();
                if (inv != null) for (int i = 0; i < inv.getSlots(); i++) {
                    ItemStack s = inv.getStackInSlot(i);
                    if (plainBlock(s)) out.merge(s.getItem(), (long) s.getCount(), Long::sum);
                }
            }
            case NETWORK -> {
                InventorySummary sum = LogisticsManager.getSummaryOfNetwork(link.freqId, true);
                for (BigItemStack b : sum.getStacks())
                    if (b.count > 0 && plainBlock(b.stack)) out.merge(b.stack.getItem(), (long) b.count, Long::sum);
            }
            case NONE -> {
            }
        }
        return out;
    }

    /** Pulls up to {@code perItem} of each block into the output (storage), or orders them to the address (network). */
    public Component export(List<Item> items, int perItem, Player player) {
        if (!running()) return Component.translatable("gui.createswatches.no_rotation").withStyle(ChatFormatting.RED);
        Source src = source();
        if (src == Source.INVENTORY) {
            IItemHandler inv = attached();
            if (inv == null) return Component.empty();
            int moved = 0;
            for (Item item : items) {
                if (!(item instanceof BlockItem)) continue;
                int want = Math.min(perItem, item.getDefaultMaxStackSize() * SLOTS);
                for (int i = 0; i < inv.getSlots() && want > 0; i++) {
                    ItemStack s = inv.getStackInSlot(i);
                    if (!s.is(item) || !s.getComponentsPatch().isEmpty()) continue;
                    ItemStack sim = inv.extractItem(i, Math.min(want, s.getMaxStackSize()), true);
                    if (sim.isEmpty()) continue;
                    int fits = sim.getCount() - ItemHandlerHelper.insertItemStacked(output, sim, true).getCount();
                    if (fits <= 0) break;
                    ItemStack got = inv.extractItem(i, fits, false);
                    ItemStack rest = ItemHandlerHelper.insertItemStacked(output, got, false);
                    if (!rest.isEmpty()) ItemHandlerHelper.insertItemStacked(inv, rest, false);
                    want -= got.getCount() - rest.getCount();
                    moved += got.getCount() - rest.getCount();
                }
            }
            if (moved > 0) startTyping();
            return Component.empty();
        }
        if (src == Source.NETWORK) {
            if (address.isBlank()) return Component.translatable("gui.createswatches.no_address").withStyle(ChatFormatting.RED);
            Map<Item, Long> have = stock();
            List<BigItemStack> order = new ArrayList<>();
            for (Item item : items) {
                long n = Math.min(perItem, have.getOrDefault(item, 0L));
                if (n > 0) order.add(new BigItemStack(new ItemStack(item), (int) n));
            }
            if (order.isEmpty()) return Component.empty();
            boolean ok = LogisticsManager.broadcastPackageRequest(link.freqId, LogisticallyLinkedBehaviour.RequestType.PLAYER,
                    PackageOrderWithCrafts.simple(order), null, address);
            if (!ok) return Component.translatable("gui.createswatches.order_busy").withStyle(ChatFormatting.RED);
            startTyping();
            return Component.translatable("gui.createswatches.ordered", order.size(), address).withStyle(ChatFormatting.GREEN);
        }
        return Component.empty();
    }

    /** Puts the output slots back into the storage it sits on. */
    public void returnAll() {
        IItemHandler inv = source() == Source.INVENTORY ? attached() : null;
        if (inv == null || !running()) return;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = output.getStackInSlot(i);
            if (s.isEmpty()) continue;
            ItemStack rest = ItemHandlerHelper.insertItemStacked(inv, s.copy(), false);
            output.setStackInSlot(i, rest);
        }
    }

    /**
     * Takes one of {@code item} for an applicator: from the output slots first, then the storage underneath.
     * Network blocks have to be ordered into the carousel first.
     */
    public boolean take(Item item, boolean simulate) {
        if (!running()) return false;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = output.getStackInSlot(i);
            if (s.is(item) && s.getComponentsPatch().isEmpty()) return !output.extractItem(i, 1, simulate).isEmpty();
        }
        IItemHandler inv = source() == Source.INVENTORY ? attached() : null;
        if (inv == null) return false;
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s.is(item) && s.getComponentsPatch().isEmpty() && !inv.extractItem(i, 1, simulate).isEmpty()) return true;
        }
        return false;
    }

    public void setPalette(int[] colours) {
        int n = Math.min(colours.length, MAX_PALETTE);
        palette = new int[n];
        for (int i = 0; i < n; i++) palette[i] = colours[i] & 0xFFFFFF;
        setChanged();
        sendData();
    }

    public void startTyping() {
        typing = TYPE_TICKS;
        sendData();
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide && (level.getGameTime() + worldPosition.hashCode()) % 100 == 0) refreshShowcase();
        if (typing <= 0) return;
        typing--;
        if (level != null && !level.isClientSide) {
            if (typing > 6 && typing % 3 == 0)
                level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.35f, 1.6f + level.random.nextFloat() * 0.4f);
            if (typing == 2) level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.5f, 2f);
        }
    }

    public void dropContents() {
        if (level == null) return;
        for (int i = 0; i < SLOTS; i++) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), output.getStackInSlot(i));
        for (int i = 0; i < SLOTS; i++) output.setStackInSlot(i, ItemStack.EMPTY);
    }

    @Override
    public void destroy() {
        super.destroy();
        dropContents();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider regs, boolean clientPacket) {
        super.write(tag, regs, clientPacket);
        tag.putBoolean("Tuned", tuned);
        tag.putString("Address", address);
        tag.putIntArray("Palette", palette);
        if (clientPacket) {
            tag.putInt("Typing", typing);
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (Item i : showcase) list.add(net.minecraft.nbt.StringTag.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(i).toString()));
            tag.put("Showcase", list);
        }
        else tag.put("Output", output.serializeNBT(regs));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider regs, boolean clientPacket) {
        super.read(tag, regs, clientPacket);
        tuned = tag.getBoolean("Tuned");
        address = tag.getString("Address");
        palette = tag.getIntArray("Palette");
        if (clientPacket) {
            typing = tag.getInt("Typing");
            List<Item> list = new ArrayList<>();
            for (net.minecraft.nbt.Tag t : tag.getList("Showcase", net.minecraft.nbt.Tag.TAG_STRING)) {
                net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(t.getAsString());
                if (id != null) net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id).ifPresent(list::add);
            }
            showcase = list;
        }
        else if (tag.contains("Output")) output.deserializeNBT(regs, tag.getCompound("Output"));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        tooltip.add(Component.literal("    ").append(Component.translatable("goggles.createswatches.source." + source().name().toLowerCase())
                .withStyle(ChatFormatting.GRAY)));
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CarouselMenu(id, inv, this);
    }
}
