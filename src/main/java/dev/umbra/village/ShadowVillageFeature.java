package dev.umbra.village;

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
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * An abandoned shadow village in the Underside: a plaza with a dead well and soul lanterns on posts,
 * a ring of ruined huts, and the shadow folk who still live there.
 * One per {@link #CELL}×{@link #CELL}-chunk cell at most (the same scheme as the mazes).
 * Everything is placed within 20 blocks of the chunk centre so it stays inside the writable 3×3 chunk area.
 */
public record ShadowVillageFeature() implements Feature {
    public static final MapCodec<ShadowVillageFeature> CODEC = MapCodec.unit(new ShadowVillageFeature());
    public static final int CELL = 12;
    public static final int SPREAD = 6;
    public static final ResourceKey<LootTable> HUT_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, "chests/shadow_village"));
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    @Override
    public MapCodec<ShadowVillageFeature> codec() {
        return CODEC;
    }

    public static int[] chosenChunkOf(long seed, int gx, int gz) {
        RandomSource r = RandomSource.create(seed ^ (gx * 198491317L + gz * 6542989L) ^ 0x2545F4914F6CDD1DL);
        return new int[]{gx * CELL + r.nextInt(SPREAD), gz * CELL + r.nextInt(SPREAD)};
    }

    public static boolean chosenChunk(long seed, int cx, int cz) {
        int[] c = chosenChunkOf(seed, Math.floorDiv(cx, CELL), Math.floorDiv(cz, CELL));
        return c[0] == cx && c[1] == cz;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // since 1.4.1 villages are built at runtime by VillageManager (bigger than a feature may write)
        return false;
    }

    /** Highest spot below {@code from} with solid ground and four blocks of air above it. */
    public static BlockPos findFloor(WorldGenLevel level, BlockPos from, int top, int bottom) {
        for (int y = top; y > bottom; y--) {
            BlockPos p = new BlockPos(from.getX(), y, from.getZ());
            BlockState below = level.getBlockState(p.below());
            if (below.isAir() || !below.getFluidState().isEmpty() || below.canBeReplaced()) continue;
            boolean room = true;
            for (int dy = 0; dy < 4 && room; dy++) room = level.getBlockState(p.above(dy)).isAir();
            if (room) return p;
        }
        return null;
    }

    /**
     * The real ground for a village in this column: of all the floors between {@code top} and {@code bottom}, the one
     * with the widest flat footing round it (a ledge or the top of a pillar has none), and of equals the one nearest
     * {@code near}. Before 2.5 the highest floor won, and villages sat on ledges high up the cave walls.
     */
    public static BlockPos groundFloor(WorldGenLevel level, BlockPos column, int top, int bottom, int near) {
        BlockPos best = null;
        int bestScore = -1;
        for (int y = top; y > bottom; y--) {
            BlockPos p = new BlockPos(column.getX(), y, column.getZ());
            BlockState below = level.getBlockState(p.below());
            if (below.isAir() || !below.getFluidState().isEmpty() || below.canBeReplaced()) continue;
            boolean room = true;
            for (int dy = 0; dy < 4 && room; dy++) room = level.getBlockState(p.above(dy)).isAir();
            if (!room) continue;
            int support = 0;
            for (int dx = -10; dx <= 10; dx += 2)
                for (int dz = -10; dz <= 10; dz += 2) {
                    for (int dy = -2; dy <= 2; dy++) {       // ground within two blocks up or down counts as the same floor
                        BlockPos q = p.offset(dx, dy, dz);
                        if (!level.getBlockState(q.below()).isAir() && level.getBlockState(q).isAir() && level.getBlockState(q.above()).isAir()) {
                            support++;
                            break;
                        }
                    }
                }
            int score = support * 64 - Math.min(63, Math.abs(y - near));
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
            y -= 3;                                         // the same floor is not counted again just below
        }
        return best;
    }

    public static void build(WorldGenLevel level, BlockPos centre, RandomSource random) {
        plaza(level, centre, random);
        int huts = 4 + random.nextInt(2);
        double start = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < huts; i++) {
            double a = start + i * Math.PI * 2 / huts + (random.nextDouble() - 0.5) * 0.4;
            int r = 11 + random.nextInt(3);
            BlockPos at = centre.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r));
            BlockPos floor = findFloor(level, at.above(6), at.getY() + 6, at.getY() - 8);
            if (floor == null) floor = at;
            hut(level, floor, random, Direction.fromYRot(Math.toDegrees(a) + 90));
        }
        int folk = 3 + random.nextInt(3);
        for (int i = 0; i < folk; i++) {
            ShadowVillager v = ModEntities.SHADOW_VILLAGER.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (v == null) continue;
            v.snapTo(centre.getX() + 0.5 + random.nextInt(7) - 3, centre.getY(), centre.getZ() + 0.5 + random.nextInt(7) - 3, random.nextFloat() * 360, 0);
            v.setPersistenceRequired();
            level.addFreshEntity(v);
        }
    }

    public static void plaza(WorldGenLevel level, BlockPos c, RandomSource random) {
        BlockState ash = ModBlocks.ASH_STONE.get().defaultBlockState();
        for (int x = -5; x <= 5; x++)
            for (int z = -5; z <= 5; z++) {
                if (x * x + z * z > 30) continue;
                foundation(level, c.offset(x, -1, z), random.nextInt(4) == 0 ? ModBlocks.ASH_SOIL.get().defaultBlockState() : Blocks.COBBLED_DEEPSLATE.defaultBlockState());
                for (int y = 0; y < 4; y++) level.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), FLAGS);
            }
        // the dead well
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) {
                boolean rim = x != 0 || z != 0;
                level.setBlock(c.offset(x, 0, z), rim ? Blocks.COBBLED_DEEPSLATE_WALL.defaultBlockState() : Blocks.AIR.defaultBlockState(), FLAGS);
                if (!rim) for (int y = -1; y >= -4; y--) level.setBlock(c.offset(0, y, 0), y == -4 ? ModBlocks.ASH_SOIL.get().defaultBlockState() : Blocks.AIR.defaultBlockState(), FLAGS);
            }
        // lantern posts
        BlockState lantern = dev.umbra.registry.ModBlocks.LUMINITE_LANTERN_BLOCK.get().defaultBlockState().setValue(LanternBlock.HANGING, false);
        int[][] posts = {{4, 0}, {-4, 0}, {0, 4}, {0, -4}};
        for (int[] p : posts) {
            BlockPos base = c.offset(p[0], 0, p[1]);
            for (int y = 0; y < 2; y++) level.setBlock(base.above(y), ModBlocks.DEAD_LOG.get().defaultBlockState(), FLAGS);
            // half of them went out long ago
            level.setBlock(base.above(2), random.nextBoolean() ? lantern : Blocks.AIR.defaultBlockState(), FLAGS);
        }
    }

    /** A ruined 7×7 hut facing {@code door}. */
    public static void hut(WorldGenLevel level, BlockPos floor, RandomSource random, Direction door) {
        BlockState log = ModBlocks.DEAD_LOG.get().defaultBlockState();
        BlockState wall = ModBlocks.ASH_STONE.get().defaultBlockState();
        BlockState planks = Blocks.PALE_OAK_PLANKS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -3; x <= 3; x++)
            for (int z = -3; z <= 3; z++) {
                foundation(level, floor.offset(x, -1, z), random.nextInt(6) == 0 ? ModBlocks.ASH_SOIL.get().defaultBlockState() : planks);
                boolean edgeX = Math.abs(x) == 3, edgeZ = Math.abs(z) == 3;
                for (int y = 0; y <= 3; y++) {
                    BlockPos p = floor.offset(x, y, z);
                    BlockState s;
                    if (y == 3) s = random.nextInt(4) == 0 ? air : planks;          // roof with holes
                    else if (edgeX && edgeZ) s = log;                                // corner posts
                    else if (edgeX || edgeZ) s = random.nextInt(5) == 0 ? air : wall; // crumbling walls
                    else s = air;
                    level.setBlock(p, s, FLAGS);
                }
            }
        // door and windows
        BlockPos d = floor.relative(door, 3);
        level.setBlock(d, air, FLAGS);
        level.setBlock(d.above(), air, FLAGS);
        Direction side = door.getClockWise();
        level.setBlock(floor.relative(side, 3).above(), air, FLAGS);
        level.setBlock(floor.relative(side.getOpposite(), 3).above(), air, FLAGS);
        // inside: cobwebs, a lantern that went out, maybe a chest
        Direction back = door.getOpposite();
        BlockPos corner = floor.relative(back, 2).relative(side, 2);
        if (random.nextInt(3) != 0) {
            level.setBlock(corner, Blocks.CHEST.defaultBlockState(), FLAGS);
            RandomizableContainer.setBlockEntityLootTable(level, random, corner, HUT_LOOT);
        } else {
            level.setBlock(corner, Blocks.BARREL.defaultBlockState(), FLAGS);
        }
        level.setBlock(floor.relative(back, 2).relative(side.getOpposite(), 2), dev.umbra.registry.ModBlocks.LUMINITE_LANTERN_BLOCK.get().defaultBlockState().setValue(LanternBlock.HANGING, false), FLAGS);
        for (int i = 0; i < 3; i++) {
            BlockPos web = floor.offset(random.nextInt(5) - 2, 2, random.nextInt(5) - 2);
            if (level.getBlockState(web).isAir()) level.setBlock(web, Blocks.COBWEB.defaultBlockState(), FLAGS);
        }
    }

    /** Places {@code top} and props it up with ash stone down to the ground (at most 6 blocks). */
    public static void foundation(WorldGenLevel level, BlockPos top, BlockState state) {
        level.setBlock(top, state, FLAGS);
        BlockState ash = ModBlocks.ASH_STONE.get().defaultBlockState();
        for (int i = 1; i <= 24; i++) {
            BlockPos p = top.below(i);
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced()) break;
            level.setBlock(p, ash, FLAGS);
        }
    }
}
