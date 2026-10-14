package dev.umbra.lighthouse;

import dev.umbra.journal.Journal;
import dev.umbra.light.DarknessProof;
import dev.umbra.light.WardBlock;
import dev.umbra.registry.ModItems;
import dev.umbra.sanity.Sanity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The lamp of the lighthouse. It starts dead. Feed it luminite: every {@link #PORTION} crystals it grows one level,
 * and each level widens the circle in which nothing dark can live (like a ward, only bigger), brightens the light and
 * lengthens the beam. At the top level it also soothes the mind of everyone inside.
 */
public class LighthouseLampBlock extends Block implements DarknessProof {
    public static final int MAX_LEVEL = 4;
    public static final int PORTION = 4;
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, MAX_LEVEL);
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, PORTION - 1);
    private static final int[] RADIUS = {0, 14, 22, 32, 44};
    private static final int[] LIGHT = {0, 6, 9, 12, 15};
    private static final int[] BEAM = {0, 6, 12, 20, 32};
    private static final int PULSE = 20;

    public LighthouseLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0).setValue(FILL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, FILL);
    }

    public static int radius(int level) {
        return RADIUS[Math.max(0, Math.min(MAX_LEVEL, level))];
    }

    public static int light(int level) {
        return LIGHT[Math.max(0, Math.min(MAX_LEVEL, level))];
    }

    /** Crystals still needed to reach the next level (0 at the top). */
    public static int needed(BlockState state) {
        return state.getValue(LEVEL) >= MAX_LEVEL ? 0 : PORTION - state.getValue(FILL);
    }

    /** The state after one more crystal. */
    public static BlockState feed(BlockState state) {
        int level = state.getValue(LEVEL), fill = state.getValue(FILL);
        if (level >= MAX_LEVEL) return state;
        if (fill + 1 >= PORTION) return state.setValue(LEVEL, level + 1).setValue(FILL, 0);
        return state.setValue(FILL, fill + 1);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && state.getValue(LEVEL) > 0) level.scheduleTick(pos, this, PULSE);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.LUMINITE_CRYSTAL.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (state.getValue(LEVEL) >= MAX_LEVEL) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("block.umbra.lighthouse_lamp.full"));
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            BlockState next = feed(state);
            server.setBlock(pos, next, Block.UPDATE_ALL);
            if (!player.isCreative()) stack.shrink(1);
            int lvl = next.getValue(LEVEL);
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.8F + lvl * 0.15F);
            if (lvl > state.getValue(LEVEL)) {
                server.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5F, 0.6F + lvl * 0.2F);
                server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 40, 0.4, 0.8, 0.4, 0.05);
                server.scheduleTick(pos, this, PULSE);
                if (player instanceof ServerPlayer sp) {
                    sp.sendOverlayMessage(Component.translatable("block.umbra.lighthouse_lamp.grew", lvl, MAX_LEVEL, radius(lvl)));
                    Journal.unlock(sp, "lighthouse");
                    if (lvl >= 2) Journal.unlock(sp, "yan_2");
                    if (lvl >= MAX_LEVEL) Journal.unlock(sp, "yan_3");
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            int lvl = state.getValue(LEVEL);
            if (player instanceof ServerPlayer sp) Journal.unlock(sp, "lighthouse");
            player.sendOverlayMessage(lvl == 0
                    ? Component.translatable("block.umbra.lighthouse_lamp.dead", PORTION - state.getValue(FILL))
                    : lvl >= MAX_LEVEL
                    ? Component.translatable("block.umbra.lighthouse_lamp.max", radius(lvl))
                    : Component.translatable("block.umbra.lighthouse_lamp.status", lvl, MAX_LEVEL, radius(lvl), needed(state)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int lvl = state.getValue(LEVEL);
        if (lvl <= 0) return;
        level.scheduleTick(pos, this, PULSE);
        int r = radius(lvl);
        WardBlock.mark(level, pos, r);
        WardBlock.repel(level, pos, r);
        // the beam
        int beam = BEAM[lvl];
        for (int i = 0; i < 3 + lvl; i++) {
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5 + random.nextGaussian() * 0.15, pos.getY() + 1.0 + random.nextInt(beam),
                    pos.getZ() + 0.5 + random.nextGaussian() * 0.15, 1, 0, 0.1, 0, 0.0);
        }
        // at the top the lamp calms everyone under it
        if (lvl >= MAX_LEVEL && Sanity.enabled()) {
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) <= (double) r * r) Sanity.add(p, 0.6F);
            }
        }
    }
}
