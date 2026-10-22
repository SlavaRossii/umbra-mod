package dev.umbra.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * 2.10: how the weapons strike in first person, each its own way instead of the one sword swing for all: the spear
 * thrusts straight ahead, the scythe goes round in a wide flat sweep, the dagger stabs short and quick. (Held still,
 * they sit where any weapon sits.)
 */
public final class WeaponSwings {
    private WeaponSwings() {}

    public enum Kind { SPEAR, SCYTHE, DAGGER }

    public static IClientItemExtensions of(Kind kind) {
        return new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack pose, PlayerRenderState player, HumanoidArm arm, ItemStack stack, float partial,
                                                   float equip, float swing) {
                float f = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
                // where the game holds any item
                pose.translate(f * 0.56F, -0.52F + equip * -0.6F, -0.72F);
                if (swing <= 0.0F) return true;
                float s = Mth.sin(Mth.sqrt(swing) * Mth.PI);
                switch (kind) {
                    case SPEAR -> {
                        // a thrust: drawn back a little, then driven straight out
                        float back = swing < 0.25F ? Mth.sin(swing / 0.25F * Mth.PI) * 0.12F : 0.0F;
                        pose.translate(f * -0.1F * s, 0.05F * s, -0.6F * s + back);
                        pose.rotateDegrees(Axis.XP, -10.0F * s);
                        pose.rotateDegrees(Axis.YP, f * 8.0F * s);
                    }
                    case SCYTHE -> {
                        // a wide flat sweep from your side across in front of you
                        pose.translate(f * -0.25F * s, 0.12F * s, -0.15F * s);
                        pose.rotateDegrees(Axis.YP, f * (50.0F - 130.0F * swing) * s);
                        pose.rotateDegrees(Axis.ZP, f * -35.0F * s);
                        pose.rotateDegrees(Axis.XP, -15.0F * s);
                    }
                    case DAGGER -> {
                        // short and quick, a stab with a twist
                        pose.translate(f * -0.05F * s, 0.03F * s, -0.45F * s);
                        pose.rotateDegrees(Axis.XP, -20.0F * s);
                        pose.rotateDegrees(Axis.ZP, f * 25.0F * s);
                    }
                }
                return true;
            }
        };
    }
}
