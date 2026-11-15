package com.lach.createswatches;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config: {@code config/createswatches-server.toml}. */
public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue APPLICATOR_RANGE;
    public static final ModConfigSpec.BooleanValue NEEDS_ROTATION;
    public static final ModConfigSpec.BooleanValue CARD_COSTS_PAPER;
    public static final ModConfigSpec.DoubleValue STRESS_IMPACT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("carousel");
        NEEDS_ROTATION = b.comment("The carousel only reads storage and exports while it turns.")
                .define("needsRotation", true);
        STRESS_IMPACT = b.comment("Stress the carousel puts on its network, per RPM.")
                .defineInRange("stressImpact", 1.0, 0.0, 1024.0);
        CARD_COSTS_PAPER = b.comment("Typing a Swatch Card uses one paper from the player's inventory.")
                .define("cardCostsPaper", true);
        b.pop();
        b.push("applicator");
        APPLICATOR_RANGE = b.comment("How far, in blocks, a Swatch Applicator reaches its linked carousel. 0 = anywhere in the same dimension.")
                .defineInRange("range", 64, 0, 100000);
        b.pop();
        SPEC = b.build();
    }

    private Config() {
    }
}
