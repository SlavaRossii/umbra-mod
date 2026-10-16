package dev.umbra.night;

import dev.umbra.journal.Journal;
import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModEntities;
import dev.umbra.registry.ModSounds;
import dev.umbra.sanity.Sanity;
import dev.umbra.util.UmbraUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/**
 * Some nights are not like the others. Which night brings what is decided by the world seed and the day, so every
 * player on the same night lives through the same thing:
 * <ul>
 *   <li><b>blood moon</b> (one night in six): the dark sends more of its own, and the mind wears down faster;</li>
 *   <li><b>fog</b> (one in six): a white wall a few blocks away, and footsteps somewhere in it;</li>
 *   <li><b>silence</b> (one in eight): every creature falls quiet. Then, at the end, something does not.</li>
 * </ul>
 * The client learns tonight's event through the player's {@link ModAttachments#NIGHT_EVENT} (fog colour, muted sounds).
 */
public final class NightEvents {
    public static final String NONE = "", BLOOD = "blood_moon", FOG = "fog", SILENCE = "silence";

    private NightEvents() {}

    private static String current = NONE;
    private static boolean wasNight;
    private static long checkedAt = -1;
    /** For tests and /umbra night: the next night brings this. */
    public static String forced;

    /** Tonight's event (overworld only), or NONE. Rolled once when the night falls; gone at dawn. */
    public static String tonight(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return NONE;
        if (checkedAt != level.getGameTime()) {
            checkedAt = level.getGameTime();
            boolean night = UmbraUtil.isNight(level);
            if (night && !wasNight) current = roll(level);
            if (!night) current = NONE;
            wasNight = night;
        }
        return current;
    }

    private static String roll(ServerLevel level) {
        if (forced != null) {
            String f = forced;
            forced = null;
            return f;
        }
        if (!dev.umbra.UmbraConfig.on(dev.umbra.UmbraConfig.NIGHT_EVENTS) || level.getGameTime() < 48000L) return NONE;  // the first nights are ordinary
        int roll = level.getRandom().nextInt(48);
        if (roll < 8) return BLOOD;
        if (roll < 16) return FOG;
        if (roll < 22) return SILENCE;
        return NONE;
    }

    /** Starts an event right now (tests, /umbra night). */
    public static void force(ServerLevel level, String event) {
        current = event;
        wasNight = true;
        checkedAt = level.getGameTime();
    }

    /** Every second for every player. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 7 || !(player.level() instanceof ServerLevel level)) return;
        String now = level.dimension() == Level.OVERWORLD ? tonight(level) : NONE;
        String was = player.getData(ModAttachments.NIGHT_EVENT);
        if (!now.equals(was)) {
            player.setData(ModAttachments.NIGHT_EVENT, now);
            if (!now.isEmpty()) begin(level, player, now);
            else if (was.equals(SILENCE)) breakSilence(level, player);
        }
        if (now.isEmpty() || player.isCreative() || player.isSpectator()) return;
        RandomSource r = player.getRandom();
        switch (now) {
            case BLOOD -> {
                Sanity.add(player, -0.15F);
                // the dark sends its own: one of them every half minute or so, out of sight
                if (r.nextInt(30) == 0) spawnNear(level, player, r.nextBoolean() ? ModEntities.CRAWLER.get() : ModEntities.BLIND.get());
            }
            case FOG -> {
                if (r.nextInt(240) == 0) dev.umbra.steps.StepsManager.forceStart(player);
            }
            case SILENCE -> {
                Sanity.add(player, -0.05F);
            }
            default -> { }
        }
    }

    private static void begin(ServerLevel level, ServerPlayer player, String event) {
        player.sendSystemMessage(Component.translatable("night.umbra." + event).withStyle(event.equals(BLOOD) ? ChatFormatting.DARK_RED : ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (event.equals(BLOOD)) dev.umbra.util.UmbraUtil.privateSound(player, ModSounds.BLOOD_MOON, SoundSource.AMBIENT, 1.0F, 1.0F);
        Journal.unlock(player, "night_" + event);
    }

    /** At the end of a silent night, the silence breaks all at once. */
    private static void breakSilence(ServerLevel level, ServerPlayer player) {
        dev.umbra.util.UmbraUtil.privateSound(player, ModSounds.SILENCE, SoundSource.AMBIENT, 1.0F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 0.8F, 0.4F);
        Sanity.add(player, -6.0F);
    }

    private static void spawnNear(ServerLevel level, ServerPlayer player, EntityType<? extends Mob> type) {
        RandomSource r = player.getRandom();
        for (int i = 0; i < 8; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 16 + r.nextInt(10);
            BlockPos at = dev.umbra.sanity.Hallucinations.findStandable(level,
                    BlockPos.containing(player.getX() + Math.cos(a) * d, player.getY(), player.getZ() + Math.sin(a) * d), 6);
            if (at == null || UmbraUtil.effectiveLight(level, at) > 7 || dev.umbra.light.WardBlock.protects(level, net.minecraft.world.phys.Vec3.atCenterOf(at))) continue;
            Mob m = type.create(level, EntitySpawnReason.EVENT);
            if (m == null) return;
            m.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360, 0);
            level.addFreshEntity(m);
            return;
        }
    }
}
