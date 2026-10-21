package dev.umbra.altar;

import dev.umbra.boss.SummonSealItem;
import dev.umbra.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 2.8: the sacrificial altar (Жертвенник). Lay a seal on it, put in its dish what the seal asks for, make the place
 * round it right, and lay your hand on it (use it with an empty hand): the rite begins, and its master steps out.
 * Sneak and use it with an empty hand to take back what lies on it. It wakes only in the Underside.
 * glow: 0 asleep, 1 a seal lies on it, 2 the rite is under way.
 */
public class SummoningAltarBlock extends BaseEntityBlock {
    public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, 2);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(1.5, 4, 1.5, 14.5, 11, 14.5),
            Block.box(0.5, 11, 0.5, 15.5, 14, 15.5));

    public SummoningAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GLOW, 0));
    }

    public static int light(BlockState state) {
        return switch (state.getValue(GLOW)) { case 1 -> 7; case 2 -> 13; default -> 3; };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GLOW);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SummoningAltarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.SUMMONING_ALTAR.get(), SummoningAltarBlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.SUMMONING_ALTAR.get(), SummoningAltarBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level.getBlockEntity(pos) instanceof SummoningAltarBlockEntity altar)) return InteractionResult.PASS;
        if (stack.getItem() instanceof SummonSealItem) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (altar.riteRunning()) return InteractionResult.FAIL;
            if (!altar.seal().isEmpty()) {
                player.sendOverlayMessage(Component.translatable("message.umbra.altar.taken"));
                return InteractionResult.FAIL;
            }
            altar.placeSeal(stack, player);
            level.setBlock(pos, state.setValue(GLOW, 1), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 0.6F);
            if (player instanceof ServerPlayer sp) {
                dev.umbra.journal.Journal.unlock(sp, "altar");
                Ritual r = altar.ritual();
                Component lack = altar.missing();
                sp.sendOverlayMessage(Component.translatable("message.umbra.altar.laid." + r.id, lack == null ? Component.empty() : lack));
            }
            return InteractionResult.SUCCESS;
        }
        if (altar.seal().isEmpty()) return InteractionResult.PASS;
        Ritual r = altar.ritual();
        boolean wanted = r != null && r.offerings.stream().anyMatch(o -> o.matches(stack));
        if (!wanted) {
            // a block in the hand is meant to be placed (a chest, a campfire round the altar); anything else is a wrong gift
            if (stack.getItem() instanceof BlockItem) return InteractionResult.PASS;
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.umbra.altar.unwanted"));
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        int put = altar.offer(stack, player);
        if (put > 0) {
            level.playSound(null, pos, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.3F);
            ((ServerLevel) level).sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 6, 0.2, 0.05, 0.2, 0.01);
            Component lack = altar.missing();
            player.sendOverlayMessage(lack == null ? Component.translatable("message.umbra.altar.ready") : Component.translatable("message.umbra.altar.wants", lack));
        } else {
            player.sendOverlayMessage(Component.translatable("message.umbra.altar.enough"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SummoningAltarBlockEntity altar)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (altar.seal().isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.umbra.altar.empty"));
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            if (altar.riteRunning()) return InteractionResult.FAIL;
            altar.takeBack(player);
            level.setBlock(pos, state.setValue(GLOW, 0), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 0.7F);
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) altar.begin((ServerLevel) level, sp);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(GLOW) == 0 && random.nextInt(6) != 0) return;
        // smoke from the horn tips, soul sparks when a seal lies on it
        double x = pos.getX() + (random.nextBoolean() ? 0.08 : 0.92), z = pos.getZ() + (random.nextBoolean() ? 0.08 : 0.92);
        level.addParticle(state.getValue(GLOW) == 0 ? ParticleTypes.SMOKE : ParticleTypes.SOUL_FIRE_FLAME, x, pos.getY() + 1.3, z, 0, 0.01, 0);
    }
}
