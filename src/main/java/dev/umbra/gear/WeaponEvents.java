package dev.umbra.gear;

import dev.umbra.journal.Bestiary;
import dev.umbra.registry.ModItems;
import dev.umbra.sanity.Sanity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 2.5: the weapons and the weapon enchantments, and the full-set bonuses of the armour.
 * <ul>
 *   <li>Luminite spear: reaches further (attribute), +3 against the things of the Underside.</li>
 *   <li>Shepherd's scythe: a full swing cuts everything within 3 blocks of the target for 60%.</li>
 *   <li>Shadow dagger: quick (attribute), twice the damage from behind.</li>
 *   <li>Radiance I-III: +1.5 per level against the things of the Underside.</li>
 *   <li>Clarity I-II: each of them you kill gives back 2 sanity per level.</li>
 *   <li>Shadow Cut I-II: 15% per level that your shadow strikes again from behind for half.</li>
 *   <li>Full gloom: resistance in the Underside, their blows 15% weaker. Full shadow: night sight in the dark, and
 *       crouching makes the far ones lose you. Full luminite: their blows 20% weaker, light eaters burn near you.</li>
 * </ul>
 */
public final class WeaponEvents {
    private WeaponEvents() {}

    public static final float SPEAR_BONUS = 3.0F, RADIANCE_PER_LEVEL = 1.5F, SCYTHE_SWEEP = 0.6F, BACKSTAB = 2.0F;
    public static final float SHADOW_CUT_CHANCE = 0.15F, CLARITY_PER_LEVEL = 2.0F;
    public static final float GLOOM_SET_GUARD = 0.85F, LUMINITE_SET_GUARD = 0.8F;
    private static boolean echoing;

    /** A creature of the Underside (or one of its bosses). */
    public static boolean dark(LivingEntity e) {
        return Bestiary.ids().containsKey(e.getType()) || e instanceof dev.umbra.boss.UmbraBoss;
    }

    /** Is {@code attacker} behind {@code target} (outside the front half-circle of its body)? */
    public static boolean behind(LivingEntity target, LivingEntity attacker) {
        Vec3 look = Vec3.directionFromRotation(0.0F, target.yBodyRot);
        Vec3 to = attacker.position().subtract(target.position());
        to = new Vec3(to.x, 0, to.z);
        if (to.lengthSqr() < 1.0E-4) return false;
        return look.dot(to.normalize()) < -0.3;
    }

    /** Extra damage of the weapon in hand before armour; used by the event and by the tests. */
    public static float modify(ServerPlayer player, LivingEntity target, float amount) {
        if (!(player.level() instanceof ServerLevel level)) return amount;
        var hand = player.getMainHandItem();
        if (dark(target)) {
            if (hand.is(ModItems.LUMINITE_SPEAR.get())) amount += SPEAR_BONUS;
            amount += RADIANCE_PER_LEVEL * Gear.level(level, hand, Gear.RADIANCE);
        }
        if (hand.is(ModItems.SHADOW_DAGGER.get()) && behind(target, player)) {
            amount *= BACKSTAB;
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
        return amount;
    }

    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        // their blows on a player in a full set
        if (target instanceof ServerPlayer victim && event.getSource().getEntity() instanceof LivingEntity src && dark(src)) {
            if (Gear.pieces(victim, Gear.GLOOM) == 4) event.setAmount(event.getAmount() * GLOOM_SET_GUARD);
            if (Gear.pieces(victim, Gear.LUMINITE) == 4) event.setAmount(event.getAmount() * LUMINITE_SET_GUARD);
        }
        // a player's blow in melee
        if (echoing || !(event.getSource().getEntity() instanceof ServerPlayer player) || event.getSource().getDirectEntity() != player) return;
        event.setAmount(dev.umbra.craft.Infusions.modify(player, target, modify(player, target, event.getAmount())));
    }

