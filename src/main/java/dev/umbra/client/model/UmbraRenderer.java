package dev.umbra.client.model;

import dev.umbra.Umbra;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

import java.util.function.BiConsumer;
import java.util.function.Function;

/** Renderer for a mob with one of Umbra's generated models. */
public class UmbraRenderer<T extends Mob> extends MobRenderer<T, UmbraRenderState, UmbraModel> {
    private final Identifier texture;
    private final BiConsumer<T, UmbraRenderState> extract;

    public UmbraRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, String texture, float shadow,
                         BiConsumer<Function<String, ModelPart>, UmbraRenderState> animator, BiConsumer<T, UmbraRenderState> extract) {
        super(context, new UmbraModel(context.bakeLayer(layer), animator), shadow);
        this.texture = Identifier.fromNamespaceAndPath(Umbra.MODID, "textures/entity/" + texture + ".png");
        this.extract = extract;
    }

    @Override
    public UmbraRenderState createRenderState() {
        return new UmbraRenderState();
    }

    @Override
    public void extractRenderState(T entity, UmbraRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.attack = entity.getAttackAnim(partialTick);
        state.health = entity.getMaxHealth() <= 0 ? 1.0F : entity.getHealth() / entity.getMaxHealth();
        if (extract != null) extract.accept(entity, state);
    }

    @Override
    public Identifier getTextureLocation(UmbraRenderState state) {
        return texture;
    }
}
