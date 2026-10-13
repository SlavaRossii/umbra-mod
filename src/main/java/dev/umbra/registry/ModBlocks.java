package dev.umbra.registry;

import dev.umbra.Umbra;
import dev.umbra.darkness.LivingDarknessBlock;
import dev.umbra.mimic.MimicBlock;
import dev.umbra.quantum.QuantumBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Umbra.MODID);

    // --- Living darkness ---
    public static final DeferredBlock<LivingDarknessBlock> LIVING_DARKNESS = BLOCKS.registerBlock("living_darkness",
            LivingDarknessBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .noCollision()
                    .noOcclusion()
                    .replaceable()
                    .randomTicks()
                    .noLootTable()
                    .strength(-1.0F, 0.0F)
                    .pushReaction(PushReaction.POPPED)
                    .isSuffocating((s, l, p) -> false)
                    .isViewBlocking((s, l, p) -> false)
                    .sound(SoundType.WOOL));

    // --- Quantum maze ---
    public static final DeferredBlock<QuantumBlock> QUANTUM_BLOCK = BLOCKS.registerBlock("quantum_block",
            QuantumBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .randomTicks()
                    .pushReaction(PushReaction.IMMOVEABLE)
                    .isSuffocating((s, l, p) -> !s.getValue(QuantumBlock.OPEN))
                    .isViewBlocking((s, l, p) -> !s.getValue(QuantumBlock.OPEN))
                    .isRedstoneConductor((s, l, p) -> false));

    public static final DeferredBlock<Block> MAZE_WALL = BLOCKS.registerSimpleBlock("maze_wall",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .pushReaction(PushReaction.IMMOVEABLE));

    // --- Mimics: each mimic pretends to be one vanilla block ---
    public static final Map<String, DeferredBlock<MimicBlock>> MIMICS = new LinkedHashMap<>();
    public static final DeferredBlock<MimicBlock> MIMIC_CRAFTING_TABLE = mimic("crafting_table", () -> Blocks.CRAFTING_TABLE);
    public static final DeferredBlock<MimicBlock> MIMIC_BOOKSHELF = mimic("bookshelf", () -> Blocks.BOOKSHELF);
    public static final DeferredBlock<MimicBlock> MIMIC_OAK_PLANKS = mimic("oak_planks", () -> Blocks.OAK_PLANKS);
    public static final DeferredBlock<MimicBlock> MIMIC_SPRUCE_PLANKS = mimic("spruce_planks", () -> Blocks.SPRUCE_PLANKS);
    public static final DeferredBlock<MimicBlock> MIMIC_COBBLESTONE = mimic("cobblestone", () -> Blocks.COBBLESTONE);
    public static final DeferredBlock<MimicBlock> MIMIC_STONE_BRICKS = mimic("stone_bricks", () -> Blocks.STONE_BRICKS);

    private static DeferredBlock<MimicBlock> mimic(String original, Supplier<Block> originalBlock) {
        DeferredBlock<MimicBlock> block = BLOCKS.registerBlock("mimic_" + original,
                props -> new MimicBlock(props, originalBlock),
                () -> BlockBehaviour.Properties.ofFullCopy(originalBlock.get())
                        .noLootTable()
                        // the tell: it sounds slightly wrong when you walk on it
                        .sound(SoundType.HONEY_BLOCK));
        MIMICS.put(original, block);
        return block;
    }

    private ModBlocks() {}
}
