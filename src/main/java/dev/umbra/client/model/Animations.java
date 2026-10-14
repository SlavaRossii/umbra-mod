package dev.umbra.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import java.util.function.Function;

/**
 * Animations of Umbra's creatures, written as formulas of time t (ticks), walk phase w and walk speed s.
 * All angles are offsets added to the rest pose from GeneratedModels (radians).
 */
public final class Animations {
    private Animations() {}

    private static final float DEG = Mth.DEG_TO_RAD;

    /** Head follows the look direction. */
    private static void look(ModelPart head, UmbraRenderState st, float scale) {
        head.yRot += st.yRot * DEG * scale;
        head.xRot += st.xRot * DEG * scale;
    }

    /** Biped walk: legs swing in opposition, arms opposite to legs. */
    private static void walk(Function<String, ModelPart> p, UmbraRenderState st, float legAmp, float armAmp) {
        float w = st.walkAnimationPos, s = Math.min(1.0F, st.walkAnimationSpeed);
        float swing = Mth.cos(w * 0.6662F) * s;
        p.apply("leg_r").xRot += swing * legAmp;
        p.apply("leg_l").xRot -= swing * legAmp;
        if (armAmp != 0) {
            p.apply("arm_r").xRot -= swing * armAmp;
            p.apply("arm_l").xRot += swing * armAmp;
        }
    }

    /** Overhead two-handed strike driven by the attack swing (0..1). */
    private static void slam(Function<String, ModelPart> p, UmbraRenderState st, float amp) {
        if (st.attack <= 0) return;
        float a = Mth.sin(st.attack * Mth.PI);
        p.apply("arm_r").xRot -= a * amp;
        p.apply("arm_l").xRot -= a * amp;
    }

    // ---------------------------------------------------------------- creatures

    public static void crawler(Function<String, ModelPart> p, UmbraRenderState st) {
        look(p.apply("head"), st, 1.0F);
        float w = st.walkAnimationPos * 1.3F, s = Math.min(1.0F, st.walkAnimationSpeed);
        for (int i = 0; i < 3; i++) {
            // tripod gait: legs r0, l1, r2 move together, the others in counter-phase
            float phaseR = (i % 2 == 0) ? 0 : Mth.PI;
            float phaseL = phaseR + Mth.PI;
            ModelPart r = p.apply("leg_r" + i + "_upper"), l = p.apply("leg_l" + i + "_upper");
            r.yRot += Mth.cos(w + phaseR) * 0.4F * s;
            l.yRot -= Mth.cos(w + phaseL) * 0.4F * s;
            r.zRot += Math.abs(Mth.sin(w + phaseR)) * 0.3F * s;
            l.zRot -= Math.abs(Mth.sin(w + phaseL)) * 0.3F * s;
        }
        p.apply("body").y += Mth.sin(st.ageInTicks * 0.15F) * 0.3F;
        if (st.attack > 0) p.apply("head").xRot -= Mth.sin(st.attack * Mth.PI) * 0.5F;
    }

    public static void blind(Function<String, ModelPart> p, UmbraRenderState st) {
        walk(p, st, 1.0F, 0.4F);
        ModelPart head = p.apply("head");
        look(head, st, 0.6F);
        // listening: the head twitches now and then
        float t = st.ageInTicks;
        if (((int) (t / 30)) % 3 == 0) head.zRot += Mth.sin(t * 1.7F) * 0.12F;
        p.apply("arm_r").zRot += Mth.sin(t * 0.05F) * 0.05F;
        p.apply("arm_l").zRot -= Mth.sin(t * 0.05F) * 0.05F;
        slam(p, st, 1.6F);
    }

    public static void lightEater(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks;
        float speed = st.flag ? 1.9F : 1.1F;   // charging: frantic wings
        float flap = Mth.sin(t * speed) * 0.85F;
        p.apply("wing_r").zRot += flap;
        p.apply("wing_l").zRot -= flap;
        p.apply("wing_r2").zRot += flap * 0.7F;
        p.apply("wing_l2").zRot -= flap * 0.7F;
        p.apply("body").y += Mth.sin(t * 0.2F) * 1.0F;
        p.apply("antenna_r").xRot += Mth.sin(t * 0.3F) * 0.15F;
        p.apply("antenna_l").xRot += Mth.sin(t * 0.3F + 1) * 0.15F;
        look(p.apply("head"), st, 0.5F);
    }

