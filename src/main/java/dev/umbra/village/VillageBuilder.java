package dev.umbra.village;

import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;

/**
 * Shadow village layouts. Each returns the position of its bell (the bell rings by itself; see VillageManager).
 * <ul>
 *   <li>RING: a plaza with a dead well, huts in a wide circle, dead fields and a few graves.</li>
 *   <li>STREET: a long street with huts on both sides, lantern posts, the well in the middle, the bell at the end.</li>
 *   <li>HAMLET: scattered huts around a chapel with a bell tower and a graveyard.</li>
 * </ul>
 */
public final class VillageBuilder {
    public enum Layout { RING, STREET, HAMLET }

    private static final int FLAGS = Block.UPDATE_CLIENTS;

    private VillageBuilder() {}

    public static BlockPos build(WorldGenLevel level, BlockPos centre, RandomSource random, Layout layout) {
        BlockPos bell = switch (layout) {
            case RING -> ring(level, centre, random);
            case STREET -> street(level, centre, random);
            case HAMLET -> hamlet(level, centre, random);
        };
        scatter(level, centre, random, 26);
        int folk = 6 + random.nextInt(5);
        for (int i = 0; i < folk; i++) {
            ShadowVillager v = ModEntities.SHADOW_VILLAGER.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (v == null) continue;
            v.snapTo(centre.getX() + 0.5 + random.nextInt(9) - 4, centre.getY(), centre.getZ() + 0.5 + random.nextInt(9) - 4, random.nextFloat() * 360, 0);
            v.setPersistenceRequired();
            level.addFreshEntity(v);
        }
        return bell;
    }

    private static BlockPos floorNear(WorldGenLevel level, BlockPos at) {
        BlockPos f = ShadowVillageFeature.findFloor(level, at.above(6), at.getY() + 6, at.getY() - 8);
        if (f == null) f = ShadowVillageFeature.findFloor(level, at.above(12), at.getY() + 12, at.getY() - 30);
        return f == null ? at : f;
    }

