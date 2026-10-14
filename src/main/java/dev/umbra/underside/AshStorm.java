package dev.umbra.underside;

import dev.umbra.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Ash storms over the ash plains. When they blow is decided by game time alone, so server and client agree
 * without any syncing: the day is cut into 5-minute windows and about one in four has a storm that rises,
 * rages and settles again.
 */
public final class AshStorm {
    public static final int WINDOW = 6000;
    private static final int START = 600, END = 5400, RAMP = 400;

    private AshStorm() {}

    /** 0..1: how hard the storm blows at this time (anywhere in the ash plains). */
    public static float strength(long gameTime) {
        long w = Math.floorDiv(gameTime, WINDOW);
        long h = (w + 0x632BE59BD9B4E019L) * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        if (Math.floorMod(h, 4) != 0) return 0.0F;
        int in = (int) Math.floorMod(gameTime, WINDOW);
        if (in < START || in > END) return 0.0F;
        return Math.min(1.0F, Math.min(in - START, END - in) / (float) RAMP);
    }

    /** Storm strength where the player stands (0 outside the ash plains). Forced storms (command) count fully. */
    public static float at(Player player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        if (!UndersideBiomes.in(level, pos, UndersideBiomes.ASH_PLAINS)) return 0.0F;
        float s = player.getData(ModAttachments.STORM_TEST) ? 1.0F : strength(level.getGameTime());
        // the ash clasp: the storm is only a grey haze
        return dev.umbra.gear.Gear.hasArtifact(player, dev.umbra.gear.Gear.ASH_CLASP) ? s * 0.25F : s;
    }
}
