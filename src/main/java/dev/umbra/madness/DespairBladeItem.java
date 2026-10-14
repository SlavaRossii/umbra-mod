package dev.umbra.madness;

import dev.umbra.sanity.Sanity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Blade of despair: the less sanity you have left, the deeper it cuts (up to +10 damage). */
public class DespairBladeItem extends Item {
    public static final float MAX_BONUS = 10.0F;

    public DespairBladeItem(Properties properties) {
        super(properties);
    }

    public static float bonus(Player player) {
        if (!Sanity.enabled()) return MAX_BONUS / 2;
        return MAX_BONUS * (Sanity.MAX - Sanity.get(player)) / Sanity.MAX;
    }

    public static void onHurt(LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof Player p && p.getMainHandItem().getItem() instanceof DespairBladeItem) {
            event.setAmount(event.getAmount() + bonus(p));
        }
    }
}
