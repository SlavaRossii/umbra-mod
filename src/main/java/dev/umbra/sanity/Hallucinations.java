package dev.umbra.sanity;

import dev.umbra.registry.ModEntities;
import dev.umbra.util.UmbraUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** What a frightened mind adds to the world. Purely cosmetic: hallucinations never hurt. */
public final class Hallucinations {
    private static final List<SoundEvent> FAKE_SOUNDS = List.of(
            SoundEvents.CREEPER_PRIMED,
            SoundEvents.ZOMBIE_AMBIENT,
            SoundEvents.SKELETON_AMBIENT,
            SoundEvents.SPIDER_AMBIENT,
            SoundEvents.ENDERMAN_AMBIENT,
            SoundEvents.WOODEN_DOOR_OPEN,
            SoundEvents.GLASS_BREAK,
            SoundEvents.STONE_STEP,
            SoundEvents.WARDEN_HEARTBEAT,
            SoundEvents.WOODEN_DOOR_CLOSE,
            SoundEvents.CHEST_OPEN,
            SoundEvents.CHEST_CLOSE,
            SoundEvents.TNT_PRIMED,
            SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
            SoundEvents.STONE_BREAK,
            SoundEvents.GRAVEL_STEP,
            SoundEvents.FIRE_EXTINGUISH,
            SoundEvents.ENDERMAN_STARE,
            SoundEvents.SCULK_SHRIEKER_SHRIEK,
            SoundEvents.PLAYER_HURT,
            SoundEvents.SKELETON_SHOOT);

    /** Phantom looks: familiar mobs (index into {@link #LOOKS}). */
    public static final int LOOKS = 4;

    private Hallucinations() {}

    /** Called once a second from {@link Sanity#tick}. */
    static void tick(ServerLevel level, ServerPlayer player, float sanity) {
        RandomSource random = player.getRandom();
        if (sanity < Sanity.ANXIOUS) {
            int chance = sanity < Sanity.BREAKDOWN ? 12 : sanity < Sanity.PARANOID ? 25 : 45;
            if (random.nextInt(chance) == 0) fakeSound(level, player, random);
        }
        if (sanity < Sanity.PARANOID) {
            int chance = sanity < Sanity.BREAKDOWN ? 60 : 110;
            if (random.nextInt(chance) == 0) spawnFigure(level, player, random);
        }
        // phantoms, in tiers that match the insights
        if (sanity < 45.0F && random.nextInt(70) == 0) spawnPhantom(level, player, PhantomEntity.Mode.WATCH, random);
        if (sanity < 30.0F && random.nextInt(90) == 0) spawnPhantom(level, player, PhantomEntity.Mode.AMBUSH, random);
        if (sanity < 15.0F && random.nextInt(110) == 0 && phantoms(level, player, PhantomEntity.Mode.SHADOW) < 2) {
            spawnPhantom(level, player, PhantomEntity.Mode.SHADOW, random);
        }
        // your name, whispered
        if (sanity < 30.0F && random.nextInt(400) == 0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.umbra.whisper_name", player.getName())
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC));
        }
    }

    private static int phantoms(ServerLevel level, ServerPlayer player, PhantomEntity.Mode mode) {
        return level.getEntitiesOfClass(PhantomEntity.class, player.getBoundingBox().inflate(48), p -> p.mode() == mode && p.shownTo(player)).size();
    }

    /** Spawns a phantom: watchers far off, ambushers right in front, shadows out of sight. */
    public static boolean spawnPhantom(ServerLevel level, ServerPlayer player, PhantomEntity.Mode mode, RandomSource random) {
        Vec3 look = UmbraUtil.horizontal(player.getLookAngle());
        if (look == Vec3.ZERO) look = new Vec3(0, 0, 1);
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle, dist;
            switch (mode) {
                case WATCH -> { angle = Math.toRadians(random.nextInt(120) - 60); dist = 16 + random.nextInt(12); }
                case AMBUSH -> { angle = Math.toRadians(random.nextInt(30) - 15); dist = 4 + random.nextInt(2); }
                default -> { angle = Math.toRadians(120 + random.nextInt(120)); dist = 10 + random.nextInt(8); }
            }
            Vec3 dir = new Vec3(look.x * Math.cos(angle) - look.z * Math.sin(angle), 0, look.x * Math.sin(angle) + look.z * Math.cos(angle));
            BlockPos ground = findStandable(level, BlockPos.containing(player.position().add(dir.scale(dist))), 6);
            if (ground == null) continue;
            var type = mode == PhantomEntity.Mode.SHADOW ? ModEntities.PHANTOM_SHADOW.get() : ModEntities.PHANTOMS.get(random.nextInt(ModEntities.PHANTOMS.size())).get();
            PhantomEntity phantom = type.create(level, EntitySpawnReason.EVENT);
            if (phantom == null) return false;
            phantom.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 0, 0);
            phantom.setup(mode, player);
            return level.addFreshEntity(phantom);
        }
        return false;
    }

    public static void fakeSound(ServerLevel level, ServerPlayer player, RandomSource random) {
        SoundEvent sound = FAKE_SOUNDS.get(random.nextInt(FAKE_SOUNDS.size()));
        Vec3 behind = UmbraUtil.horizontal(player.getLookAngle()).scale(-(3 + random.nextInt(6)))
                .add(random.nextGaussian() * 2, 0, random.nextGaussian() * 2);
        Vec3 at = player.position().add(behind);
        dev.umbra.util.Private.sound(player, sound, SoundSource.HOSTILE, at.x, at.y, at.z, 0.6F, 0.9F + random.nextFloat() * 0.2F);
    }

    /** A still figure at the edge of your vision. It is gone when you look straight at it or come close. */
    public static boolean spawnFigure(ServerLevel level, ServerPlayer player, RandomSource random) {
        Vec3 look = UmbraUtil.horizontal(player.getLookAngle());
        if (look == Vec3.ZERO) look = new Vec3(0, 0, 1);
        for (int attempt = 0; attempt < 10; attempt++) {
            // 60..150 degrees to either side: peripheral vision or behind
            double angle = Math.toRadians((60 + random.nextInt(90)) * (random.nextBoolean() ? 1 : -1));
            Vec3 dir = new Vec3(look.x * Math.cos(angle) - look.z * Math.sin(angle), 0, look.x * Math.sin(angle) + look.z * Math.cos(angle));
            double dist = 14 + random.nextInt(10);
            Vec3 target = player.position().add(dir.scale(dist));
            BlockPos ground = findStandable(level, BlockPos.containing(target), 8);
            if (ground == null) continue;
            HallucinationEntity figure = ModEntities.HALLUCINATION.get().create(level, EntitySpawnReason.EVENT);
            if (figure == null) return false;
            figure.setOwner(player);
            Vec3 face = player.position().subtract(Vec3.atBottomCenterOf(ground));
            float yaw = (float) (Math.toDegrees(Math.atan2(face.z, face.x)) - 90.0);
            figure.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, yaw, 0.0F);
            figure.setYHeadRot(yaw);
            figure.setYBodyRot(yaw);
            return level.addFreshEntity(figure);
        }
        return false;
    }

    /** Finds air with solid ground below and two blocks of headroom, searching up and down. */
    public static BlockPos findStandable(ServerLevel level, BlockPos start, int range) {
        for (int dy = 0; dy <= range; dy++) {
            for (int sign : new int[]{1, -1}) {
                BlockPos p = start.above(dy * sign);
                if (!level.isLoaded(p)) continue;
                if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                        && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)) {
                    return p;
                }
            }
        }
        return null;
    }
}
