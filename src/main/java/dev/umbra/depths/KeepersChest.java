package dev.umbra.depths;

import dev.umbra.registry.ModBlockEntities;
import dev.umbra.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 2.9: the Keepers' chest, the sea-chest on the dais of the sanctum: dark wood gone green, bronze bands, barnacles,
 * a lock of pearl. It cannot be broken and does not open by hand; the Drowned Keeper's key opens it (the key is
 * spent). Each time he is beaten it fills again and shuts, and he lets fall a new key.
 * {@link #OPEN}: the lid is up (unlocked) and the gold shows.
 */
public class KeepersChest extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

    public KeepersChest(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    /** The key: the lock opens and the key stays in it. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(OPEN) || !stack.is(ModItems.KEEPERS_KEY.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        unlock(server, pos, state);
        if (!player.isCreative()) stack.shrink(1);
        if (player instanceof ServerPlayer sp) {
            dev.umbra.journal.Journal.unlock(sp, "depths_treasure");
            sp.sendOverlayMessage(Component.translatable("block.umbra.keepers_chest.unlocked").withStyle(ChatFormatting.AQUA));
            if (level.getBlockEntity(pos) instanceof Entity chest) sp.openMenu(chest);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!state.getValue(OPEN)) {
            server.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 0.7F);
            player.sendOverlayMessage(Component.translatable("block.umbra.keepers_chest.locked").withStyle(ChatFormatting.DARK_AQUA));
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof Entity chest) player.openMenu(chest);
        return InteractionResult.SUCCESS;
    }

    public static void unlock(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.2F, 0.6F);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 30, 0.35, 0.2, 0.35, 0.1);
        level.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.02);
    }

    /** What lies in the chest: 27 slots, filled from the Depths' table each time it is shut again. */
    public static class Entity extends RandomizableContainerBlockEntity {
        private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

        public Entity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.KEEPERS_CHEST.get(), pos, state);
        }

        @Override
        protected void saveAdditional(ValueOutput out) {
            super.saveAdditional(out);
            if (!this.trySaveLootTable(out)) ContainerHelper.saveAllItems(out, this.items, true);
        }

        @Override
        protected void loadAdditional(ValueInput in) {
            super.loadAdditional(in);
            this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
            if (!this.tryLoadLootTable(in)) ContainerHelper.loadAllItems(in, this.items);
        }

        @Override
        public int getContainerSize() {
            return 27;
        }

        @Override
        protected NonNullList<ItemStack> getItems() {
            return this.items;
        }

        @Override
        protected void setItems(NonNullList<ItemStack> items) {
            this.items = items;
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.umbra.keepers_chest");
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
            return ChestMenu.threeRows(id, inventory, this);
        }
    }
}
