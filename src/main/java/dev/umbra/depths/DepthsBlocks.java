package dev.umbra.depths;

import dev.umbra.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 2.7: the fixtures of the drowned halls. All of them stand in water and cannot be broken.
 * <ul>
 *   <li>{@link Valve}: a wheel on a pipe. Turned, it lifts one of the three sluice gates before the sanctum.</li>
 *   <li>{@link Bell}: a drowned bell. When all four have rung, the Drowned Keeper wakes in the sanctum.</li>
 *   <li>{@link Lamp}: a Keepers' lamp gone out. A luminite crystal lights it again: light under the water,
 *       and near a burning one the mind comes back.</li>
 * </ul>
 */
public final class DepthsBlocks {
    private DepthsBlocks() {}

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty ON = BooleanProperty.create("on");

    /** A block that sits in the water and is turned, rung or lit once. */
    public abstract static class Fixture extends Block implements SimpleWaterloggedBlock {
        private final VoxelShape shape;

        protected Fixture(Properties properties, VoxelShape shape) {
            super(properties);
            this.shape = shape;
            registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, true).setValue(ON, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(WATERLOGGED, ON);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return shape;
        }
    }

    public static class Valve extends Fixture {
        public Valve(Properties properties) {
            super(properties, Block.box(2, 2, 2, 14, 14, 14));
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            if (state.getValue(ON)) {
                player.sendOverlayMessage(Component.translatable("block.umbra.depths_valve.done").withStyle(ChatFormatting.DARK_AQUA));
                return InteractionResult.SUCCESS;
            }
            level.setBlock(pos, state.setValue(ON, true), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.5F, 0.5F);
            server.playSound(null, pos, SoundEvents.CONDUIT_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.6F);
            server.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.1);
            int open = Depths.valveTurned(server, pos);
            if (player instanceof ServerPlayer sp) {
                dev.umbra.journal.Journal.unlock(sp, "depths_sluice");
                sp.sendOverlayMessage(Component.translatable(open >= Depths.VALVES ? "block.umbra.depths_valve.all" : "block.umbra.depths_valve.turned", open, Depths.VALVES)
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static class Bell extends Fixture {
        public Bell(Properties properties) {
            super(properties, Block.box(3, 2, 3, 13, 15, 13));
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            server.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 3.0F, 0.45F);
            server.playSound(null, pos, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 2.0F, 0.5F);
            server.sendParticles(ParticleTypes.BUBBLE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 40, 0.6, 0.6, 0.6, 0.2);
            if (state.getValue(ON)) return InteractionResult.SUCCESS;
            level.setBlock(pos, state.setValue(ON, true), Block.UPDATE_ALL);
            int rung = Depths.bellRung(server, pos, player instanceof ServerPlayer sp ? sp : null);
            if (player instanceof ServerPlayer sp) {
                dev.umbra.journal.Journal.unlock(sp, "depths_bells");
                if (rung < Depths.BELLS)
                    sp.sendOverlayMessage(Component.translatable("block.umbra.depths_bell.rung", rung, Depths.BELLS).withStyle(ChatFormatting.DARK_AQUA));
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static class Lamp extends Fixture {
        public Lamp(Properties properties) {
            super(properties, Block.box(3, 0, 3, 13, 14, 13));
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!stack.is(ModItems.LUMINITE_CRYSTAL.get()) || state.getValue(ON)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            level.setBlock(pos, state.setValue(ON, true), Block.UPDATE_ALL);
            if (!player.isCreative()) stack.shrink(1);
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5F, 0.8F);
            server.playSound(null, pos, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.4F);
            server.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.05);
            if (player instanceof ServerPlayer sp) {
                dev.umbra.journal.Journal.unlock(sp, "depths_lamps");
                sp.sendOverlayMessage(Component.translatable("block.umbra.depths_lamp.lit").withStyle(ChatFormatting.AQUA));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && !state.getValue(ON))
                player.sendOverlayMessage(Component.translatable("block.umbra.depths_lamp.dark").withStyle(ChatFormatting.DARK_AQUA));
            return InteractionResult.SUCCESS;
        }
    }

    /** Is there a lit Keepers' lamp within {@code r} blocks? */
    public static boolean nearLitLamp(Level level, BlockPos at, int r) {
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-r, -r, -r), at.offset(r, r, r))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof Lamp && s.getValue(ON)) return true;
        }
        return false;
    }
}
