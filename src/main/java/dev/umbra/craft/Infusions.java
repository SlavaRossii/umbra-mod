package dev.umbra.craft;

import dev.umbra.gear.EnchantEvents;
import dev.umbra.gear.WeaponEvents;
import dev.umbra.registry.ModComponents;
import dev.umbra.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 2.6: a boss's relic (its trophy) infused into a melee weapon at the gloom anvil. One relic per weapon; a new one
 * replaces the old. What each does on a hit:
 * <ul>
 *   <li>Watcher: the eye pins it: Slowness II for 3 s.</li>
 *   <li>Mimic Queen: hungry: you get back a sixth of the damage as health.</li>
 *   <li>Shepherd: the flock turns: creatures of the dark round the struck one go for it.</li>
 *   <li>Doppelganger: one hit in four is repeated by your reflection, half as hard.</li>
 *   <li>Architect: one hit in four roots it to the ground for a second and a half.</li>
 *   <li>Primordial Shadow: in the dark (light 4 or less) a third harder against the things of the dark.</li>
 * </ul>
 */
public final class Infusions {
    private Infusions() {}

    public static final TagKey<Item> MELEE = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, "enchantable/melee"));

    /** The boss whose relic this is, or null. */
    public static String relicOf(ItemStack stack) {
        for (var e : ModItems.TROPHIES.entrySet()) if (stack.is(e.getValue().get())) return e.getKey();
        return null;
    }

    /** Can this take a relic? Melee weapons. */
    public static boolean takes(ItemStack stack) {
        return !stack.isEmpty() && stack.is(MELEE);
    }

    public static String of(ItemStack stack) {
        String s = stack.get(ModComponents.INFUSION.get());
        return s == null ? "" : s;
    }

    /** Damage before armour, changed by the relic (only the Primordial Shadow's changes it). */
    public static float modify(ServerPlayer player, LivingEntity target, float amount) {
        if ("primordial_shadow".equals(of(player.getMainHandItem())) && WeaponEvents.dark(target)
                && player.level().getMaxLocalRawBrightness(player.blockPosition()) <= 4) return amount * 1.33F;
        return amount;
    }

    /** After a blow has landed. Called inside WeaponEvents' guard, so the extra blows do not loop. */
    public static void afterHit(ServerLevel level, ServerPlayer player, LivingEntity target, float dealt) {
        String relic = of(player.getMainHandItem());
        if (relic.isEmpty() || dealt <= 0) return;
        var r = player.getRandom();
        switch (relic) {
            case "watcher" -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
            case "mimic_queen" -> player.heal(dealt / 6.0F);
            case "shepherd" -> {
                for (Mob m : level.getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(6.0), x -> x != target && x.isAlive() && WeaponEvents.dark(x)
                        && !(x instanceof dev.umbra.boss.UmbraBoss))) {
                    m.setTarget(target);
                }
                level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 1.0, target.getZ(), 6, 0.6, 0.4, 0.6, 0.02);
            }
            case "doppelganger" -> {
                if (r.nextInt(4) == 0) {
                    EnchantEvents.later(level, target, player, 11, dealt * 0.5F);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
                }
            }
            case "architect" -> {
                if (r.nextInt(4) == 0) {
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 9));
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.DEEPSLATE_PLACE, SoundSource.PLAYERS, 1.0F, 0.6F);
                    level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.1, target.getZ(), 10, 0.4, 0.05, 0.4, 0.05);
                }
            }
            default -> { }
        }
    }
}
