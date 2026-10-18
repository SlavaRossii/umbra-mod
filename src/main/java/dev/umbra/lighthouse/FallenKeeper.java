package dev.umbra.lighthouse;

import dev.umbra.boss.BossMoves;
import dev.umbra.boss.UmbraBoss;
import dev.umbra.journal.Journal;
import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModEntities;
import dev.umbra.sanity.Sanity;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 2.7: the Fallen Keeper. When one who took the Primordial heart in the dark comes back to the lighthouse, Yan sets
 * down his pipe and takes up the lamp against them: the last boss of the dark ending.
 * <ul>
 *   <li>Phase 1, the lantern: he swings it; the lamp above marks where you stand and strikes there a moment later;
 *       every so often the lantern flares and burns one who has taken the dark into himself. After a flare he is
 *       blind himself for two seconds: that is the moment.</li>
 *   <li>Phase 2, the lamp breaks: the lighthouse goes dark, and the beams come rolling along the ground at you.</li>
 *   <li>Phase 3, Marta's echo: two echoes of her rise and keep him (he takes a third of every blow while one
 *       stands); he pulls you in and the ground bursts round him.</li>
 * </ul>
 */
public class FallenKeeper extends UmbraBoss {
    public static final String ECHO_TAG = "umbra_marta_echo";
    public static final float ECHO_SHIELD = 0.33F;
    public static final float BEAM = 8.0F, FLARE = 5.0F;
    private final BossMoves.Bursts beams = new BossMoves.Bursts();
    private int beamTimer, flareTimer, rollTimer, pullTimer;
    private boolean echoes;

