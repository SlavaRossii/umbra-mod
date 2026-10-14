package dev.umbra.lake;

import com.mojang.serialization.MapCodec;
import dev.umbra.Umbra;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

/**
 * A temple drowned in a black lake: broken columns stick out of the water and mark the place. Below, a hall of
 * dusk stone with a collapsed roof, an altar and a chest. Something with a pale light on a stalk guards it.
 */
public record SunkenTempleFeature() implements Feature {
    public static final MapCodec<SunkenTempleFeature> CODEC = MapCodec.unit(new SunkenTempleFeature());
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Umbra.MODID, "chests/sunken_temple"));
    private static final int FLAGS = Block.UPDATE_CLIENTS;
    /** Hall depth below the water surface. */
    public static final int DEPTH = 10;

    @Override
    public MapCodec<SunkenTempleFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos f = BlackLakeFeature.openFloor(level, origin);
        if (f == null || f.getY() < DEPTH + 8) return false;
        build(level, f, random);
        return true;
    }

    /** Builds lake and temple around {@code floor}; returns the chest position. */
    public static BlockPos build(WorldGenLevel level, BlockPos floor, RandomSource random) {
        int surface = BlackLakeFeature.lake(level, floor, 13, 13, 6, random);
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState dusk = ModBlocks.DUSK_STONE.get().defaultBlockState();
        BlockState trim = ModBlocks.GLOOM_BLOCK.get().defaultBlockState();
        BlockState stone = ModBlocks.ASH_STONE.get().defaultBlockState();
        int y0 = surface - DEPTH + 1;          // hall floor level (water)
        BlockPos c = new BlockPos(floor.getX(), y0, floor.getZ());
        List<BlockPos> pit = new ArrayList<>();
        // a pit for the hall, all water
        for (int dx = -7; dx <= 7; dx++)
            for (int dz = -7; dz <= 7; dz++)
                for (int y = y0; y <= surface; y++) {
                    BlockPos p = new BlockPos(c.getX() + dx, y, c.getZ() + dz);
                    level.setBlock(p, water, FLAGS);
                    pit.add(p);
                }
        BlackLakeFeature.seal(level, pit, stone);
        // foundation
        for (int dx = -6; dx <= 6; dx++)
            for (int dz = -6; dz <= 6; dz++) level.setBlock(c.offset(dx, -1, dz), dx % 3 == 0 && dz % 3 == 0 ? trim : dusk, FLAGS);
        // walls, 9x9, 7 high, door on the north side
        for (int dx = -4; dx <= 4; dx++)
            for (int dz = -4; dz <= 4; dz++) {
                boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 4;
                if (!edge) continue;
                for (int y = 0; y < 7; y++) {
                    boolean door = dz == -4 && Math.abs(dx) <= 1 && y < 4;
                    boolean window = (Math.abs(dx) == 4 || Math.abs(dz) == 4) && (dx + dz) % 3 == 0 && y == 4;
                    boolean broken = y >= 5 && random.nextInt(4) == 0;
                    if (door || window || broken) continue;
                    level.setBlock(c.offset(dx, y, dz), y == 0 || y == 6 ? trim : dusk, FLAGS);
                }
            }
        // roof, fallen in the middle
        for (int dx = -4; dx <= 4; dx++)
            for (int dz = -4; dz <= 4; dz++) {
                if (dx * dx + dz * dz <= 5 + random.nextInt(4)) continue;
                level.setBlock(c.offset(dx, 7, dz), dusk, FLAGS);
            }
        // rubble from the roof on the floor
        for (int i = 0; i < 6; i++) level.setBlock(c.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2), dusk, FLAGS);
        // four broken columns at the corners, standing out of the water
        for (int[] q : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) {
            int top = surface + 1 + random.nextInt(5);
            for (int y = y0 - 1; y <= top; y++) level.setBlock(c.offset(q[0], y - y0, q[1]), y == top ? trim : dusk, FLAGS);
        }
        // altar and chest
        level.setBlock(c.offset(0, 0, 2), trim, FLAGS);
        BlockPos chest = c.offset(0, 1, 2);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(BlockStateProperties.WATERLOGGED, true), FLAGS);
        RandomizableContainer.setBlockEntityLootTable(level, random, chest, LOOT);
        // drowned lanterns, still burning
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.WATERLOGGED, true);
        for (int[] q : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) level.setBlock(c.offset(q[0], 0, q[1]), lantern, FLAGS);
        level.setBlock(c.offset(-1, 0, 3), ModBlocks.LUMINITE_CLUSTER.get().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true), FLAGS);
        // the guards
        for (int i = 0; i < 2; i++) {
            Lurker l = ModEntities.LURKER.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (l == null) continue;
            l.snapTo(c.getX() + 0.5 + (i == 0 ? -5 : 5), y0 + 3, c.getZ() + 0.5, random.nextFloat() * 360, 0);
            l.setPersistenceRequired();
            level.addFreshEntity(l);
        }
        return chest;
    }
}
