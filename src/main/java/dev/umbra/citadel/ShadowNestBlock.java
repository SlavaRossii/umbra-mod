package dev.umbra.citadel;

import dev.umbra.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A nest of the citadel's living shadow: while someone is near it keeps calling creatures out of the dark,
 * never more than {@link #CAP} of its kind around it. It can be broken (slowly), and stops when it is.
 */
public class ShadowNestBlock extends Block {
    public static final int RANGE = 14;
    public static final int CAP = 3;

    public ShadowNestBlock(Properties properties) {
        super(properties);
    }

    /** What this particular nest calls: mostly crawlers, now and then a blind one. */
    public static EntityType<? extends Mob> kind(BlockPos pos) {
        return Math.floorMod(pos.getX() * 31 + pos.getZ() * 17, 3) == 0 ? ModEntities.BLIND.get() : ModEntities.CRAWLER.get();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, 40);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // safety net after world generation or a reload
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, 40);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        wave(level, pos, true);
        level.scheduleTick(pos, this, 80 + random.nextInt(80));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SCULK_SOUL, pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
    }

    /** One call: spawns a creature next to the nest if someone is near and fewer than {@link #CAP} stand around. */
    public static boolean wave(ServerLevel level, BlockPos pos, boolean needPlayer) {
        if (needPlayer && level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, RANGE, false) == null) return false;
        EntityType<? extends Mob> type = kind(pos);
        int cap = type == ModEntities.BLIND.get() ? 1 : CAP;
        AABB box = new AABB(pos).inflate(12.0, 4.0, 12.0);
        if (level.getEntitiesOfClass(Mob.class, box, m -> m.getType() == type).size() >= cap) return false;
        RandomSource random = level.getRandom();
        for (int tries = 0; tries < 12; tries++) {
            BlockPos at = pos.offset(random.nextInt(9) - 4, 1 + random.nextInt(2) - 1, random.nextInt(9) - 4);
            if (at.distSqr(pos) < 4) continue;
            if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir() || !level.getBlockState(at.below()).isSolid()) continue;
            Mob mob = type.create(level, EntitySpawnReason.SPAWNER);
            if (mob == null) return false;
            mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(mob);
            level.sendParticles(ParticleTypes.SQUID_INK, at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5, 12, 0.3, 0.5, 0.3, 0.02);
            level.playSound(null, at, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.HOSTILE, 0.8F, 0.6F);
            return true;
        }
        return false;
    }
}
