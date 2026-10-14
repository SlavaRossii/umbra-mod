package dev.umbra.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A figure of a person, made of ash. It never attacks. But it is not always where it was:
 * while nobody looks at it, now and then it stands a little closer. A blow turns it back into dust.
 */
public class AshFigure extends PathfinderMob {
    private int timer;

    public AshFigure(EntityType<? extends AshFigure> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) return;
        if (++timer < 100 + this.random.nextInt(200)) return;
        timer = 0;
        Player p = level.getNearestPlayer(this, 32);
        if (p == null || p.isCreative() || this.distanceTo(p) < 5 || watched(level, p)) return;
        Vec3 step = p.position().subtract(this.position()).normalize().scale(2 + this.random.nextInt(3));
        BlockPos to = dev.umbra.sanity.Hallucinations.findStandable(level, BlockPos.containing(this.position().add(step)), 3);
        if (to == null || to.distSqr(p.blockPosition()) < 16) return;
        Vec3 face = p.position().subtract(Vec3.atBottomCenterOf(to));
        float yaw = (float) (Math.toDegrees(Math.atan2(face.z, face.x)) - 90.0);
        this.snapTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, yaw, 0);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
    }

    private boolean watched(ServerLevel level, Player p) {
        Vec3 eye = p.getEyePosition();
        Vec3 c = this.getBoundingBox().getCenter();
        Vec3 dir = c.subtract(eye).normalize();
        return p.getViewVector(1.0F).dot(dir) > 0.4
                && level.clip(new ClipContext(eye, c, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        level.sendParticles(ParticleTypes.ASH, getX(), getY() + 1, getZ(), 60, 0.3, 0.8, 0.3, 0.02);
        level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 10, 0.3, 0.8, 0.3, 0.01);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
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
