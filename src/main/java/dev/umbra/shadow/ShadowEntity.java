package dev.umbra.shadow;

import dev.umbra.light.LightSensitive;
import dev.umbra.registry.ModItems;
import dev.umbra.sanity.Sanity;
import dev.umbra.util.UmbraUtil;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The player's shadow. It has no AI of its own: {@link ShadowManager} walks it along the player's trail.
 * It cannot be hurt by ordinary weapons; hitting it while holding a light source dispels it.
 */
public class ShadowEntity extends Monster implements LightSensitive {
    private boolean managed;
    private boolean ally;

    public ShadowEntity(EntityType<? extends ShadowEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setSilent(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    void markManaged() {
        this.managed = true;
    }

    /** A freed shadow: fights for its player, cannot be hurt or dispelled. */
    void makeAlly() {
        this.ally = true;
    }

    public boolean isAlly() {
        return ally;
    }

    @Override
    protected void registerGoals() {
        // no goals: movement is driven by ShadowManager
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        this.setNoGravity(true);
        // A shadow that lost its owner (e.g. after a reload) simply fades away.
        if (this.level() instanceof ServerLevel && !this.managed && this.tickCount > 5) {
            this.dissipate();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (ally) return false;
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player && UmbraUtil.holdsLight(player)) {
            onLit(level, player);
            return true;
        }
        return false;
    }

    /** Light dispels the shadow and leaves a shard of it behind. */
    @Override
    public void onLit(ServerLevel level, Player source) {
        if (!this.isAlive() || ally) return;
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.0F, 0.6F);
        level.addFreshEntity(new ItemEntity(level, this.getX(), this.getY() + 1.0, this.getZ(), new ItemStack(ModItems.SHADOW_SHARD.get())));
        if (source != null) Sanity.add(source, Sanity.Shock.SHADOW_DISPELLED);
        if (source instanceof net.minecraft.server.level.ServerPlayer sp) dev.umbra.journal.Journal.unlock(sp, "shadow_dispelled");
        this.dissipate();
    }

    public void dissipate() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 30, 0.3, 0.6, 0.3, 0.02);
        }
        this.discard();
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
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }
}
