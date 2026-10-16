package dev.umbra.sanity;

import dev.umbra.light.LightSensitive;
import dev.umbra.util.UmbraUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Phantoms: familiar mobs that are not there. What they do depends on how far gone the mind is.
 * <ul>
 *   <li>{@link Mode#WATCH} (sanity &lt; 45): stands far away and stares; gone when looked at or approached.</li>
 *   <li>{@link Mode#AMBUSH} (sanity &lt; 30): appears right in front of you and rushes at your face, then vanishes.</li>
 *   <li>{@link Mode#SHADOW} (sanity &lt; 15): a shadow version that really attacks (weakly); light dissolves it.</li>
 * </ul>
 * All of them flicker, and none of them drop anything.
 */
public class PhantomEntity extends Monster implements LightSensitive {
    public enum Mode { WATCH, AMBUSH, SHADOW }

    private Mode mode = Mode.WATCH;
    private UUID owner;
    private int stared;
    private int flicker;

    public PhantomEntity(EntityType<? extends PhantomEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public void setup(Mode mode, Player owner) {
        this.mode = mode;
        this.owner = owner.getUUID();
        this.setSilent(mode != Mode.SHADOW);
        if (mode == Mode.SHADOW) {
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
            this.setTarget(owner);
        }
    }

    public Mode mode() {
        return mode;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) return;
        Player player = owner == null ? null : level.getPlayerByUUID(owner);
        int maxAge = mode == Mode.WATCH ? 20 * 30 : mode == Mode.AMBUSH ? 30 : 20 * 40;
        if (player == null || player.level() != level || this.tickCount > maxAge) {
            vanish(level);
            return;
        }
        // flicker
        if (flicker > 0 && --flicker == 0) this.setInvisible(false);
        else if (flicker == 0 && this.random.nextInt(40) == 0) {
            this.setInvisible(true);
            flicker = 2 + this.random.nextInt(4);
        }
        switch (mode) {
            case WATCH -> watch(level, player);
            case AMBUSH -> ambush(level, player);
            case SHADOW -> {
                this.setTarget(player);
                if (UmbraUtil.effectiveLight(level, this.blockPosition()) >= 11) vanish(level);
            }
        }
    }

    private void face(Player player) {
        Vec3 d = player.position().subtract(this.position());
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
    }

    private void watch(ServerLevel level, Player player) {
        face(player);
        if (this.distanceTo(player) < 10.0) {
            vanish(level);
            return;
        }
        Vec3 look = player.getViewVector(1.0F);
        Vec3 to = this.getEyePosition().subtract(player.getEyePosition()).normalize();
        if (look.dot(to) > 0.95 && player.hasLineOfSight(this)) {
            if (++stared > 12) vanish(level);
        } else {
            stared = 0;
        }
    }

    private void ambush(ServerLevel level, Player player) {
        face(player);
        if (this.tickCount == 1) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.2F, 0.6F + this.random.nextFloat() * 0.3F);
            Sanity.add(player, -2.0F);
        }
        Vec3 d = player.getEyePosition().subtract(this.getEyePosition());
        if (d.length() < 1.6) {
            vanish(level);
            return;
        }
        Vec3 v = d.normalize().scale(0.55);
        this.setDeltaMovement(v.x, Math.max(-0.2, v.y), v.z);
        this.noPhysics = true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (mode != Mode.SHADOW) {
            vanish(level);
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        return mode == Mode.SHADOW && super.doHurtTarget(level, target);
    }

    @Override
    public boolean isPushable() {
        return mode == Mode.SHADOW;
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
    }

    public void vanish(ServerLevel level) {
        level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0, getZ(), 15, 0.2, 0.6, 0.2, 0.01);
        this.discard();
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        vanish(level);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }
}
