package dev.umbra.quantum;

import dev.umbra.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds a small walled maze. Part of its walls are quantum blocks, so the layout
 * keeps changing behind the player's back. A chest waits in the middle.
 */
public final class QuantumMaze {
    public static final int CELLS = 7;
    public static final int SIZE = CELLS * 2 + 1;   // 15
    public static final int HEIGHT = 5;             // floor, 3 air, ceiling
    private static final float QUANTUM_SHARE = 0.35F;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    private QuantumMaze() {}

    /** @param corner minimum corner of the maze, at floor level */
    public static void build(WorldGenLevel level, BlockPos corner, RandomSource random) {
        BlockState wall = ModBlocks.MAZE_WALL.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        boolean[][] solid = carve(random);

        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                // foundation so it does not float on uneven ground
                for (int y = -1; y >= -6; y--) {
                    BlockPos p = corner.offset(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (!s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced()) break;
                    level.setBlock(p, Blocks.COBBLED_DEEPSLATE.defaultBlockState(), FLAGS);
                }
                level.setBlock(corner.offset(x, 0, z), wall, FLAGS);
                level.setBlock(corner.offset(x, HEIGHT - 1, z), wall, FLAGS);
                for (int y = HEIGHT; y < HEIGHT + 4; y++) level.setBlock(corner.offset(x, y, z), air, FLAGS);

                boolean isWall = solid[x][z];
                boolean border = x == 0 || z == 0 || x == SIZE - 1 || z == SIZE - 1;
                boolean pillar = x % 2 == 0 && z % 2 == 0;
                boolean segment = !border && !pillar && (x % 2 == 0 || z % 2 == 0);
                boolean quantum = segment && random.nextFloat() < QUANTUM_SHARE;
                for (int y = 1; y < HEIGHT - 1; y++) {
                    BlockPos p = corner.offset(x, y, z);
                    if (quantum) {
                        level.setBlock(p, ModBlocks.QUANTUM_BLOCK.get().defaultBlockState().setValue(QuantumBlock.OPEN, !isWall), FLAGS);
                        scheduleQuantum(level, p);
                    } else {
                        level.setBlock(p, isWall ? wall : air, FLAGS);
                    }
                }
            }
        }

        // entrance on the west side, and a little doorway outside it
        int mid = SIZE / 2;
        for (int y = 1; y < HEIGHT - 1; y++) {
            level.setBlock(corner.offset(0, y, mid), air, FLAGS);
            level.setBlock(corner.offset(-1, y, mid), air, FLAGS);
        }

        // dim soul lanterns (the darkness cannot eat those)
        List<int[]> cells = new ArrayList<>();
        for (int cx = 0; cx < CELLS; cx++) for (int cz = 0; cz < CELLS; cz++) cells.add(new int[]{cx * 2 + 1, cz * 2 + 1});
        Collections.shuffle(cells, new java.util.Random(random.nextLong()));
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        for (int i = 0; i < 5; i++) {
            int[] c = cells.get(i);
            level.setBlock(corner.offset(c[0], HEIGHT - 2, c[1]), lantern, FLAGS);
        }

        // the prize
        BlockPos chest = corner.offset(SIZE / 2, 1, SIZE / 2);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), FLAGS);
        RandomizableContainer.setBlockEntityLootTable(level, random, chest, BuiltInLootTables.ANCIENT_CITY);
    }

    private static void scheduleQuantum(WorldGenLevel level, BlockPos pos) {
        level.scheduleTick(pos, ModBlocks.QUANTUM_BLOCK.get(), 10 + (pos.hashCode() & 15));
    }

    /** Classic recursive-backtracker maze. true = wall. */
    static boolean[][] carve(RandomSource random) {
        boolean[][] solid = new boolean[SIZE][SIZE];
        for (boolean[] row : solid) java.util.Arrays.fill(row, true);
        boolean[][] seen = new boolean[CELLS][CELLS];
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, 0});
        seen[0][0] = true;
        solid[1][1] = false;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!stack.isEmpty()) {
            int[] cur = stack.peek();
            List<int[]> options = new ArrayList<>();
            for (int[] d : dirs) {
                int nx = cur[0] + d[0], nz = cur[1] + d[1];
                if (nx >= 0 && nz >= 0 && nx < CELLS && nz < CELLS && !seen[nx][nz]) options.add(new int[]{nx, nz});
            }
            if (options.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] next = options.get(random.nextInt(options.size()));
            seen[next[0]][next[1]] = true;
            solid[next[0] * 2 + 1][next[1] * 2 + 1] = false;
            solid[cur[0] + next[0] + 1][cur[1] + next[1] + 1] = false; // the wall between
            stack.push(next);
        }
        return solid;
    }
}
