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
        build(level, findSolidGround(level, cx * 16 + 8, cz * 16 + 8), random);
        return true;
    }

    /**
     * A cave floor with room above and solid ground under most of a 9x9 square, so the altar never hangs in the air.
     * If there is none in this column, a chamber is carved out at the usual height.
     */
    public static BlockPos findSolidGround(WorldGenLevel level, int x, int z) {
        for (int y = 105; y > 14; y--) {
            BlockPos p = new BlockPos(x, y, z);
            if (!level.getBlockState(p).isAir() || level.getBlockState(p.below()).isAir()) continue;
            boolean room = true;
            for (int dy = 0; dy < 6 && room; dy++) room = level.getBlockState(p.above(dy)).isAir();
            if (!room) continue;
            int solid = 0;
            for (int dx = -4; dx <= 4; dx += 2)
                for (int dz = -4; dz <= 4; dz += 2)
                    for (int dy = 1; dy <= 3; dy++)
                        if (!level.getBlockState(p.offset(dx, -dy, dz)).isAir()) {
                            solid++;
                            break;
                        }
            if (solid >= 22) return p;
        }
        return new BlockPos(x, dev.umbra.underside.Underside.FALLBACK_Y, z);
    }

    /**
     * The altar: a stepped platform of dusk stone with four lantern pillars, the gate standing on top.
     * The doorway faces south (z+), 3 wide and 4 high, centred one block above {@code floor}.
     */
    public static void build(WorldGenLevel level, BlockPos floor, RandomSource random) {
        BlockState wall = ModBlocks.MAZE_WALL.get().defaultBlockState();
        BlockState dusk = ModBlocks.DUSK_STONE.get().defaultBlockState();
        BlockState gate = ModBlocks.CITADEL_GATE.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        // a chamber of air above, solid ground below
        for (int x = -6; x <= 6; x++)
            for (int z = -6; z <= 6; z++) {
                for (int y = 0; y <= 8; y++) level.setBlock(floor.offset(x, y, z), air, FLAGS);
                dev.umbra.village.ShadowVillageFeature.foundation(level, floor.offset(x, -1, z), ModBlocks.ASH_STONE.get().defaultBlockState());
            }
        // two steps of dusk stone
        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++) {
                level.setBlock(floor.offset(x, 0, z), dusk, FLAGS);
                if (Math.abs(x) <= 3 && Math.abs(z) <= 3) level.setBlock(floor.offset(x, 1, z), dusk, FLAGS);
            }
        BlockPos top = floor.above(2);
        for (int x = -2; x <= 2; x++)
            for (int y = 0; y <= 5; y++) {
                boolean frame = Math.abs(x) == 2 || y == 5;
                level.setBlock(top.offset(x, y, 0), frame ? wall : (y <= 3 ? gate : wall), FLAGS);
            }
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState();
        for (int[] p : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            for (int y = 0; y < 3; y++) level.setBlock(top.offset(p[0], y, p[1]), wall, FLAGS);
            level.setBlock(top.offset(p[0], 3, p[1]), lantern, FLAGS);
        }
    }
}
