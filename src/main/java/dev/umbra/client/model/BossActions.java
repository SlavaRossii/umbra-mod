package dev.umbra.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import java.util.function.Function;

/**
 * 2.10: what the bosses' bodies do when they act ({@code UmbraBoss.ACT_*}): the wind-up before a throw from afar (so it
 * can be seen coming) and the throw itself; the gathering before something heavy and the blow; the call of their own;
 * the roar of a new phase; and the flinch when they are hit. Added on top of each one's own motion (see
 * {@link Animations}); every one reads how far into its action it is, 0..1.
 */
final class BossActions {
    private BossActions() {}

    static final int SWING = 1, CAST = 2, SLAM = 3, ROAR = 4, SUMMON = 5, BEAM = 6;

    /** Winds up over the first three quarters, lets go over the last. */
    static float wind(float a) {
        if (a < 0) return 0;
        return a < 0.75F ? a / 0.75F : 1.0F - (a - 0.75F) / 0.25F;
    }

    /** The throw itself: nothing until three quarters, then one quick swing. */
    static float release(float a) {
        if (a < 0.75F) return 0;
        return Mth.sin((a - 0.75F) / 0.25F * Mth.PI);
    }

    static float bump(float a) {
        return a < 0 ? 0 : Mth.sin(a * Mth.PI);
    }

    /** Up fast, held, down over the last fifth. */
    static float hold(float a) {
        if (a < 0) return 0;
        if (a < 0.2F) return a / 0.2F;
        if (a > 0.8F) return (1.0F - a) / 0.2F;
        return 1.0F;
    }

    static void flinch(ModelPart part, UmbraRenderState st, float amp) {
        if (st.hurt > 0) part.xRot -= st.hurt * st.hurt * amp;
    }

    // ---------------------------------------------------------------- the Watcher
    static void watcher(Function<String, ModelPart> p, UmbraRenderState st) {
        ModelPart eye = p.apply("eye"), up = p.apply("lid_up"), down = p.apply("lid_down");
        float t = st.ageInTicks;
        float beam = st.act(BEAM, 30), roar = st.act(ROAR, 40), rain = st.act(SUMMON, 24);
        if (beam >= 0) {
            // its gaze gathers: the lids wide, the eye trembling harder, the pupil a burning point
            eye.x += Mth.sin(t * 3.1F) * 0.35F * beam;
            eye.y += Mth.cos(t * 2.7F) * 0.25F * beam;
            up.xRot -= 0.4F * hold(beam);
            down.xRot += 0.4F * hold(beam);
            ModelPart pu = p.apply("pupil");
            pu.xScale = pu.yScale = Math.max(0.3F, pu.xScale * (1.0F - 0.65F * beam));
        }
        if (roar >= 0) {
            float h = hold(roar);
            up.xRot -= 0.45F * h;
            down.xRot += 0.45F * h;
            eye.z += 1.5F * h;
            eye.xRot += Mth.sin(t * 1.3F) * 0.12F * h;
            for (int i = 0; i < 6; i++) p.apply("nerve_" + i).yRot += Mth.sin(t * 0.9F - i) * 0.35F * h;
        }
        if (rain >= 0) eye.xRot -= 0.55F * bump(rain);                // it looks up, and the tears fall
        if (st.hurt > 0) {
            eye.z += st.hurt * 1.4F;
            up.xRot += st.hurt * 0.35F;
            down.xRot -= st.hurt * 0.35F;
        }
    }

    // ---------------------------------------------------------------- the Mimic Queen
    static void mimicQueen(Function<String, ModelPart> p, UmbraRenderState st) {
        ModelPart lid = p.apply("lid"), body = p.apply("body");
        float cast = st.act(CAST, 22), slam = st.act(SLAM, 24), sum = st.act(SUMMON, 30), roar = st.act(ROAR, 40);
        if (cast >= 0) {
            // she rears back with her maw open, and spits
            float w = wind(cast), r = release(cast);
            lid.xRot -= w * 0.9F + r * 0.5F;
            body.xRot -= w * 0.3F - r * 0.45F;
            p.apply("tongue_a").xRot += w * 0.5F;
            p.apply("tongue_b").xRot -= r * 0.6F;
        }
        if (slam >= 0) {
            // she crouches on all six before the leap
            float w = wind(slam);
            body.y += w * 2.2F;
            for (int i = 0; i < 3; i++) {
                p.apply("leg_r" + i + "_upper").zRot -= w * 0.35F;
                p.apply("leg_l" + i + "_upper").zRot += w * 0.35F;
            }
        }
        if (sum >= 0) {
            float b = bump(sum);
            for (int i = 0; i < 5; i++) p.apply("brood_" + i + "_lid").xRot -= b * 1.1F;
            body.zRot += Mth.sin(st.ageInTicks * 1.3F) * 0.06F * b;
        }
        if (roar >= 0) {
            float h = hold(roar);
            lid.xRot -= h * 1.25F;
            body.xRot -= h * 0.3F;
            p.apply("tongue_b").xRot -= Mth.sin(st.ageInTicks * 0.9F) * 0.6F * h;
            for (int i = 0; i < 3; i++) {
                p.apply("leg_r" + i + "_upper").zRot -= h * 0.3F;
                p.apply("leg_l" + i + "_upper").zRot += h * 0.3F;
            }
        }
        flinch(body, st, 0.25F);
    }

