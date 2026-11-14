package com.lach.createswatches.client;

import com.lach.createswatches.CreateSwatches;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;

/** With Flywheel on, the shaft is an instanced visual; the renderer still draws the carriage, keys and paper. */
final class TypewriterVisuals {
    private TypewriterVisuals() {
    }

    static void register() {
        SimpleBlockEntityVisualizer.builder(CreateSwatches.TYPEWRITER_BE.get())
                .factory(OrientedRotatingVisual.backHorizontal(AllPartialModels.SHAFT_HALF))
                .neverSkipVanillaRender()
                .apply();
    }
}
