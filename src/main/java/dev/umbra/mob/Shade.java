package dev.umbra.mob;

import dev.umbra.light.LightSensitive;
import dev.umbra.util.UmbraUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Тень (стая): small, quick, weak — but never alone. Bright light burns them; the Dispeller destroys them outright. */
public class Shade extends Monster implements LightSensitive {
    public Shade(EntityType<? extends Shade> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.ATTACK_DAMAGE, 2.5)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.SCALE, 0.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(Shade.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel level && this.tickCount % 10 == 5) {
            // a light held in the hand scorches them: 1.8.2, within three blocks
            for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(3.0), pl -> !pl.isSpectator() && UmbraUtil.holdsLight(pl))) {
                level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 4, 0.2, 0.3, 0.2, 0.01);
                this.hurtServer(level, level.damageSources().playerAttack(p), 2.0F);
                break;
            }
        }
        if (this.level() instanceof ServerLevel level && this.tickCount % 20 == 0) {
            BlockPos head = BlockPos.containing(this.getEyePosition());
            if (UmbraUtil.effectiveLight(level, head) >= 11) {
                level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 3, 0.2, 0.3, 0.2, 0.01);
                this.hurtServer(level, level.damageSources().magic(), 1.0F);
            }
        }
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 10, 0.2, 0.3, 0.2, 0.02);
        this.hurtServer(level, source != null ? level.damageSources().playerAttack(source) : level.damageSources().magic(), 100.0F);
    }
}
