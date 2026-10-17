package dev.umbra.journal;

import dev.umbra.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The journal writes itself: each first encounter adds an entry (lang keys journal.umbra.&lt;id&gt;.title/.text).
 * New content: call {@link #unlock} with a new id and add both lang lines. The order below is the reading order.
 */
public final class Journal {
    public static final List<String> ENTRIES = java.util.stream.Stream.concat(java.util.stream.Stream.of(
            "start", "sanity", "insight_1", "nightmare", "luminite", "shadow", "shadow_dispelled", "steps", "mimic", "mimic_crafting_table", "mimic_bookshelf", "mimic_planks", "mimic_stone", "mimic_barrel",
            "mimic_furnace", "mimic_hay", "mimic_log", "mimic_ore", "mimic_chest", "mimic_villager",
            "darkness", "maze", "underside", "shadow_village", "crystal_grottos", "black_lakes", "sunken_temple", "whispering_forest", "ash_storm",
            "insight_2", "crawler", "blind", "light_eater", "mimicker", "shade", "lurker",
            "insight_3", "watcher", "mimic_queen", "shepherd", "doppelganger", "citadel", "statue", "keeper", "architect", "primordial_shadow", "freed",
            "note_1", "note_2", "note_3", "note_4", "note_5", "note_6", "note_7", "note_8",
            "keeper_1", "keeper_2", "keeper_3", "keeper_4", "keeper_5", "keeper_6", "keeper_7",
            // 1.9: the lighthouse, Marta's map, Yan; errands follow
            "marta_map", "lighthouse", "yan_1", "yan_2", "yan_3", "yan_4",
            // 2.2: onslaughts on the lighthouse, the two endings
            "onslaught", "onslaught_won", "ending_light", "ending_dark",
            // 2.4: what the journal works out about each boss, strange nights, the trophy shelf, Yan after the dawn
            "weak_watcher", "weak_mimic_queen", "weak_shepherd", "weak_doppelganger", "weak_architect", "weak_primordial_shadow",
            "night_blood_moon", "night_fog", "night_silence", "trophy_shelf", "yan_dawn"), dev.umbra.quest.Quests.ENTRY_IDS.stream()).toList();

    /** Sections of the journal window, in tab order. */
    public static final String[] SECTIONS = {"chronicle", "insights", "mimics", "creatures", "bosses", "notes", "quests"};

    /** Index into {@link #SECTIONS} for an entry. */
    public static int section(String id) {
        if (id.startsWith("insight")) return 1;
        if (id.startsWith("mimic") && !id.equals("mimicker") && !id.equals("mimic_queen")) return 2;
        if (id.startsWith("note_") || id.startsWith("keeper_")) return 5;
        if (id.startsWith("quest_")) return 6;
        if (id.startsWith("weak_")) return 4;
        return switch (id) {
            case "crawler", "blind", "light_eater", "mimicker", "shade", "lurker", "statue", "keeper" -> 3;
            case "watcher", "mimic_queen", "shepherd", "doppelganger", "architect", "primordial_shadow" -> 4;
            default -> 0;
        };
    }

    /** Set on the client: opens the journal window. */
    public static Runnable openScreen = () -> {};

    /** Insights: thoughts that only come when the mind is slipping. Sanity thresholds, in order. */
    public static final String[] INSIGHTS = {"insight_1", "insight_2", "insight_3"};
    public static final float[] INSIGHT_AT = {40.0F, 25.0F, 10.0F};

    /** Recipes the hero "figures out" when an entry is written (they appear in the recipe book). */
    public static final Map<String, List<String>> RECIPES = Map.ofEntries(
            Map.entry("start", List.of("journal", "sedative")),
            // 1.8.2: the lamp is what you find yourself; the rest of the craft is the Keepers' (see the keeper_N pages)
            Map.entry("luminite", List.of("luminite_lamp", "luminite_spear", "luminite_bow")),
            Map.entry("shadow_dispelled", List.of("dusk_stone", "antipsychotic", "shadow_dagger", "shadow_knife")),
            Map.entry("underside", List.of("dead_log_to_planks", "gloom_axe", "gloom_block", "gloom_boots", "gloom_chestplate", "gloom_helmet",
                    "gloom_ingot_from_blasting", "gloom_ingot_from_block", "gloom_ingot_from_smelting", "gloom_leggings",
                    "gloom_pickaxe", "gloom_shovel", "gloom_sword", "quantum_compass",
                    "seal_of_watching", "seal_of_mimicry", "seal_of_herding", "seal_of_reflection")),
            Map.entry("watcher", List.of("underside_key")),
            Map.entry("mimic_queen", List.of("underside_key")),
            Map.entry("shepherd", List.of("underside_key", "shepherd_scythe")),
            Map.entry("doppelganger", List.of("underside_key")),
            Map.entry("insight_1", List.of("whisper_table")),
            // the Keepers of the Vein: notes of the miners who guarded the luminite, one craft per page
            Map.entry("keeper_1", List.of("mirror")),
            Map.entry("keeper_2", List.of("ward")),
            Map.entry("keeper_3", List.of("light_arrow", "eternal_torch")),
            Map.entry("keeper_4", List.of("dispeller", "luminite_lantern")),
            Map.entry("keeper_5", List.of("sedative_from_luminite")),
            Map.entry("lighthouse", List.of("keeper_horn")),
            // 2.3: the altar and the anvil are found with the Underside, the candles with luminite
            Map.entry("crystal_grottos", List.of("luminite_block", "luminite_crystal_from_block", "luminite_candle", "thick_luminite_candle", "melted_luminite_candle", "luminite_candelabrum", "echo_altar", "luminite_lantern_block", "echo_staff")),
            Map.entry("shadow_village", List.of("gloom_anvil")));

    /** Lines in a hand that is not yours: they creep into the journal when sanity is low. */
    public static final int WHISPERS = 8;

    public static final float CORRUPT_BELOW = 25.0F;

    private Journal() {}

    public static boolean has(ServerPlayer player, String id) {
        return player.getData(ModAttachments.JOURNAL).contains(id);
    }

    /** Adds the entry if new and tells the player. */
    public static void unlock(ServerPlayer player, String id) {
        if (!ENTRIES.contains(id) || has(player, id)) return;
        List<String> list = new ArrayList<>(player.getData(ModAttachments.JOURNAL));
        list.add(id);
        player.setData(ModAttachments.JOURNAL, List.copyOf(list));
        awardRecipes(player, id);
        award(player, id);
        if (id.startsWith("insight")) {
            player.sendSystemMessage(Component.translatable("message.umbra.insight",
                    Component.translatable("journal.umbra." + id + ".title")).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
            return;
        }
        player.sendOverlayMessage(Component.translatable("message.umbra.journal_new",
                Component.translatable("journal.umbra." + id + ".title")).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 0.9F);
    }

    /** The advancement of an entry (generated by tools/gen_advancements.py): the screen of achievements mirrors the journal. */
    public static boolean award(ServerPlayer player, String id) {
        var server = player.level().getServer();
        if (server == null) return false;
        var holder = server.getAdvancements().get(net.minecraft.resources.Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, "e_" + id));
        if (holder == null) return false;
        player.getAdvancements().award(holder, "entry");
        return true;
    }

    /** Puts the recipes tied to entry {@code id} into the player's recipe book. */
    public static void awardRecipes(ServerPlayer player, String id) {
        List<String> recipes = RECIPES.get(id);
        if (recipes == null || player.level().getServer() == null) return;
        var manager = player.level().getServer().getRecipeManager();
        List<net.minecraft.world.item.crafting.RecipeHolder<?>> holders = new ArrayList<>();
        for (String r : recipes) {
            manager.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, r))).ifPresent(holders::add);
        }
        if (!holders.isEmpty()) player.awardRecipes(holders);
    }

    /** For testers: every entry at once, quietly (no toast and sound per entry), with the recipes they open. */
    public static int unlockAll(ServerPlayer player) {
        player.setData(ModAttachments.JOURNAL, List.copyOf(ENTRIES));
        awardAll(player);
        for (String id : ENTRIES) award(player, id);
        return ENTRIES.size();
    }

    /** For testers: a blank journal (recipes stay in the book). */
    public static void clear(ServerPlayer player) {
        player.setData(ModAttachments.JOURNAL, List.of());
    }

    /** On login: recipes for everything already written (older saves, lost recipe books). */
    public static void awardAll(ServerPlayer player) {
        for (String id : player.getData(ModAttachments.JOURNAL)) awardRecipes(player, id);
    }

    /** Prints the journal to chat. */
    public static void read(ServerPlayer player) {
        List<String> got = player.getData(ModAttachments.JOURNAL);
        player.sendSystemMessage(Component.translatable("journal.umbra.header", got.size(), ENTRIES.size()).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        // a slipping mind reads its own words badly, and finds lines it did not write
        float sanity = dev.umbra.sanity.Sanity.enabled() ? dev.umbra.sanity.Sanity.get(player) : 100.0F;
        boolean slipping = sanity < CORRUPT_BELOW;
        var random = player.getRandom();
        for (String id : ENTRIES) {
            if (!got.contains(id)) continue;
            boolean insight = id.startsWith("insight");
            player.sendSystemMessage(Component.translatable("journal.umbra." + id + ".title").withStyle(insight ? ChatFormatting.DARK_PURPLE : ChatFormatting.LIGHT_PURPLE));
            if (slipping && !insight && random.nextFloat() < 0.35F) {
                player.sendSystemMessage(Component.translatable("journal.umbra." + id + ".text").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.OBFUSCATED));
            } else {
                player.sendSystemMessage(Component.translatable("journal.umbra." + id + ".text").withStyle(ChatFormatting.GRAY));
            }
            if (slipping && random.nextFloat() < 0.25F) {
                player.sendSystemMessage(Component.translatable("journal.umbra.whisper." + random.nextInt(WHISPERS)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            }
        }
        if (got.size() < ENTRIES.size()) {
            player.sendSystemMessage(Component.translatable("journal.umbra.more").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
