package dev.umbra.lighthouse;

import com.mojang.serialization.MapCodec;
import dev.umbra.citadel.CitadelGateFeature;
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
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The great lighthouse: a stone tower about sixty blocks tall standing on a rock platform, with a dead lamp in a glass
 * room on top, a gallery around it, a keeper's house at its foot and a wooden dock for the black water.
 * One per {@link #CELL}x{@link #CELL}-chunk cell, in a chunk picked by the world seed.
 * Everything lies within 13 blocks of the centre of the chosen chunk (the limit for a feature).
 */
public record LighthouseFeature() implements Feature {
    public static final MapCodec<LighthouseFeature> CODEC = MapCodec.unit(new LighthouseFeature());
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, "chests/lighthouse_house"));
    public static final int CELL = 32;
    public static final int SPREAD = 10;
    /** Height of the lamp above the platform floor. */
    public static final int LAMP_Y = 56;
    /** Where the keeper's house door and the keeper stand, relative to the tower axis. */
    public static final int HOUSE_Z = 8;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    @Override
    public MapCodec<LighthouseFeature> codec() {
        return CODEC;
    }

    public static int[] chosenChunkOf(long seed, int gx, int gz) {
        RandomSource r = RandomSource.create(seed ^ (gx * 341873128712L + gz * 132897987541L) ^ 0x3C6EF372FE94F82BL);
        // never on top of (or next to) a citadel gate: draw again until the neighbourhood is free
        int x = 0, z = 0;
        for (int attempt = 0; attempt < 32; attempt++) {
            x = gx * CELL + r.nextInt(SPREAD);
            z = gz * CELL + r.nextInt(SPREAD);
            boolean clash = false;
            for (int dx = -1; dx <= 1 && !clash; dx++)
                for (int dz = -1; dz <= 1 && !clash; dz++) clash = CitadelGateFeature.chosenChunk(seed, x + dx, z + dz);
            if (!clash) break;
        }
        return new int[]{x, z};
    }

    public static boolean chosenChunk(long seed, int cx, int cz) {
        int[] c = chosenChunkOf(seed, Math.floorDiv(cx, CELL), Math.floorDiv(cz, CELL));
        return c[0] == cx && c[1] == cz;
    }

    /** The chunk of the lighthouse nearest to (x, z). */
    public static int[] nearest(long seed, int x, int z) {
        int gx = Math.floorDiv(x >> 4, CELL), gz = Math.floorDiv(z >> 4, CELL);
        int[] best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                int[] c = chosenChunkOf(seed, gx + dx, gz + dz);
                double d = Math.pow(c[0] * 16 + 8 - x, 2) + Math.pow(c[1] * 16 + 8 - z, 2);
                if (d < bestD) {
                    bestD = d;
                    best = c;
                }
            }
        return best;
    }

    /** "~N blocks to the north-east" towards the nearest lighthouse. */
    public static net.minecraft.network.chat.Component describeNearest(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.player.Player player, String key) {
        int[] c = nearest(level.getSeed(), (int) player.getX(), (int) player.getZ());
        double dx = c[0] * 16 + 8 - player.getX(), dz = c[1] * 16 + 8 - player.getZ();
        int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        String[] dirs = {"s", "sw", "w", "nw", "n", "ne", "e", "se"};
        String dir = dirs[(int) Math.floorMod(Math.round(yaw / 45.0), 8)];
        return net.minecraft.network.chat.Component.translatable(key, dist,
                net.minecraft.network.chat.Component.translatable("direction.umbra." + dir));
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int cx = origin.getX() >> 4, cz = origin.getZ() >> 4;
        if (!chosenChunk(level.getSeed(), cx, cz)) return false;
        build(level, CitadelGateFeature.findSolidGround(level, cx * 16 + 8, cz * 16 + 8), random);
        return true;
    }

    /** True if x, z (relative to the axis) lie inside the circle of the given radius. */
    private static boolean in(int x, int z, double r) {
        return x * x + z * z <= r * r + 0.5;
    }

    /** @param floor the platform floor block on the tower axis */
    public static void build(WorldGenLevel level, BlockPos floor, RandomSource random) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState band = Blocks.POLISHED_DIORITE.defaultBlockState();
        BlockState dusk = ModBlocks.DUSK_STONE.get().defaultBlockState();
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState ash = ModBlocks.ASH_STONE.get().defaultBlockState();

        // 1. the rock the lighthouse stands on: a platform 17 wide and 21 long, propped up to the ground
        for (int x = -8; x <= 8; x++)
            for (int z = -8; z <= 12; z++) {
                for (int y = 1; y <= LAMP_Y + 12; y++) {
                    level.setBlock(floor.offset(x, y, z), air, FLAGS);
                }
                BlockState top = random.nextInt(6) == 0 ? mossy : random.nextInt(3) == 0 ? ash : dusk;
                dev.umbra.village.ShadowVillageFeature.foundation(level, floor.offset(x, 0, z), top);
            }
        // carve a clear shaft for the tower so the cave ceiling does not cut it off
        for (int x = -7; x <= 7; x++)
            for (int z = -7; z <= 7; z++) {
                if (!in(x, z, 7)) continue;
                for (int y = 1; y <= LAMP_Y + 8; y++) level.setBlock(floor.offset(x, y, z), air, FLAGS);
            }

        // 2. the tower: a ring of radius 4, hollow inside, banded every 8 layers
        for (int y = 1; y <= LAMP_Y - 2; y++) {
            for (int x = -4; x <= 4; x++)
                for (int z = -4; z <= 4; z++) {
                    boolean ring = in(x, z, 4) && !in(x, z, 3);
                    if (!ring) continue;
                    BlockState s = (y % 8 == 0) ? band : (random.nextInt(14) == 0 ? mossy : bricks);
                    level.setBlock(floor.offset(x, y, z), s, FLAGS);
                }
            // little windows
            if (y % 10 == 5) {
                level.setBlock(floor.offset(4, y, 0), glass, FLAGS);
                level.setBlock(floor.offset(-4, y, 0), glass, FLAGS);
            }
        }
        // the door, facing the house (south, +z)
        for (int y = 1; y <= 2; y++) level.setBlock(floor.offset(0, y, 4), air, FLAGS);
        level.setBlock(floor.offset(0, 1, 3), air, FLAGS);
        // the ladder up the north wall, with a landing every 12 blocks
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        for (int y = 1; y <= LAMP_Y - 1; y++) level.setBlock(floor.offset(0, y, -3), ladder, FLAGS);
        for (int y = 12; y < LAMP_Y - 2; y += 12) {
            for (int x = -3; x <= 3; x++)
                for (int z = -3; z <= 3; z++) {
                    if (!in(x, z, 3) || (x == 0 && z == -3)) continue;
                    level.setBlock(floor.offset(x, y, z), planks, FLAGS);
                }
            level.setBlock(floor.offset(0, y + 1, 2), Blocks.LANTERN.defaultBlockState(), FLAGS);
        }

        // 3. the gallery: a disc of radius 6 at the top of the tower, with a railing
        int gy = LAMP_Y - 1;
        for (int x = -6; x <= 6; x++)
            for (int z = -6; z <= 6; z++) {
                if (!in(x, z, 6) || (x == 0 && z == -3)) continue;
                level.setBlock(floor.offset(x, gy, z), bricks, FLAGS);
                if (in(x, z, 6) && !in(x, z, 5)) level.setBlock(floor.offset(x, gy + 1, z), Blocks.IRON_BARS.defaultBlockState(), FLAGS);
            }
        // 4. the lamp room: a square of glass around the lamp, a door to the gallery on the south side
        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++) {
                boolean wall = Math.abs(x) == 4 || Math.abs(z) == 4;
                for (int y = 0; y <= 3; y++) {
                    BlockPos p = floor.offset(x, LAMP_Y + y, z);
                    if (wall) level.setBlock(p, (y == 0 && (Math.abs(x) == 4 && Math.abs(z) == 4)) ? bricks : glass, FLAGS);
                    else level.setBlock(p, air, FLAGS);
                }
                level.setBlock(floor.offset(x, LAMP_Y + 4, z), bricks, FLAGS);
                if (Math.abs(x) <= 3 && Math.abs(z) <= 3) level.setBlock(floor.offset(x, LAMP_Y + 5, z), bricks, FLAGS);
                if (Math.abs(x) <= 1 && Math.abs(z) <= 1) level.setBlock(floor.offset(x, LAMP_Y + 6, z), bricks, FLAGS);
            }
        for (int y = 0; y <= 1; y++) level.setBlock(floor.offset(0, LAMP_Y + y, 4), air, FLAGS);
        level.setBlock(floor.offset(0, LAMP_Y + 7, 0), Blocks.LIGHTNING_ROD.defaultBlockState(), FLAGS);
        // the lamp itself, on a little plinth in the middle; the ladder comes out beside it
        level.setBlock(floor.offset(0, LAMP_Y - 1 + 0, 0), bricks, FLAGS);
        BlockPos lamp = floor.offset(0, LAMP_Y, 0);
        level.setBlock(lamp, ModBlocks.LIGHTHOUSE_LAMP.get().defaultBlockState(), FLAGS);
        level.setBlock(floor.offset(0, LAMP_Y - 1, -3), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH), FLAGS);
        level.setBlock(floor.offset(0, LAMP_Y, -3), ladder, FLAGS);
        level.setBlock(floor.offset(0, LAMP_Y + 1, -3), ladder, FLAGS);
        level.setBlock(floor.offset(-4, LAMP_Y + 1, 0), Blocks.SOUL_LANTERN.defaultBlockState(), FLAGS);
        // the ladder needs something solid behind it
        for (int y = 0; y <= 1; y++) level.setBlock(floor.offset(0, LAMP_Y + y, -4), bricks, FLAGS);

        // 5. the keeper's house on the south of the platform: 7 wide, 5 deep, door to the tower
        house(level, floor.offset(-3, 1, 6), random);
        // 6. a wooden dock reaching out west, where the black water would lap
        for (int x = -14; x <= -9; x++)
            for (int z = 0; z <= 1; z++) {
                level.setBlock(floor.offset(x, 0, z), planks, FLAGS);
                if (x % 3 == 0) level.setBlock(floor.offset(x, 1, z), Blocks.SPRUCE_FENCE.defaultBlockState(), FLAGS);
            }
        level.setBlock(floor.offset(-14, 1, 0), Blocks.LANTERN.defaultBlockState(), FLAGS);

        // 7. the keeper himself
        LampKeeper keeper = ModEntities.LAMP_KEEPER.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (keeper != null) {
            keeper.snapTo(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + HOUSE_Z + 0.5, 0, 0);
            keeper.setPersistenceRequired();
            level.addFreshEntity(keeper);
        }
    }

    /** @param c minimum corner, one above the platform. */
    private static void house(WorldGenLevel level, BlockPos c, RandomSource random) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = 0; x < 7; x++)
            for (int z = 0; z < 5; z++) {
                boolean edge = x == 0 || z == 0 || x == 6 || z == 4;
                boolean corner = (x == 0 || x == 6) && (z == 0 || z == 4);
                for (int y = 0; y <= 3; y++) {
                    BlockPos p = c.offset(x, y, z);
                    if (corner) level.setBlock(p, log, FLAGS);
                    else if (edge && y <= 2) level.setBlock(p, planks, FLAGS);
                    else if (y == 3) level.setBlock(p, bricks, FLAGS);
                    else level.setBlock(p, air, FLAGS);
                }
                level.setBlock(c.offset(x, 4, z), Blocks.SPRUCE_SLAB.defaultBlockState(), FLAGS);
            }
        // door on the north wall (towards the tower), a window on the south
        for (int y = 0; y <= 1; y++) level.setBlock(c.offset(3, y, 0), air, FLAGS);
        level.setBlock(c.offset(3, 1, 4), Blocks.GLASS.defaultBlockState(), FLAGS);
        level.setBlock(c.offset(1, 1, 1), Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.SOUTH)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT), FLAGS);
        level.setBlock(c.offset(1, 1, 2), Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.SOUTH)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD), FLAGS);
        level.setBlock(c.offset(5, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), FLAGS);
        level.setBlock(c.offset(5, 1, 3), Blocks.FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.FurnaceBlock.FACING, Direction.WEST), FLAGS);
        level.setBlock(c.offset(3, 2, 2), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), FLAGS);
        level.setBlock(c.offset(3, 3, 2), planks, FLAGS);
        BlockPos chest = c.offset(5, 1, 2);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST), FLAGS);
        RandomizableContainer.setBlockEntityLootTable(level, random, chest, LOOT);
    }
}
