package dev.umbra.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * 2.8: a lantern is held out ahead on a straight arm, carefully, without the swing of walking; it sways only a little.
 * An extra arm pose (META-INF/enumextensions.json adds it to HumanoidModel.ArmPose); the lanterns ask for it through
 * their client extensions (UmbraClient). The lantern models turn 90 degrees more than the arm so they hang upright.
 */
public final class LanternHold {
    public static final float ARM = -1.25F;

    public static final EnumProxy<HumanoidModel.ArmPose> POSE = new EnumProxy<>(HumanoidModel.ArmPose.class, false, false,
            (IArmPoseTransformer) (model, state, arm) -> {
                ModelPart a = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                a.xRot = ARM + Mth.sin(state.ageInTicks * 0.067F) * 0.03F;
                a.yRot = arm == HumanoidArm.RIGHT ? -0.15F : 0.15F;
                a.zRot = 0.0F;
            });

    private LanternHold() {}
}
