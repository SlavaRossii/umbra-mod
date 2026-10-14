package dev.umbra.mob;

import dev.umbra.light.LightSensitive;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Слепец: big, strong, and blind. It hunts only by sound: sprinting is heard from far away, walking from closer,
 * sneaking almost not at all. Stand still or sneak and it loses you.
 */
public class Blind extends Monster implements LightSensitive {
    private static final double SPRINT_RANGE = 22.0;
    private static final double WALK_RANGE = 12.0;
    private static final double SNEAK_RANGE = 2.5;
    private static final int FORGET_TICKS = 60;

    private final Map<UUID, Vec3> lastPositions = new HashMap<>();
    private int sinceHeard;

    public Blind(EntityType<? extends Blind> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.29)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.SCALE, 1.15);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        // no eyes, no look/target goals: hearing is handled in customServerAiStep
    }

    /** How far away the player can be heard right now. */
    static double noiseRange(Player player, Vec3 previous) {
        double moved = previous == null ? 0 : player.position().distanceTo(previous);
        double quiet = 1.0 - dev.umbra.gear.Gear.stealth(player);   // shadow gear, boots of silence, the ash clasp
        if (player.isSprinting()) return SPRINT_RANGE * quiet;
        if (player.isShiftKeyDown()) return SNEAK_RANGE * quiet;
        if (moved > 0.05 * 5) return WALK_RANGE * quiet;
        return SNEAK_RANGE * quiet;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.tickCount % 5 != 0) return;
        Player heard = null;
        double best = Double.MAX_VALUE;
        for (Player p : level.players()) {
            if (p.isSpectator() || p.isCreative()) continue;
            Vec3 prev = lastPositions.put(p.getUUID(), p.position());
            double d = p.distanceTo(this);
            if (d <= noiseRange(p, prev) && d < best) {
                best = d;
                heard = p;
            }
        }
        if (heard != null) {
            if (this.getTarget() != heard) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SNIFF, SoundSource.HOSTILE, 1.0F, 0.7F);
            }
            this.setTarget(heard);
            sinceHeard = 0;
        } else if (this.getTarget() != null && (sinceHeard += 5) > FORGET_TICKS) {
            this.setTarget(null);
            this.getNavigation().stop();
        }
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        // it cannot see the light, but it feels the heat: stunned
        this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 5, 4));
        this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 5, 1));
    }
}
