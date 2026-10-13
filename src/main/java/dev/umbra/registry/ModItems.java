package dev.umbra.registry;

import dev.umbra.Umbra;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Umbra.MODID);

    public static final DeferredItem<BlockItem> QUANTUM_BLOCK = ITEMS.registerSimpleBlockItem("quantum_block", ModBlocks.QUANTUM_BLOCK);
    public static final DeferredItem<BlockItem> MAZE_WALL = ITEMS.registerSimpleBlockItem("maze_wall", ModBlocks.MAZE_WALL);

    private ModItems() {}
}
