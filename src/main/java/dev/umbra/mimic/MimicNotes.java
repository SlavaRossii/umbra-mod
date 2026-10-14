package dev.umbra.mimic;

import dev.umbra.journal.Journal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Every kind of mimic has its own tell, and the journal learns it the first time you meet that kind
 * (exposed or sprung). Later mimics of the same kind add nothing; kinds you have not met stay unwritten.
 */
public final class MimicNotes {
    public static final List<String> KINDS = List.of(
            "crafting_table", "bookshelf", "planks", "stone", "barrel", "furnace", "hay", "log", "ore", "chest", "villager");

    private MimicNotes() {}

    public static String kindOf(Block original) {
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(original).getPath();
        if (id.endsWith("_ore")) return "ore";
        if (id.endsWith("_planks")) return "planks";
        if (id.equals("cobblestone") || id.equals("stone_bricks")) return "stone";
        if (id.equals("hay_block")) return "hay";
        if (id.endsWith("_log")) return "log";
        return id;
    }

    /** Writes the general mimic entry and the note for this kind, for everyone who saw it. */
    public static void note(ServerLevel level, BlockPos pos, String kind) {
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(16))) {
            Journal.unlock(p, "mimic");
            if (KINDS.contains(kind)) Journal.unlock(p, "mimic_" + kind);
        }
    }
}
