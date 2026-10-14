package dev.umbra.underside;

import dev.umbra.Umbra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/** Biomes of the Underside (data in data/umbra/worldgen/biome). */
public final class UndersideBiomes {
    public static final ResourceKey<Biome> ASH_PLAINS = key("ash_plains");
    public static final ResourceKey<Biome> INVERTED_FOREST = key("inverted_forest");
    public static final ResourceKey<Biome> QUANTUM_RUINS = key("quantum_ruins");
    public static final ResourceKey<Biome> ABYSS = key("abyss");
    public static final ResourceKey<Biome> CRYSTAL_GROTTOS = key("crystal_grottos");
    public static final ResourceKey<Biome> BLACK_LAKES = key("black_lakes");
    public static final ResourceKey<Biome> WHISPERING_FOREST = key("whispering_forest");

    private UndersideBiomes() {}

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Umbra.MODID, name));
    }

    public static boolean in(Level level, BlockPos pos, ResourceKey<Biome> biome) {
        return Underside.is(level) && level.getBiome(pos).is(biome);
    }
}
