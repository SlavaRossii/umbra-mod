package dev.umbra.util;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.UUID;

/**
 * What only one mind perceives. On a server the others must not hear your footsteps-that-are-not-there or see
 * the figure at the edge of your vision: these go to one player's connection, and nowhere else.
 */
public final class Private {
    private Private() {}

    public static void sound(ServerPlayer player, SoundEvent sound, SoundSource source, double x, double y, double z, float volume, float pitch) {
        sound(player, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source, x, y, z, volume, pitch);
    }

    public static void sound(ServerPlayer player, Holder<SoundEvent> sound, SoundSource source, double x, double y, double z, float volume, float pitch) {
        if (player.connection == null) return;
        player.connection.send(new ClientboundSoundPacket(sound, source, x, y, z, volume, pitch, player.getRandom().nextLong()));
    }

    /** A sound right where the player stands. */
    public static void sound(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch) {
        sound(player, sound, source, player.getX(), player.getY(), player.getZ(), volume, pitch);
    }

    public static void sound(ServerPlayer player, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch) {
        sound(player, sound, source, player.getX(), player.getY(), player.getZ(), volume, pitch);
    }

    public static void particles(ServerPlayer player, ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (player.level() instanceof ServerLevel level) level.sendParticles(player, particle, false, false, x, y, z, count, dx, dy, dz, speed);
    }

    /** An entity seen by one player only: everyone else's client never learns it exists. */
    public interface Vision {
        UUID seer();

        default boolean shownTo(ServerPlayer player) {
            UUID seer = seer();
            return seer != null && seer.equals(player.getUUID());
        }
    }
}
