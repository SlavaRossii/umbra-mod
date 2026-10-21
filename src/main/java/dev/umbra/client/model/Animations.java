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

    /**
     * 2.7.6: the Queen walks like a spider, three legs at a time; her lid breathes and gapes when she bites; the tongue
     * sways its golden key before her and whips out on a bite; the little chests on her back snap their lids each to
     * its own rhythm; the eye in her lock follows you (it is in the lid, which turns with her look a little).
     */
    public static void mimicQueen(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks;
        float w = st.walkAnimationPos * 1.2F, s = Math.min(1.0F, st.walkAnimationSpeed);
        float bite = st.attack > 0 ? Mth.sin(st.attack * Mth.PI) : 0;
        p.apply("lid").xRot -= 0.1F + Mth.sin(t * 0.08F) * 0.08F + bite * 1.0F;
        p.apply("lid").yRot += st.yRot * DEG * 0.1F;
        ModelPart ta = p.apply("tongue_a"), tb = p.apply("tongue_b");
        ta.yRot += Mth.sin(t * 0.07F) * 0.15F;
        tb.yRot += Mth.sin(t * 0.07F - 0.7F) * 0.2F;
        tb.xRot += Mth.sin(t * 0.11F) * 0.08F - bite * 0.3F;
        ta.xRot -= bite * 0.4F;
        p.apply("lure").xRot += Mth.sin(t * 0.13F) * 0.25F;
        p.apply("lure").zRot += Mth.cos(t * 0.09F) * 0.15F;
        for (int i = 0; i < 5; i++) {
            float snap = Math.max(0.0F, Mth.sin(t * (0.12F + i * 0.03F) + i * 1.9F));
            p.apply("brood_" + i + "_lid").xRot -= snap * snap * 0.7F;
        }
        p.apply("brood").y += Mth.sin(t * 0.1F) * 0.3F;
        for (int i = 0; i < 3; i++) {
            float ph = (i % 2) * Mth.PI;
            float lift = Math.max(0.0F, Mth.sin(w + ph)) * 0.35F * s;
            float swing = Mth.cos(w + ph) * 0.35F * s;
            p.apply("leg_r" + i + "_upper").yRot += swing;
            p.apply("leg_r" + i + "_upper").zRot += lift;
            p.apply("leg_l" + i + "_upper").yRot += swing;
            p.apply("leg_l" + i + "_upper").zRot -= Math.max(0.0F, Mth.sin(w + ph + Mth.PI)) * 0.35F * s;
        }
        ModelPart body = p.apply("body");
        body.y += Math.abs(Mth.sin(w)) * -0.8F * s + Mth.sin(t * 0.08F) * 0.3F;
        body.xRot += bite * 0.15F;
    }

    /**
     * 2.7.5: the Shepherd hangs above the ground and sways; the torn hem and the strips of his mantle drift; the bell
     * swings; his flock circles low around him. On a strike the scythe goes round in a wide flat sweep.
     */
    public static void shepherd(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks, s = Math.min(1.0F, st.walkAnimationSpeed);
        look(p.apply("head"), st, 0.8F);
        float hover = Mth.sin(t * 0.07F) * 0.8F;
        ModelPart body = p.apply("body"), skirt = p.apply("skirt");
        body.y += hover;
        skirt.y += hover;
        body.xRot += s * 0.15F;
        skirt.xRot += s * 0.2F + Mth.sin(t * 0.05F) * 0.03F;
        skirt.zRot += Mth.sin(t * 0.06F) * 0.03F;
        for (int i = 0; i < 8; i++) {
            ModelPart tt = p.apply("tatter_" + i);
            tt.xRot += Mth.sin(t * 0.11F + i * 0.8F) * 0.18F + s * 0.35F;
            tt.zRot += Mth.cos(t * 0.09F + i * 1.1F) * 0.1F;
        }
        for (int i = 0; i < 3; i++) {
            ModelPart c = p.apply("cape_" + i);
            c.xRot += Mth.sin(t * 0.08F + i) * 0.08F + s * 0.4F;
            c.zRot += Mth.cos(t * 0.07F + i * 1.3F) * 0.05F;
        }
        ModelPart bell = p.apply("bell");
        bell.xRot += Mth.sin(t * 0.12F) * 0.15F - s * 0.3F;
        bell.zRot += Mth.cos(t * 0.1F) * 0.1F;
        p.apply("arm_l").xRot += Mth.sin(t * 0.06F) * 0.06F;
        p.apply("finger_l_0").xRot -= Mth.sin(t * 0.1F) * 0.15F;
        p.apply("finger_l_1").xRot -= Mth.sin(t * 0.1F + 0.6F) * 0.15F;
        if (st.attack > 0) {
            float a = Mth.sin(st.attack * Mth.PI);
            ModelPart arm = p.apply("arm_r");
            arm.xRot -= a * 1.3F;
            arm.zRot += a * 0.6F;
            body.yRot += (st.attack - 0.5F) * 1.6F * a;
            p.apply("scythe").xRot += a * 0.5F;
        }
        p.apply("herd").yRot += t * 0.04F;
        for (int i = 0; i < 4; i++) p.apply("wisp_" + i).y += Mth.sin(t * 0.15F + i * 1.6F) * 1.0F;
    }

    /**
     * 2.7.4: the Architect, a building that walks. Heavy steps; the mallet comes down from overhead on a strike while
     * the dividers open; the keystone in his chest turns; the arch behind his head rises and settles; the plumb bobs
     * swing on their chains; pieces of the maze's walls go round him, turning over, faster as he weakens.
     */
    public static void architect(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks, s = Math.min(1.0F, st.walkAnimationSpeed);
        int phase = st.health > 0.66F ? 1 : st.health > 0.33F ? 2 : 3;
        walk(p, st, 0.7F, 0.35F);
        look(p.apply("head"), st, 0.5F);
        ModelPart body = p.apply("body");
        body.zRot += Mth.cos(st.walkAnimationPos * 0.6662F) * s * 0.05F;
        body.y += Math.abs(Mth.cos(st.walkAnimationPos * 0.6662F)) * s * 0.8F;
        p.apply("keystone").zRot += t * 0.05F;
        p.apply("arch").y += Mth.sin(t * 0.06F) * 0.6F;
        for (int i = 0; i < 3; i++) {
            ModelPart pl = p.apply("plumb_" + i);
            pl.xRot += Mth.sin(t * 0.09F + i * 1.3F) * 0.12F + s * 0.25F;
            pl.zRot += Mth.cos(t * 0.07F + i * 0.9F) * 0.08F;
        }
        float open = 0.05F + Mth.sin(t * 0.05F) * 0.04F;
        if (st.attack > 0) {
            float a = Mth.sin(st.attack * Mth.PI);
            p.apply("arm_r").xRot -= a * 2.6F;
            p.apply("fore_r").xRot += a * 0.4F;
            p.apply("arm_l").xRot -= a * 0.9F;
            open += a * 0.35F;
            body.xRot += a * 0.15F;
        }
        p.apply("divider_a").zRot += open;
        p.apply("divider_b").zRot -= open;
        p.apply("orbit").yRot += t * (0.03F + phase * 0.015F);
        for (int i = 0; i < 5; i++) {
            ModelPart f = p.apply("fragment_" + i);
            f.y += Mth.sin(t * 0.08F + i * 1.26F) * 1.5F;
            f.xRot += t * 0.02F * (i % 2 == 0 ? 1 : -1);
            f.zRot += Mth.sin(t * 0.04F + i) * 0.3F;
        }
    }

    /**
     * 2.7.3: the Primordial Shadow hangs in the air. Her wings beat slowly and open wider as she weakens; the tail
     * sways under her; her heart beats, faster in each phase; the shards circle her, faster in each phase.
     */
    public static void primordialShadow(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks, s = Math.min(1.0F, st.walkAnimationSpeed);
        int phase = st.health > 0.66F ? 1 : st.health > 0.33F ? 2 : 3;
        look(p.apply("head"), st, 0.8F);
        ModelPart body = p.apply("body");
        body.y += Mth.sin(t * 0.08F) * 1.2F;
        body.xRot += s * 0.25F;
        // the wings: a slow, heavy beat; wider open in each phase
        float open = (phase - 1) * 0.3F;
        float beat = Mth.sin(t * (0.07F + phase * 0.025F)) * (0.25F + phase * 0.06F);
        p.apply("wing_r").yRot -= open + beat * 0.5F;
        p.apply("wing_l").yRot += open + beat * 0.5F;
        p.apply("wing_r").zRot += beat;
        p.apply("wing_l").zRot -= beat;
        p.apply("wing_r_b").zRot += beat * 0.6F;
        p.apply("wing_l_b").zRot -= beat * 0.6F;
        p.apply("wing_r_c").zRot += Mth.sin(t * 0.1F - 0.8F) * 0.2F;
        p.apply("wing_l_c").zRot -= Mth.sin(t * 0.1F - 0.8F) * 0.2F;
        // the tail of smoke she hangs on
        String[] tail = {"tail_a", "tail_b", "tail_c", "tail_d"};
        for (int i = 0; i < tail.length; i++) {
            p.apply(tail[i]).zRot += Mth.sin(t * 0.09F - i * 0.7F) * (0.08F + i * 0.05F);
            p.apply(tail[i]).xRot += Mth.cos(t * 0.07F - i * 0.6F) * (0.05F + i * 0.03F) + s * 0.15F;
        }
        // the heart
        float beatRate = 0.15F + phase * 0.1F;
        float pulse = 1.0F + Math.max(0.0F, Mth.sin(t * beatRate)) * (0.15F + phase * 0.08F);
        ModelPart heart = p.apply("heart");
        heart.xScale = heart.yScale = heart.zScale = pulse;
        // arms reaching, claws flexing
        p.apply("arm_r").xRot += Mth.sin(t * 0.06F) * 0.1F;
        p.apply("arm_l").xRot -= Mth.sin(t * 0.06F) * 0.1F;
        p.apply("claw_r").xRot -= Mth.sin(t * 0.13F) * 0.2F;
        p.apply("claw_l").xRot -= Mth.cos(t * 0.13F) * 0.2F;
        if (st.attack > 0) {
            float a = Mth.sin(st.attack * Mth.PI);
            p.apply("arm_r").xRot -= a * 1.8F;
            p.apply("arm_l").xRot -= a * 1.8F;
            p.apply("wing_r").yRot += a * 0.5F;
            p.apply("wing_l").yRot -= a * 0.5F;
        }
        // the shards: an orbit that quickens, each shard turning on itself
        p.apply("orbit").yRot += t * (0.02F + phase * 0.015F);
        for (int i = 0; i < 6; i++) {
            ModelPart sh = p.apply("shard_" + i);
            sh.y += Mth.sin(t * 0.1F + i) * 1.0F;
            sh.yRot += t * 0.05F;
        }
        p.apply("horn_r_c").zRot += Mth.sin(t * 0.05F) * 0.03F;
        p.apply("horn_l_c").zRot -= Mth.sin(t * 0.05F) * 0.03F;
    }

    public static void shadowVillager(Function<String, ModelPart> p, UmbraRenderState st) {
        look(p.apply("head"), st, 1.0F);
        p.apply("legs").xRot += Mth.cos(st.walkAnimationPos * 0.6662F) * 0.15F * Math.min(1.0F, st.walkAnimationSpeed);
        // nodding while trading
        if (st.flag) p.apply("head").xRot += Mth.sin(st.ageInTicks * 0.4F) * 0.15F;
    }

    /** A chest while it waits (legs folded away, lid shut); a mouth on legs once it shows itself. */
    public static void mimicker(Function<String, ModelPart> p, UmbraRenderState st) {
        boolean hidden = st.flag;
        for (int i = 0; i < 2; i++)
            for (String side : new String[]{"r", "l"}) {
                p.apply("leg_" + side + i + "_upper").visible = !hidden;
            }
        p.apply("eyes").visible = !hidden;
        p.apply("tongue").visible = !hidden;
        if (hidden) return;
        float t = st.ageInTicks;
        p.apply("body").y -= 8.0F;
        float open = 0.35F + Mth.sin(t * 0.25F) * 0.25F + (st.attack > 0 ? Mth.sin(st.attack * Mth.PI) * 0.8F : 0);
        p.apply("lid").xRot -= open;
        p.apply("tongue").xRot += Mth.sin(t * 0.4F) * 0.15F;
        float w = st.walkAnimationPos * 1.4F, s = Math.min(1.0F, st.walkAnimationSpeed);
        for (int i = 0; i < 2; i++) {
            float ph = i * Mth.PI;
            p.apply("leg_r" + i + "_upper").yRot += Mth.cos(w + ph) * 0.4F * s;
            p.apply("leg_l" + i + "_upper").yRot -= Mth.cos(w + ph) * 0.4F * s;
            p.apply("leg_r" + i + "_upper").zRot += Math.abs(Mth.sin(w + ph)) * 0.3F * s;
            p.apply("leg_l" + i + "_upper").zRot -= Math.abs(Mth.sin(w + ph)) * 0.3F * s;
        }
    }

    /** Yan: an old man's slow walk, a lantern that sways on its pole. */
    public static void lampKeeper(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks;
        look(p.apply("head"), st, 1.0F);
        p.apply("legs").xRot += Mth.cos(st.walkAnimationPos * 0.6662F) * 0.12F * Math.min(1.0F, st.walkAnimationSpeed);
        p.apply("lantern").zRot += Mth.sin(t * 0.07F) * 0.18F;
        p.apply("lantern").xRot += Mth.cos(t * 0.05F) * 0.1F;
        p.apply("arm_l").zRot -= Mth.sin(t * 0.05F) * 0.04F;
        p.apply("body").xRot += 0.08F;
    }

    /** The ash figure barely sways, as if the wind could take it apart. */
    public static void weeper(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks, w = st.walkAnimationPos, s = Math.min(1.0F, st.walkAnimationSpeed);
        look(p.apply("head"), st, 0.5F);
        // she sobs: the shoulders shake; the arms hang and sway as she drifts
        p.apply("body").xRot += Mth.sin(t * 0.9F) * 0.025F;
        p.apply("arm_r").xRot += Mth.cos(w * 0.6662F) * s * 0.3F + Mth.sin(t * 0.07F) * 0.05F;
        p.apply("arm_l").xRot -= Mth.cos(w * 0.6662F) * s * 0.3F;
        if (st.attack > 0) p.apply("arm_l").xRot -= Mth.sin(st.attack * Mth.PI) * 2.0F;   // the throw
    }

    public static void bellringer(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks, w = st.walkAnimationPos, s = Math.min(1.0F, st.walkAnimationSpeed);
        look(p.apply("head"), st, 0.6F);
        p.apply("body").zRot += Mth.cos(w * 0.6662F) * s * 0.08F;      // a limping waddle
        p.apply("bell").zRot += Mth.sin(t * 0.3F) * 0.06F;
        if (st.attack > 0) p.apply("arm_r").zRot += Mth.sin(st.attack * Mth.PI * 2) * 0.9F;   // the ring
    }

    public static void ashFigure(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks;
        p.apply("body").zRot += Mth.sin(t * 0.03F) * 0.02F;
        p.apply("head").xRot += Mth.sin(t * 0.02F) * 0.03F;
    }

    // ---------------------------------------------------------------- 2.7.1

    /** Yan, fallen: a slow, heavy walk, the cloak in rags swaying, the halo turning, the lantern swinging on its hook. */
    public static void fallenKeeper(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks, s = Math.min(1.0F, st.walkAnimationSpeed);
        walk(p, st, 0.7F, 0.0F);
        look(p.apply("head"), st, 0.9F);
        for (int i = 0; i < 6; i++) p.apply("cloak_" + i).xRot += Mth.sin(t * 0.07F + i * 0.9F) * 0.07F + s * 0.35F;
        p.apply("skirt_front").xRot -= s * 0.25F * Math.abs(Mth.cos(st.walkAnimationPos * 0.6662F));
        p.apply("skirt_back").xRot += s * 0.3F;
        ModelPart halo = p.apply("halo");
        halo.zRot += t * 0.03F;
        halo.y += Mth.sin(t * 0.08F) * 0.5F;
        p.apply("lantern").zRot += Mth.sin(t * 0.06F) * 0.2F;
        p.apply("lantern").xRot += Mth.cos(t * 0.05F) * 0.12F;
        p.apply("arm_l").zRot -= 0.1F + Mth.sin(t * 0.05F) * 0.06F;
        p.apply("claws").xRot -= Mth.sin(t * 0.11F) * 0.15F;
        if (st.attack > 0) {
            float a = Mth.sin(st.attack * Mth.PI);
            p.apply("arm_r").xRot -= a * 1.6F;   // the staff comes down
            p.apply("arm_l").xRot -= a * 0.8F;
        }
    }

    /** The Drowned Keeper: a heavy tread, kelp drifting as in water, the anchor swinging on its chain, a two-fisted slam. */
    public static void drownedKeeper(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks, s = Math.min(1.0F, st.walkAnimationSpeed);
        walk(p, st, 0.6F, 0.35F);
        look(p.apply("head"), st, 0.7F);
        p.apply("body").y += Mth.sin(t * 0.06F) * 0.4F;
        for (int i = 0; i < 5; i++) {
            ModelPart k = p.apply("kelp_" + i);
            k.xRot += Mth.sin(t * 0.05F + i * 1.3F) * 0.15F + s * 0.2F;
            k.zRot += Mth.cos(t * 0.04F + i) * 0.1F;
        }
        p.apply("chain_a").xRot += Mth.sin(t * 0.05F) * 0.2F - s * 0.3F;
        p.apply("chain_b").xRot += Mth.sin(t * 0.05F + 0.6F) * 0.2F;
        p.apply("anchor").zRot += Mth.sin(t * 0.07F) * 0.15F;
        p.apply("fin").zRot += Mth.sin(t * 0.09F) * 0.05F;
        slam(p, st, 2.2F);
    }

    /** The acolyte swims: bent forward, arms sweeping, the robe and the kelp trailing; it grabs with both hands. */
    public static void acolyte(Function<String, ModelPart> p, UmbraRenderState st) {
        float t = st.ageInTicks, s = Math.min(1.0F, st.walkAnimationSpeed);
        look(p.apply("head"), st, 0.6F);
        p.apply("body").xRot += s * 0.5F;
        float stroke = Mth.sin(t * 0.25F) * (0.25F + s * 0.6F);
        p.apply("arm_r").xRot += stroke;
        p.apply("arm_l").xRot -= stroke;
        p.apply("fore_r").xRot -= Math.abs(stroke) * 0.6F;
        p.apply("fore_l").xRot -= Math.abs(stroke) * 0.6F;
        p.apply("robe_a").xRot += s * 0.4F + Mth.sin(t * 0.12F) * 0.05F;
        p.apply("robe_b").xRot += s * 0.3F + Mth.sin(t * 0.12F + 1.2F) * 0.12F;
        p.apply("kelp_a").zRot += Mth.sin(t * 0.08F) * 0.2F;
        p.apply("kelp_b").zRot += Mth.cos(t * 0.07F) * 0.2F;
        p.apply("kelp_c").xRot += Mth.sin(t * 0.06F) * 0.15F + s * 0.4F;
        p.apply("hood_tip").xRot += Mth.sin(t * 0.1F) * 0.1F;
        if (st.attack > 0) {
            float a = Mth.sin(st.attack * Mth.PI);
            p.apply("arm_r").xRot -= a * 1.5F;
            p.apply("arm_l").xRot -= a * 1.5F;
        }
    }
}
