package dev.umbra.decor;

import com.mojang.serialization.MapCodec;
import dev.umbra.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** The whispering forest: dead trees standing close, bare branches, cobwebs between them. */
public record DeadTreeFeature() implements Feature {
    public static final MapCodec<DeadTreeFeature> CODEC = MapCodec.unit(new DeadTreeFeature());
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    @Override
    public MapCodec<DeadTreeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos f = UndersideDecorFeature.floor(level, origin, 40);
        if (f == null) return false;
        int h = 5 + random.nextInt(7);
        for (int y = 0; y < h; y++) if (!level.getBlockState(f.above(y)).isAir()) return false;
        tree(level, f, h, random);
        return true;
    }

    public static void tree(WorldGenLevel level, BlockPos f, int h, RandomSource random) {
        BlockState log = ModBlocks.DEAD_LOG.get().defaultBlockState();
        for (int y = 0; y < h; y++) level.setBlock(f.above(y), log, FLAGS);
        // roots
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (random.nextInt(2) == 0 && level.getBlockState(f.relative(d)).isAir()) {
                level.setBlock(f.relative(d), log.setValue(RotatedPillarBlock.AXIS, d.getAxis()), FLAGS);
            }
        }
        // branches, crooked and bare
        for (int i = 0; i < 2 + random.nextInt(3); i++) {
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos p = f.above(h / 2 + random.nextInt(Math.max(1, h / 2)));
            int len = 2 + random.nextInt(3);
            for (int j = 1; j <= len; j++) {
                p = p.relative(d);
                if (j == len && random.nextBoolean()) p = p.above();
                if (!level.getBlockState(p).isAir()) break;
                level.setBlock(p, log.setValue(RotatedPillarBlock.AXIS, j == len ? Direction.Axis.Y : d.getAxis()), FLAGS);
                if (random.nextInt(5) == 0 && level.getBlockState(p.below()).isAir()) level.setBlock(p.below(), Blocks.COBWEB.defaultBlockState(), FLAGS);
            }
        }
    }
}
