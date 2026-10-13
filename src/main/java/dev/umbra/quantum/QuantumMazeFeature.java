package dev.umbra.quantum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** World generation hook: places a quantum maze on the surface (rare, see placed_feature json). */
public class QuantumMazeFeature extends Feature<NoneFeatureConfiguration> {
    public QuantumMazeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        if (!level.getBlockState(origin.below()).getFluidState().isEmpty()) return false;
        if (!level.getBlockState(origin).getFluidState().isEmpty()) return false;
        BlockPos corner = origin.offset(-QuantumMaze.SIZE / 2, -1, -QuantumMaze.SIZE / 2);
        QuantumMaze.build(level, corner, context.random());
        return true;
    }
}
