package dev.umbra.registry;

import dev.umbra.Umbra;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 2.8: the mod's block entities (the sacrificial altar is the first). */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Umbra.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.umbra.altar.SummoningAltarBlockEntity>> SUMMONING_ALTAR =
            BLOCK_ENTITIES.register("summoning_altar", () -> new BlockEntityType<>(dev.umbra.altar.SummoningAltarBlockEntity::new, ModBlocks.SUMMONING_ALTAR.get()));

    private ModBlockEntities() {}
}
