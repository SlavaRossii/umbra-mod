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
        dev.umbra.registry.ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModFeatures.FEATURES.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        dev.umbra.registry.ModComponents.COMPONENTS.register(modBus);
        dev.umbra.registry.ModMenus.MENUS.register(modBus);
        dev.umbra.registry.ModSounds.SOUNDS.register(modBus);
        dev.umbra.registry.ModEffects.MOB_EFFECTS.register(modBus);
        ModTabs.TABS.register(modBus);
        container.registerConfig(ModConfig.Type.LOCAL, UmbraConfig.SPEC, "umbra.toml");

        modBus.addListener(Umbra::onAttributes);
        modBus.addListener(Umbra::onSpawnPlacements);

        NeoForge.EVENT_BUS.addListener(dev.umbra.journal.JournalEvents::onOpen);
        NeoForge.EVENT_BUS.addListener(Umbra::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Umbra::onSetSpawn);
        NeoForge.EVENT_BUS.addListener(Umbra::onWakeUp);
        NeoForge.EVENT_BUS.addListener(Umbra::onFinishUsing);
        NeoForge.EVENT_BUS.addListener(Umbra::onLogout);
        NeoForge.EVENT_BUS.addListener(Umbra::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(Umbra::onCommands);
        NeoForge.EVENT_BUS.addListener(dev.umbra.journal.JournalEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(dev.umbra.journal.JournalEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(dev.umbra.decor.DeathDouble::onDeath);
        NeoForge.EVENT_BUS.addListener(dev.umbra.light.LightArrowItem::onImpact);
        NeoForge.EVENT_BUS.addListener(dev.umbra.light.WardBlock::onFinalizeSpawn);
        NeoForge.EVENT_BUS.addListener(dev.umbra.nightmare.Nightmares::onHurt);
        NeoForge.EVENT_BUS.addListener(dev.umbra.light.SmolderingTorchBlock::onPlaced);
        NeoForge.EVENT_BUS.addListener(dev.umbra.mimic.MimicChests::onRightClick);
        NeoForge.EVENT_BUS.addListener(dev.umbra.mimic.MimicChests::onLeftClick);
        NeoForge.EVENT_BUS.addListener(dev.umbra.lighthouse.LighthouseRaid::onServerTick);
        NeoForge.EVENT_BUS.addListener(dev.umbra.ending.Endings::onTarget);
        NeoForge.EVENT_BUS.addListener(dev.umbra.ending.Endings::onIncoming);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.EnchantEvents::onDamage);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.EnchantEvents::onResonance);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.EnchantEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.EnchantEvents::onDrops);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.EnchantEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.EnchantEvents::onImpact);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.EnchantEvents::onFall);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.EnchantEvents::onAnvil);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.EnchantEvents::onTooltip);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.EnchantEvents::onIncoming);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.WeaponEvents::onIncoming);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.WeaponEvents::onDamage);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.WeaponEvents::arrowTrail);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.WeaponEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.WeaponArts::onAttack);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.WeaponArts::onDeath);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.WeaponArts::onImpact);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.WeaponArts::levelTick);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.RangedWeapons::onImpact);
        NeoForge.EVENT_BUS.addListener(dev.umbra.mob.Weeper::onShard);
        NeoForge.EVENT_BUS.addListener(dev.umbra.gear.RangedWeapons::onIncoming);

        if (Boolean.getBoolean("umbra.smoketest")) SmokeTest.install();
        // CI: a dedicated server that only makes a world for the client screenshots, then stops
        if (Boolean.getBoolean("umbra.makeworld"))
            NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> {
                // the client opens it without asking about the experimental settings (it would wait on that screen)
                if (e.getServer().getWorldData() instanceof net.minecraft.world.level.storage.PrimaryLevelData pld) pld.withConfirmedWarning(true);
                e.getServer().halt(false);
            });
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SHADOW.get(), ShadowEntity.createAttributes().build());
        event.put(ModEntities.HALLUCINATION.get(), HallucinationEntity.createAttributes().build());
        event.put(ModEntities.CRAWLER.get(), Crawler.createAttributes().build());
        event.put(ModEntities.LURKER.get(), dev.umbra.lake.Lurker.createAttributes().build());
        event.put(ModEntities.BLIND.get(), Blind.createAttributes().build());
        event.put(ModEntities.WEEPER.get(), dev.umbra.mob.Weeper.createAttributes().build());
        event.put(ModEntities.BELLRINGER.get(), dev.umbra.mob.Bellringer.createAttributes().build());
        event.put(ModEntities.LIGHT_EATER.get(), LightEater.createAttributes().build());
        event.put(ModEntities.MIMICKER.get(), Mimicker.createAttributes().build());
        event.put(ModEntities.SHADE.get(), Shade.createAttributes().build());
        for (var ph : java.util.List.of(ModEntities.PHANTOM_ZOMBIE, ModEntities.PHANTOM_SKELETON, ModEntities.PHANTOM_HUSK, ModEntities.PHANTOM_STEVE, ModEntities.PHANTOM_SHADOW)) {
            event.put(ph.get(), dev.umbra.sanity.PhantomEntity.createAttributes().build());
        }
        event.put(ModEntities.ASH_FIGURE.get(), dev.umbra.decor.AshFigure.createAttributes().build());
        event.put(ModEntities.DEATH_DOUBLE.get(), dev.umbra.decor.DeathDouble.createAttributes().build());
        event.put(ModEntities.CITADEL_STATUE.get(), dev.umbra.citadel.CitadelStatue.createAttributes().build());
        event.put(ModEntities.CITADEL_KEEPER.get(), dev.umbra.citadel.CitadelKeeper.createAttributes().build());
        event.put(ModEntities.SHADOW_VILLAGER.get(), dev.umbra.village.ShadowVillager.createAttributes().build());
        event.put(ModEntities.LAMP_KEEPER.get(), dev.umbra.lighthouse.LampKeeper.createAttributes().build());
        event.put(ModEntities.FALLEN_KEEPER.get(), dev.umbra.lighthouse.FallenKeeper.createAttributes().build());
        event.put(ModEntities.ACOLYTE.get(), dev.umbra.depths.Acolyte.createAttributes().build());
        event.put(ModEntities.DROWNED_KEEPER.get(), dev.umbra.depths.DrownedKeeper.createAttributes().build());
        event.put(ModEntities.WATCHER.get(), Watcher.createAttributes().build());
        event.put(ModEntities.MIMIC_QUEEN.get(), MimicQueen.createAttributes().build());
        event.put(ModEntities.SHEPHERD.get(), Shepherd.createAttributes().build());
        event.put(ModEntities.DOPPELGANGER.get(), Doppelganger.createAttributes().build());
        event.put(ModEntities.ARCHITECT.get(), Architect.createAttributes().build());
        event.put(ModEntities.PRIMORDIAL_SHADOW.get(), PrimordialShadow.createAttributes().build());
    }

    /** Out in the world they need the dark; a spawner cage (the citadel's, lit by luminite) calls them anyway. */
    private static <T extends Monster> boolean darkOrCaged(net.minecraft.world.entity.EntityType<T> type, net.minecraft.world.level.ServerLevelAccessor level,
                                                           net.minecraft.world.entity.EntitySpawnReason reason, net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
        if (reason == net.minecraft.world.entity.EntitySpawnReason.SPAWNER) return Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random);
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        var ground = SpawnPlacementTypes.ON_GROUND;
        var heightmap = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        event.register(ModEntities.LURKER.get(), net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                dev.umbra.lake.Lurker::checkSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.CRAWLER.get(), ground, heightmap, Umbra::darkOrCaged, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.BLIND.get(), ground, heightmap, Umbra::darkOrCaged, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.MIMICKER.get(), ground, heightmap, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.WEEPER.get(), ground, heightmap, Umbra::darkOrCaged, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SHADE.get(), ground, heightmap, Umbra::darkOrCaged, RegisterSpawnPlacementsEvent.Operation.REPLACE);
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
        dev.umbra.registry.ModEffects.tick(player);
        dev.umbra.gear.Gear.tick(player);
        dev.umbra.gear.WeaponEvents.setTick(player);
        dev.umbra.gear.SetPowers.tick(player);
        dev.umbra.journal.JournalEvents.tick(player);
        dev.umbra.nightmare.Nightmares.tick(player);
        dev.umbra.village.VillageManager.tick(player);
        dev.umbra.lighthouse.LighthouseManager.tick(player);
        dev.umbra.lighthouse.YanErrands.tick(player);
        dev.umbra.depths.Depths.pearlTick(player);
        dev.umbra.depths.Depths.tick(player);
        dev.umbra.underside.UndersideAmbience.tick(player);
        dev.umbra.ending.Endings.tick(player);
        dev.umbra.night.NightEvents.tick(player);
    }

    private static void onSetSpawn(PlayerSetSpawnEvent event) {
        // getNewSpawn() is null when the spawn point is being reset (a lost bed, a drained anchor): that happens in the
        // middle of a respawn, and an exception here used to leave a dead player unable to come back
        if (event.getEntity() instanceof ServerPlayer player && !event.isForced() && event.getNewSpawn() != null) {
            MimicManager.setHome(player, event.getNewSpawn());
        }
    }

    private static void onWakeUp(PlayerWakeUpEvent event) {
        // a full night's sleep (the night was skipped), not leaving the bed by hand
        if (event.getEntity() instanceof ServerPlayer player && !event.wakeImmediately()) {
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
            dev.umbra.underside.UndersideAmbience.forget(player);
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
