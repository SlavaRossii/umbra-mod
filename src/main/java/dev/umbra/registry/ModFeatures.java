package dev.umbra.registry;

import com.mojang.serialization.MapCodec;
import dev.umbra.Umbra;
import dev.umbra.quantum.QuantumMazeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURES = DeferredRegister.create(Registries.FEATURE_TYPE, Umbra.MODID);

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<QuantumMazeFeature>> QUANTUM_MAZE =
            FEATURES.register("quantum_maze", () -> QuantumMazeFeature.CODEC);

    private ModFeatures() {}
}