    // ---------------------------------------------------------------- the Shepherd
    static void shepherd(Function<String, ModelPart> p, UmbraRenderState st) {
        ModelPart arm = p.apply("arm_r"), armL = p.apply("arm_l"), body = p.apply("body"), head = p.apply("head");
        float cast = st.act(CAST, 22), slam = st.act(SLAM, 20), sum = st.act(SUMMON, 30), roar = st.act(ROAR, 40);
        if (cast >= 0) {
            // the scythe drawn back over his shoulder, then the sweep that sends the sickles
            float w = wind(cast), r = release(cast);
            arm.xRot -= w * 1.6F;
            arm.zRot += w * 0.9F;
            body.yRot -= w * 0.5F - r * 1.0F;
            p.apply("scythe").xRot += w * 0.4F;
        }
        if (slam >= 0) {
            float w = wind(slam), r = release(slam);
            arm.xRot -= w * 2.4F - r * 1.2F;
            armL.xRot -= w * 1.2F;
            body.xRot += r * 0.3F;
            p.apply("bell").zRot += Mth.sin(st.ageInTicks * 0.8F) * 0.6F * bump(slam);
        }
        float call = Math.max(hold(sum), hold(roar));
        if (call > 0) {
            // arms spread, head thrown back: he calls the flock home
            armL.zRot -= call * 1.1F;
            arm.zRot += call * 1.1F;
            armL.xRot -= call * 0.6F;
            arm.xRot -= call * 0.6F;
            head.xRot -= call * 0.45F;
            p.apply("herd").yRot += st.ageInTicks * 0.2F * call;
        }
        flinch(body, st, 0.18F);
    }

    // ---------------------------------------------------------------- the Architect
    static void architect(Function<String, ModelPart> p, UmbraRenderState st) {
        ModelPart armR = p.apply("arm_r"), armL = p.apply("arm_l"), body = p.apply("body");
        float cast = st.act(CAST, 24), slam = st.act(SLAM, 24), sum = st.act(SUMMON, 30), roar = st.act(ROAR, 40);
        if (cast >= 0) {
            // a block of the maze heaved back over his head, and thrown
            float w = wind(cast), r = release(cast);
            armR.xRot -= w * 2.8F - r * 1.6F;
            p.apply("fore_r").xRot -= w * 0.6F;
            armL.xRot -= w * 0.6F;
            body.xRot -= w * 0.15F - r * 0.25F;
        }
        if (slam >= 0) {
            // both fists high, and down on the floor: the quake
            float w = wind(slam), r = release(slam);
            armR.xRot -= w * 2.9F - r * 2.0F;
            armL.xRot -= w * 2.9F - r * 2.0F;
            body.xRot += r * 0.35F;
            body.y -= w * 1.0F;
        }
        if (sum >= 0) {
            float h = hold(sum);
            p.apply("divider_a").zRot += h * 0.8F;
            p.apply("divider_b").zRot -= h * 0.8F;
            armR.zRot += h * 0.8F;
            armL.zRot -= h * 0.8F;
            p.apply("orbit").yRot += st.ageInTicks * 0.15F * h;
        }
        if (roar >= 0) {
            float h = hold(roar);
            p.apply("arch").y -= h * 2.5F;
            p.apply("keystone").zRot += st.ageInTicks * 0.4F * h;
            armR.xRot -= h * 1.5F;
            armL.xRot -= h * 1.5F;
            p.apply("head").xRot -= h * 0.5F;
        }
        flinch(body, st, 0.12F);
    }

