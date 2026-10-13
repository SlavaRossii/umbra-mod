package dev.umbra.mimic;

import dev.umbra.sanity.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Supplier;

/**
 * Looks almost like {@link #original} (slightly wrong texture orientation, wrong footstep sound).
 * Hit it first (left click) and it is exposed: it turns back into the real block and drops a reward.
 * Use it, stand on it or linger next to it, and it springs.
 */
public class MimicBlock extends Block {
    private static final int CHECK_INTERVAL = 20;
    private final Supplier<Block> original;

    public MimicBlock(Properties properties, Supplier<Block> original) {
        super(properties);
        this.original = original;
    }

    public Block original() {
        return original.get();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, CHECK_INTERVAL);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        List<Player> near = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(1.5), p -> !p.isSpectator() && !p.isCreative());
        if (!near.isEmpty() && random.nextInt(8) == 0) {
            spring(level, pos);
            return;
        }
        level.scheduleTick(pos, this, CHECK_INTERVAL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) spring(sl, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) spring(sl, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel sl && entity instanceof Player p && !p.isCreative() && !p.isSpectator()) {
            spring(sl, pos);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel sl) expose(sl, pos);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) dev.umbra.journal.Journal.unlock(sp, "mimic");
    }

    /** The player noticed: the mimic flees and leaves the real block plus a reward. */
    public void expose(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, original().defaultBlockState(), Block.UPDATE_ALL);
        Vec3 c = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 25, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, pos, SoundEvents.SILVERFISH_DEATH, SoundSource.BLOCKS, 1.0F, 0.5F);
        ExperienceOrb.award(level, c, 12);
        ItemEntity reward = new ItemEntity(level, c.x, c.y + 0.6, c.z, new ItemStack(Items.EMERALD, 1 + level.getRandom().nextInt(2)));
        level.addFreshEntity(reward);
    }

    /** Too late. */
    public void spring(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        Vec3 c = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 40, 0.5, 0.5, 0.5, 0.1);
        level.playSound(null, pos, SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.0F, 0.7F);
        for (int i = 0; i < 4; i++) {
            EntityTypes.SILVERFISH.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
        }
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(6.0))) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 5, 0));
            Sanity.add(p, Sanity.Shock.MIMIC_SPRUNG);
            if (p instanceof net.minecraft.server.level.ServerPlayer sp) dev.umbra.journal.Journal.unlock(sp, "mimic");
        }
    }
}
