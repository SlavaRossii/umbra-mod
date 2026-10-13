package dev.umbra.darkness;

import dev.umbra.light.DarknessProof;
import dev.umbra.util.UmbraUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Darkness that behaves like a very slow fluid: it creeps through unlit, enclosed air,
 * recedes from light and snuffs out torches at its edge. Lanterns and glowstone are safe.
 */
public class LivingDarknessBlock extends Block {
    /** Darkness survives where block light is at most this. */
    public static final int MAX_LIGHT = 4;
    private static final int TORCH_SEARCH = 11;
    private static final int MASS_RADIUS = 2;
    private static final int MASS_NEEDED = 10;

    private static final Direction[] SPREAD_DIRS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
            Direction.DOWN, Direction.UP
    };

    public LivingDarknessBlock(Properties properties) {
        super(properties);
    }

    public static boolean canExist(Level level, BlockPos pos) {
        return UmbraUtil.skyLight(level, pos) == 0 && UmbraUtil.blockLight(level, pos) <= MAX_LIGHT;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canExist(level, pos)) {
            level.removeBlock(pos, false);
            level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.01);
            return;
        }
        // creep
        for (int i = 0; i < 2; i++) {
            BlockPos target = pos.relative(SPREAD_DIRS[random.nextInt(SPREAD_DIRS.length)]);
            if (level.isLoaded(target) && level.getBlockState(target).isAir() && canExist(level, target)) {
                level.setBlock(target, this.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        // snuff out a torch if there is enough darkness here
        if (random.nextInt(12) == 0 && countMass(level, pos) >= MASS_NEEDED) {
            BlockPos torch = findTorch(level, pos);
            if (torch != null) {
                level.destroyBlock(torch, true);
                level.playSound(null, torch, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 0.8F);
                level.sendParticles(ParticleTypes.SMOKE, torch.getX() + 0.5, torch.getY() + 0.6, torch.getZ() + 0.5, 8, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }

    private int countMass(ServerLevel level, BlockPos pos) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-MASS_RADIUS, -MASS_RADIUS, -MASS_RADIUS), pos.offset(MASS_RADIUS, MASS_RADIUS, MASS_RADIUS))) {
            if (level.getBlockState(p).is(this)) n++;
        }
        return n;
    }

    private static BlockPos findTorch(ServerLevel level, BlockPos pos) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-TORCH_SEARCH, -TORCH_SEARCH / 2, -TORCH_SEARCH), pos.offset(TORCH_SEARCH, TORCH_SEARCH / 2, TORCH_SEARCH))) {
            Block b = level.getBlockState(p).getBlock();
            if (b instanceof BaseTorchBlock && !(b instanceof RedstoneTorchBlock) && !(b instanceof DarknessProof)) {
                double d = p.distSqr(pos);
                if (d < bestDist) {
                    bestDist = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    /** Used by seeding and /umbra darkness. */
    public static boolean seed(ServerLevel level, BlockPos pos, Block darkness) {
        if (!level.getBlockState(pos).isAir() || !canExist(level, pos)) return false;
        level.setBlock(pos, darkness.defaultBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }

    public static boolean isTorch(BlockState state) {
        return state.getBlock() instanceof BaseTorchBlock && !state.is(Blocks.REDSTONE_TORCH) && !state.is(Blocks.REDSTONE_WALL_TORCH);
    }
}