    // ---------------------------------------------------------------- the Primordial Shadow
    static void primordialShadow(Function<String, ModelPart> p, UmbraRenderState st) {
        ModelPart armR = p.apply("arm_r"), armL = p.apply("arm_l"), body = p.apply("body");
        ModelPart wingR = p.apply("wing_r"), wingL = p.apply("wing_l");
        float cast = st.act(CAST, 22), slam = st.act(SLAM, 24), sum = st.act(SUMMON, 30), roar = st.act(ROAR, 40);
        if (cast >= 0) {
            // claws open towards you, wings spread, the heart swelling: and the clots go
            float w = wind(cast), r = release(cast);
            armR.xRot -= w * 1.3F + r * 0.4F;
            armL.xRot -= w * 1.3F + r * 0.4F;
            p.apply("claw_r").xRot += w * 0.6F;
            p.apply("claw_l").xRot += w * 0.6F;
            wingR.yRot -= w * 0.4F;
            wingL.yRot += w * 0.4F;
            ModelPart heart = p.apply("heart");
            heart.xScale = heart.yScale = heart.zScale = heart.xScale * (1.0F + 0.4F * w);
        }
        if (slam >= 0) {
            // wings folded back, head down: the dash
            float h = hold(slam);
            wingR.yRot += h * 0.8F;
            wingL.yRot -= h * 0.8F;
            body.xRot += h * 0.45F;
        }
        if (sum >= 0) {
            float b = bump(sum);
            armR.xRot -= b * 1.4F;
            armL.xRot -= b * 1.4F;
            armR.zRot += Mth.sin(sum * Mth.PI * 2) * 0.4F;
            armL.zRot -= Mth.sin(sum * Mth.PI * 2) * 0.4F;
        }
        if (roar >= 0) {
            float h = hold(roar);
            wingR.yRot -= h * 0.9F;
            wingL.yRot += h * 0.9F;
            p.apply("head").xRot -= h * 0.6F;
            armR.xRot -= h * 2.0F;
            armL.xRot -= h * 2.0F;
        }
        flinch(body, st, 0.15F);
    }

    // ---------------------------------------------------------------- the Fallen Keeper
    static void fallenKeeper(Function<String, ModelPart> p, UmbraRenderState st) {
        ModelPart armR = p.apply("arm_r"), armL = p.apply("arm_l"), head = p.apply("head");
        float cast = st.act(CAST, 22), flare = st.act(BEAM, 20), sum = st.act(SUMMON, 30), roar = st.act(ROAR, 40);
        if (cast >= 0) {
            // the lantern held up high, and its sparks flung at you
            float w = wind(cast), r = release(cast);
            armL.xRot -= w * 1.7F - r * 0.6F;
            p.apply("lantern").xRot += w * 0.3F;
            p.apply("halo").y -= w * 1.0F;
        }
        if (flare >= 0) {
            float h = hold(flare);
            armL.xRot -= h * 1.9F;
            head.xRot -= h * 0.3F;
        }
        if (sum >= 0) {
            float b = bump(sum);
            armR.xRot -= b * 1.2F;
            armL.xRot -= b * 1.2F;
            p.apply("claws").xRot += b * 0.4F;
        }
        if (roar >= 0) {
            float h = hold(roar);
            p.apply("halo").y -= h * 2.0F;
            armR.xRot -= h * 1.6F;
            armL.xRot -= h * 1.6F;
            head.xRot -= h * 0.5F;
        }
        flinch(head, st, 0.2F);
    }

    // ---------------------------------------------------------------- the Drowned Keeper
    static void drownedKeeper(Function<String, ModelPart> p, UmbraRenderState st) {
        ModelPart armR = p.apply("arm_r"), armL = p.apply("arm_l"), body = p.apply("body");
        float cast = st.act(CAST, 22), slam = st.act(SLAM, 24), sum = st.act(SUMMON, 30), roar = st.act(ROAR, 40);
        if (cast >= 0) {
            // the harpoon drawn back, and thrown
            float w = wind(cast), r = release(cast);
            armR.xRot -= w * 2.4F - r * 1.5F;
            body.yRot -= w * 0.3F - r * 0.5F;
        }
        if (slam >= 0) {
            float h = hold(slam);
            body.xRot += h * 0.5F;
            armR.xRot -= h * 1.2F;
            armL.xRot -= h * 1.2F;
        }
        float call = Math.max(hold(sum), hold(roar));
        if (call > 0) {
            armR.zRot += call * 1.0F;
            armL.zRot -= call * 1.0F;
            p.apply("head").xRot -= call * 0.4F;
            p.apply("chain_a").xRot += Mth.sin(st.ageInTicks * 0.6F) * 0.5F * call;
            p.apply("anchor").zRot += Mth.sin(st.ageInTicks * 0.5F) * 0.4F * call;
        }
        flinch(body, st, 0.15F);
    }
}
