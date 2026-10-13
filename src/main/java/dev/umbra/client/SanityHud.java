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

        g.fill(left - 1, top - 1, right + 1, bottom + 1, 0xC0000000);
        if (filled > 0) g.fill(right - filled, top, right, bottom, color);
        hud.rightHeight += HEIGHT + 3;
    }
}
