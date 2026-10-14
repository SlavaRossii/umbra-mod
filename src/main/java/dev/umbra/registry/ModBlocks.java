package dev.umbra.registry;

import dev.umbra.Umbra;
import dev.umbra.darkness.LivingDarknessBlock;
import dev.umbra.light.EternalTorchBlock;
import dev.umbra.light.EternalWallTorchBlock;
import dev.umbra.light.HeldLightBlock;
import dev.umbra.light.SmolderingTorchBlock;
import dev.umbra.light.SmolderingWallTorchBlock;
import dev.umbra.light.WardBlock;
import dev.umbra.mimic.MimicBlock;
import dev.umbra.quantum.MazeCoreBlock;
import dev.umbra.quantum.QuantumBlock;
import dev.umbra.underside.RiftBlock;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.resources.Identifier;
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
                    .isRedstoneConductor((s, l, p) -> false));

    public static final DeferredBlock<Block> MAZE_WALL = BLOCKS.registerSimpleBlock("maze_wall",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .pushReaction(PushReaction.IMMOVEABLE));

    public static final DeferredBlock<MazeCoreBlock> MAZE_CORE = BLOCKS.registerBlock("maze_core",
            MazeCoreBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .randomTicks()
                    .pushReaction(PushReaction.IMMOVEABLE));

    // --- Luminite: the light that the dark fears ---
    public static final DeferredBlock<DropExperienceBlock> LUMINITE_ORE = BLOCKS.registerBlock("luminite_ore",
            p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).lightLevel(s -> 5));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_LUMINITE_ORE = BLOCKS.registerBlock("deepslate_luminite_ore",
            p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE).lightLevel(s -> 5));
    public static final DeferredBlock<Block> LUMINITE_LAMP = BLOCKS.registerSimpleBlock("luminite_lamp",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SEA_LANTERN).lightLevel(s -> 12).mapColor(MapColor.COLOR_LIGHT_BLUE));
    public static final DeferredBlock<EternalTorchBlock> ETERNAL_TORCH = BLOCKS.registerBlock("eternal_torch",
            p -> new EternalTorchBlock(ParticleTypes.END_ROD, p),
            () -> torchProperties());
    public static final DeferredBlock<EternalWallTorchBlock> ETERNAL_WALL_TORCH = BLOCKS.registerBlock("eternal_wall_torch",
            p -> new EternalWallTorchBlock(ParticleTypes.END_ROD, p),
            () -> torchProperties());

    /** Invisible light that follows a lit luminite lantern and removes itself when the lantern is gone. */
    public static final DeferredBlock<HeldLightBlock> HELD_LIGHT = BLOCKS.registerBlock("held_light",
            HeldLightBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .noCollision()
                    .noOcclusion()
                    .replaceable()
                    .noLootTable()
                    .strength(-1.0F, 0.0F)
                    .lightLevel(s -> HeldLightBlock.LIGHT)
                    .pushReaction(PushReaction.POPPED));

    /** The Ward: burns luminite and keeps everything dark away. */
    public static final DeferredBlock<WardBlock> WARD = BLOCKS.registerBlock("ward",
            WardBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BEACON)
                    .strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(WardBlock.CHARGE) > 0 ? 15 : 0)
                    .mapColor(MapColor.COLOR_LIGHT_BLUE));

    /** What an ordinary torch turns into in the Underside: dim, and it burns out. */
    public static final DeferredBlock<SmolderingTorchBlock> SMOLDERING_TORCH = BLOCKS.registerBlock("smoldering_torch",
            p -> new SmolderingTorchBlock(ParticleTypes.SMOKE, p),
            () -> torchProperties().lightLevel(s -> SmolderingTorchBlock.LIGHT));
    public static final DeferredBlock<SmolderingWallTorchBlock> SMOLDERING_WALL_TORCH = BLOCKS.registerBlock("smoldering_wall_torch",
            p -> new SmolderingWallTorchBlock(ParticleTypes.SMOKE, p),
            () -> torchProperties().lightLevel(s -> SmolderingTorchBlock.LIGHT));

    /** The Whisper Table: crafts for a slipping mind. */
    public static final DeferredBlock<dev.umbra.madness.WhisperTableBlock> WHISPER_TABLE = BLOCKS.registerBlock("whisper_table",
            dev.umbra.madness.WhisperTableBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).mapColor(MapColor.COLOR_BLACK).lightLevel(s -> 3));

    /** The way out of a nightmare. */
    public static final DeferredBlock<dev.umbra.nightmare.DreamExitBlock> DREAM_EXIT = BLOCKS.registerBlock("dream_exit",
            dev.umbra.nightmare.DreamExitBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .lightLevel(s -> 15)
                    .sound(SoundType.AMETHYST)
                    .pushReaction(PushReaction.IMMOVEABLE)
                    .mapColor(MapColor.SNOW));

    // --- The Quantum Citadel (1.4) ---
    public static final DeferredBlock<dev.umbra.citadel.CitadelGateBlock> CITADEL_GATE = BLOCKS.registerBlock("citadel_gate",
            dev.umbra.citadel.CitadelGateBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .noCollision()
                    .noOcclusion()
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .lightLevel(s -> 9)
                    .sound(SoundType.GLASS)
                    .pushReaction(PushReaction.IMMOVEABLE)
                    .mapColor(MapColor.COLOR_CYAN));
    public static final DeferredBlock<dev.umbra.citadel.CitadelSealBlock> CITADEL_SEAL = BLOCKS.registerBlock("citadel_seal",
            dev.umbra.citadel.CitadelSealBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .lightLevel(s -> 4)
                    .pushReaction(PushReaction.IMMOVEABLE));
    public static final DeferredBlock<dev.umbra.citadel.PhantomWallBlock> PHANTOM_WALL = BLOCKS.registerBlock("phantom_wall",
            dev.umbra.citadel.PhantomWallBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .noOcclusion()
                    .isSuffocating((s, l, p) -> false)
                    .pushReaction(PushReaction.IMMOVEABLE));

    private static BlockBehaviour.Properties torchProperties() {
        return BlockBehaviour.Properties.of()
                .noCollision()
                .instabreak()
                .lightLevel(s -> 14)
                .sound(SoundType.WOOD)
                .pushReaction(PushReaction.POPPED);
    }

    // --- The Underside ---
    public static final DeferredBlock<Block> ASH_STONE = BLOCKS.registerSimpleBlock("ash_stone",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.COLOR_GRAY));
    public static final DeferredBlock<Block> ASH_SOIL = BLOCKS.registerSimpleBlock("ash_soil",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SOUL_SOIL).mapColor(MapColor.COLOR_LIGHT_GRAY));
    public static final DeferredBlock<Block> DUSK_STONE = BLOCKS.registerSimpleBlock("dusk_stone",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(30.0F, 1200.0F).mapColor(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<RotatedPillarBlock> DEAD_LOG = BLOCKS.registerBlock("dead_log",
            RotatedPillarBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).mapColor(MapColor.COLOR_GRAY));
    public static final DeferredBlock<DropExperienceBlock> GLOOM_ORE = BLOCKS.registerBlock("gloom_ore",
            p -> new DropExperienceBlock(UniformInt.of(1, 3), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE).mapColor(MapColor.COLOR_GRAY));
    public static final DeferredBlock<DropExperienceBlock> ECHO_QUARTZ_ORE = BLOCKS.registerBlock("echo_quartz_ore",
            p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE).lightLevel(s -> 3));
    public static final DeferredBlock<Block> GLOOM_BLOCK = BLOCKS.registerSimpleBlock("gloom_block",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).mapColor(MapColor.COLOR_BLACK));
    public static final DeferredBlock<RiftBlock> RIFT = BLOCKS.registerBlock("rift",
            RiftBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .noCollision()
                    .strength(-1.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(s -> 6)
                    .noLootTable()
                    .pushReaction(PushReaction.IMMOVEABLE)
                    .mapColor(MapColor.COLOR_PURPLE));

    // --- Mimics: each mimic pretends to be one vanilla block ---
    public static final Map<String, DeferredBlock<MimicBlock>> MIMICS = new LinkedHashMap<>();
    public static final DeferredBlock<MimicBlock> MIMIC_CRAFTING_TABLE = mimic("crafting_table", () -> Blocks.CRAFTING_TABLE);
    public static final DeferredBlock<MimicBlock> MIMIC_BOOKSHELF = mimic("bookshelf", () -> Blocks.BOOKSHELF);
    public static final DeferredBlock<MimicBlock> MIMIC_OAK_PLANKS = mimic("oak_planks", () -> Blocks.OAK_PLANKS);
    public static final DeferredBlock<MimicBlock> MIMIC_SPRUCE_PLANKS = mimic("spruce_planks", () -> Blocks.SPRUCE_PLANKS);
    public static final DeferredBlock<MimicBlock> MIMIC_COBBLESTONE = mimic("cobblestone", () -> Blocks.COBBLESTONE);
    public static final DeferredBlock<MimicBlock> MIMIC_STONE_BRICKS = mimic("stone_bricks", () -> Blocks.STONE_BRICKS);
    // furniture of villages and camps
    public static final DeferredBlock<MimicBlock> MIMIC_BARREL = mimic("barrel", () -> Blocks.BARREL);
    public static final DeferredBlock<MimicBlock> MIMIC_FURNACE = mimic("furnace", () -> Blocks.FURNACE);
    public static final DeferredBlock<MimicBlock> MIMIC_HAY_BLOCK = mimic("hay_block", () -> Blocks.HAY_BLOCK);
    public static final DeferredBlock<MimicBlock> MIMIC_OAK_LOG = mimic("oak_log", () -> Blocks.OAK_LOG);
    // ores: mining one is exactly what it wants
    public static final String[] MIMIC_ORES = {
            "coal_ore", "iron_ore", "copper_ore", "gold_ore", "redstone_ore", "lapis_ore", "diamond_ore", "emerald_ore",
            "deepslate_coal_ore", "deepslate_iron_ore", "deepslate_copper_ore", "deepslate_gold_ore",
            "deepslate_redstone_ore", "deepslate_lapis_ore", "deepslate_diamond_ore"};
    static {
        for (String ore : MIMIC_ORES) {
            mimic(ore, () -> net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(ore)), true);
        }
    }

    private static DeferredBlock<MimicBlock> mimic(String original, Supplier<Block> originalBlock) {
        return mimic(original, originalBlock, false);
    }

    private static DeferredBlock<MimicBlock> mimic(String original, Supplier<Block> originalBlock, boolean ore) {
        DeferredBlock<MimicBlock> block = BLOCKS.registerBlock("mimic_" + original,
                props -> new MimicBlock(props, originalBlock, ore),
                () -> BlockBehaviour.Properties.ofFullCopy(originalBlock.get())
                        .noLootTable()
                        // some originals (furnace, redstone ore) read a LIT property we do not have
                        .lightLevel(s -> 0)
                        // and logs pick their map colour from an AXIS property
                        .mapColor(originalBlock.get().defaultMapColor())
                        // the tell: it sounds slightly wrong when you walk on it
                        .sound(SoundType.HONEY_BLOCK));
        MIMICS.put(original, block);
        return block;
    }

    private ModBlocks() {}
}
