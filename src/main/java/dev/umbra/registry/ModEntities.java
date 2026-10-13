package dev.umbra.registry;

import dev.umbra.Umbra;
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

    private ModEntities() {}
}
