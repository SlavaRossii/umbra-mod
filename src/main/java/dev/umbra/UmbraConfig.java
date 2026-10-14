package dev.umbra;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * config/umbra.toml — feature switches and difficulty knobs.
 * New features should add their switch here so players can turn them off without removing the mod.
 */
public final class UmbraConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    static {
        B.comment("Feature switches").push("features");
    }

    public static final ModConfigSpec.BooleanValue SHADOW = B.comment("Your shadow detaches at night and follows your path").define("shadow", true);
    public static final ModConfigSpec.BooleanValue STEPS = B.comment("Footsteps behind you").define("footsteps", true);
    public static final ModConfigSpec.BooleanValue MIMICS = B.comment("Blocks of your base get replaced by mimics while you are away").define("mimics", true);
    public static final ModConfigSpec.BooleanValue DARKNESS = B.comment("Living darkness grows in deep caves").define("livingDarkness", true);
    public static final ModConfigSpec.BooleanValue SANITY = B.comment("Sanity meter, hallucinations and pills").define("sanity", true);
    public static final ModConfigSpec.BooleanValue NIGHTMARES = B.comment("Sleeping with low sanity can drop you into a nightmare").define("nightmares", true);

    static {
        B.pop();
        B.comment("Difficulty (1.0 = intended: scary, but beatable)").push("difficulty");
    }

    public static final ModConfigSpec.DoubleValue SANITY_DRAIN = B.comment("How fast sanity drains").defineInRange("sanityDrain", 1.0, 0.0, 5.0);
    public static final ModConfigSpec.DoubleValue SANITY_REGEN = B.comment("How fast sanity recovers").defineInRange("sanityRegen", 1.0, 0.0, 5.0);
    public static final ModConfigSpec.DoubleValue SHADOW_FREQUENCY = B.comment("How often the shadow detaches").defineInRange("shadowFrequency", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.IntValue MAZE_PATTERN_SECONDS = B.comment("How long a quantum maze keeps one change pattern").defineInRange("mazePatternSeconds", 30, 5, 600);

    static {
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();

    private UmbraConfig() {}

    /** Safe read: config values throw if read before the config is loaded (e.g. in early tests). */
    public static boolean on(ModConfigSpec.BooleanValue v) {
        try {
            return v.getAsBoolean();
        } catch (IllegalStateException e) {
            return v.getDefault();
        }
    }

    public static double num(ModConfigSpec.DoubleValue v) {
        try {
            return v.getAsDouble();
        } catch (IllegalStateException e) {
            return v.getDefault();
        }
    }

    public static int num(ModConfigSpec.IntValue v) {
        try {
            return v.getAsInt();
        } catch (IllegalStateException e) {
            return v.getDefault();
        }
    }
}
