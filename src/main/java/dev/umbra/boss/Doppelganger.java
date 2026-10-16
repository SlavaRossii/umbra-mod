package dev.umbra.boss;

import dev.umbra.light.LightSensitive;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;

/**
 * Двойник: takes your weapon and armour, and steps where you stood three seconds ago.
 */
public class Doppelganger extends UmbraBoss implements LightSensitive, dev.umbra.util.Mirrored {
    private static final int ECHO_DELAY = 60;
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    private boolean copied;
    private int swapTimer;
    private int decoyTimer;

    public Doppelganger(EntityType<? extends Doppelganger> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
    }

    private static final net.minecraft.network.syncher.EntityDataAccessor<String> MIRROR =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Doppelganger.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MIRROR, "");
    }

    @Override
    public java.util.UUID mirrorOf() {
        String s = this.entityData.get(MIRROR);
        try {
            return s.isEmpty() ? null : java.util.UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public int mirrorKind() {
        return 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void bossTick(ServerLevel level) {
        if (!(this.getTarget() instanceof Player target)) return;
        int phase = trackPhase(level);
        // swaps places with you — suddenly you stand where it stood
        if (++swapTimer >= (phase == 1 ? 180 : 120) && this.distanceTo(target) < 20) {
            swapTimer = 0;
            Vec3 me = this.position(), you = target.position();
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, me.x, me.y + 1, me.z, 30, 0.3, 0.8, 0.3, 0.05);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, you.x, you.y + 1, you.z, 30, 0.3, 0.8, 0.3, 0.05);
            this.teleportTo(you.x, you.y, you.z);
            target.teleportTo(me.x, me.y, me.z);
            level.playSound(null, me.x, me.y, me.z, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.8F);
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0));
            stun(STUN_TICKS);
        }
        // phase 2: copies of the copy
        if (phase == 2 && ++decoyTimer >= 240) {
            decoyTimer = 0;
            for (var m : BossMoves.summon(level, this, dev.umbra.registry.ModEntities.PHANTOM_STEVE.get(), 2, target)) {
                if (m instanceof dev.umbra.sanity.PhantomEntity ph) ph.setup(dev.umbra.sanity.PhantomEntity.Mode.SHADOW, target);
            }
        }
        if (!copied || this.tickCount % 200 == 0) {
            copied = true;
            // it takes your face as well as your gear
            this.entityData.set(MIRROR, target.getUUID().toString());
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                this.setItemSlot(slot, target.getItemBySlot(slot).copy());
                this.setDropChance(slot, 0.0F);
            }
        }
        trail.addLast(target.position());
        while (trail.size() > ECHO_DELAY) trail.pollFirst();
        if (this.tickCount % 100 == 0 && trail.size() >= ECHO_DELAY) {
            Vec3 echo = trail.peekFirst();
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1, this.getZ(), 25, 0.3, 0.8, 0.3, 0.05);
            this.teleportTo(echo.x, echo.y, echo.z);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, echo.x, echo.y + 1, echo.z, 25, 0.3, 0.8, 0.3, 0.05);
        }
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 6, 1));
        this.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 6, 0));
    }
}
