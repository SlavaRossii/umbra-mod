package dev.umbra.journal;

import dev.umbra.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;

/**
 * The journal writes itself: each first encounter adds an entry (lang keys journal.umbra.&lt;id&gt;.title/.text).
 * New content: call {@link #unlock} with a new id and add both lang lines. The order below is the reading order.
 */
public final class Journal {
    public static final List<String> ENTRIES = List.of(
            "start", "shadow", "shadow_dispelled", "steps", "mimic", "darkness", "maze", "sanity", "luminite",
            "underside", "crawler", "blind", "light_eater", "mimicker", "shade",
            "watcher", "mimic_queen", "shepherd", "doppelganger", "architect", "primordial_shadow", "freed");

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
        player.sendOverlayMessage(Component.translatable("message.umbra.journal_new",
                Component.translatable("journal.umbra." + id + ".title")).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 0.9F);
    }

    /** Prints the journal to chat. */
    public static void read(ServerPlayer player) {
        List<String> got = player.getData(ModAttachments.JOURNAL);
        player.sendSystemMessage(Component.translatable("journal.umbra.header", got.size(), ENTRIES.size()).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        for (String id : ENTRIES) {
            if (!got.contains(id)) continue;
            player.sendSystemMessage(Component.translatable("journal.umbra." + id + ".title").withStyle(ChatFormatting.LIGHT_PURPLE));
            player.sendSystemMessage(Component.translatable("journal.umbra." + id + ".text").withStyle(ChatFormatting.GRAY));
        }
        if (got.size() < ENTRIES.size()) {
            player.sendSystemMessage(Component.translatable("journal.umbra.more").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
