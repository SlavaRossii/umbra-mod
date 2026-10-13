package dev.umbra;

import dev.umbra.darkness.DarknessManager;
import dev.umbra.light.DispellerItem;
import dev.umbra.quantum.MazeController;
import dev.umbra.quantum.MazePattern;
import dev.umbra.quantum.MazePatterns;
import dev.umbra.quantum.QuantumBlock;
import dev.umbra.registry.ModItems;
import dev.umbra.sanity.Hallucinations;
import dev.umbra.sanity.Sanity;
import dev.umbra.mob.Blind;
import dev.umbra.mob.Crawler;
import dev.umbra.mob.LightEater;
import dev.umbra.mob.Mimicker;
import dev.umbra.mob.Shade;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import dev.umbra.underside.RiftFrame;
import dev.umbra.underside.RiftTravel;
import dev.umbra.underside.Underside;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import dev.umbra.mimic.MimicBlock;
import dev.umbra.mimic.MimicManager;
import dev.umbra.quantum.QuantumMaze;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModEntities;
import dev.umbra.shadow.ShadowEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Self-test for CI: runs only when the JVM is started with -Dumbra.smoketest=true.
 * Exercises the server-side logic of every feature on a real dedicated server, writes a report and stops the server.
 */
final class SmokeTest {
    private static final int Y = 220;
    private static final BlockPos MAZE = new BlockPos(0, Y, 0);
    private static final BlockPos BOX = new BlockPos(40, Y, 0);
    private static final BlockPos MIMIC = new BlockPos(80, Y, 0);
    private static final BlockPos SHADOW = new BlockPos(100, Y, 0);
    private static final BlockPos PLATFORM = new BlockPos(130, Y + 20, 0);
    private static final int BOX_SIZE = 9;

    private static int tick;
    private static final List<String> failures = new ArrayList<>();
    private static final List<String> passes = new ArrayList<>();
    private static ShadowEntity shadow;
    private static int darknessAfterSpread;

    private SmokeTest() {}

    static void install() {
        Umbra.LOGGER.info("UMBRA SMOKETEST: enabled");
        NeoForge.EVENT_BUS.addListener(SmokeTest::onTick);
    }

