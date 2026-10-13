package dev.umbra.quantum;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** World generation hook: places a quantum maze on the surface (rare, see placed_feature json). */
public record QuantumMazeFeature() implements Feature {
    public static final MapCodec<QuantumMazeFeature> CODEC = MapCodec.unit(new QuantumMazeFeature());

    @Override
    public MapCodec<QuantumMazeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!level.getBlockState(origin.below()).getFluidState().isEmpty()) return false;
        if (!level.getBlockState(origin).getFluidState().isEmpty()) return false;
        BlockPos corner = origin.offset(-QuantumMaze.SIZE / 2, -1, -QuantumMaze.SIZE / 2);
        QuantumMaze.build(level, corner, random);
        return true;
    }
}
