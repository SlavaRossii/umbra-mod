package dev.umbra.item;

import dev.umbra.quantum.MazeController;
import dev.umbra.quantum.QuantumMaze;
import dev.umbra.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Inside a quantum maze it tells you which way the maze is changing. */
public class QuantumCompassItem extends Item {
    public QuantumCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        BlockPos core = findCore(server, player.blockPosition());
        if (core == null) {
            if (dev.umbra.underside.Underside.is(server) && !dev.umbra.citadel.Citadel.inPocket(player)) {
                player.sendOverlayMessage(dev.umbra.citadel.CitadelGateFeature.describeNearest(server, player, "message.umbra.compass_citadel"));
            } else if (server.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                // outside a maze the needle leans towards the nearest one
                BlockPos m = dev.umbra.quantum.QuantumMazeFeature.nearest(server.getSeed(), player.getBlockX(), player.getBlockZ());
                double dx = m.getX() - player.getX(), dz = m.getZ() - player.getZ();
                String[] dirs = {"s", "sw", "w", "nw", "n", "ne", "e", "se"};
                String dir = dirs[(int) Math.floorMod(Math.round(Math.toDegrees(Math.atan2(-dx, dz)) / 45.0), 8)];
                player.sendOverlayMessage(Component.translatable("message.umbra.compass_maze", (int) Math.round(Math.sqrt(dx * dx + dz * dz)),
                        Component.translatable("direction.umbra." + dir)));
            } else {
                player.sendOverlayMessage(Component.translatable("message.umbra.compass_none"));
            }
        } else {
            String pattern = MazeController.patternId(server, core);
            player.sendOverlayMessage(pattern == null
                    ? Component.translatable("message.umbra.compass_idle")
                    : Component.translatable("message.umbra.compass", Component.translatable("maze.umbra.pattern." + pattern),
                    MazeController.secondsLeft(server, core)));
        }
        player.getCooldowns().addCooldown(player.getItemInHand(hand), 20);
        return InteractionResult.SUCCESS;
    }

    public static BlockPos findCore(ServerLevel level, BlockPos from) {
        int r = QuantumMaze.SIZE;
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++)
                for (int dy = -QuantumMaze.HEIGHT - 2; dy <= 2; dy++) {
                    BlockPos p = from.offset(dx, dy, dz);
                    if (level.getBlockState(p).is(ModBlocks.MAZE_CORE.get())) return p;
                }
        return null;
    }
}
