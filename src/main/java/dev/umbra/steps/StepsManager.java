package dev.umbra.steps;

import dev.umbra.UmbraConfig;
import dev.umbra.sanity.Sanity;
import dev.umbra.util.UmbraUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Footsteps behind you": during an episode every step the player takes is echoed by a step behind them,
 * with a small delay. The echo gets closer and the delay shorter. Turning around ends it — nobody is there.
 */
public final class StepsManager {
    private static final int START_CHANCE = 6000;      // ~5 minutes on average in the dark
    private static final int COOLDOWN = 20 * 60 * 4;
    private static final double STRIDE = 1.7;           // blocks per footstep
    private static final double START_DISTANCE = 14.0;
    private static final double END_DISTANCE = 1.6;
    private static final int START_DELAY = 12;
    private static final int END_DELAY = 3;
    private static final int STEPS_PER_EPISODE = 28;

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        boolean active;
        int stepsDone;
        double walked;
        int cooldown = 20 * 60;
        Vec3 last;
        Vec3 moveDir = Vec3.ZERO;
        long tick;
        final List<Echo> pending = new ArrayList<>();
    }

    private record Echo(long at, Vec3 pos) {}

    private StepsManager() {}

    public static void forget(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    public static boolean forceStart(ServerPlayer player) {
        State s = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        start(s);
        return true;
    }

    private static void start(State s) {
        s.active = true;
        s.stepsDone = 0;
        s.walked = 0;
        s.pending.clear();
    }

    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        State s = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        s.tick++;
        Vec3 pos = player.position();
        if (s.last == null) s.last = pos;
        Vec3 delta = pos.subtract(s.last);
        s.last = pos;
        double horiz = Math.sqrt(delta.horizontalDistanceSqr());
        if (horiz > 0.05) s.moveDir = UmbraUtil.horizontal(delta);

        playPending(level, s);

        if (!s.active) {
            if (s.cooldown > 0) { s.cooldown--; return; }
            if (player.isCreative() || !UmbraConfig.on(UmbraConfig.STEPS)) return;
            if (!isSpooky(level, player)) return;
            if (player.getRandom().nextDouble() < Sanity.dread(player) / START_CHANCE) start(s);
            return;
        }

        // Turned around? The steps stop. There is nobody there.
        Vec3 look = UmbraUtil.horizontal(player.getLookAngle());
        if (s.moveDir != Vec3.ZERO && look.dot(s.moveDir) < -0.5) {
            stop(s);
            return;
        }
        if (horiz > 2.0) { // teleport or vehicle: abort quietly
            stop(s);
            return;
        }

        if (player.onGround() && !player.isShiftKeyDown()) {
            s.walked += horiz;
        }
        if (s.walked >= STRIDE) {
            s.walked = 0;
            float progress = Math.min(1.0F, s.stepsDone / (float) STEPS_PER_EPISODE);
            double dist = START_DISTANCE + (END_DISTANCE - START_DISTANCE) * progress;
            int delay = Math.round(START_DELAY + (END_DELAY - START_DELAY) * progress);
            Vec3 behind = s.moveDir == Vec3.ZERO ? look : s.moveDir;
            Vec3 echoPos = pos.subtract(behind.scale(dist));
            s.pending.add(new Echo(s.tick + delay, echoPos));
            s.stepsDone++;
            if (s.stepsDone > STEPS_PER_EPISODE) finale(level, player, s);
        }
    }

    private static boolean isSpooky(ServerLevel level, ServerPlayer player) {
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        boolean underground = UmbraUtil.skyLight(level, eye) == 0;
        return UmbraUtil.isNight(level) || underground;
    }

    private static void playPending(ServerLevel level, State s) {
        Iterator<Echo> it = s.pending.iterator();
        while (it.hasNext()) {
            Echo e = it.next();
            if (e.at() > s.tick) continue;
            it.remove();
            BlockPos ground = BlockPos.containing(e.pos().x, e.pos().y - 0.2, e.pos().z);
            for (int i = 0; i < 4 && level.getBlockState(ground).isAir(); i++) ground = ground.below();
            BlockState state = level.getBlockState(ground);
            SoundEvent step = state.isAir() ? SoundEvents.STONE_STEP : state.getSoundType().getStepSound();
            float volume = state.getSoundType().getVolume() * 0.3F;
            level.playSound(null, e.pos().x, e.pos().y, e.pos().z, step, SoundSource.HOSTILE, volume, state.getSoundType().getPitch());
        }
    }

    private static void finale(ServerLevel level, ServerPlayer player, State s) {
        Vec3 p = player.getEyePosition();
        level.playSound(null, p.x, p.y, p.z, SoundEvents.WARDEN_NEARBY_CLOSEST, SoundSource.HOSTILE, 0.6F, 1.6F);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 4, 0));
        Sanity.add(player, Sanity.Shock.STEPS_FINALE);
        stop(s);
    }

    private static void stop(State s) {
        s.active = false;
        s.pending.clear();
        s.cooldown = COOLDOWN;
    }

    public static String debug(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        if (s == null) return "no state";
        return "active=" + s.active + " steps=" + s.stepsDone + " cooldown=" + s.cooldown;
    }
}
