package dev.umbra.quantum;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Hidden under the centre of every quantum maze; ticks {@link MazeController}. Unbreakable. */
public class MazeCoreBlock extends Block {
    public MazeCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, 20);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // safety net after world generation or a reload
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, 20);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int next = MazeController.tick(level, pos);
        level.scheduleTick(pos, this, next);
    }
}
