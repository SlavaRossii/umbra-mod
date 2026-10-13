package dev.umbra.boss;

import dev.umbra.light.LightSensitive;
import dev.umbra.quantum.MazeController;
import dev.umbra.quantum.MazePatterns;
import dev.umbra.registry.ModEntities;
import dev.umbra.sanity.Hallucinations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Архитектор: master of the quantum ruins. Fights inside his own maze, which he rearranges every few seconds;
 * he steps through walls to appear beside you and calls shades from the cracks.
 */
public class Architect extends UmbraBoss implements LightSensitive {
    private int stepTimer;
    private int patternTimer;
    private int shadeTimer;
    private int stunned;

    public Architect(EntityType<? extends Architect> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.SCALE, 1.3);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void bossTick(ServerLevel level) {
        if (stunned > 0) {
            stunned--;
            this.getNavigation().stop();
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null) return;

        if (++stepTimer >= 100) { // step through the walls to your side
            stepTimer = 0;
            BlockPos spot = Hallucinations.findStandable(level, target.blockPosition().offset(this.random.nextInt(7) - 3, 0, this.random.nextInt(7) - 3), 3);
            if (spot != null && spot.distSqr(target.blockPosition()) > 2) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1, this.getZ(), 30, 0.4, 1, 0.4, 0.05);
                this.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
                level.playSound(null, spot, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.6F);
            }
        }
        if (++patternTimer >= 200) { // reshape the maze
            patternTimer = 0;
            var all = MazePatterns.ALL;
            MazeController.force(level, this.blockPosition(), all.get(this.random.nextInt(all.size())).id());
        }
        if (++shadeTimer >= 240) {
            shadeTimer = 0;
            for (int i = 0; i < 2; i++) {
                var shade = ModEntities.SHADE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                if (shade == null) continue;
                shade.snapTo(this.getX(), this.getY(), this.getZ(), 0, 0);
                level.addFreshEntity(shade);
                shade.setTarget(target);
            }
        }
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        stunned = 40;
        this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 3));
        this.hurtServer(level, source != null ? level.damageSources().playerAttack(source) : level.damageSources().magic(), 8.0F);
    }
}
