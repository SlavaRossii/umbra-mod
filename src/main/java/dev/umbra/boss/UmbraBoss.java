package dev.umbra.boss;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Base for Umbra's minibosses and bosses: a boss bar for nearby players, never despawns, and no knockback.
 * Subclasses implement their mechanic in {@link #bossTick(ServerLevel)}.
 */
public abstract class UmbraBoss extends Monster {
    private final ServerBossEvent bossEvent;

    protected UmbraBoss(EntityType<? extends UmbraBoss> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(java.util.UUID.randomUUID(), this.getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10);
        this.setPersistenceRequired();
        this.xpReward = 50;
    }

    // --- 2.10: what the boss is doing, for its model: an action and the tick it began --------------------------
    /** Nothing in particular. */
    public static final int ACT_NONE = 0;
    /** A blow in reach. */
    public static final int ACT_SWING = 1;
    /** Winding up a throw, a spit, a volley from afar. */
    public static final int ACT_CAST = 2;
    /** Winding up something heavy: a slam, a leap, a shockwave. */
    public static final int ACT_SLAM = 3;
    /** The roar of a new phase. */
    public static final int ACT_ROAR = 4;
    /** Calling its own (shades, acolytes, brood). */
    public static final int ACT_SUMMON = 5;
    /** Gathering a beam. */
    public static final int ACT_BEAM = 6;

    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ACTION =
            net.minecraft.network.syncher.SynchedEntityData.defineId(UmbraBoss.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ACTION_AT =
            net.minecraft.network.syncher.SynchedEntityData.defineId(UmbraBoss.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTION, ACT_NONE);
        builder.define(ACTION_AT, -1000);
    }

    /** Starts an action (its model plays it from now). */
    public void startAction(int action) {
        this.entityData.set(ACTION, action);
        this.entityData.set(ACTION_AT, (int) this.level().getGameTime());   // the world's clock: the same on both sides
    }

    public int action() {
        return this.entityData.get(ACTION);
    }

    /** For the client check's photographs: held at {@code age} ticks into {@code action} (-1: let go). */
    private int heldAction = -1, heldAge;

    public void holdAction(int action, int age) {
        heldAction = age < 0 ? -1 : action;
        heldAge = age;
        if (heldAction >= 0) startAction(action);
    }

    @Override
    public void tick() {
        super.tick();
        if (heldAction >= 0 && !this.level().isClientSide()) {
            this.entityData.set(ACTION, heldAction);
            this.entityData.set(ACTION_AT, (int) this.level().getGameTime() - heldAge);
        }
    }

    /** Ticks since the action began (with the partial tick, for drawing). */
    public float actionAge(float partial) {
        return (int) this.level().getGameTime() - this.entityData.get(ACTION_AT) + partial;
    }

    /** 2.10: what it has thrown and is still flying (every boss may throw; most do, from afar). */
    protected final BossMoves.Bolts bolts = new BossMoves.Bolts();

    public BossMoves.Bolts bolts() {
        return bolts;
    }

    // --- 2.10: near and far -----------------------------------------------------------------------------------
    /**
     * A boss does not walk the length of the arena after someone who stands off: past {@link #rangedFrom()} blocks
     * (or after a few blows in reach) it winds up and strikes from where it is ({@link #rangedAttack}). Bosses that
     * fight this way call {@link #rangedTick} from their {@link #bossTick}.
     */
    private int rangedCooldown = 60, rangedWindup = -1, meleeHits;
    private net.minecraft.world.entity.LivingEntity rangedTarget;

    /** Distance past which it would rather strike from afar. */
    protected double rangedFrom() {
        return 7.0;
    }

    /** Ticks between two attacks from afar. */
    protected int rangedEvery() {
        return phase() == 1 ? 90 : 65;
    }

    /** Ticks it winds up before it lets go (what you have to see it coming). */
    protected int rangedWindupTicks() {
        return 16;
    }

    /** The attack from afar itself (when the wind-up is done). */
    protected void rangedAttack(ServerLevel level, net.minecraft.world.entity.LivingEntity target) {
    }

    /** The sound of the wind-up. */
    protected net.minecraft.sounds.SoundEvent rangedWindupSound() {
        return net.minecraft.sounds.SoundEvents.EVOKER_PREPARE_ATTACK;
    }

    /** Is it winding up a strike from afar right now? */
    public boolean windingUp() {
        return rangedWindup >= 0;
    }

    /**
     * Call each tick from {@link #bossTick}: returns true while it winds up (it stands, turned to its target, and the
     * rest of its tick should wait).
     */
    protected boolean rangedTick(ServerLevel level, net.minecraft.world.entity.LivingEntity target) {
        if (rangedWindup >= 0) {
            this.getNavigation().stop();
            if (rangedTarget != null && rangedTarget.isAlive()) this.getLookControl().setLookAt(rangedTarget, 30.0F, 30.0F);
            if (--rangedWindup < 0) {
                if (rangedTarget != null && rangedTarget.isAlive()) rangedAttack(level, rangedTarget);
                rangedTarget = null;
            }
            return true;
        }
        if (target == null || --rangedCooldown > 0) return false;
        double d = this.distanceTo(target);
        if ((d > rangedFrom() || meleeHits >= 3) && d < 32 && this.hasLineOfSight(target)) {
            rangedTarget = target;
            rangedWindup = rangedWindupTicks();
            rangedCooldown = rangedEvery();
            meleeHits = 0;
            startAction(ACT_CAST);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), rangedWindupSound(), net.minecraft.sounds.SoundSource.HOSTILE, 1.2F, 0.8F);
            return true;
        }
        return false;
    }

    /** Where its throws leave from (a little in front of its eyes). */
    protected net.minecraft.world.phys.Vec3 throwFrom() {
        return this.getEyePosition().add(this.getLookAngle().scale(this.getBbWidth() * 0.6 + 0.4));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        // the journal starts working out how to beat it as soon as you face it
        if (this.tickCount % 40 == 9) {
            String id = dev.umbra.journal.Bestiary.ids().get(this.getType());
            if (id != null) for (ServerPlayer p : opponents(level)) dev.umbra.journal.Journal.unlock(p, "weak_" + id);
        }
        if (pinned != null && pinned.isAlive()) this.setTarget(pinned);
        bossTick(level);
    }

    /** For the smoke test: a target its own eyes would not pick (a fake player), kept through its target goals. */
    private net.minecraft.world.entity.LivingEntity pinned;

    public void pinTarget(net.minecraft.world.entity.LivingEntity target) {
        pinned = target;
        this.setTarget(target);
    }

    // --- 1.8.2: the window after a big move ---------------------------------------------------------------
    /** How long a boss stands winded after a leap, dash, cage or swap (2 seconds), and what it costs it. */
    public static final int STUN_TICKS = 40;
    public static final float STUN_DAMAGE = 1.5F;
    private int stun;

    /** The boss is spent: it does not move or act for a moment and takes +50% damage. */
    public void stun(int ticks) {
        if (stun <= 0 && this.level() instanceof ServerLevel level) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.IRON_GOLEM_DAMAGE, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.5F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + this.getBbHeight(), this.getZ(), 16, 0.4, 0.3, 0.4, 0.05);
        }
        stun = Math.max(stun, ticks);
    }

    public boolean isStunned() {
        return stun > 0;
    }

    /** Housekeeping that must go on while the boss is stunned (delayed bursts, crumbling blocks). */
    protected void stunnedTick(ServerLevel level) {
    }

    @Override
    public void aiStep() {
        if (this.level() instanceof ServerLevel b && bolts.size() > 0) bolts.tick(b, this, opponents(b));
        if (guard > 0 && this.level() instanceof ServerLevel g) {
            guard--;
            // 2.9: the second wind mends a fixed share of its health, and it shows (the phase it has reached stays reached)
            this.heal(this.getMaxHealth() * GUARD_HEAL / GUARD_TICKS);
            if (guard % 5 == 0) g.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(), 6, 0.8, 1.0, 0.8, 0.02);
            if (guard % 10 == 0) g.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 2, 0.6, 0.3, 0.6, 0.0);
            if (guard % 20 == 0) g.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.BEACON_AMBIENT, net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 0.6F);
            bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        }
        if (stun > 0 && this.level() instanceof ServerLevel level) {
            stun--;
            this.getNavigation().stop();
            bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            if (this.tickCount % 4 == 0) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, this.getX(), this.getY() + this.getBbHeight() + 0.2, this.getZ(), 3, 0.3, 0.1, 0.3, 0.02);
            }
            stunnedTick(level);
        }
        super.aiStep();
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || stun > 0;
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (guard > 0 && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(), 8, 0.5, 0.6, 0.5, 0.1);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.SHIELD_BLOCK.value(), net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.6F);
            return false;
        }
        amount *= weakness(level, source);
        return super.hurtServer(level, source, stun > 0 ? amount * STUN_DAMAGE : amount);
    }

    /** 2.9: blows of this boss's that a shield has stopped in a row; the {@link BossMoves#GUARD_HITS}th beats it aside. */
    private int blockedHits;

    @Override
    public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
        boolean shielded = target instanceof net.minecraft.world.entity.player.Player p && !BossMoves.raisedShield(p, this).isEmpty();
        startAction(ACT_SWING);
        boolean hit = super.doHurtTarget(level, target);
        if (hit) meleeHits++;
        if (shielded && target instanceof net.minecraft.world.entity.player.Player p && ++blockedHits >= BossMoves.GUARD_HITS) {
            blockedHits = 0;
            BossMoves.breakGuard(level, p, BossMoves.GUARD_BREAK);
        } else if (!shielded) {
            blockedHits = 0;
        }
        return hit;
    }

    /** How much of a blow lands, from this boss's own rules (its weak point). 1 = all of it. */
    protected float weakness(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        return 1.0F;
    }

    // --- 2.4: a new phase is a second wind; light helps, it does not kill ---------------------------------
    /** Ticks of the shield after a phase change, and how much health comes back during it (2.9: all of it, past the line). */
    public static final int GUARD_TICKS = 80;
    public static final float GUARD_HEAL = 0.15F;
    /** A flash of light never takes more than this share of a boss's health, and stuns it at most this often. */
    public static final float LIGHT_CAP = 0.02F;
    public static final int LIGHT_STUN_COOLDOWN = 200;
    private int guard;
    private long lightStunAt = -100000;

    public boolean isGuarded() {
        return guard > 0;
    }

    /** Light hurts, a little: at most {@link #LIGHT_CAP} of the boss's health per flash. */
    protected void lightHurt(ServerLevel level, net.minecraft.world.entity.player.Player source, float amount) {
        float dmg = Math.min(amount, this.getMaxHealth() * LIGHT_CAP);
        this.hurtServer(level, source != null ? level.damageSources().playerAttack(source) : level.damageSources().magic(), dmg);
    }

    /** Light stuns, but not over and over: once in {@link #LIGHT_STUN_COOLDOWN} ticks. */
    protected boolean lightStun(ServerLevel level) {
        if (level.getGameTime() - lightStunAt < LIGHT_STUN_COOLDOWN) return false;
        lightStunAt = level.getGameTime();
        stun(STUN_TICKS);
        return true;
    }

    // --- 1.8.2: shrouds are chipped away, not waited out -------------------------------------------------
    /** Hits with a light in either hand needed to tear a shroud of living darkness off a boss. */
    public static final int SHROUD_HITS = 3;
    private int shroudHits;
    private long shroudHitAt;

    /**
     * Call when a hit lands on a shrouded boss: a player holding a light (a lantern in the off hand, say) chips the
     * shroud; the third hit within three seconds tears it down. Returns true when it is down and the hit may land.
     */
    protected boolean chipShroud(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        if (!(source.getEntity() instanceof net.minecraft.world.entity.player.Player p) || !dev.umbra.util.UmbraUtil.holdsLight(p)) return false;
        long now = level.getGameTime();
        if (now - shroudHitAt > 60) shroudHits = 0;
        shroudHitAt = now;
        shroudHits++;
        level.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.6F + 0.3F * shroudHits);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(), 8, 0.4, 0.5, 0.4, 0.03);
        if (shroudHits >= SHROUD_HITS) {
            shroudHits = 0;
            return true;
        }
        return false;
    }

    protected abstract void bossTick(ServerLevel level);

    /** Fight phase from health alone: 1 above half, 2 below. Bosses with more phases override this. */
    protected int rawPhase() {
        return this.getHealth() > this.getMaxHealth() * 0.5F ? 1 : 2;
    }

    /** The phase the fight is in: once reached, a phase stays (a second wind that heals past the line does not undo it). */
    public final int phase() {
        return Math.max(rawPhase(), lastPhase);
    }

    private int lastPhase = 1;

    /** Call from bossTick: announces when the phase changes. */
    public int trackPhase(ServerLevel level) {
        int p = Math.max(rawPhase(), lastPhase);   // a boss never goes back a phase, and gets each second wind once
        if (p != lastPhase) {
            // a second wind: shielded for a moment while a quarter of its health comes back
            if (p > lastPhase) {
                guard = GUARD_TICKS;
                stun = 0;
                rangedWindup = -1;
                startAction(ACT_ROAR);
                level.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.5F);
            }
            lastPhase = p;
            BossMoves.phase(level, this, p);
            onPhase(level, p);
        }
        return p;
    }

    protected void onPhase(ServerLevel level, int phase) {
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // --- 2.10: a boss dies slowly: it sinks and comes apart into smoke over two and a half seconds -------------
    public static final int DEATH_TICKS = 50;

    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.level() instanceof ServerLevel level) {
            if (this.deathTime == 1) bolts.clear();
            if (this.deathTime % 3 == 0) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                        4 + this.deathTime / 6, this.getBbWidth() * 0.5, this.getBbHeight() * 0.4, this.getBbWidth() * 0.5, 0.02);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, this.getX(), this.getY() + this.getBbHeight() * 0.3, this.getZ(),
                        2, this.getBbWidth() * 0.4, this.getBbHeight() * 0.3, this.getBbWidth() * 0.4, 0.02);
            }
            if (this.deathTime == DEATH_TICKS - 5)
                level.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.WARDEN_DEATH, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.4F);
            if (this.deathTime >= DEATH_TICKS && !this.isRemoved()) {
                level.broadcastEntityEvent(this, (byte) 60);
                this.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);
            }
        }
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    /** Players this boss is fighting (within 32 blocks, survival). */
    public java.util.List<ServerPlayer> opponents(ServerLevel level) {
        java.util.List<ServerPlayer> list = new java.util.ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && !p.isCreative() && p.distanceTo(this) < 32) list.add(p);
        }
        return list;
    }
}
