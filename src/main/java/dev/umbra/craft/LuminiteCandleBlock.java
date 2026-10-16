package dev.umbra.craft;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Luminite candles: one to four on a block, each a little steady light. Around an echo altar they deepen its echo. */
public class LuminiteCandleBlock extends Block {
    public static final IntegerProperty CANDLES = IntegerProperty.create("candles", 1, 4);
    private static final VoxelShape[] SHAPES = {
            Block.box(7, 0, 7, 9, 8, 9), Block.box(4, 0, 7, 9, 8, 11), Block.box(4, 0, 5, 12, 9, 11), Block.box(4, 0, 4, 12, 9, 12)};

    public LuminiteCandleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CANDLES, 1));
    }

    public static int light(BlockState state) {
        return 4 + 3 * (state.getValue(CANDLES) - 1);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CANDLES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(CANDLES) - 1];
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(this.asItem()) || state.getValue(CANDLES) >= 4) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(CANDLES, state.getValue(CANDLES) + 1), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.BLOCKS, 0.8F, 1.4F);
            if (!player.isCreative()) stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
