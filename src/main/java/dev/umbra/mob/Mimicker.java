package dev.umbra.mob;

import dev.umbra.light.LightSensitive;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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

import java.util.List;

/**
 * Подражатель: invisible, it makes the sounds of harmless things — a cow, a villager, footsteps — to lure you close.
 * Get within a few blocks and it shows itself.
 */
public class Mimicker extends Monster implements LightSensitive {
    private static final List<SoundEvent> LURES = List.of(
            SoundEvents.COW_AMBIENT, SoundEvents.VILLAGER_AMBIENT, SoundEvents.CHICKEN_AMBIENT,
            SoundEvents.SHEEP_AMBIENT, SoundEvents.PIG_AMBIENT, SoundEvents.STONE_STEP, SoundEvents.WOODEN_DOOR_OPEN);
    private static final double REVEAL_RANGE = 5.0;
    private int hiddenAgain;

    public Mimicker(EntityType<? extends Mimicker> type, Level level) {
        super(type, level);
        this.setInvisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.45, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5));
    }

    public boolean isHidden() {
        return this.isInvisible();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        Player nearest = level.getNearestPlayer(this, 24.0);
        if (nearest != null && (nearest.isCreative() || nearest.isSpectator())) nearest = null;
        if (isHidden()) {
            if (nearest != null && this.tickCount % 120 == 0 && this.random.nextInt(2) == 0) {
                SoundEvent lure = LURES.get(this.random.nextInt(LURES.size()));
                level.playSound(null, this.getX(), this.getY(), this.getZ(), lure, SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            if (nearest != null && nearest.distanceTo(this) < REVEAL_RANGE) reveal(level, nearest);
        } else if (this.getTarget() == null && ++hiddenAgain > 20 * 30) {
            this.setInvisible(true);
            hiddenAgain = 0;
        }
    }

    public void reveal(ServerLevel level, Player target) {
        this.setInvisible(false);
        hiddenAgain = 0;
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.2F, 0.6F);
        if (target != null) this.setTarget(target);
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        reveal(level, source);
        this.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 8, 1));
        this.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 10, 0));
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (isHidden()) reveal(level, source.getEntity() instanceof Player p ? p : null);
        return super.hurtServer(level, source, amount);
    }
}
