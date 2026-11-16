package com.lach.createswatches;

import com.lach.createswatches.net.AddressPayload;
import com.lach.createswatches.net.ApplicatorConfigPayload;
import com.lach.createswatches.net.CandidatesPayload;
import com.lach.createswatches.net.CycleApplicatorPayload;
import com.lach.createswatches.net.ExportPayload;
import com.lach.createswatches.net.LoadApplicatorPayload;
import com.lach.createswatches.net.PrintCardPayload;
import com.lach.createswatches.net.RefreshPayload;
import com.lach.createswatches.net.ReturnPayload;
import com.simibubi.create.api.stress.BlockStressValues;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;

@Mod(CreateSwatches.MODID)
public class CreateSwatches {
    public static final String MODID = "createswatches";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ApplicatorData>> APPLICATOR_DATA = COMPONENTS.registerComponentType("applicator",
            b -> b.persistent(ApplicatorData.CODEC).networkSynchronized(ApplicatorData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SwatchCardData>> CARD_DATA = COMPONENTS.registerComponentType("swatch_card",
            b -> b.persistent(SwatchCardData.CODEC).networkSynchronized(SwatchCardData.STREAM_CODEC));

    public static final DeferredBlock<CarouselBlock> CAROUSEL = BLOCKS.registerBlock("swatch_carousel", CarouselBlock::new, CarouselBlock.PROPERTIES);
    public static final DeferredItem<CarouselItem> CAROUSEL_ITEM = ITEMS.register("swatch_carousel", () -> new CarouselItem(CAROUSEL.get(), new Item.Properties()));
    public static final DeferredItem<ApplicatorItem> APPLICATOR = ITEMS.registerItem("swatch_applicator", ApplicatorItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<SwatchCardItem> SWATCH_CARD = ITEMS.registerItem("swatch_card", SwatchCardItem::new, new Item.Properties().stacksTo(16));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CarouselBlockEntity>> CAROUSEL_BE = BLOCK_ENTITIES.register("swatch_carousel",
            () -> BlockEntityType.Builder.of((pos, state) -> new CarouselBlockEntity(CreateSwatches.CAROUSEL_BE.get(), pos, state), CAROUSEL.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<CarouselMenu>> CAROUSEL_MENU = MENUS.register("swatch_carousel",
            () -> IMenuTypeExtension.create((id, inv, buf) -> new CarouselMenu(id, inv)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.createswatches"))
            .icon(() -> new ItemStack(CAROUSEL_ITEM.get()))
            .displayItems((p, out) -> {
                out.accept(CAROUSEL_ITEM.get());
                out.accept(APPLICATOR.get());
            }).build());

    public CreateSwatches(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(CreateSwatches::registerCaps);
        modBus.addListener(CreateSwatches::registerPayloads);
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(() ->
                BlockStressValues.IMPACTS.register(CAROUSEL.get(), () -> Config.SPEC.isLoaded() ? Config.STRESS_IMPACT.get() : 1.0)));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private static void registerCaps(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CAROUSEL_BE.get(), (be, side) -> be.external());
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToClient(CandidatesPayload.TYPE, CandidatesPayload.STREAM_CODEC, (p, ctx) -> ClientBridge.candidates(p));
        r.playToServer(RefreshPayload.TYPE, RefreshPayload.STREAM_CODEC, (p, ctx) -> {
            CarouselMenu menu = openMenu(ctx);
            if (menu != null) menu.sendCandidates();
        });
        r.playToServer(ReturnPayload.TYPE, ReturnPayload.STREAM_CODEC, (p, ctx) -> {
            CarouselMenu menu = openMenu(ctx);
            if (menu != null) {
                menu.returnAll();
                menu.sendCandidates();
            }
        });
        r.playToServer(ExportPayload.TYPE, ExportPayload.STREAM_CODEC, (p, ctx) -> {
            CarouselMenu menu = openMenu(ctx);
            if (menu != null) {
                menu.export(p.items(), p.perItem(), p.palette());
                menu.sendCandidates();
            }
        });
        r.playToServer(AddressPayload.TYPE, AddressPayload.STREAM_CODEC, (p, ctx) -> {
            CarouselMenu menu = openMenu(ctx);
            if (menu != null) menu.setAddress(p.address());
        });
        r.playToServer(PrintCardPayload.TYPE, PrintCardPayload.STREAM_CODEC, (p, ctx) -> {
            CarouselMenu menu = openMenu(ctx);
            if (menu != null) menu.printCard(p.name(), p.palette(), p.blocks());
        });
        r.playToServer(LoadApplicatorPayload.TYPE, LoadApplicatorPayload.STREAM_CODEC, (p, ctx) -> {
            CarouselMenu menu = openMenu(ctx);
            if (menu != null) loadApplicator((ServerPlayer) ctx.player(), menu, p);
        });
        r.playToServer(ApplicatorConfigPayload.TYPE, ApplicatorConfigPayload.STREAM_CODEC, (p, ctx) -> {
            ItemStack held = ctx.player().getItemInHand(p.hand());
            if (!(held.getItem() instanceof ApplicatorItem)) return;
            ApplicatorData d = ApplicatorItem.data(held);
            held.set(APPLICATOR_DATA.get(), d.withSelected(p.selected() < 0 ? ApplicatorData.RANDOM : p.selected()).withPlacement(p.mode(), p.size()));
        });
        r.playToServer(CycleApplicatorPayload.TYPE, CycleApplicatorPayload.STREAM_CODEC, (p, ctx) -> {
            ItemStack held = ctx.player().getMainHandItem();
            if (held.getItem() instanceof ApplicatorItem) ApplicatorItem.cycle(ctx.player(), held, p.dir());
        });
    }

    /** Fills the first applicator found (hands first, then inventory) with the carousel's picks and links it. */
    private static void loadApplicator(ServerPlayer player, CarouselMenu menu, LoadApplicatorPayload p) {
        GlobalPos link = menu.carouselPos();
        if (link == null) return;
        List<Item> blocks = p.items().stream().filter(i -> i instanceof BlockItem).distinct().toList();
        ItemStack target = ItemStack.EMPTY;
        for (ItemStack s : List.of(player.getMainHandItem(), player.getOffhandItem())) if (s.is(APPLICATOR.get())) target = s;
        for (int i = 0; target.isEmpty() && i < player.getInventory().getContainerSize(); i++)
            if (player.getInventory().getItem(i).is(APPLICATOR.get())) target = player.getInventory().getItem(i);
        if (target.isEmpty()) {
            player.displayClientMessage(Component.translatable("gui.createswatches.no_applicator").withStyle(ChatFormatting.RED), false);
            return;
        }
        if (blocks.isEmpty()) return;
        ApplicatorData old = ApplicatorItem.data(target);
        target.set(APPLICATOR_DATA.get(), new ApplicatorData(blocks, 0, p.name(), Optional.of(link), old.mode(), old.size()));
        player.displayClientMessage(Component.translatable("gui.createswatches.applicator_loaded", blocks.size(), p.name()).withStyle(ChatFormatting.GREEN), false);
    }

    private static CarouselMenu openMenu(IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp && sp.containerMenu instanceof CarouselMenu m && m.stillValid(sp)) return m;
        return null;
    }
}
