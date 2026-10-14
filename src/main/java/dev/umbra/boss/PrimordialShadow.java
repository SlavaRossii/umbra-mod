package dev.umbra.boss;

import dev.umbra.light.LightSensitive;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModEntities;
import dev.umbra.shadow.ShadowManager;
import dev.umbra.util.UmbraUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.level.block.Block;

/**
 * Первородная Тень — the final boss. Three phases:
 *  1) swarm: she fights with her brood of shades;
 *  2) darkness: the arena drowns in living darkness and she hides inside it — burn it away to reach her;
 *  3) light: only light can hurt her now (hit her holding a light, or flash her with the Dispeller).
 * Defeat her and your own shadow becomes your ally.
 */
public class PrimordialShadow extends UmbraBoss implements LightSensitive {
    private int timer;
    private int noShroud;
    private boolean dashing;
    private int dashTimer;
    private final BossMoves.Bursts bursts = new BossMoves.Bursts();

    public PrimordialShadow(EntityType<? extends PrimordialShadow> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.SCALE, 1.8);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** 1, 2 or 3. */
    public int phase() {
        float f = this.getHealth() / this.getMaxHealth();
        return f > 0.66F ? 1 : f > 0.33F ? 2 : 3;
    }

    private boolean shrouded(ServerLevel level) {
        BlockPos c = this.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-1, 0, -1), c.offset(1, 3, 1))) {
            if (level.getBlockState(p).is(ModBlocks.LIVING_DARKNESS.get())) return true;
        }
        return false;
    }

    @Override
    protected void bossTick(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (noShroud > 0) noShroud--;
        timer++;
        int phase = trackPhase(level);
        bursts.tick(level, this, 9.0F);
        // a dash through the arena, ending in a blow on the ground
        if (dashing && this.onGround() && dashTimer > 5) {
            dashing = false;
            BossMoves.shockwave(level, this, 3.5, 8.0F, 1.2);
            stun(STUN_TICKS);
        }
        dashTimer++;
        if (!dashing && target != null && dashTimer >= (phase == 3 ? 80 : 140) && this.distanceTo(target) > 5) {
            dashTimer = 0;
            dashing = true;
            BossMoves.leap(this, target, 1.6);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.0F, 1.6F);
        }
        if (phase >= 2 && timer % 220 == 0) BossMoves.pull(level, this, 16, 1.4);
        if (phase == 3) {
            if (timer % 80 == 0) for (ServerPlayer p : opponents(level)) bursts.mark(level, p.blockPosition(), 25);
            if (timer % 120 == 60) BossMoves.fangsRing(level, this, this.position(), 3.5, 14, 20);
        }
        switch (phase) {
            case 1 -> {
                if (timer % 300 == 0 && target != null) BossMoves.shades(level, this, target, 4);
            }
            case 2 -> {
                if (noShroud == 0 && timer % 60 == 0) {
                    BlockPos c = this.blockPosition();
                    for (BlockPos p : BlockPos.betweenClosed(c.offset(-1, 0, -1), c.offset(1, 3, 1))) {
                        if (level.getBlockState(p).isAir()) level.setBlock(p, ModBlocks.LIVING_DARKNESS.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                if (timer % 100 == 0) {
                    for (ServerPlayer p : opponents(level)) {
                        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 6, 0));
                        BlockPos t = p.blockPosition();
                        for (int a = 0; a < 360; a += 15) {
                            double rad = Math.toRadians(a);
                            BlockPos q = t.offset((int) Math.round(Math.cos(rad) * 4), 0, (int) Math.round(Math.sin(rad) * 4));
                            for (int y = 0; y < 3; y++) {
                                if (level.getBlockState(q.above(y)).isAir()) level.setBlock(q.above(y), ModBlocks.LIVING_DARKNESS.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                            }
                        }
                    }
                }
            }
            default -> {
                if (timer % 20 == 0) level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 2, this.getZ(), 10, 0.6, 1.2, 0.6, 0.02);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        int phase = phase();
        if (phase == 2 && shrouded(level) && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (!chipShroud(level, source)) return false;
            tearShroud(level);
        }
        if (phase == 3) {
            boolean light = source.getEntity() instanceof Player p && UmbraUtil.holdsLight(p);
            if (!light) {
                level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 2, this.getZ(), 6, 0.5, 0.8, 0.5, 0.02);
                return false;
            }
        }
        return super.hurtServer(level, source, amount);
    }

    private void tearShroud(ServerLevel level) {
        BlockPos c = this.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-2, -1, -2), c.offset(2, 4, 2))) {
            if (level.getBlockState(p).is(ModBlocks.LIVING_DARKNESS.get())) level.removeBlock(p, false);
        }
        noShroud = 20 * 4;
    }

    @Override
    protected void stunnedTick(ServerLevel level) {
        bursts.tick(level, this, 9.0F);
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        tearShroud(level);
        if (phase() == 3) {
            // light is the only thing that truly hurts her; bypass the phase-3 check
            super.hurtServer(level, source != null ? level.damageSources().playerAttack(source) : level.damageSources().magic(), 25.0F);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_DEATH, SoundSource.HOSTILE, 2.0F, 0.6F);
            for (ServerPlayer p : level.players()) {
                if (p.distanceTo(this) < 64) {
                    ShadowManager.free(p);
                    p.sendSystemMessage(Component.translatable("message.umbra.freed"));
                }
            }
        }
    }
}
