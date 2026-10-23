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
    private static final int W = 380, H = 214, LIST_W = 112, ROWS = 9, ROW_H = 17;
    private static int tab, listPage;
    private static String open;

    private int left, top, textPage, textPages = 1;
    private final List<String> got;
    private final boolean slipping;
    private final Set<String> scrambled = new HashSet<>();
    private final String whisper;
    private final net.minecraft.world.entity.player.Player me;

    public JournalScreen() {
        super(Component.translatable("item.umbra.journal"));
        var player = net.minecraft.client.Minecraft.getInstance().player;
        this.me = player;
        this.got = player == null ? List.of() : player.getData(ModAttachments.JOURNAL);
        float sanity = player == null ? 100.0F : player.getData(ModAttachments.SANITY);
        this.slipping = sanity < Journal.CORRUPT_BELOW;
        RandomSource r = RandomSource.create();
        if (slipping) {
            for (String page : Journal.PAGE_ORDER) if (Journal.known(got, page) && !page.startsWith("insight") && r.nextFloat() < 0.35F) scrambled.add(page);
        }
        this.whisper = slipping ? "journal.umbra.whisper." + r.nextInt(Journal.WHISPERS) : null;
        if (open != null && (!Journal.PAGE_ORDER.contains(open) || !Journal.known(got, open))) open = null;
    }

    private List<String> entries() {
        List<String> out = new ArrayList<>();
        for (String page : Journal.PAGE_ORDER) if (Journal.section(page) == tab) out.add(page);
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
        if (tab == Journal.ATLAS) return;   // the atlas is drawn, not listed
        // entry list
        List<String> list = entries();
        int pages = Math.max(1, (list.size() + ROWS - 1) / ROWS);
        if (listPage >= pages) listPage = pages - 1;
        for (int i = 0; i < ROWS; i++) {
            int idx = listPage * ROWS + i;
            if (idx >= list.size()) break;
            String id = list.get(idx);
            boolean known = Journal.known(got, id);
            MutableComponent label = known ? Journal.pageTitle(id).copy()
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

    /** The open page: creatures open up with what you know, errands show how far along you are (see {@link Journal#pageText}). */
    private MutableComponent fullText() {
        return Journal.pageText(me, got, open);
    }

    private int textWidth() {
        return W - LIST_W - 30;
    }

    private int linesPerPage() {
        return (H - 66) / (this.font.lineHeight + 1);
    }

    private List<FormattedCharSequence> pageLines() {
        if (open == null) return List.of();
        MutableComponent text = fullText();
        text = scrambled.contains(open) ? text.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.OBFUSCATED) : text.withStyle(s -> s.withColor(0x2A1E14));
        return this.font.split(text, textWidth());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        // leather cover, two pages
        g.fill(left - 4, top - 4, left + W + 4, top + H + 4, 0xFF24160E);
        g.outline(left - 4, top - 4, W + 8, H + 8, 0xFF4E3220);
        if (tab == Journal.ATLAS) {
            g.fill(left, top, left + W, top + H, 0xFFD8C6A0);
            return;
        }
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
        if (tab == Journal.ATLAS) {
            atlas(g, mouseX, mouseY);
            return;
        }
        g.centeredText(this.font, Component.translatable("journal.umbra.tab." + Journal.SECTIONS[tab]).withStyle(ChatFormatting.BOLD),
                left + (LIST_W + 12) / 2, top + 7, 0xFF3A2614);
        int count = Journal.knownPages(got), all = Journal.PAGE_ORDER.size();
        int rx = left + LIST_W + 18;
        if (open == null) {
            g.text(this.font, Component.translatable("journal.umbra.header", count, all), rx, top + 8, 0xFF3A2614, false);
            g.textWithWordWrap(this.font, Component.translatable("journal.umbra.pick"), rx, top + 26, textWidth(), 0xFF5A4630, false);
            if (count < all) {
                g.textWithWordWrap(this.font, Component.translatable("journal.umbra.more").withStyle(ChatFormatting.ITALIC), rx, top + H - 40, textWidth(), 0xFF6E5A40, false);
            }
        } else {
            boolean insight = open.startsWith("insight");
            g.textWithWordWrap(this.font, Journal.pageTitle(open).copy().withStyle(ChatFormatting.BOLD), rx, top + 8, textWidth(),
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

    /** 2.6: the atlas: what you have found, round where you stand, in the world you stand in. North is up. */
    private void atlas(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x0 = left + 8, y0 = top + 20, x1 = left + W - 8, y1 = top + H - 8;
        int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
        g.centeredText(this.font, Component.translatable("journal.umbra.tab.atlas").withStyle(ChatFormatting.BOLD), left + W / 2, top + 6, 0xFF3A2614);
        g.outline(x0, y0, x1 - x0, y1 - y0, 0xFF8A7454);
        if (me == null) return;
        String here = dev.umbra.journal.Atlas.dim(me.level());
        var places = dev.umbra.journal.Atlas.places(me).stream().filter(p -> p.dim().equals(here)).toList();
        double far = 64;
        for (var p : places) far = Math.max(far, Math.max(Math.abs(p.x() - me.getX()), Math.abs(p.z() - me.getZ())) * 1.15);
        double scale = Math.min((x1 - x0) / 2.0 - 8, (y1 - y0) / 2.0 - 8) / far;
        // a faint grid every hundred blocks (or a thousand, when far)
        int step = far > 1500 ? 1000 : far > 300 ? 200 : 50;
        for (int k = -(int) (far / step) - 1; k <= far / step + 1; k++) {
            int gx = (int) Math.round(cx + (Math.floorDiv((int) me.getX(), step) * step + k * step - me.getX()) * scale);
            int gy = (int) Math.round(cy + (Math.floorDiv((int) me.getZ(), step) * step + k * step - me.getZ()) * scale);
            if (gx > x0 && gx < x1) g.fill(gx, y0 + 1, gx + 1, y1 - 1, 0x22604830);
            if (gy > y0 && gy < y1) g.fill(x0 + 1, gy, x1 - 1, gy + 1, 0x22604830);
        }
        g.text(this.font, Component.literal("N"), cx - 2, y0 + 3, 0xFF6E5A40, false);
        Component hover = null;
        for (var p : places) {
            int px = (int) Math.round(cx + (p.x() - me.getX()) * scale), py = (int) Math.round(cy + (p.z() - me.getZ()) * scale);
            px = Math.max(x0 + 4, Math.min(x1 - 4, px));
            py = Math.max(y0 + 4, Math.min(y1 - 4, py));
            int col = switch (p.kind()) {
                case "lighthouse" -> 0xFFE0B040;
                case "citadel" -> 0xFF6A2A8A;
                case "shadow_village" -> 0xFF4A4A60;
                case "maze" -> 0xFF2A7A8A;
                case "sunken_temple" -> 0xFF2A5A9A;
                case "underside" -> 0xFF101010;
                case "depths" -> 0xFF1A3A5A;
                default -> 0xFF9A1A1A;     // a boss
            };
            g.fill(px - 3, py - 3, px + 4, py + 4, 0xFF2A1E14);
            g.fill(px - 2, py - 2, px + 3, py + 3, col);
            if (Math.abs(mouseX - px) <= 4 && Math.abs(mouseY - py) <= 4) {
                int dist = (int) Math.round(Math.hypot(p.x() - me.getX(), p.z() - me.getZ()));
                hover = Component.translatable("journal.umbra.atlas.place", Component.translatable("journal.umbra.atlas." + p.kind()), dist, p.x(), p.z());
            }
        }
        // you
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFFFFFFFF);
        g.outline(cx - 3, cy - 3, 7, 7, 0xFF2A1E14);
        if (places.isEmpty()) g.centeredText(this.font, Component.translatable("journal.umbra.atlas.empty").withStyle(ChatFormatting.ITALIC), cx, y1 - 14, 0xFF6E5A40);
        if (hover != null) g.centeredText(this.font, hover, cx, y1 - 14, 0xFF3A2614);
        else g.text(this.font, Component.translatable("journal.umbra.atlas.scale", step), x0 + 4, y1 - 12, 0xFF6E5A40, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
