package dev.umbra.quantum;

import dev.umbra.UmbraConfig;
import dev.umbra.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Drives the quantum walls of one maze per {@link MazeCoreBlock}. Runtime state lives in memory only. */
public final class MazeController {
    private static final int ACTIVE_INTERVAL = 10;
    private static final int IDLE_INTERVAL = 40;
    private static final int RESCAN_TICKS = 200;
    private static final int CHANGES_PER_STEP = 12;

    private static final Map<String, Runtime> RUNTIMES = new HashMap<>();

    /** Per-maze state. Patterns may use {@link #phase}, {@link #counter}, {@link #axis}, {@link #data}. */
    public static final class Runtime {
        MazePattern pattern;
        int ticksInPattern;
        int sinceScan = RESCAN_TICKS;
        List<BlockPos> columns = new ArrayList<>();
        String forced;
        public int phase;
        public int counter;
        public int axis;
        public Object data;
    }

    private MazeController() {}

    public static BlockPos cornerOf(BlockPos core) {
        return core.offset(-QuantumMaze.SIZE / 2, 1, -QuantumMaze.SIZE / 2);
    }

    private static String key(ServerLevel level, BlockPos core) {
        return level.dimension() + "@" + core.asLong();
    }

    /** @return delay until the next tick */
    static int tick(ServerLevel level, BlockPos core) {
        BlockPos corner = cornerOf(core);
        AABB bounds = new AABB(corner.getX() - 2, corner.getY() - 1, corner.getZ() - 2,
                corner.getX() + QuantumMaze.SIZE + 2, corner.getY() + QuantumMaze.HEIGHT + 1, corner.getZ() + QuantumMaze.SIZE + 2);
        List<ServerPlayer> inside = new ArrayList<>();
        for (ServerPlayer p : level.players()) if (!p.isSpectator() && bounds.contains(p.position())) inside.add(p);
        if (inside.isEmpty()) return IDLE_INTERVAL;
        // the Architect's seal: the maze stays as it is while its bearer is inside
        for (ServerPlayer p : inside) if (dev.umbra.gear.Gear.hasArtifact(p, dev.umbra.gear.Gear.ARCHITECT_SEAL)) return ACTIVE_INTERVAL;

        Runtime rt = RUNTIMES.computeIfAbsent(key(level, core), k -> new Runtime());
        rt.sinceScan += ACTIVE_INTERVAL;
        if (rt.sinceScan >= RESCAN_TICKS) {
            rt.columns = scan(level, corner);
            rt.sinceScan = 0;
        }
        if (rt.columns.isEmpty()) return IDLE_INTERVAL;

        int patternTicks = UmbraConfig.num(UmbraConfig.MAZE_PATTERN_SECONDS) * 20;
        if (rt.pattern == null || rt.ticksInPattern >= patternTicks || rt.forced != null) {
            MazePattern next = rt.forced != null ? MazePatterns.byId(rt.forced) : MazePatterns.random(level.getRandom(), rt.pattern);
            rt.forced = null;
            if (next == null) next = MazePatterns.FLICKER;
            rt.pattern = next;
            rt.ticksInPattern = 0;
            rt.phase = 0;
            rt.counter = 0;
            rt.data = null;
            next.start(new MazeContext(level, corner, inside, rt.columns, rt, 0));
            for (ServerPlayer p : inside) {
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 0.8F, 0.5F);
            }
        }
        rt.ticksInPattern += ACTIVE_INTERVAL;
        rt.pattern.step(new MazeContext(level, corner, inside, rt.columns, rt, CHANGES_PER_STEP));
        return ACTIVE_INTERVAL;
    }

    private static List<BlockPos> scan(ServerLevel level, BlockPos corner) {
        List<BlockPos> list = new ArrayList<>();
        for (int x = 0; x < QuantumMaze.SIZE; x++) {
            for (int z = 0; z < QuantumMaze.SIZE; z++) {
                for (int f = 0; f < QuantumMaze.FLOORS; f++) {
                    BlockPos p = corner.offset(x, QuantumMaze.floorBase(f) + 1, z);
                    BlockState s = level.getBlockState(p);
                    if (s.is(ModBlocks.QUANTUM_BLOCK.get()) && s.getValue(QuantumBlock.LINKED)) list.add(p.immutable());
                }
            }
        }
        return list;
    }

    /** /umbra mazepattern: forces a pattern on the maze the player stands in. */
    public static boolean force(ServerLevel level, BlockPos playerPos, String patternId) {
        for (int dx = -QuantumMaze.SIZE; dx <= QuantumMaze.SIZE; dx++) {
            for (int dz = -QuantumMaze.SIZE; dz <= QuantumMaze.SIZE; dz++) {
                for (int dy = -QuantumMaze.HEIGHT - 2; dy <= 2; dy++) {
                    BlockPos p = playerPos.offset(dx, dy, dz);
                    if (level.getBlockState(p).is(ModBlocks.MAZE_CORE.get())) {
                        RUNTIMES.computeIfAbsent(key(level, p), k -> new Runtime()).forced = patternId;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Test hook: runs a pattern on the maze of {@code core} with nobody inside, so every wall may change.
     * @return how many columns differ afterwards
     */
    public static int runForTest(ServerLevel level, BlockPos core, MazePattern pattern, int steps) {
        BlockPos corner = cornerOf(core);
        List<BlockPos> cols = scan(level, corner);
        List<Boolean> before = new ArrayList<>();
        for (BlockPos c : cols) before.add(QuantumBlock.isOpen(level, c));
        Runtime rt = new Runtime();
        pattern.start(new MazeContext(level, corner, List.of(), cols, rt, 0));
        for (int i = 0; i < steps; i++) pattern.step(new MazeContext(level, corner, List.of(), cols, rt, CHANGES_PER_STEP));
        int changed = 0;
        for (int i = 0; i < cols.size(); i++) if (QuantumBlock.isOpen(level, cols.get(i)) != before.get(i)) changed++;
        return changed;
    }

    /** Current pattern id, or null if the maze is idle. */
    public static String patternId(ServerLevel level, BlockPos core) {
        Runtime rt = RUNTIMES.get(key(level, core));
        return rt == null || rt.pattern == null ? null : rt.pattern.id();
    }

    public static int secondsLeft(ServerLevel level, BlockPos core) {
        Runtime rt = RUNTIMES.get(key(level, core));
        if (rt == null) return 0;
        return Math.max(0, (UmbraConfig.num(UmbraConfig.MAZE_PATTERN_SECONDS) * 20 - rt.ticksInPattern) / 20);
    }

    /** For /umbra status and tests. */
    public static String describe(ServerLevel level, BlockPos core) {
        Runtime rt = RUNTIMES.get(key(level, core));
        return rt == null || rt.pattern == null ? "idle" : rt.pattern.id() + " (" + rt.columns.size() + " quantum walls)";
    }
}
