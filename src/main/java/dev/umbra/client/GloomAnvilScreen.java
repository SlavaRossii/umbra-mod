package dev.umbra.client;

import dev.umbra.Umbra;
import dev.umbra.craft.GloomAnvilMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The gloom anvil's window (background: textures/gui/container/gloom_anvil.png). */
public class GloomAnvilScreen extends ItemCombinerScreen<GloomAnvilMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(Umbra.MODID, "textures/gui/container/gloom_anvil.png");

    public GloomAnvilScreen(GloomAnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BG);
    }

    @Override
    protected void extractErrorIcon(GuiGraphicsExtractor g, int x, int y) {
    }
}
