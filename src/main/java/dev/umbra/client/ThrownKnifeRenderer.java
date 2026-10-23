package dev.umbra.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.umbra.gear.ThrownKnife;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * 2.9: the thrown knife, drawn as the 3D knife (not a flat picture turned to the eye): point first along its flight,
 * as an arrow is drawn, and turning slowly about its blade while it flies.
 */
public class ThrownKnifeRenderer extends EntityRenderer<ThrownKnife, ThrownKnifeRenderer.State> {
    public static class State extends EntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public float yRot, xRot, spin;
    }

    public ThrownKnifeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ThrownKnife knife, State state, float partial) {
        super.extractRenderState(knife, state, partial);
        state.yRot = Mth.lerp(partial, knife.yRotO, knife.getYRot());
        state.xRot = Mth.lerp(partial, knife.xRotO, knife.getXRot());
        state.spin = knife.isStuck() ? 0.0F : (knife.tickCount + partial) * 28.0F;
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state.item, knife.getItem(), ItemDisplayContext.NONE, knife.level(), null, knife.getId());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        pose.pushPose();
        // as an arrow: x along the flight
        pose.rotateDegrees(Axis.YP, state.yRot - 90.0F);
        pose.rotateDegrees(Axis.ZP, state.xRot);
        pose.rotateDegrees(Axis.XP, state.spin);
        // the knife is built upright (point up, along y): laid along x, point first
        pose.rotateDegrees(Axis.ZP, -90.0F);
        float s = 0.75F;
        pose.scale(s, s, s);
        pose.translate(0.0F, 0.11F, 0.0F);     // its middle on the entity, not its pommel
        state.item.submit(pose, out, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        super.submit(state, pose, out, camera);
    }
}
