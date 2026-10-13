package dev.umbra.light;

import net.minecraft.world.item.ArrowItem;

/** Light arrows burst like a small Dispeller flash where they land. The burst is handled in {@link #onImpact}. */
public class LightArrowItem extends ArrowItem {
    public static final double BURST = 6.0;

    public LightArrowItem(Properties properties) {
        super(properties);
    }

    public static void onImpact(net.neoforged.neoforge.event.entity.ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow)) return;
        if (!(arrow.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        if (!(arrow.getPickupItemStackOrigin().getItem() instanceof LightArrowItem)) return;
        var at = event.getRayTraceResult().getLocation();
        DispellerItem.flash(level, null, at, arrow.getDeltaMovement().lengthSqr() > 0 ? arrow.getDeltaMovement() : new net.minecraft.world.phys.Vec3(0, -1, 0), BURST, -1.0);
    }
}
