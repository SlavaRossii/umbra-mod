package dev.umbra.client;

import dev.umbra.registry.ModAttachments;
import dev.umbra.sanity.Sanity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/** The sanity bar, drawn above the hunger bar. Purple when calm, it darkens and flickers as the mind frays. */
public final class SanityHud {
    private static final int WIDTH = 81;
    private static final int HEIGHT = 4;

    private SanityHud() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isCreative() || player.isSpectator()) return;
        if (!Sanity.enabled()) return;
        float sanity = player.getData(ModAttachments.SANITY);

        var hud = mc.gui.hud;
        int right = g.guiWidth() / 2 + 91;
        int bottom = g.guiHeight() - hud.rightHeight + 9;
        int top = bottom - HEIGHT;
        int left = right - WIDTH;

        int filled = Math.round(WIDTH * sanity / Sanity.MAX);
        int color;
        if (sanity >= Sanity.ANXIOUS) color = 0xFFB08CFF;
        else if (sanity >= Sanity.PARANOID) color = 0xFF8A5CD6;
        else if (sanity >= Sanity.BREAKDOWN) color = 0xFF6A2FA0;
        else color = (player.tickCount / 4) % 2 == 0 ? 0xFF4A145E : 0xFF8E1E3A; // flickering

        // a shaking bar when the mind is breaking
        if (sanity < 15.0F) {
            int phase = player.tickCount;
            int dx = (phase / 2) % 5 == 0 ? (phase % 2 == 0 ? 1 : -1) : 0;
            int dy = (phase / 3) % 7 == 0 ? 1 : 0;
            left += dx;
            right += dx;
            top += dy;
            bottom += dy;
        }
        g.fill(left - 1, top - 1, right + 1, bottom + 1, 0xC0000000);
        if (filled > 0) g.fill(right - filled, top, right, bottom, color);
        // cracks below 30: dark fissures across the bar, more as it falls
        if (sanity < 30.0F) {
            int[] cracks = {7, 19, 26, 38, 47, 58, 66, 74};
            int count = sanity < 15.0F ? cracks.length : 4;
            for (int i = 0; i < count; i++) {
                int x = right - cracks[i];
                g.fill(x, top - 1, x + 1, bottom + 1, 0xFF0A0010);
                if (i % 2 == 0) g.fill(x + 1, top + 1, x + 2, bottom, 0xFF0A0010);
            }
        }
        // a small eye at the left end that opens as sanity falls
        if (sanity < 45.0F) {
            int ex = left - 7, ey = top - 1;
            int eye = sanity < 15.0F ? 0xFFFF3040 : sanity < 30.0F ? 0xFFB070FF : 0xFF8060B0;
            g.fill(ex, ey + 2, ex + 5, ey + 4, 0xFFE0D8F0);
            g.fill(ex + 2, ey + 1, ex + 3, ey + 5, eye);
            g.fill(ex + 1, ey + 2, ex + 4, ey + 4, eye);
        }
        hud.rightHeight += HEIGHT + 3;
    }
}
