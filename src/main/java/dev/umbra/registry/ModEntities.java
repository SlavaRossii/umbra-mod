package dev.umbra.registry;

import dev.umbra.Umbra;
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
import dev.umbra.sanity.HallucinationEntity;
import dev.umbra.shadow.ShadowEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Umbra.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ShadowEntity>> SHADOW = ENTITIES.registerEntityType("shadow",
            ShadowEntity::new, MobCategory.MISC,
            b -> b.sized(0.6F, 1.8F).clientTrackingRange(10).updateInterval(1).fireImmune().noSummon());

    public static final DeferredHolder<EntityType<?>, EntityType<dev.umbra.village.ShadowVillager>> SHADOW_VILLAGER = ENTITIES.registerEntityType("shadow_villager",
            dev.umbra.village.ShadowVillager::new, MobCategory.MISC,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<dev.umbra.citadel.CitadelStatue>> CITADEL_STATUE = ENTITIES.registerEntityType("citadel_statue",
            dev.umbra.citadel.CitadelStatue::new, MobCategory.MONSTER, b -> b.sized(0.7F, 2.0F).clientTrackingRange(10).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<dev.umbra.citadel.CitadelKeeper>> CITADEL_KEEPER = ENTITIES.registerEntityType("citadel_keeper",
            dev.umbra.citadel.CitadelKeeper::new, MobCategory.MONSTER, b -> b.sized(0.7F, 2.0F).clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<HallucinationEntity>> HALLUCINATION = ENTITIES.registerEntityType("hallucination",
            HallucinationEntity::new, MobCategory.MISC,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).fireImmune().noSummon());

    // --- Creatures of the Underside ---
    public static final DeferredHolder<EntityType<?>, EntityType<Crawler>> CRAWLER = ENTITIES.registerEntityType("crawler",
            Crawler::new, MobCategory.MONSTER, b -> b.sized(1.3F, 0.8F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Blind>> BLIND = ENTITIES.registerEntityType("blind",
            Blind::new, MobCategory.MONSTER, b -> b.sized(0.7F, 2.1F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<LightEater>> LIGHT_EATER = ENTITIES.registerEntityType("light_eater",
            LightEater::new, MobCategory.MONSTER, b -> b.sized(0.5F, 0.8F).clientTrackingRange(8).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<Mimicker>> MIMICKER = ENTITIES.registerEntityType("mimicker",
            Mimicker::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Shade>> SHADE = ENTITIES.registerEntityType("shade",
            Shade::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).clientTrackingRange(8));

    // --- Minibosses ---
    public static final DeferredHolder<EntityType<?>, EntityType<Watcher>> WATCHER = ENTITIES.registerEntityType("watcher",
            Watcher::new, MobCategory.MONSTER, b -> b.sized(0.7F, 2.0F).clientTrackingRange(10).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<MimicQueen>> MIMIC_QUEEN = ENTITIES.registerEntityType("mimic_queen",
            MimicQueen::new, MobCategory.MONSTER, b -> b.sized(0.7F, 2.0F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Shepherd>> SHEPHERD = ENTITIES.registerEntityType("shepherd",
            Shepherd::new, MobCategory.MONSTER, b -> b.sized(0.7F, 2.0F).clientTrackingRange(10).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<Doppelganger>> DOPPELGANGER = ENTITIES.registerEntityType("doppelganger",
            Doppelganger::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.8F).clientTrackingRange(10));

    // --- Bosses ---
    public static final DeferredHolder<EntityType<?>, EntityType<Architect>> ARCHITECT = ENTITIES.registerEntityType("architect",
            Architect::new, MobCategory.MONSTER, b -> b.sized(0.7F, 2.0F).clientTrackingRange(12).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<PrimordialShadow>> PRIMORDIAL_SHADOW = ENTITIES.registerEntityType("primordial_shadow",
            PrimordialShadow::new, MobCategory.MONSTER, b -> b.sized(0.7F, 2.0F).clientTrackingRange(12).fireImmune());

    private ModEntities() {}
}
