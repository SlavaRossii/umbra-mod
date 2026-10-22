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

    /** Adds a layer drawn at full brightness from {@code textures/entity/<texture>_glow.png}: what shines in the dark. */
    public UmbraRenderer<T> glowing(String texture) {
        Identifier glow = Identifier.fromNamespaceAndPath(Umbra.MODID, "textures/entity/" + texture + "_glow.png");
        this.addLayer(new net.minecraft.client.renderer.entity.layers.EyesLayer<>(this) {
            @Override
            public net.minecraft.client.renderer.rendertype.RenderType renderType() {
                return net.minecraft.client.renderer.rendertype.RenderTypes.eyes(glow);
            }

            @Override
            public void submit(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector out, int light,
                               UmbraRenderState state, float yRot, float xRot) {
                if (!state.hidden) super.submit(pose, out, light, state, yRot, xRot);
            }
        });
        return this;
    }

    @Override
    public UmbraRenderState createRenderState() {
        return new UmbraRenderState();
    }

    @Override
    public void extractRenderState(T entity, UmbraRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.attack = entity.getSwingAnimation(partialTick);
        state.hidden = entity.isInvisible();
        state.health = entity.getMaxHealth() <= 0 ? 1.0F : entity.getHealth() / entity.getMaxHealth();
        state.hurt = entity.hurtTime > 0 ? Math.max(0.0F, (entity.hurtTime - partialTick) / 10.0F) : 0.0F;
        state.boss = entity instanceof dev.umbra.boss.UmbraBoss;
        if (entity instanceof dev.umbra.boss.UmbraBoss b) {
            state.action = b.action();
            state.actionAge = b.actionAge(partialTick);
        } else {
            state.action = 0;
            state.actionAge = 1000.0F;
        }
        if (extract != null) extract.accept(entity, state);
    }

    /**
     * 2.10: a boss does not tip over when it dies: it sags, sinks into the ground and shrinks away into its smoke
     * (the smoke is the server's, see UmbraBoss.tickDeath).
     */
    @Override
    protected void setupRotations(UmbraRenderState state, com.mojang.blaze3d.vertex.PoseStack pose, float bodyRot, float scale) {
        if (!state.boss || state.deathTime <= 0) {
            super.setupRotations(state, pose, bodyRot, scale);
            return;
        }
        float dying = state.deathTime;
        state.deathTime = 0.0F;
        super.setupRotations(state, pose, bodyRot, scale);
        state.deathTime = dying;
        float t = Math.min(1.0F, dying / dev.umbra.boss.UmbraBoss.DEATH_TICKS);
        float shake = net.minecraft.util.Mth.sin(dying * 2.1F) * 0.04F * (1.0F - t);
        pose.translate(shake, -t * t * 0.9F, 0.0F);
        pose.rotateDegrees(com.mojang.math.Axis.XP, t * 18.0F);
        float k = 1.0F - 0.55F * t * t;
        pose.scale(k, k, k);
    }

    @Override
    public Identifier getTextureLocation(UmbraRenderState state) {
        return texture;
    }
}
