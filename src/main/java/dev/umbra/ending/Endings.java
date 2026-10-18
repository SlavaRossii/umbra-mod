package dev.umbra.ending;

import dev.umbra.boss.UmbraBoss;
import dev.umbra.journal.Bestiary;
import dev.umbra.journal.Journal;
import dev.umbra.light.LightSensitive;
import dev.umbra.registry.ModAttachments;
import dev.umbra.sanity.Sanity;
import dev.umbra.underside.Underside;
import dev.umbra.util.UmbraUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/**
 * The two endings. The Primordial Shadow leaves her heart behind, and what you do with it is final:
 * <ul>
 *   <li><b>Dawn</b> ({@link #LIGHT}): burn the heart in a lighthouse lamp at full strength. The dark loses its grip
 *   on you: the horrors of the upper world come no harder than on your first night, the mind drains half as fast.</li>
 *   <li><b>Darkness</b> ({@link #DARK}): take the heart into yourself somewhere dark. The Underside's creatures know
 *   you as their own and leave you be unless you strike first, and you see in the dark; but daylight weakens you and
 *   your mind never climbs above {@link #DARK_SANITY_CAP} again.</li>
 * </ul>
 */
public final class Endings {
    public static final String LIGHT = "light", DARK = "dark";
    public static final float DARK_SANITY_CAP = 60.0F;
    /** Where you must stand to take the heart: no brighter than this. */
    public static final int DARK_ENOUGH = 4;

    private Endings() {}

    public static String of(LivingEntity player) {
        return player.getData(ModAttachments.ENDING);
    }

    public static boolean chosen(LivingEntity player) {
        return !of(player).isEmpty();
    }

    public static boolean isLight(LivingEntity player) {
        return LIGHT.equals(of(player));
    }

    public static boolean isDark(LivingEntity player) {
        return DARK.equals(of(player));
    }

    // ------------------------------------------------------------------ choosing

