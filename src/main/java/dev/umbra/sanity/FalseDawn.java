package dev.umbra.sanity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The false dawn: very rarely, deep in the night with a broken mind, it suddenly seems to be morning.
 * The world brightens, something like birdsong... and fifteen seconds later the night is back, darker.
 */
public final class FalseDawn {
    /** Per-second chance while it can happen: about once per hour spent below 15 sanity at night. */
    public static final int ONE_IN = 3600;
    public static final int LENGTH = 20 * 15;
    private static final Map<UUID, Long> ENDS = new HashMap<>();

    private FalseDawn() {}

    /** Called once a second from Sanity. */
    static void tick(ServerLevel level, ServerPlayer player, float sanity) {
        Long end = ENDS.get(player.getUUID());
        if (end != null) {
            if (level.getGameTime() >= end) finish(level, player);
            return;
        }
        if (sanity >= 15.0F || level.dimension() != Level.OVERWORLD || !dev.umbra.util.UmbraUtil.isNight(level)) return;
        if (player.getRandom().nextInt(ONE_IN) == 0) start(level, player);
    }

    public static void start(ServerLevel level, ServerPlayer player) {
        ENDS.put(player.getUUID(), level.getGameTime() + LENGTH);
        player.removeEffect(MobEffects.DARKNESS);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, LENGTH + 20, 0, false, false));
        player.sendOverlayMessage(Component.translatable("message.umbra.false_dawn.start").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 1.0F, 1.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.AMBIENT, 0.6F, 1.8F);
    }

    private static void finish(ServerLevel level, ServerPlayer player) {
        ENDS.remove(player.getUUID());
        player.removeEffect(MobEffects.NIGHT_VISION);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 4, 0, false, false));
        player.sendOverlayMessage(Component.translatable("message.umbra.false_dawn.end").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.0F, 0.5F);
        Sanity.add(player, -5.0F);
    }

    public static boolean active(ServerPlayer player) {
        return ENDS.containsKey(player.getUUID());
    }
}
