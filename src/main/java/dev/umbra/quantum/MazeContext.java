package dev.umbra.quantum;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.List;

/** Everything a {@link MazePattern} may look at or change. */
public final class MazeContext {
    public final ServerLevel level;
    public final BlockPos corner;
    public final RandomSource random;
    public final List<ServerPlayer> players;
    /** Bottom block of every quantum column of this maze. */
    public final List<BlockPos> columns;
    /** Free per-maze state for patterns (phase counters etc.). */
    public final MazeController.Runtime state;
    private int budget;

    MazeContext(ServerLevel level, BlockPos corner, List<ServerPlayer> players, List<BlockPos> columns, MazeController.Runtime state, int budget) {
        this.level = level;
        this.corner = corner;
        this.random = level.getRandom();
        this.players = players;
        this.columns = columns;
        this.state = state;
        this.budget = budget;
    }

    public int localX(BlockPos p) {
        return p.getX() - corner.getX();
    }

    public int localZ(BlockPos p) {
        return p.getZ() - corner.getZ();
    }

    public int floor(BlockPos p) {
        return Math.max(0, Math.min(QuantumMaze.FLOORS - 1, (p.getY() - corner.getY() - 1) / QuantumMaze.STOREY));
    }

    public boolean isOpen(BlockPos column) {
        return QuantumBlock.isOpen(level, column);
    }

    public boolean budgetLeft() {
        return budget > 0;
    }

    /** Opens or closes a column if nobody sees it. Counts against this step's change budget. */
    public boolean set(BlockPos column, boolean open) {
        if (budget <= 0) return false;
        if (QuantumBlock.setColumn(level, column, open)) {
            budget--;
            return true;
        }
        return false;
    }

    public boolean flip(BlockPos column) {
        return set(column, !isOpen(column));
    }

    /** Is the maze solid at local (x, z) on the given floor (reads the world, any block type)? */
    public boolean solidAt(int floor, int x, int z) {
        BlockPos p = corner.offset(x, QuantumMaze.floorBase(floor) + 2, z);
        return !level.getBlockState(p).getCollisionShape(level, p).isEmpty();
    }
}
