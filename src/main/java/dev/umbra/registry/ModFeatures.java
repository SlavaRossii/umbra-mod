package dev.umbra.registry;

import com.mojang.serialization.MapCodec;
import dev.umbra.Umbra;
import dev.umbra.quantum.QuantumMazeFeature;
import dev.umbra.underside.HangingTreeFeature;
import dev.umbra.underside.UndersideMazeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURES = DeferredRegister.create(Registries.FEATURE_TYPE, Umbra.MODID);

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<QuantumMazeFeature>> QUANTUM_MAZE =
            FEATURES.register("quantum_maze", () -> QuantumMazeFeature.CODEC);

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<HangingTreeFeature>> HANGING_TREE =
            FEATURES.register("hanging_tree", () -> HangingTreeFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<UndersideMazeFeature>> UNDERSIDE_MAZE =
            FEATURES.register("underside_maze", () -> UndersideMazeFeature.CODEC);

    private ModFeatures() {}
}
