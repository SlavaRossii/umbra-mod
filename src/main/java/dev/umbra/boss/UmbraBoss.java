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

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        // the journal starts working out how to beat it as soon as you face it
        if (this.tickCount % 40 == 9) {
            String id = dev.umbra.journal.Bestiary.ids().get(this.getType());
            if (id != null) for (ServerPlayer p : opponents(level)) dev.umbra.journal.Journal.unlock(p, "weak_" + id);
        }
        bossTick(level);
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
        if (guard > 0 && this.level() instanceof ServerLevel g) {
            guard--;
            // the second wind mends, but never back over the line it has just crossed (or it would cross it again, and again)
            float before = this.getHealth();
            this.heal(this.getMaxHealth() * GUARD_HEAL / GUARD_TICKS);
            if (phase() < lastPhase) this.setHealth(before);
            if (guard % 5 == 0) g.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(), 6, 0.8, 1.0, 0.8, 0.02);
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

    /** How much of a blow lands, from this boss's own rules (its weak point). 1 = all of it. */
    protected float weakness(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        return 1.0F;
    }

    // --- 2.4: a new phase is a second wind; light helps, it does not kill ---------------------------------
    /** Ticks of the shield after a phase change, and how much health comes back during it. */
    public static final int GUARD_TICKS = 80;
    public static final float GUARD_HEAL = 0.25F;
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

    /** Fight phase from health: 1 above half, 2 below. Bosses with more phases override this. */
    public int phase() {
        return this.getHealth() > this.getMaxHealth() * 0.5F ? 1 : 2;
    }

    private int lastPhase = 1;

    /** Call from bossTick: announces when the phase changes. */
    public int trackPhase(ServerLevel level) {
        int p = Math.max(phase(), lastPhase);   // a boss never goes back a phase, and gets each second wind once
        if (p != lastPhase) {
            // a second wind: shielded for a moment while a quarter of its health comes back
            if (p > lastPhase) {
                guard = GUARD_TICKS;
                stun = 0;
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
