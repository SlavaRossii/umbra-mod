package dev.umbra.client.model;

import dev.umbra.Umbra;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

/** Model layer ids of Umbra's creatures. */
public final class ModLayers {
    public static final ModelLayerLocation CRAWLER = layer("crawler");
    public static final ModelLayerLocation BLIND = layer("blind");
    public static final ModelLayerLocation LIGHT_EATER = layer("light_eater");
    public static final ModelLayerLocation SHADE = layer("shade");
    public static final ModelLayerLocation STATUE = layer("citadel_statue");
    public static final ModelLayerLocation KEEPER = layer("citadel_keeper");
    public static final ModelLayerLocation WATCHER = layer("watcher");
    public static final ModelLayerLocation MIMIC_QUEEN = layer("mimic_queen");
    public static final ModelLayerLocation SHEPHERD = layer("shepherd");
    public static final ModelLayerLocation ARCHITECT = layer("architect");
    public static final ModelLayerLocation PRIMORDIAL_SHADOW = layer("primordial_shadow");
    public static final ModelLayerLocation SHADOW_VILLAGER = layer("shadow_villager");

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(Umbra.MODID, name), "main");
    }

    private ModLayers() {}
}
