package dev.umbra.boss;

import dev.umbra.light.LightSensitive;
import dev.umbra.sanity.Sanity;
import dev.umbra.util.UmbraUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Смотрящий: it moves only while you look at it. Look away and it freezes — but ignore it for too long
 * and it steps right in front of your eyes.
 */
public class Watcher extends UmbraBoss implements LightSensitive {
    private int unseenTicks;
    private int attackCooldown;
    private int fangTimer;
    private int gaze;

    public Watcher(EntityType<? extends Watcher> type, Level level) {
        super(type, level, BossEvent.BossBarColor.WHITE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.MOVEMENT_SPEED, 0.42)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.SCALE, 1.5);
    }

    @Override
    protected void registerGoals() {
    }

    /** True if any opponent is looking at us. */
    public boolean isWatched(ServerLevel level) {
        for (ServerPlayer p : opponents(level)) {
            Vec3 to = this.getEyePosition().subtract(p.getEyePosition()).normalize();
            if (p.getViewVector(1.0F).dot(to) > 0.6 && p.hasLineOfSight(this)) return true;
        }
        return false;
    }

    @Override
    protected void bossTick(ServerLevel level) {
        if (attackCooldown > 0) attackCooldown--;
        ServerPlayer target = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer p : opponents(level)) {
            double d = p.distanceTo(this);
            if (d < best) {
                best = d;
                target = p;
            }
        }
        if (target == null) return;
        this.setTarget(target);
        int phase = trackPhase(level);
        mirrorTick(level);

        if (isWatched(level)) {
            unseenTicks = 0;
            this.getNavigation().moveTo(target, 1.4);
            if (best < 2.6 && attackCooldown == 0) {
                this.doHurtTarget(level, target);
                attackCooldown = phase == 1 ? 20 : 12;
            }
            // jaws snap out of the floor along its gaze
            if (++fangTimer >= (phase == 1 ? 120 : 70) && best > 3 && best < 18) {
                fangTimer = 0;
                BossMoves.fangsLine(level, this, target, 14);
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.0F, 0.6F);
            }
            // phase 2: staring back at it too long hurts the mind
            if (phase == 2 && ++gaze % 40 == 0) {
                for (ServerPlayer p : opponents(level)) {
                    Vec3 to = this.getEyePosition().subtract(p.getEyePosition()).normalize();
                    if (p.getViewVector(1.0F).dot(to) > 0.9) {
                        Sanity.add(p, -1.5F);
                        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 0));
                    }
                }
            }
        } else {
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            if (++unseenTicks > (phase == 1 ? 20 * 8 : 20 * 5)) {
                // you cannot ignore it forever
                Vec3 ahead = UmbraUtil.horizontal(target.getLookAngle()).scale(6.0).add(target.position());
                level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1, this.getZ(), 20, 0.3, 1, 0.3, 0.02);
                this.teleportTo(ahead.x, target.getY(), ahead.z);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.0F, 0.5F);
                Sanity.add(target, -4.0F);
                if (phase == 2) BossMoves.fangsRing(level, this, target.position(), 2.5, 10, 25);
                unseenTicks = 0;
            }
        }
    }

    /** Ticks someone has been holding a mirror up to the eye. */
    private int mirrored;
    public static final float ARMOURED_EYE = 0.4F;

    @Override
    protected float weakness(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        return isStunned() ? 1.0F : ARMOURED_EYE;
    }

    /** A mirror held up to the eye: its own stare comes back at it and it reels. */
    private void mirrorTick(ServerLevel level) {
        boolean any = false;
        for (ServerPlayer p : opponents(level)) {
            boolean holds = p.getMainHandItem().is(dev.umbra.registry.ModItems.MIRROR.get()) || p.getOffhandItem().is(dev.umbra.registry.ModItems.MIRROR.get());
            Vec3 to = this.getEyePosition().subtract(p.getEyePosition()).normalize();
            if (holds && p.getViewVector(1.0F).dot(to) > 0.85 && p.hasLineOfSight(this)) any = true;
        }
        mirrored = any ? mirrored + 1 : 0;
        if (mirrored >= 40) {
            mirrored = -60;
            stun(STUN_TICKS + 40);
            level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getEyeY(), this.getZ(), 40, 0.6, 0.6, 0.6, 0.15);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.0F, 0.5F);
        }
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
        lightHurt(level, source, 6.0F);
    }
}
