package dev.umbra.registry;

import dev.umbra.Umbra;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Umbra.MODID);

    /** Every Umbra item. New items: add them to the list below. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.umbra"))
            .icon(() -> ModItems.DISPELLER.get().getDefaultInstance())
            .displayItems((params, out) -> {
                out.accept(ModItems.JOURNAL.get());
                out.accept(ModItems.DISPELLER.get());
                out.accept(ModItems.LUMINITE_LANTERN.get());
                out.accept(ModItems.WARD.get());
                out.accept(ModItems.LIGHTHOUSE_LAMP.get());
                out.accept(ModItems.MARTA_MAP.get());
                out.accept(ModItems.KEEPER_HORN.get());
                out.accept(ModItems.PRIMORDIAL_HEART.get());
                out.accept(ModItems.WHISPER_TABLE.get());
                out.accept(ModItems.LUMINITE_LANTERN_BLOCK.get());
                out.accept(ModItems.TROPHY_SHELF.get());
                ModItems.TROPHIES.values().forEach(t -> out.accept(t.get()));
                out.accept(ModItems.ECHO_ALTAR.get());
                out.accept(ModItems.GLOOM_ANVIL.get());
                out.accept(ModItems.LUMINITE_CANDLE.get());
                out.accept(ModItems.THICK_LUMINITE_CANDLE.get());
                out.accept(ModItems.MELTED_LUMINITE_CANDLE.get());
                out.accept(ModItems.LUMINITE_CANDELABRUM.get());
                out.accept(ModItems.SILENCE_TINCTURE.get());
                out.accept(ModItems.MADMAN_MASK.get());
                out.accept(ModItems.DESPAIR_BLADE.get());
                out.accept(ModItems.LIGHT_ARROW.get());
                out.accept(ModItems.QUANTUM_COMPASS.get());
                out.accept(ModItems.MIRROR.get());
                out.accept(ModItems.ETERNAL_TORCH.get());
                out.accept(ModItems.LUMINITE_LAMP.get());
                out.accept(ModItems.LUMINITE_CRYSTAL.get());
                out.accept(ModItems.LUMINITE_ORE.get());
                out.accept(ModItems.DEEPSLATE_LUMINITE_ORE.get());
                out.accept(ModItems.SHADOW_SHARD.get());
                out.accept(ModItems.SEDATIVE.get());
                out.accept(ModItems.ANTIPSYCHOTIC.get());
                out.accept(ModItems.DUSK_STONE.get());
                out.accept(ModItems.ASH_STONE.get());
                out.accept(ModItems.ASH_SOIL.get());
                out.accept(ModItems.DEAD_LOG.get());
                out.accept(ModItems.GLOOM_ORE.get());
                out.accept(ModItems.RAW_GLOOM.get());
                out.accept(ModItems.GLOOM_INGOT.get());
                out.accept(ModItems.GLOOM_BLOCK.get());
                out.accept(ModItems.LUMINITE_BLOCK.get());
                out.accept(ModItems.ECHO_QUARTZ_ORE.get());
                out.accept(ModItems.ECHO_QUARTZ.get());
                out.accept(ModItems.GLOOM_SWORD.get());
                out.accept(ModItems.GLOOM_PICKAXE.get());
                out.accept(ModItems.GLOOM_AXE.get());
                out.accept(ModItems.GLOOM_SHOVEL.get());
                out.accept(ModItems.GLOOM_HELMET.get());
                out.accept(ModItems.GLOOM_CHESTPLATE.get());
                out.accept(ModItems.GLOOM_LEGGINGS.get());
                out.accept(ModItems.GLOOM_BOOTS.get());
                for (var piece : ModItems.ARMOR_PIECES) out.accept(piece.get());
                out.accept(ModItems.LUMINITE_SHIELD.get());
                out.accept(ModItems.ASH_CLASP.get());
                out.accept(ModItems.ARCHITECT_SEAL.get());
                out.accept(ModItems.CRAWLER_SPAWN_EGG.get());
                out.accept(ModItems.LURKER_SPAWN_EGG.get());
                out.accept(ModItems.BLIND_SPAWN_EGG.get());
                out.accept(ModItems.LIGHT_EATER_SPAWN_EGG.get());
                out.accept(ModItems.MIMICKER_SPAWN_EGG.get());
                out.accept(ModItems.SHADE_SPAWN_EGG.get());
                out.accept(ModItems.SHADOW_VILLAGER_SPAWN_EGG.get());
                out.accept(ModItems.LAMP_KEEPER_SPAWN_EGG.get());
                out.accept(ModItems.CITADEL_STATUE_SPAWN_EGG.get());
                out.accept(ModItems.CITADEL_KEEPER_SPAWN_EGG.get());
                out.accept(ModItems.SEAL_OF_WATCHING.get());
                out.accept(ModItems.SEAL_OF_MIMICRY.get());
                out.accept(ModItems.SEAL_OF_HERDING.get());
                out.accept(ModItems.SEAL_OF_REFLECTION.get());
                out.accept(ModItems.WATCHER_EYE.get());
                out.accept(ModItems.MIMIC_CROWN.get());
                out.accept(ModItems.SHEPHERD_CROOK.get());
                out.accept(ModItems.MIRROR_MASK.get());
                out.accept(ModItems.UNDERSIDE_KEY.get());
                out.accept(ModItems.ABYSS_HEART.get());
                out.accept(ModItems.DAWN_LANTERN.get());
                out.accept(ModItems.EXPLORER_NOTE.get());
                out.accept(ModItems.KEEPER_PAGE.get());
                out.accept(ModItems.LUMINITE_CLUSTER.get());
                out.accept(ModItems.ECHO_CLUSTER.get());
                out.accept(ModItems.GLOOM_MUSHROOM.get());
                out.accept(ModItems.ASH_FIGURE_SPAWN_EGG.get());
                out.accept(ModItems.QUANTUM_BLOCK.get());
                out.accept(ModItems.MAZE_WALL.get());
            })
            .build());

    private ModTabs() {}
}
