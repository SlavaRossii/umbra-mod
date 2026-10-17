package dev.umbra.underside;

import dev.umbra.light.WardBlock;
import dev.umbra.registry.ModEntities;
import dev.umbra.registry.ModSounds;
import dev.umbra.sanity.Sanity;
import dev.umbra.util.UmbraUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Life of the Underside you only hear. Water drips, a bell tolls somewhere far off, stone groans. As the mind slips
 * the sounds come closer: steps overhead, knocking in the walls, then whispers and breathing right behind you.
 * Each player hears their own (sounds are sent to them alone). Also the weather of the place: ash storms
 * over the plains and the whispering forest that listens for running feet.
 */
public final class UndersideAmbience {
    private static final Map<UUID, Integer> SPRINT = new HashMap<>();
    private static final Map<UUID, Long> FOREST_COOLDOWN = new HashMap<>();

    private UndersideAmbience() {}

    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !Underside.is(level)) return;
        forest(level, player);
        if (player.tickCount % 20 != 7) return;
        RandomSource r = player.getRandom();
        float sanity = Sanity.enabled() ? Sanity.get(player) : 100.0F;
        boolean rested = WardBlock.protects(player);
        BlockPos pos = player.blockPosition();

        // the place itself
        if (r.nextInt(12) == 0) play(player, ModSounds.DRIP, around(player, r, 3, 9).add(0, 2 + r.nextInt(5), 0), 0.8F, 0.9F + r.nextFloat() * 0.3F);
        if (r.nextInt(70) == 0) play(player, ModSounds.CREAK, around(player, r, 6, 14), 1.0F, 0.8F + r.nextFloat() * 0.4F);
        if (r.nextInt(160) == 0) play(player, ModSounds.BELL, around(player, r, 45, 55), 4.0F, 0.9F + r.nextFloat() * 0.2F);
        if (UndersideBiomes.in(level, pos, UndersideBiomes.BLACK_LAKES) && r.nextInt(35) == 0) {
            play(player, ModSounds.SPLASH, around(player, r, 10, 22), 1.6F, 0.7F + r.nextFloat() * 0.4F);
        }

        // the storm
        float storm = AshStorm.at(player);
        if (storm > 0.3F) {
            if (r.nextInt(5) == 0) play(player, ModSounds.GUST, around(player, r, 3, 7), 0.25F + 0.3F * storm, 0.8F + r.nextFloat() * 0.3F);   // 2.5: quieter, it was drowning everything
            if (r.nextInt(30) == 0) play(player, ModSounds.STEPS_BEHIND, behind(player, 5 + r.nextInt(4)), 1.0F, 0.9F);
            if (storm > 0.9F && r.nextInt(10) == 0) Sanity.add(player, -0.5F);
        }

        if (rested) return;
        // a slipping mind hears more
        if (sanity < 60) {
            if (r.nextInt(90) == 0) play(player, ModSounds.STEPS_ABOVE, player.position().add(r.nextGaussian() * 3, 6, r.nextGaussian() * 3), 1.2F, 0.9F + r.nextFloat() * 0.2F);
            if (r.nextInt(150) == 0) play(player, ModSounds.KNOCK, around(player, r, 3, 6), 1.0F, 0.9F + r.nextFloat() * 0.2F);
        }
        if (sanity < 35) {
            if (r.nextInt(45) == 0) play(player, ModSounds.WHISPER, behind(player, 1.5), 0.8F, 0.85F + r.nextFloat() * 0.3F);
            if (r.nextInt(80) == 0) play(player, ModSounds.STEPS_BEHIND, behind(player, 4), 1.0F, 1.0F);
            if (r.nextInt(120) == 0) play(player, ModSounds.BREATH, behind(player, 1.0), 0.7F, 0.9F + r.nextFloat() * 0.2F);
        }
        if (sanity < 15) {
            if (r.nextInt(60) == 0) play(player, ModSounds.WAIL, around(player, r, 30, 45), 3.0F, 0.8F + r.nextFloat() * 0.3F);
            if (r.nextInt(80) == 0) play(player, ModSounds.HEARTBEAT_FAR, player.position(), 0.8F, 1.0F);
        }
    }

    /** The whispering forest hears running feet. Run too long and everything nearby comes for you. */
    private static void forest(ServerLevel level, ServerPlayer player) {
        UUID id = player.getUUID();
        boolean inForest = UndersideBiomes.in(level, player.blockPosition(), UndersideBiomes.WHISPERING_FOREST);
        int run = SPRINT.getOrDefault(id, 0);
        if (!inForest || player.isCreative()) {
            if (run != 0) SPRINT.remove(id);
            return;
        }
        run = player.isSprinting() ? run + 1 : Math.max(0, run - 2);
        SPRINT.put(id, run);
        if (run > 0 && run % 40 == 0) {
            play(player, ModSounds.WHISPER, behind(player, 2.0), 1.0F, 0.8F + player.getRandom().nextFloat() * 0.3F);
            Sanity.add(player, -1.0F);
            if (run == 40) player.sendOverlayMessage(Component.translatable("message.umbra.forest.hush"));
        }
        if (run >= FOREST_ALERT) {
            SPRINT.put(id, 0);
            long now = level.getGameTime();
            if (now < FOREST_COOLDOWN.getOrDefault(id, 0L)) return;
            FOREST_COOLDOWN.put(id, now + 20 * 60);
            alert(level, player);
        }
    }

    public static final int FOREST_ALERT = 120;

    /** The forest heard you: every monster near turns to you, and shades step out from the trees. Returns how many came. */
    public static int alert(ServerLevel level, ServerPlayer player) {
        int n = 0;
        for (Monster m : level.getEntitiesOfClass(Monster.class, new AABB(player.blockPosition()).inflate(24))) {
            m.setTarget(player);
            n++;
        }
        RandomSource r = player.getRandom();
        for (int i = 0; i < 2; i++) {
            var shade = ModEntities.SHADE.get().create(level, EntitySpawnReason.EVENT);
            if (shade == null) continue;
            Vec3 at = behind(player, 8 + r.nextInt(5));
            BlockPos stand = dev.umbra.sanity.Hallucinations.findStandable(level, BlockPos.containing(at), 4);
            if (stand == null) {
                shade.discard();
                continue;
            }
            shade.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
            level.addFreshEntity(shade);
            shade.setTarget(player);
            n++;
        }
        play(player, ModSounds.WAIL, around(player, r, 20, 30), 3.0F, 1.3F);
        player.sendOverlayMessage(Component.translatable("message.umbra.forest.heard"));
        Sanity.add(player, -4.0F);
        return n;
    }

    public static void forget(ServerPlayer player) {
        SPRINT.remove(player.getUUID());
        FOREST_COOLDOWN.remove(player.getUUID());
    }

    public static void play(ServerPlayer player, Holder<SoundEvent> sound, Vec3 at, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(sound, SoundSource.AMBIENT, at.x, at.y, at.z, volume, pitch, player.getRandom().nextLong()));
    }

    private static Vec3 around(ServerPlayer player, RandomSource r, double min, double max) {
        double a = r.nextDouble() * Mth.TWO_PI, d = min + r.nextDouble() * (max - min);
        return player.position().add(Math.cos(a) * d, r.nextGaussian(), Math.sin(a) * d);
    }

    private static Vec3 behind(ServerPlayer player, double d) {
        Vec3 look = UmbraUtil.horizontal(player.getLookAngle());
        if (look.lengthSqr() < 1.0E-4) look = new Vec3(0, 0, 1);
        return player.position().add(look.normalize().scale(-d)).add(0, 1.4, 0);
    }
}
