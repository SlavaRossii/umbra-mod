package dev.umbra.decor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Where you died, you are still standing. A dark copy watches you come back for your things,
 * turning its head after you. Come close and it is gone, with a whisper. Harmless.
 */
public class DeathDouble extends PathfinderMob {
    private static final int LIFETIME = 20 * 60 * 20;

    public DeathDouble(EntityType<? extends DeathDouble> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
    }

    public static final String OWNER_TAG = "umbra_double_of:";

    /** Whose death this is, or null for doubles from older saves. */
    public java.util.UUID owner() {
        for (String t : entityTags()) {
            if (t.startsWith(OWNER_TAG)) {
                try {
                    return java.util.UUID.fromString(t.substring(OWNER_TAG.length()));
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }

    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        // only one per player: the newest death (on a server, other players keep theirs)
        String tag = OWNER_TAG + player.getUUID();
        for (DeathDouble old : level.getEntitiesOfClass(DeathDouble.class, new AABB(player.blockPosition()).inflate(256), d -> d.entityTags().contains(tag))) old.discard();
        DeathDouble d = dev.umbra.registry.ModEntities.DEATH_DOUBLE.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (d == null) return;
        d.addTag(tag); // entity tags are saved with the entity: the double remembers whose it is
        d.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
        level.addFreshEntity(d);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) return;
        if (this.tickCount > LIFETIME) {
            this.discard();
            return;
        }
        // it watches the one who died here; others can only see it standing
        java.util.UUID owner = owner();
        Player p = owner != null ? level.getPlayerByUUID(owner) : level.getNearestPlayer(this, 48);
        if (p == null || !p.isAlive() || p.level() != level || this.distanceToSqr(p) > 48 * 48) return;
        Vec3 face = p.position().subtract(this.position());
        float yaw = (float) (Math.toDegrees(Math.atan2(face.z, face.x)) - 90.0);
        this.setYHeadRot(yaw);
        if (this.tickCount > 100 && this.distanceTo(p) < 3.0) {
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 20, 0.2, 0.6, 0.2, 0.01);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.AMBIENT, 1.0F, 0.6F);
            p.sendOverlayMessage(Component.translatable("message.umbra.death_double").withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC));
            this.discard();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 20, 0.2, 0.6, 0.2, 0.01);
        this.discard();
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
