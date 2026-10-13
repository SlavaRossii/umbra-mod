package dev.umbra.client;

import dev.umbra.Umbra;
import dev.umbra.mob.LightEater;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.vex.VexModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.VexRenderState;
import net.minecraft.resources.Identifier;

/** A pale moth-spirit: the vex body with its own texture. */
public class LightEaterRenderer extends MobRenderer<LightEater, VexRenderState, VexModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Umbra.MODID, "textures/entity/light_eater.png");

    public LightEaterRenderer(EntityRendererProvider.Context context) {
        super(context, new VexModel(context.bakeLayer(ModelLayers.VEX)), 0.2F);
    }

    @Override
    public VexRenderState createRenderState() {
        return new VexRenderState();
    }

    @Override
    public void extractRenderState(LightEater entity, VexRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isCharging = entity.isCharging();
    }

    @Override
    public Identifier getTextureLocation(VexRenderState state) {
        return TEXTURE;
    }
}