    /** Lurker: a slow travelling wave down the body; the jaw gapes when it bites; the lure sways. */
    public static void lurker(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks, s = 0.5F + Math.min(1.0F, st.walkAnimationSpeed);
        p.apply("body1").yRot += Mth.sin(t * 0.18F * s) * 0.15F;
        p.apply("body2").yRot += Mth.sin(t * 0.18F * s - 0.9F) * 0.3F;
        p.apply("body3").yRot += Mth.sin(t * 0.18F * s - 1.8F) * 0.4F;
        p.apply("fin").yRot += Mth.sin(t * 0.18F * s - 2.6F) * 0.5F;
        p.apply("head").yRot += Mth.sin(t * 0.18F * s + 0.9F) * 0.1F;
        p.apply("fin_r").zRot += Mth.sin(t * 0.3F) * 0.3F;
        p.apply("fin_l").zRot -= Mth.sin(t * 0.3F) * 0.3F;
        p.apply("stalk").xRot += Mth.sin(t * 0.1F) * 0.15F;
        p.apply("jaw").xRot += 0.15F + (st.attack > 0 ? Mth.sin(st.attack * Mth.PI) * 0.8F : 0);
        look(p.apply("head"), st, 0.4F);
    }

    public static void shade(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks;
        p.apply("core").y += Mth.sin(t * 0.18F) * 1.2F;
        look(p.apply("core"), st, 0.7F);
        p.apply("tail1").zRot += Mth.sin(t * 0.25F) * 0.25F;
        p.apply("tail2").zRot += Mth.sin(t * 0.25F - 0.8F) * 0.3F;
        p.apply("tail3").zRot += Mth.sin(t * 0.25F - 1.6F) * 0.35F;
        p.apply("arm_r").zRot += Mth.sin(t * 0.2F) * 0.2F;
        p.apply("arm_l").zRot -= Mth.sin(t * 0.2F) * 0.2F;
        if (st.attack > 0) {
            float a = Mth.sin(st.attack * Mth.PI);
            p.apply("arm_r").xRot -= a * 1.5F;
            p.apply("arm_l").xRot -= a * 1.5F;
        }
    }

    /** Statue: frozen in one of three poses; the pose changes whenever it has moved (variant from position). */
    public static void statue(Function<String, ModelPart> p, UmbraRenderState st) {
        ModelPart r = p.apply("arm_r"), l = p.apply("arm_l"), head = p.apply("head");
        switch (st.variant) {
            case 0 -> { // weeping: hands over the face
                r.xRot -= 2.4F; r.zRot += 0.5F;
                l.xRot -= 2.4F; l.zRot -= 0.5F;
                head.xRot += 0.35F;
            }
            case 1 -> { // reaching for you
                r.xRot -= 1.5F; l.xRot -= 1.4F;
                head.xRot -= 0.1F;
            }
            default -> { // pointing, head tilted
                r.xRot -= 1.6F; r.yRot -= 0.3F;
                l.xRot += 0.1F;
                head.zRot += 0.35F;
            }
        }
    }

    public static void keeper(Function<String, ModelPart> p, UmbraRenderState st) {
        walk(p, st, 0.8F, 0.5F);
        look(p.apply("head"), st, 0.8F);
        slam(p, st, 2.4F);
        p.apply("body").xRot += st.attack > 0 ? Mth.sin(st.attack * Mth.PI) * 0.25F : 0;
    }

