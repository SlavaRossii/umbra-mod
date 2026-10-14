package dev.umbra.quest;

import dev.umbra.journal.Bestiary;
import dev.umbra.journal.Journal;
import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Errands from the shadow folk. Each villager always has the same one (chosen by its UUID); once accepted it is written
 * into the journal's "Errands" tab. Bring what is asked, kill what is asked, or just get somewhere, then speak to the
 * same villager again for the reward.
 */
public final class Quests {
    public enum Kind { ITEM, KILL, ENTRY }

    public record Quest(String id, Kind kind, String target, Supplier<Item> item, int count, Supplier<Item> reward, int rewardCount) {
        public String entry() {
            return "quest_" + id;
        }
    }

    public static final List<Quest> ALL = List.of(
            new Quest("crystals", Kind.ITEM, "", () -> ModItems.LUMINITE_CRYSTAL.get(), 6, () -> ModItems.SHADOW_SHARD.get(), 3),
            new Quest("shades", Kind.KILL, "shade", null, 8, () -> ModItems.LUMINITE_CRYSTAL.get(), 3),
            new Quest("crawlers", Kind.KILL, "crawler", null, 4, () -> ModItems.ECHO_QUARTZ.get(), 2),
            new Quest("blinds", Kind.KILL, "blind", null, 3, () -> ModItems.SEDATIVE.get(), 2),
            new Quest("eaters", Kind.KILL, "light_eater", null, 3, () -> ModItems.LUMINITE_CRYSTAL.get(), 4),
            new Quest("mimickers", Kind.KILL, "mimicker", null, 3, () -> ModItems.SHADOW_SHARD.get(), 3),
            new Quest("quartz", Kind.ITEM, "", () -> ModItems.ECHO_QUARTZ.get(), 4, () -> ModItems.LUMINITE_CRYSTAL.get(), 3),
            new Quest("page", Kind.ITEM, "", () -> ModItems.KEEPER_PAGE.get(), 1, () -> ModItems.LUMINITE_CRYSTAL.get(), 5),
            new Quest("temple", Kind.ENTRY, "sunken_temple", null, 1, () -> ModItems.ECHO_QUARTZ.get(), 3),
            new Quest("lurker", Kind.KILL, "lurker", null, 1, () -> ModItems.SHADOW_SHARD.get(), 4),
            new Quest("citadel", Kind.ENTRY, "citadel", null, 1, () -> ModItems.ANTIPSYCHOTIC.get(), 1),
            new Quest("lighthouse", Kind.ENTRY, "lighthouse", null, 1, () -> ModItems.LUMINITE_CRYSTAL.get(), 6));

    /** Journal entry ids of all errands (appended to {@link Journal#ENTRIES}). */
    public static final List<String> ENTRY_IDS = ALL.stream().map(Quest::entry).toList();

    private Quests() {}

    public static Quest byId(String id) {
        for (Quest q : ALL) if (q.id().equals(id)) return q;
        return null;
    }

    public static Quest forVillager(UUID uuid) {
        return ALL.get(Math.floorMod((int) (uuid.getMostSignificantBits() >>> 8), ALL.size()));
    }

    public static boolean isDone(Player player, Quest q) {
        return player.getData(ModAttachments.QUESTS_DONE).contains(q.id());
    }

    /** The baseline of an active errand (kills when it was taken), or -1 if it is not active. */
    public static int activeBaseline(Player player, Quest q) {
        for (String s : player.getData(ModAttachments.QUESTS_ACTIVE)) {
            int i = s.indexOf('@');
            if (i > 0 && s.substring(0, i).equals(q.id())) {
                try {
                    return Integer.parseInt(s.substring(i + 1));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return -1;
    }

    public static int have(Player player, Item item) {
        return player.getInventory().countItem(item);
    }

    /** How far along the player is (0..count). */
    public static int progress(Player player, Quest q, int baseline) {
        return switch (q.kind()) {
            case ITEM -> Math.min(q.count(), have(player, q.item().get()));
            case KILL -> Math.max(0, Math.min(q.count(), Bestiary.kills(player, q.target()) - baseline));
            case ENTRY -> player.getData(ModAttachments.JOURNAL).contains(q.target()) ? 1 : 0;
        };
    }

    /**
     * A shadow villager was spoken to. Returns SUCCESS if the conversation was about an errand (offer, reward,
     * reminder), or null if the villager should simply trade.
     */
    public static InteractionResult talk(ServerPlayer player, UUID villager) {
        Quest q = forVillager(villager);
        if (isDone(player, q)) return null;
        int base = activeBaseline(player, q);
        if (base < 0) {
            accept(player, q);
            return InteractionResult.SUCCESS;
        }
        if (progress(player, q, base) >= q.count()) {
            finish(player, q);
            return InteractionResult.SUCCESS;
        }
        player.sendOverlayMessage(Component.translatable("message.umbra.quest_progress",
                Component.translatable("journal.umbra." + q.entry() + ".title"), progress(player, q, base), q.count()));
        return null;
    }

    public static void accept(ServerPlayer player, Quest q) {
        List<String> list = new ArrayList<>(player.getData(ModAttachments.QUESTS_ACTIVE));
        int base = q.kind() == Kind.KILL ? Bestiary.kills(player, q.target()) : 0;
        list.add(q.id() + "@" + base);
        player.setData(ModAttachments.QUESTS_ACTIVE, List.copyOf(list));
        Journal.unlock(player, q.entry());
        player.sendSystemMessage(Component.translatable("message.umbra.quest_offer", Component.translatable("journal.umbra." + q.entry() + ".title"))
                .withStyle(net.minecraft.ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("quest.umbra." + q.id() + ".ask").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
    }

    public static void finish(ServerPlayer player, Quest q) {
        if (q.kind() == Kind.ITEM) {
            int left = q.count();
            var inv = player.getInventory();
            for (int slot = 0; slot < inv.getContainerSize() && left > 0; slot++) {
                ItemStack s = inv.getItem(slot);
                if (s.is(q.item().get())) {
                    int take = Math.min(left, s.getCount());
                    s.shrink(take);
                    left -= take;
                }
            }
        }
        List<String> active = new ArrayList<>();
        for (String s : player.getData(ModAttachments.QUESTS_ACTIVE)) if (!s.startsWith(q.id() + "@")) active.add(s);
        player.setData(ModAttachments.QUESTS_ACTIVE, List.copyOf(active));
        List<String> done = new ArrayList<>(player.getData(ModAttachments.QUESTS_DONE));
        done.add(q.id());
        player.setData(ModAttachments.QUESTS_DONE, List.copyOf(done));
        ItemStack reward = new ItemStack(q.reward().get(), q.rewardCount());
        if (!player.getInventory().add(reward)) {
            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), reward));
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.sendSystemMessage(Component.translatable("message.umbra.quest_done", Component.translatable("journal.umbra." + q.entry() + ".title"))
                .withStyle(net.minecraft.ChatFormatting.GREEN));
        player.sendSystemMessage(Component.translatable("quest.umbra." + q.id() + ".thanks").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
    }

    /** For tests: take, finish and see everything at once. */
    public static boolean isActive(Player player, Quest q) {
        return activeBaseline(player, q) >= 0;
    }
}
