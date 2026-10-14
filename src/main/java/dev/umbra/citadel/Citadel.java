package dev.umbra.citadel;

import dev.umbra.boss.Architect;
import dev.umbra.journal.Journal;
import dev.umbra.mimic.MimicChests;
import dev.umbra.quantum.QuantumMaze;
import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModEntities;
import dev.umbra.underside.Underside;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Quantum Citadel. Its gates stand in the Underside (one per {@link CitadelGateFeature#CELL}-chunk cell);
 * the citadel itself lies "behind" them, sealed inside unbreakable walls high above the caves, and is built the
 * first time someone steps through. Wings, west to east along +x:
 *
 *   entrance hall → quantum maze → hall of living darkness (statues) → vault (mimic chests, a keeper)
 *   → library (a keeper; a wall only a slipping mind can walk through) → sealed door → the Architect's maze.
 *
 * The Underside Key opens the sealed door and wakes the Architect.
 */
public final class Citadel {
    /** The citadel hangs right above its gate, high over the caves, out of reach of the terrain. */
    public static final int POCKET_Y = 200;
    public static final int WEST = 113;
    public static final int LENGTH = 226;
    public static final int HALF_WIDTH = 20;

    // wing offsets (x relative to the origin), all at floor level y = 0. Corridors are 5 wide and 4 high.
    public static final int HALL_TO = 16;
    public static final int MAZE_1 = 22, MAZE_CELLS = 9, MAZE_PITCH = 4, MAZE_SIZE = MAZE_CELLS * MAZE_PITCH + 1; // 37
    public static final int DARK_FROM = 64, DARK_TO = 114;
    public static final int VAULT_FROM = 115, VAULT_TO = 137;
    public static final int LIBRARY_FROM = 143, LIBRARY_TO = 167;
    public static final int GARDEN_FROM = 173, GARDEN_TO = 195;
    public static final int SEAL_X = 200;
    public static final int THRONE = 201;
    public static final BlockPos ARRIVAL = new BlockPos(5, 0, 0);

    public static final ResourceKey<LootTable> LIBRARY_LOOT = key("chests/citadel_library");
    public static final ResourceKey<LootTable> SECRET_LOOT = key("chests/citadel_secret");
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    private Citadel() {}

    private static ResourceKey<LootTable> key(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, path));
    }

    // ------------------------------------------------------------------ geometry

    /** Origin (entrance hall floor, west end) of the citadel whose gate is in chunk (cx, cz). */
    public static BlockPos origin(int gateChunkX, int gateChunkZ) {
        return new BlockPos(gateChunkX * 16 + 8 - WEST, POCKET_Y, gateChunkZ * 16 + 8);
    }

    public static boolean inPocket(Entity e) {
        return Underside.is(e.level()) && e.getY() > POCKET_Y - 15;
    }

    /** Origin of the citadel containing {@code pos}, or null. */
    public static BlockPos originAt(long seed, Vec3 pos) {
        int x = (int) Math.floor(pos.x), z = (int) Math.floor(pos.z);
        int c = CitadelGateFeature.CELL;
        for (int gx = Math.floorDiv(Math.floorDiv(x - LENGTH + WEST - 8, 16), c); gx <= Math.floorDiv(Math.floorDiv(x + 3 + WEST - 8, 16), c); gx++)
            for (int gz = Math.floorDiv(Math.floorDiv(z - 40, 16), c); gz <= Math.floorDiv(Math.floorDiv(z + 40, 16), c); gz++) {
                int[] chunk = CitadelGateFeature.chosenChunkOf(seed, gx, gz);
                BlockPos o = origin(chunk[0], chunk[1]);
                if (x >= o.getX() - 3 && x <= o.getX() + LENGTH && Math.abs(z - o.getZ()) <= HALF_WIDTH) return o;
            }
        return null;
    }
    public static boolean isBuilt(ServerLevel level, BlockPos origin) {
        return level.getBlockState(origin.offset(-3, -3, 0)).is(ModBlocks.MAZE_WALL.get())
                && level.getBlockState(origin.offset(1, 0, 0)).isAir();
    }

    // ------------------------------------------------------------------ travel

    /** Through a gate in the caves: into its citadel (built on first visit). */
    public static TeleportTransition enter(ServerLevel underside, Entity entity, BlockPos gate) {
        BlockPos o = origin(gate.getX() >> 4, gate.getZ() >> 4);
        if (!isBuilt(underside, o)) build(underside, o, underside.getRandom());
        if (entity instanceof ServerPlayer p) {
            p.setData(ModAttachments.CITADEL_RETURN, gate.asLong());
            Journal.unlock(p, "citadel");
            p.sendSystemMessage(Component.translatable("message.umbra.citadel.enter").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }
        return new TeleportTransition(underside, Vec3.atBottomCenterOf(o.offset(ARRIVAL)), Vec3.ZERO, -90.0F, 0.0F, TeleportTransition.PLAY_PORTAL_SOUND);
    }

    /** Back through the gate in the entrance hall: to where you came in (or the gate of this cell). */
    public static TeleportTransition leave(ServerLevel underside, Entity entity) {
        BlockPos back = null;
        if (entity instanceof ServerPlayer p && p.getData(ModAttachments.CITADEL_RETURN) != Long.MIN_VALUE) {
            back = BlockPos.of(p.getData(ModAttachments.CITADEL_RETURN));
        }
        if (back == null) {
            BlockPos o = originAt(underside.getSeed(), entity.position());
            back = o == null ? new BlockPos((int) entity.getX(), Underside.FALLBACK_Y, (int) entity.getZ())
                    : new BlockPos(o.getX() + WEST, Underside.FALLBACK_Y, o.getZ());
        }
        // step out in front of the gate (the arch faces south)
        Vec3 at = Vec3.atBottomCenterOf(back.relative(Direction.SOUTH, 2));
        return new TeleportTransition(underside, at, Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.PLAY_PORTAL_SOUND);
    }

    // ------------------------------------------------------------------ the sealed door

    /** The Underside Key turns in the seal: the door crumbles and the Architect wakes. */
    public static Architect openThrone(ServerLevel level, BlockPos sealPos, Player player) {
        // the seal is 3×3 (x = SEAL_X, y 0..2, z -1..1): its lowest, most northern block gives the origin
        int minY = sealPos.getY(), minZ = sealPos.getZ();
        for (int dy = -2; dy <= 2; dy++)
            for (int dz = -2; dz <= 2; dz++)
                if (level.getBlockState(sealPos.offset(0, dy, dz)).is(ModBlocks.CITADEL_SEAL.get())) {
                    minY = Math.min(minY, sealPos.getY() + dy);
                    minZ = Math.min(minZ, sealPos.getZ() + dz);
                }
        return openThroneAt(level, new BlockPos(sealPos.getX() - SEAL_X, minY, minZ + 1), player);
    }

    public static Architect openThroneAt(ServerLevel level, BlockPos o, Player player) {
        for (int y = 0; y <= 2; y++)
            for (int z = -1; z <= 1; z++) {
                BlockPos p = o.offset(SEAL_X, y, z);
                if (level.getBlockState(p).is(ModBlocks.CITADEL_SEAL.get())) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
                }
            }
        BlockPos corner = o.offset(THRONE, -1, -QuantumMaze.SIZE / 2);
        int mid = QuantumMaze.SIZE / 2;
        BlockPos spawn = corner.offset(mid, 1, mid + 2);
        Architect boss = ModEntities.ARCHITECT.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (boss == null) return null;
        boss.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0, 0);
        if (player != null) boss.setTarget(player);
        level.addFreshEntity(boss);
        level.playSound(null, spawn, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.0F, 0.5F);
        return boss;
    }

    // ------------------------------------------------------------------ building

    public static void build(ServerLevel level, BlockPos o, RandomSource random) {
        BlockState wall = ModBlocks.MAZE_WALL.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        // 1. one solid unbreakable block to carve from
        fill(level, o.offset(-3, -3, -HALF_WIDTH), o.offset(LENGTH, 10, HALF_WIDTH), wall);

        // 2. entrance hall with the way back
        fill(level, o.offset(1, 0, -8), o.offset(HALL_TO, 5, 8), air);
        for (int y = 0; y <= 3; y++)
            for (int z = -1; z <= 1; z++) level.setBlock(o.offset(0, y, z), ModBlocks.CITADEL_GATE.get().facing(Direction.Axis.Z), FLAGS);
        lanterns(level, o, 2, HALL_TO, -8, 8, 5);
        corridor(level, o, HALL_TO + 1, MAZE_1 - 1);

        // 3. a wide quantum maze: 9×9 cells, passages 3 blocks wide
        wideMaze(level, o.offset(MAZE_1, 0, -MAZE_SIZE / 2), random);
        corridor(level, o, MAZE_1 + MAZE_SIZE, DARK_FROM - 1);

        // 4. the hall of living darkness: pillars, statues
        fill(level, o.offset(DARK_FROM, 0, -5), o.offset(DARK_TO, 4, 5), ModBlocks.LIVING_DARKNESS.get().defaultBlockState());
        for (int x = DARK_FROM + 4; x < DARK_TO; x += 8)
            for (int z : new int[]{-3, 3})
                fill(level, o.offset(x, 0, z), o.offset(x, 4, z), wall);
        for (int x = DARK_FROM + 10; x < DARK_TO; x += 12) spawn(level, ModEntities.CITADEL_STATUE.get(), o.offset(x, 0, random.nextInt(5) - 2));

        // 5. the vault: half the chests are hungry
        fill(level, o.offset(VAULT_FROM, 0, -11), o.offset(VAULT_TO, 5, 11), air);
        int n = 0;
        for (int x = VAULT_FROM + 2; x <= VAULT_TO - 2; x += 3) {
            for (int z : new int[]{-10, 10}) {
                BlockPos c = o.offset(x, 0, z);
                if (random.nextBoolean()) {
                    MimicChests.plant(level, c);
                } else {
                    level.setBlock(c, Blocks.CHEST.defaultBlockState(), FLAGS);
                    RandomizableContainer.setBlockEntityLootTable(level, random, c, n % 3 == 0 ? QuantumMaze.PRIZE : QuantumMaze.COMMON);
                }
                n++;
            }
        }
        lanterns(level, o, VAULT_FROM, VAULT_TO, -11, 11, 5);
        spawn(level, ModEntities.CITADEL_KEEPER.get(), o.offset((VAULT_FROM + VAULT_TO) / 2, 0, 0));
        spawn(level, ModEntities.CITADEL_STATUE.get(), o.offset(VAULT_TO - 3, 0, 6));
        corridor(level, o, VAULT_TO + 1, LIBRARY_FROM - 1);

        // 6. the library
        fill(level, o.offset(LIBRARY_FROM, 0, -10), o.offset(LIBRARY_TO, 6, 10), air);
        for (int x = LIBRARY_FROM; x <= LIBRARY_TO; x++)
            for (int y = 0; y <= 4; y++) {
                if (x % 4 == 0) continue;
                level.setBlock(o.offset(x, y, -10), Blocks.BOOKSHELF.defaultBlockState(), FLAGS);
                level.setBlock(o.offset(x, y, 10), Blocks.BOOKSHELF.defaultBlockState(), FLAGS);
            }
        for (int x = LIBRARY_FROM + 3; x <= LIBRARY_TO - 3; x += 5)
            for (int z : new int[]{-5, 5})
                for (int dz = -1; dz <= 1; dz++)
                    for (int y = 0; y <= 2; y++) level.setBlock(o.offset(x, y, z + dz), Blocks.BOOKSHELF.defaultBlockState(), FLAGS);
        BlockPos lib = o.offset(LIBRARY_FROM + 2, 0, 8);
        level.setBlock(lib, Blocks.CHEST.defaultBlockState(), FLAGS);
        RandomizableContainer.setBlockEntityLootTable(level, random, lib, LIBRARY_LOOT);
        lanterns(level, o, LIBRARY_FROM, LIBRARY_TO, -9, 9, 6);
        spawn(level, ModEntities.CITADEL_KEEPER.get(), o.offset((LIBRARY_FROM + LIBRARY_TO) / 2, 0, 0));
        // the secret: behind the north shelves, a wall a sane mind cannot pass
        int sx = SECRET_X;
        fill(level, o.offset(sx - 3, 0, -19), o.offset(sx + 3, 3, -14), air);
        for (int x = sx - 1; x <= sx + 1; x++)
            for (int y = 0; y <= 2; y++)
                for (int z = -13; z <= -10; z++) level.setBlock(o.offset(x, y, z), ModBlocks.PHANTOM_WALL.get().defaultBlockState(), FLAGS);
        BlockPos secret = o.offset(sx, 0, -18);
        level.setBlock(secret, Blocks.CHEST.defaultBlockState(), FLAGS);
        RandomizableContainer.setBlockEntityLootTable(level, random, secret, SECRET_LOOT);
        level.setBlock(o.offset(sx, 3, -16), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), FLAGS);
        corridor(level, o, LIBRARY_TO + 1, GARDEN_FROM - 1);

        // 7. the dead garden: ash, dead trees, crawlers in the branches
        fill(level, o.offset(GARDEN_FROM, 0, -10), o.offset(GARDEN_TO, 7, 10), air);
        fill(level, o.offset(GARDEN_FROM, -1, -10), o.offset(GARDEN_TO, -1, 10), ModBlocks.ASH_SOIL.get().defaultBlockState());
        for (int t = 0; t < 7; t++) {
            BlockPos base = o.offset(GARDEN_FROM + 2 + random.nextInt(GARDEN_TO - GARDEN_FROM - 3), 0, random.nextInt(17) - 8);
            int h = 3 + random.nextInt(4);
            for (int y = 0; y < h; y++) level.setBlock(base.above(y), ModBlocks.DEAD_LOG.get().defaultBlockState(), FLAGS);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (random.nextInt(3) == 0) continue;
                BlockPos br = base.above(h - 1 - random.nextInt(2)).relative(d);
                level.setBlock(br, ModBlocks.DEAD_LOG.get().defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, d.getAxis()), FLAGS);
            }
        }
        lanterns(level, o, GARDEN_FROM, GARDEN_TO, -10, 10, 7);
        for (int i = 0; i < 3; i++) spawn(level, ModEntities.CRAWLER.get(), o.offset(GARDEN_FROM + 4 + i * 7, 0, random.nextInt(9) - 4));
        corridor(level, o, GARDEN_TO + 1, SEAL_X - 1);

        // 8. the Architect's maze behind the seal
        QuantumMaze.build(level, o.offset(THRONE, -1, -QuantumMaze.SIZE / 2), random);
        for (int y = 0; y <= 2; y++)
            for (int z = -1; z <= 1; z++) level.setBlock(o.offset(SEAL_X, y, z), ModBlocks.CITADEL_SEAL.get().defaultBlockState(), FLAGS);
    }

    public static final int SECRET_X = (LIBRARY_FROM + LIBRARY_TO) / 2;

    /** A 5-wide, 4-high corridor along x at z = -2..2. */
    private static void corridor(ServerLevel level, BlockPos o, int x0, int x1) {
        if (x1 < x0) return;
        fill(level, o.offset(x0, 0, -2), o.offset(x1, 3, 2), Blocks.AIR.defaultBlockState());
    }

    /**
     * A one-storey maze with 3-wide passages. {@code corner} is its north-west corner at floor level.
     * Some walls and some passages are quantum: they shift while nobody is looking.
     */
    private static void wideMaze(ServerLevel level, BlockPos corner, RandomSource random) {
        int n = MAZE_CELLS, p = MAZE_PITCH;
        boolean[][] seen = new boolean[n][n];
        boolean[][] eastOpen = new boolean[n][n], southOpen = new boolean[n][n];
        java.util.ArrayDeque<int[]> stack = new java.util.ArrayDeque<>();
        int mid = n / 2;
        stack.push(new int[]{0, mid});
        seen[0][mid] = true;
        while (!stack.isEmpty()) {
            int[] c = stack.peek();
            java.util.List<int[]> next = new java.util.ArrayList<>();
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : dirs) {
                int x = c[0] + d[0], z = c[1] + d[1];
                if (x >= 0 && z >= 0 && x < n && z < n && !seen[x][z]) next.add(new int[]{x, z, d[0], d[1]});
            }
            if (next.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] t = next.get(random.nextInt(next.size()));
            if (t[2] == 1) eastOpen[c[0]][c[1]] = true;
            if (t[2] == -1) eastOpen[t[0]][t[1]] = true;
            if (t[3] == 1) southOpen[c[0]][c[1]] = true;
            if (t[3] == -1) southOpen[t[0]][t[1]] = true;
            seen[t[0]][t[1]] = true;
            stack.push(new int[]{t[0], t[1]});
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState quantum = ModBlocks.QUANTUM_BLOCK.get().defaultBlockState();
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                int x0 = 1 + i * p, z0 = 1 + j * p;
                fill(level, corner.offset(x0, 0, z0), corner.offset(x0 + p - 2, 3, z0 + p - 2), air);
                if (random.nextInt(6) == 0) level.setBlock(corner.offset(x0 + 1, 3, z0 + 1), lantern, FLAGS);
                if (i < n - 1) wallStrip(level, corner, x0 + p - 1, z0, true, eastOpen[i][j], random, air, quantum);
                if (j < n - 1) wallStrip(level, corner, x0, z0 + p - 1, false, southOpen[i][j], random, air, quantum);
            }
        // way in (west, middle row) and way out (east, middle row)
        int zMid = 1 + mid * p;
        fill(level, corner.offset(0, 0, zMid), corner.offset(0, 3, zMid + p - 2), air);
        fill(level, corner.offset(MAZE_SIZE - 1, 0, zMid), corner.offset(MAZE_SIZE - 1, 3, zMid + p - 2), air);
        // two chests in random cells
        for (int k = 0; k < 2; k++) {
            BlockPos c = corner.offset(1 + random.nextInt(n) * p, 0, 1 + random.nextInt(n) * p);
            level.setBlock(c, Blocks.CHEST.defaultBlockState(), FLAGS);
            RandomizableContainer.setBlockEntityLootTable(level, random, c, QuantumMaze.COMMON);
        }
    }

    /** The 3-wide wall between two cells: open passage, solid wall, or (sometimes) quantum either way. */
    private static void wallStrip(ServerLevel level, BlockPos corner, int x, int z, boolean alongZ, boolean open, RandomSource random,
                                  BlockState air, BlockState quantum) {
        boolean q = random.nextInt(open ? 5 : 4) == 0;
        for (int k = 0; k < MAZE_PITCH - 1; k++) {
            BlockPos base = alongZ ? corner.offset(x, 0, z + k) : corner.offset(x + k, 0, z);
            for (int y = 0; y <= 3; y++) {
                BlockState s = q ? quantum.setValue(dev.umbra.quantum.QuantumBlock.OPEN, open) : (open ? air : null);
                if (s != null) level.setBlock(base.above(y), s, Block.UPDATE_ALL);
            }
        }
    }

    private static void fill(ServerLevel level, BlockPos a, BlockPos b, BlockState state) {
        for (BlockPos p : BlockPos.betweenClosed(a, b)) level.setBlock(p, state, FLAGS);
    }

    private static void lanterns(ServerLevel level, BlockPos o, int x0, int x1, int z0, int z1, int ceiling) {
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        for (int x = x0 + 2; x <= x1 - 2; x += 5) {
            level.setBlock(o.offset(x, ceiling, z0 + 1), lantern, FLAGS);
            level.setBlock(o.offset(x, ceiling, z1 - 1), lantern, FLAGS);
        }
    }

    private static void spawn(ServerLevel level, EntityType<? extends Mob> type, BlockPos at) {
        Mob mob = type.create(level, EntitySpawnReason.STRUCTURE);
        if (mob == null) return;
        mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
    }

    public static AABB bounds(BlockPos o) {
        return AABB.encapsulatingFullBlocks(o.offset(-3, -3, -HALF_WIDTH), o.offset(LENGTH, 10, HALF_WIDTH));
    }

    /** Statues and keepers of a citadel (tests). */
    public static int count(ServerLevel level, BlockPos o, Class<? extends Entity> type) {
        return level.getEntitiesOfClass(type, bounds(o)).size();
    }
}