    public FallenKeeper(EntityType<? extends FallenKeeper> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW);
        this.xpReward = 200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 800.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.SCALE, 1.25);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public int phase() {
        float h = this.getHealth() / this.getMaxHealth();
        return h > 0.66F ? 1 : h > 0.33F ? 2 : 3;
    }

    /** Yan stands up against one who took the dark. Called by Yan himself when such a one comes near. */
    public static FallenKeeper rise(ServerLevel level, LampKeeper yan, ServerPlayer who) {
        FallenKeeper boss = ModEntities.FALLEN_KEEPER.get().create(level, EntitySpawnReason.EVENT);
        if (boss == null) return null;
        // he steps out to meet you (his house is too small for a fight): four blocks from you, towards where he stood
        Vec3 to = yan.position().subtract(who.position());
        Vec3 at = to.horizontalDistance() > 5 ? who.position().add(to.x / to.horizontalDistance() * 4, 0, to.z / to.horizontalDistance() * 4) : yan.position();
        BlockPos ab = BlockPos.containing(at);
        if (!level.getBlockState(ab).getCollisionShape(level, ab).isEmpty() || !level.getBlockState(ab.above()).getCollisionShape(level, ab.above()).isEmpty()
                || !level.getBlockState(ab.above(2)).getCollisionShape(level, ab.above(2)).isEmpty()) at = yan.position();
        boss.snapTo(at.x, at.y, at.z, yan.getYRot(), 0);
        boss.setTarget(who);
        level.addFreshEntity(boss);
        level.sendParticles(ParticleTypes.END_ROD, yan.getX(), yan.getY() + 1.0, yan.getZ(), 80, 0.6, 1.0, 0.6, 0.15);
        level.playSound(null, yan.getX(), yan.getY(), yan.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.0F, 0.5F);
        java.util.Set<ServerPlayer> told = new java.util.LinkedHashSet<>(level.getEntitiesOfClass(ServerPlayer.class, yan.getBoundingBox().inflate(48)));
        told.add(who);
        for (ServerPlayer p : told) {
            for (int i = 1; i <= 3; i++)
                p.sendSystemMessage(Component.translatable("dialogue.umbra.yan.fall." + i).withStyle(i == 3 ? ChatFormatting.GOLD : ChatFormatting.GRAY, ChatFormatting.ITALIC));
            Journal.unlock(p, "fallen_keeper");
        }
        yan.discard();
        return boss;
    }

    @Override
    protected void bossTick(ServerLevel level) {
        int phase = trackPhase(level);
        beams.tick(level, this, BEAM);
        LivingEntity target = this.getTarget();
        if (target == null) return;
        // the lamp above marks where you stand, and strikes
        if (++beamTimer >= (phase == 1 ? 90 : 70)) {
            beamTimer = 0;
            for (ServerPlayer p : opponents(level)) {
                BlockPos at = p.blockPosition();
                beams.mark(level, at, 30);
                for (int y = 0; y < 12; y += 2) level.sendParticles(ParticleTypes.END_ROD, at.getX() + 0.5, at.getY() + y, at.getZ() + 0.5, 2, 0.1, 0.5, 0.1, 0.0);
            }
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 1.5F, 1.6F);
        }
        // the lantern flares: it burns those who took the dark; then he cannot see for a moment
        if (++flareTimer >= 200) {
            flareTimer = 0;
            flare(level);
        }
        // phase 2 on: a beam rolls along the ground from him to you
        if (phase >= 2 && ++rollTimer >= 110) {
            rollTimer = 0;
            Vec3 from = this.position(), dir = target.position().subtract(from);
            double len = Math.max(1.0, dir.horizontalDistance());
            for (int i = 2; i <= Math.min(18, (int) len + 4); i += 2) {
                BlockPos p = BlockPos.containing(from.x + dir.x / len * i, target.getY(), from.z + dir.z / len * i);
                beams.mark(level, p, 8 + i * 2);
            }
        }
        // phase 3: pulled in, the ground bursts round him
        if (phase == 3 && ++pullTimer >= 160) {
            pullTimer = 0;
            BossMoves.pull(level, this, 14, 1.2);
            BlockPos c = this.blockPosition();
            for (int a = 0; a < 10; a++) {
                double rad = a * Math.PI / 5;
                beams.mark(level, c.offset((int) Math.round(Math.cos(rad) * 3.5), 0, (int) Math.round(Math.sin(rad) * 3.5)), 30);
            }
        }
    }

    /** The lantern flares: every player who took the dark and is near burns and loses some mind; then he is winded. */
    public int flare(ServerLevel level) {
        return flare(level, java.util.List.of());
    }

    /** @param also players to count that the level may not list (the tests' fake ones) */
    public int flare(ServerLevel level, java.util.List<ServerPlayer> also) {
        int burnt = 0;
        level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.5, this.getZ(), 60, 0.4, 0.4, 0.4, 0.4);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2.0F, 1.8F);
        java.util.Set<ServerPlayer> near = new java.util.LinkedHashSet<>(level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(10)));
        near.addAll(also);
        for (ServerPlayer p : near) {
            if (p.isCreative() || p.isSpectator()) continue;
            if (dev.umbra.ending.Endings.isDark(p)) {
                p.hurtServer(level, level.damageSources().indirectMagic(this, this), FLARE);
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                if (Sanity.enabled()) Sanity.add(p, -8.0F);
                burnt++;
            }
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
        }
        stun(STUN_TICKS);
        return burnt;
    }

    @Override
    protected void onPhase(ServerLevel level, int phase) {
        if (phase == 2) {
            // the lamp breaks
            BlockPos lamp = LampKeeper.findLamp(level, getX(), getZ());
            if (lamp != null) {
                level.setBlock(lamp, level.getBlockState(lamp).setValue(LighthouseLampBlock.LEVEL, 0).setValue(LighthouseLampBlock.FILL, 0), 3);
                level.sendParticles(ParticleTypes.END_ROD, lamp.getX() + 0.5, lamp.getY() + 0.5, lamp.getZ() + 0.5, 120, 1.5, 1.5, 1.5, 0.3);
                level.playSound(null, lamp, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 3.0F, 0.5F);
            }
            say(level, "dialogue.umbra.yan.fall.lamp");
        }
        if (phase == 3 && !echoes) {
            echoes = true;
            for (Mob m : BossMoves.summon(level, this, ModEntities.SHADE.get(), 2, getTarget())) {
                m.addTag(ECHO_TAG);
                m.setCustomName(Component.translatable("entity.umbra.marta_echo"));
                m.setCustomNameVisible(true);
                var hp = m.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) hp.setBaseValue(40.0);
                m.setHealth(m.getMaxHealth());
            }
            say(level, "dialogue.umbra.yan.fall.marta");
        }
    }

    private void say(ServerLevel level, String key) {
        for (ServerPlayer p : opponents(level)) p.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
    }

    public int echoesStanding(ServerLevel level) {
        return level.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(40), m -> m.isAlive() && m.entityTags().contains(ECHO_TAG)).size();
    }

    @Override
    protected float weakness(ServerLevel level, DamageSource source) {
        return echoesStanding(level) > 0 ? ECHO_SHIELD : 1.0F;
    }

    @Override
    protected void stunnedTick(ServerLevel level) {
        beams.tick(level, this, BEAM);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) fallen(level, java.util.List.of());
    }

    /** What his fall leaves: the world remembers, the echoes go, the dark ending closes for everyone near. */
    public void fallen(ServerLevel level, java.util.List<ServerPlayer> also) {
        level.setData(ModAttachments.YAN.get(), "fallen");
        for (Mob m : level.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(48), m -> m.entityTags().contains(ECHO_TAG))) m.discard();
        java.util.Set<ServerPlayer> near = new java.util.LinkedHashSet<>(level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(64)));
        near.addAll(also);
        for (ServerPlayer p : near) {
            Journal.unlock(p, "ending_fallen");
            dev.umbra.ending.Endings.fallenCredits(p);
        }
    }
}
