package dev.umbra.depths;

import dev.umbra.journal.Journal;
import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModEntities;
import dev.umbra.registry.ModItems;
import dev.umbra.underside.Underside;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Depths, the drowned halls of the Keepers. One sealed place for the whole world, deep in the rock of the
 * Underside far from everything, reached with the Key of the Depths from any water of the Underside, and left the
 * same way. Built on the first visit.
 * <p>
 * 2.7: drowned to the vault, every hall of it, and no air anywhere (the key gives one long breath; bring your own
 * after that). The halls branch and wind: a landing, a gallery with two bell chambers off it, the hall of pillars
 * with two valve rooms, a crossing to the north with a third valve, a fourth bell in a crypt, and behind three sluice
 * gates the sanctum. What is to be done here:
 * <ul>
 *   <li>the Keepers' lamps have gone out: a luminite crystal lights one (light under water, and the mind mends near it);</li>
 *   <li>three valves, one gate each: turn all three and the way to the sanctum is open;</li>
 *   <li>currents at the mouths of two side passages drag you into them; at their ends, caches;</li>
 *   <li>drowned acolytes in the water; words scratched in the walls of three rooms;</li>
 *   <li>four bells: when all have rung, the Drowned Keeper wakes in the sanctum. Beaten, the bells go quiet and can
 *       be rung again.</li>
 * </ul>
 */
public final class Depths {
    private Depths() {}

