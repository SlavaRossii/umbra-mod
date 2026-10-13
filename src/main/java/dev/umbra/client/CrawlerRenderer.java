package dev.umbra.client;

import dev.umbra.Umbra;
import dev.umbra.mob.Crawler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class CrawlerRenderer extends SpiderRenderer<Crawler> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Umbra.MODID, "textures/entity/crawler.png");

    public CrawlerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
