package dev.umbra.boss;

import dev.umbra.light.LightSensitive;
import dev.umbra.mimic.MimicManager;
import dev.umbra.mob.Mimicker;
import dev.umbra.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Королева подмен: every so often she vanishes among her brood — mimickers and mimic blocks.
 * While hidden she cannot be hurt. Kill the brood or light her up to drag her out.
 */
public class MimicQueen extends UmbraBoss implements LightSensitive {
    private static final int CYCLE = 20 * 15;
    private static final int HIDE_MAX = 20 * 10;
    private int timer;
    private int hidden;
    private final List<UUID> brood = new ArrayList<>();
    private final BossMoves.Bursts spit = new BossMoves.Bursts();
    private int leapTimer;
    private boolean leaping;
    private int spitTimer;

    public MimicQueen(EntityType<? extends MimicQueen> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.SCALE, 1.3);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean isDisguised() {
        return hidden > 0;
    }

    @Override
    protected void bossTick(ServerLevel level) {
        int phase = trackPhase(level);
        spit.tick(level, this, 6.0F);
        if (isDisguised()) {
            hidden++;
            this.getNavigation().stop();
            brood.removeIf(id -> {
                Entity e = level.getEntity(id);
                return e == null || !e.isAlive();
            });
            if (brood.isEmpty() || hidden > HIDE_MAX) reveal(level);
            return;
        }
        LivingEntity target = this.getTarget();
        // leap and bite: lands with a slam
        if (leaping && this.onGround() && leapTimer > 5) {
            leaping = false;
            BossMoves.shockwave(level, this, 3.0, 7.0F, 0.9);
        }
        leapTimer++;
        if (!leaping && target != null && leapTimer >= (phase == 1 ? 100 : 60)) {
            double d = this.distanceTo(target);
            if (d > 4 && d < 14) {
                leapTimer = 0;
                leaping = true;
                BossMoves.leap(this, target, Math.min(1.4, d / 8));
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0F, 1.5F);
            }
        }
        // phase 2: spits mimic bile — the floor under you bursts a second later
        if (phase == 2 && target != null && ++spitTimer >= 70) {
            spitTimer = 0;
            for (ServerPlayer p : opponents(level)) spit.mark(level, p.blockPosition(), 25);
        }
        if (++timer >= (phase == 1 ? CYCLE : CYCLE * 2 / 3) && target != null) {
            timer = 0;
            disguise(level);
        }
    }

    private void disguise(ServerLevel level) {
        hidden = 1;
        this.setInvisible(true);
        level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1, this.getZ(), 40, 0.6, 1, 0.6, 0.05);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.0F, 1.4F);
        brood.clear();
        Player target = this.getTarget() instanceof Player p ? p : null;
        for (int i = 0; i < (phase() == 1 ? 4 : 6); i++) {
            Mimicker m = ModEntities.MIMICKER.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (m == null) continue;
            m.snapTo(this.getX() + this.random.nextGaussian() * 3, this.getY(), this.getZ() + this.random.nextGaussian() * 3, 0, 0);
            level.addFreshEntity(m);
            m.reveal(level, target);
            brood.add(m.getUUID());
        }
        // and the room itself turns on you
        BlockPos c = this.blockPosition();
        for (int i = 0; i < 30; i++) {
            MimicManager.tryMimic(level, c.offset(this.random.nextInt(17) - 8, this.random.nextInt(5) - 2, this.random.nextInt(17) - 8));
        }
    }

    private void reveal(ServerLevel level) {
        hidden = 0;
        this.setInvisible(false);
        level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1, this.getZ(), 30, 0.5, 1, 0.5, 0.05);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isDisguised()) return false;
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        if (isDisguised()) reveal(level);
        this.hurtServer(level, source != null ? level.damageSources().playerAttack(source) : level.damageSources().magic(), 4.0F);
    }
}
