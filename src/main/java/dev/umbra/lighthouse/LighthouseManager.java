package dev.umbra.lighthouse;

import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModBlocks;
import dev.umbra.underside.Underside;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 2.5: the lighthouse is raised when someone first comes near its place, not while the world is generated. By then
 * the whole neighbourhood is loaded, so it can look round: of a hundred-odd spots within {@link #SEARCH} blocks of the
 * seed's point it takes the most open one (most air round it, the flattest floor), carves a wide grotto there with a
 * black pool for the dock, and builds. The platform is saved on the level, so the map, the horn and Yan know the
 * exact place. Before 2.5 it stood on the centre of its chunk whatever was there, often in a pit between cliffs.
 */
public final class LighthouseManager {
    private LighthouseManager() {}

    /** Raised when a player comes this close to the seed's point. */
    public static final int TRIGGER = 150;
    /** How far from the seed's point it may move to find room. */
    public static final int SEARCH = 48;
    /** Radius of the grotto carved round it. */
    public static final int GROTTO = 26;
    /** Lowest and highest platform: the tower and the grotto's dome must stay under the roof. */
    public static final int MIN_Y = 14, MAX_Y = 120 - (LighthouseFeature.LAMP_Y + 16);
    private static final int FLAGS = net.minecraft.world.level.block.Block.UPDATE_CLIENTS;

    /** The platform, if the lighthouse has been raised. */
    public static BlockPos built(ServerLevel level) {
        long v = level.getData(ModAttachments.LIGHTHOUSE);
        return v == Long.MIN_VALUE ? null : BlockPos.of(v);
    }

    /** Where it is (or will be): the platform once raised, otherwise the seed's point at the middle of its chunk. */
    public static BlockPos centre(ServerLevel level) {
        BlockPos b = built(level);
        if (b != null) return b;
        int[] c = LighthouseFeature.site(level.getSeed());
        return new BlockPos(c[0] * 16 + 8, 64, c[1] * 16 + 8);
    }

    /** Every second for every player in the Underside. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 13 || !(player.level() instanceof ServerLevel level) || !Underside.is(level)) return;
        if (built(level) != null) return;
        BlockPos c = centre(level);
        double dx = c.getX() - player.getX(), dz = c.getZ() - player.getZ();
        if (dx * dx + dz * dz > TRIGGER * TRIGGER) return;
        int reach = SEARCH + GROTTO + 16;
        for (int ox = -reach; ox <= reach; ox += 16)
            for (int oz = -reach; oz <= reach; oz += 16)
                if (!level.hasChunkAt(c.offset(ox, 0, oz))) return;      // wait until all of it is loaded
        raise(level);
    }

    /** Picks the spot, carves the grotto, builds the lighthouse and remembers it. Returns the platform. */
    public static BlockPos raise(ServerLevel level) {
        BlockPos done = built(level);
        if (done != null) return done;
        BlockPos c = centre(level);
        BlockPos platform = pick(level, c.getX(), c.getZ());
        RandomSource random = RandomSource.create(level.getSeed() ^ 0x5DEECE66DL);
        grotto(level, platform, random);
        LighthouseFeature.build(level, platform, random);
        level.setData(ModAttachments.LIGHTHOUSE, platform.asLong());
        return platform;
    }

    /** The most open floor near (x, z); the platform is one block below it (it replaces the top of the ground). */
    public static BlockPos pick(ServerLevel level, int x, int z) {
        BlockPos best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int ox = -SEARCH; ox <= SEARCH; ox += 8)
            for (int oz = -SEARCH; oz <= SEARCH; oz += 8) {
                if (ox * ox + oz * oz > SEARCH * SEARCH) continue;
                int cx = x + ox, cz = z + oz;
                for (int y = 104; y > MIN_Y; y--) {
                    BlockPos p = new BlockPos(cx, y, cz);
                    BlockState below = level.getBlockState(p.below());
                    if (below.isAir() || !below.getFluidState().isEmpty() || !level.getBlockState(p).isAir()) continue;
                    if (!level.getBlockState(p.above()).isAir() || !level.getBlockState(p.above(2)).isAir()) continue;
                    int score = openness(level, p) - Math.abs(ox) / 4 - Math.abs(oz) / 4;   // a little loyalty to the seed's point
                    if (score > bestScore) {
                        bestScore = score;
                        best = p;
                    }
                    y -= 4;
                }
            }
        if (best == null) best = new BlockPos(x, 48, z);
        int py = Math.max(MIN_Y, Math.min(MAX_Y, best.getY() - 1));
        return new BlockPos(best.getX(), py, best.getZ());
    }

    /** How much room there is round a floor: air in a ring of samples at several heights, ground at the same level. */
    static int openness(ServerLevel level, BlockPos p) {
        int score = 0;
        for (int r : new int[]{8, 16})
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                int sx = (int) Math.round(Math.cos(a) * r), sz = (int) Math.round(Math.sin(a) * r);
                for (int h : new int[]{2, 8, 16, 26}) if (level.getBlockState(p.offset(sx, h, sz)).isAir()) score += 2;
                // a floor nearby at about the same height: flat ground
                for (int dy = -3; dy <= 3; dy++) {
                    BlockPos q = p.offset(sx, dy, sz);
                    if (level.getBlockState(q).isAir() && !level.getBlockState(q.below()).isAir() && level.getBlockState(q.below()).getFluidState().isEmpty()) {
                        score += 3;
                        break;
                    }
                }
            }
        for (int h = 3; h <= 40; h += 3) if (level.getBlockState(p.above(h)).isAir()) score += 2;   // open sky of the cave above
        return score;
    }

    /**
     * A grotto round the platform: a level floor of dusk stone and ash, a dome of air high enough for the tower, the
     * ground made good under it, and a black pool on the west where the dock reaches out. Bedrock is never touched.
     */
    public static void grotto(ServerLevel level, BlockPos platform, RandomSource random) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState stone = Blocks.DEEPSLATE.defaultBlockState();
        BlockState dusk = ModBlocks.DUSK_STONE.get().defaultBlockState();
        BlockState ash = ModBlocks.ASH_SOIL.get().defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        int height = LighthouseFeature.LAMP_Y + 14;
        for (int x = -GROTTO; x <= GROTTO; x++)
            for (int z = -GROTTO; z <= GROTTO; z++) {
                double d = Math.sqrt(x * x + z * z) / GROTTO;
                if (d > 1.0) continue;
                // the dome sinks towards the edge, with a little roughness so it does not look poured
                int top = (int) Math.round(height * Math.sqrt(1.0 - d * d * 0.92)) + random.nextInt(2);
                for (int y = 1; y <= top; y++) set(level, platform.offset(x, y, z), air);
                // the floor, made good: no holes, no steps
                BlockState floor = random.nextInt(5) == 0 ? ash : dusk;
                set(level, platform.offset(x, 0, z), floor);
                for (int y = -1; y >= -4; y--) {
                    BlockPos p = platform.offset(x, y, z);
                    if (level.getBlockState(p).isAir() || !level.getBlockState(p).getFluidState().isEmpty()) set(level, p, stone);
                }
            }
        // the pool on the west, under the dock (x -9..-14): three deep, with a rim of stone round it
        for (int x = -24; x <= -7; x++)
            for (int z = -9; z <= 10; z++) {
                double e = Math.pow((x + 15.5) / 8.5, 2) + Math.pow((z - 0.5) / 9.5, 2);
                if (e > 1.25) continue;
                if (e > 1.0) {
                    for (int y = -4; y <= -1; y++) set(level, platform.offset(x, y, z), stone);
                    continue;
                }
                set(level, platform.offset(x, 0, z), air);
                for (int y = -3; y <= -1; y++) set(level, platform.offset(x, y, z), water);
                set(level, platform.offset(x, -4, z), stone);
            }
        // a few luminite lamps in the walls of the grotto, so it is not a black bowl
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + 0.3;
            BlockPos p = platform.offset((int) Math.round(Math.cos(a) * (GROTTO + 1)), 3 + random.nextInt(4), (int) Math.round(Math.sin(a) * (GROTTO + 1)));   // set into the wall
            set(level, p, ModBlocks.LUMINITE_LAMP.get().defaultBlockState());
        }
    }

    private static void set(ServerLevel level, BlockPos p, BlockState s) {
        BlockState old = level.getBlockState(p);
        if (old.is(Blocks.BEDROCK)) return;
        // a skull or chest the cave generated here is still "pending" in its chunk: wake it first, so that replacing
        // it removes it for good (otherwise the chunk later complains about a block entity standing in thin air)
        if (old.hasBlockEntity()) level.getBlockEntity(p);
        level.setBlock(p, s, FLAGS);
    }
}
