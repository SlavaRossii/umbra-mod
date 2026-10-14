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

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<dev.umbra.village.ShadowVillageFeature>> SHADOW_VILLAGE =
            FEATURES.register("shadow_village", () -> dev.umbra.village.ShadowVillageFeature.CODEC);

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<dev.umbra.lake.BlackLakeFeature>> BLACK_LAKE =
            FEATURES.register("black_lake", () -> dev.umbra.lake.BlackLakeFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<dev.umbra.lake.SunkenTempleFeature>> SUNKEN_TEMPLE =
            FEATURES.register("sunken_temple", () -> dev.umbra.lake.SunkenTempleFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<dev.umbra.decor.CrystalGrottoFeature>> CRYSTAL_GROTTO =
            FEATURES.register("crystal_grotto", () -> dev.umbra.decor.CrystalGrottoFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<dev.umbra.decor.DeadTreeFeature>> DEAD_TREE =
            FEATURES.register("dead_tree", () -> dev.umbra.decor.DeadTreeFeature.CODEC);

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<dev.umbra.citadel.CitadelGateFeature>> CITADEL_GATE =
            FEATURES.register("citadel_gate", () -> dev.umbra.citadel.CitadelGateFeature.CODEC);

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<dev.umbra.decor.UndersideDecorFeature>> UNDERSIDE_DECOR =
            FEATURES.register("underside_decor", () -> dev.umbra.decor.UndersideDecorFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<dev.umbra.decor.UndersideLandmarkFeature>> UNDERSIDE_LANDMARK =
            FEATURES.register("underside_landmark", () -> dev.umbra.decor.UndersideLandmarkFeature.CODEC);

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<dev.umbra.village.KeepersWorkshopFeature>> KEEPERS_WORKSHOP =
            FEATURES.register("keepers_workshop", () -> dev.umbra.village.KeepersWorkshopFeature.CODEC);

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<dev.umbra.lighthouse.LighthouseFeature>> LIGHTHOUSE =
            FEATURES.register("lighthouse", () -> dev.umbra.lighthouse.LighthouseFeature.CODEC);

    private ModFeatures() {}
}
