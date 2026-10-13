package dev.umbra;

import com.mojang.logging.LogUtils;
import dev.umbra.command.UmbraCommand;
import dev.umbra.darkness.DarknessManager;
import dev.umbra.mimic.MimicManager;
import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModEntities;
import dev.umbra.registry.ModFeatures;
import dev.umbra.registry.ModItems;
import dev.umbra.shadow.ShadowEntity;
import dev.umbra.shadow.ShadowManager;
import dev.umbra.steps.StepsManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

@Mod(Umbra.MODID)
public class Umbra {
    public static final String MODID = "umbra";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Umbra(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModFeatures.FEATURES.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);

        modBus.addListener(Umbra::onAttributes);
        modBus.addListener(Umbra::onCreativeTab);

        NeoForge.EVENT_BUS.addListener(Umbra::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Umbra::onSetSpawn);
        NeoForge.EVENT_BUS.addListener(Umbra::onLogout);
        NeoForge.EVENT_BUS.addListener(Umbra::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(Umbra::onCommands);

        if (Boolean.getBoolean("umbra.smoketest")) SmokeTest.install();
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SHADOW.get(), ShadowEntity.createAttributes().build());
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.QUANTUM_BLOCK.get());
            event.accept(ModItems.MAZE_WALL.get());
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isSpectator()) return;
        ShadowManager.tick(player);
        StepsManager.tick(player);
        MimicManager.tick(player);
        DarknessManager.tick(player);
    }

    private static void onSetSpawn(PlayerSetSpawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isForced() && event.getNewSpawn() != null) {
            MimicManager.setHome(player, event.getNewSpawn());
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShadowManager.forget(player);
            StepsManager.forget(player);
            MimicManager.forget(player);
        }
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShadowManager.forget(player);
            StepsManager.forget(player);
        }
    }

    private static void onCommands(RegisterCommandsEvent event) {
        UmbraCommand.register(event.getDispatcher());
    }
}
