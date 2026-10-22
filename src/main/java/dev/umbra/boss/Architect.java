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
    private long cageStunAt;
    private int quakeTimer;
    private int cageTimer;
    private final BossMoves.Bursts quake = new BossMoves.Bursts();
    private final BossMoves.TempBlocks cage = new BossMoves.TempBlocks();

    /** 2.10: what he throws from afar: blocks of his maze, lobbed high; where one lands, a piece of wall stands. */
    private final BossMoves.TempBlocks rubble = new BossMoves.TempBlocks();
    private final BossMoves.Style stones = new BossMoves.Style(
            new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, dev.umbra.registry.ModBlocks.MAZE_WALL.get().defaultBlockState()),
            ParticleTypes.CRIT, 0.05,
            new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, dev.umbra.registry.ModBlocks.MAZE_WALL.get().defaultBlockState()),
            SoundEvents.STONE_BREAK)
            .onHit((l, p) -> p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 30, 1)))
            .onLand((l, at) -> {
                BlockPos b = BlockPos.containing(at);
                if (!l.getBlockState(b).isAir()) b = b.above();
                for (int y = 0; y < 2; y++)
                    if (l.getBlockState(b.above(y)).isAir()) rubble.place(l, b.above(y), dev.umbra.registry.ModBlocks.MAZE_WALL.get().defaultBlockState(), 20 * 6);
            });

    @Override
    protected void rangedAttack(ServerLevel level, LivingEntity target) {
        int n = phase() == 1 ? 3 : 5;
        bolts.shoot(level, this.getEyePosition().add(0, 0.6, 0), target.position(), n, phase() == 1 ? 26.0F : 40.0F, 0.7, 7.0F, stones);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 1.0F, 0.5F);
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    @Override
    protected int rangedWindupTicks() {
        return 18;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent rangedWindupSound() {
        return SoundEvents.GRINDSTONE_USE;
    }

    public Architect(EntityType<? extends Architect> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.SCALE, 1.8);    // 2.9: a block taller and half a block wider than before (3.6 x 1.4)
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void bossTick(ServerLevel level) {
        int phase = trackPhase(level);
        quake.tick(level, this, 8.0F);
        cage.tick(level);
        rubble.tick(level);
        if (cageStunAt > 0 && this.tickCount >= cageStunAt) {
            // the cage has crumbled: he is spent
            cageStunAt = 0;
            stun(STUN_TICKS);
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null) return;
        if (rangedTick(level, target)) return;

        // the floor gives way: a cross of cracks around you
        if (++quakeTimer >= (phase == 1 ? 140 : 90)) {
            quakeTimer = 0;
            startAction(ACT_SLAM);
            BlockPos t = target.blockPosition();
            quake.mark(level, t, 30);
            for (int i = 2; i <= (phase == 1 ? 4 : 6); i += 2) {
                quake.mark(level, t.offset(i, 0, 0), 30 + i * 3);
                quake.mark(level, t.offset(-i, 0, 0), 30 + i * 3);
                quake.mark(level, t.offset(0, 0, i), 30 + i * 3);
                quake.mark(level, t.offset(0, 0, -i), 30 + i * 3);
            }
            level.playSound(null, t, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6F, 0.5F);
        }
        // phase 2: walls him into a cell for a few seconds, then walks in
        if (phase == 2 && ++cageTimer >= 320) {
            cageTimer = 0;
            startAction(ACT_SUMMON);
            BlockPos t = target.blockPosition();
            var wall = dev.umbra.registry.ModBlocks.MAZE_WALL.get().defaultBlockState();
            for (int x = -2; x <= 2; x++)
                for (int z = -2; z <= 2; z++)
                    if (Math.abs(x) == 2 || Math.abs(z) == 2)
                        for (int y = 0; y < 3; y++) cage.place(level, t.offset(x, y, z), wall, 20 * 5);
            level.playSound(null, t, SoundEvents.IRON_DOOR_CLOSE, SoundSource.HOSTILE, 1.0F, 0.5F);
            this.teleportTo(t.getX() + 1.5, t.getY(), t.getZ() + 0.5);
            cageStunAt = this.tickCount + 20 * 5;
        }
        if (++stepTimer >= (phase == 1 ? 100 : 60)) { // step through the walls to your side
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
            startAction(ACT_SUMMON);
            BossMoves.shades(level, this, target, 2);
        }
    }

    @Override
    protected void stunnedTick(ServerLevel level) {
        quake.tick(level, this, 8.0F);
        cage.tick(level);
        rubble.tick(level);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.level() instanceof ServerLevel level) {
            cage.clear(level);
            rubble.clear(level);
        }
        super.remove(reason);
    }

    public static final float STONE_SKIN = 0.35F;

    /** Stone: blows glance off him unless he is winded after one of his great moves. */
    @Override
    protected float weakness(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        return isStunned() ? 1.0F : STONE_SKIN;
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        lightStun(level);
        lightHurt(level, source, 8.0F);
    }
}
