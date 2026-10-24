package dev.umbra.gear;

import dev.umbra.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * 2.12.2: night goggles. Luminite lenses on a gloom frame, worn on the head (in place of a helmet): in the dark you see
 * as by day. The lenses burn their light while they work (an hour, a point every ten seconds in the dark) and go dim
 * at the end, never break: a luminite crystal at an anvil mends them. In daylight they rest.
 */
public final class NightGoggles {
    private NightGoggles() {}

    /** Points of charge: one every {@link #DRAIN_TICKS} in the dark, an hour in all. */
    public static final int CHARGE = 360, DRAIN_TICKS = 200, DARK = 7;

    public static boolean worn(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.NIGHT_GOGGLES.get());
    }

    /** Light left in the lenses (0 = dim). */
    public static int charge(ItemStack stack) {
        return stack.getMaxDamage() - 1 - stack.getDamageValue();
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 3 || !worn(player)) return;
        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (charge(stack) <= 0) return;
        boolean dark = player.level().getMaxLocalRawBrightness(player.blockPosition()) <= DARK;
        if (!dark) return;
        MobEffectInstance nv = player.getEffect(MobEffects.NIGHT_VISION);
        if (nv == null || nv.getDuration() < 240) player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
        if (!player.isCreative() && player.tickCount % DRAIN_TICKS == 3) {
            stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, stack.getDamageValue() + 1));
            if (charge(stack) <= 0) {
                player.removeEffect(MobEffects.NIGHT_VISION);
                player.sendOverlayMessage(Component.translatable("message.umbra.night_goggles.dim").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            }
        }
    }
}
