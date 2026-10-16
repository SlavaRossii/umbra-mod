package dev.umbra.client;

import dev.umbra.Umbra;
import dev.umbra.util.Mirrored;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

import java.util.UUID;

/**
 * A creature in a player's own shape: the player's skin, darkened to a shadow (with white eyes; violet for a freed
 * shadow), or barely changed and pale for the Doppelganger (with red eyes). Falls back to a plain texture if the
 * player's skin is unknown (offline, or nobody with that face is online).
 */
public class MirrorRenderer<T extends Mob & Mirrored> extends HumanoidMobRenderer<T, MirrorRenderer.State, HumanoidModel<MirrorRenderer.State>> {
    public static class State extends HumanoidRenderState {
        public Identifier skin;
        public int kind;
    }

    private static final int[] TINT = {0xFF1C1626, 0xFF2C1E44, 0xFFB4AEC0};
    private static final Identifier[] EYES = {tex("mirror_eyes_shadow"), tex("mirror_eyes_ally"), tex("mirror_eyes_double")};
    private final Identifier fallback;

    public MirrorRenderer(EntityRendererProvider.Context context, String fallback) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.0F);
        this.fallback = tex(fallback);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return RenderTypes.eyes(current);
            }

            @Override
            public void submit(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector out, int light, State state, float yRot, float xRot) {
                current = EYES[Math.max(0, Math.min(2, state.kind))];
                super.submit(pose, out, light, state, yRot, xRot);
            }
        });
    }

    private static Identifier current = tex("mirror_eyes_shadow");

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(Umbra.MODID, "textures/entity/" + name + ".png");
    }

    /** Draws the armour the mob wears (the Doppelganger wears yours). */
    public MirrorRenderer<T> withArmor(EntityRendererProvider.Context context) {
        this.addLayer(new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,
                net.minecraft.client.renderer.entity.ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
                context.getEquipmentRenderer()));
        return this;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.kind = entity.mirrorKind();
        state.skin = skinOf(entity.mirrorOf());
    }

    private Identifier skinOf(UUID id) {
        if (id == null) return fallback;
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            var info = connection.getPlayerInfo(id);
            if (info != null) return info.getSkin().body().texturePath();
        }
        return net.minecraft.client.resources.DefaultPlayerSkin.get(id).body().texturePath();
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return state.skin != null ? state.skin : fallback;
    }

    @Override
    protected int getModelTint(State state) {
        return TINT[Math.max(0, Math.min(2, state.kind))];
    }
}
