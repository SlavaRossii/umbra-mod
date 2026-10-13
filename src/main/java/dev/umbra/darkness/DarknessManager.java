package dev.umbra.darkness;

import dev.umbra.UmbraConfig;
import dev.umbra.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Seeds living darkness in deep caves around players and hurts players who stay inside it. */
public final class DarknessManager {
    private static final int SEED_INTERVAL = 100;
    private static final int SEED_MAX_Y = 40;
    private static final int SEED_RADIUS = 24;
    private static final int GRACE_TICKS = 20 * 5;

    private static final Map<UUID, Integer> INSIDE = new HashMap<>();

    private DarknessManager() {}

    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;

        // inside the darkness
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        if (level.getBlockState(eye).is(ModBlocks.LIVING_DARKNESS.get()) && !player.isCreative()) {
            int t = INSIDE.merge(player.getUUID(), 1, Integer::sum);
            if (t % 20 == 1) player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
            if (t > GRACE_TICKS && t % 40 == 0) player.hurtServer(level, level.damageSources().magic(), 1.0F);
        } else {
            INSIDE.remove(player.getUUID());
        }

        // seeding
        if (player.tickCount % SEED_INTERVAL != 0 || !UmbraConfig.on(UmbraConfig.DARKNESS)) return;
        if (level.dimension() != Level.OVERWORLD || player.getY() > SEED_MAX_Y + 16) return;
        RandomSource random = level.getRandom();
        if (random.nextInt(3) != 0) return;
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos pos = player.blockPosition().offset(
                    random.nextInt(SEED_RADIUS * 2 + 1) - SEED_RADIUS,
                    random.nextInt(25) - 12,
                    random.nextInt(SEED_RADIUS * 2 + 1) - SEED_RADIUS);
            if (pos.getY() > SEED_MAX_Y || !level.isLoaded(pos)) continue;
            if (pos.distSqr(player.blockPosition()) < 8 * 8) continue; // never right next to the player
            if (LivingDarknessBlock.seed(level, pos, ModBlocks.LIVING_DARKNESS.get())) return;
        }
    }

    /** /umbra darkness: plants a cluster around a point. */
    public static int seedCluster(ServerLevel level, BlockPos center, int count) {
        int placed = 0;
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < count * 20 && placed < count; attempt++) {
            BlockPos pos = center.offset(random.nextInt(9) - 4, random.nextInt(5) - 2, random.nextInt(9) - 4);
            if (LivingDarknessBlock.seed(level, pos, ModBlocks.LIVING_DARKNESS.get())) placed++;
        }
        return placed;
    }
}