    public static void watcher(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks;
        ModelPart eye = p.apply("eye");
        eye.y += Mth.sin(t * 0.1F) * 1.5F;
        look(eye, st, 1.0F);
        for (int i = 0; i < 4; i++) {
            float ph = i * 1.57F;
            p.apply("tentacle" + i + "_a").xRot += Mth.sin(t * 0.12F + ph) * 0.25F;
            p.apply("tentacle" + i + "_a").zRot += Mth.cos(t * 0.1F + ph) * 0.2F;
            p.apply("tentacle" + i + "_b").xRot += Mth.sin(t * 0.12F + ph - 0.7F) * 0.3F;
            p.apply("tentacle" + i + "_c").xRot += Mth.sin(t * 0.12F + ph - 1.4F) * 0.4F;
        }
        // the stare: the eye swells while it attacks
        float k = 1.0F + (st.attack > 0 ? Mth.sin(st.attack * Mth.PI) * 0.15F : 0);
        eye.xScale = eye.yScale = eye.zScale = k;
    }

    public static void mimicQueen(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks;
        // the lid breathes, and gapes wide when she bites
        float open = 0.12F + Mth.sin(t * 0.08F) * 0.1F + (st.attack > 0 ? Mth.sin(st.attack * Mth.PI) * 0.9F : 0);
        p.apply("lid").xRot -= open;
        p.apply("tongue").xRot += Mth.sin(t * 0.3F) * 0.1F;
        float w = st.walkAnimationPos * 1.2F, s = Math.min(1.0F, st.walkAnimationSpeed);
        for (int i = 0; i < 2; i++) {
            float ph = i * Mth.PI;
            p.apply("leg_r" + i + "_upper").yRot += Mth.cos(w + ph) * 0.4F * s;
            p.apply("leg_l" + i + "_upper").yRot -= Mth.cos(w + ph + Mth.PI) * 0.4F * s;
        }
        p.apply("body").y += Math.abs(Mth.sin(w)) * -1.0F * s;
    }

    public static void shepherd(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks;
        look(p.apply("head"), st, 0.8F);
        p.apply("skirt").xRot += Mth.sin(st.walkAnimationPos * 0.6F) * 0.08F * Math.min(1.0F, st.walkAnimationSpeed);
        p.apply("skirt").zRot += Mth.sin(t * 0.07F) * 0.03F;
        p.apply("arm_l").xRot += Mth.sin(t * 0.06F) * 0.05F;
        if (st.attack > 0) {
            float a = Mth.sin(st.attack * Mth.PI);
            p.apply("arm_r").xRot -= a * 1.8F;   // the crook comes down
        }
    }

    public static void architect(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks;
        walk(p, st, 0.9F, 0.5F);
        look(p.apply("head"), st, 0.9F);
        ModelPart orbit = p.apply("orbit");
        orbit.yRot += t * 0.06F;
        for (int i = 0; i < 4; i++) {
            ModelPart c = p.apply("cube" + i);
            c.y += Mth.sin(t * 0.1F + i * 1.57F) * 2.0F;
            c.xRot += t * 0.08F;
            c.zRot += t * 0.05F;
        }
        slam(p, st, 2.0F);
    }

    public static void primordialShadow(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks;
        walk(p, st, 0.7F, 0.4F);
        look(p.apply("head"), st, 0.8F);
        // the wings open wider as she weakens (three phases)
        int phase = st.health > 0.66F ? 1 : st.health > 0.33F ? 2 : 3;
        float spread = (phase - 1) * 0.35F;
        float beat = Mth.sin(t * (0.08F + phase * 0.03F)) * (0.15F + phase * 0.05F);
        p.apply("wing_r").yRot -= spread + beat;
        p.apply("wing_l").yRot += spread + beat;
        p.apply("wing_r_tip").zRot += Mth.sin(t * 0.1F) * 0.15F;
        p.apply("wing_l_tip").zRot -= Mth.sin(t * 0.1F) * 0.15F;
        p.apply("body").y += Mth.sin(t * 0.1F) * 0.5F;
        slam(p, st, 1.8F);
    }

    public static void shadowVillager(Function<String, ModelPart> p, UmbraRenderState st) {
        look(p.apply("head"), st, 1.0F);
        p.apply("legs").xRot += Mth.cos(st.walkAnimationPos * 0.6662F) * 0.15F * Math.min(1.0F, st.walkAnimationSpeed);
        // nodding while trading
        if (st.flag) p.apply("head").xRot += Mth.sin(st.ageInTicks * 0.4F) * 0.15F;
    }
}
