package dev.umbra.lake;

import dev.umbra.light.LightSensitive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * Глубинник: lives in the black lakes, where nothing can be seen. A long eel with a pale lure on a stalk.
 * It only hunts what is in the water: bites and drags you down. Light makes it dive away.
 */
public class Lurker extends Monster implements LightSensitive {
    private int dry;
    private int scared;

    public Lurker(EntityType<? extends Lurker> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.08F, 0.1F, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 1.1)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0, 20));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, (target, level) -> target.isInWater()));
    }

    /** Only lakes and only where the water is dark. */
    public static boolean checkSpawn(EntityType<Lurker> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER);
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
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) return;
        if (scared > 0) {
            scared--;
            this.setTarget(null);
        }
        LivingEntity target = this.getTarget();
        if (target != null && !target.isInWater()) this.setTarget(null);
        if (this.isInWater()) {
            dry = 0;
            if (this.tickCount % 30 == 0) level.sendParticles(ParticleTypes.GLOW, this.getX(), this.getY() + 0.8, this.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
        } else if (this.onGround()) {
            // a fish on land: flops back towards the water and dries out
            if (this.random.nextInt(10) == 0) {
                this.setDeltaMovement(this.getDeltaMovement().add((this.random.nextFloat() - 0.5) * 0.3, 0.35, (this.random.nextFloat() - 0.5) * 0.3));
            }
            if (++dry > 200 && dry % 20 == 0) this.hurtServer(level, level.damageSources().dryOut(), 1.0F);
        }
    }

    /** Bites and drags the prey down. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living && living.isInWater()) {
            Vec3 v = living.getDeltaMovement();
            living.setDeltaMovement(v.x * 0.2, -0.7, v.z * 0.2);
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
            if (living instanceof ServerPlayer sp) sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.DROWNED_SHOOT, SoundSource.HOSTILE, 0.8F, 0.5F);
        }
        return hit;
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        scared = 20 * 6;
        this.setTarget(null);
        this.hurtServer(level, source != null ? level.damageSources().playerAttack(source) : level.damageSources().magic(), 4.0F);
        this.setDeltaMovement(this.getDeltaMovement().add(0, -0.6, 0));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return distance > 64 * 64;
    }
}
