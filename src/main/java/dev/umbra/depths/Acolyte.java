package dev.umbra.depths;

import dev.umbra.light.LightSensitive;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * 2.7: the drowned acolyte. One of those who stayed with the Keepers when the halls were flooded, and never left.
 * Swims, holds on to you and pulls you to the bottom; light makes it let go and draw back.
 */
public class Acolyte extends Monster implements LightSensitive {
    private int scared;

    public Acolyte(EntityType<? extends Acolyte> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.06F, 0.1F, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.9)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 3.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 0.8, 30));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
            this.moveRelative(0.05F, input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        } else {
            super.travel(input);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) return;
        if (scared > 0) {
            scared--;
            this.setTarget(null);
        }
        if (this.isInWater() && this.tickCount % 40 == 0)
            level.sendParticles(ParticleTypes.BUBBLE, this.getX(), this.getY() + 1.6, this.getZ(), 3, 0.1, 0.1, 0.1, 0.02);
    }

    /** Holds on and drags the prey to the bottom. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living && living.isInWater()) {
            pull(living);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.DROWNED_HURT_WATER, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
        return hit;
    }

    public static void pull(LivingEntity living) {
        Vec3 v = living.getDeltaMovement();
        living.setDeltaMovement(v.x * 0.3, -0.5, v.z * 0.3);
        living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1));
        living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 60, 0));
        if (living instanceof ServerPlayer sp && sp.connection != null) sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        scared = 20 * 5;
        this.setTarget(null);
        if (source != null) {
            Vec3 away = this.position().subtract(source.position()).normalize().scale(0.6);
            this.setDeltaMovement(away);
        }
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.DROWNED_AMBIENT_WATER;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.DROWNED_HURT_WATER;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.DROWNED_DEATH_WATER;
    }
}
