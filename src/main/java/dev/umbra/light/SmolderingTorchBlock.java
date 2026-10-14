package dev.umbra.light;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * In the Underside an ordinary torch only smoulders: it lights a small circle and burns out after a few minutes.
 * Only eternal torches and lamps last there.
 */
public class SmolderingTorchBlock extends TorchBlock {
    public static final int LIGHT = 8;
    /** 4 to 6 minutes. */
    public static final int BURN_MIN = 20 * 60 * 4;
    public static final int BURN_SPREAD = 20 * 60 * 2;

    public SmolderingTorchBlock(SimpleParticleType particle, Properties properties) {
        super(particle, properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        scheduleBurnout(level, pos, this);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        burnOut(level, pos);
    }

    /** Torches placed in the Underside turn into smouldering ones. */
    public static void onPlaced(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !dev.umbra.underside.Underside.is(level)) return;
        BlockState placed = event.getPlacedBlock();
        BlockState replacement = null;
        if (placed.is(Blocks.TORCH) || placed.is(Blocks.SOUL_TORCH) || placed.is(Blocks.COPPER_TORCH)) {
            replacement = dev.umbra.registry.ModBlocks.SMOLDERING_TORCH.get().defaultBlockState();
        } else if (placed.is(Blocks.WALL_TORCH) || placed.is(Blocks.SOUL_WALL_TORCH) || placed.is(Blocks.COPPER_WALL_TORCH)) {
            replacement = dev.umbra.registry.ModBlocks.SMOLDERING_WALL_TORCH.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, placed.getValue(net.minecraft.world.level.block.WallTorchBlock.FACING));
        }
        if (replacement == null) return;
        BlockPos pos = event.getPos();
        BlockState target = replacement;
        Block original = placed.getBlock();
        level.getServer().execute(() -> {
            if (level.getBlockState(pos).is(original)) level.setBlock(pos, target, Block.UPDATE_ALL);
        });
    }

    static void scheduleBurnout(Level level, BlockPos pos, Block block) {
        if (!level.isClientSide()) level.scheduleTick(pos, block, BURN_MIN + level.getRandom().nextInt(BURN_SPREAD));
    }

    static void burnOut(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 6, 0.1, 0.1, 0.1, 0.01);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.2F);
    }
}
