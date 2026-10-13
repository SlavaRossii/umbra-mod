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
    }

    @SubscribeEvent
    static void registerGui(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, Identifier.fromNamespaceAndPath(Umbra.MODID, "sanity"), SanityHud::render);
    }
}
