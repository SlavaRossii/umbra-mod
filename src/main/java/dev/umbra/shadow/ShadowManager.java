package dev.umbra.shadow;

import dev.umbra.Umbra;
import dev.umbra.UmbraConfig;
import dev.umbra.sanity.Sanity;
import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModEntities;
import dev.umbra.underside.Underside;
import net.minecraft.world.entity.monster.Monster;
import dev.umbra.util.UmbraUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Records every player's path as breadcrumbs. At night the shadow detaches and walks that exact path.
 * While the player keeps moving it falls behind; when the player stands still it catches up.
 */
public final class ShadowManager {
    private static final double CRUMB_SPACING = 0.5;
    private static final int MAX_TRAIL = 600;           // ~300 blocks of path
    private static final int MIN_TRAIL_TO_SPAWN = 120;  // player walked ~60 blocks this night
    private static final int SPAWN_BEHIND = 70;         // shadow appears ~35 blocks back along the path
    private static final int SPAWN_CHANCE = 1500;       // 1 in N per tick once conditions are met (~75 s)
    private static final int COOLDOWN_AFTER_CATCH = 20 * 60 * 5;
    private static final int COOLDOWN_AFTER_DISPEL = 20 * 60 * 3;
    private static final double BASE_SPEED = 0.17;      // blocks per tick: slower than walking (0.216), faster than sneaking
    private static final double CATCH_DISTANCE = 1.1;

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        final ArrayDeque<Vec3> trail = new ArrayDeque<>();
        Vec3 last;
        UUID shadowId;
        int cooldown;
        /** Freed shadow told to stay: where it guards. */
        Vec3 stayAt;
    }

    private ShadowManager() {}

    public static void forget(ServerPlayer player) {
        State s = STATES.remove(player.getUUID());
        if (s != null) {
            ShadowEntity shadow = find(player.level(), s);
            if (shadow != null) shadow.dissipate();
        }
    }

    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        State s = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        if (s.cooldown > 0) s.cooldown--;

        recordTrail(player, s);

        if (isFreed(player)) {
            allyTick(level, player, s);
            return;
        }

        ShadowEntity shadow = find(level, s);
        if (s.shadowId != null && shadow == null) {
            // it was dispelled by the player or removed some other way
            s.shadowId = null;
            s.cooldown = Math.max(s.cooldown, COOLDOWN_AFTER_DISPEL);
        }

        boolean night = UmbraUtil.isNight(level);
        if (shadow == null) {
            if (!night) {
                s.trail.clear();
                return;
            }
            if (player.isCreative() || s.cooldown > 0 || s.trail.size() < MIN_TRAIL_TO_SPAWN) return;
            if (!UmbraConfig.on(UmbraConfig.SHADOW)) return;
            if (dev.umbra.light.WardBlock.protects(player)) return;
            double chance = UmbraConfig.num(UmbraConfig.SHADOW_FREQUENCY) * Sanity.dread(player) / SPAWN_CHANCE;
            if (player.getRandom().nextDouble() >= chance) return;
            spawn(level, player, s, false);
            return;
        }

        shadow.markManaged();
        if (!night || player.isCreative() || shadow.distanceToSqr(player) > 128 * 128) {
            shadow.dissipate();
            s.shadowId = null;
            return;
        }
        moveShadow(level, player, s, shadow);
    }

    public static boolean isFreed(ServerPlayer player) {
        return player.getData(ModAttachments.FREED);
    }

    /** Called when the Primordial Shadow falls: this player's shadow becomes an ally. */
    public static void free(ServerPlayer player) {
        player.setData(ModAttachments.FREED, true);
        dev.umbra.journal.Journal.unlock(player, "freed");
        State s = STATES.get(player.getUUID());
        if (s != null) {
            ShadowEntity old = find(player.level(), s);
            if (old != null) old.dissipate();
            s.shadowId = null;
        }
    }

    /** Shift + right-click on your freed shadow: stay here / follow me. Returns true if it now stays. */
    public static boolean toggleStay(ServerPlayer player, ShadowEntity shadow) {
        State s = STATES.get(player.getUUID());
        if (s == null || !shadow.getUUID().equals(s.shadowId)) return false;
        s.stayAt = s.stayAt == null ? shadow.position() : null;
        boolean stay = s.stayAt != null;
        player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(stay ? "message.umbra.ally.stay" : "message.umbra.ally.follow"));
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, shadow.getX(), shadow.getY() + 1, shadow.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
            level.playSound(null, shadow.getX(), shadow.getY(), shadow.getZ(), net.minecraft.sounds.SoundEvents.SOUL_ESCAPE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, stay ? 0.7F : 1.3F);
        }
        return stay;
    }

    public static boolean isStaying(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        return s != null && s.stayAt != null;
    }

    /** The freed shadow walks beside you at night and in the Underside, and strikes monsters that come close. */
    private static void allyTick(ServerLevel level, ServerPlayer player, State s) {
        boolean active = UmbraUtil.isNight(level) || Underside.is(level);
        ShadowEntity ally = find(level, s);
        if (!active || player.isSpectator()) {
            if (ally != null) ally.dissipate();
            s.shadowId = null;
            s.stayAt = null;
            return;
        }
        // too far from its guard post: it comes back to you
        if (s.stayAt != null && s.stayAt.distanceToSqr(player.position()) > 64 * 64) s.stayAt = null;
        if (ally == null) {
            ally = ModEntities.SHADOW.get().create(level, EntitySpawnReason.EVENT);
            if (ally == null) return;
            ally.makeAlly();
            ally.mirror(player.getUUID());
            ally.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
            level.addFreshEntity(ally);
            s.shadowId = ally.getUUID();
        }
        ally.markManaged();
        ally.makeAlly();
        if (s.stayAt != null) {
            ally.setPos(s.stayAt.x, s.stayAt.y, s.stayAt.z);
        } else {
            Vec3 side = UmbraUtil.horizontal(player.getLookAngle()).scale(-1.5).add(player.position());
            Vec3 pos = ally.position().lerp(side, 0.25);
            ally.setPos(pos.x, side.y, pos.z);
            ally.setYRot(player.getYRot());
            ally.setYHeadRot(player.getYHeadRot());
            ally.setYBodyRot(player.getYRot());
        }
        if (player.tickCount % 20 == 0) {
            Monster prey = null;
            double best = 36.0;
            for (Monster m : level.getEntitiesOfClass(Monster.class, ally.getBoundingBox().inflate(6.0))) {
                if (m instanceof ShadowEntity) continue;
                double d = m.distanceToSqr(ally);
                if (d < best) {
                    best = d;
                    prey = m;
                }
            }
            if (prey != null) {
                prey.hurtServer(level, level.damageSources().playerAttack(player), dev.umbra.ending.Endings.isDark(player) ? 10.0F : 5.0F);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, prey.getX(), prey.getY() + 1, prey.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
            }
        }
    }

    /** Used by /umbra shadow. */
    public static boolean forceSpawn(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        State s = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        ShadowEntity old = find(level, s);
        if (old != null) old.dissipate();
        s.cooldown = 0;
        return spawn(level, player, s, true);
    }

    private static void recordTrail(ServerPlayer player, State s) {
        Vec3 pos = player.position();
        if (s.last == null) {
            s.last = pos;
            s.trail.addLast(pos);
            return;
        }
        double d = pos.distanceTo(s.last);
        if (d > 10.0) {
            // teleported: the path is broken
            s.trail.clear();
            s.trail.addLast(pos);
            s.last = pos;
            return;
        }
        if (d >= CRUMB_SPACING) {
            s.trail.addLast(pos);
            s.last = pos;
            while (s.trail.size() > MAX_TRAIL) s.trail.pollFirst();
        }
    }

    private static boolean spawn(ServerLevel level, ServerPlayer player, State s, boolean forced) {
        Vec3 start;
        if (s.trail.size() > SPAWN_BEHIND) {
            while (s.trail.size() > SPAWN_BEHIND) s.trail.pollFirst();
            start = s.trail.peekFirst();
        } else if (forced) {
            Vec3 back = UmbraUtil.horizontal(player.getLookAngle()).scale(-12.0);
            start = player.position().add(back);
            s.trail.clear();
            s.trail.addLast(player.position());
        } else {
            return false;
        }

        ShadowEntity shadow = ModEntities.SHADOW.get().create(level, EntitySpawnReason.EVENT);
        if (shadow == null) return false;
        shadow.markManaged();
        shadow.mirror(player.getUUID());
        shadow.snapTo(start.x, start.y, start.z, player.getYRot(), 0.0F);
        level.addFreshEntity(shadow);
        s.shadowId = shadow.getUUID();

        dev.umbra.util.Private.sound(player, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.35F, 0.5F);
        player.sendOverlayMessage(Component.translatable("message.umbra.shadow_detached"));
        Sanity.add(player, Sanity.Shock.SHADOW_DETACHED);
        dev.umbra.journal.Journal.unlock(player, "shadow");
        Umbra.LOGGER.debug("Shadow detached for {} at {}", player.getName().getString(), start);
        return true;
    }

    private static void moveShadow(ServerLevel level, ServerPlayer player, State s, ShadowEntity shadow) {
        Vec3 pos = shadow.position();
        BlockPos bp = BlockPos.containing(pos.x, pos.y + 1.0, pos.z);

        double speed = BASE_SPEED;
        int light = UmbraUtil.effectiveLight(level, bp);
        if (light >= 10) speed *= 0.35;       // light slows it down
        else if (light <= 2) speed *= 1.4;    // darkness makes it faster

        double remaining = speed;
        Vec3 cur = pos;
        while (remaining > 1.0E-4) {
            Vec3 target = s.trail.peekFirst();
            if (target == null) target = player.position();
            double dist = cur.distanceTo(target);
            if (dist <= remaining) {
                cur = target;
                remaining -= dist;
                if (s.trail.isEmpty()) break;
                s.trail.pollFirst();
            } else {
                cur = cur.add(target.subtract(cur).scale(remaining / dist));
                remaining = 0;
            }
        }

        Vec3 delta = cur.subtract(pos);
        if (delta.horizontalDistanceSqr() > 1.0E-6) {
            float yaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
            shadow.setYRot(yaw);
            shadow.setYHeadRot(yaw);
            shadow.setYBodyRot(yaw);
        }
        shadow.setPos(cur.x, cur.y, cur.z);
        shadow.setDeltaMovement(delta);

        if (shadow.tickCount % 40 == 0) {
            level.playSound(null, cur.x, cur.y, cur.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.25F, 0.6F);
        }

        if (shadow.distanceTo(player) < CATCH_DISTANCE) {
            caught(level, player, s, shadow);
        }
    }

    private static void caught(ServerLevel level, ServerPlayer player, State s, ShadowEntity shadow) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.0F, 0.4F);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 12, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 6, 1));
        player.hurtServer(level, level.damageSources().magic(), 7.0F);
        player.sendOverlayMessage(Component.translatable("message.umbra.shadow_caught"));
        Sanity.add(player, Sanity.Shock.SHADOW_CAUGHT);
        shadow.dissipate();
        s.shadowId = null;
        s.trail.clear();
        s.cooldown = COOLDOWN_AFTER_CATCH;
    }

    private static ShadowEntity find(net.minecraft.world.level.Level level, State s) {
        if (s.shadowId == null || !(level instanceof ServerLevel sl)) return null;
        Entity e = sl.getEntity(s.shadowId);
        return e instanceof ShadowEntity shadow && e.isAlive() ? shadow : null;
    }

    /** For /umbra status. */
    public static String debug(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        if (s == null) return "no state";
        return "trail=" + s.trail.size() + " cooldown=" + s.cooldown + " shadow=" + (s.shadowId != null);
    }
}
