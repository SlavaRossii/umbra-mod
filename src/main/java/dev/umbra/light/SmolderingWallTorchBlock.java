package dev.umbra.light;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SmolderingWallTorchBlock extends WallTorchBlock {
    public SmolderingWallTorchBlock(SimpleParticleType particle, Properties properties) {
        super(particle, properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        SmolderingTorchBlock.scheduleBurnout(level, pos, this);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        SmolderingTorchBlock.burnOut(level, pos);
    }
}
