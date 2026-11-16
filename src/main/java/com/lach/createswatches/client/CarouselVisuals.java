package com.lach.createswatches.client;

import com.lach.createswatches.CreateSwatches;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;

/** With Flywheel on, the base cog is an instanced visual; the renderer still draws the tile ring and screen. */
final class CarouselVisuals {
    private CarouselVisuals() {
    }

    static void register() {
        SimpleBlockEntityVisualizer.builder(CreateSwatches.CAROUSEL_BE.get())
                .factory(SingleAxisRotatingVisual.of(AllPartialModels.SHAFTLESS_COGWHEEL))
                .neverSkipVanillaRender()
                .apply();
    }
}
