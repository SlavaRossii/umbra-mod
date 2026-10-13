package dev.umbra.shadow;

import dev.umbra.Umbra;
import dev.umbra.registry.ModEntities;
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
            if (player.getRandom().nextInt(SPAWN_CHANCE) != 0) return;
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
        shadow.snapTo(start.x, start.y, start.z, player.getYRot(), 0.0F);
        level.addFreshEntity(shadow);
        s.shadowId = shadow.getUUID();

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.35F, 0.5F);
        player.displayClientMessage(Component.translatable("message.umbra.shadow_detached"), true);
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
        player.displayClientMessage(Component.translatable("message.umbra.shadow_caught"), true);
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
