package com.lach.createswatches.client;

import com.lach.createswatches.CreateSwatches;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;

/** With Flywheel on, the shaft is an instanced visual; the renderer still draws the tile ring and screen. */
final class CarouselVisuals {
    private CarouselVisuals() {
    }

    static void register() {
        SimpleBlockEntityVisualizer.builder(CreateSwatches.CAROUSEL_BE.get())
                .factory(OrientedRotatingVisual.backHorizontal(CarouselRenderer.COG))
                .neverSkipVanillaRender()
                .apply();
    }
}
