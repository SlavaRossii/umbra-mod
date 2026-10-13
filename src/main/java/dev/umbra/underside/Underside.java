package dev.umbra.underside;

import dev.umbra.Umbra;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** The Underside (Изнанка): a dead, roofed, lightless copy of the world. Defined by data in data/umbra/dimension. */
public final class Underside {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Umbra.MODID, "underside"));
    /** Sanity drains this much faster per second while you are there. */
    public static final float SANITY_DRAIN = 0.3F;
    /** Y the arrival platform is carved at when no cave floor is found. */
    public static final int FALLBACK_Y = 64;

    private Underside() {}

    public static boolean is(Level level) {
        return level.dimension() == LEVEL;
    }
}
