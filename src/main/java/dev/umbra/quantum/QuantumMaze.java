package dev.umbra.quantum;

import dev.umbra.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A two-storey walled maze. Every shell, floor and wall block is unbreakable.
 * About a third of the inner walls are quantum: a {@link MazeCoreBlock} under the centre rearranges them
 * while nobody is looking, following one of the {@link MazePatterns}.
 *
 * Layout (local coordinates, corner = minimum corner at the ground floor):
 *   y=0 floor, y=1..3 floor 1, y=4 ceiling/floor, y=5..7 floor 2, y=8 roof, core at (SIZE/2, -1, SIZE/2).
 */
public final class QuantumMaze {
    public static final int CELLS = 11;
    public static final int SIZE = CELLS * 2 + 1;   // 23
    public static final int FLOORS = 2;
    public static final int STOREY = 4;             // slab + 3 air
    public static final int HEIGHT = FLOORS * STOREY + 1;
    private static final float QUANTUM_SHARE = 0.35F;
    private static final int LADDERS = 3;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    private QuantumMaze() {}

    /** Floor base (slab) y offset of storey {@code f}. */
    public static int floorBase(int f) {
        return f * STOREY;
    }

    /** @param corner minimum corner of the maze, at ground-floor level */
    public static void build(WorldGenLevel level, BlockPos corner, RandomSource random) {
        BlockState wall = ModBlocks.MAZE_WALL.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState quantum = ModBlocks.QUANTUM_BLOCK.get().defaultBlockState().setValue(QuantumBlock.LINKED, true);

        boolean[][][] solid = new boolean[FLOORS][][];
        for (int f = 0; f < FLOORS; f++) solid[f] = carve(random);

        // ladders from floor 1 to floor 2; the wall they lean on must stay a real wall
        Set<Long> fixed = new HashSet<>();
        List<int[]> ladders = pickLadders(solid[0], random, fixed);

        int mid = SIZE / 2;
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                // unbreakable foundation down to the ground
                for (int y = -1; y >= -8; y--) {
                    BlockPos p = corner.offset(x, y, z);
                    BlockState s = level.getBlockState(p);
                    boolean ground = !s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced();
                    if (ground && y < -1) break;
                    level.setBlock(p, wall, FLAGS);
                }
                for (int f = 0; f <= FLOORS; f++) level.setBlock(corner.offset(x, floorBase(f), z), wall, FLAGS);
                for (int y = HEIGHT; y < HEIGHT + 4; y++) level.setBlock(corner.offset(x, y, z), air, FLAGS);

                boolean border = x == 0 || z == 0 || x == SIZE - 1 || z == SIZE - 1;
                boolean pillar = x % 2 == 0 && z % 2 == 0;
                boolean segment = !border && !pillar && (x % 2 == 0 || z % 2 == 0);
                for (int f = 0; f < FLOORS; f++) {
                    boolean isWall = solid[f][x][z];
                    boolean q = segment && !(f == 0 && fixed.contains(key(x, z))) && random.nextFloat() < QUANTUM_SHARE;
                    for (int y = floorBase(f) + 1; y < floorBase(f) + STOREY; y++) {
                        BlockPos p = corner.offset(x, y, z);
                        if (q) {
                            level.setBlock(p, quantum.setValue(QuantumBlock.OPEN, !isWall), FLAGS);
                        } else {
                            level.setBlock(p, isWall ? wall : air, FLAGS);
                        }
                    }
                }
            }
        }

        for (int[] l : ladders) {
            Direction toWall = Direction.from2DDataValue(l[2]);
            BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, toWall.getOpposite());
            for (int y = 1; y <= STOREY; y++) level.setBlock(corner.offset(l[0], y, l[1]), ladder, FLAGS);
        }

        // entrance on the west side of the ground floor
        for (int y = 1; y < STOREY; y++) {
            level.setBlock(corner.offset(0, y, mid), air, FLAGS);
            level.setBlock(corner.offset(-1, y, mid), air, FLAGS);
        }

        // dim soul lanterns (the living darkness cannot eat those)
        List<int[]> cells = new ArrayList<>();
        for (int cx = 0; cx < CELLS; cx++) for (int cz = 0; cz < CELLS; cz++) cells.add(new int[]{cx * 2 + 1, cz * 2 + 1});
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        for (int f = 0; f < FLOORS; f++) {
            Collections.shuffle(cells, new java.util.Random(random.nextLong()));
            int placed = 0;
            for (int[] c : cells) {
                if (placed >= 7) break;
                if (isLadderCell(ladders, c[0], c[1])) continue;
                level.setBlock(corner.offset(c[0], floorBase(f) + STOREY - 1, c[1]), lantern, FLAGS);
                placed++;
            }
        }

        // prizes: three common chests downstairs (centre and two far corners), upstairs one more and the real prize.
        // Common chests can be mimics (see MimicChests).
        int far = SIZE - 2;
        placeChest(level, random, corner.offset(mid, 1, mid), COMMON);
        placeChestIfFree(level, random, ladders, corner, 1, 1, 1, COMMON);
        placeChestIfFree(level, random, ladders, corner, far, 1, far, COMMON);
        placeChestIfFree(level, random, ladders, corner, far, floorBase(1) + 1, 1, COMMON);
        placeChest(level, random, corner.offset(mid, floorBase(1) + 1, mid), PRIZE);

        // the core that drives the quantum walls
        BlockPos core = corner.offset(mid, -1, mid);
        level.setBlock(core, ModBlocks.MAZE_CORE.get().defaultBlockState(), FLAGS);
        level.scheduleTick(core, ModBlocks.MAZE_CORE.get(), 20);
    }

    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> COMMON = lootKey("chests/maze_common");
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> PRIZE = lootKey("chests/maze_prize");

    private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootKey(String path) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, path));
    }

    private static void placeChestIfFree(WorldGenLevel level, RandomSource random, List<int[]> ladders, BlockPos corner, int x, int y, int z,
                                         net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot) {
        if (isLadderCell(ladders, x, z)) return;
        placeChest(level, random, corner.offset(x, y, z), loot);
    }

    private static void placeChest(WorldGenLevel level, RandomSource random, BlockPos pos, net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), FLAGS);
        RandomizableContainer.setBlockEntityLootTable(level, random, pos, loot);
    }

    private static boolean isLadderCell(List<int[]> ladders, int x, int z) {
        for (int[] l : ladders) if (l[0] == x && l[1] == z) return true;
        return false;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    /** Picks ladder cells away from the centre; each entry is {x, z, 2D direction towards its wall}. */
    private static List<int[]> pickLadders(boolean[][] solid, RandomSource random, Set<Long> fixed) {
        List<int[]> result = new ArrayList<>();
        int mid = SIZE / 2;
        for (int attempt = 0; attempt < 200 && result.size() < LADDERS; attempt++) {
            int x = random.nextInt(CELLS) * 2 + 1, z = random.nextInt(CELLS) * 2 + 1;
            if (Math.abs(x - mid) + Math.abs(z - mid) < 6 || isLadderCell(result, x, z)) continue;
            if (x == 1 && z == mid) continue; // keep the entrance clear
            for (int d = 0; d < 4; d++) {
                Direction dir = Direction.from2DDataValue(d);
                int wx = x + dir.getStepX(), wz = z + dir.getStepZ();
                if (solid[wx][wz]) {
                    fixed.add(key(wx, wz));
                    result.add(new int[]{x, z, d});
                    break;
                }
            }
        }
        return result;
    }

    /** Classic recursive-backtracker maze. true = wall. */
    public static boolean[][] carve(RandomSource random) {
        boolean[][] solid = new boolean[SIZE][SIZE];
        for (boolean[] row : solid) Arrays.fill(row, true);
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
            solid[cur[0] + next[0] + 1][cur[1] + next[1] + 1] = false;
            stack.push(next);
        }
        // a few extra openings so it is not a single forced path
        for (int i = 0; i < CELLS; i++) {
            int x = random.nextInt(SIZE - 2) + 1, z = random.nextInt(SIZE - 2) + 1;
            if ((x % 2 == 0) != (z % 2 == 0)) solid[x][z] = false;
        }
        return solid;
    }
}
