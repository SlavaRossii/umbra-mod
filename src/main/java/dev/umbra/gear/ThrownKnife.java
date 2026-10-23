package dev.umbra.gear;

import dev.umbra.registry.ModEntities;
import dev.umbra.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 2.9: a thrown shadow knife, a thing of its own and not a snowball with a knife's picture: it flies point first,
 * turning slowly about its blade, and when it misses it sticks in the wall for a couple of seconds before it drops
 * (half the time it can be picked up again). Damage as before: {@link RangedWeapons#KNIFE_DAMAGE}, twice from behind.
 */
public class ThrownKnife extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> STUCK = SynchedEntityData.defineId(ThrownKnife.class, EntityDataSerializers.BOOLEAN);
    /** Ticks a knife stays in the wall it hit. */
    public static final int STICK_TICKS = 50;
    private int stuckTime;

    public ThrownKnife(EntityType<? extends ThrownKnife> type, Level level) {
        super(type, level);
    }

    public ThrownKnife(Level level, LivingEntity owner, ItemStack stack) {
        super(ModEntities.THROWN_KNIFE.get(), owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SHADOW_KNIFE.get();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STUCK, false);
    }

    public boolean isStuck() {
        return this.entityData.get(STUCK);
    }

    /** A knife falls less than a snowball: it is thrown hard and flies flat. */
    @Override
    protected double getDefaultGravity() {
        return 0.015;
    }

    @Override
    public void tick() {
        if (isStuck()) {
            // in the wall: it does not move or turn any more, and in a while it drops out
            this.xo = this.getX();
            this.yo = this.getY();
            this.zo = this.getZ();
            this.xRotO = this.getXRot();
            this.yRotO = this.getYRot();
            this.setDeltaMovement(Vec3.ZERO);
            if (this.level() instanceof ServerLevel level && ++stuckTime > STICK_TICKS) {
                drop(level, this.position());
                this.discard();
            }
            return;
        }
        super.tick();
        if (this.level().isClientSide() && this.tickCount % 2 == 0)
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(this.level() instanceof ServerLevel level)) return;
        if (result.getEntity() instanceof LivingEntity target && this.getOwner() instanceof LivingEntity owner && target != owner) {
            float dmg = RangedWeapons.KNIFE_DAMAGE;
            if (WeaponEvents.behind(target, owner)) {
                dmg *= 2.0F;
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
            }
            target.hurtServer(level, level.damageSources().thrown(this, owner), dmg);
            level.sendParticles(ParticleTypes.SQUID_INK, result.getLocation().x, result.getLocation().y, result.getLocation().z, 3, 0.1, 0.1, 0.1, 0.01);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.7F, 1.4F);
        drop(level, result.getLocation());
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(this.level() instanceof ServerLevel level)) return;
        // the point goes a little way into the wall, and there it stays
        Vec3 d = this.getDeltaMovement();
        Vec3 at = result.getLocation().subtract(d.lengthSqr() > 1.0E-6 ? d.normalize().scale(-0.12) : Vec3.ZERO);
        this.setPos(at.x, at.y, at.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        this.entityData.set(STUCK, true);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 0.8F, 1.3F);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 4, 0.05, 0.05, 0.05, 0.1);
    }

    @Override
    protected void onHit(HitResult result) {
        // no snowball's end: entity hits and wall hits are handled above, and a knife in a wall stays a while
        super.onHit(result);
    }

    /** Half of the knives can be picked up again (never in creative: it did not cost anything). */
    private void drop(ServerLevel level, Vec3 at) {
        if (this.getOwner() instanceof Player p && p.isCreative()) return;
        if (level.getRandom().nextBoolean())
            level.addFreshEntity(new ItemEntity(level, at.x, at.y + 0.1, at.z, new ItemStack(ModItems.SHADOW_KNIFE.get())));
    }

    // ------------------------------------------------------------------ for the screenshots of the client check

    /** A knife hanging in the air, flying nowhere (yaw and pitch as a projectile counts them). */
    public static ThrownKnife display(ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        ThrownKnife k = new ThrownKnife(ModEntities.THROWN_KNIFE.get(), level);
        k.setPos(x, y, z);
        k.setNoGravity(true);
        k.setYRot(yaw);
        k.setXRot(pitch);
        k.yRotO = yaw;
        k.xRotO = pitch;
        k.entityData.set(STUCK, true);
        k.stuckTime = -100000;
        return k;
    }
}
