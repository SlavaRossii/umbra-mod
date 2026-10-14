package dev.umbra.lake;

import com.mojang.serialization.MapCodec;
import dev.umbra.decor.UndersideDecorFeature;
import dev.umbra.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import java.util.ArrayList;
import java.util.List;

/**
 * A black lake on a cave floor: an uneven bowl of still water, sealed so that it never leaks into the caves below,
 * with a shore of ash and bones. The biome tints the water black and drowns sight in it.
 */
public record BlackLakeFeature() implements Feature {
    public static final MapCodec<BlackLakeFeature> CODEC = MapCodec.unit(new BlackLakeFeature());
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    @Override
    public MapCodec<BlackLakeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos f = openFloor(level, origin);
        if (f == null) return false;
        lake(level, f, 6 + random.nextInt(7), 6 + random.nextInt(7), 4 + random.nextInt(4), random);
        return true;
    }

    /** A floor under {@code origin} with at least 5 blocks of air above it. */
    public static BlockPos openFloor(WorldGenLevel level, BlockPos origin) {
        BlockPos f = UndersideDecorFeature.floor(level, origin, 90);
        if (f == null || f.getY() < 8) return null;
        for (int y = 0; y < 5; y++) if (!level.getBlockState(f.above(y)).isAir()) return null;
        return f;
    }

    /**
     * Carves the lake. The water's top layer replaces the ground under {@code floor}.
     * Returns the y of the water surface.
     */
    public static int lake(WorldGenLevel level, BlockPos floor, int rx, int rz, int depth, RandomSource random) {
        int surface = floor.getY() - 1;
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState stone = ModBlocks.ASH_STONE.get().defaultBlockState();
        BlockState soil = ModBlocks.ASH_SOIL.get().defaultBlockState();
        List<BlockPos> placed = new ArrayList<>();
        double phase = random.nextDouble() * 6.28;
        for (int dx = -rx - 2; dx <= rx + 2; dx++) {
            for (int dz = -rz - 2; dz <= rz + 2; dz++) {
                double d = (double) dx * dx / (rx * rx) + (double) dz * dz / (rz * rz);
                d += 0.12 * Math.sin(dx * 0.7 + phase) * Math.cos(dz * 0.6 - phase); // ragged shore
                BlockPos col = new BlockPos(floor.getX() + dx, surface, floor.getZ() + dz);
                if (d <= 1.0) {
                    int dep = Math.max(1, (int) Math.round(depth * (1.0 - d) + 0.6));
                    for (int y = 0; y < dep; y++) {
                        BlockPos p = col.below(y);
                        level.setBlock(p, water, FLAGS);
                        placed.add(p);
                    }
                    for (int y = 1; y <= 3; y++) {
                        BlockPos p = col.above(y);
                        if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS);
                    }
                } else if (d <= 1.45) {
                    for (int y = 0; y <= depth; y++) {
                        BlockPos p = col.below(y);
                        BlockState s = level.getBlockState(p);
                        if (s.isAir() || !s.getFluidState().isEmpty()) level.setBlock(p, stone, FLAGS);
                    }
                    if (level.getBlockState(col.above()).isAir()) {
                        level.setBlock(col, soil, FLAGS);
                        if (random.nextInt(14) == 0) level.setBlock(col.above(), Blocks.BONE_BLOCK.defaultBlockState(), FLAGS);
                        else if (random.nextInt(30) == 0) level.setBlock(col.above(), Blocks.SKELETON_SKULL.defaultBlockState(), FLAGS);
                    }
                }
            }
        }
        seal(level, placed, stone);
        return surface;
    }

    /** No water may touch air from the side or below: plug every hole. */
    public static void seal(WorldGenLevel level, List<BlockPos> water, BlockState plug) {
        for (BlockPos p : water) {
            for (Direction d : Direction.values()) {
                if (d == Direction.UP) continue;
                BlockPos q = p.relative(d);
                BlockState s = level.getBlockState(q);
                if (s.isAir() || (!s.getFluidState().isEmpty() && !s.is(Blocks.WATER))) level.setBlock(q, plug, FLAGS);
            }
        }
    }
}
