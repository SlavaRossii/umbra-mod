package dev.umbra.boss;

import dev.umbra.light.LightSensitive;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Пастух тьмы: wrapped in living darkness, untouchable while the shroud holds. Burn it away with the Dispeller,
 * then strike. He herds waves of darkness at you and calls shades to his side.
 */
public class Shepherd extends UmbraBoss implements LightSensitive {
    private int waveTimer;
    private int shadeTimer;
    private int noShroud;
    private int sweepTimer;
    private int pulseTimer;
    private final BossMoves.Bursts bursts = new BossMoves.Bursts();

    public Shepherd(EntityType<? extends Shepherd> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 160.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.SCALE, 1.4);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Is the shroud of darkness around him intact? */
    public boolean shrouded(ServerLevel level) {
        BlockPos c = this.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-1, 0, -1), c.offset(1, 2, 1))) {
            if (level.getBlockState(p).is(ModBlocks.LIVING_DARKNESS.get())) return true;
        }
        return false;
    }

    private void weaveShroud(ServerLevel level) {
        BlockPos c = this.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-1, 0, -1), c.offset(1, 2, 1))) {
            if (level.getBlockState(p).isAir()) level.setBlock(p, ModBlocks.LIVING_DARKNESS.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void bossTick(ServerLevel level) {
        if (noShroud > 0) noShroud--;
        else if (this.tickCount % 40 == 0) weaveShroud(level);

        int phase = trackPhase(level);
        bursts.tick(level, this, 7.0F);
        LivingEntity target = this.getTarget();
        if (target == null) return;
        // crook sweep: too close and you are thrown away
        if (++sweepTimer >= 70 && this.distanceTo(target) < 4.5) {
            sweepTimer = 0;
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            BossMoves.shockwave(level, this, 4.5, 6.0F, 1.5);
        }
        // phase 2: the herd is called home — pulled in, then the darkness bursts around him
        if (phase == 2 && ++pulseTimer >= 180) {
            pulseTimer = 0;
            BossMoves.pull(level, this, 14, 1.3);
            BlockPos c = this.blockPosition();
            for (int a = 0; a < 8; a++) {
                double rad = a * Math.PI / 4;
                bursts.mark(level, c.offset((int) Math.round(Math.cos(rad) * 3), 0, (int) Math.round(Math.sin(rad) * 3)), 30);
            }
        }
        if (++waveTimer >= (phase == 1 ? 120 : 90)) {
            waveTimer = 0;
            BlockPos t = target.blockPosition();
            int r = 3 + this.random.nextInt(3);
            for (int a = 0; a < 360; a += 12) {
                double rad = Math.toRadians(a);
                BlockPos p = t.offset((int) Math.round(Math.cos(rad) * r), 0, (int) Math.round(Math.sin(rad) * r));
                for (int y = 0; y < 3; y++) {
                    if (level.getBlockState(p.above(y)).isAir()) level.setBlock(p.above(y), ModBlocks.LIVING_DARKNESS.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            level.playSound(null, t, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0F, 0.5F);
        }
        if (++shadeTimer >= (phase == 1 ? 400 : 260)) {
            shadeTimer = 0;
            for (int i = 0; i < (phase == 1 ? 3 : 4); i++) {
                var shade = ModEntities.SHADE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                if (shade == null) continue;
                shade.snapTo(this.getX() + this.random.nextGaussian() * 2, this.getY(), this.getZ() + this.random.nextGaussian() * 2, 0, 0);
                level.addFreshEntity(shade);
                shade.setTarget(target);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (shrouded(level) && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.5, this.getZ(), 8, 0.4, 0.6, 0.4, 0.02);
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        BlockPos c = this.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-2, -1, -2), c.offset(2, 3, 2))) {
            if (level.getBlockState(p).is(ModBlocks.LIVING_DARKNESS.get())) level.removeBlock(p, false);
        }
        noShroud = 20 * 5;
    }
}
