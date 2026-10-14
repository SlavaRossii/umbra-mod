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
    public static final int WEST = 62;
    public static final int LENGTH = 156;

    // wing offsets (x relative to the origin), all at floor level y = 0
    public static final int MAZE_1 = 18;
    public static final int DARK_FROM = 46, DARK_TO = 86;
    public static final int VAULT_FROM = 87, VAULT_TO = 103;
    public static final int LIBRARY_FROM = 108, LIBRARY_TO = 124;
    public static final int SEAL_X = 129;
    public static final int THRONE = 130;
    public static final BlockPos ARRIVAL = new BlockPos(4, 0, 0);

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
            for (int gz = Math.floorDiv(Math.floorDiv(z - 30, 16), c); gz <= Math.floorDiv(Math.floorDiv(z + 30, 16), c); gz++) {
                int[] chunk = CitadelGateFeature.chosenChunkOf(seed, gx, gz);
                BlockPos o = origin(chunk[0], chunk[1]);
                if (x >= o.getX() - 3 && x <= o.getX() + LENGTH && Math.abs(z - o.getZ()) <= 14) return o;
            }
        return null;
    }
    public static boolean isBuilt(ServerLevel level, BlockPos origin) {
        return level.getBlockState(origin.offset(-3, -10, 0)).is(ModBlocks.MAZE_WALL.get())
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
        // 1. solid unbreakable block
        fill(level, o.offset(-3, -10, -14), o.offset(LENGTH, 13, 14), wall);
        // 2. entrance hall with the way back
        fill(level, o.offset(1, 0, -6), o.offset(12, 4, 6), air);
        for (int y = 0; y <= 3; y++)
            for (int z = -1; z <= 1; z++) level.setBlock(o.offset(0, y, z), ModBlocks.CITADEL_GATE.get().defaultBlockState(), FLAGS);
        lanterns(level, o, 2, 12, -6, 6, 4);
        // 3. corridor → first maze
        fill(level, o.offset(13, 0, -1), o.offset(MAZE_1 - 1, 2, 1), air);
        QuantumMaze.build(level, o.offset(MAZE_1, -1, -QuantumMaze.SIZE / 2), random);
        int mazeEnd = MAZE_1 + QuantumMaze.SIZE - 1;
        fill(level, o.offset(mazeEnd, 0, 0), o.offset(DARK_FROM - 1, 2, 0), air);
        fill(level, o.offset(mazeEnd + 1, 0, -1), o.offset(DARK_FROM - 1, 2, 1), air);
        // 4. the hall of living darkness, with statues
        fill(level, o.offset(DARK_FROM, 0, -3), o.offset(DARK_TO, 3, 3), ModBlocks.LIVING_DARKNESS.get().defaultBlockState());
        for (int x : new int[]{DARK_FROM + 10, DARK_FROM + 22, DARK_FROM + 34}) spawn(level, ModEntities.CITADEL_STATUE.get(), o.offset(x, 0, random.nextInt(5) - 2));
        // 5. the vault: half the chests are hungry
        fill(level, o.offset(VAULT_FROM, 0, -8), o.offset(VAULT_TO, 4, 8), air);
        int n = 0;
        for (int x = VAULT_FROM + 2; x <= VAULT_TO - 2; x += 3) {
            for (int z : new int[]{-7, 7}) {
                BlockPos c = o.offset(x, 0, z);
                if (n % 2 == random.nextInt(2)) {
                    MimicChests.plant(level, c);
                } else {
                    level.setBlock(c, Blocks.CHEST.defaultBlockState(), FLAGS);
                    RandomizableContainer.setBlockEntityLootTable(level, random, c, n % 3 == 0 ? QuantumMaze.PRIZE : QuantumMaze.COMMON);
                }
                n++;
            }
        }
        lanterns(level, o, VAULT_FROM + 1, VAULT_TO - 1, -8, 8, 4);
        spawn(level, ModEntities.CITADEL_KEEPER.get(), o.offset((VAULT_FROM + VAULT_TO) / 2, 0, 0));
        // 6. corridor → library
        fill(level, o.offset(VAULT_TO + 1, 0, -1), o.offset(LIBRARY_FROM - 1, 2, 1), air);
        fill(level, o.offset(LIBRARY_FROM, 0, -7), o.offset(LIBRARY_TO, 5, 7), air);
        for (int x = LIBRARY_FROM; x <= LIBRARY_TO; x++)
            for (int y = 0; y <= 3; y++) {
                if (x % 4 == 0) continue;
                level.setBlock(o.offset(x, y, -7), Blocks.BOOKSHELF.defaultBlockState(), FLAGS);
                level.setBlock(o.offset(x, y, 7), Blocks.BOOKSHELF.defaultBlockState(), FLAGS);
            }
        for (int x = LIBRARY_FROM + 3; x <= LIBRARY_TO - 3; x += 5)
            for (int z : new int[]{-3, 3}) {
                for (int y = 0; y <= 2; y++) level.setBlock(o.offset(x, y, z), Blocks.BOOKSHELF.defaultBlockState(), FLAGS);
            }
        BlockPos lib = o.offset(LIBRARY_FROM + 2, 0, 5);
        level.setBlock(lib, Blocks.CHEST.defaultBlockState(), FLAGS);
        RandomizableContainer.setBlockEntityLootTable(level, random, lib, LIBRARY_LOOT);
        lanterns(level, o, LIBRARY_FROM + 1, LIBRARY_TO - 1, -6, 6, 5);
        spawn(level, ModEntities.CITADEL_KEEPER.get(), o.offset((LIBRARY_FROM + LIBRARY_TO) / 2, 0, 0));
        // the secret: behind the north shelves, a wall a sane mind cannot pass
        int sx = (LIBRARY_FROM + LIBRARY_TO) / 2;
        fill(level, o.offset(sx - 3, 0, -13), o.offset(sx + 3, 3, -10), air);
        for (int x = sx - 1; x <= sx + 1; x++)
            for (int y = 0; y <= 2; y++) {
                level.setBlock(o.offset(x, y, -7), ModBlocks.PHANTOM_WALL.get().defaultBlockState(), FLAGS);
                level.setBlock(o.offset(x, y, -8), ModBlocks.PHANTOM_WALL.get().defaultBlockState(), FLAGS);
                level.setBlock(o.offset(x, y, -9), ModBlocks.PHANTOM_WALL.get().defaultBlockState(), FLAGS);
            }
        BlockPos secret = o.offset(sx, 0, -12);
        level.setBlock(secret, Blocks.CHEST.defaultBlockState(), FLAGS);
        RandomizableContainer.setBlockEntityLootTable(level, random, secret, SECRET_LOOT);
        level.setBlock(o.offset(sx, 3, -11), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), FLAGS);
        // 7. corridor → the Architect's maze behind the seal
        fill(level, o.offset(LIBRARY_TO + 1, 0, -1), o.offset(SEAL_X - 1, 2, 1), air);
        QuantumMaze.build(level, o.offset(THRONE, -1, -QuantumMaze.SIZE / 2), random);
        for (int y = 0; y <= 2; y++)
            for (int z = -1; z <= 1; z++) level.setBlock(o.offset(SEAL_X, y, z), ModBlocks.CITADEL_SEAL.get().defaultBlockState(), FLAGS);
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

    /** Statues and keepers of a citadel (tests). */
    public static int count(ServerLevel level, BlockPos o, Class<? extends Entity> type) {
        return level.getEntitiesOfClass(type, AABB.encapsulatingFullBlocks(o.offset(-3, -10, -14), o.offset(LENGTH, 13, 14))).size();
    }
}