    private static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        tick++;
        try {
            switch (tick) {
                case 20 -> setup(level);
                case 40 -> testMaze(level);
                case 60 -> testMimic(level);
                case 80 -> spawnShadow(level);
                case 120 -> testShadowGone();
                case 140 -> seedDarkness(level);
                case 160 -> spreadDarkness(level);
                case 180 -> placeLight(level);
                case 260 -> recedeDarkness(level);
                case 270 -> testDispeller(level);
                case 280 -> testPatterns(level);
                case 290 -> testSanity(level);
                case 300 -> testOre(level);
                case 310 -> testFrame(level);
                case 320 -> testUnderside(server, level);
                case 340 -> spawnMobs(level);
                case 350 -> testShadeLit(level);
                case 460 -> checkMobs(level);
                case 470 -> testDrops(level);
                case 490 -> finish(server);
                default -> { }
            }
        } catch (Throwable t) {
            Umbra.LOGGER.error("UMBRA SMOKETEST: exception at tick {}", tick, t);
            failures.add("exception at tick " + tick + ": " + t);
            finish(server);
        }
    }

    private static void check(boolean ok, String what) {
        (ok ? passes : failures).add(what);
        Umbra.LOGGER.info("UMBRA SMOKETEST: {} {}", ok ? "ok  " : "FAIL", what);
    }

    private static void setup(ServerLevel level) {
        for (int cx = -2; cx <= 11; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
            }
        }
        // sealed stone box for the darkness test
        for (int x = 0; x < BOX_SIZE; x++)
            for (int y = 0; y < BOX_SIZE; y++)
                for (int z = 0; z < BOX_SIZE; z++) {
                    boolean shell = x == 0 || y == 0 || z == 0 || x == BOX_SIZE - 1 || y == BOX_SIZE - 1 || z == BOX_SIZE - 1;
                    level.setBlock(BOX.offset(x, y, z), shell ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
        level.setBlock(MIMIC.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        for (int x = -15; x <= 15; x++)
            for (int z = -15; z <= 15; z++)
                level.setBlock(PLATFORM.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        check(true, "setup");
    }

    private static void testMaze(ServerLevel level) {
        QuantumMaze.build(level, MAZE, level.getRandom());
        int walls = 0, quantum = 0, ladders = 0;
        for (int x = 0; x < QuantumMaze.SIZE; x++)
            for (int z = 0; z < QuantumMaze.SIZE; z++)
                for (int y = 0; y < QuantumMaze.HEIGHT; y++) {
                    BlockState s = level.getBlockState(MAZE.offset(x, y, z));
                    if (s.is(ModBlocks.MAZE_WALL.get())) walls++;
                    if (s.is(ModBlocks.QUANTUM_BLOCK.get()) && s.getValue(QuantumBlock.LINKED)) quantum++;
                    if (s.is(Blocks.LADDER)) ladders++;
                }
        int mid = QuantumMaze.SIZE / 2;
        check(walls > 2000, "maze walls built (" + walls + ")");
        check(quantum > 100, "maze has linked quantum walls (" + quantum + ")");
        check(ladders >= 4, "maze has ladders between floors (" + ladders + " blocks)");
        check(level.getBlockState(MAZE.offset(mid, 1, mid)).is(Blocks.CHEST), "ground floor chest");
        check(level.getBlockState(MAZE.offset(mid, QuantumMaze.floorBase(1) + 1, mid)).is(Blocks.CHEST), "upper floor chest");
        check(level.getBlockState(MAZE.offset(mid, -1, mid)).is(ModBlocks.MAZE_CORE.get()), "maze core under the centre");
        check(ModBlocks.MAZE_WALL.get().defaultBlockState().getDestroySpeed(level, MAZE) < 0, "maze walls are unbreakable");
    }

    private static void testPatterns(ServerLevel level) {
        BlockPos core = MAZE.offset(QuantumMaze.SIZE / 2, -1, QuantumMaze.SIZE / 2);
        for (MazePattern p : MazePatterns.ALL) {
            if (p == MazePatterns.HUNT) continue; // needs a real player inside
            int changed = MazeController.runForTest(level, core, p, 12);
            check(changed > 0, "maze pattern '" + p.id() + "' changes walls (" + changed + ")");
        }
    }

    private static void testDispeller(ServerLevel level) {
        int before = countDarkness(level);
        Vec3 origin = Vec3.atCenterOf(BOX.offset(BOX_SIZE - 2, BOX_SIZE / 2, BOX_SIZE / 2));
        int burned = DispellerItem.flash(level, null, origin, new Vec3(-1, 0, 0));
        check(before == 0 || burned > 0, "dispeller burns living darkness (" + burned + " of " + before + ")");
    }

    private static void testSanity(ServerLevel level) {
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.snapTo(PLATFORM.getX() + 0.5, PLATFORM.getY(), PLATFORM.getZ() + 0.5, 0, 0);
        Sanity.set(fake, 50);
        check(Math.abs(Sanity.get(fake) - 50) < 0.01, "sanity can be set");
        Sanity.set(fake, 150);
        check(Sanity.get(fake) == Sanity.MAX, "sanity is capped at 100");
        Sanity.set(fake, 20);
        fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SEDATIVE.get(), 2));
        ModItems.SEDATIVE.get().use(level, fake, InteractionHand.MAIN_HAND);
        check(Math.abs(Sanity.get(fake) - 50) < 0.01, "sedative restores 30 sanity (" + Sanity.get(fake) + ")");
        check(fake.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "a pill is used up");
        check(Sanity.dread(fake) > 1.0F, "low sanity makes things nastier");
        check(Hallucinations.spawnFigure(level, fake, level.getRandom()), "a hallucination can appear");
        Sanity.set(fake, Sanity.MAX);
    }

    private static final BlockPos FRAME = new BlockPos(170, Y, 0);

    private static void testFrame(ServerLevel level) {
        // hand-built frame, then open it with the shard logic
        BlockState dusk = ModBlocks.DUSK_STONE.get().defaultBlockState();
        for (int x = -1; x <= 2; x++)
            for (int y = -1; y <= 3; y++)
                level.setBlock(FRAME.offset(x, y, 0), (x == -1 || x == 2 || y == -1 || y == 3) ? dusk : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        check(RiftFrame.tryOpen(level, FRAME.offset(1, 1, 0)), "dusk stone frame opens a rift");
        check(level.getBlockState(FRAME).is(ModBlocks.RIFT.get()) && level.getBlockState(FRAME.offset(1, 2, 0)).is(ModBlocks.RIFT.get()), "rift fills the whole frame");
        level.setBlock(FRAME.offset(-1, 1, 0), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        check(!level.getBlockState(FRAME.offset(0, 1, 0)).is(ModBlocks.RIFT.get()), "breaking the frame closes the rift");
    }

    private static void testUnderside(MinecraftServer server, ServerLevel overworld) {
        ServerLevel under = server.getLevel(Underside.LEVEL);
        check(under != null, "the Underside dimension exists");
        if (under == null) return;
        int ash = 0, bedrock = 0, gloom = 0;
        for (int cx = 0; cx < 3; cx++)
            for (int cz = 0; cz < 3; cz++) {
                under.getChunk(cx, cz);
                for (BlockPos p : BlockPos.betweenClosed(cx * 16, 0, cz * 16, cx * 16 + 15, 127, cz * 16 + 15)) {
                    BlockState s = under.getBlockState(p);
                    if (s.is(ModBlocks.ASH_STONE.get()) || s.is(ModBlocks.ASH_SOIL.get())) ash++;
                    else if (s.is(Blocks.BEDROCK)) bedrock++;
                    else if (s.is(ModBlocks.GLOOM_ORE.get())) gloom++;
                }
            }
        check(ash > 10000, "the Underside is made of ash (" + ash + " blocks)");
        check(bedrock > 100, "the Underside has a bedrock floor and roof (" + bedrock + ")");
        check(gloom > 0, "gloom ore generates in the Underside (" + gloom + ")");

        FakePlayer fake = FakePlayerFactory.getMinecraft(overworld);
        TeleportTransition t = RiftTravel.destination(overworld, fake, new BlockPos(300, 80, 300));
        check(t != null && t.newLevel() == under, "a rift leads to the Underside");
        if (t != null) {
            BlockPos arrival = BlockPos.containing(t.position());
            boolean riftNear = false;
            for (BlockPos p : BlockPos.betweenClosed(arrival.offset(-4, -4, -4), arrival.offset(4, 4, 4))) {
                if (under.getBlockState(p).is(ModBlocks.RIFT.get())) { riftNear = true; break; }
            }
            check(riftNear, "a return rift is built on arrival");
            check(under.getBlockState(arrival).isAir() && under.getBlockState(arrival.below()).isSolid(), "arrival spot is safe (" + arrival.toShortString() + ")");
            TeleportTransition back = RiftTravel.destination(under, fake, arrival);
            check(back != null && back.newLevel() == overworld, "the rift leads back to the world");
            TeleportTransition again = RiftTravel.destination(overworld, fake, new BlockPos(300, 80, 300));
            check(again != null && again.position().distanceTo(t.position()) < 1.0, "the same rift is reused next time");
        }
    }

    private static Crawler crawler;
    private static Blind blind;
    private static LightEater eater;
    private static Mimicker mimicker;
    private static final BlockPos TORCH = PLATFORM.offset(8, 0, 0);

    private static <T extends Mob> T spawn(ServerLevel level, net.minecraft.world.entity.EntityType<T> type, BlockPos at) {
        T mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (mob == null) return null;
        mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        return mob;
    }

    private static void spawnMobs(ServerLevel level) {
        crawler = spawn(level, ModEntities.CRAWLER.get(), PLATFORM.offset(-10, 0, -10));
        blind = spawn(level, ModEntities.BLIND.get(), PLATFORM.offset(-10, 0, 10));
        mimicker = spawn(level, ModEntities.MIMICKER.get(), PLATFORM.offset(10, 0, 10));
        level.setBlock(TORCH, Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
        eater = spawn(level, ModEntities.LIGHT_EATER.get(), PLATFORM.offset(3, 1, 0));
        check(crawler != null && blind != null && mimicker != null && eater != null, "all creatures can be created");
        check(mimicker != null && mimicker.isHidden(), "the mimicker starts invisible");
    }

    private static void testShadeLit(ServerLevel level) {
        Shade shade = spawn(level, ModEntities.SHADE.get(), PLATFORM.offset(0, 0, -6));
        check(shade != null, "a shade can be created");
        if (shade != null) {
            DispellerItem.flash(level, null, Vec3.atCenterOf(PLATFORM.offset(0, 1, 0)), new Vec3(0, 0, -1));
            check(!shade.isAlive(), "the Dispeller destroys a shade");
        }
    }

    private static void checkMobs(ServerLevel level) {
        check(crawler != null && crawler.isAlive(), "crawler lives and ticks");
        check(blind != null && blind.isAlive() && blind.getTarget() == null, "the blind one does not see anyone");
        check(mimicker != null && mimicker.isAlive(), "mimicker lives and ticks");
        check(eater != null && eater.isAlive(), "light eater lives and ticks");
        check(!level.getBlockState(TORCH).is(Blocks.TORCH), "the light eater ate the torch");
    }

    private static void testDrops(ServerLevel level) {
        int ok = 0;
        BlockPos[] spots = {PLATFORM.offset(-5, 0, 5), PLATFORM.offset(-3, 0, 5), PLATFORM.offset(-1, 0, 5)};
        Block[] blocks = {ModBlocks.LUMINITE_LAMP.get(), ModBlocks.ETERNAL_TORCH.get(), ModBlocks.DEAD_LOG.get()};
        for (int i = 0; i < spots.length; i++) {
            level.setBlock(spots[i], blocks[i].defaultBlockState(), Block.UPDATE_ALL);
            level.destroyBlock(spots[i], true);
            if (!level.getEntitiesOfClass(ItemEntity.class, new AABB(spots[i]).inflate(1.5)).isEmpty()) ok++;
        }
        check(ok == spots.length, "blocks drop themselves when broken (" + ok + "/" + spots.length + ")");
    }

    private static void testOre(ServerLevel level) {
        int ores = 0;
        for (int cx = -2; cx <= 11; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                for (BlockPos p : BlockPos.betweenClosed(cx * 16, -64, cz * 16, cx * 16 + 15, 24, cz * 16 + 15)) {
                    BlockState s = level.getBlockState(p);
                    if (s.is(ModBlocks.LUMINITE_ORE.get()) || s.is(ModBlocks.DEEPSLATE_LUMINITE_ORE.get())) ores++;
                }
            }
        }
        check(ores > 0, "luminite ore generates underground (" + ores + " blocks in 70 chunks)");
    }

    private static void testMimic(ServerLevel level) {
        level.setBlock(MIMIC, Blocks.CRAFTING_TABLE.defaultBlockState(), Block.UPDATE_ALL);
        check(MimicManager.tryMimic(level, MIMIC), "crafting table can be replaced");
        BlockState s = level.getBlockState(MIMIC);
        check(s.getBlock() instanceof MimicBlock, "mimic placed");
        if (s.getBlock() instanceof MimicBlock mimic) {
            mimic.expose(level, MIMIC);
            check(level.getBlockState(MIMIC).is(Blocks.CRAFTING_TABLE), "exposed mimic turns back into a crafting table");
        }
        MimicManager.tryMimic(level, MIMIC);
        if (level.getBlockState(MIMIC).getBlock() instanceof MimicBlock mimic) {
            mimic.spring(level, MIMIC);
            check(level.getBlockState(MIMIC).isAir(), "sprung mimic leaves air");
            int fish = level.getEntitiesOfClass(Silverfish.class, new AABB(MIMIC).inflate(4)).size();
            check(fish > 0, "sprung mimic releases silverfish (" + fish + ")");
        } else {
            check(false, "second mimic placed");
        }
    }

    private static void spawnShadow(ServerLevel level) {
        shadow = ModEntities.SHADOW.get().create(level, EntitySpawnReason.EVENT);
        check(shadow != null, "shadow entity created");
        if (shadow != null) {
            shadow.snapTo(SHADOW.getX() + 0.5, SHADOW.getY(), SHADOW.getZ() + 0.5, 0, 0);
            check(level.addFreshEntity(shadow), "shadow added to the world");
        }
    }

    private static void testShadowGone() {
        // nobody manages this shadow, so it must fade by itself
        check(shadow != null && !shadow.isAlive(), "unowned shadow dissipates");
    }

    private static void seedDarkness(ServerLevel level) {
        BlockPos center = BOX.offset(BOX_SIZE / 2, BOX_SIZE / 2, BOX_SIZE / 2);
        boolean seeded = dev.umbra.darkness.LivingDarknessBlock.seed(level, center, ModBlocks.LIVING_DARKNESS.get());
        check(seeded, "darkness can be seeded in a sealed dark box");
        if (!seeded) {
            Umbra.LOGGER.info("UMBRA SMOKETEST: sky={} block={}", dev.umbra.util.UmbraUtil.skyLight(level, center), dev.umbra.util.UmbraUtil.blockLight(level, center));
        }
    }

    private static void spreadDarkness(ServerLevel level) {
        randomTicks(level, 60);
        darknessAfterSpread = countDarkness(level);
        check(darknessAfterSpread > 30, "darkness spreads (" + darknessAfterSpread + " blocks)");
    }

    private static void placeLight(ServerLevel level) {
        level.setBlock(BOX.offset(1, 1, 1), Blocks.GLOWSTONE.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void recedeDarkness(ServerLevel level) {
        randomTicks(level, 20);
        int after = countDarkness(level);
        check(after < darknessAfterSpread, "darkness recedes from light (" + darknessAfterSpread + " -> " + after + ")");
        int seeded = DarknessManager.seedCluster(level, BOX.offset(6, 6, 6), 5);
        check(seeded >= 0, "seedCluster runs");
    }

    private static void randomTicks(ServerLevel level, int rounds) {
        for (int r = 0; r < rounds; r++) {
            List<BlockPos> dark = new ArrayList<>();
            for (BlockPos p : BlockPos.betweenClosed(BOX, BOX.offset(BOX_SIZE - 1, BOX_SIZE - 1, BOX_SIZE - 1))) {
                if (level.getBlockState(p).is(ModBlocks.LIVING_DARKNESS.get())) dark.add(p.immutable());
            }
            for (BlockPos p : dark) {
                BlockState s = level.getBlockState(p);
                if (s.is(ModBlocks.LIVING_DARKNESS.get())) s.randomTick(level, p, level.getRandom());
            }
        }
    }

    private static int countDarkness(ServerLevel level) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(BOX, BOX.offset(BOX_SIZE - 1, BOX_SIZE - 1, BOX_SIZE - 1))) {
            if (level.getBlockState(p).is(ModBlocks.LIVING_DARKNESS.get())) n++;
        }
        return n;
    }

    private static void finish(MinecraftServer server) {
        String verdict = failures.isEmpty() ? "PASSED" : "FAILED";
        StringBuilder report = new StringBuilder("UMBRA SMOKETEST: " + verdict + "\n");
        for (String p : passes) report.append("ok   ").append(p).append('\n');
        for (String f : failures) report.append("FAIL ").append(f).append('\n');
        Umbra.LOGGER.info("\n{}", report);
        try {
            Files.writeString(Path.of("umbra-smoketest.txt"), report.toString());
        } catch (IOException e) {
            Umbra.LOGGER.error("could not write report", e);
        }
        tick = Integer.MIN_VALUE; // stop scheduling
        server.halt(false);
    }
}
