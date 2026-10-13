package dev.umbra.quantum;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
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

import java.util.ArrayList;
import java.util.List;

/**
 * A wall that only exists while it is observed. Solid it looks exactly like a maze wall; open it is an empty passage.
 * A vertical stack of quantum blocks behaves as one wall ("column").
 *
 * LINKED blocks belong to a generated maze and are driven by its {@link MazeCoreBlock};
 * free-standing ones (placed by a player) just flicker on their own.
 */
public class QuantumBlock extends Block {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");

    private static final int CHECK_INTERVAL = 10;
    private static final int IDLE_INTERVAL = 40;
    public static final double RANGE = 32.0;
    private static final float FLIP_CHANCE = 0.18F;
    /** cos(70°): anything within 70° of the view direction counts as "seen". */
    private static final double VIEW_COS = 0.34;

    public QuantumBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(OPEN, false).setValue(LINKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN, LINKED);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this) && !state.getValue(LINKED)) level.scheduleTick(pos, this, CHECK_INTERVAL);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(LINKED);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, CHECK_INTERVAL);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LINKED)) return; // the maze core decides
        if (level.getBlockState(pos.below()).is(this)) return; // only the bottom block of a stack leads
        Player nearest = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, RANGE, false);
        if (nearest == null) {
            level.scheduleTick(pos, this, IDLE_INTERVAL);
            return;
        }
        if (random.nextFloat() < FLIP_CHANCE) setColumn(level, pos, !state.getValue(OPEN));
        level.scheduleTick(pos, this, CHECK_INTERVAL);
    }

    // ---------------------------------------------------------------- column helpers (used by mazes too)

    /** Blocks of the quantum stack starting at {@code bottom}. */
    public static List<BlockPos> column(Level level, BlockPos bottom) {
        List<BlockPos> list = new ArrayList<>();
        BlockPos p = bottom;
        while (list.size() < 8 && level.getBlockState(p).getBlock() instanceof QuantumBlock) {
            list.add(p);
            p = p.above();
        }
        return list;
    }

    public static boolean isOpen(Level level, BlockPos bottom) {
        BlockState s = level.getBlockState(bottom);
        return s.getBlock() instanceof QuantumBlock && s.getValue(OPEN);
    }

    /**
     * Tries to set the whole column open/closed. Refuses while anyone can see it,
     * and never closes it on top of a living entity.
     * @return true if the column changed
     */
    public static boolean setColumn(ServerLevel level, BlockPos bottom, boolean open) {
        List<BlockPos> col = column(level, bottom);
        if (col.isEmpty() || isOpen(level, bottom) == open) return false;
        BlockPos top = col.get(col.size() - 1);
        AABB box = new AABB(bottom.getX(), bottom.getY(), bottom.getZ(), bottom.getX() + 1, top.getY() + 1, bottom.getZ() + 1);
        if (!open && !level.getEntitiesOfClass(LivingEntity.class, box).isEmpty()) return false;
        if (isObserved(level, box)) return false;
        for (BlockPos c : col) {
            BlockState cs = level.getBlockState(c);
            level.setBlock(c, cs.setValue(OPEN, open), Block.UPDATE_ALL);
        }
        return true;
    }

    /** True if any player within range can currently see any part of {@code box}. */
    public static boolean isObserved(ServerLevel level, AABB box) {
        Vec3 center = box.getCenter();
        double[] xs = {box.minX + 0.05, box.maxX - 0.05};
        double[] ys = {box.minY + 0.05, box.maxY - 0.05};
        double[] zs = {box.minZ + 0.05, box.maxZ - 0.05};
        List<Vec3> samples = new ArrayList<>(9);
        samples.add(center);
        for (double x : xs) for (double y : ys) for (double z : zs) samples.add(new Vec3(x, y, z));

        for (Player player : level.players()) {
            if (player.isSpectator()) continue;
            Vec3 eye = player.getEyePosition();
            double d2 = eye.distanceToSqr(center);
            if (d2 > RANGE * RANGE) continue;
            if (box.inflate(1.5).contains(eye)) return true; // right next to it: no cheating
            Vec3 look = player.getViewVector(1.0F);
            for (Vec3 point : samples) {
                Vec3 dir = point.subtract(eye);
                double len = dir.length();
                if (len < 1.0E-4) return true;
                if (look.dot(dir.scale(1.0 / len)) < VIEW_COS) continue;
                BlockHitResult hit = level.clip(new ClipContext(eye, point, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
                if (hit.getType() == HitResult.Type.MISS) return true;
                BlockPos hp = hit.getBlockPos();
                if (box.contains(Vec3.atCenterOf(hp))) return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- shape & light

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
