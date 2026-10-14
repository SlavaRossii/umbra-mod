package dev.umbra.village;

import dev.umbra.journal.Journal;
import dev.umbra.light.LightSensitive;
import dev.umbra.mimic.MimicBlock;
import dev.umbra.registry.ModEntities;
import dev.umbra.registry.ModItems;
import dev.umbra.sanity.Sanity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

/**
 * Shadow folk of the abandoned villages. They trade for shadow shards and echo quartz.
 * Some of them are not folk at all: about one in seven is a mimic wearing a villager.
 * Try to trade with one and it bites. Hit it first, or flash it with light, and it flees, dropping what it hoarded.
 * Killing a real one weighs on your mind.
 */
public class ShadowVillager extends WanderingTrader implements LightSensitive {
    public static final int MIMIC_PERCENT = 15;

    public ShadowVillager(EntityType<? extends ShadowVillager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.45).add(Attributes.MAX_HEALTH, 20.0);
    }

    /** Decided by the entity's UUID, so it never changes and needs no saving. */
    public boolean isMimic() {
        return Math.floorMod(this.getUUID().getLeastSignificantBits(), 100) < MIMIC_PERCENT;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.3));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        MerchantOffers offers = this.getOffers();
        ShadowTrades.fill(offers, this.random);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isMimic() && !player.isCreative() && this.level() instanceof ServerLevel level) {
            spring(level, player);
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) Journal.unlock(sp, "shadow_village");
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isMimic() && source.getEntity() instanceof Player p) {
            expose(level, p);
            return true;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (!isMimic() && source.getEntity() instanceof ServerPlayer player) {
            // it was only a villager
            Sanity.add(player, -10.0F);
            Journal.unlock(player, "shadow_village");
        }
        super.die(source);
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        if (isMimic()) {
            expose(level, source);
        } else {
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.6, getZ(), 4, 0.2, 0.2, 0.2, 0.01);
        }
    }

    /** Caught: it drops the villager shape and runs, leaving its hoard. */
    public void expose(ServerLevel level, Player by) {
        level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.0, getZ(), 30, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.SILVERFISH_DEATH, SoundSource.HOSTILE, 1.0F, 0.5F);
        RandomSource r = this.random;
        level.addFreshEntity(new ItemEntity(level, getX(), getY() + 0.5, getZ(), new ItemStack(ModItems.SHADOW_SHARD.get(), 2 + r.nextInt(3))));
        if (r.nextInt(3) == 0) level.addFreshEntity(new ItemEntity(level, getX(), getY() + 0.5, getZ(), new ItemStack(ModItems.ECHO_QUARTZ.get())));
        if (by instanceof ServerPlayer sp) Journal.unlock(sp, "mimic");
        this.discard();
    }

    /** Too late: it bites, and what was under the villager comes out. */
    public void spring(ServerLevel level, Player victim) {
        MimicBlock.bite(level, victim);
        level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.0, getZ(), 40, 0.5, 0.8, 0.5, 0.1);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.0F, 0.6F);
        var mimicker = ModEntities.MIMICKER.get().spawn(level, blockPosition(), EntitySpawnReason.MOB_SUMMONED);
        if (mimicker != null) mimicker.reveal(level, victim);
        for (int i = 0; i < 2; i++) ModEntities.SHADE.get().spawn(level, blockPosition(), EntitySpawnReason.MOB_SUMMONED);
        victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 5, 0));
        Sanity.add(victim, Sanity.Shock.MIMIC_SPRUNG);
        if (victim instanceof ServerPlayer sp) Journal.unlock(sp, "mimic");
        this.discard();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isTrading() ? SoundEvents.WANDERING_TRADER_TRADE : SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F;
    }
}
