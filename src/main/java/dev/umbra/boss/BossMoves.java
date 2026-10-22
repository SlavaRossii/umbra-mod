package dev.umbra.boss;

import dev.umbra.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Attacks the bosses share. Each one is telegraphed (particles or a warm-up) so it can be dodged.
 */
public final class BossMoves {
    private BossMoves() {}

    /** A line of jaws snapping out of the floor from the boss towards the target. */
    public static void fangsLine(ServerLevel level, LivingEntity boss, LivingEntity target, int count) {
        Vec3 from = boss.position(), dir = target.position().subtract(from);
        double len = Math.max(1, dir.horizontalDistance());
        float yaw = (float) Mth.atan2(dir.z, dir.x);
        for (int i = 1; i <= count; i++) {
            double t = i * 1.25;
            if (t > len + 3) break;
            Vec3 p = from.add(dir.x / len * t, 0, dir.z / len * t);
            level.addFreshEntity(new EvokerFangs(level, p.x, ground(level, p), p.z, yaw, 6 + i * 2, boss));
        }
    }

    /** A ring of jaws around a point (the target stands in the middle: step out!). */
    public static void fangsRing(ServerLevel level, LivingEntity owner, Vec3 center, double radius, int count, int warmup) {
        for (int i = 0; i < count; i++) {
            double a = i * Mth.TWO_PI / count;
            double x = center.x + Math.cos(a) * radius, z = center.z + Math.sin(a) * radius;
            level.addFreshEntity(new EvokerFangs(level, x, ground(level, new Vec3(x, center.y, z)), z, (float) a, warmup, owner));
        }
    }

    private static double ground(ServerLevel level, Vec3 p) {
        BlockPos b = BlockPos.containing(p);
        for (int i = 0; i < 6; i++) {
            if (!level.getBlockState(b.below()).isAir()) return b.getY();
            b = b.below();
        }
        return p.y;
    }

