package dev.umbra.client;

import dev.umbra.journal.Journal;
import dev.umbra.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The journal as a book: section tabs on top, the entries of the section on the left, the open page on the right.
 * Entries not yet written show as "???". A slipping mind (sanity below {@link Journal#CORRUPT_BELOW}) reads some
 * pages scrambled and finds lines it never wrote.
 */
public class JournalScreen extends Screen {
    private static final int W = 340, H = 214, LIST_W = 112, ROWS = 9, ROW_H = 17;
    private static int tab, listPage;
    private static String open;

    private int left, top, textPage, textPages = 1;
    private final List<String> got;
    private final boolean slipping;
    private final Set<String> scrambled = new HashSet<>();
    private final String whisper;

    public JournalScreen() {
        super(Component.translatable("item.umbra.journal"));
        var player = net.minecraft.client.Minecraft.getInstance().player;
        this.got = player == null ? List.of() : player.getData(ModAttachments.JOURNAL);
        float sanity = player == null ? 100.0F : player.getData(ModAttachments.SANITY);
        this.slipping = sanity < Journal.CORRUPT_BELOW;
        RandomSource r = RandomSource.create();
        if (slipping) {
            for (String id : got) if (!id.startsWith("insight") && r.nextFloat() < 0.35F) scrambled.add(id);
        }
        this.whisper = slipping ? "journal.umbra.whisper." + r.nextInt(Journal.WHISPERS) : null;
        if (open != null && !got.contains(open)) open = null;
    }

    private List<String> entries() {
        List<String> out = new ArrayList<>();
        for (String id : Journal.ENTRIES) if (Journal.section(id) == tab) out.add(id);
        return out;
    }

    @Override
    protected void init() {
        left = (this.width - W) / 2;
        top = Math.max(4, (this.height - H) / 2);
        // section tabs
        int tw = (W - 8) / Journal.SECTIONS.length;
        for (int i = 0; i < Journal.SECTIONS.length; i++) {
            final int t = i;
            MutableComponent label = Component.translatable("journal.umbra.tab." + Journal.SECTIONS[i]);
            if (i == tab) label = label.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.UNDERLINE);
            Button b = Button.builder(label, btn -> {
                tab = t;
                listPage = 0;
                open = null;
                textPage = 0;
                rebuildWidgets();
            }).bounds(left + 4 + i * tw, top - 22, tw - 2, 20).build();
            addRenderableWidget(b);
        }
        // entry list
        List<String> list = entries();
        int pages = Math.max(1, (list.size() + ROWS - 1) / ROWS);
        if (listPage >= pages) listPage = pages - 1;
        for (int i = 0; i < ROWS; i++) {
            int idx = listPage * ROWS + i;
            if (idx >= list.size()) break;
            String id = list.get(idx);
            boolean known = got.contains(id);
            MutableComponent label = known ? Component.translatable("journal.umbra." + id + ".title")
                    : Component.translatable("journal.umbra.locked").withStyle(ChatFormatting.DARK_GRAY);
            if (id.equals(open)) label = label.withStyle(ChatFormatting.LIGHT_PURPLE);
            Button b = Button.builder(label, btn -> {
                open = id;
                textPage = 0;
                rebuildWidgets();
            }).bounds(left + 6, top + 22 + i * ROW_H, LIST_W, ROW_H - 1).build();
            b.active = known;
            addRenderableWidget(b);
        }
        if (pages > 1) {
            Button prev = Button.builder(Component.literal("<"), btn -> { listPage--; rebuildWidgets(); }).bounds(left + 6, top + H - 24, 20, 18).build();
            Button next = Button.builder(Component.literal(">"), btn -> { listPage++; rebuildWidgets(); }).bounds(left + 6 + LIST_W - 20, top + H - 24, 20, 18).build();
            prev.active = listPage > 0;
            next.active = listPage < pages - 1;
            addRenderableWidget(prev);
            addRenderableWidget(next);
        }
        // page turning of a long entry
        if (open != null) {
            textPages = Math.max(1, (pageLines().size() + linesPerPage() - 1) / linesPerPage());
            if (textPages > 1) {
                int rx = left + LIST_W + 16;
                Button prev = Button.builder(Component.literal("<"), btn -> { textPage--; rebuildWidgets(); }).bounds(rx, top + H - 24, 20, 18).build();
                Button next = Button.builder(Component.literal(">"), btn -> { textPage++; rebuildWidgets(); }).bounds(left + W - 26, top + H - 24, 20, 18).build();
                prev.active = textPage > 0;
                next.active = textPage < textPages - 1;
                addRenderableWidget(prev);
                addRenderableWidget(next);
            }
        }
    }

    private int textWidth() {
        return W - LIST_W - 30;
    }

    private int linesPerPage() {
        return (H - 66) / (this.font.lineHeight + 1);
    }

    private List<FormattedCharSequence> pageLines() {
        if (open == null) return List.of();
        MutableComponent text = Component.translatable("journal.umbra." + open + ".text");
        text = scrambled.contains(open) ? text.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.OBFUSCATED) : text.withStyle(s -> s.withColor(0x2A1E14));
        return this.font.split(text, textWidth());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        // leather cover, two pages
        g.fill(left - 4, top - 4, left + W + 4, top + H + 4, 0xFF24160E);
        g.outline(left - 4, top - 4, W + 8, H + 8, 0xFF4E3220);
        g.fill(left, top, left + LIST_W + 12, top + H, 0xFFCDBB97);
        g.fill(left + LIST_W + 12, top, left + W, top + H, 0xFFE2D2AE);
        g.fill(left + LIST_W + 11, top, left + LIST_W + 13, top + H, 0xFF8A7454);
        if (slipping) { // stains creep over the pages
            RandomSource r = RandomSource.create(got.size() * 31L + 7);
            for (int i = 0; i < 14; i++) {
                int x = left + r.nextInt(W - 12), y = top + r.nextInt(H - 12), s = 3 + r.nextInt(9);
                g.fill(x, y, x + s, y + s / 2 + 2, 0x55401010);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(this.font, Component.translatable("journal.umbra.tab." + Journal.SECTIONS[tab]).withStyle(ChatFormatting.BOLD),
                left + (LIST_W + 12) / 2, top + 7, 0xFF3A2614);
        int count = 0;
        for (String id : Journal.ENTRIES) if (got.contains(id)) count++;
        int rx = left + LIST_W + 18;
        if (open == null) {
            g.text(this.font, Component.translatable("journal.umbra.header", count, Journal.ENTRIES.size()), rx, top + 8, 0xFF3A2614, false);
            g.textWithWordWrap(this.font, Component.translatable("journal.umbra.pick"), rx, top + 26, textWidth(), 0xFF5A4630, false);
            if (count < Journal.ENTRIES.size()) {
                g.textWithWordWrap(this.font, Component.translatable("journal.umbra.more").withStyle(ChatFormatting.ITALIC), rx, top + H - 40, textWidth(), 0xFF6E5A40, false);
            }
        } else {
            boolean insight = open.startsWith("insight");
            g.textWithWordWrap(this.font, Component.translatable("journal.umbra." + open + ".title").withStyle(ChatFormatting.BOLD), rx, top + 8, textWidth(),
                    insight ? 0xFF5A1E78 : 0xFF3A2614, false);
            List<FormattedCharSequence> lines = pageLines();
            int per = linesPerPage(), from = textPage * per;
            int y = top + 30;
            for (int i = from; i < Math.min(lines.size(), from + per); i++) {
                g.text(this.font, lines.get(i), rx, y, 0xFF2A1E14, false);
                y += this.font.lineHeight + 1;
            }
            if (textPages > 1) g.centeredText(this.font, (textPage + 1) + " / " + textPages, left + LIST_W + 12 + (W - LIST_W - 12) / 2, top + H - 19, 0xFF6E5A40);
        }
        if (whisper != null) {
            g.centeredText(this.font, Component.translatable(whisper).withStyle(ChatFormatting.ITALIC), this.width / 2, top + H + 8, 0xFF8A1010);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
