package dev.umbra.citadel;

import com.mojang.serialization.MapCodec;
import dev.umbra.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * The gate of a Quantum Citadel: an unbreakable arch on a cave floor, with a shimmering doorway.
 * One per {@link #CELL}×{@link #CELL}-chunk cell, at least {@code CELL - SPREAD + 1} chunks apart,
 * so the citadels hanging above them never overlap.
 */
public record CitadelGateFeature() implements Feature {
    public static final MapCodec<CitadelGateFeature> CODEC = MapCodec.unit(new CitadelGateFeature());
    public static final int CELL = 24;
    public static final int SPREAD = 8;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    @Override
    public MapCodec<CitadelGateFeature> codec() {
        return CODEC;
    }

    public static int[] chosenChunkOf(long seed, int gx, int gz) {
        RandomSource r = RandomSource.create(seed ^ (gx * 73428767L + gz * 912931L) ^ 0x6A09E667F3BCC909L);
        return new int[]{gx * CELL + r.nextInt(SPREAD), gz * CELL + r.nextInt(SPREAD)};
    }

    public static boolean chosenChunk(long seed, int cx, int cz) {
        int[] c = chosenChunkOf(seed, Math.floorDiv(cx, CELL), Math.floorDiv(cz, CELL));
        return c[0] == cx && c[1] == cz;
    }

    /** Nearest gate chunk to (x, z), searching the cells around it. */
    public static int[] nearest(long seed, int x, int z) {
        int gx = Math.floorDiv(x >> 4, CELL), gz = Math.floorDiv(z >> 4, CELL);
        int[] best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                int[] c = chosenChunkOf(seed, gx + dx, gz + dz);
                double d = Math.pow(c[0] * 16 + 8 - x, 2) + Math.pow(c[1] * 16 + 8 - z, 2);
                if (d < bestD) {
                    bestD = d;
                    best = c;
                }
            }
        return best;
    }

    /** "~N blocks to the north-east" towards the nearest gate. */
    public static net.minecraft.network.chat.Component describeNearest(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.player.Player player, String key) {
        int[] c = nearest(level.getSeed(), (int) player.getX(), (int) player.getZ());
        double dx = c[0] * 16 + 8 - player.getX(), dz = c[1] * 16 + 8 - player.getZ();
        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        // 0 = south (+z), going clockwise like Minecraft yaw
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        String[] dirs = {"s", "sw", "w", "nw", "n", "ne", "e", "se"};
        String dir = dirs[(int) Math.floorMod(Math.round(yaw / 45.0), 8)];
        return net.minecraft.network.chat.Component.translatable(key, dist,
                net.minecraft.network.chat.Component.translatable("direction.umbra." + dir));
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int cx = origin.getX() >> 4, cz = origin.getZ() >> 4;
        if (!chosenChunk(level.getSeed(), cx, cz)) return false;
        BlockPos floor = dev.umbra.village.ShadowVillageFeature.findFloor(level, new BlockPos(cx * 16 + 8, 120, cz * 16 + 8), 120, 10);
        if (floor == null) floor = new BlockPos(cx * 16 + 8, dev.umbra.underside.Underside.FALLBACK_Y, cz * 16 + 8);
        build(level, floor, random);
        return true;
    }

    /** The arch faces south (z+); its doorway is 3 wide, 4 high, centred on {@code floor}. */
    public static void build(WorldGenLevel level, BlockPos floor, RandomSource random) {
        BlockState wall = ModBlocks.MAZE_WALL.get().defaultBlockState();
        BlockState gate = ModBlocks.CITADEL_GATE.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        // a small clearing with a floor
        for (int x = -4; x <= 4; x++)
            for (int z = -3; z <= 4; z++) {
                level.setBlock(floor.offset(x, -1, z), wall, FLAGS);
                for (int y = 0; y <= 5; y++) level.setBlock(floor.offset(x, y, z), air, FLAGS);
            }
        for (int x = -2; x <= 2; x++)
            for (int y = 0; y <= 5; y++) {
                boolean frame = Math.abs(x) == 2 || y == 5;
                level.setBlock(floor.offset(x, y, 0), frame ? wall : (y <= 3 ? gate : wall), FLAGS);
            }
        // broken quantum pillars around it
        for (int[] p : new int[][]{{-4, -2}, {4, -2}, {-4, 3}, {4, 3}}) {
            int h = 1 + random.nextInt(4);
            for (int y = 0; y < h; y++) level.setBlock(floor.offset(p[0], y, p[1]), wall, FLAGS);
        }
    }
}
