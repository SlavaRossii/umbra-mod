package dev.umbra.mimic;

import dev.umbra.Umbra;
import dev.umbra.UmbraConfig;
import dev.umbra.sanity.Sanity;
import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * While the player is away from home, something moves in. When they come back,
 * one or two ordinary blocks of the base may have been replaced by mimics.
 */
public final class MimicManager {
    private static final double AWAY_DISTANCE = 64.0;
    private static final double RETURN_DISTANCE = 40.0;
    private static final int MIN_AWAY_TICKS = 20 * 60 * 4;   // gone at least 4 minutes
    private static final int SEARCH_RADIUS = 12;
    private static final int SEARCH_HEIGHT = 6;

    private static final Map<UUID, Integer> AWAY = new HashMap<>();

    private MimicManager() {}

    public static void setHome(ServerPlayer player, BlockPos pos) {
        player.setData(ModAttachments.HOME, pos.asLong());
    }

    public static BlockPos getHome(ServerPlayer player) {
        long packed = player.getData(ModAttachments.HOME);
        return packed == Long.MIN_VALUE ? null : BlockPos.of(packed);
    }

    public static void forget(ServerPlayer player) {
        AWAY.remove(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;
        // after the dawn nothing moves into your house any more
        if (dev.umbra.ending.Endings.isLight(player)) return;
        if (!(player.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (player.tickCount % WILD_INTERVAL == WILD_INTERVAL / 2) wild(level, player);
        BlockPos home = getHome(player);
        if (home == null) return;

        double dist = Math.sqrt(player.blockPosition().distSqr(home));
        int away = AWAY.getOrDefault(player.getUUID(), 0);
        if (dist > AWAY_DISTANCE) {
            AWAY.put(player.getUUID(), away + 20);
        } else if (dist < RETURN_DISTANCE && away > 0) {
            AWAY.put(player.getUUID(), 0);
            float chance = Math.min(0.95F, 0.6F * (float) Math.sqrt(Sanity.dread(player)));
            if (away >= MIN_AWAY_TICKS && !player.isCreative() && !dev.umbra.light.WardBlock.protects(level, net.minecraft.world.phys.Vec3.atCenterOf(home)) && UmbraConfig.on(UmbraConfig.MIMICS) && level.getRandom().nextFloat() < chance) {
                int count = 1 + level.getRandom().nextInt(2);
                int placed = substitute(level, home, count, level.getRandom());
                Umbra.LOGGER.debug("Placed {} mimics around {}'s home", placed, player.getName().getString());
            }
        }
    }

    private static final int WILD_INTERVAL = 20 * 60;

    /**
     * Out in the world: underground an ore you are about to mine may be a mimic; at night, away from home,
     * so may the barrel or the furnace of a village. Roughly one every few minutes, more often as sanity drops.
     */
    private static void wild(ServerLevel level, ServerPlayer player) {
        if (player.isCreative() || !UmbraConfig.on(UmbraConfig.MIMICS)) return;
        if (dev.umbra.light.WardBlock.protects(player)) return;
        RandomSource random = level.getRandom();
        float dread = (float) Math.sqrt(Sanity.dread(player));
        boolean underground = dev.umbra.util.UmbraUtil.skyLight(level, player.blockPosition()) == 0 && player.getY() < 64;
        if (underground) {
            if (random.nextFloat() < 0.3F * dread) substituteNear(level, player, true, random);
            return;
        }
        BlockPos home = getHome(player);
        boolean awayFromHome = home == null || home.distSqr(player.blockPosition()) > 48 * 48;
        if (awayFromHome && dev.umbra.util.UmbraUtil.isNight(level) && random.nextFloat() < 0.25F * dread) {
            substituteNear(level, player, false, random);
        }
    }

    /** One ore (or one piece of furniture) near the player, not right under their nose. */
    public static boolean substituteNear(ServerLevel level, ServerPlayer player, boolean ores, RandomSource random) {
        BlockPos c = player.blockPosition();
        for (int attempt = 0; attempt < 600; attempt++) {
            BlockPos pos = c.offset(random.nextInt(25) - 12, random.nextInt(13) - 6, random.nextInt(25) - 12);
            if (pos.distSqr(c) < 4 * 4 || !level.isLoaded(pos)) continue;
            MimicBlock m = mimicFor(level.getBlockState(pos).getBlock());
            if (m == null || m.isOre() != ores) continue;
            if (tryMimic(level, pos)) return true;
        }
        return false;
    }

    /** Replaces up to {@code count} suitable blocks around {@code center} with their mimics. */
    public static int substitute(ServerLevel level, BlockPos center, int count, RandomSource random) {
        int placed = 0;
        for (int attempt = 0; attempt < 400 && placed < count; attempt++) {
            BlockPos pos = center.offset(
                    random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS,
                    random.nextInt(SEARCH_HEIGHT * 2 + 1) - SEARCH_HEIGHT,
                    random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS);
            if (!level.isLoaded(pos)) continue;
            MimicBlock m = mimicFor(level.getBlockState(pos).getBlock());
            if (m == null || m.isOre()) continue;
            if (tryMimic(level, pos)) placed++;
        }
        return placed;
    }

    /** Turns the block at {@code pos} into its mimic if there is one. */
    public static boolean tryMimic(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        MimicBlock mimic = mimicFor(state.getBlock());
        if (mimic == null) return false;
        // only blocks that the player could actually see and touch
        if (!level.getBlockState(pos.above()).isAir() && !level.getBlockState(pos.north()).isAir()
                && !level.getBlockState(pos.south()).isAir() && !level.getBlockState(pos.east()).isAir()
                && !level.getBlockState(pos.west()).isAir()) {
            return false;
        }
        level.setBlock(pos, mimic.defaultBlockState(), Block.UPDATE_ALL);
        return true;
    }

    private static MimicBlock mimicFor(Block block) {
        for (DeferredBlock<MimicBlock> m : ModBlocks.MIMICS.values()) {
            if (m.get().original() == block) return m.get();
        }
        return null;
    }
}
