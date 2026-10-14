package dev.umbra.citadel;

import dev.umbra.light.LightSensitive;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Keeper of the citadel: slow, armoured, hits hard, and now and then lunges across the room.
 * Light dazes it: slowed and weakened for a few seconds.
 */
public class CitadelKeeper extends Monster implements LightSensitive {
    private int lunge;

    public CitadelKeeper(EntityType<? extends CitadelKeeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.SCALE, 1.2);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        LivingEntity target = this.getTarget();
        if (target == null || this.hasEffect(MobEffects.SLOWNESS)) return;
        if (++lunge >= 120 && this.distanceTo(target) > 5 && this.distanceTo(target) < 14 && this.hasLineOfSight(target)) {
            lunge = 0;
            Vec3 d = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(d.x * 1.4, 0.45, d.z * 1.4);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.8F, 1.3F);
        }
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 3));
        this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
        this.hurtServer(level, source != null ? level.damageSources().playerAttack(source) : level.damageSources().magic(), 4.0F);
    }
}
