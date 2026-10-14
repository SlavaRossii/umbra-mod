package dev.umbra.decor;

import com.mojang.serialization.MapCodec;
import dev.umbra.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Crystal grottos: luminite spires grow from the floor and hang from the ceiling, ringed with small crystals. */
public record CrystalGrottoFeature() implements Feature {
    public static final MapCodec<CrystalGrottoFeature> CODEC = MapCodec.unit(new CrystalGrottoFeature());
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    @Override
    public MapCodec<CrystalGrottoFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!level.getBlockState(origin).isAir()) return false;
        if (random.nextInt(3) == 0) {
            BlockPos ceiling = origin;
            for (int i = 0; i < 40 && level.getBlockState(ceiling.above()).isAir(); i++) ceiling = ceiling.above();
            if (!level.getBlockState(ceiling.above()).isSolidRender()) return false;
            spire(level, ceiling, Direction.DOWN, 3 + random.nextInt(6), random);
            return true;
        }
        BlockPos f = UndersideDecorFeature.floor(level, origin, 40);
        if (f == null) return false;
        spire(level, f, Direction.UP, 4 + random.nextInt(8), random);
        return true;
    }

    /** A tapering spire of luminite growing in {@code dir} from {@code base}, with clusters around its foot. */
    public static void spire(WorldGenLevel level, BlockPos base, Direction dir, int height, RandomSource random) {
        BlockState block = ModBlocks.LUMINITE_BLOCK.get().defaultBlockState();
        int radius = height > 7 ? 1 : 0;
        int lean = random.nextInt(3) - 1, leanZ = random.nextInt(3) - 1;
        for (int i = 0; i < height; i++) {
            int r = i < height / 2 ? radius : 0;
            int ox = lean * i / 4, oz = leanZ * i / 4;
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > r) continue;
                    BlockPos p = base.relative(dir, i).offset(ox + dx, 0, oz + dz);
                    if (level.getBlockState(p).isAir()) level.setBlock(p, block, FLAGS);
                }
        }
        BlockPos tip = base.relative(dir, height).offset(lean * height / 4, 0, leanZ * height / 4);
        if (level.getBlockState(tip).isAir()) level.setBlock(tip, cluster(dir), FLAGS);
        // small crystals around the foot
        for (int i = 0; i < 4 + random.nextInt(5); i++) {
            BlockPos p = base.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            BlockPos support = p.relative(dir.getOpposite());
            if (level.getBlockState(p).isAir() && level.getBlockState(support).isSolidRender()) level.setBlock(p, cluster(dir), FLAGS);
        }
    }

    private static BlockState cluster(Direction facing) {
        return ModBlocks.LUMINITE_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, facing);
    }
}
