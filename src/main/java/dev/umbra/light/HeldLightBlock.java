package dev.umbra.light;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The light of a lit luminite lantern: an invisible, intangible light source next to its carrier.
 * It checks itself twice a second and disappears as soon as no lit lantern is close by,
 * so nothing is ever left behind (not even after a crash: the first tick after loading cleans it up).
 */
public class HeldLightBlock extends Block implements DarknessProof, net.minecraft.world.level.block.SimpleWaterloggedBlock {
    public static final int LIGHT = 15;   // 2.12.2: as bright as light gets (it was 12)
    /** How far from the one carrying the light it may stand (the light thrown ahead stands a few blocks off). */
    public static final double REACH = 6.5;
    private static final int CHECK = 10;

    /** 2.7: under water too (the Depths are drowned): it holds the water it stands in and gives it back. */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty WATERLOGGED =
            net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

    public HeldLightBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    protected net.minecraft.world.level.material.FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? net.minecraft.world.level.material.Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /** What is left when the light goes: water if it stood in water. */
    public static BlockState after(BlockState state) {
        return state.hasProperty(WATERLOGGED) && state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, CHECK);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (Player p : level.players()) {
            if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < REACH * REACH && (LanternItem.carriesLit(p) || dev.umbra.lighthouse.YanErrands.holdsMartasLantern(p) || dev.umbra.gear.SetPowers.shines(p) || dev.umbra.lighthouse.KeeperStaffItem.holds(p))) {
                level.scheduleTick(pos, this, CHECK);
                return;
            }
        }
        level.setBlock(pos, after(state), Block.UPDATE_ALL);
    }
}
