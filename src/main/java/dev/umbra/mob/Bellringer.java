package dev.umbra.mob;

import dev.umbra.light.LightSensitive;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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

/**
 * 2.6, Звонарь: keeps the bell of a shadow village. When it sees you it rings: heads swim, feet drag, and everything
 * of the dark for twenty blocks round comes to the sound. Every ten seconds while it lives.
 */
public class Bellringer extends Monster implements LightSensitive {
    public static final int RING_EVERY = 200;
    public static final double RING_RADIUS = 12.0, CALL_RADIUS = 24.0;
    private int ringTimer = 60;

    public Bellringer(EntityType<? extends Bellringer> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (ringTimer > 0) ringTimer--;
        LivingEntity t = this.getTarget();
        if (ringTimer == 0 && t != null && t.distanceTo(this) < RING_RADIUS + 2) {
            ring(level, java.util.List.of());
            ringTimer = RING_EVERY;
        }
    }

    /** Rings: players near reel, the dark around comes. Returns how many players were struck. */
    public int ring(ServerLevel level, java.util.List<Player> also) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 3.0F, 0.6F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.2, this.getZ(), 1, 0, 0, 0, 0);
        int n = 0;
        java.util.Set<Player> near = new java.util.LinkedHashSet<>(level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(RING_RADIUS), x -> !x.isSpectator() && !x.isCreative()));
        for (Player p : also) if (p.distanceTo(this) <= RING_RADIUS) near.add(p);
        for (Player p : near) {
            p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0));
            p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
            dev.umbra.sanity.Sanity.add(p, -2.0F);
            for (Mob m : level.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(CALL_RADIUS),
                    x -> x != this && x.isAlive() && dev.umbra.gear.WeaponEvents.dark(x) && x.getTarget() == null)) {
                m.setTarget(p);
            }
            n++;
        }
        return n;
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        ringTimer = Math.max(ringTimer, 100);   // the light chokes the bell for a while
    }
}
