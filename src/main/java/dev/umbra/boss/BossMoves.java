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
            p.hurtServer(level, level.damageSources().mobAttack(boss), damage);
            Vec3 v = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            p.push(v.x * knock, 0.45, v.z * knock);
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
            hit++;
        }
        return hit;
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
