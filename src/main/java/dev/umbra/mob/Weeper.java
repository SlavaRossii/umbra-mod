package dev.umbra.mob;

import dev.umbra.light.LightSensitive;
import dev.umbra.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 2.6, Плакальщица: keeps to the dark at a distance and throws shards of shadow that blind for a moment. Come close
 * and she backs away; flash a light at her and she flees for good.
 */
public class Weeper extends Monster implements RangedAttackMob, LightSensitive {
    public static final float SHARD_DAMAGE = 4.0F;

    public Weeper(EntityType<? extends Weeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 5.0F, 1.1, 1.3));
        this.goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0, 40, 70, 16.0F));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!(this.level() instanceof ServerLevel level)) return;
        Snowball shard = new Snowball(level, this, new ItemStack(ModItems.SHADOW_SHARD.get()));
        double dx = target.getX() - this.getX(), dz = target.getZ() - this.getZ();
        double dy = target.getEyeY() - 0.6 - shard.getY();
        shard.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.15, dz, 1.3F, 6.0F);
        level.addFreshEntity(shard);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ALLAY_HURT, SoundSource.HOSTILE, 0.8F, 0.4F);
    }

    /** A shard thrown by a Weeper (see {@link #onShard}). */
    public static boolean isShard(net.minecraft.world.entity.Entity e) {
        return e instanceof Snowball s && s.getOwner() instanceof Weeper && s.getItem().is(ModItems.SHADOW_SHARD.get());
    }

    public static void onShard(net.neoforged.neoforge.event.entity.ProjectileImpactEvent event) {
        if (!isShard(event.getProjectile()) || !(event.getProjectile().level() instanceof ServerLevel level)) return;
        if (event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit && hit.getEntity() instanceof LivingEntity victim
                && !(victim instanceof Weeper)) {
            Snowball s = (Snowball) event.getProjectile();
            victim.hurtServer(level, level.damageSources().thrown(s, s.getOwner()), SHARD_DAMAGE);
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
            if (victim instanceof Player p) dev.umbra.sanity.Sanity.add(p, -1.0F);
        }
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        this.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 1));
        this.setTarget(null);
        this.getNavigation().stop();
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ALLAY_DEATH, SoundSource.HOSTILE, 1.0F, 0.5F);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return dev.umbra.registry.ModSounds.WHISPER.value();
    }
}
