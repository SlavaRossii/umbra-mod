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
import dev.umbra.registry.ModTabs;
import dev.umbra.sanity.HallucinationEntity;
import dev.umbra.sanity.Sanity;
import dev.umbra.shadow.ShadowEntity;
import dev.umbra.shadow.ShadowManager;
import dev.umbra.steps.StepsManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

/**
 * Umbra (Изнанка). Entry point: registers content and routes game events to the feature managers.
 * See docs/ARCHITECTURE.md for where things live and how to add more.
 */
@Mod(Umbra.MODID)
public class Umbra {
    public static final String MODID = "umbra";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Umbra(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModFeatures.FEATURES.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModTabs.TABS.register(modBus);
        container.registerConfig(ModConfig.Type.LOCAL, UmbraConfig.SPEC, "umbra.toml");

        modBus.addListener(Umbra::onAttributes);

        NeoForge.EVENT_BUS.addListener(Umbra::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Umbra::onSetSpawn);
        NeoForge.EVENT_BUS.addListener(Umbra::onWakeUp);
        NeoForge.EVENT_BUS.addListener(Umbra::onFinishUsing);
        NeoForge.EVENT_BUS.addListener(Umbra::onLogout);
        NeoForge.EVENT_BUS.addListener(Umbra::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(Umbra::onCommands);

        if (Boolean.getBoolean("umbra.smoketest")) SmokeTest.install();
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SHADOW.get(), ShadowEntity.createAttributes().build());
        event.put(ModEntities.HALLUCINATION.get(), HallucinationEntity.createAttributes().build());
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isSpectator()) return;
        ShadowManager.tick(player);
        StepsManager.tick(player);
        MimicManager.tick(player);
        DarknessManager.tick(player);
        Sanity.tick(player);
    }

    private static void onSetSpawn(PlayerSetSpawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isForced()) {
            MimicManager.setHome(player, event.getNewSpawn());
        }
    }

    private static void onWakeUp(PlayerWakeUpEvent event) {
        // a full night's sleep, not being woken up by a monster
        if (event.getEntity() instanceof ServerPlayer player && !event.wakeImmediately() && event.updateLevel()) {
            Sanity.add(player, Sanity.Shock.SLEEP);
        }
    }

    private static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().has(DataComponents.FOOD)) {
            Sanity.add(player, Sanity.Shock.FOOD);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShadowManager.forget(player);
            StepsManager.forget(player);
            MimicManager.forget(player);
            Sanity.forget(player);
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
