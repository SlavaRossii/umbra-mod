package dev.umbra.quantum;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The ways a quantum maze rearranges itself. Every pattern only changes walls nobody is looking at. */
public final class MazePatterns {
    private MazePatterns() {}

    /** Flicker: a few random walls appear or vanish. */
    public static final MazePattern FLICKER = new MazePattern() {
        public String id() { return "flicker"; }
        public void step(MazeContext c) {
            for (int i = 0; i < 3; i++) c.flip(c.columns.get(c.random.nextInt(c.columns.size())));
        }
    };

    /** Wave: a line sweeps across the maze, flipping every wall it passes. */
    public static final MazePattern WAVE = new MazePattern() {
        public String id() { return "wave"; }
        public void start(MazeContext c) {
            c.state.axis = c.random.nextInt(4); // 0:+x 1:-x 2:+z 3:-z
            c.state.counter = 0;
        }
        public void step(MazeContext c) {
            int line = (c.state.axis % 2 == 0) ? c.state.counter : QuantumMaze.SIZE - 1 - c.state.counter;
            for (BlockPos col : c.columns) {
                int coord = c.state.axis < 2 ? c.localX(col) : c.localZ(col);
                if (coord == line) c.flip(col);
            }
            c.state.counter = (c.state.counter + 1) % QuantumMaze.SIZE;
        }
    };

    /** Breath: the maze slowly inhales (all quantum walls close) and exhales (all open). */
    public static final MazePattern BREATH = new MazePattern() {
        public String id() { return "breath"; }
        public void step(MazeContext c) {
            c.state.counter++;
            if (c.state.counter >= 8) { // every 4 seconds
                c.state.counter = 0;
                c.state.phase ^= 1;
            }
            boolean open = c.state.phase == 1;
            List<BlockPos> order = new ArrayList<>(c.columns);
            Collections.shuffle(order, new java.util.Random(c.random.nextLong()));
            for (BlockPos col : order) {
                if (!c.budgetLeft()) break;
                if (c.isOpen(col) != open) c.set(col, open);
            }
        }
    };

    /** Hunt: walls close behind you and open to the sides — the maze herds you. */
    public static final MazePattern HUNT = new MazePattern() {
        public String id() { return "hunt"; }
        public void step(MazeContext c) {
            for (ServerPlayer p : c.players) {
                Vec3 eye = p.getEyePosition();
                Vec3 look = p.getLookAngle();
                for (BlockPos col : c.columns) {
                    Vec3 to = Vec3.atCenterOf(col).add(0, 1, 0).subtract(eye);
                    double dist = to.length();
                    if (dist > 7) continue;
                    double dot = look.dot(to.scale(1.0 / Math.max(dist, 1.0E-4)));
                    if (dot < -0.2 && dist < 5) c.set(col, false);
                    else if (Math.abs(dot) < 0.5 && c.random.nextFloat() < 0.3F) c.set(col, true);
                }
            }
        }
    };

    /** Rebuild: the maze quietly grows into a completely different layout. */
    public static final MazePattern REBUILD = new MazePattern() {
        public String id() { return "rebuild"; }
        public void start(MazeContext c) {
            boolean[][][] target = new boolean[QuantumMaze.FLOORS][][];
            for (int f = 0; f < QuantumMaze.FLOORS; f++) target[f] = QuantumMaze.carve(c.random);
            c.state.data = target;
        }
        public void step(MazeContext c) {
            boolean[][][] target = (boolean[][][]) c.state.data;
            List<BlockPos> order = new ArrayList<>(c.columns);
            Collections.shuffle(order, new java.util.Random(c.random.nextLong()));
            for (BlockPos col : order) {
                if (!c.budgetLeft()) break;
                boolean wantOpen = !target[c.floor(col)][c.localX(col)][c.localZ(col)];
                if (c.isOpen(col) != wantOpen) c.set(col, wantOpen);
            }
        }
    };

    /** Mirror: one half of the maze turns into the reflection of the other. */
    public static final MazePattern MIRROR = new MazePattern() {
        public String id() { return "mirror"; }
        public void start(MazeContext c) {
            c.state.axis = c.random.nextInt(2);
        }
        public void step(MazeContext c) {
            List<BlockPos> order = new ArrayList<>(c.columns);
            Collections.shuffle(order, new java.util.Random(c.random.nextLong()));
            int last = QuantumMaze.SIZE - 1;
            for (BlockPos col : order) {
                if (!c.budgetLeft()) break;
                int x = c.localX(col), z = c.localZ(col);
                int mx = c.state.axis == 0 ? last - x : x;
                int mz = c.state.axis == 1 ? last - z : z;
                boolean wantOpen = !c.solidAt(c.floor(col), mx, mz);
                if (c.isOpen(col) != wantOpen) c.set(col, wantOpen);
            }
        }
    };

    public static final List<MazePattern> ALL = List.of(FLICKER, WAVE, BREATH, HUNT, REBUILD, MIRROR);

    public static MazePattern byId(String id) {
        for (MazePattern p : ALL) if (p.id().equals(id)) return p;
        return null;
    }

    public static MazePattern random(RandomSource random, MazePattern avoid) {
        MazePattern p;
        do {
            p = ALL.get(random.nextInt(ALL.size()));
        } while (p == avoid && ALL.size() > 1);
        return p;
    }
}
