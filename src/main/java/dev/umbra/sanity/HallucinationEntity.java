package dev.umbra.sanity;

import dev.umbra.light.LightSensitive;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * A figure only a frightened mind sees. It stands still and watches you, and dissolves when you
 * look straight at it, get close, hit it, or light it up.
 */
public class HallucinationEntity extends Monster implements LightSensitive {
    private static final int MAX_AGE = 20 * 30;
    private UUID owner;
    private int stared;

    public HallucinationEntity(EntityType<? extends HallucinationEntity> type, Level level) {
        super(type, level);
        this.setSilent(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    public void setOwner(Player player) {
        this.owner = player.getUUID();
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) return;
        Player player = owner == null ? null : level.getPlayerByUUID(owner);
        if (player == null || this.tickCount > MAX_AGE || this.distanceTo(player) < 8.0) {
            vanish();
            return;
        }
        // keep watching the player
        Vec3 face = player.position().subtract(this.position());
        float yaw = (float) (Math.toDegrees(Math.atan2(face.z, face.x)) - 90.0);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
        this.setYRot(yaw);
        // looked at directly for half a second -> gone
        Vec3 look = player.getViewVector(1.0F);
        Vec3 to = this.getEyePosition().subtract(player.getEyePosition()).normalize();
        if (look.dot(to) > 0.96 && player.hasLineOfSight(this)) {
            if (++stared > 10) vanish();
        } else {
            stared = 0;
        }
    }

    public void vanish() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 15, 0.2, 0.6, 0.2, 0.01);
        }
        this.discard();
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        vanish();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        vanish();
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }
}
