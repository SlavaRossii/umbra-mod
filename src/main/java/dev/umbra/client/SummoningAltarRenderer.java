package dev.umbra.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.umbra.altar.SummoningAltarBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 2.8: what lies on the sacrificial altar. The seal hangs over the dish and slowly turns; the offerings lie round it in
 * the dish. While the rite runs the seal rises and spins faster and faster, and the offerings shrink away as they burn.
 */
public class SummoningAltarRenderer implements BlockEntityRenderer<SummoningAltarBlockEntity, SummoningAltarRenderer.State> {
    public static final int RITE_LENGTH_GUESS = 180;
    private final ItemModelResolver items;

    public SummoningAltarRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState seal = new ItemStackRenderState();
        public final ItemStackRenderState[] offerings = new ItemStackRenderState[SummoningAltarBlockEntity.SLOTS - 1];
        public int offered;
        public float time;
        /** 0..1 through the rite, or -1. */
        public float rite = -1.0F;

        public State() {
            for (int i = 0; i < offerings.length; i++) offerings[i] = new ItemStackRenderState();
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SummoningAltarBlockEntity altar, State s, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(altar, s, partialTick, camera, crumbling);
        var level = altar.getLevel();
        int seed = (int) altar.getBlockPos().asLong();
        items.updateForTopItem(s.seal, altar.seal(), ItemDisplayContext.FIXED, level, null, seed);
        List<ItemStack> list = altar.offerings();
        s.offered = Math.min(list.size(), s.offerings.length);
        for (int i = 0; i < s.offered; i++) items.updateForTopItem(s.offerings[i], list.get(i), ItemDisplayContext.FIXED, level, null, seed + i + 1);
        s.time = level == null ? 0 : (level.getGameTime() % 24000L) + partialTick;
        var ritual = altar.ritual();
        if (altar.riteRunning() && level != null && ritual != null) {
            s.rite = Mth.clamp((level.getGameTime() - altar.riteStart() + partialTick) / ritual.length, 0.0F, 1.0F);
        } else {
            s.rite = -1.0F;
        }
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        float rite = Math.max(0.0F, s.rite);
        // the offerings, lying round the seal in the dish
        for (int i = 0; i < s.offered; i++) {
            if (s.offerings[i].isEmpty()) continue;
            float a = (float) (i * Math.PI * 2 / Math.max(3, s.offered)) + 0.4F;
            float burn = s.rite >= 0 ? 1.0F - rite : 1.0F;
            if (burn <= 0.02F) continue;
            pose.pushPose();
            pose.translate(0.5F + Mth.cos(a) * 0.3F, 0.83F + rite * 0.25F, 0.5F + Mth.sin(a) * 0.3F);
            pose.rotate(Axis.YP, -a);
            pose.rotateDegrees(Axis.XP, 90.0F);
            pose.scale(0.24F * burn, 0.24F * burn, 0.24F * burn);
            s.offerings[i].submit(pose, out, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        // the seal hangs over the dish and turns; the rite lifts it and spins it up
        if (!s.seal.isEmpty()) {
            float spin = s.time * 2.0F + (s.rite >= 0 ? rite * rite * 2200.0F : 0.0F);
            float y = 1.12F + Mth.sin(s.time * 0.08F) * 0.04F + rite * 0.45F;
            pose.pushPose();
            pose.translate(0.5F, y, 0.5F);
            pose.rotateDegrees(Axis.YP, spin);
            float k = 0.5F + rite * 0.15F;
            pose.scale(k, k, k);
            int light = s.rite >= 0 ? 0xF000F0 : s.lightCoords;
            s.seal.submit(pose, out, light, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }
}
