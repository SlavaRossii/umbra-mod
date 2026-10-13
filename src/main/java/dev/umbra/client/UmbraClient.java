package dev.umbra.client;

import dev.umbra.Umbra;
import dev.umbra.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Umbra.MODID, value = Dist.CLIENT)
public final class UmbraClient {
    private UmbraClient() {}

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SHADOW.get(), ShadowRenderer::new);
    }
}