    public static void onDamage(LivingDamageEvent.Post event) {
        if (echoing || !(event.getSource().getEntity() instanceof ServerPlayer player) || event.getSource().getDirectEntity() != player) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        LivingEntity target = event.getEntity();
        float dealt = event.getHealthDamage();
        var hand = player.getMainHandItem();
        echoing = true;
        try {
            dev.umbra.craft.Infusions.afterHit(level, player, target, dealt);
            // the scythe: a full swing cuts all round the target
            if (hand.is(ModItems.SHEPHERD_SCYTHE.get()) && player.getAttackStrengthScale(0.5F) > 0.9F) {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.0),
                        x -> x != target && x != player && x.isAlive() && x instanceof Mob && !(x instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame()))) {
                    e.hurtServer(level, level.damageSources().playerAttack(player), dealt * SCYTHE_SWEEP);
                }
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 3, 1.2, 0.2, 1.2, 0);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            // Shadow Cut: your shadow strikes from behind
            int cut = Gear.level(level, hand, Gear.SHADOW_CUT);
            if (cut > 0 && target.isAlive() && player.getRandom().nextFloat() < SHADOW_CUT_CHANCE * cut) {
                Vec3 back = target.position().subtract(Vec3.directionFromRotation(0.0F, target.yBodyRot).scale(0.8));
                level.sendParticles(ParticleTypes.SQUID_INK, back.x, back.y + 1.0, back.z, 12, 0.2, 0.5, 0.2, 0.02);
                level.playSound(null, back.x, back.y, back.z, SoundEvents.PHANTOM_BITE, SoundSource.PLAYERS, 0.8F, 0.6F);
                EnchantEvents.later(level, target, player, 11, Math.max(1.0F, dealt * 0.5F));
            }
        } finally {
            echoing = false;
        }
    }

    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !dark(event.getEntity())) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        int clarity = Gear.level(level, player.getMainHandItem(), Gear.CLARITY);
        if (clarity > 0 && Sanity.enabled()) Sanity.add(player, CLARITY_PER_LEVEL * clarity);
    }

    // ------------------------------------------------------------------ full sets

    private static final String[] SET_TAGS = {"umbra_set_gloom", "umbra_set_luminite", "umbra_set_shadow"};

    /** Once a second, for every player: the bonuses of a full set (all four pieces, any tiers). */
    public static void setTick(ServerPlayer player) {
        if (player.tickCount % 20 != 11 || !(player.level() instanceof ServerLevel level)) return;
        for (int set = 0; set < 3; set++) {
            boolean full = Gear.pieces(player, set) == 4;
            boolean had = player.entityTags().contains(SET_TAGS[set]);
            if (full && !had) {
                player.addTag(SET_TAGS[set]);
                player.sendOverlayMessage(Component.translatable("set.umbra." + Gear.SETS[set] + ".on").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            } else if (!full && had) {
                player.removeTag(SET_TAGS[set]);
            }
        }
        if (Gear.pieces(player, Gear.GLOOM) == 4 && dev.umbra.underside.Underside.is(level)) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0, true, false, true));
        }
        if (Gear.pieces(player, Gear.SHADOW) == 4) {
            if (level.getMaxLocalRawBrightness(player.blockPosition()) <= 7) {
                MobEffectInstance nv = player.getEffect(MobEffects.NIGHT_VISION);
                if (nv == null || nv.getDuration() < 220) player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
            }
            if (player.isCrouching()) {
                for (Mob m : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24.0), x -> x.getTarget() == player && dark(x) && !(x instanceof dev.umbra.boss.UmbraBoss))) {
                    if (m.distanceTo(player) > 6.0F) m.setTarget(null);
                }
            }
        }
        if (Gear.pieces(player, Gear.LUMINITE) == 4) {
            for (dev.umbra.mob.LightEater e : level.getEntitiesOfClass(dev.umbra.mob.LightEater.class, player.getBoundingBox().inflate(8.0))) {
                e.hurtServer(level, level.damageSources().magic(), 2.0F);
            }
        }
    }

    /** Is a full set worn? (for the tests) */
    public static boolean fullSet(Player player, int set) {
        return Gear.pieces(player, set) == 4;
    }
}
