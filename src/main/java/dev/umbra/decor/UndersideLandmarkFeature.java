package dev.umbra.decor;

import com.mojang.serialization.MapCodec;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Rarer sights of the Underside: a dead explorer's camp (with a note), a broken rift frame
 * somebody tried to escape through, and figures of people made of ash.
 */
public record UndersideLandmarkFeature() implements Feature {
    public static final MapCodec<UndersideLandmarkFeature> CODEC = MapCodec.unit(new UndersideLandmarkFeature());
    public static final ResourceKey<LootTable> CAMP_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, "chests/explorer_camp"));
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    @Override
    public MapCodec<UndersideLandmarkFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos f = UndersideDecorFeature.floor(level, origin, 50);
        if (f == null) return false;
        int kind = random.nextInt(5);
        if (kind <= 1) camp(level, f, random);
        else if (kind == 2) brokenRift(level, f, random);
        else figures(level, f, random);
        return true;
    }

    public static void camp(WorldGenLevel level, BlockPos f, RandomSource random) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -3; x <= 3; x++)
            for (int z = -3; z <= 3; z++) {
                dev.umbra.village.ShadowVillageFeature.foundation(level, f.offset(x, -1, z), ModBlocks.ASH_SOIL.get().defaultBlockState());
                for (int y = 0; y <= 3; y++) {
                    BlockPos p = f.offset(x, y, z);
                    // another camp's chest or fire next door: leave it (clearing it at generation leaves its data behind)
                    if (!level.getBlockState(p).hasBlockEntity()) level.setBlock(p, air, FLAGS);
                }
            }
        level.setBlock(f, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false), FLAGS);
        // a little A-frame tent of grey wool to the north
        for (int z = -3; z <= -1; z++) {
            level.setBlock(f.offset(-1, 0, z), vanilla("gray_wool"), FLAGS);
            level.setBlock(f.offset(1, 0, z), vanilla("gray_wool"), FLAGS);
            level.setBlock(f.offset(0, 1, z), vanilla("gray_wool"), FLAGS);
            level.setBlock(f.offset(0, 0, z), vanilla("gray_carpet"), FLAGS);
        }
        BlockPos chest = f.offset(2, 0, -2);
        if (!level.getBlockState(chest).hasBlockEntity()) {
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), FLAGS);
            RandomizableContainer.setBlockEntityLootTable(level, random, chest, CAMP_LOOT);
        }
        // what is left of the explorer
        level.setBlock(f.offset(-2, 0, 2), Blocks.SKELETON_SKULL.defaultBlockState(), FLAGS);
        level.setBlock(f.offset(-1, 0, 2), Blocks.BONE_BLOCK.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X), FLAGS);
        level.setBlock(f.offset(2, 0, 2), dev.umbra.registry.ModBlocks.LUMINITE_LANTERN_BLOCK.get().defaultBlockState(), FLAGS);
    }

    private static BlockState vanilla(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id)).defaultBlockState();
    }

    public static void brokenRift(WorldGenLevel level, BlockPos f, RandomSource random) {
        BlockState dusk = ModBlocks.DUSK_STONE.get().defaultBlockState();
        for (int x = -2; x <= 2; x++)
            for (int y = 0; y <= 5; y++) {
                boolean frame = Math.abs(x) == 2 || y == 0 || y == 5;
                if (frame && random.nextInt(3) != 0) level.setBlock(f.offset(x, y, 0), dusk, FLAGS);
            }
        for (int i = 0; i < 4; i++) {
            BlockPos p = UndersideDecorFeature.floor(level, f.offset(random.nextInt(7) - 3, 3, random.nextInt(5) + 1), 6);
            if (p != null) level.setBlock(p, dusk, FLAGS);
        }
    }

    public static void figures(WorldGenLevel level, BlockPos f, RandomSource random) {
        int n = 1 + random.nextInt(3);
        for (int i = 0; i < n; i++) {
            BlockPos p = UndersideDecorFeature.floor(level, f.offset(random.nextInt(9) - 4, 3, random.nextInt(9) - 4), 8);
            if (p == null) continue;
            AshFigure fig = ModEntities.ASH_FIGURE.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (fig == null) continue;
            fig.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360, 0);
            level.addFreshEntity(fig);
        }
    }
}
