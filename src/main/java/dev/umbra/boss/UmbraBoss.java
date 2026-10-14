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
        bossTick(level);
    }

    protected abstract void bossTick(ServerLevel level);

    /** Fight phase from health: 1 above half, 2 below. Bosses with more phases override this. */
    public int phase() {
        return this.getHealth() > this.getMaxHealth() * 0.5F ? 1 : 2;
    }

    private int lastPhase = 1;

    /** Call from bossTick: announces when the phase changes. */
    protected int trackPhase(ServerLevel level) {
        int p = phase();
        if (p != lastPhase) {
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
