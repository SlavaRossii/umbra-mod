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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The lighthouse of the Underside: one per world, the only one, with Yan the keeper. It stands a few hundred blocks
 * from the middle of the world in a hollow it carves for itself (the Underside is a cave: a tower must fit under its
 * roof), on a rock platform, with a spiral stair up to a dead lamp in a glass room, a gallery round it, the keeper's
 * house at its foot and a wooden dock. Everything lies within 15 blocks of the centre of its chunk.
 */
public record LighthouseFeature() implements Feature {
    public static final MapCodec<LighthouseFeature> CODEC = MapCodec.unit(new LighthouseFeature());
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, "chests/lighthouse_house"));
    /** Height of the lamp above the platform floor. */
    public static final int LAMP_Y = 28;
    /** Radius of the hollow round the tower. */
    public static final int HOLLOW = 15;
    /** Where the keeper's house door and the keeper stand, relative to the tower axis. */
    public static final int HOUSE_Z = 8;
    /** Platform floor limits: the whole thing must fit between the cave's floor and its roof (128). */
    public static final int MIN_FLOOR = 14, MAX_FLOOR = 128 - LAMP_Y - 14;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    @Override
    public MapCodec<LighthouseFeature> codec() {
        return CODEC;
    }

    /** The one chunk of the lighthouse, picked by the seed: 250 to 450 blocks from the middle, away from citadel gates. */
    public static int[] site(long seed) {
        RandomSource r = RandomSource.create(seed ^ 0x3C6EF372FE94F82BL);
        int x = 20, z = 0;
        for (int attempt = 0; attempt < 64; attempt++) {
            double a = r.nextDouble() * Math.PI * 2, d = 250 + r.nextInt(200);
            x = ((int) Math.floor(Math.cos(a) * d)) >> 4;
            z = ((int) Math.floor(Math.sin(a) * d)) >> 4;
            boolean clash = false;
            for (int dx = -2; dx <= 2 && !clash; dx++)
                for (int dz = -2; dz <= 2 && !clash; dz++) clash = CitadelGateFeature.chosenChunk(seed, x + dx, z + dz);
            if (!clash) break;
        }
        return new int[]{x, z};
    }

    /** Kept for callers that think in cells: there is one lighthouse, whatever the cell. */
    public static int[] chosenChunkOf(long seed, int gx, int gz) {
        return site(seed);
    }

    public static boolean chosenChunk(long seed, int cx, int cz) {
        int[] c = site(seed);
        return c[0] == cx && c[1] == cz;
    }

    /** The chunk of the lighthouse (there is only one). */
    public static int[] nearest(long seed, int x, int z) {
        return site(seed);
    }

    /** "~N blocks to the north-east" towards the lighthouse. */
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
        build(level, floorAt(level, cx * 16 + 8, cz * 16 + 8), random);
        return true;
    }

    /** A cave floor for the platform, kept between the cave's floor and roof (the hollow is carved round it anyway). */
    public static BlockPos floorAt(WorldGenLevel level, int x, int z) {
        BlockPos g = CitadelGateFeature.findSolidGround(level, x, z);
        int y = g == null ? 48 : g.getY() - 1;
        y = Math.max(MIN_FLOOR, Math.min(MAX_FLOOR, y));
        return new BlockPos(x, y, z);
    }

    private static BlockState vanilla(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id)).defaultBlockState();
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

        BlockState lampLight = ModBlocks.LUMINITE_LANTERN_BLOCK.get().defaultBlockState();
        BlockState wallLight = ModBlocks.LUMINITE_LAMP.get().defaultBlockState();
        BlockState slabB = Blocks.STONE_BRICK_SLAB.defaultBlockState();
        BlockState slabT = Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,
                net.minecraft.world.level.block.state.properties.SlabType.TOP);

        // 0. the hollow: the Underside is a cave, so the lighthouse makes room for itself (a dome over the platform)
        for (int x = -HOLLOW; x <= HOLLOW; x++)
            for (int z = -HOLLOW; z <= HOLLOW; z++) {
                double d = Math.sqrt(x * x + (z - 2) * (z - 2));
                if (d > HOLLOW) continue;
                int top = LAMP_Y + 12 - (int) Math.round(Math.pow(d / HOLLOW, 2) * 14);
                for (int y = 1; y <= top; y++) level.setBlock(floor.offset(x, y, z), air, FLAGS);
            }

        // 1. the rock the lighthouse stands on: a platform 17 wide and 21 long, propped up to the ground
        for (int x = -8; x <= 8; x++)
            for (int z = -8; z <= 12; z++) {
                BlockState top = random.nextInt(6) == 0 ? mossy : random.nextInt(3) == 0 ? ash : dusk;
                dev.umbra.village.ShadowVillageFeature.foundation(level, floor.offset(x, 0, z), top);
            }

        // 2. the tower: a ring of radius 4, banded every 8 layers, a solid newel in the middle
        for (int y = 1; y <= LAMP_Y - 2; y++) {
            for (int x = -4; x <= 4; x++)
                for (int z = -4; z <= 4; z++) {
                    BlockPos p = floor.offset(x, y, z);
                    if (in(x, z, 4) && !in(x, z, 3)) level.setBlock(p, (y % 8 == 0) ? band : (random.nextInt(14) == 0 ? mossy : bricks), FLAGS);
                    else if (Math.abs(x) <= 1 && Math.abs(z) <= 1) level.setBlock(p, x == 0 && z == 0 ? band : bricks, FLAGS);
                    else if (in(x, z, 3)) level.setBlock(p, air, FLAGS);
                }
            if (y % 9 == 5) {
                level.setBlock(floor.offset(4, y, 0), glass, FLAGS);
                level.setBlock(floor.offset(-4, y, 0), glass, FLAGS);
                level.setBlock(floor.offset(0, y, -4), glass, FLAGS);
            }
            // luminite set into the wall: the stair is never dark, and nothing comes to eat the light
            if (y % 9 == 2) level.setBlock(floor.offset(y % 2 == 0 ? 3 : -3, y, y % 2 == 0 ? -2 : 2), wallLight, FLAGS);
        }
        // the door, facing the house (south, +z)
        for (int y = 1; y <= 2; y++) level.setBlock(floor.offset(0, y, 4), air, FLAGS);

        // 3. the gallery: a disc of radius 6 round the lamp room, with a railing
        int gy = LAMP_Y - 1;
        for (int x = -6; x <= 6; x++)
            for (int z = -6; z <= 6; z++) {
                if (!in(x, z, 6)) continue;
                level.setBlock(floor.offset(x, gy, z), bricks, FLAGS);
                if (!in(x, z, 5)) level.setBlock(floor.offset(x, gy + 1, z), Blocks.IRON_BARS.defaultBlockState(), FLAGS);
            }

        // the spiral stair round the newel: half a block up for every sixteenth of a turn, starting by the door;
        // where its last turn comes up, the floor above is left open
        for (int x = -3; x <= 3; x++)
            for (int z = -3; z <= 3; z++) {
                if (!in(x, z, 3) || (Math.abs(x) <= 1 && Math.abs(z) <= 1)) continue;
                double a = Math.atan2(x, z);                         // 0 at the door (south), growing towards the east
                int bucket = (int) Math.floor(((a + Math.PI * 2) % (Math.PI * 2)) / (Math.PI * 2) * 16);
                int topY = 0;
                for (int turn = 0; turn < 16; turn++) {
                    int half = turn * 16 + bucket + 1;                // in half blocks above the floor
                    int y = (half + 1) / 2;
                    if (y >= gy) break;
                    level.setBlock(floor.offset(x, y, z), half % 2 == 1 ? slabB : slabT, FLAGS);
                    topY = y;
                }
                if (topY >= gy - 4) level.setBlock(floor.offset(x, gy, z), air, FLAGS);
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
        level.setBlock(floor.offset(0, LAMP_Y + 7, 0), Blocks.END_ROD.defaultBlockState(), FLAGS);
        // the lamp on top of the newel
        level.setBlock(floor.offset(0, LAMP_Y - 1, 0), bricks, FLAGS);
        BlockPos lamp = floor.offset(0, LAMP_Y, 0);
        level.setBlock(lamp, ModBlocks.LIGHTHOUSE_LAMP.get().defaultBlockState(), FLAGS);
        level.setBlock(floor.offset(-3, LAMP_Y + 3, -3), lampLight.setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), FLAGS);

        // 5. the keeper's house on the south of the platform: 7 wide, 5 deep, door to the tower
        house(level, floor.offset(-3, 1, 6), random);
        // 6. a wooden dock reaching out west, where the black water would lap
        for (int x = -14; x <= -9; x++)
            for (int z = 0; z <= 1; z++) {
                level.setBlock(floor.offset(x, 0, z), planks, FLAGS);
                if (x % 3 == 0) level.setBlock(floor.offset(x, 1, z), Blocks.SPRUCE_FENCE.defaultBlockState(), FLAGS);
            }
        level.setBlock(floor.offset(-14, 1, 0), lampLight, FLAGS);

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
        // a cot: wool on a plank frame
        level.setBlock(c.offset(1, 1, 1), vanilla("red_wool"), FLAGS);
        level.setBlock(c.offset(1, 1, 2), vanilla("white_wool"), FLAGS);
        level.setBlock(c.offset(5, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), FLAGS);
        level.setBlock(c.offset(5, 1, 3), Blocks.FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.FurnaceBlock.FACING, Direction.WEST), FLAGS);
        level.setBlock(c.offset(3, 2, 2), ModBlocks.LUMINITE_LANTERN_BLOCK.get().defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), FLAGS);
        level.setBlock(c.offset(3, 3, 2), planks, FLAGS);
        // the shelf for the six relics, by the south wall
        level.setBlock(c.offset(2, 1, 3), ModBlocks.TROPHY_SHELF.get().defaultBlockState(), FLAGS);
        BlockPos chest = c.offset(5, 1, 2);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST), FLAGS);
        RandomizableContainer.setBlockEntityLootTable(level, random, chest, LOOT);
    }
}
