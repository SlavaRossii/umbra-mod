package dev.umbra.light;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Implemented by every creature of the dark that reacts to the Dispeller's flash (and, later, other light weapons).
 * Future mobs and bosses: implement this to take light damage, get stunned, flee, etc.
 */
public interface LightSensitive {
    void onLit(ServerLevel level, Player source);
}
