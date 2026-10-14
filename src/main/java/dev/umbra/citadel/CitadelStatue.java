package dev.umbra.citadel;

import dev.umbra.light.LightSensitive;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Citadel statue: a stone figure that cannot move while anyone looks at it. Look away and it is beside you.
 * A flash of light petrifies it for a few seconds.
 */
public class CitadelStatue extends Monster implements LightSensitive {
    public static final double SEE_RANGE = 40.0;
    public static final double VIEW_COS = 0.55;
    private boolean frozen;
    private int stunned;

    public CitadelStatue(EntityType<? extends CitadelStatue> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.42)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public boolean isFrozen() {
        return frozen;
    }

    /** Does any player have it in sight right now? */
    public boolean isWatched(ServerLevel level) {
        Vec3 c = this.getBoundingBox().getCenter();
        for (Player p : level.players()) {
            if (p.isSpectator() || p.distanceToSqr(this) > SEE_RANGE * SEE_RANGE) continue;
            Vec3 eye = p.getEyePosition();
            for (Vec3 point : new Vec3[]{c, this.getEyePosition()}) {
                Vec3 dir = point.subtract(eye);
                double len = dir.length();
                if (len < 1.0E-3) return true;
                if (p.getViewVector(1.0F).dot(dir.scale(1.0 / len)) < VIEW_COS) continue;
                if (level.clip(new ClipContext(eye, point, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS) return true;
            }
        }
        return false;
    }

    /**
     * Decided here and not in customServerAiStep: the game skips the whole AI step of an immobile mob, so a flag that
     * is only recomputed inside it can never be cleared again and the statue would stay stone for good after the
     * first glance (or the first flash).
     */
    @Override
    public void aiStep() {
        if (this.level() instanceof ServerLevel level && this.isAlive()) watch(level);
        super.aiStep();
    }

    private void watch(ServerLevel level) {
        if (stunned > 0) stunned--;
        boolean was = frozen;
        frozen = stunned > 0 || isWatched(level);
        if (frozen) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, Math.min(0, this.getDeltaMovement().y), 0);
        } else if (was && this.getTarget() != null) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.GRINDSTONE_USE, SoundSource.HOSTILE, 0.6F, 0.5F);
        }
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || frozen;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (frozen) return false;
        return super.doHurtTarget(level, target);
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        stunned = 100;
        frozen = true;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.STONE_HIT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.STONE_BREAK;
    }
}
