package dev.umbra.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.umbra.Umbra;
import dev.umbra.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Vector3f;

/**
 * 2.8.1: a lantern in first person is held in your own hand, in your own skin: the player's arm, raised a little and
 * set a little out to the side, and the lantern hanging from the fist by its handle, always upright (it hangs by
 * its own weight, whichever way you look). It swings with the arm when you strike, so you can fight with it in hand.
 */
@EventBusSubscriber(modid = Umbra.MODID, value = Dist.CLIENT)
public final class LanternFirstPerson {
    /**
     * How the arm is held: raised (blocks up), set out to the side (blocks), brought nearer (blocks), tilted up (degrees),
     * and leant over (degrees round the line of sight, about the fist): the game's empty hand stands almost upright on
     * the screen, so a lantern hanging from it would hide behind the forearm; leant over, the arm comes in from the side
     * and the lantern hangs free.
     */
    public record Hold(float raise, float out, float near, float lift, float lean) {}

    public static final Hold[] HOLDS = {
            new Hold(0.10F, 0.04F, 0.10F, 0.0F, 35.0F),
            new Hold(0.16F, 0.06F, 0.12F, 4.0F, 45.0F),
            new Hold(0.06F, 0.02F, 0.06F, 0.0F, 25.0F),
            new Hold(0.20F, 0.10F, 0.15F, 6.0F, 55.0F),
    };
    /** Which hold is drawn (the screenshots of the client check compare them). */
    public static int hold = 0;

    private static final ItemStackRenderState ITEM = new ItemStackRenderState();
    private static final boolean DEBUG = Boolean.getBoolean("umbra.clientshots");

    private LanternFirstPerson() {}

    /** Where the handle is in each lantern's model (in sixteenths, from the bottom of its box). */
    private static float grip(ItemStack stack) {
        if (stack.is(ModItems.MARTA_LANTERN.get())) return 15.9F;
        if (stack.is(ModItems.DAWN_LANTERN.get())) return 14.8F;
        return 14.7F;
    }

    private static boolean isLantern(ItemStack stack) {
        return stack.is(ModItems.LUMINITE_LANTERN.get()) || stack.is(ModItems.MARTA_LANTERN.get()) || stack.is(ModItems.DAWN_LANTERN.get());
    }

    /** The game's own transforms for an empty hand (with its swing and its rise when you take something up), and ours. */
    private static void armTransform(PoseStack pose, Hold h, float f, float attack, float sq, float equip) {
        pose.translate(f * h.out(), h.raise(), h.near());
        pose.rotateDegrees(Axis.XP, h.lift());
        pose.translate(f * (-0.3F * Mth.sin(sq * Mth.PI) + 0.64000005F), 0.4F * Mth.sin(sq * (Mth.PI * 2.0F)) - 0.6F + equip * -0.6F,
                -0.4F * Mth.sin(attack * Mth.PI) - 0.71999997F);
        pose.rotateDegrees(Axis.YP, f * 45.0F);
        pose.rotateDegrees(Axis.YP, f * Mth.sin(sq * Mth.PI) * 70.0F);
        pose.rotateDegrees(Axis.ZP, f * Mth.sin(attack * attack * Mth.PI) * -20.0F);
        pose.translate(f * -1.0F, 3.6F, 3.5F);
        pose.rotateDegrees(Axis.ZP, f * 120.0F);
        pose.rotateDegrees(Axis.XP, 200.0F);
        pose.rotateDegrees(Axis.YP, f * -135.0F);
        pose.translate(f * 5.6F, 0.0F, 0.0F);
    }

    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !isLantern(stack)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        event.setCanceled(true);

        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? mc.player.getMainArm() : mc.player.getMainArm().getOpposite();
        boolean right = arm == HumanoidArm.RIGHT;
        float f = right ? 1.0F : -1.0F;
        float attack = event.getSwingProgress();
        float equip = event.getEquipProgress();
        Hold h = HOLDS[Mth.clamp(hold, 0, HOLDS.length - 1)];
        PoseStack pose = event.getPoseStack();
        AvatarRenderState avatar = event.getPlayerState().avatarRenderState;
        int light = event.getPackedLight();

        // the arm, as the game draws an empty hand, raised a little, set out to the side and leant over about the fist
        float sq = Mth.sqrt(attack);
        pose.pushPose();
        armTransform(pose, h, f, attack, sq, equip);
        Vector3f fist = pose.last().pose().transformPosition(new Vector3f(f * -5.5F / 16.0F, 11.6F / 16.0F, 0.0F));
        pose.popPose();
        pose.pushPose();
        pose.translate(fist.x(), fist.y(), fist.z());
        pose.rotateDegrees(Axis.ZP, f * h.lean());
        pose.translate(-fist.x(), -fist.y(), -fist.z());
        armTransform(pose, h, f, attack, sq, equip);
        if (!avatar.isInvisible) {
            var renderer = mc.getEntityRenderDispatcher().getRenderer(avatar);
            if (renderer instanceof AvatarRenderer<?> ar) {
                var skin = avatar.skin.body().texturePath();
                if (right) ar.renderRightHand(pose, event.getSubmitNodeCollector(), light, skin, avatar.showRightSleeve);
                else ar.renderLeftHand(pose, event.getSubmitNodeCollector(), light, skin, avatar.showLeftSleeve);
            }
        }
        pose.popPose();

        // the lantern, hanging from the fist: upright whatever the pitch, swaying when the arm swings
        pose.pushPose();
        pose.translate(fist.x(), fist.y(), fist.z() + 0.04F);
        pose.rotateDegrees(Axis.XP, event.getInterpolatedPitch());
        float sway = Mth.sin(sq * Mth.PI) * 25.0F + Mth.sin((mc.player.tickCount + event.getPartialTick()) * 0.08F) * 1.5F;
        pose.rotateDegrees(Axis.ZP, f * sway);
        pose.rotateDegrees(Axis.YP, f * -20.0F);
        float s = 0.42F;
        pose.scale(s, s, s);
        pose.translate(0.0F, 0.5F - grip(stack) / 16.0F, 0.0F);
        mc.getItemModelResolver().updateForTopItem(ITEM, stack, ItemDisplayContext.NONE, mc.level, null, 0);
        ITEM.submit(pose, event.getSubmitNodeCollector(), light, OverlayTexture.NO_OVERLAY, 0);
        if (DEBUG && mc.player.tickCount % 100 == 0)
            Umbra.LOGGER.info("UMBRA CLIENTSHOTS: lantern in the {} hand, hold {}, fist at {}, drawn {}", event.getHand(), hold, fist, !ITEM.isEmpty());
        pose.popPose();
    }
}
