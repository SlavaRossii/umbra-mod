package dev.umbra;

import com.mojang.logging.LogUtils;
import dev.umbra.command.UmbraCommand;
import dev.umbra.darkness.DarknessManager;
import dev.umbra.mimic.MimicManager;
import dev.umbra.boss.Architect;
import dev.umbra.boss.Doppelganger;
import dev.umbra.boss.PrimordialShadow;
import dev.umbra.boss.MimicQueen;
import dev.umbra.boss.Shepherd;
import dev.umbra.boss.Watcher;
import dev.umbra.mob.Blind;
import dev.umbra.mob.Crawler;
import dev.umbra.mob.LightEater;
import dev.umbra.mob.Mimicker;
import dev.umbra.mob.Shade;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
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
        modBus.addListener(Umbra::onSpawnPlacements);

        NeoForge.EVENT_BUS.addListener(Umbra::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Umbra::onSetSpawn);
        NeoForge.EVENT_BUS.addListener(Umbra::onWakeUp);
        NeoForge.EVENT_BUS.addListener(Umbra::onFinishUsing);
        NeoForge.EVENT_BUS.addListener(Umbra::onLogout);
        NeoForge.EVENT_BUS.addListener(Umbra::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(Umbra::onCommands);
        NeoForge.EVENT_BUS.addListener(dev.umbra.journal.JournalEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(dev.umbra.journal.JournalEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(dev.umbra.light.LightArrowItem::onImpact);
        NeoForge.EVENT_BUS.addListener(dev.umbra.light.WardBlock::onFinalizeSpawn);
        NeoForge.EVENT_BUS.addListener(dev.umbra.nightmare.Nightmares::onHurt);
        NeoForge.EVENT_BUS.addListener(dev.umbra.madness.DespairBladeItem::onHurt);
        NeoForge.EVENT_BUS.addListener(dev.umbra.light.SmolderingTorchBlock::onPlaced);
        NeoForge.EVENT_BUS.addListener(dev.umbra.mimic.MimicChests::onRightClick);
        NeoForge.EVENT_BUS.addListener(dev.umbra.mimic.MimicChests::onLeftClick);

        if (Boolean.getBoolean("umbra.smoketest")) SmokeTest.install();
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SHADOW.get(), ShadowEntity.createAttributes().build());
        event.put(ModEntities.HALLUCINATION.get(), HallucinationEntity.createAttributes().build());
        event.put(ModEntities.CRAWLER.get(), Crawler.createAttributes().build());
        event.put(ModEntities.BLIND.get(), Blind.createAttributes().build());
        event.put(ModEntities.LIGHT_EATER.get(), LightEater.createAttributes().build());
        event.put(ModEntities.MIMICKER.get(), Mimicker.createAttributes().build());
        event.put(ModEntities.SHADE.get(), Shade.createAttributes().build());
        event.put(ModEntities.CITADEL_STATUE.get(), dev.umbra.citadel.CitadelStatue.createAttributes().build());
        event.put(ModEntities.CITADEL_KEEPER.get(), dev.umbra.citadel.CitadelKeeper.createAttributes().build());
        event.put(ModEntities.SHADOW_VILLAGER.get(), dev.umbra.village.ShadowVillager.createAttributes().build());
        event.put(ModEntities.WATCHER.get(), Watcher.createAttributes().build());
        event.put(ModEntities.MIMIC_QUEEN.get(), MimicQueen.createAttributes().build());
        event.put(ModEntities.SHEPHERD.get(), Shepherd.createAttributes().build());
        event.put(ModEntities.DOPPELGANGER.get(), Doppelganger.createAttributes().build());
        event.put(ModEntities.ARCHITECT.get(), Architect.createAttributes().build());
        event.put(ModEntities.PRIMORDIAL_SHADOW.get(), PrimordialShadow.createAttributes().build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        var ground = SpawnPlacementTypes.ON_GROUND;
        var heightmap = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        event.register(ModEntities.CRAWLER.get(), ground, heightmap, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.BLIND.get(), ground, heightmap, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.MIMICKER.get(), ground, heightmap, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SHADE.get(), ground, heightmap, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.LIGHT_EATER.get(), SpawnPlacementTypes.NO_RESTRICTIONS, heightmap, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isSpectator()) return;
        ShadowManager.tick(player);
        StepsManager.tick(player);
        MimicManager.tick(player);
        dev.umbra.mimic.MimicChests.tick(player);
        DarknessManager.tick(player);
        Sanity.tick(player);
        dev.umbra.journal.JournalEvents.tick(player);
        dev.umbra.nightmare.Nightmares.tick(player);
    }

    private static void onSetSpawn(PlayerSetSpawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isForced()) {
            MimicManager.setHome(player, event.getNewSpawn());
        }
    }

    private static void onWakeUp(PlayerWakeUpEvent event) {
        // a full night's sleep, not being woken up by a monster
        if (event.getEntity() instanceof ServerPlayer player && !event.wakeImmediately() && event.updateLevel()) {
            // a troubled mind may not get its rest
            if (dev.umbra.nightmare.Nightmares.onSleep(player)) return;
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
