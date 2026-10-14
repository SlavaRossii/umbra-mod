package dev.umbra.registry;

import com.mojang.serialization.Codec;
import dev.umbra.Umbra;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Umbra.MODID);

    /** The artifact pinned to a chestplate ("ash_clasp", "architect_seal"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ARTIFACT = COMPONENTS.registerComponentType("artifact",
            b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    private ModComponents() {}
}
