package dev.umbra.gear;

import dev.umbra.Umbra;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 2.7: what makes each set its own, beyond the numbers.
 * <ul>
 *   <li>Shadow, any tiers, full: a tenth quicker on your feet (III: three twentieths).</li>
 *   <li>Gloom III, full, "Bastion": a blow that would leave you under 30% health raises a wall: Resistance II for
 *       5 s and four hearts of absorption for 10 s. Once in 90 s.</li>
 *   <li>Shadow III, full, "Slip": one blow in five from the things of the dark goes through where you were.</li>
 *   <li>Luminite III, full, "Halo": you shine like a lit lantern, and the dark that comes within 5 blocks is weakened.</li>
 * </ul>
 */
public final class SetPowers {
    private SetPowers() {}

    public static final float SLIP = 0.2F;
    public static final float BASTION_AT = 0.3F;
    public static final int BASTION_COOLDOWN = 20 * 90;
    public static final double HALO = 5.0;
    private static final Identifier SHADOW_SPEED = Identifier.fromNamespaceAndPath(Umbra.MODID, "shadow_set_speed");

    /** All four pieces of the set, all of tier III. */
    public static boolean master(Player player, int set) {
        return Gear.tierSum(player, set) == 12;
    }

    /** Every tick, for every player. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 4 == 0 && master(player, Gear.LUMINITE) && player.level() instanceof ServerLevel level)
            dev.umbra.light.LanternItem.placeLight(level, player);
        if (player.tickCount % 20 != 13 || !(player.level() instanceof ServerLevel level)) return;
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (Gear.pieces(player, Gear.SHADOW) == 4)
                speed.addOrUpdateTransientModifier(new AttributeModifier(SHADOW_SPEED, master(player, Gear.SHADOW) ? 0.15 : 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            else speed.removeModifier(SHADOW_SPEED);
        }
        if (master(player, Gear.LUMINITE)) {
            for (Mob m : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(HALO), x -> x.isAlive() && WeaponEvents.dark(x) && x.distanceTo(player) <= HALO)) {
                m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true));
            }
        }
    }

    /** Does the halo keep a held light alive by this player? */
    public static boolean shines(Player player) {
        return master(player, Gear.LUMINITE);
    }

    /** Blows taken by a player: the slip and the bastion. */
    public static void incoming(LivingIncomingDamageEvent event, ServerPlayer victim) {
        if (!(victim.level() instanceof ServerLevel level)) return;
        if (event.getSource().getEntity() instanceof LivingEntity src && WeaponEvents.dark(src) && event.getSource().getDirectEntity() == src
                && master(victim, Gear.SHADOW) && victim.getRandom().nextFloat() < SLIP) {
            event.setCanceled(true);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, victim.getX(), victim.getY() + 1.0, victim.getZ(), 14, 0.4, 0.6, 0.4, 0.02);
            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8F, 1.3F);
            return;
        }
        if (master(victim, Gear.GLOOM)) bastion(level, victim, event.getAmount());
    }

    /** Raises the bastion if this blow would bring the player under the line and it is ready. Returns whether it rose. */
    public static boolean bastion(ServerLevel level, ServerPlayer victim, float amount) {
        if (victim.getHealth() - amount >= victim.getMaxHealth() * BASTION_AT) return false;
        long now = level.getGameTime();
        long last = victim.getPersistentData().getLongOr("umbra_bastion", Long.MIN_VALUE / 2);
        if (now - last < BASTION_COOLDOWN) return false;
        victim.getPersistentData().putLong("umbra_bastion", now);
        victim.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1, false, true));
        victim.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1, false, true));
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 20, 0.5, 0.7, 0.5, 0.1);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 0.5F);
        victim.sendOverlayMessage(Component.translatable("set.umbra.gloom.bastion").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        return true;
    }
}
