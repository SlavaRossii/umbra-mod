package dev.umbra.underside;

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

/** A dead tree growing down from the cave ceiling, its roots in the rock above. */
public record HangingTreeFeature() implements Feature {
    public static final MapCodec<HangingTreeFeature> CODEC = MapCodec.unit(new HangingTreeFeature());

    @Override
    public MapCodec<HangingTreeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // find the ceiling above an air pocket
        BlockPos p = origin;
        if (!level.getBlockState(p).isAir()) return false;
        int up = 0;
        while (up < 40 && level.getBlockState(p.above()).isAir()) {
            p = p.above();
            up++;
        }
        BlockState ceiling = level.getBlockState(p.above());
        if (up >= 40 || !ceiling.is(ModBlocks.ASH_STONE.get()) && !ceiling.is(ModBlocks.ASH_SOIL.get())) return false;

        BlockState log = ModBlocks.DEAD_LOG.get().defaultBlockState();
        int length = 4 + random.nextInt(6);
        BlockPos tip = p;
        for (int i = 0; i < length; i++) {
            BlockPos q = p.below(i);
            if (!level.getBlockState(q).isAir()) break;
            dev.umbra.decor.UndersideDecorFeature.put(level, q, log, Block.UPDATE_CLIENTS);
            tip = q;
            // crooked branches
            if (i > 1 && random.nextInt(3) == 0) {
                Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                int branch = 1 + random.nextInt(3);
                for (int b = 1; b <= branch; b++) {
                    BlockPos bp = q.relative(d, b).below(b / 2);
                    if (!level.getBlockState(bp).isAir()) break;
                    dev.umbra.decor.UndersideDecorFeature.put(level, bp, log.setValue(RotatedPillarBlock.AXIS, d.getAxis()), Block.UPDATE_CLIENTS);
                    if (random.nextInt(2) == 0 && level.getBlockState(bp.below()).isAir()) {
                        dev.umbra.decor.UndersideDecorFeature.put(level, bp.below(), Blocks.PALE_HANGING_MOSS.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        if (random.nextInt(3) == 0 && level.getBlockState(tip.below()).isAir()) {
            dev.umbra.decor.UndersideDecorFeature.put(level, tip.below(), Blocks.COBWEB.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return true;
    }
}
