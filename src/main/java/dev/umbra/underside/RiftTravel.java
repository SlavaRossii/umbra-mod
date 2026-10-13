package dev.umbra.underside;

import dev.umbra.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Where a rift leads: the same x/z on the other side. Reuses a rift nearby, otherwise builds one. */
public final class RiftTravel {
    private static final int SEARCH_RADIUS = 12;

    private RiftTravel() {}

    public static TeleportTransition destination(ServerLevel from, Entity entity, BlockPos pos) {
        ServerLevel to = from.getServer().getLevel(Underside.is(from) ? Level.OVERWORLD : Underside.LEVEL);
        if (to == null) return null;
        BlockPos target = new BlockPos(pos.getX(), 0, pos.getZ());
        BlockPos arrival = findRift(to, target);
        if (arrival == null) arrival = makeRift(to, target);
        // step out in front of the rift so we don't bounce straight back
        Vec3 at = Vec3.atBottomCenterOf(arrival.relative(Direction.SOUTH, 2));
        return new TeleportTransition(to, at, Vec3.ZERO, 180.0F, 0.0F, TeleportTransition.PLAY_PORTAL_SOUND);
    }

    /** Finds the bottom block of an existing rift near {@code target}. */
    private static BlockPos findRift(ServerLevel level, BlockPos target) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        int minY = level.getMinY(), maxY = level.getMinY() + level.getHeight() - 1;
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos p = new BlockPos(target.getX() + dx, y, target.getZ() + dz);
                    if (level.getBlockState(p).is(ModBlocks.RIFT.get()) && !level.getBlockState(p.below()).is(ModBlocks.RIFT.get())) {
                        double d = dx * dx + dz * dz;
                        if (d < bestD) {
                            bestD = d;
                            best = p;
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Builds a rift (with a little platform) at a sensible spot and returns its bottom-left interior block. */
    private static BlockPos makeRift(ServerLevel level, BlockPos target) {
        BlockPos base;
        if (Underside.is(level)) {
            base = findCaveFloor(level, target);
            if (base == null) base = new BlockPos(target.getX(), Underside.FALLBACK_Y, target.getZ());
        } else {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
            base = new BlockPos(target.getX(), y, target.getZ());
        }
        // clear a small room and lay a floor so arrival is always safe
        Block floor = Underside.is(level) ? ModBlocks.ASH_STONE.get() : Blocks.COBBLED_DEEPSLATE;
        for (int dx = -2; dx <= 3; dx++) {
            for (int dz = -1; dz <= 3; dz++) {
                level.setBlock(base.offset(dx, -1, dz), floor.defaultBlockState(), Block.UPDATE_ALL);
                for (int dy = 0; dy <= 4; dy++) level.setBlock(base.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        RiftFrame.build(level, base);
        return base;
    }

    /** A cave floor in the Underside with headroom, as close to y=64 as possible. */
    private static BlockPos findCaveFloor(ServerLevel level, BlockPos target) {
        for (int d = 0; d < 48; d++) {
            for (int sign : new int[]{1, -1}) {
                int y = Underside.FALLBACK_Y + d * sign;
                if (y < 8 || y > 118) continue;
                BlockPos p = new BlockPos(target.getX(), y, target.getZ());
                if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)
                        && level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                        && level.getBlockState(p.above(2)).isAir() && level.getBlockState(p.above(3)).isAir()) {
                    return p;
                }
            }
        }
        return null;
    }
}
