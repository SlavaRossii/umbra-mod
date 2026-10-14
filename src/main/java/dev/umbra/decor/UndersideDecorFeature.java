package dev.umbra.decor;

import com.mojang.serialization.MapCodec;
import dev.umbra.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Small things that fill the Underside's caverns: bones and skulls in the ash, ash mounds,
 * crystal clusters on the walls, ash pillars from floor to ceiling, glowing mushrooms.
 */
public record UndersideDecorFeature() implements Feature {
    public static final MapCodec<UndersideDecorFeature> CODEC = MapCodec.unit(new UndersideDecorFeature());
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    @Override
    public MapCodec<UndersideDecorFeature> codec() {
        return CODEC;
    }

    /** Walks down from {@code origin} to the first air block standing on solid ground. */
    public static BlockPos floor(WorldGenLevel level, BlockPos origin, int maxSteps) {
        BlockPos p = origin;
        for (int i = 0; i < maxSteps && p.getY() > 6; i++) {
            if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isSolidRender()) return p;
            p = p.below();
        }
        return null;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int kind = random.nextInt(10);
        if (kind <= 1) return crystal(level, origin, random);
        BlockPos f = floor(level, origin, 40);
        if (f == null) return false;
        switch (kind) {
            case 2, 3 -> bones(level, f, random);
            case 4, 5 -> mound(level, f, random);
            case 6 -> pillar(level, f, random);
            case 7, 8 -> mushrooms(level, f, random);
            default -> {
                level.setBlock(f, Blocks.SKELETON_SKULL.defaultBlockState(), FLAGS);
                level.setBlock(f.east(), Blocks.BONE_BLOCK.defaultBlockState(), FLAGS);
            }
        }
        return true;
    }

    private static void bones(WorldGenLevel level, BlockPos f, RandomSource random) {
        for (int i = 0; i < 3 + random.nextInt(4); i++) {
            BlockPos p = floor(level, f.offset(random.nextInt(5) - 2, 2, random.nextInt(5) - 2), 5);
            if (p == null) continue;
            level.setBlock(p, random.nextInt(4) == 0 ? Blocks.SKELETON_SKULL.defaultBlockState()
                    : Blocks.BONE_BLOCK.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,
                    random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z), FLAGS);
        }
    }

    private static void mound(WorldGenLevel level, BlockPos f, RandomSource random) {
        BlockState ash = ModBlocks.ASH_SOIL.get().defaultBlockState();
        int r = 1 + random.nextInt(2);
        for (int x = -r; x <= r; x++)
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z > r * r + 1) continue;
                BlockPos p = floor(level, f.offset(x, 2, z), 5);
                if (p != null) level.setBlock(p, ash, FLAGS);
            }
        level.setBlock(f.above(), ash, FLAGS);
    }

    private static void pillar(WorldGenLevel level, BlockPos f, RandomSource random) {
        int h = 0;
        while (h < 24 && level.getBlockState(f.above(h)).isAir()) h++;
        if (h < 4 || h >= 24) return;
        BlockState ash = ModBlocks.ASH_STONE.get().defaultBlockState();
        for (int y = 0; y < h; y++) {
            level.setBlock(f.above(y), ash, FLAGS);
            if (y < 2 || y > h - 3) for (Direction d : Direction.Plane.HORIZONTAL) if (random.nextBoolean()) level.setBlock(f.above(y).relative(d), ash, FLAGS);
        }
    }

    private static void mushrooms(WorldGenLevel level, BlockPos f, RandomSource random) {
        for (int i = 0; i < 2 + random.nextInt(5); i++) {
            BlockPos p = floor(level, f.offset(random.nextInt(5) - 2, 2, random.nextInt(5) - 2), 5);
            if (p != null && level.getBlockState(p.below()).isSolidRender()) level.setBlock(p, ModBlocks.GLOOM_MUSHROOM.get().defaultBlockState(), FLAGS);
        }
    }

    /** A cluster growing out of the nearest wall (or ceiling). Luminite glows; echo quartz is rarer. */
    private static boolean crystal(WorldGenLevel level, BlockPos origin, RandomSource random) {
        if (!level.getBlockState(origin).isAir()) return false;
        Block cluster = random.nextInt(4) == 0 ? ModBlocks.ECHO_CLUSTER.get() : ModBlocks.LUMINITE_CLUSTER.get();
        for (Direction d : Direction.values()) {
            BlockPos support = origin.relative(d);
            if (!level.getBlockState(support).isSolidRender()) continue;
            level.setBlock(origin, cluster.defaultBlockState().setValue(AmethystClusterBlock.FACING, d.getOpposite()), FLAGS);
            return true;
        }
        return false;
    }
}
