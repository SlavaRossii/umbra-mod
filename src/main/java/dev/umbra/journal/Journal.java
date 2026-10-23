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
            "insight_3", "altar", "watcher", "mimic_queen", "shepherd", "doppelganger", "citadel", "statue", "keeper", "architect", "primordial_shadow", "freed",
            "note_1", "note_2", "note_3", "note_4", "note_5", "note_6", "note_7", "note_8",
            "keeper_1", "keeper_2", "keeper_3", "keeper_4", "keeper_5", "keeper_6", "keeper_7",
            // 1.9: the lighthouse, Marta's map, Yan; errands follow
            "marta_map", "lighthouse", "yan_1", "yan_2", "yan_3", "yan_4",
            // 2.2: onslaughts on the lighthouse, the two endings
            "onslaught", "onslaught_won", "ending_light", "ending_dark",
            // 2.4: what the journal works out about each boss, strange nights, the trophy shelf, Yan after the dawn
            "weak_watcher", "weak_mimic_queen", "weak_shepherd", "weak_doppelganger", "weak_architect", "weak_primordial_shadow",
            "night_blood_moon", "night_fog", "night_silence", "trophy_shelf", "yan_dawn",
            // 2.6: Yan's gifts for his errands
            "yan_gift_lantern", "yan_gift_spyglass", "yan_gift_bell",
            // 2.6: who throws from the dark and who rings the bell
            "weeper", "bellringer",
            // 2.7: how Yan's story ends
            "fallen_keeper", "weak_fallen_keeper", "ending_fallen", "keeper_staff", "yan_farewell",
            // 2.7: the drowned halls, what is to be done there and who lives there
            "acolyte", "drowned_keeper", "weak_drowned_keeper", "depths_sluice", "depths_bells", "depths_lamps",
            "depths_note_1", "depths_note_2", "depths_note_3",
            // 2.9: the Keeper's chest
            "depths_treasure",
            // 2.6: the drowned halls
            "depths"), dev.umbra.quest.Quests.ENTRY_IDS.stream()).toList();

    /** Sections of the journal window, in tab order. */
    public static final String[] SECTIONS = {"chronicle", "insights", "creatures", "bosses", "notes", "quests", "atlas"};
    /** The atlas tab: a map, not a list of entries. */
    public static final int ATLAS = 6;

    /** Index into {@link #SECTIONS} for an entry (or a page: a page goes where its first entry goes). */
    public static int section(String id) {
        if (id.startsWith("insight")) return 1;
        if (id.equals("mimics") || id.startsWith("mimic") && !id.equals("mimicker") && !id.equals("mimic_queen")) return 2;
        if (id.startsWith("note_") || id.startsWith("depths_note_") || (id.startsWith("keeper_") && !id.equals("keeper_staff"))) return 4;
        if (id.startsWith("quest_")) return 5;
        if (id.startsWith("weak_")) return 3;
        return switch (id) {
            case "crawler", "blind", "light_eater", "mimicker", "shade", "lurker", "statue", "keeper", "weeper", "bellringer", "acolyte" -> 2;
            case "watcher", "mimic_queen", "shepherd", "doppelganger", "architect", "primordial_shadow", "fallen_keeper", "drowned_keeper" -> 3;
            default -> 0;
        };
    }

    // --- 2.11: pages ----------------------------------------------------------------------------------------
    /**
     * The journal reads in pages, fewer than its entries: what you know of the mimics is one page, a boss and how to
     * beat it are one page, notes found together are read together. The entries keep their own ids (and what they
     * open: recipes, errands, the next page of the Keepers), only the reading is merged. A page with an id of its own
     * ("mimics", "nights", "yan_gifts") has a title and a short line of its own; otherwise it is named after its first
     * entry. Old saves lose nothing: whatever was written shows in its page.
     */
    public static final Map<String, List<String>> PAGES = pages();

    private static Map<String, List<String>> pages() {
        Map<String, List<String>> m = new java.util.LinkedHashMap<>();
        m.put("mimics", List.of("mimic", "mimic_crafting_table", "mimic_bookshelf", "mimic_planks", "mimic_stone", "mimic_barrel", "mimic_furnace",
                "mimic_hay", "mimic_log", "mimic_ore", "mimic_chest", "mimic_villager"));
        m.put("shadow", List.of("shadow", "shadow_dispelled"));
        for (String b : new String[]{"watcher", "mimic_queen", "shepherd", "doppelganger", "architect", "fallen_keeper", "drowned_keeper"})
            m.put(b, List.of(b, "weak_" + b));
        m.put("primordial_shadow", List.of("primordial_shadow", "weak_primordial_shadow", "freed"));
        m.put("note_1", List.of("note_1", "note_2"));
        m.put("note_3", List.of("note_3", "note_4"));
        m.put("note_5", List.of("note_5", "note_6"));
        m.put("note_7", List.of("note_7", "note_8"));
        m.put("keeper_1", List.of("keeper_1", "keeper_2", "keeper_3"));
        m.put("keeper_4", List.of("keeper_4", "keeper_5"));
        m.put("keeper_6", List.of("keeper_6", "keeper_7"));
        m.put("depths_note_1", List.of("depths_note_1", "depths_note_2", "depths_note_3"));
        m.put("yan_1", List.of("yan_1", "yan_2"));
        m.put("yan_3", List.of("yan_3", "yan_4"));
        m.put("onslaught", List.of("onslaught", "onslaught_won"));
        m.put("nights", List.of("night_blood_moon", "night_fog", "night_silence"));
        m.put("yan_gifts", List.of("yan_gift_lantern", "yan_gift_spyglass", "yan_gift_bell"));
        m.put("yan_dawn", List.of("yan_dawn", "keeper_staff", "yan_farewell"));
        m.put("depths", List.of("depths", "depths_sluice", "depths_bells", "depths_lamps", "depths_treasure"));
        return java.util.Collections.unmodifiableMap(m);
    }

    private static final Map<String, String> PAGE_OF = pageIndex();

    private static Map<String, String> pageIndex() {
        Map<String, String> m = new java.util.HashMap<>();
        for (var e : PAGES.entrySet()) for (String id : e.getValue()) m.put(id, e.getKey());
        return m;
    }

    /** The page entry {@code id} is read in (its own id when it stands alone). */
    public static String pageOf(String id) {
        return PAGE_OF.getOrDefault(id, id);
    }

    /** The entries of a page, in reading order. */
    public static List<String> members(String page) {
        return PAGES.getOrDefault(page, List.of(page));
    }

    /** Every page, in reading order (where its first entry stands among {@link #ENTRIES}). */
    public static final List<String> PAGE_ORDER = ENTRIES.stream().map(Journal::pageOf).distinct().toList();

    /** Whether anything on the page has been written. */
    public static boolean known(List<String> got, String page) {
        for (String id : members(page)) if (got.contains(id)) return true;
        return false;
    }

    /** Whether the page has a title of its own (not its first entry's). */
    public static boolean ownTitle(String page) {
        return PAGES.containsKey(page) && !PAGES.get(page).contains(page);
    }

    public static Component pageTitle(String page) {
        return Component.translatable("journal.umbra." + page + ".title");
    }

    /**
     * The page as it reads to {@code me}: creatures open up with what you know of them, errands show how far along
     * you are, and every further entry of the page follows under its own heading.
     */
    public static net.minecraft.network.chat.MutableComponent pageText(net.minecraft.world.entity.player.Player me, List<String> got, String page) {
        net.minecraft.network.chat.MutableComponent t = Component.empty();
        boolean first = true;
        if (ownTitle(page)) {
            t.append(Component.translatable("journal.umbra." + page + ".text").withStyle(ChatFormatting.ITALIC));
            first = false;
        }
        for (String id : members(page)) {
            if (!got.contains(id)) continue;
            if (!first) t.append("\n\n");
            if (!id.equals(page)) t.append(Component.translatable("journal.umbra." + id + ".title").withStyle(ChatFormatting.BOLD)).append("\n");
            t.append(entryText(me, id));
            first = false;
        }
        return t;
    }

    private static net.minecraft.network.chat.MutableComponent entryText(net.minecraft.world.entity.player.Player me, String id) {
        if (me != null && Bestiary.isCreature(id)) {
            int kills = Bestiary.kills(me, id);
            int st = Bestiary.stage(id, kills);
            net.minecraft.network.chat.MutableComponent t = Component.translatable("journal.umbra." + id + ".look");
            if (st >= 1) t.append("\n\n").append(Component.translatable("journal.umbra." + id + ".text"));
            if (st >= 2) t.append("\n\n").append(Component.translatable("journal.umbra." + id + ".weak"));
            else t.append("\n\n").append(Component.translatable("journal.umbra.bestiary.hint" + st, Bestiary.toNext(id, kills)).withStyle(ChatFormatting.ITALIC));
            return t;
        }
        net.minecraft.network.chat.MutableComponent t = Component.translatable("journal.umbra." + id + ".text");
        if (me != null && id.startsWith("quest_")) {
            var q = dev.umbra.quest.Quests.byId(id.substring("quest_".length()));
            if (q != null) {
                t.append("\n\n");
                if (dev.umbra.quest.Quests.isDone(me, q)) t.append(Component.translatable("journal.umbra.quest.done").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
                else {
                    int base = Math.max(0, dev.umbra.quest.Quests.activeBaseline(me, q));
                    t.append(Component.translatable("journal.umbra.quest.progress", dev.umbra.quest.Quests.progress(me, q, base), q.count()).withStyle(ChatFormatting.DARK_AQUA));
                }
            }
        }
        return t;
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
            Map.entry("yan_1", List.of("trophy_shelf")),
            Map.entry("ash_storm", List.of("ash_clasp")),
            Map.entry("yan_gift_lantern", List.of("marta_lantern")),
            Map.entry("lurker", List.of("depths_key")),     // 2.9: the Depths in the middle of the game (the lurker's scales)
            Map.entry("yan_gift_spyglass", List.of("yan_spyglass")),
            Map.entry("yan_gift_bell", List.of("keeper_bell")),
            Map.entry("luminite", List.of("luminite_lamp", "luminite_spear", "luminite_bow", "luminite_shield")),
            Map.entry("shadow_dispelled", List.of("dusk_stone", "antipsychotic", "shadow_dagger", "shadow_knife")),
            Map.entry("underside", List.of("dead_log_to_planks", "gloom_axe", "gloom_block", "gloom_boots", "gloom_chestplate", "gloom_helmet",
                    "gloom_ingot_from_blasting", "gloom_ingot_from_block", "gloom_ingot_from_smelting", "gloom_leggings",
                    "gloom_pickaxe", "gloom_shovel", "gloom_sword", "quantum_compass",
                    "seal_of_watching", "seal_of_mimicry", "seal_of_herding", "seal_of_reflection", "summoning_altar")),
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
        boolean pageKnown = known(player.getData(ModAttachments.JOURNAL), pageOf(id));
        List<String> list = new ArrayList<>(player.getData(ModAttachments.JOURNAL));
        list.add(id);
        player.setData(ModAttachments.JOURNAL, List.copyOf(list));
        awardRecipes(player, id);
        award(player, id);
        Atlas.onUnlock(player, id);
        if (id.startsWith("insight")) {
            player.sendSystemMessage(Component.translatable("message.umbra.insight",
                    Component.translatable("journal.umbra." + id + ".title")).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
            return;
        }
        // a page that is already there grows by a line: say so, under the page's name
        player.sendOverlayMessage(Component.translatable(pageKnown ? "message.umbra.journal_more" : "message.umbra.journal_new",
                pageTitle(pageOf(id))).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
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
        int known = knownPages(got);
        player.sendSystemMessage(Component.translatable("journal.umbra.header", known, PAGE_ORDER.size()).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        // a slipping mind reads its own words badly, and finds lines it did not write
        float sanity = dev.umbra.sanity.Sanity.enabled() ? dev.umbra.sanity.Sanity.get(player) : 100.0F;
        boolean slipping = sanity < CORRUPT_BELOW;
        var random = player.getRandom();
        for (String page : PAGE_ORDER) {
            if (!known(got, page)) continue;
            boolean insight = page.startsWith("insight");
            player.sendSystemMessage(pageTitle(page).copy().withStyle(insight ? ChatFormatting.DARK_PURPLE : ChatFormatting.LIGHT_PURPLE));
            if (slipping && !insight && random.nextFloat() < 0.35F) {
                player.sendSystemMessage(pageText(player, got, page).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.OBFUSCATED));
            } else {
                player.sendSystemMessage(pageText(player, got, page).withStyle(ChatFormatting.GRAY));
            }
            if (slipping && random.nextFloat() < 0.25F) {
                player.sendSystemMessage(Component.translatable("journal.umbra.whisper." + random.nextInt(WHISPERS)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            }
        }
        if (known < PAGE_ORDER.size()) {
            player.sendSystemMessage(Component.translatable("journal.umbra.more").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    /** How many pages have anything written on them. */
    public static int knownPages(List<String> got) {
        int n = 0;
        for (String page : PAGE_ORDER) if (known(got, page)) n++;
        return n;
    }
}