    /** The heart burns in the lamp at {@code lamp}. Returns false if this player already chose. */
    public static boolean dawn(ServerPlayer player, ServerLevel level, BlockPos lamp) {
        if (chosen(player)) return false;
        player.setData(ModAttachments.ENDING, LIGHT);
        // the flash: everything dark for 64 blocks burns
        for (Entity e : level.getEntities((Entity) null, new AABB(lamp).inflate(64), e -> e instanceof LightSensitive && !(e instanceof UmbraBoss))) {
            ((LightSensitive) e).onLit(level, player);
        }
        level.sendParticles(ParticleTypes.END_ROD, lamp.getX() + 0.5, lamp.getY() + 1.0, lamp.getZ() + 0.5, 300, 2.0, 6.0, 2.0, 0.25);
        level.playSound(null, lamp, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 3.0F, 0.5F);
        level.playSound(null, lamp, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 3.0F, 1.4F);
        Sanity.set(player, Sanity.MAX);
        player.removeEffect(MobEffects.DARKNESS);
        Journal.unlock(player, "ending_light");
        credits(player, LIGHT, ChatFormatting.GOLD);
        player.sendSystemMessage(Component.translatable("ending.umbra.light.yan").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        return true;
    }

    /** The heart is taken in the dark. Returns false if this player already chose or it is too bright here. */
    public static boolean darkness(ServerPlayer player) {
        if (chosen(player) || !darkEnough(player)) return false;
        player.setData(ModAttachments.ENDING, DARK);
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), 120, 0.6, 1.0, 0.6, 0.05);
        level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.8, 1.0, 0.8, 0.03);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 3.0F, 0.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.5F, 0.4F);
        if (Sanity.get(player) > DARK_SANITY_CAP) Sanity.set(player, DARK_SANITY_CAP);
        Journal.unlock(player, "ending_dark");
        credits(player, DARK, ChatFormatting.DARK_PURPLE);
        // 2.7: and far away, at the lighthouse, an old man lifts his head
        player.sendSystemMessage(Component.translatable("ending.umbra.dark.yan").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        return true;
    }

    /** 2.7: the dark ending's last page, when the Fallen Keeper falls. */
    public static void fallenCredits(ServerPlayer player) {
        if (player.connection != null) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 120, 40));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("ending.umbra.fallen.title").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("ending.umbra.fallen.subtitle").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        }
        for (int i = 1; i <= 3; i++)
            player.sendSystemMessage(Component.translatable("ending.umbra.fallen." + i).withStyle(i == 3 ? ChatFormatting.DARK_PURPLE : ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    public static boolean darkEnough(ServerPlayer player) {
        return UmbraUtil.effectiveLight(player.level(), BlockPos.containing(player.getEyePosition())) <= DARK_ENOUGH;
    }

    /** A title on screen and the closing lines in chat. */
    private static void credits(ServerPlayer player, String which, ChatFormatting color) {
        if (player.connection != null) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 120, 40));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("ending.umbra." + which + ".title").withStyle(color, ChatFormatting.BOLD)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("ending.umbra." + which + ".subtitle").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        }
        for (int i = 1; i <= LINES; i++) {
            player.sendSystemMessage(Component.translatable("ending.umbra." + which + "." + i).withStyle(i == LINES ? color : ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        player.sendSystemMessage(Component.translatable("ending.umbra.thanks").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static final int LINES = 5;

    // ------------------------------------------------------------------ living with it

    /** Every second for every player. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 11 || !isDark(player) || player.isSpectator()) return;
        Level level = player.level();
        // the dark keeps you company: at night a shade sometimes walks with you for a while, and the night thickens
        if (level instanceof ServerLevel sl && level.dimension() == Level.OVERWORLD && UmbraUtil.isNight(sl)) {
            if (player.getRandom().nextInt(240) == 0) {
                BlockPos at = dev.umbra.sanity.Hallucinations.findStandable(sl, player.blockPosition().offset(player.getRandom().nextInt(13) - 6, 0, player.getRandom().nextInt(13) - 6), 4);
                var shade = dev.umbra.registry.ModEntities.SHADE.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.EVENT);
                if (at != null && shade != null) {
                    shade.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
                    sl.addFreshEntity(shade);
                }
            }
            if (player.getRandom().nextInt(120) == 0) player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false));
        }
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        // eyes made for the dark
        if (Underside.is(level) || UmbraUtil.effectiveLight(level, eye) <= 7) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 16, 0, true, false));
            player.removeEffect(MobEffects.DARKNESS);
        }
        // and skin that remembers it: open daylight weakens you
        if (level.dimension() == Level.OVERWORLD && !UmbraUtil.isNight((ServerLevel) level) && UmbraUtil.skyLight(level, eye) >= 13 && !level.isRaining()) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 3, 0, true, true));
        }
    }

    /** Dawn: the Underside's creatures strike you a third softer. */
    public static void onIncoming(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isLight(player)) return;
        if (event.getSource().getEntity() instanceof Mob m && Bestiary.ids().containsKey(m.getType())) event.setAmount(event.getAmount() * 0.7F);
    }

    /** Dark ending: the creatures of the Underside do not turn on one of their own, unless struck first. Bosses always do. */
    public static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player) || !isDark(player)) return;
        if (!(event.getEntity() instanceof Mob mob) || mob instanceof UmbraBoss) return;
        if (!Bestiary.ids().containsKey(mob.getType())) return;
        if (mob.getLastHurtByMob() == player) return;
        event.setCanceled(true);
    }

    /** Dawn: the dark presses on you no harder than on your first night. */
    public static float dread(LivingEntity player, float dread) {
        return isLight(player) ? 1.0F : dread;
    }

    /** Drain multiplier from the ending. */
    public static float drain(LivingEntity player) {
        return isLight(player) ? 0.5F : 1.0F;
    }

    /** Highest sanity this player's mind can reach. */
    public static float cap(LivingEntity player) {
        return isDark(player) ? DARK_SANITY_CAP : Sanity.MAX;
    }
}
