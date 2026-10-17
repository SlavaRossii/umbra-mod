package dev.umbra.journal;

import dev.umbra.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 2.6: the atlas page of the journal. Places are marked when they are found (the journal entry that tells of them, a
 * village raised near you, the lighthouse); the page draws them round you, in the world you stand in.
 */
public final class Atlas {
    private Atlas() {}

    /** Journal entries that mark a place where the player stands when they are written. */
    public static final Set<String> KINDS = Set.of("lighthouse", "citadel", "shadow_village", "sunken_temple", "maze", "underside",
            "watcher", "mimic_queen", "shepherd", "doppelganger", "architect", "primordial_shadow", "depths");

    /** A place: kind, dimension ("o" the overworld, "u" the Underside, "d" the Depths), x, z. */
    public record Place(String kind, String dim, int x, int z) {
        static Place parse(String s) {
            String[] p = s.split("\\|");
            if (p.length != 4) return null;
            try {
                return new Place(p[0], p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    public static String dim(net.minecraft.world.level.Level level) {
        if (dev.umbra.underside.Underside.is(level)) return "u";
        String path = level.dimension().identifier().getPath();
        return path.contains("depths") ? "d" : "o";
    }

    public static List<Place> places(Player player) {
        List<Place> out = new ArrayList<>();
        for (String s : player.getData(ModAttachments.PLACES)) {
            Place p = Place.parse(s);
            if (p != null) out.add(p);
        }
        return out;
    }

    /** Marks a place, unless one of the same kind is already marked within 48 blocks. */
    public static boolean mark(ServerPlayer player, String kind, BlockPos at) {
        String d = dim(player.level());
        for (Place p : places(player)) {
            if (p.kind().equals(kind) && p.dim().equals(d) && Math.abs(p.x() - at.getX()) < 48 && Math.abs(p.z() - at.getZ()) < 48) return false;
        }
        List<String> list = new ArrayList<>(player.getData(ModAttachments.PLACES));
        list.add(kind + "|" + d + "|" + at.getX() + "|" + at.getZ());
        player.setData(ModAttachments.PLACES, List.copyOf(list));
        return true;
    }

    /** A journal entry was written: mark where (the lighthouse at its own place). */
    public static void onUnlock(ServerPlayer player, String id) {
        if (!KINDS.contains(id)) return;
        BlockPos at = player.blockPosition();
        if (id.equals("lighthouse") && player.level() instanceof ServerLevel level) at = dev.umbra.lighthouse.LighthouseManager.centre(level);
        mark(player, id, at);
    }
}