    /** A blow on the ground: everyone near is hurt and thrown back. */
    public static int shockwave(ServerLevel level, LivingEntity boss, double radius, float damage, double knock) {
        level.sendParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY() + 0.2, boss.getZ(), 6, radius / 3, 0.1, radius / 3, 0.0);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 0.6F);
        int hit = 0;
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, boss.getBoundingBox().inflate(radius))) {
            if (p.isCreative() || p.isSpectator()) continue;
            Vec3 away = p.position().subtract(boss.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > radius * radius) continue;
            boolean shield = !raisedShield(p, null).isEmpty();
            p.hurtServer(level, level.damageSources().mobAttack(boss), damage);
            if (shield) breakGuard(level, p, HEAVY_GUARD_BREAK);       // 2.9: a shield does not hold against the ground itself
            Vec3 v = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            p.push(v.x * knock, 0.45, v.z * knock);
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
            hit++;
        }
        return hit;
    }

    // --- 2.9: a boss cannot be blocked for ever ---------------------------------------------------------------
    /** Seconds a heavy blow (a shockwave, a landing, a rush) knocks a shield out for; seconds the third blocked blow does. */
    public static final float HEAVY_GUARD_BREAK = 5.0F, GUARD_BREAK = 3.0F;
    /** Blocked blows in a row after which a boss beats the shield aside. */
    public static final int GUARD_HITS = 3;

    /** The shield a player holds raised (towards {@code from}, if given), or EMPTY. */
    public static net.minecraft.world.item.ItemStack raisedShield(net.minecraft.world.entity.player.Player p, net.minecraft.world.entity.Entity from) {
        if (!p.isUsingItem()) return net.minecraft.world.item.ItemStack.EMPTY;
        net.minecraft.world.item.ItemStack use = p.getUseItem();
        if (use.get(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS) == null) return net.minecraft.world.item.ItemStack.EMPTY;
        if (from != null) {
            Vec3 to = from.position().subtract(p.position()).multiply(1, 0, 1);
            Vec3 look = p.getViewVector(1.0F).multiply(1, 0, 1);
            if (to.lengthSqr() > 1.0E-4 && look.lengthSqr() > 1.0E-4 && to.normalize().dot(look.normalize()) < 0.0) return net.minecraft.world.item.ItemStack.EMPTY;
        }
        return use;
    }

    /** Knocks the raised shield out of the guard for {@code seconds}, as an axe does. Returns true if there was one. */
    public static boolean breakGuard(ServerLevel level, net.minecraft.world.entity.player.Player p, float seconds) {
        net.minecraft.world.item.ItemStack use = raisedShield(p, null);
        if (use.isEmpty()) return false;
        net.minecraft.world.item.component.BlocksAttacks b = use.get(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS);
        b.disable(level, p, seconds, use);
        return true;
    }

    /** Leaps at the target. */
    public static void leap(LivingEntity boss, LivingEntity target, double speed) {
        Vec3 d = target.position().subtract(boss.position());
        Vec3 h = new Vec3(d.x, 0, d.z).normalize().scale(speed);
        boss.setDeltaMovement(h.x, 0.6, h.z);
    }

    /** Drags everyone within {@code radius} towards the boss. */
    public static void pull(ServerLevel level, LivingEntity boss, double radius, double strength) {
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, boss.getX(), boss.getY() + 1, boss.getZ(), 60, radius / 2, 1, radius / 2, 0.2);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 1.0F, 1.2F);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, boss.getBoundingBox().inflate(radius))) {
            if (p.isCreative() || p.isSpectator()) continue;
            Vec3 to = boss.position().subtract(p.position()).normalize().scale(strength);
            p.push(to.x, 0.25, to.z);
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
        }
    }

    /** A boss never has more than this many shades around it, however often it calls. */
    public static final int SHADE_CAP = 4;

    /** Calls up to {@code n} shades, but never above {@link #SHADE_CAP} alive within 40 blocks. Returns how many came. */
    public static int shades(ServerLevel level, LivingEntity boss, LivingEntity target, int n) {
        int room = SHADE_CAP - level.getEntitiesOfClass(dev.umbra.mob.Shade.class, boss.getBoundingBox().inflate(40.0)).size();
        int made = 0;
        for (int i = 0; i < Math.min(n, room); i++) {
            var shade = ModEntities.SHADE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (shade == null) continue;
            shade.snapTo(boss.getX() + boss.getRandom().nextGaussian() * 2.0, boss.getY(), boss.getZ() + boss.getRandom().nextGaussian() * 2.0, 0, 0);
            level.addFreshEntity(shade);
            if (target != null) shade.setTarget(target);
            made++;
        }
        return made;
    }

    /** Calls helpers around the boss, all hunting the target. */
    public static <T extends Mob> List<T> summon(ServerLevel level, LivingEntity boss, EntityType<T> type, int n, LivingEntity target) {
        List<T> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            T m = type.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (m == null) continue;
            m.snapTo(boss.getX() + boss.getRandom().nextGaussian() * 2.5, boss.getY(), boss.getZ() + boss.getRandom().nextGaussian() * 2.5, 0, 0);
            level.addFreshEntity(m);
            if (target != null) m.setTarget(target);
            level.sendParticles(ParticleTypes.SQUID_INK, m.getX(), m.getY() + 1, m.getZ(), 15, 0.3, 0.6, 0.3, 0.03);
            out.add(m);
        }
        return out;
    }

    /** Spots on the floor that glow, then burst a moment later. Delayed hits: see {@link Bursts}. */
    public static final class Bursts {
        private final List<long[]> pending = new ArrayList<>(); // x, y, z, tick

        public void mark(ServerLevel level, BlockPos at, int delay) {
            pending.add(new long[]{at.getX(), at.getY(), at.getZ(), level.getGameTime() + delay});
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, 25, 0.6, 0.05, 0.6, 0.01);
        }

        public void tick(ServerLevel level, LivingEntity owner, float damage) {
            long now = level.getGameTime();
            pending.removeIf(b -> {
                if (now < b[3]) {
                    if ((b[3] - now) % 5 == 0) level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, b[0] + 0.5, b[1] + 0.1, b[2] + 0.5, 6, 0.6, 0.05, 0.6, 0.01);
                    return false;
                }
                level.sendParticles(ParticleTypes.EXPLOSION, b[0] + 0.5, b[1] + 0.5, b[2] + 0.5, 2, 0.3, 0.3, 0.3, 0.0);
                level.playSound(null, b[0], b[1], b[2], SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 1.3F);
                for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(b[0] - 1, b[1] - 1, b[2] - 1, b[0] + 2, b[1] + 3, b[2] + 2))) {
                    if (!p.isCreative() && !p.isSpectator()) p.hurtServer(level, level.damageSources().mobAttack(owner), damage);
                }
                return true;
            });
        }

        public int size() {
            return pending.size();
        }
    }

    /** Blocks that stand for a while and then crumble back to air. */
    public static final class TempBlocks {
        private final List<long[]> placed = new ArrayList<>(); // pos, tick

        public void place(ServerLevel level, BlockPos at, net.minecraft.world.level.block.state.BlockState state, int ticks) {
            if (!level.getBlockState(at).isAir()) return;
            level.setBlock(at, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
            placed.add(new long[]{at.asLong(), level.getGameTime() + ticks});
        }

        public void tick(ServerLevel level) {
            long now = level.getGameTime();
            placed.removeIf(b -> {
                if (now < b[1]) return false;
                BlockPos p = BlockPos.of(b[0]);
                level.removeBlock(p, false);
                level.sendParticles(ParticleTypes.SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.01);
                return true;
            });
        }

        /** On death or unload nothing must stay behind. */
        public void clear(ServerLevel level) {
            for (long[] b : placed) level.removeBlock(BlockPos.of(b[0]), false);
            placed.clear();
        }

        public int size() {
            return placed.size();
        }
    }

    /**
     * 2.7.7: tears, the Watcher's attack from afar. Each tear is a drop flying in a shallow arc (a trail of falling
     * water and a glint, so it is seen coming); it bursts on the first wall or player it meets. A shield in front of you
     * stops it. A tear that strikes hurts, and the eyes fill: a few seconds of darkness and a little of your mind.
     */
    public static final class Tears {
        public static final double GRAVITY = 0.025;
        public static final int LIFE = 80;
        private final List<double[]> flying = new ArrayList<>(); // x, y, z, vx, vy, vz, age

        /** Weeps {@code n} tears from {@code from} at {@code at}, fanned over {@code spreadDeg} degrees. */
        public void shoot(ServerLevel level, Vec3 from, Vec3 at, int n, float spreadDeg, double speed) {
            Vec3 d = at.subtract(from);
            double t = Math.max(4.0, d.length() / speed);
            Vec3 v = new Vec3(d.x / t, d.y / t + 0.5 * GRAVITY * t, d.z / t);
            for (int i = 0; i < n; i++) {
                float off = n == 1 ? 0 : (i / (float) (n - 1) - 0.5F) * spreadDeg * Mth.DEG_TO_RAD;
                double c = Math.cos(off), s = Math.sin(off);
                flying.add(new double[]{from.x, from.y, from.z, v.x * c - v.z * s, v.y, v.x * s + v.z * c, 0});
            }
            level.playSound(null, from.x, from.y, from.z, SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 0.8F, 1.7F);
        }

        /** Moves the tears on; returns how many struck one of {@code targets} this tick. */
        public int tick(ServerLevel level, LivingEntity owner, List<? extends ServerPlayer> targets, float damage) {
            int[] hits = {0};
            flying.removeIf(t -> {
                Vec3 prev = new Vec3(t[0], t[1], t[2]);
                t[4] -= GRAVITY;
                t[0] += t[3];
                t[1] += t[4];
                t[2] += t[5];
                t[6]++;
                Vec3 pos = new Vec3(t[0], t[1], t[2]);
                level.sendParticles(ParticleTypes.FALLING_WATER, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.0);
                if ((int) t[6] % 2 == 0) level.sendParticles(ParticleTypes.GLOW, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
                for (ServerPlayer p : targets) {
                    AABB box = p.getBoundingBox().inflate(0.25);
                    if (p.isAlive() && (box.contains(pos) || box.clip(prev, pos).isPresent())) {
                        hits[0]++;
                        boolean hurt = p.hurtServer(level, level.damageSources().thrown(owner, owner), damage);
                        if (hurt) {
                            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 50, 0));
                            dev.umbra.sanity.Sanity.add(p, -1.0F);
                        }
                        splash(level, pos);
                        return true;
                    }
                }
                BlockPos at = BlockPos.containing(pos);
                if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty() || t[6] > LIFE) {
                    splash(level, pos);
                    return true;
                }
                return false;
            });
            return hits[0];
        }

        private static void splash(ServerLevel level, Vec3 pos) {
            level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y, pos.z, 14, 0.2, 0.15, 0.2, 0.1);
            level.sendParticles(ParticleTypes.FALLING_WATER, pos.x, pos.y, pos.z, 6, 0.25, 0.1, 0.25, 0.0);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.HOSTILE, 1.0F, 0.8F);
        }

        public int size() {
            return flying.size();
        }

        /** On death or unload nothing must keep flying. */
        public void clear() {
            flying.clear();
        }
    }

    /** Announces a new phase to everyone fighting. */
    public static void phase(ServerLevel level, UmbraBoss boss, int phase) {
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.7F, 1.4F);
        level.sendParticles(ParticleTypes.SQUID_INK, boss.getX(), boss.getY() + 1, boss.getZ(), 80, 1, 1.5, 1, 0.1);
        for (ServerPlayer p : boss.opponents(level)) {
            p.sendOverlayMessage(Component.translatable("message.umbra.boss.phase", boss.getDisplayName(), phase)
                    .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD));
        }
    }
}
