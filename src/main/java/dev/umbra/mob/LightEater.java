package dev.umbra.mob;

import dev.umbra.Umbra;
import dev.umbra.light.DarknessProof;
import dev.umbra.light.LightSensitive;
import dev.umbra.registry.ModBlocks;
import dev.umbra.util.UmbraUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Пожиратель света: a pale moth-spirit that drifts through walls towards any light and eats it.
 * It attacks players who carry light. Luminite in any form (lamps, blocks, clusters, ore) and eternal torches
 * are too pure for it: the tag {@code umbra:light_eater_ignores} lists what it leaves alone.
 */
public class LightEater extends Vex implements LightSensitive {
    public static final TagKey<Block> IGNORED = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Umbra.MODID, "light_eater_ignores"));
    private static final int SEARCH = 16;
    private BlockPos food;
    private int stunned;

    public LightEater(EntityType<? extends LightEater> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vex.createAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void registerGoals() {
        // behaviour lives in customServerAiStep
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        // no sword for this one
    }

    public static boolean edible(BlockState state) {
        if (state.getLightEmission() < 10) return false;
        if (state.getBlock() instanceof DarknessProof || state.is(IGNORED) || state.is(ModBlocks.LUMINITE_LAMP.get())) return false;
        if (state.getBlock() instanceof dev.umbra.underside.RiftBlock) return false;
        return state.getBlock().defaultDestroyTime() >= 0;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (stunned > 0) {
            stunned--;
            return;
        }
        // 1) players carrying light are the juiciest target
        Player carrier = null;
        for (Player p : level.players()) {
            if (!p.isSpectator() && !p.isCreative() && p.distanceTo(this) < 12 && UmbraUtil.holdsLight(p)) {
                carrier = p;
                break;
            }
        }
        if (carrier != null) {
            Vec3 to = carrier.getEyePosition();
            this.getMoveControl().setWantedPosition(to.x, to.y - 0.5, to.z, 1.0);
            this.setIsCharging(true);
            if (this.distanceTo(carrier) < 1.8 && this.tickCount % 15 == 0) {
                carrier.hurtServer(level, level.damageSources().mobAttack(this), 3.0F);
            }
            return;
        }
        this.setIsCharging(false);
        // 2) otherwise: find a light and eat it
        if (food == null || !edible(level.getBlockState(food)) || this.tickCount % 40 == 0) {
            food = findFood(level);
        }
        if (food != null) {
            Vec3 c = Vec3.atCenterOf(food);
            this.getMoveControl().setWantedPosition(c.x, c.y, c.z, 0.7);
            if (this.position().distanceTo(c) < 1.6) {
                level.destroyBlock(food, true);
                level.playSound(null, food, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.8F, 1.6F);
                level.sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 10, 0.2, 0.2, 0.2, 0.01);
                food = null;
            }
        } else if (this.tickCount % 60 == 0) {
            Vec3 drift = this.position().add(this.random.nextGaussian() * 6, this.random.nextGaussian() * 2, this.random.nextGaussian() * 6);
            this.getMoveControl().setWantedPosition(drift.x, drift.y, drift.z, 0.4);
        }
    }

    private BlockPos findFood(ServerLevel level) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        BlockPos c = this.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-SEARCH, -SEARCH / 2, -SEARCH), c.offset(SEARCH, SEARCH / 2, SEARCH))) {
            BlockState s = level.getBlockState(p);
            if (edible(s)) {
                double d = p.distSqr(c);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    @Override
    public void onLit(ServerLevel level, Player source) {
        stunned = 60;
        this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 3));
        this.hurtServer(level, source != null ? level.damageSources().playerAttack(source) : level.damageSources().magic(), 5.0F);
    }
}
