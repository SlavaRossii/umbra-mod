package dev.umbra.underside;

import com.mojang.serialization.MapCodec;
import dev.umbra.quantum.QuantumMaze;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Quantum ruins: mazes embedded in the Underside's caverns, standing on the nearest floor below the origin.
 * Spacing works like vanilla structures: the world is cut into {@link #CELL}×{@link #CELL}-chunk cells and only one
 * chunk per cell (chosen from the seed) may hold a maze, so two mazes are always at least {@code CELL - SPREAD} chunks apart.
 */
public record UndersideMazeFeature() implements Feature {
    public static final MapCodec<UndersideMazeFeature> CODEC = MapCodec.unit(new UndersideMazeFeature());

    public static final int CELL = 14;
    public static final int SPREAD = 9;

    public static boolean chosenChunk(long seed, int cx, int cz) {
        int gx = Math.floorDiv(cx, CELL), gz = Math.floorDiv(cz, CELL);
        RandomSource r = RandomSource.create(seed ^ (gx * 341873128712L + gz * 132897987541L) ^ 0x5DEECE66DL);
        return cx == gx * CELL + r.nextInt(SPREAD) && cz == gz * CELL + r.nextInt(SPREAD);
    }

    @Override
    public MapCodec<UndersideMazeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!chosenChunk(level.getSeed(), origin.getX() >> 4, origin.getZ() >> 4)) return false;
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
