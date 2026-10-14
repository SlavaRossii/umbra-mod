package dev.umbra.quantum;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * World generation hook: places a quantum maze on the surface (rare, see placed_feature json).
 *
 * 1.8.2: only on flat dry ground (no grey boxes on hills). The maze is sunk two blocks into the ground,
 * ringed by a low earth mound, and entered down a short staircase on the west side.
 */
public record QuantumMazeFeature() implements Feature {
    public static final MapCodec<QuantumMazeFeature> CODEC = MapCodec.unit(new QuantumMazeFeature());

    /** How far the floor sits under the surface. */
    static final int SINK = 2;
    /** Width of the mound around the walls. */
    static final int MOUND = 3;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    @Override
    public MapCodec<QuantumMazeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!isFlat(level, origin)) return false;
        int ground = origin.getY();                               // first free block above the surface
        BlockPos corner = new BlockPos(origin.getX() - QuantumMaze.SIZE / 2, ground - 1 - SINK, origin.getZ() - QuantumMaze.SIZE / 2);
        QuantumMaze.build(level, corner, random);
        moundAndStairs(level, corner, ground);
        return true;
    }

    /** Dry land, and the surface of the whole footprint (plus the mound and stairs) within one block of height. */
    static boolean isFlat(WorldGenLevel level, BlockPos origin) {
        return isFlat(level, origin, QuantumMaze.SIZE / 2 + MOUND + 3);
    }

    /** Same check for any square footprint of the given half-width. */
    public static boolean isFlat(WorldGenLevel level, BlockPos origin, int half) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -half; dx <= half; dx += 3) {
            for (int dz = -half; dz <= half; dz += 3) {
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                BlockState under = level.getBlockState(new BlockPos(x, h - 1, z));
                if (!under.getFluidState().isEmpty() || under.canBeReplaced()) return false;
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        return max - min <= 1 && origin.getY() >= min && origin.getY() <= max;
    }

    private static void moundAndStairs(WorldGenLevel level, BlockPos corner, int ground) {
        int size = QuantumMaze.SIZE;
        int mid = size / 2;
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        // earth mound: 3, 2, 1 blocks high at distance 1, 2, 3 from the wall; the stair trench stays open
        for (int dx = -MOUND; dx < size + MOUND; dx++) {
            for (int dz = -MOUND; dz < size + MOUND; dz++) {
                int dist = Math.max(Math.max(-dx, dx - (size - 1)), Math.max(-dz, dz - (size - 1)));
                if (dist < 1) continue;
                if (dx < 0 && Math.abs(dz - mid) <= 1) continue;  // stair corridor
                int height = MOUND + 1 - dist;
                int x = corner.getX() + dx, z = corner.getZ() + dz;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);   // first free block
                for (int i = 0; i < height; i++) {
                    level.setBlock(new BlockPos(x, top + i, z), i == height - 1 ? grass : dirt, FLAGS);
                }
            }
        }
        // staircase down to the entrance (the entrance itself is carved by QuantumMaze.build)
        int floorY = corner.getY();                 // slab level; feet stand on floorY + 1
        BlockState stair = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        BlockState brick = Blocks.STONE_BRICKS.defaultBlockState();
        for (int step = 1; step <= SINK; step++) {
            int x = corner.getX() - step;
            for (int dz = -1; dz <= 1; dz++) {
                int z = corner.getZ() + mid + dz;
                boolean centre = dz == 0;
                int stepY = floorY + step;          // step 1 at floorY+1 (feet floorY+2), step 2 at floorY+2 ...
                // air column over the step and a stone floor under it
                for (int y = stepY + 1; y <= ground + 2; y++) level.setBlock(new BlockPos(x, y, z), air, FLAGS);
                for (int y = floorY; y < stepY; y++) level.setBlock(new BlockPos(x, y, z), brick, FLAGS);
                level.setBlock(new BlockPos(x, stepY, z), centre ? stair : brick, FLAGS);
            }
        }
    }
}
