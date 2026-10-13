package dev.umbra.underside;

import com.mojang.serialization.MapCodec;
import dev.umbra.quantum.QuantumMaze;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Quantum ruins: mazes embedded in the Underside's caverns, standing on the nearest floor below the origin. */
public record UndersideMazeFeature() implements Feature {
    public static final MapCodec<UndersideMazeFeature> CODEC = MapCodec.unit(new UndersideMazeFeature());

    @Override
    public MapCodec<UndersideMazeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos p = origin;
        int steps = 0;
        while (steps < 64 && p.getY() > 12 && !(level.getBlockState(p).isAir() && !level.getBlockState(p.below()).isAir())) {
            p = p.below();
            steps++;
        }
        if (steps >= 64 || p.getY() <= 12 || p.getY() + QuantumMaze.HEIGHT > 118) return false;
        if (!level.getBlockState(p.below()).getFluidState().isEmpty()) return false;
        QuantumMaze.build(level, p.offset(-QuantumMaze.SIZE / 2, -1, -QuantumMaze.SIZE / 2), random);
        return true;
    }
}
