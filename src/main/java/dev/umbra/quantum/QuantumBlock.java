package dev.umbra.quantum;

import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wall that only exists while it is observed. Whenever no player is looking at it,
 * it may flip between solid (indistinguishable from a maze wall) and open (empty passage).
 */
public class QuantumBlock extends Block {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    private static final int CHECK_INTERVAL = 10;
    private static final int IDLE_INTERVAL = 40;
    private static final double RANGE = 32.0;
    private static final float FLIP_CHANCE = 0.18F;
    /** cos(70°): anything within 70° of the view direction counts as "seen". */
    private static final double VIEW_COS = 0.34;

    private static final double[][] SAMPLES = {
            {0.5, 0.5, 0.5},
            {0.05, 0.05, 0.05}, {0.95, 0.05, 0.05}, {0.05, 0.95, 0.05}, {0.95, 0.95, 0.05},
            {0.05, 0.05, 0.95}, {0.95, 0.05, 0.95}, {0.05, 0.95, 0.95}, {0.95, 0.95, 0.95}
    };

    public QuantumBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) level.scheduleTick(pos, this, CHECK_INTERVAL);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // safety net for blocks placed during world generation (no onPlace there)
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, CHECK_INTERVAL);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Player nearest = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, RANGE, false);
        if (nearest == null) {
            level.scheduleTick(pos, this, IDLE_INTERVAL);
            return;
        }
        // A vertical stack of quantum blocks behaves as one wall; the bottom block decides for all of them.
        if (level.getBlockState(pos.below()).is(this)) {
            return; // not the leader: no own ticks needed
        }
        List<BlockPos> column = new ArrayList<>();
        BlockPos p = pos;
        while (column.size() < 8 && level.getBlockState(p).is(this)) {
            column.add(p);
            p = p.above();
        }
        if (random.nextFloat() < FLIP_CHANCE && !anyObserved(level, column)) {
            boolean open = state.getValue(OPEN);
            boolean blocked = open && column.stream().anyMatch(c -> occupied(level, c)); // never materialise inside someone
            if (!blocked) {
                for (BlockPos c : column) {
                    BlockState cs = level.getBlockState(c);
                    level.setBlock(c, cs.setValue(OPEN, !open), Block.UPDATE_ALL);
                }
            }
        }
        level.scheduleTick(pos, this, CHECK_INTERVAL);
    }

    private static boolean anyObserved(ServerLevel level, List<BlockPos> column) {
        for (BlockPos c : column) if (isObserved(level, c)) return true;
        return false;
    }

    private static boolean occupied(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new AABB(pos)).isEmpty();
    }

    /** True if any player within range can currently see any part of this block. */
    public static boolean isObserved(ServerLevel level, BlockPos pos) {
        for (Player player : level.players()) {
            if (player.isSpectator()) continue;
            Vec3 eye = player.getEyePosition();
            if (eye.distanceToSqr(Vec3.atCenterOf(pos)) > RANGE * RANGE) continue;
            if (eye.distanceToSqr(Vec3.atCenterOf(pos)) < 2.0 * 2.0) return true; // too close to cheat
            Vec3 look = player.getViewVector(1.0F);
            for (double[] s : SAMPLES) {
                Vec3 point = new Vec3(pos.getX() + s[0], pos.getY() + s[1], pos.getZ() + s[2]);
                Vec3 dir = point.subtract(eye);
                double len = dir.length();
                if (len < 1.0E-4) return true;
                if (look.dot(dir.scale(1.0 / len)) < VIEW_COS) continue;
                BlockHitResult hit = level.clip(new ClipContext(eye, point, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
                if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos)) return true;
            }
        }
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(OPEN) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return state.getValue(OPEN);
    }

    @Override
    protected int getLightDampening(BlockState state) {
        return state.getValue(OPEN) ? 0 : 15;
    }
}
