package dev.umbra.client;

import dev.umbra.Umbra;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * CI only (the client check and the screenshot tour): other mods tried alongside Umbra (the compatibility check) may
 * leave warnings while loading (an old property in their mods.toml, say), and the game stops on a screen of them
 * until someone presses "Proceed". Nobody is there to press it: this goes on as that button would.
 */
@EventBusSubscriber(modid = Umbra.MODID, value = Dist.CLIENT)
public final class CiWarnings {
    private static final boolean ON = Boolean.getBoolean("umbra.clientcheck") || Boolean.getBoolean("umbra.clientshots");
    private static int seen;

    private CiWarnings() {}

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        if (!ON) return;
        var screen = Minecraft.getInstance().gui.screen();
        if (!(screen instanceof net.neoforged.neoforge.client.gui.LoadingErrorScreen) || ++seen % 40 != 20) return;
        try {
            var f = net.neoforged.neoforge.client.gui.LoadingErrorScreen.class.getDeclaredField("nextScreenTask");
            f.setAccessible(true);
            Object task = f.get(screen);
            Umbra.LOGGER.info("UMBRA CI: the mod loading warnings screen is up; going on past it");
            if (task instanceof Runnable r) r.run();
        } catch (Throwable t) {
            Umbra.LOGGER.error("UMBRA CI: could not go on past the warnings screen", t);
        }
    }
}