    /** 2.7: a new place (the 2.6 halls, if a world has them, are left where they were). */
    public static final BlockPos ORIGIN = new BlockPos(120000, 140, 4000);
    public static final int VALVES = 3, BELLS = 4;
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, "chests/depths"));
    public static final ResourceKey<LootTable> CACHE = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, "chests/depths_cache"));
    private static final int FLAGS = Block.UPDATE_CLIENTS;
    public static final String KEEPER_TAG = "umbra_drowned_keeper";

    // ------------------------------------------------------------------ the plan (x east, z south, y up from the floor)

    /** A hall: centre, half sizes, height of the water inside. */
    public record Room(String name, int x, int z, int hx, int hz, int h) {
        public boolean contains(BlockPos local) {
            return Math.abs(local.getX() - x) <= hx && Math.abs(local.getZ() - z) <= hz && local.getY() >= 0 && local.getY() <= h;
        }
    }

    public static final Room LANDING = new Room("landing", 0, 0, 4, 4, 6);
    public static final Room GALLERY = new Room("gallery", 0, -28, 8, 5, 7);
    public static final Room BELL_W = new Room("bell_w", -32, -30, 4, 4, 7);
    public static final Room BELL_E = new Room("bell_e", 32, -22, 4, 4, 7);
    public static final Room PILLARS = new Room("pillars", 0, -60, 11, 8, 10);
    public static final Room VALVE_W = new Room("valve_w", -34, -68, 4, 4, 6);
    public static final Room VALVE_E = new Room("valve_e", 34, -74, 4, 4, 6);
    public static final Room BELL_N = new Room("bell_n", 24, -100, 5, 4, 8);
    public static final Room VALVE_N = new Room("valve_n", -28, -102, 4, 4, 6);
    public static final Room CRYPT = new Room("crypt", -14, -124, 4, 4, 7);
    public static final Room CACHE_W = new Room("cache_w", -46, -47, 2, 2, 3);
    public static final Room CACHE_E = new Room("cache_e", 46, -41, 2, 2, 3);
    public static final Room SANCTUM = new Room("sanctum", 0, -152, 10, 10, 12);
    public static final java.util.List<Room> ROOMS = java.util.List.of(LANDING, GALLERY, BELL_W, BELL_E, PILLARS, VALVE_W, VALVE_E, BELL_N, VALVE_N, CRYPT, CACHE_W, CACHE_E, SANCTUM);

    /** Passages: runs of straight pieces through these points, three wide and four high. */
    public static final int[][][] PASSAGES = {
            {{0, -4}, {0, -10}, {6, -10}, {6, -17}, {0, -17}, {0, -23}},                       // landing - gallery
            {{-8, -28}, {-16, -28}, {-16, -36}, {-24, -36}, {-24, -30}, {-28, -30}},            // gallery - west bell
            {{8, -28}, {18, -28}, {18, -20}, {28, -20}, {28, -22}},                            // gallery - east bell
            {{0, -33}, {0, -40}, {-6, -40}, {-6, -46}, {0, -46}, {0, -52}},                    // gallery - pillars
            {{-11, -60}, {-20, -60}, {-20, -68}, {-30, -68}},                                  // pillars - west valve
            {{11, -62}, {22, -62}, {22, -74}, {30, -74}},                                      // pillars - east valve
            {{0, -68}, {0, -80}, {8, -80}, {8, -88}, {0, -88}},                                // pillars - the crossing
            {{0, -88}, {14, -88}, {14, -100}, {19, -100}},                                     // crossing - north bell
            {{0, -88}, {-16, -88}, {-16, -102}, {-24, -102}},                                  // crossing - north valve
            {{0, -88}, {0, -100}, {-6, -100}, {-6, -112}, {0, -112}, {0, -142}},               // crossing - the gates - sanctum
            {{-6, -106}, {-14, -106}, {-14, -120}},                                            // to the crypt
            {{-36, -30}, {-46, -30}, {-46, -45}},                                              // west bell - its cache (current)
            {{36, -22}, {46, -22}, {46, -39}},                                                 // east bell - its cache (current)
    };

    /** Where the sluice gates stand across the last passage, nearest first. */
    public static final int[] GATES_Z = {-120, -127, -134};

    /** Currents: box (min x, min z, max x, max z, in local coords), the push (dx, dz) and the cache it runs into. */
    public static final int[][] CURRENTS = {
            {-46, -31, -36, -29, -1, 0, 0}, {-47, -45, -45, -31, 0, -1, 0},     // into the west cache
            {36, -23, 46, -21, 1, 0, 1}, {45, -39, 47, -23, 0, -1, 1},          // into the east cache
    };

    public static final BlockPos[] VALVE_AT = {local(VALVE_W.x - VALVE_W.hx, 2, VALVE_W.z), local(VALVE_E.x + VALVE_E.hx, 2, VALVE_E.z), local(VALVE_N.x - VALVE_N.hx, 2, VALVE_N.z)};
    public static final BlockPos[] BELL_AT = {local(BELL_W.x, 2, BELL_W.z), local(BELL_E.x, 2, BELL_E.z), local(BELL_N.x, 2, BELL_N.z), local(CRYPT.x, 2, CRYPT.z)};
    public static final BlockPos CHEST_AT = local(SANCTUM.x, 2, SANCTUM.z - SANCTUM.hz + 2);
    /** The caches' chests, where the currents run (west, east). */
    public static final BlockPos[] CACHE_CHEST = {local(CACHE_W.x, 1, CACHE_W.z - 1), local(CACHE_E.x, 1, CACHE_E.z - 1)};
    /** 2.9: a mark in the floor under the landing: the halls have had their 2.9 furnishing (more cages, the Keepers' chest). */
    public static final BlockPos MARK_29 = local(0, -1, 0);
    public static final BlockPos BOSS_AT = local(SANCTUM.x, 2, SANCTUM.z);
    public static final BlockPos ARRIVAL = local(0, 2, 0);

    private static BlockPos local(int x, int y, int z) {
        return ORIGIN.offset(x, y, z);
    }

    // ------------------------------------------------------------------ where things are

    public static AABB bounds() {
        return new AABB(ORIGIN.getX() - 52, ORIGIN.getY() - 4, ORIGIN.getZ() - 166, ORIGIN.getX() + 52, ORIGIN.getY() + 16, ORIGIN.getZ() + 8);
    }

    public static boolean inside(Player p) {
        return Underside.is(p.level()) && bounds().contains(p.position());
    }

    public static boolean isBuilt(ServerLevel level) {
        return level.getBlockState(BOSS_AT.below()).is(Blocks.POLISHED_DEEPSLATE) && level.getBlockState(ARRIVAL).is(Blocks.WATER);
    }

    /** The hall a position is in, or null (in a passage, or outside). */
    public static Room roomAt(BlockPos p) {
        BlockPos l = p.subtract(ORIGIN);
        for (Room r : ROOMS) if (r.contains(l)) return r;
        return null;
    }

    /** Into the Depths (built on the first visit); the way back is remembered. The key gives one long breath. */
    public static void enter(ServerLevel underside, ServerPlayer player) {
        if (!isBuilt(underside)) build(underside, underside.getRandom());
        else upgrade(underside, underside.getRandom());
        player.setData(ModAttachments.DEPTHS_RETURN, player.blockPosition().asLong());
        Journal.unlock(player, "depths");
        player.sendSystemMessage(Component.translatable("message.umbra.depths.enter").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.translatable("message.umbra.depths.enter_hint").withStyle(ChatFormatting.GRAY));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING, BREATH, 0, false, true, true));
        player.teleport(new TeleportTransition(underside, Vec3.atBottomCenterOf(ARRIVAL), Vec3.ZERO, 180.0F, 0.0F, TeleportTransition.PLAY_PORTAL_SOUND));
    }

    /** The one long breath the key gives on the way down. */
    public static final int BREATH = 20 * 90;

    /** Back to the water you came down through. */
    public static void leave(ServerLevel underside, ServerPlayer player) {
        long back = player.getData(ModAttachments.DEPTHS_RETURN);
        BlockPos to = back == Long.MIN_VALUE ? dev.umbra.lighthouse.LighthouseManager.centre(underside).above() : BlockPos.of(back).above();
        player.teleport(new TeleportTransition(underside, Vec3.atBottomCenterOf(to), Vec3.ZERO, player.getYRot(), 0.0F, TeleportTransition.PLAY_PORTAL_SOUND));
    }

    // ------------------------------------------------------------------ building

    public static void build(ServerLevel level, RandomSource random) {
        // 1. what is hollow: halls and passages, as a set of local positions
        java.util.Set<Long> hollow = new java.util.HashSet<>();
        for (Room r : ROOMS)
            for (int x = -r.hx; x <= r.hx; x++)
                for (int z = -r.hz; z <= r.hz; z++)
                    for (int y = 1; y <= r.h; y++) hollow.add(BlockPos.asLong(r.x + x, y, r.z + z));
        for (int[][] run : PASSAGES)
            for (int i = 0; i + 1 < run.length; i++) {
                int x0 = run[i][0], z0 = run[i][1], x1 = run[i + 1][0], z1 = run[i + 1][1];
                int dx = Integer.signum(x1 - x0), dz = Integer.signum(z1 - z0);
                int x = x0, z = z0;
                while (true) {
                    for (int a = -1; a <= 1; a++)
                        for (int b = -1; b <= 1; b++)
                            for (int y = 1; y <= 4; y++) hollow.add(BlockPos.asLong(x + a, y, z + b));
                    if (x == x1 && z == z1) break;
                    x += dx;
                    z += dz;
                }
            }
        // 2. a skin of stone two thick round it, the floor under it, then the water
        BlockState shell = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState moss = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        java.util.Set<Long> skin = new java.util.HashSet<>();
        for (long h : hollow) {
            int hx = BlockPos.getX(h), hy = BlockPos.getY(h), hz = BlockPos.getZ(h);
            for (int a = -2; a <= 2; a++)
                for (int b = -2; b <= 2; b++)
                    for (int c = -2; c <= 2; c++) {
                        long n = BlockPos.asLong(hx + a, hy + b, hz + c);
                        if (!hollow.contains(n)) skin.add(n);
                    }
        }
        for (long n : skin) {
            BlockPos p = ORIGIN.offset(BlockPos.getX(n), BlockPos.getY(n), BlockPos.getZ(n));
            boolean underfoot = hollow.contains(BlockPos.asLong(BlockPos.getX(n), BlockPos.getY(n) + 1, BlockPos.getZ(n))) && BlockPos.getY(n) == 0;
            level.setBlock(p, underfoot ? (random.nextInt(6) == 0 ? moss : floor) : (random.nextInt(11) == 0 ? moss : shell), FLAGS);
        }
        for (long h : hollow) level.setBlock(ORIGIN.offset(BlockPos.getX(h), BlockPos.getY(h), BlockPos.getZ(h)), water, FLAGS);
        // 3. the halls' furniture
        for (int x = PILLARS.x - PILLARS.hx + 3; x <= PILLARS.x + PILLARS.hx - 3; x += 5)
            for (int z : new int[]{PILLARS.z - 4, PILLARS.z + 4})
                for (int y = 1; y <= PILLARS.h; y++) level.setBlock(local(x, y, z), random.nextBoolean() ? moss : shell, FLAGS);
        BlockState lamp = ModBlocks.DEPTHS_LAMP.get().defaultBlockState();
        for (Room r : ROOMS) {
            if (r.hx < 3) continue;
            for (int[] c : new int[][]{{-1, -1}, {1, 1}, {-1, 1}, {1, -1}}) {
                if (r != SANCTUM && r != PILLARS && c[0] != c[1]) continue;    // two lamps a hall, four in the big ones
                level.setBlock(local(r.x + c[0] * (r.hx - 1), 1, r.z + c[1] * (r.hz - 1)), lamp, FLAGS);
            }
        }
        for (BlockPos v : VALVE_AT) level.setBlock(v, ModBlocks.DEPTHS_VALVE.get().defaultBlockState(), FLAGS);
        for (BlockPos b : BELL_AT) {
            level.setBlock(b.below(), Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState().setValue(net.minecraft.world.level.block.WallBlock.WATERLOGGED, true), FLAGS);
            level.setBlock(b, ModBlocks.DEPTHS_BELL.get().defaultBlockState(), FLAGS);
        }
        for (int i = 0; i < GATES_Z.length; i++) setGate(level, i, true);
        // the sanctum: a dais, the chest, the place he sleeps
        for (int x = -3; x <= 3; x++)
            for (int z = -2; z <= 2; z++) level.setBlock(local(SANCTUM.x + x, 1, SANCTUM.z - SANCTUM.hz + 2 + z), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), FLAGS);
        level.setBlock(BOSS_AT.below(), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), FLAGS);
        for (BlockPos c : CACHE_CHEST) chest(level, random, c, CACHE, net.minecraft.core.Direction.SOUTH);
        // 4. the dark that lives here
        cage(level, ModEntities.ACOLYTE.get(), local(GALLERY.x + 6, 1, GALLERY.z + 4));
        cage(level, ModEntities.ACOLYTE.get(), local(PILLARS.x - 9, 1, PILLARS.z));
        cage(level, ModEntities.LURKER.get(), local(PILLARS.x + 9, 1, PILLARS.z + 6));
        for (Room r : new Room[]{GALLERY, BELL_W, BELL_E, PILLARS, VALVE_W, VALVE_E, BELL_N, VALVE_N, CRYPT})
            spawn(level, ModEntities.ACOLYTE.get(), local(r.x + random.nextInt(3) - 1, 2, r.z + random.nextInt(3) - 1));
        for (int i = 0; i < 2; i++) spawn(level, ModEntities.LURKER.get(), local(GALLERY.x - 4 + i * 8, 3, GALLERY.z));
        level.setBlock(CHEST_AT, Blocks.WATER.defaultBlockState(), FLAGS);
        furnish29(level, random);
    }

    // ------------------------------------------------------------------ 2.9: more of the dark, and the Keepers' treasure

    /** The 2.9 cages: twice as many as before (the old three and these three), and as many again of the dark at once. */
    public static final BlockPos[] CAGES_29 = {local(BELL_N.x + 3, 1, BELL_N.z + 2), local(CRYPT.x + 2, 1, CRYPT.z - 2), local(VALVE_W.x + 2, 1, VALVE_W.z + 2)};

    public static boolean furnished29(ServerLevel level) {
        return level.getBlockState(MARK_29).is(Blocks.CHISELED_DEEPSLATE);
    }

    /** Halls built before 2.9 get their new furnishing the next time someone comes down. */
    public static void upgrade(ServerLevel level, RandomSource random) {
        if (furnished29(level)) return;
        // the old sanctum chest gives what is in it to the Keepers' chest
        java.util.List<net.minecraft.world.item.ItemStack> kept = new java.util.ArrayList<>();
        if (level.getBlockEntity(CHEST_AT) instanceof net.minecraft.world.Container old) {
            for (int i = 0; i < old.getContainerSize(); i++) if (!old.getItem(i).isEmpty()) kept.add(old.getItem(i).copy());
            old.clearContent();
        }
        level.setBlock(CHEST_AT, Blocks.WATER.defaultBlockState(), FLAGS);
        furnish29(level, random);
        if (!kept.isEmpty() && level.getBlockEntity(CHEST_AT) instanceof KeepersChest.Entity chest) {
            chest.unpackLootTable(null);
            for (net.minecraft.world.item.ItemStack st : kept)
                for (int i = 0; i < chest.getContainerSize(); i++)
                    if (chest.getItem(i).isEmpty()) {
                        chest.setItem(i, st);
                        break;
                    }
        }
    }

    private static void furnish29(ServerLevel level, RandomSource random) {
        for (int i = 0; i < CAGES_29.length; i++) cage(level, i == 2 ? ModEntities.LURKER.get() : ModEntities.ACOLYTE.get(), CAGES_29[i]);
        for (Room r : new Room[]{GALLERY, BELL_W, BELL_E, PILLARS, VALVE_W, VALVE_E, BELL_N, VALVE_N, CRYPT})
            spawn(level, ModEntities.ACOLYTE.get(), local(r.x + random.nextInt(5) - 2, 3, r.z + random.nextInt(5) - 2));
        for (int i = 0; i < 2; i++) spawn(level, ModEntities.LURKER.get(), local(PILLARS.x - 5 + i * 10, 4, PILLARS.z));
        // the Keepers' chest on the dais: shut, and only his key opens it
        level.setBlock(CHEST_AT, ModBlocks.KEEPERS_CHEST.get().defaultBlockState()
                .setValue(KeepersChest.FACING, net.minecraft.core.Direction.SOUTH).setValue(KeepersChest.WATERLOGGED, true), Block.UPDATE_ALL);
        RandomizableContainer.setBlockEntityLootTable(level, random, CHEST_AT, LOOT);
        level.setBlock(MARK_29, Blocks.CHISELED_DEEPSLATE.defaultBlockState(), FLAGS);
    }

    /** He is beaten: his chest fills again and locks, waiting for the key he let fall. */
    public static void lockTreasure(ServerLevel level) {
        BlockState s = level.getBlockState(CHEST_AT);
        if (!(s.getBlock() instanceof KeepersChest)) return;
        level.setBlock(CHEST_AT, s.setValue(KeepersChest.OPEN, false), Block.UPDATE_ALL);
        if (level.getBlockEntity(CHEST_AT) instanceof KeepersChest.Entity chest) {
            // filled at once, round anything left in it (a pending table would not save what is already inside)
            chest.unpackLootTable(null);
            chest.setLootTable(LOOT, level.getRandom().nextLong());
            chest.unpackLootTable(null);
            chest.setChanged();
        }
    }

    private static void chest(ServerLevel level, RandomSource random, BlockPos at, ResourceKey<LootTable> loot, net.minecraft.core.Direction facing) {
        level.setBlock(at, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing).setValue(ChestBlock.WATERLOGGED, true), FLAGS);
        RandomizableContainer.setBlockEntityLootTable(level, random, at, loot);
    }

    /** Raises ({@code shut}) or lifts gate {@code i}: iron bars across the last passage. */
    static void setGate(ServerLevel level, int i, boolean shut) {
        BlockState bars = Blocks.IRON_BARS.defaultBlockState().setValue(net.minecraft.world.level.block.IronBarsBlock.WATERLOGGED, true)
                .setValue(net.minecraft.world.level.block.IronBarsBlock.EAST, true).setValue(net.minecraft.world.level.block.IronBarsBlock.WEST, true);
        for (int x = -1; x <= 1; x++)
            for (int y = 1; y <= 4; y++) level.setBlock(local(x, y, GATES_Z[i]), shut ? bars : Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
    }

    public static boolean gateShut(ServerLevel level, int i) {
        return level.getBlockState(local(0, 2, GATES_Z[i])).is(Blocks.IRON_BARS);
    }

    private static void cage(ServerLevel level, EntityType<?> type, BlockPos at) {
        level.setBlock(at, Blocks.SPAWNER.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity c) {
            c.setEntityId(type, level.getRandom());
            c.setChanged();
        }
    }

    private static void spawn(ServerLevel level, EntityType<? extends Mob> type, BlockPos at) {
        Mob m = type.create(level, EntitySpawnReason.STRUCTURE);
        if (m == null) return;
        m.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        m.setPersistenceRequired();
        level.addFreshEntity(m);
    }

    // ------------------------------------------------------------------ the valves, the bells, the Keeper

    /** A valve was turned: as many gates are lifted as valves stand open. Returns that number. */
    public static int valveTurned(ServerLevel level, BlockPos at) {
        int open = 0;
        for (BlockPos v : VALVE_AT) {
            BlockState s = level.getBlockState(v);
            if (s.getBlock() instanceof DepthsBlocks.Valve && s.getValue(DepthsBlocks.ON)) open++;
        }
        for (int i = 0; i < Math.min(open, GATES_Z.length); i++) {
            if (!gateShut(level, i)) continue;
            setGate(level, i, false);
            BlockPos g = local(0, 2, GATES_Z[i]);
            level.playSound(null, g, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 3.0F, 0.4F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE_COLUMN_UP, g.getX() + 0.5, g.getY() + 0.5, g.getZ() + 0.5, 40, 1.0, 1.2, 0.2, 0.1);
        }
        return open;
    }

    public static int bellsRung(ServerLevel level) {
        int rung = 0;
        for (BlockPos b : BELL_AT) {
            BlockState s = level.getBlockState(b);
            if (s.getBlock() instanceof DepthsBlocks.Bell && s.getValue(DepthsBlocks.ON)) rung++;
        }
        return rung;
    }

    /** A bell was rung; the fourth wakes the Keeper. Returns how many have rung. */
    public static int bellRung(ServerLevel level, BlockPos at, ServerPlayer who) {
        int rung = bellsRung(level);
        if (rung >= BELLS && keeper(level) == null) wake(level, who);
        return rung;
    }

    /** The Drowned Keeper, if he is awake. */
    public static DrownedKeeper keeper(ServerLevel level) {
        var list = level.getEntitiesOfClass(DrownedKeeper.class, bounds(), e -> e.isAlive());
        return list.isEmpty() ? null : list.get(0);
    }

    public static DrownedKeeper wake(ServerLevel level, ServerPlayer who) {
        DrownedKeeper k = ModEntities.DROWNED_KEEPER.get().create(level, EntitySpawnReason.EVENT);
        if (k == null) return null;
        k.snapTo(BOSS_AT.getX() + 0.5, BOSS_AT.getY(), BOSS_AT.getZ() + 0.5, 0, 0);
        k.setPersistenceRequired();
        k.addTag(KEEPER_TAG);
        level.addFreshEntity(k);
        level.playSound(null, BOSS_AT, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 3.0F, 0.5F);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, bounds())) {
            p.sendSystemMessage(Component.translatable("message.umbra.depths.wake").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            Journal.unlock(p, "drowned_keeper");
        }
        return k;
    }

    /** He is beaten: the bells go quiet and can be rung again. */
    public static void keeperDown(ServerLevel level) {
        lockTreasure(level);
        for (BlockPos b : BELL_AT) {
            BlockState s = level.getBlockState(b);
            if (s.getBlock() instanceof DepthsBlocks.Bell) level.setBlock(b, s.setValue(DepthsBlocks.ON, false), Block.UPDATE_ALL);
        }
    }

    /** Lit lamps within reach of the sanctum's middle (they are what he cannot bear). */
    public static int sanctumLamps(ServerLevel level) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(local(SANCTUM.x - SANCTUM.hx, 1, SANCTUM.z - SANCTUM.hz), local(SANCTUM.x + SANCTUM.hx, 1, SANCTUM.z + SANCTUM.hz))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof DepthsBlocks.Lamp && s.getValue(DepthsBlocks.ON)) n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ every second inside

    /** Every tick for every player: currents every tick, the rest once a second. */
    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !Underside.is(level) || !bounds().contains(player.position())) return;
        // the currents drag
        BlockPos l = player.blockPosition().subtract(ORIGIN);
        if (player.isInWater() && !player.isSpectator()) {
            for (int[] c : CURRENTS) {
                if (l.getX() < c[0] || l.getX() > c[2] || l.getZ() < c[1] || l.getZ() > c[3]) continue;
                if (!currentRuns(level, c[6])) break;
                player.push(c[4] * CURRENT, 0, c[5] * CURRENT);
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
                if (player.tickCount % 5 == 0)
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE, player.getX(), player.getY() + 1, player.getZ(), 4, 0.5, 0.5, 0.5, 0.1);
                if (level.getGameTime() % CURRENT_CYCLE == 0)
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.AMBIENT, 0.8F, 0.7F);
                break;
            }
        }
        if (player.tickCount % 20 != 9) return;
        Room r = roomAt(player.blockPosition());
        if (r == BELL_W) Journal.unlock(player, "depths_note_1");
        if (r == VALVE_E) Journal.unlock(player, "depths_note_2");
        if (r == SANCTUM) Journal.unlock(player, "depths_note_3");
        if (dev.umbra.sanity.Sanity.enabled() && DepthsBlocks.nearLitLamp(level, player.blockPosition(), 6)) dev.umbra.sanity.Sanity.add(player, 0.5F);
    }

    /**
     * 2.9: the currents are no trap any more. They pull in waves ({@link #CURRENT_ON} ticks of every {@link #CURRENT_CYCLE},
     * then calm water), a swimmer makes headway even against the pull, and once a cache has been emptied its current
     * dies away for good.
     */
    public static final double CURRENT = 0.024;
    public static final int CURRENT_CYCLE = 160, CURRENT_ON = 100;

    /** Is the current into cache {@code cache} pulling right now? */
    public static boolean currentRuns(ServerLevel level, int cache) {
        return currentRunsAt(level, cache, level.getGameTime());
    }

    /** Would it pull at game time {@code time} (in a wave, and its cache not yet emptied)? */
    public static boolean currentRunsAt(ServerLevel level, int cache, long time) {
        if (time % CURRENT_CYCLE >= CURRENT_ON) return false;
        return !(level.getBlockEntity(CACHE_CHEST[cache]) instanceof net.minecraft.world.Container c) || !c.isEmpty();
    }

    /** The old cages, before 2.9 (with {@link #CAGES_29}, the six of them). */
    public static final BlockPos[] CAGES_OLD = {local(GALLERY.x + 6, 1, GALLERY.z + 4), local(PILLARS.x - 9, 1, PILLARS.z), local(PILLARS.x + 9, 1, PILLARS.z + 6)};

    // ------------------------------------------------------------------ the key

    /** The Key of the Depths: used standing in water in the Underside it takes you down; in the Depths, back up. */
    public static class KeyItem extends Item {
        public KeyItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
            if (inside(sp)) {
                leave(server, sp);
                return InteractionResult.SUCCESS;
            }
            if (!Underside.is(server) || !sp.isInWater()) {
                sp.sendOverlayMessage(Component.translatable("message.umbra.depths.water").withStyle(ChatFormatting.DARK_AQUA));
                return InteractionResult.FAIL;
            }
            server.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
            enter(server, sp);
            sp.getCooldowns().addCooldown(sp.getItemInHand(hand), 60);
            return InteractionResult.SUCCESS;
        }
    }

    /** The Keepers' pearl, pinned to a chestplate: breath under water, and the dark weighs less on the mind. */
    public static void pearlTick(ServerPlayer player) {
        if (player.tickCount % 20 != 5 || !dev.umbra.gear.Gear.hasArtifact(player, dev.umbra.gear.Gear.KEEPERS_PEARL)) return;
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING, 60, 0, true, false, true));
    }

    /** For the journal's sense of where things are. */
    public static boolean near(ServerLevel level, BlockPos p) {
        return Underside.is(level) && bounds().inflate(16).contains(Vec3.atCenterOf(p));
    }

    static ServerLevel underside(net.minecraft.server.MinecraftServer server) {
        return server.getLevel(Underside.LEVEL);
    }
}
