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
    // 2.7.7: it weeps at you from afar
    private final BossMoves.Tears tears = new BossMoves.Tears();
    private int tearTimer;
    private int cry = -1;
    /** Ticks the tears take to well up before they fly (the lids tremble, then squeeze). */
    public static final int CRY_WINDUP = 15;
    public static final byte EVENT_CRY = 90, EVENT_LIT = 91, EVENT_CRY_STOP = 92;
    /**
     * 2.10: it fights from afar and keeps you there: nearer than {@link #NEAR} it drifts back (no faster than you walk),
     * further than {@link #FAR} it comes after you. It has no bite any more. Its attacks: tears; its gaze gathered into
     * a beam ({@link #BEAM_CHARGE} ticks to gather — get behind something — and then it is spent for
     * {@link #BEAM_TIRED} ticks); in its second phase a rain of tears where you stand. While it weeps, gathers its
     * gaze or is spent its eye is open and every blow lands whole (an arrow too).
     */
    public static final double NEAR = 7.0, FAR = 15.0;
    public static final int BEAM_CHARGE = 30, BEAM_TIRED = 40;
    private int beamTimer, beam = -1, rainTimer;
    private final BossMoves.Bursts rain = new BossMoves.Bursts();

    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> WATCHED =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Watcher.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    // client side only: what its model needs to know
    /** Its own clock, which stops while nobody looks at it. */
    public float clientClock;
    public float clientLastAge = -1.0F;
    public int cryAt = -1000, litAt = -1000;

    public Watcher(EntityType<? extends Watcher> type, Level level) {
        super(type, level, BossEvent.BossBarColor.WHITE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.SCALE, 1.5);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WATCHED, false);
    }

    /** Is someone looking at it (as the server last saw it)? */
    public boolean watchedNow() {
        return this.entityData.get(WATCHED);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_CRY) cryAt = this.tickCount;
        else if (id == EVENT_CRY_STOP) cryAt = -1000;
        else if (id == EVENT_LIT) litAt = this.tickCount;
        else super.handleEntityEvent(id);
    }

    public BossMoves.Tears tears() {
        return tears;
    }

    @Override
    protected void stunnedTick(ServerLevel level) {
        tears.tick(level, this, opponents(level), tearDamage());
        rain.tick(level, this, 5.0F);
    }

    /** Is its eye open wide (weeping, gathering its gaze, spent)? Then every blow lands whole. */
    public boolean eyeOpen() {
        return cry >= 0 || beam >= 0 || isStunned();
    }

    public boolean gathering() {
        return beam >= 0;
    }

    private float tearDamage() {
        return phase() == 1 ? 4.0F : 5.0F;
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
        tears.tick(level, this, opponents(level), tearDamage());
        rain.tick(level, this, 5.0F);
        ServerPlayer target = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer p : opponents(level)) {
            double d = p.distanceTo(this);
            if (d < best) {
                best = d;
                target = p;
            }
        }
        boolean watched = target != null && isWatched(level);
        this.entityData.set(WATCHED, watched);
        if (target == null) return;
        this.setTarget(target);
        int phase = trackPhase(level);
        mirrorTick(level);

        if (watched) {
            unseenTicks = 0;
            // the tears well up while it stares, standing still; then they fly
            if (cry >= 0) {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (++cry >= CRY_WINDUP) {
                    cry = -1;
                    Vec3 from = this.getEyePosition().add(this.getLookAngle().scale(1.2)).add(0, -0.3, 0);
                    tears.shoot(level, from, target.getEyePosition().add(0, -0.4, 0), phase == 1 ? 3 : 5, phase == 1 ? 16.0F : 34.0F, 0.85);
                }
                return;
            }
            // its gaze, gathered into a beam: get out of its sight before it is done
            if (beam >= 0) {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                Vec3 eye = this.getEyePosition();
                if (++beam % 3 == 0) {
                    Vec3 d = target.getEyePosition().subtract(eye);
                    for (int i = 1; i < 10; i++) {
                        Vec3 q = eye.add(d.scale(i / 10.0));
                        level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(0xC81E1E, 0.6F + beam / (float) BEAM_CHARGE),
                                q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0.0);
                    }
                }
                if (beam >= BEAM_CHARGE) {
                    beam = -1;
                    fireBeam(level, target, phase);
                }
                return;
            }
            // it keeps you at a distance: it drifts back from you (no faster than you walk) and follows from afar
            if (best < NEAR) {
                Vec3 away = this.position().subtract(target.position()).multiply(1, 0, 1);
                if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
                Vec3 to = this.position().add(away.normalize().scale(6.0));
                this.getNavigation().moveTo(to.x, to.y, to.z, 0.8);
            } else if (best > FAR) {
                this.getNavigation().moveTo(target, 0.85);
            } else {
                this.getNavigation().stop();
            }
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (++tearTimer >= (phase == 1 ? 70 : 50) && best > 3 && best < 26 && this.hasLineOfSight(target)) {
                tearTimer = 0;
                cry = 0;
                startAction(ACT_CAST);
                level.broadcastEntityEvent(this, EVENT_CRY);
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.DROWNED_HURT_WATER, SoundSource.HOSTILE, 1.2F, 0.5F);
                return;
            }
            if (++beamTimer >= (phase == 1 ? 200 : 140) && best < 26 && this.hasLineOfSight(target)) {
                beamTimer = 0;
                beam = 0;
                startAction(ACT_BEAM);
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 1.5F, 1.2F);
                return;
            }
            // phase 2: a rain of tears where you stand (and a little round it)
            if (phase == 2 && ++rainTimer >= 180) {
                rainTimer = 0;
                startAction(ACT_SUMMON);
                for (ServerPlayer p : opponents(level)) {
                    net.minecraft.core.BlockPos c = p.blockPosition();
                    rain.mark(level, c, 30);
                    for (int i = 0; i < 4; i++) rain.mark(level, c.offset(this.random.nextInt(5) - 2, 0, this.random.nextInt(5) - 2), 30 + i * 4);
                }
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.HOSTILE, 1.5F, 0.6F);
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
            if (cry >= 0) {
                // you looked away: the tears dry
                cry = -1;
                level.broadcastEntityEvent(this, EVENT_CRY_STOP);
            }
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

    /** The gathered gaze lets go: whoever still stands in its sight is struck; and then it is spent. */
    private void fireBeam(ServerLevel level, ServerPlayer target, int phase) {
        Vec3 eye = this.getEyePosition();
        Vec3 end = target.getEyePosition();
        boolean seen = this.hasLineOfSight(target) && eye.distanceTo(end) < 28;
        Vec3 d = end.subtract(eye);
        for (int i = 1; i <= 16; i++) {
            Vec3 q = eye.add(d.scale(i / 16.0));
            level.sendParticles(ParticleTypes.SONIC_BOOM, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            if (!seen && i > 4) break;
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.5F, 1.3F);
        if (seen) {
            target.hurtServer(level, level.damageSources().indirectMagic(this, this), phase == 1 ? 7.0F : 9.0F);
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
            Sanity.add(target, -3.0F);
        }
        stun(BEAM_TIRED);
    }

    /** Ticks someone has been holding a mirror up to the eye. */
    private int mirrored;
    /** 2.10: a little softer than before (0.4): a bow has to be able to finish it. */
    public static final float ARMOURED_EYE = 0.6F;

    @Override
    protected float weakness(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        return eyeOpen() ? 1.0F : ARMOURED_EYE;
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
        level.broadcastEntityEvent(this, EVENT_LIT);
        this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
        lightHurt(level, source, 6.0F);
    }
}
