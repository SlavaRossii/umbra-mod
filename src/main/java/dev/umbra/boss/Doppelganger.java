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
public class Doppelganger extends UmbraBoss implements LightSensitive {
    private static final int ECHO_DELAY = 60;
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    private boolean copied;

    public Doppelganger(EntityType<? extends Doppelganger> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
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
        if (!copied || this.tickCount % 200 == 0) {
            copied = true;
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
