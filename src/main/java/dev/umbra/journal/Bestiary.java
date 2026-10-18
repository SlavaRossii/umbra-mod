package dev.umbra.journal;

import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What you know about a creature grows as you meet it: seen at a distance (a silhouette and a name), then fought
 * (its habits), then understood (its weakness). The kills are counted per creature id.
 */
public final class Bestiary {
    private static Map<EntityType<?>, String> ids;

    private Bestiary() {}

    /** Creature entity types and their journal ids (built on first use, after registration). */
    public static synchronized Map<EntityType<?>, String> ids() {
        if (ids == null) {
            ids = Map.ofEntries(
                    Map.entry(ModEntities.CRAWLER.get(), "crawler"),
                    Map.entry(ModEntities.BLIND.get(), "blind"),
                    Map.entry(ModEntities.WEEPER.get(), "weeper"),
                    Map.entry(ModEntities.BELLRINGER.get(), "bellringer"),
                    Map.entry(ModEntities.LIGHT_EATER.get(), "light_eater"),
                    Map.entry(ModEntities.MIMICKER.get(), "mimicker"),
                    Map.entry(ModEntities.SHADE.get(), "shade"),
                    Map.entry(ModEntities.LURKER.get(), "lurker"),
                    Map.entry(ModEntities.WATCHER.get(), "watcher"),
                    Map.entry(ModEntities.MIMIC_QUEEN.get(), "mimic_queen"),
                    Map.entry(ModEntities.SHEPHERD.get(), "shepherd"),
                    Map.entry(ModEntities.DOPPELGANGER.get(), "doppelganger"),
                    Map.entry(ModEntities.ARCHITECT.get(), "architect"),
                    Map.entry(ModEntities.CITADEL_STATUE.get(), "statue"),
                    Map.entry(ModEntities.CITADEL_KEEPER.get(), "keeper"),
                    Map.entry(ModEntities.PRIMORDIAL_SHADOW.get(), "primordial_shadow"),
                    Map.entry(ModEntities.FALLEN_KEEPER.get(), "fallen_keeper"),
                    Map.entry(ModEntities.ACOLYTE.get(), "acolyte"),
                    Map.entry(ModEntities.DROWNED_KEEPER.get(), "drowned_keeper"));
        }
        return ids;
    }

    /** Creature ids with a bestiary page (the same ids as their journal entries). */
    public static final List<String> CREATURES = List.of("crawler", "blind", "weeper", "bellringer", "light_eater", "mimicker", "shade", "lurker", "statue", "keeper",
            "watcher", "mimic_queen", "shepherd", "doppelganger", "architect", "primordial_shadow", "fallen_keeper", "acolyte", "drowned_keeper");

    public static final List<String> BOSSES = List.of("watcher", "mimic_queen", "shepherd", "doppelganger", "architect", "primordial_shadow");

    public static boolean isCreature(String id) {
        return CREATURES.contains(id);
    }

    public static int kills(Player player, String id) {
        for (String s : player.getData(ModAttachments.BESTIARY)) {
            int i = s.indexOf(':');
            if (i > 0 && s.substring(0, i).equals(id)) {
                try {
                    return Integer.parseInt(s.substring(i + 1));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    public static void addKill(Player player, String id) {
        int n = kills(player, id) + 1;
        List<String> out = new ArrayList<>();
        for (String s : player.getData(ModAttachments.BESTIARY)) if (!s.startsWith(id + ":")) out.add(s);
        out.add(id + ":" + n);
        player.setData(ModAttachments.BESTIARY, List.copyOf(out));
    }

    /** 0: seen only; 1: fought (habits); 2: understood (weakness). */
    public static int stage(String id, int kills) {
        if (BOSSES.contains(id)) return kills >= 1 ? 2 : 0;
        return kills >= 4 ? 2 : kills >= 1 ? 1 : 0;
    }

    public static int stage(Player player, String id) {
        return stage(id, kills(player, id));
    }

    /** Kills still needed for the next stage, or 0 at the top. */
    public static int toNext(String id, int kills) {
        int st = stage(id, kills);
        if (st >= 2) return 0;
        if (BOSSES.contains(id)) return 1 - kills;
        return st == 0 ? 1 - kills : 4 - kills;
    }
}
