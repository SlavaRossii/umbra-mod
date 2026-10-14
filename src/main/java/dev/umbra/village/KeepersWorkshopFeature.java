package dev.umbra.village;

import com.mojang.serialization.MapCodec;
import dev.umbra.quantum.QuantumMazeFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The Keepers' workshop: a small collapsed stone hut on flat ground, where the miners of the Vein worked.
 * A crafting table, a furnace, an anvil, shelves, and a chest that holds the first pages of their notes.
 */
public record KeepersWorkshopFeature() implements Feature {
    public static final MapCodec<KeepersWorkshopFeature> CODEC = MapCodec.unit(new KeepersWorkshopFeature());
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, "chests/keepers_workshop"));
    public static final int WIDTH = 9, DEPTH = 7;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    @Override
    public MapCodec<KeepersWorkshopFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!QuantumMazeFeature.isFlat(level, origin, 7)) return false;
        BlockPos c = new BlockPos(origin.getX() - WIDTH / 2, origin.getY() - 1, origin.getZ() - DEPTH / 2);   // floor level, minimum corner
        build(level, c, random);
        return true;
    }

    /** @param c minimum corner at floor level (the floor itself is at y = c.y) */
    public static void build(WorldGenLevel level, BlockPos c, RandomSource random) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState cracked = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                boolean edge = x == 0 || z == 0 || x == WIDTH - 1 || z == DEPTH - 1;
                boolean corner = (x == 0 || x == WIDTH - 1) && (z == 0 || z == DEPTH - 1);
                // foundation, floor
                for (int y = -1; y >= -3; y--) {
                    BlockPos p = c.offset(x, y, z);
                    if (!level.getBlockState(p).isAir() && level.getBlockState(p).getFluidState().isEmpty() && y < -1) break;
                    level.setBlock(p, bricks, FLAGS);
                }
                level.setBlock(c.offset(x, 0, z), random.nextInt(4) == 0 ? mossy : bricks, FLAGS);
                for (int y = 1; y <= 4; y++) {
                    BlockPos p = c.offset(x, y, z);
                    if (corner) level.setBlock(p, log, FLAGS);
                    else if (edge && y <= 3) {
                        // the walls have stood for a long time: a few bricks have fallen out
                        level.setBlock(p, random.nextInt(9) == 0 ? air : random.nextInt(3) == 0 ? mossy : random.nextInt(5) == 0 ? cracked : bricks, FLAGS);
                    } else if (y == 4) {
                        level.setBlock(p, random.nextInt(7) == 0 ? air : planks, FLAGS);   // roof, with holes
                    } else {
                        level.setBlock(p, air, FLAGS);
                    }
                }
                level.setBlock(c.offset(x, 5, z), air, FLAGS);
            }
        }
        // the door, in the middle of the west wall
        for (int y = 1; y <= 2; y++) level.setBlock(c.offset(0, y, DEPTH / 2), air, FLAGS);
        // furniture
        level.setBlock(c.offset(1, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), FLAGS);
        level.setBlock(c.offset(1, 1, 2), Blocks.FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.FurnaceBlock.FACING, Direction.EAST), FLAGS);
        level.setBlock(c.offset(1, 1, DEPTH - 2), Blocks.ANVIL.defaultBlockState(), FLAGS);
        for (int z = 1; z < DEPTH - 1; z++) {
            level.setBlock(c.offset(WIDTH - 2, 1, z), Blocks.BOOKSHELF.defaultBlockState(), FLAGS);
            if (z % 2 == 1) level.setBlock(c.offset(WIDTH - 2, 2, z), Blocks.BOOKSHELF.defaultBlockState(), FLAGS);
        }
        level.setBlock(c.offset(WIDTH / 2, 3, DEPTH / 2), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), FLAGS);
        level.setBlock(c.offset(WIDTH / 2, 4, DEPTH / 2), planks, FLAGS);
        for (int i = 0; i < 4; i++) {
            BlockPos web = c.offset(1 + random.nextInt(WIDTH - 2), 3, 1 + random.nextInt(DEPTH - 2));
            if (level.getBlockState(web).isAir()) level.setBlock(web, Blocks.COBWEB.defaultBlockState(), FLAGS);
        }
        BlockPos chest = c.offset(WIDTH - 3, 1, DEPTH / 2);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.WEST), FLAGS);
        RandomizableContainer.setBlockEntityLootTable(level, random, chest, LOOT);
    }
}
