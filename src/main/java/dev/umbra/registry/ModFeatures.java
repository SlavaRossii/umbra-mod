package dev.umbra.registry;

import dev.umbra.Umbra;
import dev.umbra.quantum.QuantumMazeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Umbra.MODID);

    public static final DeferredHolder<Feature<?>, QuantumMazeFeature> QUANTUM_MAZE = FEATURES.register("quantum_maze", QuantumMazeFeature::new);

    private ModFeatures() {}
}
