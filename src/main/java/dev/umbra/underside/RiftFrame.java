package dev.umbra.underside;

import dev.umbra.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rift frames: a rectangle of dusk stone (like a nether portal frame, corners optional),
 * interior 2..6 wide and 3..8 tall, standing along X or Z.
 */
public final class RiftFrame {
    public static final int MIN_W = 2, MAX_W = 6, MIN_H = 3, MAX_H = 8;

    private RiftFrame() {}

    public static boolean isFrame(BlockState state) {
        return state.is(ModBlocks.DUSK_STONE.get());
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.getBlock() instanceof RiftBlock;
    }

    /** Tries to open a rift in a frame that contains {@code inside}. */
    public static boolean tryOpen(Level level, BlockPos inside) {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            int[] r = measure(level, inside, axis);
            if (r != null) {
                fill(level, r, axis);
                return true;
            }
        }
        return false;
    }

    /** @return {minX/minZ origin x, y, z, width, height} of the interior, or null if this is not a valid frame */
    private static int[] measure(LevelAccessor level, BlockPos inside, Direction.Axis axis) {
        if (!isEmpty(level.getBlockState(inside))) return null;
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction left = right.getOpposite();

        BlockPos bottom = inside;
        for (int i = 0; i < MAX_H && isEmpty(level.getBlockState(bottom.below())); i++) bottom = bottom.below();
        if (!isFrame(level.getBlockState(bottom.below()))) return null;

        BlockPos start = bottom;
        for (int i = 0; i < MAX_W && isEmpty(level.getBlockState(start.relative(left))); i++) start = start.relative(left);
        if (!isFrame(level.getBlockState(start.relative(left)))) return null;

        int width = 0;
        while (width < MAX_W + 1 && isEmpty(level.getBlockState(start.relative(right, width)))) width++;
        if (width < MIN_W || width > MAX_W || !isFrame(level.getBlockState(start.relative(right, width)))) return null;

        int height = 0;
        while (height < MAX_H + 1 && isEmpty(level.getBlockState(start.above(height)))) height++;
        if (height < MIN_H || height > MAX_H) return null;

        for (int w = 0; w < width; w++) {
            if (!isFrame(level.getBlockState(start.relative(right, w).below()))) return null;
            if (!isFrame(level.getBlockState(start.relative(right, w).above(height)))) return null;
            for (int h = 0; h < height; h++) {
                if (!isEmpty(level.getBlockState(start.relative(right, w).above(h)))) return null;
            }
        }
        for (int h = 0; h < height; h++) {
            if (!isFrame(level.getBlockState(start.relative(left).above(h)))) return null;
            if (!isFrame(level.getBlockState(start.relative(right, width).above(h)))) return null;
        }
        return new int[]{start.getX(), start.getY(), start.getZ(), width, height};
    }

    private static void fill(LevelAccessor level, int[] r, Direction.Axis axis) {
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockState rift = ModBlocks.RIFT.get().defaultBlockState().setValue(RiftBlock.AXIS, axis);
        BlockPos start = new BlockPos(r[0], r[1], r[2]);
        for (int w = 0; w < r[3]; w++) {
            for (int h = 0; h < r[4]; h++) {
                level.setBlock(start.relative(right, w).above(h), rift, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    /** Builds a complete 2x3 rift with its frame, standing on {@code base} (bottom-left interior block), along X. */
    public static void build(LevelAccessor level, BlockPos base) {
        BlockState frame = ModBlocks.DUSK_STONE.get().defaultBlockState();
        for (int x = -1; x <= 2; x++) {
            for (int y = -1; y <= 3; y++) {
                boolean edge = x == -1 || x == 2 || y == -1 || y == 3;
                BlockPos p = base.offset(x, y, 0);
                level.setBlock(p, edge ? frame : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        fill(level, new int[]{base.getX(), base.getY(), base.getZ(), 2, 3}, Direction.Axis.X);
    }
}