    private static BlockPos ring(WorldGenLevel level, BlockPos c, RandomSource random) {
        ShadowVillageFeature.plaza(level, c, random);
        int huts = 6 + random.nextInt(2);
        double start = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < huts; i++) {
            double a = start + i * Math.PI * 2 / huts;
            int r = 15 + random.nextInt(5);
            BlockPos at = c.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r));
            ShadowVillageFeature.hut(level, floorNear(level, at), random, towards(at, c));
        }
        field(level, floorNear(level, c.offset(-6, 0, 22)), random);
        graves(level, floorNear(level, c.offset(20, 0, -20)), random, 4);
        return bell(level, c.offset(2, 0, -2));
    }

    private static BlockPos street(WorldGenLevel level, BlockPos c, RandomSource random) {
        boolean alongX = random.nextBoolean();
        Direction axis = alongX ? Direction.EAST : Direction.SOUTH;
        Direction side = axis.getClockWise();
        for (int t = -26; t <= 26; t++) {
            BlockPos road = floorNear(level, c.relative(axis, t));
            for (int w = -1; w <= 1; w++) {
                BlockPos p = road.relative(side, w);
                ShadowVillageFeature.foundation(level, p.below(), random.nextInt(4) == 0 ? ModBlocks.ASH_SOIL.get().defaultBlockState() : Blocks.COBBLED_DEEPSLATE.defaultBlockState());
                for (int y = 0; y < 4; y++) level.setBlock(p.above(y), Blocks.AIR.defaultBlockState(), FLAGS);
            }
            if (t % 8 == 0 && t != 0) {
                BlockPos post = road.relative(side, 2);
                for (int y = 0; y < 2; y++) level.setBlock(post.above(y), ModBlocks.DEAD_LOG.get().defaultBlockState(), FLAGS);
                level.setBlock(post.above(2), random.nextBoolean() ? dev.umbra.registry.ModBlocks.LUMINITE_LANTERN_BLOCK.get().defaultBlockState() : Blocks.AIR.defaultBlockState(), FLAGS);
            }
        }
        for (int t = -21; t <= 21; t += 9) {
            for (int s : new int[]{-1, 1}) {
                BlockPos at = c.relative(axis, t + random.nextInt(3) - 1).relative(side, s * 7);
                ShadowVillageFeature.hut(level, floorNear(level, at), random, s > 0 ? side.getOpposite() : side);
            }
        }
        well(level, floorNear(level, c));
        field(level, floorNear(level, c.relative(axis, 32)), random);
        return bell(level, floorNear(level, c.relative(axis, -29)));
    }

    private static BlockPos hamlet(WorldGenLevel level, BlockPos c, RandomSource random) {
        BlockPos bell = chapel(level, c, random);
        for (int i = 0; i < 5; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            int r = 14 + random.nextInt(9);
            BlockPos at = c.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r));
            ShadowVillageFeature.hut(level, floorNear(level, at), random, towards(at, c));
        }
        graves(level, floorNear(level, c.offset(0, 0, 10)), random, 8);
        well(level, floorNear(level, c.offset(-10, 0, 0)));
        return bell;
    }

    /** A tall chapel (9×11) with a bell tower over the door. Returns the bell. */
    private static BlockPos chapel(WorldGenLevel level, BlockPos c, RandomSource random) {
        BlockState wall = ModBlocks.ASH_STONE.get().defaultBlockState();
        BlockState log = ModBlocks.DEAD_LOG.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -4; x <= 4; x++)
            for (int z = -5; z <= 5; z++) {
                ShadowVillageFeature.foundation(level, c.offset(x, -1, z), Blocks.PALE_OAK_PLANKS.defaultBlockState());
                boolean edge = Math.abs(x) == 4 || Math.abs(z) == 5;
                boolean corner = Math.abs(x) == 4 && Math.abs(z) == 5;
                for (int y = 0; y <= 6; y++) {
                    BlockState s;
                    if (y == 6) s = random.nextInt(5) == 0 ? air : Blocks.PALE_OAK_PLANKS.defaultBlockState();
                    else if (corner) s = log;
                    else if (edge) s = (y == 2 || y == 3) && x % 2 == 0 && Math.abs(x) != 4 ? air : (random.nextInt(9) == 0 ? air : wall);
                    else s = air;
                    level.setBlock(c.offset(x, y, z), s, FLAGS);
                }
            }
        // door (south) and pews
        for (int y = 0; y <= 2; y++) for (int x = -1; x <= 1; x++) level.setBlock(c.offset(x, y, 5), air, FLAGS);
        for (int z = -2; z <= 3; z += 2)
            for (int x : new int[]{-2, -1, 1, 2}) level.setBlock(c.offset(x, 0, z), Blocks.PALE_OAK_SLAB.defaultBlockState(), FLAGS);
        level.setBlock(c.offset(0, 0, -4), Blocks.LECTERN.defaultBlockState(), FLAGS);
        BlockPos chest = c.offset(3, 0, -4);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), FLAGS);
        net.minecraft.world.RandomizableContainer.setBlockEntityLootTable(level, random, chest, ShadowVillageFeature.HUT_LOOT);
        candles(level, c.offset(-2, 0, -4), random);
        candles(level, c.offset(2, 0, -4), random);
        // the tower
        for (int y = 7; y <= 10; y++)
            for (int x = -1; x <= 1; x++)
                for (int z = 4; z <= 6; z++) {
                    boolean post = Math.abs(x) == 1 && (z == 4 || z == 6);
                    level.setBlock(c.offset(x, y, z), post || y == 10 ? log : air, FLAGS);
                }
        BlockPos bell = c.offset(0, 9, 5);
        level.setBlock(bell, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING), FLAGS);
        return bell;
    }

    private static BlockPos bell(WorldGenLevel level, BlockPos at) {
        BlockState log = ModBlocks.DEAD_LOG.get().defaultBlockState();
        level.setBlock(at.east(), log, FLAGS);
        level.setBlock(at.east().above(), log, FLAGS);
        level.setBlock(at.west(), log, FLAGS);
        level.setBlock(at.west().above(), log, FLAGS);
        level.setBlock(at.above(2).east(), Blocks.PALE_OAK_PLANKS.defaultBlockState(), FLAGS);
        level.setBlock(at.above(2), Blocks.PALE_OAK_PLANKS.defaultBlockState(), FLAGS);
        level.setBlock(at.above(2).west(), Blocks.PALE_OAK_PLANKS.defaultBlockState(), FLAGS);
        BlockPos bell = at.above();
        level.setBlock(bell, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING), FLAGS);
        level.setBlock(at, Blocks.AIR.defaultBlockState(), FLAGS);
        return bell;
    }

    private static void well(WorldGenLevel level, BlockPos c) {
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) {
                boolean rim = x != 0 || z != 0;
                level.setBlock(c.offset(x, 0, z), rim ? Blocks.COBBLED_DEEPSLATE_WALL.defaultBlockState() : Blocks.AIR.defaultBlockState(), FLAGS);
            }
        for (int y = -1; y >= -4; y--) level.setBlock(c.offset(0, y, 0), y == -4 ? ModBlocks.ASH_SOIL.get().defaultBlockState() : Blocks.AIR.defaultBlockState(), FLAGS);
    }

    /** A dead field: coarse dirt rows with dead bushes, a broken fence around it. */
    private static void field(WorldGenLevel level, BlockPos c, RandomSource random) {
        for (int x = -4; x <= 4; x++)
            for (int z = -3; z <= 3; z++) {
                BlockPos p = c.offset(x, 0, z);
                boolean edge = Math.abs(x) == 4 || Math.abs(z) == 3;
                ShadowVillageFeature.foundation(level, p.below(), Blocks.COARSE_DIRT.defaultBlockState());
                if (edge) level.setBlock(p, random.nextInt(3) == 0 ? Blocks.AIR.defaultBlockState() : Blocks.PALE_OAK_FENCE.defaultBlockState(), FLAGS);
                else level.setBlock(p, random.nextInt(3) == 0 ? Blocks.DEAD_BUSH.defaultBlockState() : Blocks.AIR.defaultBlockState(), FLAGS);
            }
    }

    private static void graves(WorldGenLevel level, BlockPos c, RandomSource random, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos g = floorNear(level, c.offset((i % 4) * 2 - 3, 0, (i / 4) * 3 - 1));
            level.setBlock(g, Blocks.COBBLED_DEEPSLATE_WALL.defaultBlockState(), FLAGS);
            if (random.nextBoolean()) level.setBlock(g.above(), Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + random.nextInt(3)), FLAGS);
            level.setBlock(g.south().below(), ModBlocks.ASH_SOIL.get().defaultBlockState(), FLAGS);
        }
    }

    private static void candles(WorldGenLevel level, BlockPos at, RandomSource random) {
        level.setBlock(at, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2 + random.nextInt(3)), FLAGS);
    }

    /** Ash piles, bones, crates and broken carts between the houses. */
    public static void scatter(WorldGenLevel level, BlockPos c, RandomSource random, int radius) {
        for (int i = 0; i < 18; i++) {
            BlockPos at = c.offset(random.nextInt(radius * 2 + 1) - radius, 0, random.nextInt(radius * 2 + 1) - radius);
            BlockPos f = ShadowVillageFeature.findFloor(level, at.above(5), at.getY() + 5, at.getY() - 6);
            if (f == null || !level.getBlockState(f).isAir()) continue;
            switch (random.nextInt(6)) {
                case 0 -> level.setBlock(f, ModBlocks.ASH_SOIL.get().defaultBlockState(), FLAGS);
                case 1 -> level.setBlock(f, Blocks.BONE_BLOCK.defaultBlockState(), FLAGS);
                case 2 -> {
                    level.setBlock(f, Blocks.BARREL.defaultBlockState(), FLAGS);
                    if (random.nextBoolean()) level.setBlock(f.above(), Blocks.BARREL.defaultBlockState(), FLAGS);
                }
                case 3 -> level.setBlock(f, Blocks.PALE_OAK_FENCE.defaultBlockState(), FLAGS);
                case 4 -> level.setBlock(f, Blocks.COBWEB.defaultBlockState(), FLAGS);
                default -> level.setBlock(f, Blocks.SKELETON_SKULL.defaultBlockState(), FLAGS);
            }
        }
    }

    private static Direction towards(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
