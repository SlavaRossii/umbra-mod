package dev.umbra.registry;

import dev.umbra.Umbra;
import dev.umbra.craft.GloomAnvilMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Umbra.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<GloomAnvilMenu>> GLOOM_ANVIL = MENUS.register("gloom_anvil",
            () -> IMenuTypeExtension.create((id, inv, buf) -> new GloomAnvilMenu(id, inv)));

    private ModMenus() {}
}
