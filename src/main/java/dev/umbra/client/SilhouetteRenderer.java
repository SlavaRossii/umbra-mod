package dev.umbra.client;

import dev.umbra.Umbra;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/** Humanoid figure with a flat texture: the shadow, hallucinations, and future shadow folk. */
public class SilhouetteRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private final Identifier texture;

    public SilhouetteRenderer(EntityRendererProvider.Context context, String textureName) {
        this(context, Identifier.fromNamespaceAndPath(Umbra.MODID, "textures/entity/" + textureName + ".png"));
    }

    /** Any 64×64 humanoid texture, vanilla ones included. */
    public SilhouetteRenderer(EntityRendererProvider.Context context, Identifier texture) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.0F);
        this.texture = texture;
    }

    /** Draws the armour the mob wears (the Doppelganger wears yours). */
    public SilhouetteRenderer<T> withArmor(EntityRendererProvider.Context context) {
        this.addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,
                net.minecraft.client.renderer.entity.ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
                context.getEquipmentRenderer()));
        return this;
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return texture;
    }
}
