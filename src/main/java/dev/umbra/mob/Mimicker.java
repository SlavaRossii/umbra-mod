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
 * Подражатель: it sits there as an ordinary chest, and makes the sounds of harmless things — a villager, a sheep,
 * a creaking lid — to lure you close. Open it, hit it, light it up or come right up to it, and it shows itself:
 * a mouth on spider legs. Left alone for half a minute it settles down into a chest again.
 */
public class Mimicker extends Monster implements LightSensitive {
    private static final List<SoundEvent> LURES = List.of(
            SoundEvents.VILLAGER_AMBIENT, SoundEvents.SHEEP_AMBIENT,
            SoundEvents.STONE_STEP, SoundEvents.WOODEN_DOOR_OPEN, SoundEvents.SPIDER_AMBIENT);
    private static final double REVEAL_RANGE = 2.5;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> HIDDEN =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Mimicker.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private int hiddenAgain;

    public Mimicker(EntityType<? extends Mimicker> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HIDDEN, true);
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
        return this.entityData.get(HIDDEN);
    }

    private void setHidden(boolean hidden) {
        this.entityData.set(HIDDEN, hidden);
    }

    /** A chest does not walk. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isHidden();
    }

    /** Opening it is the worst idea. */
    @Override
    protected net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        if (isHidden() && this.level() instanceof ServerLevel level) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 0.8F);
            reveal(level, player);
            this.doHurtTarget(level, player);
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        // decided here: an immobile mob (a hidden one) never gets its server AI step
        if (this.level() instanceof ServerLevel level && this.isAlive()) think(level);
        super.aiStep();
    }

    private void think(ServerLevel level) {
        Player nearest = level.getNearestPlayer(this, 24.0);
        if (nearest != null && (nearest.isCreative() || nearest.isSpectator())) nearest = null;
        if (isHidden()) {
            // sit square, like a chest that was put there
            this.getNavigation().stop();
            // in the middle of its block, square to the world, like any chest that was put there
            double cx = Math.floor(this.getX()) + 0.5, cz = Math.floor(this.getZ()) + 0.5;
            if (Math.abs(this.getX() - cx) > 1.0E-3 || Math.abs(this.getZ() - cz) > 1.0E-3) this.setPos(cx, this.getY(), cz);
            this.setDeltaMovement(0, Math.min(0, this.getDeltaMovement().y), 0);
            this.setXRot(0.0F);
            this.setYRot(Math.round(this.getYRot() / 90.0F) * 90.0F);
            this.setYBodyRot(this.getYRot());
            this.setYHeadRot(this.getYRot());
            if (nearest != null && this.tickCount % 120 == 0 && this.random.nextInt(2) == 0) {
                SoundEvent lure = this.random.nextInt(3) == 0 ? SoundEvents.CHEST_CLOSE : LURES.get(this.random.nextInt(LURES.size()));
                level.playSound(null, this.getX(), this.getY(), this.getZ(), lure, SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            if (nearest != null && nearest.distanceTo(this) < REVEAL_RANGE) reveal(level, nearest);
        } else if (this.getTarget() == null && ++hiddenAgain > 20 * 30) {
            setHidden(true);
            hiddenAgain = 0;
        }
    }

    public void reveal(ServerLevel level, Player target) {
        setHidden(false);
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
