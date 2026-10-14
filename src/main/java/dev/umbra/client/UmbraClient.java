package dev.umbra.client;

import dev.umbra.Umbra;
import dev.umbra.registry.ModEntities;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Umbra.MODID, value = Dist.CLIENT)
public final class UmbraClient {
    private UmbraClient() {}

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SHADOW.get(), ctx -> new SilhouetteRenderer<>(ctx, "shadow"));
        event.registerEntityRenderer(ModEntities.HALLUCINATION.get(), ctx -> new SilhouetteRenderer<>(ctx, "hallucination"));
        event.registerEntityRenderer(ModEntities.BLIND.get(), ctx -> new SilhouetteRenderer<>(ctx, "blind"));
        event.registerEntityRenderer(ModEntities.MIMICKER.get(), ctx -> new SilhouetteRenderer<>(ctx, "mimicker"));
        event.registerEntityRenderer(ModEntities.SHADE.get(), ctx -> new SilhouetteRenderer<>(ctx, "shade"));
        event.registerEntityRenderer(ModEntities.SHADOW_VILLAGER.get(), ctx -> new SilhouetteRenderer<>(ctx, "shadow_villager"));
        event.registerEntityRenderer(ModEntities.CRAWLER.get(), CrawlerRenderer::new);
        event.registerEntityRenderer(ModEntities.WATCHER.get(), ctx -> new SilhouetteRenderer<>(ctx, "watcher"));
        event.registerEntityRenderer(ModEntities.MIMIC_QUEEN.get(), ctx -> new SilhouetteRenderer<>(ctx, "mimic_queen"));
        event.registerEntityRenderer(ModEntities.SHEPHERD.get(), ctx -> new SilhouetteRenderer<>(ctx, "shepherd"));
        event.registerEntityRenderer(ModEntities.DOPPELGANGER.get(), ctx -> new SilhouetteRenderer<>(ctx, "doppelganger"));
        event.registerEntityRenderer(ModEntities.ARCHITECT.get(), ctx -> new SilhouetteRenderer<>(ctx, "architect"));
        event.registerEntityRenderer(ModEntities.PRIMORDIAL_SHADOW.get(), ctx -> new SilhouetteRenderer<>(ctx, "primordial_shadow"));
        event.registerEntityRenderer(ModEntities.LIGHT_EATER.get(), LightEaterRenderer::new);
    }

    @SubscribeEvent
    static void registerGui(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, Identifier.fromNamespaceAndPath(Umbra.MODID, "sanity"), SanityHud::render);
    }
}
