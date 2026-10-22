package dev.umbra.depths;

import dev.umbra.boss.BossMoves;
import dev.umbra.boss.UmbraBoss;
import dev.umbra.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * 2.7: the Drowned Keeper, the last of the Keepers, woken by the four bells. A real boss now, in three phases:
 * <ul>
 *   <li>1, the whirlpool: he draws everyone in, and the water bursts where you were;</li>
 *   <li>2, the murk: he clouds the water (you are blind) and calls his acolytes;</li>
 *   <li>3, the rush: he throws himself at you again and again.</li>
 * </ul>
 * His weak point is the light he gave up: while fewer than {@link #LAMPS_NEEDED} lamps burn in the sanctum he takes
 * only half of every blow.
 */
public class DrownedKeeper extends UmbraBoss {
    public static final int LAMPS_NEEDED = 2;
    public static final float DARK_GUARD = 0.5F;
    private final BossMoves.Bursts bursts = new BossMoves.Bursts();
    private int whirl, murk, call, rush, burst;

    public DrownedKeeper(EntityType<? extends DrownedKeeper> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.06F, 0.1F, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 420.0)
                .add(Attributes.MOVEMENT_SPEED, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.SCALE, 1.5);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isInWater()) {
            this.moveRelative(0.06F, input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        } else {
            super.travel(input);
        }
    }

    @Override
    protected int rawPhase() {
        float h = this.getHealth() / this.getMaxHealth();
        return h > 0.66F ? 1 : h > 0.33F ? 2 : 3;
    }

    @Override
    protected void bossTick(ServerLevel level) {
        int phase = trackPhase(level);
        bursts.tick(level, this, 7.0F);
        LivingEntity target = this.getTarget();
        if (target == null) return;
        if (++whirl >= 170) {
            whirl = 0;
            BossMoves.pull(level, this, 14, 1.0);
            level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, this.getX(), this.getY() + 1, this.getZ(), 80, 4.0, 2.0, 4.0, 0.3);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CONDUIT_ATTACK_TARGET, SoundSource.HOSTILE, 2.0F, 0.5F);
        }
        if (++burst >= (phase == 3 ? 70 : 110)) {
            burst = 0;
            for (ServerPlayer p : opponents(level)) bursts.mark(level, p.blockPosition(), 25);
        }
        if (phase >= 2 && ++murk >= 240) {
            murk = 0;
            for (ServerPlayer p : opponents(level)) {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
            }
            level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1, this.getZ(), 150, 4.0, 2.0, 4.0, 0.05);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLOW_SQUID_SQUIRT, SoundSource.HOSTILE, 2.0F, 0.5F);
        }
        if (phase >= 2 && ++call >= 300) {
            call = 0;
            if (level.getEntitiesOfClass(Acolyte.class, this.getBoundingBox().inflate(20)).size() < 4)
                BossMoves.summon(level, this, ModEntities.ACOLYTE.get(), 2, target);
        }
        if (phase == 3 && ++rush >= 80 && this.distanceTo(target) > 4) {
            rush = 0;
            Vec3 d = target.position().subtract(this.position()).normalize().scale(1.4);
            this.setDeltaMovement(d);
            level.sendParticles(ParticleTypes.BUBBLE, this.getX(), this.getY() + 1, this.getZ(), 30, 0.5, 0.5, 0.5, 0.2);
        }
    }

    @Override
    protected void stunnedTick(ServerLevel level) {
        bursts.tick(level, this, 7.0F);
    }

    /** He cannot bear the light he gave up: without the sanctum's lamps lit he takes half. */
    @Override
    protected float weakness(ServerLevel level, DamageSource source) {
        return Depths.sanctumLamps(level) >= LAMPS_NEEDED ? 1.0F : DARK_GUARD;
    }

    /** Share of a blow that lands right now (for the journal and the tests). */
    public float guard(ServerLevel level) {
        return weakness(level, level.damageSources().magic());
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity l && l.isInWater()) Acolyte.pull(l);
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            Depths.keeperDown(level);
            for (Acolyte a : level.getEntitiesOfClass(Acolyte.class, this.getBoundingBox().inflate(24))) a.discard();
        }
    }

    public static BlockPos home() {
        return Depths.BOSS_AT;
    }
}
