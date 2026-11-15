package com.lach.createswatches.client;

import com.lach.createswatches.CreateSwatches;
import com.lach.createswatches.SwatchCardData;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CreateSwatches.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    static void screens(RegisterMenuScreensEvent event) {
        event.<com.lach.createswatches.CarouselMenu, TerminalScreen>register(CreateSwatches.CAROUSEL_MENU.get(), (menu, inv, title) -> {
            SwatchCardData card = inv.player.getMainHandItem().get(CreateSwatches.CARD_DATA.get());
            if (card != null) {
                ThemeScreen.pendingCard = card;
                TerminalScreen.themeMode = true;
            }
            return TerminalScreen.themeMode ? new ThemeScreen(menu, inv, title) : new SwatchScreen(menu, inv, title);
        });
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CreateSwatches.CAROUSEL_BE.get(), CarouselRenderer::new);
    }

    @SubscribeEvent
    static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(CarouselVisuals::register);
    }

    @SubscribeEvent
    static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) rm -> BlockColorCache.clear());
    }
}
