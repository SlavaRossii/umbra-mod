package dev.umbra.craft;

import dev.umbra.journal.Journal;
import dev.umbra.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The trophy shelf in Yan's house: six places, one for the relic of each boss. Set a relic there with a click; when
 * all six stand on it, the keeper's house is a little less afraid, and the shelf gives something back.
 */
public class TrophyShelfBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final String[] BOSSES = {"watcher", "mimic_queen", "shepherd", "doppelganger", "architect", "primordial_shadow"};
    public static final BooleanProperty[] HELD = new BooleanProperty[BOSSES.length];

    static {
        for (int i = 0; i < BOSSES.length; i++) HELD[i] = BooleanProperty.create(BOSSES[i]);
    }

    private static final VoxelShape[] SHAPES = {
            Block.box(0, 0, 9, 16, 16, 16), Block.box(0, 0, 0, 7, 16, 16), Block.box(0, 0, 0, 16, 16, 7), Block.box(9, 0, 0, 16, 16, 16)};

    public TrophyShelfBlock(Properties properties) {
        super(properties);
        BlockState s = stateDefinition.any().setValue(FACING, Direction.NORTH);
        for (BooleanProperty p : HELD) s = s.setValue(p, false);
        registerDefaultState(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(HELD);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> SHAPES[1];
            case SOUTH -> SHAPES[2];
            case WEST -> SHAPES[3];
            default -> SHAPES[0];
        };
    }

    /** Which place a relic belongs in, or -1. */
    public static int slotOf(ItemStack stack) {
        for (int i = 0; i < BOSSES.length; i++) if (stack.is(ModItems.TROPHIES.get(BOSSES[i]).get())) return i;
        return -1;
    }

    public static int count(BlockState state) {
        int n = 0;
        for (BooleanProperty p : HELD) if (state.getValue(p)) n++;
        return n;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int slot = slotOf(stack);
        if (slot < 0) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (state.getValue(HELD[slot])) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("block.umbra.trophy_shelf.taken").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            BlockState next = state.setValue(HELD[slot], true);
            server.setBlock(pos, next, Block.UPDATE_ALL);
            if (!player.isCreative()) stack.shrink(1);
            server.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 0.8F);
            server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.02);
            int n = count(next);
            player.sendOverlayMessage(Component.translatable("block.umbra.trophy_shelf.set", n, BOSSES.length).withStyle(ChatFormatting.GOLD));
            if (n == BOSSES.length) complete(server, pos, player);
        }
        return InteractionResult.SUCCESS;
    }

    private static void complete(ServerLevel level, BlockPos pos, Player player) {
        level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        for (ItemStack s : new ItemStack[]{new ItemStack(ModItems.ECHO_QUARTZ.get(), 16), new ItemStack(ModItems.LUMINITE_CRYSTAL.get(), 16),
                new ItemStack(ModItems.KEEPER_PAGE.get(), 2)}) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s));
        }
        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.translatable("block.umbra.trophy_shelf.full").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            Journal.unlock(sp, "trophy_shelf");
        }
    }
}
