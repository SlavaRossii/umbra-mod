package dev.umbra.madness;

import dev.umbra.sanity.Sanity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Blade of despair: a gloom sword reforged by madness. One point sharper than the gloom sword, and
 * the less sanity you have left, the deeper it cuts: up to +10 damage at zero sanity.
 * (The bonus is applied by the vanilla attack code through {@link #getAttackDamageBonus}.)
 */
public class DespairBladeItem extends Item {
    public static final float MAX_BONUS = 10.0F;

    public DespairBladeItem(Properties properties) {
        super(properties);
    }

    public static float bonus(Player player) {
        if (!Sanity.enabled()) return MAX_BONUS / 2;
        return MAX_BONUS * (Sanity.MAX - Sanity.get(player)) / Sanity.MAX;
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        return source.getEntity() instanceof Player p ? bonus(p) : 0.0F;
    }
}
