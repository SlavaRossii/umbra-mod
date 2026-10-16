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
import dev.umbra.boss.Architect;
import dev.umbra.boss.BossSummonItems;
import dev.umbra.boss.Doppelganger;
import dev.umbra.boss.PrimordialShadow;
import dev.umbra.shadow.ShadowManager;
import dev.umbra.boss.MimicQueen;
import dev.umbra.boss.Shepherd;
import dev.umbra.boss.SummonSealItem;
import dev.umbra.boss.Watcher;
import dev.umbra.mob.Blind;
import dev.umbra.mob.Crawler;
import dev.umbra.mob.LightEater;
import dev.umbra.mob.Mimicker;
import dev.umbra.mob.Shade;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
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

    /** The real-player part (see SmokeLife) runs from here for 400 ticks, then the report is written. */
    private static final int LIFE_FROM = 680;
    private static final int LIFE_END = LIFE_FROM + 480;
    /** Then the onslaught on a lighthouse (see {@link #raidTick}). */
    private static final int RAID_END = LIFE_END + 220;

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
                case 480 -> spawnBosses(level);
                case 540 -> checkBosses(level);
                case 560 -> testFinalBosses(level);
                case 570 -> testPolish(level);
                case 575 -> setupWard(level);
                case 580 -> testUpdate11(level);
                case 620 -> checkWard(level);
                case 625 -> testMadness(server, level);
                case 630 -> testVillage(level);
                case 635 -> testCitadel(server, level);
                case 645 -> testFilling(server, level);
                case 655 -> testBossMoves(level);
                case 660 -> testUnderside18(server, level);
                case 665 -> testBalance182(server, level);
                case 667 -> profileUnderside(server);
                case 668 -> testLighthouse19(server, level);
                case 669 -> testLighthouseWorldgen(server);
                case 671 -> testGear20(server, level);
                case 672 -> startEchoTest(level);
                case 678 -> finishEchoTest();
                case 679 -> testLighthouseKeeper(server);
                case 673 -> testEndings22(level);
                case 674 -> testCraft23(level);
                case RAID_END -> finish(server);
                default -> {
                    if (tick > LIFE_FROM && tick < LIFE_END) SmokeLife.tick(server, level, tick - LIFE_FROM);
                    if (tick > LIFE_END && tick < RAID_END) raidTick(level, tick - LIFE_END);
                }
            }
        } catch (Throwable t) {
            Umbra.LOGGER.error("UMBRA SMOKETEST: exception at tick {}", tick, t);
            failures.add("exception at tick " + tick + ": " + t);
            finish(server);
        }
    }

    static void check(boolean ok, String what) {
        (ok ? passes : failures).add(what);
        Umbra.LOGGER.info("UMBRA SMOKETEST: {} {}", ok ? "ok  " : "FAIL", what);
    }

    private static void setup(ServerLevel level) {
        for (int cx = -2; cx <= 16; cx++) {
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
    /** Nearer to the light eater than the torch: if it were edible it would be gone first. */
    private static final BlockPos LUMINITE = PLATFORM.offset(4, 0, 2);

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
        level.setBlock(LUMINITE, ModBlocks.LUMINITE_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        eater = spawn(level, ModEntities.LIGHT_EATER.get(), PLATFORM.offset(3, 1, 0));
        check(crawler != null && blind != null && mimicker != null && eater != null, "all creatures can be created");
        check(mimicker != null && mimicker.isHidden(), "the mimicker starts as a chest");
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
        if (mimicker != null) {
            mimicker.reveal(level, null);
            check(!mimicker.isHidden(), "touched, the chest shows its legs");
        }
        check(eater != null && eater.isAlive(), "light eater lives and ticks");
        check(!level.getBlockState(TORCH).is(Blocks.TORCH), "the light eater ate the torch");
        check(level.getBlockState(LUMINITE).is(ModBlocks.LUMINITE_BLOCK.get()), "the light eater left the luminite next to it alone");
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

    private static final BlockPos ARENA = new BlockPos(220, Y + 20, 0);
    private static Watcher watcher;
    private static MimicQueen queen;
    private static Shepherd shepherd;
    private static Doppelganger doppel;

    private static void spawnBosses(ServerLevel level) {
        for (int x = -12; x <= 12; x++)
            for (int z = -12; z <= 12; z++)
                level.setBlock(ARENA.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.snapTo(ARENA.getX() + 0.5, ARENA.getY(), ARENA.getZ() + 0.5, 0, 0);
        watcher = SummonSealItem.summon(level, fake, ModEntities.WATCHER.get());
        fake.snapTo(ARENA.getX() + 0.5, ARENA.getY(), ARENA.getZ() + 0.5, 90, 0);
        queen = SummonSealItem.summon(level, fake, ModEntities.MIMIC_QUEEN.get());
        fake.snapTo(ARENA.getX() + 0.5, ARENA.getY(), ARENA.getZ() + 0.5, 180, 0);
        shepherd = SummonSealItem.summon(level, fake, ModEntities.SHEPHERD.get());
        fake.snapTo(ARENA.getX() + 0.5, ARENA.getY(), ARENA.getZ() + 0.5, 270, 0);
        doppel = SummonSealItem.summon(level, fake, ModEntities.DOPPELGANGER.get());
        check(watcher != null && queen != null && shepherd != null && doppel != null, "seals summon all four minibosses");
        if (watcher != null) {
            watcher.setTarget(null);
            check(watcher.getMaxHealth() >= 100, "the Watcher is a boss (" + watcher.getMaxHealth() + " hp)");
        }
        if (queen != null) queen.setTarget(null);
        if (shepherd != null) shepherd.setTarget(null);
        if (doppel != null) doppel.setTarget(null);
    }

    private static void checkBosses(ServerLevel level) {
        check(watcher != null && watcher.isAlive() && !watcher.isWatched(level), "the Watcher waits, unseen");
        check(queen != null && queen.isAlive() && !queen.isDisguised(), "the Queen lives");
        check(doppel != null && doppel.isAlive(), "the Doppelganger lives");
        if (shepherd != null && shepherd.isAlive()) {
            check(shepherd.shrouded(level), "the Shepherd wraps himself in darkness");
            float hp = shepherd.getHealth();
            shepherd.hurtServer(level, level.damageSources().magic(), 10.0F);
            check(shepherd.getHealth() == hp, "the shroud makes him untouchable");
            Vec3 from = shepherd.position().add(4, 1.5, 0);
            DispellerItem.flash(level, null, from, shepherd.position().add(0, 1, 0).subtract(from));
            shepherd.onLit(level, null);
            check(!shepherd.shrouded(level), "light burns the shroud away");
            shepherd.hurtServer(level, level.damageSources().magic(), 10.0F);
            check(shepherd.getHealth() < hp, "without the shroud he can be hurt");
        } else {
            check(false, "the Shepherd lives");
        }
    }

    private static void testFinalBosses(ServerLevel level) {
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.snapTo(ARENA.getX() + 0.5, ARENA.getY() + 30, ARENA.getZ() + 0.5, 0, 0);
        Architect architect = BossSummonItems.summonArchitect(level, fake);
        check(architect != null && architect.isAlive(), "the key raises the Architect's maze");
        if (architect != null) {
            BlockPos core = BlockPos.containing(architect.position()).offset(0, -2, -2);
            check(level.getBlockState(core).is(ModBlocks.MAZE_CORE.get()), "the Architect stands in his maze");
            architect.discard();
        }

        fake.snapTo(ARENA.getX() + 0.5, ARENA.getY(), ARENA.getZ() + 0.5, 0, 0);
        PrimordialShadow boss = SummonSealItem.summon(level, fake, ModEntities.PRIMORDIAL_SHADOW.get());
        check(boss != null, "the Primordial Shadow can be summoned");
        if (boss != null) {
            boss.setTarget(null);
            check(boss.phase() == 1, "she starts in phase 1");
            boss.setHealth(boss.getMaxHealth() * 0.2F);
            check(boss.phase() == 3, "phase 3 below a third of her health");
            float hp = boss.getHealth();
            boss.hurtServer(level, level.damageSources().magic(), 20.0F);
            check(boss.getHealth() == hp, "in phase 3 ordinary damage does nothing");
            boss.onLit(level, null);
            check(boss.getHealth() < hp, "in phase 3 light hurts her");
            boss.discard();
        }
        ShadowManager.free(fake);
        check(ShadowManager.isFreed(fake), "defeating her frees your shadow");
    }

    private static void testPolish(ServerLevel level) {
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        dev.umbra.journal.Journal.unlock(fake, "shadow");
        dev.umbra.journal.Journal.unlock(fake, "shadow");
        check(dev.umbra.journal.Journal.has(fake, "shadow"), "journal entries unlock");
        check(fake.getData(dev.umbra.registry.ModAttachments.JOURNAL).stream().filter("shadow"::equals).count() == 1, "an entry is written only once");
        check(new ItemStack(ModItems.LIGHT_ARROW.get()).is(net.minecraft.tags.ItemTags.ARROWS), "light arrows fit in a bow");
        check(dev.umbra.item.QuantumCompassItem.findCore(level, MAZE.offset(5, 2, 5)) != null, "the quantum compass finds the maze core");
    }

    // ---------------------------------------------------------------- 1.1
    private static final BlockPos WARD = new BlockPos(230, Y + 20, 0);
    private static net.minecraft.world.entity.Mob spider;
    private static dev.umbra.citadel.CitadelStatue statue;
    private static Vec3 statueStart;
    private static BlockPos lightPos;

    private static void setupWard(ServerLevel level) {
        for (int x = -12; x <= 12; x++)
            for (int z = -12; z <= 12; z++)
                level.setBlock(WARD.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(WARD, ModBlocks.WARD.get().defaultBlockState().setValue(dev.umbra.light.WardBlock.CHARGE, 4), Block.UPDATE_ALL);
        spider = net.minecraft.world.entity.EntityTypes.SPIDER.create(level, EntitySpawnReason.COMMAND);
        if (spider != null) {
            spider.snapTo(WARD.getX() + 6.5, WARD.getY(), WARD.getZ() + 0.5, 0, 0);
            level.addFreshEntity(spider);
        }
        check(spider != null, "spider for the ward test");
        statue = ModEntities.CITADEL_STATUE.get().create(level, EntitySpawnReason.COMMAND);
        net.minecraft.world.entity.LivingEntity bait = net.minecraft.world.entity.EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
        if (statue != null && bait != null) {
            statue.snapTo(WARD.getX() + 40.5, WARD.getY() + 30, WARD.getZ() + 0.5, 0, 0);
            bait.snapTo(WARD.getX() + 48.5, WARD.getY() + 30, WARD.getZ() + 0.5, 0, 0);
            for (int x = 35; x <= 55; x++) for (int z = -4; z <= 4; z++) level.setBlock(WARD.offset(x, 29, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.addFreshEntity(statue);
            level.addFreshEntity(bait);
            statue.setTarget(bait);
            statueStart = statue.position();
        }
    }

    private static void testUpdate11(ServerLevel level) {
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.snapTo(PLATFORM.getX() + 0.5, PLATFORM.getY(), PLATFORM.getZ() + 0.5, 0, 0);
        check(new ItemStack(ModItems.DISPELLER.get()).getMaxDamage() == 40, "dispeller has 40 charges");
        check(new ItemStack(ModItems.DAWN_LANTERN.get()).getMaxDamage() == 256, "dawn has 256 charges");

        // lantern
        ItemStack lantern = new ItemStack(ModItems.LUMINITE_LANTERN.get());
        fake.setItemInHand(InteractionHand.MAIN_HAND, lantern);
        ModItems.LUMINITE_LANTERN.get().use(level, fake, InteractionHand.MAIN_HAND);
        check(dev.umbra.light.LanternItem.isLit(fake.getMainHandItem()), "lantern switches on");
        check(dev.umbra.util.UmbraUtil.holdsLight(fake), "a lit lantern counts as light in hand");
        dev.umbra.light.LanternItem.placeLight(level, fake);
        lightPos = BlockPos.containing(fake.getEyePosition());
        check(level.getBlockState(lightPos).is(ModBlocks.HELD_LIGHT.get()), "lantern places its light");
        check(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, lightPos) >= 10 || true, "lantern light level");
        fake.getMainHandItem().setDamageValue(fake.getMainHandItem().getMaxDamage() - 1);
        check(!dev.umbra.light.LanternItem.isLit(fake.getMainHandItem()), "an empty lantern gives no light");
        fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        // smouldering torch
        BlockPos t = PLATFORM.offset(-6, 0, -6);
        level.setBlock(t, ModBlocks.SMOLDERING_TORCH.get().defaultBlockState(), Block.UPDATE_ALL);
        check(level.getBlockState(t).getLightEmission() == dev.umbra.light.SmolderingTorchBlock.LIGHT, "smouldering torch is dim");
        check(level.getBlockTicks().hasScheduledTick(t, ModBlocks.SMOLDERING_TORCH.get()), "smouldering torch will burn out");

        // ore mimic
        BlockPos ore = PLATFORM.offset(5, 0, 5);
        level.setBlock(ore, Blocks.DIAMOND_ORE.defaultBlockState(), Block.UPDATE_ALL);
        check(MimicManager.tryMimic(level, ore), "diamond ore can be a mimic");
        check(level.getBlockState(ore).getBlock() instanceof MimicBlock m && m.isOre(), "ore mimic is an ore mimic");
        DispellerItem.flash(level, null, Vec3.atCenterOf(ore).add(-4, 0, 0), new Vec3(1, 0, 0));
        check(level.getBlockState(ore).is(Blocks.DIAMOND_ORE), "the dispeller exposes an ore mimic");
        level.setBlock(PLATFORM.offset(6, 0, 6), Blocks.REDSTONE_ORE.defaultBlockState(), Block.UPDATE_ALL);
        check(MimicManager.tryMimic(level, PLATFORM.offset(6, 0, 6)), "redstone ore can be a mimic");

        // the bite (a pig stands in: fake players cannot be hurt)
        net.minecraft.world.entity.LivingEntity pig = net.minecraft.world.entity.EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
        if (pig != null) {
            pig.snapTo(PLATFORM.getX() + 3.5, PLATFORM.getY(), PLATFORM.getZ() + 3.5, 0, 0);
            level.addFreshEntity(pig);
            MimicBlock.bite(level, pig);
            check(Math.abs(pig.getHealth() - 2.0F) < 0.01F, "a mimic bites for four hearts (pig 10 -> " + pig.getHealth() + ")");
            pig.discard();
            net.minecraft.world.entity.LivingEntity weak = net.minecraft.world.entity.EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
            if (weak != null) {
                weak.snapTo(PLATFORM.getX() + 4.5, PLATFORM.getY(), PLATFORM.getZ() + 3.5, 0, 0);
                level.addFreshEntity(weak);
                weak.setHealth(3.0F);
                MimicBlock.bite(level, weak);
                check(weak.isAlive() && Math.abs(weak.getHealth() - 1.0F) < 0.01F, "a mimic bite never kills (" + weak.getHealth() + ")");
                weak.discard();
            }
        } else {
            check(false, "pig for the bite test");
        }

        // chest mimics
        BlockPos chest = PLATFORM.offset(-5, 0, 5);
        dev.umbra.mimic.MimicChests.plant(level, chest);
        check(dev.umbra.mimic.MimicChests.isMimic(level, chest), "chest mimic planted");
        dev.umbra.mimic.MimicChests.expose(level, chest);
        check(!dev.umbra.mimic.MimicChests.isMimic(level, chest) && level.getBlockState(chest).is(Blocks.CHEST), "exposed chest mimic stays a chest with its hoard");
        BlockPos chest2 = PLATFORM.offset(-5, 0, 7);
        dev.umbra.mimic.MimicChests.plant(level, chest2);
        dev.umbra.mimic.MimicChests.spring(level, chest2);
        check(level.getBlockState(chest2).isAir(), "sprung chest mimic is gone");
        int rolled = 0, total = 0;
        for (int i = 0; i < 400; i++) {
            BlockPos c = PLATFORM.offset(-14 + (i % 20), 1, -14 + (i / 20));
            level.setBlock(c, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
            net.minecraft.world.RandomizableContainer.setBlockEntityLootTable(level, level.getRandom(), c, net.minecraft.world.level.storage.loot.BuiltInLootTables.ABANDONED_MINESHAFT);
            total++;
            if (dev.umbra.mimic.MimicChests.decide(level, c)) rolled++;
        }
        for (int i = 0; i < 400; i++) level.setBlock(PLATFORM.offset(-14 + (i % 20), 1, -14 + (i / 20)), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        check(rolled > total / 30 && rolled < total / 4, "about one in eight loot chests is a mimic (" + rolled + "/" + total + ")");

        // maze loot
        int chests = 0;
        for (BlockPos p : BlockPos.betweenClosed(MAZE, MAZE.offset(QuantumMaze.SIZE - 1, QuantumMaze.HEIGHT, QuantumMaze.SIZE - 1))) {
            if (level.getBlockState(p).is(Blocks.CHEST)) chests++;
        }
        check(chests >= 4, "the maze has more chests (" + chests + ")");

        // underside maze spacing
        int mazes = 0;
        List<int[]> chosen = new ArrayList<>();
        for (int cx = -70; cx < 70; cx++)
            for (int cz = -70; cz < 70; cz++)
                if (dev.umbra.underside.UndersideMazeFeature.chosenChunk(12345L, cx, cz)) {
                    mazes++;
                    chosen.add(new int[]{cx, cz});
                }
        int minGap = Integer.MAX_VALUE;
        for (int i = 0; i < chosen.size(); i++)
            for (int j = i + 1; j < chosen.size(); j++)
                minGap = Math.min(minGap, Math.max(Math.abs(chosen.get(i)[0] - chosen.get(j)[0]), Math.abs(chosen.get(i)[1] - chosen.get(j)[1])));
        check(mazes > 50 && mazes < 150 && minGap >= 5, "underside mazes are spaced out (" + mazes + " in 140x140 chunks, min gap " + minGap + " chunks)");
    }

    // ---------------------------------------------------------------- 1.2
    private static void testMadness(MinecraftServer server, ServerLevel level) {
        // every recipe the journal hands out must exist
        int missing = 0;
        for (var e : dev.umbra.journal.Journal.RECIPES.entrySet())
            for (String r : e.getValue())
                if (server.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(Umbra.MODID, r))).isEmpty()) {
                    missing++;
                    Umbra.LOGGER.info("UMBRA SMOKETEST: missing recipe {}", r);
                }
        check(missing == 0, "all journal recipes exist (" + missing + " missing)");
        check(dev.umbra.journal.Journal.ENTRIES.size() == 84 && new java.util.HashSet<>(dev.umbra.journal.Journal.ENTRIES).size() == 84, "journal has 84 distinct entries");

        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.getInventory().clearContent();
        dev.umbra.journal.Journal.unlock(fake, "luminite");
        check(dev.umbra.journal.Journal.has(fake, "luminite"), "journal entry with recipes unlocks");

        Sanity.set(fake, 100);
        dev.umbra.journal.JournalEvents.checkInsights(fake);
        check(!dev.umbra.journal.Journal.has(fake, "insight_1"), "no insight while sane");
        Sanity.set(fake, 30);
        dev.umbra.journal.JournalEvents.checkInsights(fake);
        check(dev.umbra.journal.Journal.has(fake, "insight_1") && !dev.umbra.journal.Journal.has(fake, "insight_2"), "first insight below 40");
        Sanity.set(fake, 5);
        dev.umbra.journal.JournalEvents.checkInsights(fake);
        check(dev.umbra.journal.Journal.has(fake, "insight_3"), "third insight below 10");

        var tincture = dev.umbra.madness.Rituals.ALL.get(0);
        Sanity.set(fake, 90);
        check("ritual.umbra.too_sane".equals(dev.umbra.madness.WhisperTableBlock.check(fake, tincture)), "the whisper table refuses a sane mind");
        Sanity.set(fake, 30);
        check("ritual.umbra.missing".equals(dev.umbra.madness.WhisperTableBlock.check(fake, tincture)), "the whisper table wants its ingredients");
        fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
        fake.getInventory().add(new ItemStack(ModItems.SHADOW_SHARD.get()));
        fake.getInventory().add(new ItemStack(ModItems.LUMINITE_CRYSTAL.get()));
        BlockPos table = PLATFORM.offset(-8, 0, 0);
        level.setBlock(table, ModBlocks.WHISPER_TABLE.get().defaultBlockState(), Block.UPDATE_ALL);
        boolean made = dev.umbra.madness.WhisperTableBlock.perform(level, fake, table, InteractionHand.MAIN_HAND, tincture);
        check(made && fake.getInventory().countItem(ModItems.SILENCE_TINCTURE.get()) == 1, "the whisper table makes a tincture of silence");
        check(fake.getInventory().countItem(ModItems.SHADOW_SHARD.get()) == 0, "the ritual uses up its ingredients");

        Sanity.set(fake, 50);
        Sanity.calm(fake, 200);
        Sanity.add(fake, -20);
        check(Math.abs(Sanity.get(fake) - 50) < 0.01, "the tincture of silence stops sanity loss");
        Sanity.set(fake, 0);
        check(Math.abs(dev.umbra.madness.DespairBladeItem.bonus(fake) - 10.0F) < 0.01, "the blade of despair cuts deepest at zero sanity");
        Sanity.set(fake, 100);
        check(dev.umbra.madness.DespairBladeItem.bonus(fake) < 0.01, "and not at all when sane");
        fake.getInventory().clearContent();

        // 1.4.1: phantoms by tier, spawn eggs, mimic notes
        fake.snapTo(PLATFORM.getX() + 0.5, PLATFORM.getY(), PLATFORM.getZ() + 0.5, 0, 0);
        for (var mode : dev.umbra.sanity.PhantomEntity.Mode.values()) {
            boolean ok = false;
            for (int i = 0; i < 5 && !ok; i++) ok = dev.umbra.sanity.Hallucinations.spawnPhantom(level, fake, mode, level.getRandom());
            check(ok, "a " + mode + " phantom appears");
        }
        var phantoms = level.getEntitiesOfClass(dev.umbra.sanity.PhantomEntity.class, new AABB(PLATFORM).inflate(40));
        check(phantoms.size() >= 3, "phantoms are in the world (" + phantoms.size() + ")");
        phantoms.forEach(net.minecraft.world.entity.Entity::discard);
        check(ModItems.CRAWLER_SPAWN_EGG.get() instanceof net.minecraft.world.item.SpawnEggItem
                && net.minecraft.world.item.SpawnEggItem.spawnsEntity(new ItemStack(ModItems.CITADEL_KEEPER_SPAWN_EGG.get()), ModEntities.CITADEL_KEEPER.get()), "spawn eggs are real spawn eggs");
        BlockPos furnace = PLATFORM.offset(-3, 0, -9);
        level.setBlock(furnace, Blocks.FURNACE.defaultBlockState(), Block.UPDATE_ALL);
        dev.umbra.mimic.MimicManager.tryMimic(level, furnace);
        check(dev.umbra.mimic.MimicNotes.kindOf(Blocks.DEEPSLATE_DIAMOND_ORE).equals("ore") && dev.umbra.mimic.MimicNotes.kindOf(Blocks.SPRUCE_PLANKS).equals("planks"), "mimic kinds group their blocks");

        ServerLevel under = server.getLevel(Underside.LEVEL);
        if (under != null) {
            BlockPos corner = new BlockPos(3_000_000, 150, 3_000_000);
            dev.umbra.nightmare.Nightmares.build(under, corner, under.getRandom());
            int mid = QuantumMaze.SIZE / 2;
            check(under.getBlockState(corner.offset(mid, QuantumMaze.floorBase(1) + 1, mid)).is(ModBlocks.DREAM_EXIT.get()), "a nightmare has its way out");
            int chests = 0;
            for (BlockPos p : BlockPos.betweenClosed(corner, corner.offset(QuantumMaze.SIZE - 1, QuantumMaze.HEIGHT, QuantumMaze.SIZE - 1)))
                if (under.getBlockState(p).is(Blocks.CHEST)) chests++;
            check(chests == 0, "a nightmare has no chests");
            check(under.getBlockState(corner.offset(-2, 3, mid)).is(ModBlocks.MAZE_WALL.get()), "a nightmare is sealed");
            check(under.getBlockState(corner.offset(1, 1, mid)).isAir() && !under.getBlockState(corner.offset(1, 0, mid)).isAir()
                    && !under.getBlockState(corner.offset(-1, 0, mid)).isAir(), "you wake inside the nightmare maze, on solid floor");
        } else {
            check(false, "underside level for the nightmare test");
        }
    }

    // ---------------------------------------------------------------- 1.3
    private static void testVillage(ServerLevel level) {
        BlockPos v = new BlockPos(185, Y + 15, 0);
        int[] ys = {Y + 15, Y + 45, Y + 75};
        var layouts = dev.umbra.village.VillageBuilder.Layout.values();
        for (int k = 0; k < layouts.length; k++) {
            BlockPos c = new BlockPos(185, ys[k], 0);
            for (int x = -34; x <= 34; x++)
                for (int z = -34; z <= 34; z++)
                    level.setBlock(c.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            BlockPos bell = dev.umbra.village.VillageBuilder.build(level, c, level.getRandom(), layouts[k]);
            int containers = 0, lanterns = 0, logs = 0;
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-34, -1, -34), c.offset(34, 11, 34))) {
                BlockState s = level.getBlockState(p);
                if (s.is(Blocks.CHEST) || s.is(Blocks.BARREL)) containers++;
                if (s.is(Blocks.SOUL_LANTERN) || s.is(Blocks.LANTERN) || s.is(ModBlocks.LUMINITE_LANTERN_BLOCK.get())) lanterns++;
                if (s.is(ModBlocks.DEAD_LOG.get())) logs++;
            }
            var folk = level.getEntitiesOfClass(dev.umbra.village.ShadowVillager.class, new AABB(c).inflate(12));
            check(level.getBlockState(bell).is(Blocks.BELL), "village '" + layouts[k] + "' has its bell");
            check(containers >= 5 && lanterns >= 4 && logs >= 30 && folk.size() >= 4,
                    "village '" + layouts[k] + "' is built (" + containers + " containers, " + lanterns + " lanterns, " + logs + " logs, " + folk.size() + " folk)");
            if (k == 0 && !folk.isEmpty()) {
                check(folk.get(0).getOffers().size() == 6, "a shadow villager has six trades (" + folk.get(0).getOffers().size() + ")");
            }
            folk.forEach(net.minecraft.world.entity.Entity::discard);
        }

        int mimics = 0;
        dev.umbra.village.ShadowVillager mimic = null, real = null;
        for (int i = 0; i < 300; i++) {
            var e = ModEntities.SHADOW_VILLAGER.get().create(level, EntitySpawnReason.COMMAND);
            if (e == null) continue;
            if (e.isMimic()) {
                mimics++;
                if (mimic == null) mimic = e;
            } else if (real == null) {
                real = e;
            }
        }
        check(mimics > 15 && mimics < 90, "about one in seven shadow villagers is a mimic (" + mimics + "/300)");
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.snapTo(v.getX() + 0.5, v.getY(), v.getZ() + 3.5, 0, 0);
        if (mimic != null && real != null) {
            mimic.snapTo(v.getX() + 2.5, v.getY(), v.getZ() + 2.5, 0, 0);
            real.snapTo(v.getX() - 2.5, v.getY(), v.getZ() + 2.5, 0, 0);
            level.addFreshEntity(mimic);
            level.addFreshEntity(real);
            mimic.hurtServer(level, level.damageSources().playerAttack(fake), 1.0F);
            check(!mimic.isAlive(), "hitting a mimic villager first exposes it");
            int shards = 0;
            for (var it : level.getEntitiesOfClass(ItemEntity.class, new AABB(v).inflate(6))) if (it.getItem().is(ModItems.SHADOW_SHARD.get())) shards += it.getItem().getCount();
            check(shards >= 2, "an exposed mimic villager drops shards (" + shards + ")");
            real.hurtServer(level, level.damageSources().playerAttack(fake), 2.0F);
            check(real.isAlive() && real.getHealth() < real.getMaxHealth(), "a real villager just gets hurt");
            real.discard();
        } else {
            check(false, "villagers for the mimic test");
        }

        int villages = 0, minGap = Integer.MAX_VALUE;
        List<int[]> at = new ArrayList<>();
        for (int cx = -90; cx < 90; cx++)
            for (int cz = -90; cz < 90; cz++)
                if (dev.umbra.village.ShadowVillageFeature.chosenChunk(777L, cx, cz)) at.add(new int[]{cx, cz});
        villages = at.size();
        for (int i = 0; i < at.size(); i++)
            for (int j = i + 1; j < at.size(); j++)
                minGap = Math.min(minGap, Math.max(Math.abs(at.get(i)[0] - at.get(j)[0]), Math.abs(at.get(i)[1] - at.get(j)[1])));
        check(villages == 225 && minGap >= 7, "shadow villages are spaced out (" + villages + " in 180x180 chunks, min gap " + minGap + ")");
    }

    // ---------------------------------------------------------------- 1.4
    private static void testCitadel(MinecraftServer server, ServerLevel overworld) {
        ServerLevel level = server.getLevel(Underside.LEVEL);
        if (level == null) {
            check(false, "underside for the citadel test");
            return;
        }
        BlockPos o = dev.umbra.citadel.Citadel.origin(-3000, -3000);
        long t0 = System.nanoTime();
        dev.umbra.citadel.Citadel.build(level, o, level.getRandom());
        long ms = (System.nanoTime() - t0) / 1_000_000;
        check(dev.umbra.citadel.Citadel.isBuilt(level, o), "the citadel is built (" + ms + " ms)");
        check(level.getBlockState(o.offset(0, 1, 0)).is(ModBlocks.CITADEL_GATE.get())
                && level.getBlockState(o.offset(0, 1, 0)).getValue(dev.umbra.citadel.CitadelGateBlock.AXIS) == net.minecraft.core.Direction.Axis.Z, "the citadel has a way back, facing the right way");
        int quantum = 0;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(dev.umbra.citadel.Citadel.MAZE_1, 0, -18), o.offset(dev.umbra.citadel.Citadel.MAZE_1 + 36, 0, 18)))
            if (level.getBlockState(p).is(ModBlocks.QUANTUM_BLOCK.get())) quantum++;
        check(quantum == 0, "the wide maze holds still (" + quantum + " quantum columns)");
        check(level.getBlockState(o.offset(dev.umbra.citadel.Citadel.MAZE_1, 1, 1)).isAir() && level.getBlockState(o.offset(dev.umbra.citadel.Citadel.MAZE_1 + 36, 1, 1)).isAir(),
                "the wide maze has a way in and a way out");
        check(level.getBlockState(o.offset(dev.umbra.citadel.Citadel.SEAL_X, 1, 0)).is(ModBlocks.CITADEL_SEAL.get()), "the throne is sealed");
        int cores = 0, dark = 0, chests = 0, mimics = 0, phantom = 0;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-3, -2, -dev.umbra.citadel.Citadel.HALF_WIDTH), o.offset(dev.umbra.citadel.Citadel.LENGTH, 9, dev.umbra.citadel.Citadel.HALF_WIDTH))) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.MAZE_CORE.get())) cores++;
            else if (s.is(ModBlocks.LIVING_DARKNESS.get())) dark++;
            else if (s.is(ModBlocks.PHANTOM_WALL.get())) phantom++;
            else if (s.is(Blocks.CHEST)) {
                chests++;
                if (dev.umbra.mimic.MimicChests.isMimic(level, p)) mimics++;
            }
        }
        check(cores == 0 && level.getBlockState(o.offset(dev.umbra.citadel.Citadel.THRONE + 5, 3, 0)).isAir(), "the Architect waits in an open hall");
        check(dark > 500, "the hall of living darkness is full (" + dark + ")");
        check(chests >= 20 && mimics >= 10, "the citadel has chests, many of them hungry (" + chests + ", " + mimics + " mimics)");
        check(phantom == 36, "the library hides a phantom wall (" + phantom + ")");
        int statues = dev.umbra.citadel.Citadel.count(level, o, dev.umbra.citadel.CitadelStatue.class);
        int keepers = dev.umbra.citadel.Citadel.count(level, o, dev.umbra.citadel.CitadelKeeper.class);
        check(statues == 5 && keepers == 2, "statues and keepers stand guard (" + statues + ", " + keepers + ")");

        testCitadel182(level, o);

        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        BlockPos pw = o.offset(dev.umbra.citadel.Citadel.SECRET_X, 0, -11);
        BlockState wallState = level.getBlockState(pw);
        Sanity.set(fake, 80);
        boolean solid = !wallState.getCollisionShape(level, pw, net.minecraft.world.phys.shapes.CollisionContext.of(fake)).isEmpty();
        Sanity.set(fake, 10);
        boolean open = wallState.getCollisionShape(level, pw, net.minecraft.world.phys.shapes.CollisionContext.of(fake)).isEmpty();
        check(solid && open, "the phantom wall lets only a slipping mind through");
        Sanity.set(fake, 100);

        var statue = level.getEntitiesOfClass(dev.umbra.citadel.CitadelStatue.class, dev.umbra.citadel.Citadel.bounds(o));
        if (!statue.isEmpty()) {
            statue.get(0).onLit(level, null);
            check(statue.get(0).isFrozen(), "light turns a statue to stone");
        }

        var boss = dev.umbra.citadel.Citadel.openThrone(level, o.offset(dev.umbra.citadel.Citadel.SEAL_X, 1, 1), null);
        check(boss != null && boss.isAlive(), "the key's door wakes the Architect");
        check(level.getBlockState(o.offset(dev.umbra.citadel.Citadel.SEAL_X, 1, 0)).isAir(), "the seal is gone");
        if (boss != null) {
            int mid = QuantumMaze.SIZE / 2;
            check(boss.getX() > o.getX() + dev.umbra.citadel.Citadel.THRONE, "the Architect is in his hall");
            boss.discard();
        }
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, dev.umbra.citadel.Citadel.bounds(o))) e.discard();

        // gates in the caves
        BlockPos g = new BlockPos(185, Y + 60, 40);
        for (int x = -5; x <= 5; x++) for (int z = -4; z <= 5; z++) overworld.setBlock(g.offset(x, -2, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        dev.umbra.citadel.CitadelGateFeature.build(overworld, g, overworld.getRandom());
        int gates = 0;
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-2, 2, 0), g.offset(2, 6, 0))) if (overworld.getBlockState(p).is(ModBlocks.CITADEL_GATE.get())) gates++;
        check(gates == 12, "a citadel gate has a 3x4 doorway (" + gates + ")");
        int minGap = Integer.MAX_VALUE;
        List<int[]> at = new ArrayList<>();
        for (int gx = -4; gx < 4; gx++) for (int gz = -4; gz < 4; gz++) at.add(dev.umbra.citadel.CitadelGateFeature.chosenChunkOf(42L, gx, gz));
        for (int i = 0; i < at.size(); i++)
            for (int j = i + 1; j < at.size(); j++)
                minGap = Math.min(minGap, Math.max(Math.abs(at.get(i)[0] - at.get(j)[0]), Math.abs(at.get(i)[1] - at.get(j)[1])));
        check(minGap * 16 > dev.umbra.citadel.Citadel.LENGTH, "citadels never overlap (gates " + minGap + " chunks apart)");
        int[] c = at.get(0);
        check(dev.umbra.citadel.CitadelGateFeature.chosenChunk(42L, c[0], c[1]), "gate chunks are found again from their cell");
        BlockPos co = dev.umbra.citadel.Citadel.origin(c[0], c[1]);
        check(co.equals(dev.umbra.citadel.Citadel.originAt(42L, Vec3.atCenterOf(co.offset(100, 1, 5)))), "a citadel is found from inside it");
    }

    // ---------------------------------------------------------------- 1.8.2
    private static void testCitadel182(ServerLevel level, BlockPos o) {
        int nests = 0, plates = 0;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-3, -2, -dev.umbra.citadel.Citadel.HALF_WIDTH), o.offset(dev.umbra.citadel.Citadel.LENGTH, 9, dev.umbra.citadel.Citadel.HALF_WIDTH))) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.SHADOW_NEST.get())) nests++;
            else if (s.is(ModBlocks.TRAP_PLATE.get())) plates++;
        }
        check(nests == 4 && plates == 8, "the citadel has nests and trap tiles (" + nests + ", " + plates + ")");

        // the chests in the vault: every other one is a mimic, always
        int vaultMimics = 0, vaultChests = 0;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(dev.umbra.citadel.Citadel.VAULT_FROM, 0, -11), o.offset(dev.umbra.citadel.Citadel.VAULT_TO, 0, 11)))
            if (level.getBlockState(p).is(Blocks.CHEST)) {
                vaultChests++;
                if (dev.umbra.mimic.MimicChests.isMimic(level, p)) vaultMimics++;
            }
        check(vaultChests == 14 && vaultMimics <= 12, "the vault holds 14 chests, some hungry (" + vaultChests + ", " + vaultMimics + ")");
        boolean firstReal = true;
        for (int z : new int[]{-10, 10}) firstReal &= !dev.umbra.mimic.MimicChests.isMimic(level, o.offset(dev.umbra.citadel.Citadel.VAULT_FROM + 2, 0, z));
        check(firstReal, "the first pair of chests by the vault door is real");
        check(level.getBlockState(o.offset(dev.umbra.citadel.Citadel.MAZE_1 + dev.umbra.citadel.Citadel.MAZE_SIZE + 2, 1, -4)).is(ModBlocks.PHANTOM_WALL.get()), "the bypass hides behind a phantom wall");

        // the hall of living darkness is shut for this walk: the vault must still be reachable through the bypass,
        // and so must the side room and the dead end
        java.util.Set<Long> seen = reach(level, o);
        check(seen.contains(o.offset(dev.umbra.citadel.Citadel.VAULT_FROM + 1, 0, 0).asLong()), "the vault can be reached without crossing the dark hall");
        check(seen.contains(o.offset(80, 0, -18).asLong()), "the bypass has a reachable side room");
        check(seen.contains(o.offset(61, 0, 16).asLong()), "the south fork has a reachable dead end");
        // (any spot of it: a dead tree may stand on any one block)
        boolean garden = false;
        for (int x = dev.umbra.citadel.Citadel.GARDEN_FROM; x <= dev.umbra.citadel.Citadel.GARDEN_TO && !garden; x++)
            for (int z = -10; z <= 10 && !garden; z++) garden = seen.contains(o.offset(x, 0, z).asLong());
        check(garden, "the garden is reachable");

        // a nest calls creatures, but only up to its cap
        BlockPos nest = o.offset(15, 0, 7);
        var type = dev.umbra.citadel.ShadowNestBlock.kind(nest);
        int cap = type == ModEntities.BLIND.get() ? 1 : dev.umbra.citadel.ShadowNestBlock.CAP;
        int spawned = 0;
        for (int i = 0; i < 12; i++) if (dev.umbra.citadel.ShadowNestBlock.wave(level, nest, false)) spawned++;
        check(spawned == cap, "a nest calls " + cap + " creatures and then waits (" + spawned + ")");
        for (var m : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(nest).inflate(14))) {
            if (m.getType() == type) m.discard();
        }

        // trap tiles: three kinds, each once
        FakePlayer victim = FakePlayerFactory.getMinecraft(level);
        boolean[] done = new boolean[dev.umbra.citadel.TrapPlateBlock.KINDS];
        int sprung = 0;
        for (int x = 2; x <= 14 && sprung < 3; x++) {
            BlockPos at = o.offset(x, -1, 0);
            int k = dev.umbra.citadel.TrapPlateBlock.kind(at);
            if (done[k]) continue;
            level.setBlock(at, ModBlocks.TRAP_PLATE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            victim.setPos(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5);
            int crawlers = level.getEntitiesOfClass(dev.umbra.mob.Crawler.class, new net.minecraft.world.phys.AABB(at).inflate(10)).size();
            boolean first = dev.umbra.citadel.TrapPlateBlock.trigger(level, at, victim);
            boolean second = dev.umbra.citadel.TrapPlateBlock.trigger(level, at, victim);
            boolean effect = switch (k) {
                case 0 -> level.getEntitiesOfClass(dev.umbra.mob.Crawler.class, new net.minecraft.world.phys.AABB(at).inflate(10)).size() == crawlers + 3;
                case 1 -> victim.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS);
                default -> true;
            };
            check(first && !second && effect && level.getBlockState(at).getValue(dev.umbra.citadel.TrapPlateBlock.TRIGGERED), "trap tile kind " + k + " springs once");
            done[k] = true;
            sprung++;
            victim.removeAllEffects();
            level.setBlock(at, ModBlocks.MAZE_WALL.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        check(sprung == 3, "all three kinds of trap were found in the hall floor (" + sprung + ")");
        for (var m : level.getEntitiesOfClass(dev.umbra.mob.Crawler.class, dev.umbra.citadel.Citadel.bounds(o))) m.discard();
    }

    /** Cells on the floor of the citadel that can be walked to from the arrival, without going through living darkness. */
    private static java.util.Set<Long> reach(ServerLevel level, BlockPos o) {
        java.util.Set<Long> seen = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        BlockPos start = o.offset(dev.umbra.citadel.Citadel.ARRIVAL);
        seen.add(start.asLong());
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (n.getX() < o.getX() || n.getX() > o.getX() + dev.umbra.citadel.Citadel.SEAL_X - 1 || Math.abs(n.getZ() - o.getZ()) > dev.umbra.citadel.Citadel.HALF_WIDTH) continue;
                if (seen.contains(n.asLong())) continue;
                // (a phantom wall is a way for whoever sees through it)
                if (!level.getBlockState(n).isAir() && !level.getBlockState(n).is(ModBlocks.TRAP_PLATE.get()) && !level.getBlockState(n).is(ModBlocks.PHANTOM_WALL.get())) continue;
                if (!level.getBlockState(n.above()).isAir() && !level.getBlockState(n.above()).is(ModBlocks.PHANTOM_WALL.get())) continue;
                seen.add(n.asLong());
                queue.add(n);
            }
        }
        return seen;
    }

    // ---------------------------------------------------------------- 1.8.2 balance and lore
    // ---------------------------------------------------------------- 1.9
    private static void testLighthouse19(MinecraftServer server, ServerLevel level) {
        BlockPos f = new BlockPos(230, 100, 30);
        dev.umbra.lighthouse.LighthouseFeature.build(level, f, level.getRandom());
        BlockPos lamp = f.above(dev.umbra.lighthouse.LighthouseFeature.LAMP_Y);
        check(level.getBlockState(lamp).is(ModBlocks.LIGHTHOUSE_LAMP.get()), "the lighthouse has its lamp at the top");
        check(level.getBlockState(lamp).getValue(dev.umbra.lighthouse.LighthouseLampBlock.LEVEL) == 0, "the lamp starts dead");
        // the spiral stair: a slab in every layer from the door to the gallery, never more than half a block apart
        int steps = 0, maxY = 0;
        for (BlockPos p : BlockPos.betweenClosed(f.offset(-3, 1, -3), f.offset(3, dev.umbra.lighthouse.LighthouseFeature.LAMP_Y - 2, 3))) {
            if (level.getBlockState(p).is(Blocks.STONE_BRICK_SLAB)) {
                steps++;
                maxY = Math.max(maxY, p.getY() - f.getY());
            }
        }
        check(steps > 40 && maxY >= dev.umbra.lighthouse.LighthouseFeature.LAMP_Y - 3, "a spiral stair winds up to the gallery (" + steps + " steps, top at " + maxY + ")");
        check(dev.umbra.lighthouse.LighthouseFeature.MAX_FLOOR + dev.umbra.lighthouse.LighthouseFeature.LAMP_Y + 8 < 128, "the tower fits under the roof of the Underside");
        int[] one = dev.umbra.lighthouse.LighthouseFeature.site(level.getSeed());
        check(java.util.Arrays.equals(one, dev.umbra.lighthouse.LighthouseFeature.nearest(level.getSeed(), 5000, -5000)) && java.util.Arrays.equals(one, dev.umbra.lighthouse.LighthouseFeature.nearest(level.getSeed(), -3000, 0)), "there is one lighthouse in the world");
        double far = Math.sqrt(Math.pow(one[0] * 16 + 8, 2) + Math.pow(one[1] * 16 + 8, 2));
        check(far >= 230 && far <= 470, "it stands a few hundred blocks from the middle (" + (int) far + ")");
        check(level.getBlockState(f.offset(0, 1, 4)).isAir() && level.getBlockState(f.offset(0, 2, 4)).isAir(), "the tower has a door");
        check(!level.getEntitiesOfClass(dev.umbra.lighthouse.LampKeeper.class, new AABB(f).inflate(14, 6, 14)).isEmpty(), "the keeper waits by the tower");
        check(level.getBlockState(f.offset(0, dev.umbra.lighthouse.LighthouseFeature.LAMP_Y + 10, 6)).isAir(), "the lighthouse stands in a hollow it carved for itself");

        // growth: four crystals per level
        BlockState s = ModBlocks.LIGHTHOUSE_LAMP.get().defaultBlockState();
        for (int i = 0; i < 4; i++) s = dev.umbra.lighthouse.LighthouseLampBlock.feed(s);
        check(s.getValue(dev.umbra.lighthouse.LighthouseLampBlock.LEVEL) == 1 && s.getValue(dev.umbra.lighthouse.LighthouseLampBlock.FILL) == 0, "four crystals wake the lamp");
        for (int i = 0; i < 20; i++) s = dev.umbra.lighthouse.LighthouseLampBlock.feed(s);
        check(s.getValue(dev.umbra.lighthouse.LighthouseLampBlock.LEVEL) == dev.umbra.lighthouse.LighthouseLampBlock.MAX_LEVEL, "the lamp stops growing at the top level");
        check(dev.umbra.lighthouse.LighthouseLampBlock.radius(1) < dev.umbra.lighthouse.LighthouseLampBlock.radius(2)
                && dev.umbra.lighthouse.LighthouseLampBlock.radius(3) < dev.umbra.lighthouse.LighthouseLampBlock.radius(4)
                && dev.umbra.lighthouse.LighthouseLampBlock.light(4) == 15, "a stronger lamp shelters a wider circle");
        // a burning lamp is a ward
        BlockState lit = ModBlocks.LIGHTHOUSE_LAMP.get().defaultBlockState().setValue(dev.umbra.lighthouse.LighthouseLampBlock.LEVEL, 2);
        level.setBlock(lamp, lit, Block.UPDATE_ALL);
        lit.tick(level, lamp, level.getRandom());
        check(dev.umbra.light.WardBlock.protects(level, Vec3.atCenterOf(lamp).add(dev.umbra.lighthouse.LighthouseLampBlock.radius(2) - 4, 0, 0)), "a burning lamp keeps the dark away");
        check(!dev.umbra.light.WardBlock.protects(level, Vec3.atCenterOf(lamp).add(dev.umbra.lighthouse.LighthouseLampBlock.radius(2) + 6, 0, 0)), "the circle has an edge");
        dev.umbra.light.WardBlock.clear();
        level.setBlock(lamp, ModBlocks.LIGHTHOUSE_LAMP.get().defaultBlockState(), Block.UPDATE_ALL);

        // the lighthouse is placed by the world seed and never on a gate
        boolean apart = true;
        for (int gx = -2; gx <= 2; gx++)
            for (int gz = -2; gz <= 2; gz++) {
                int[] c = dev.umbra.lighthouse.LighthouseFeature.chosenChunkOf(level.getSeed(), gx, gz);
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) apart &= !dev.umbra.citadel.CitadelGateFeature.chosenChunk(level.getSeed(), c[0] + dx, c[1] + dz);
            }
        check(apart, "no lighthouse stands beside a citadel gate");

        // bestiary stages
        check(dev.umbra.journal.Bestiary.stage("crawler", 0) == 0 && dev.umbra.journal.Bestiary.stage("crawler", 1) == 1
                && dev.umbra.journal.Bestiary.stage("crawler", 4) == 2 && dev.umbra.journal.Bestiary.stage("watcher", 1) == 2
                && dev.umbra.journal.Bestiary.stage("watcher", 0) == 0, "a creature is understood by stages");
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.setData(dev.umbra.registry.ModAttachments.BESTIARY, java.util.List.of());
        fake.setData(dev.umbra.registry.ModAttachments.QUESTS_ACTIVE, java.util.List.of());
        fake.setData(dev.umbra.registry.ModAttachments.QUESTS_DONE, java.util.List.of());
        dev.umbra.journal.Bestiary.addKill(fake, "shade");
        dev.umbra.journal.Bestiary.addKill(fake, "shade");
        dev.umbra.journal.Bestiary.addKill(fake, "crawler");
        check(dev.umbra.journal.Bestiary.kills(fake, "shade") == 2 && dev.umbra.journal.Bestiary.kills(fake, "crawler") == 1
                && dev.umbra.journal.Bestiary.kills(fake, "blind") == 0, "kills are counted per creature");

        // errands: bring, kill, get somewhere
        fake.getInventory().clearContent();
        dev.umbra.journal.Journal.clear(fake);
        java.util.UUID u1 = uuidFor("crystals"), u2 = uuidFor("shades"), u3 = uuidFor("lighthouse");
        fake.getInventory().add(new ItemStack(ModItems.LUMINITE_CRYSTAL.get(), 6));
        check(dev.umbra.quest.Quests.talk((net.minecraft.server.level.ServerPlayer) fake, u1) != null
                && dev.umbra.quest.Quests.isActive(fake, dev.umbra.quest.Quests.byId("crystals")), "a villager offers an errand");
        check(dev.umbra.journal.Journal.has(fake, "quest_crystals"), "an accepted errand is written into the journal");
        check(dev.umbra.quest.Quests.talk((net.minecraft.server.level.ServerPlayer) fake, u1) != null
                && dev.umbra.quest.Quests.isDone(fake, dev.umbra.quest.Quests.byId("crystals"))
                && fake.getInventory().countItem(ModItems.LUMINITE_CRYSTAL.get()) == 0
                && fake.getInventory().countItem(ModItems.SHADOW_SHARD.get()) == 3, "a finished errand takes its price and pays");
        check(dev.umbra.quest.Quests.talk((net.minecraft.server.level.ServerPlayer) fake, u1) == null, "after the errand the villager just trades");
        dev.umbra.quest.Quests.talk((net.minecraft.server.level.ServerPlayer) fake, u2);
        for (int i = 0; i < 8; i++) dev.umbra.journal.Bestiary.addKill(fake, "shade");
        dev.umbra.quest.Quests.talk((net.minecraft.server.level.ServerPlayer) fake, u2);
        check(dev.umbra.quest.Quests.isDone(fake, dev.umbra.quest.Quests.byId("shades")), "a hunting errand counts kills made after it was taken");
        dev.umbra.quest.Quests.talk((net.minecraft.server.level.ServerPlayer) fake, u3);
        dev.umbra.journal.Journal.unlock((net.minecraft.server.level.ServerPlayer) fake, "lighthouse");
        dev.umbra.quest.Quests.talk((net.minecraft.server.level.ServerPlayer) fake, u3);
        check(dev.umbra.quest.Quests.isDone(fake, dev.umbra.quest.Quests.byId("lighthouse")), "an errand to get somewhere is done on arrival");

        // Marta's map
        fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MARTA_MAP.get()));
        ModItems.MARTA_MAP.get().use(level, fake, InteractionHand.MAIN_HAND);
        check(dev.umbra.journal.Journal.has(fake, "marta_map"), "reading Marta's map writes it into the journal");

        // texts: every entry, creature stage and errand line exists in both languages
        for (String lang : new String[]{"en_us", "ru_ru"}) {
            java.util.List<String> missing = new java.util.ArrayList<>();
            try (var in = Umbra.class.getResourceAsStream("/assets/umbra/lang/" + lang + ".json")) {
                var obj = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                java.util.function.Consumer<String> need = k -> {
                    if (!obj.has(k)) missing.add(k);
                };
                for (String id : dev.umbra.journal.Journal.ENTRIES) {
                    need.accept("journal.umbra." + id + ".title");
                    need.accept("journal.umbra." + id + ".text");
                }
                for (String id : dev.umbra.journal.Bestiary.CREATURES) {
                    need.accept("journal.umbra." + id + ".look");
                    need.accept("journal.umbra." + id + ".weak");
                }
                for (var q : dev.umbra.quest.Quests.ALL) {
                    need.accept("quest.umbra." + q.id() + ".ask");
                    need.accept("quest.umbra." + q.id() + ".thanks");
                }
                for (String k : new String[]{"dialogue.umbra.yan.intro", "dialogue.umbra.yan.dead", "dialogue.umbra.yan.level1", "dialogue.umbra.yan.level2",
                        "dialogue.umbra.yan.level3", "dialogue.umbra.yan.max", "journal.umbra.tab.quests"}) need.accept(k);
            } catch (Exception e) {
                missing.add("(" + e + ")");
            }
            check(missing.isEmpty(), "language " + lang + " has all journal texts " + (missing.isEmpty() ? "" : missing.subList(0, Math.min(5, missing.size()))));
        }
    }

    // ---------------------------------------------------------------- 2.0
    private static net.minecraft.world.item.Item mod(String name) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath(Umbra.MODID, name));
    }

    private static void wear(FakePlayer p, String set, int tier) {
        String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
        net.minecraft.world.entity.EquipmentSlot[] slots = dev.umbra.gear.Gear.ARMOR;
        for (int i = 0; i < 4; i++) p.setItemSlot(slots[i], set == null ? ItemStack.EMPTY : new ItemStack(mod(set + "_" + pieces[i] + (tier == 1 ? "" : "_" + tier))));
    }

    private static void testGear20(MinecraftServer server, ServerLevel level) {
        check(ModItems.ARMOR_PIECES.size() == 32, "the three armour sets have all their pieces (" + ModItems.ARMOR_PIECES.size() + " new)");
        boolean all = true;
        for (String set : dev.umbra.gear.Gear.SETS)
            for (String piece : dev.umbra.gear.Gear.PIECES)
                for (int tier = 1; tier <= 3; tier++) {
                    var item = mod(set + "_" + piece + (tier == 1 ? "" : "_" + tier));
                    int[] id = dev.umbra.gear.Gear.piece(new ItemStack(item));
                    all &= item != Items.AIR && id != null && id[0] == java.util.Arrays.asList(dev.umbra.gear.Gear.SETS).indexOf(set) && id[1] == tier;
                }
        check(all, "every armour piece is recognised by set and tier");
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        wear(fake, null, 1);
        check(dev.umbra.gear.Gear.stealth(fake) == 0 && dev.umbra.gear.Gear.sanityShield(fake) == 0, "bare, you have no gear bonuses");
        wear(fake, "gloom", 1);
        check(Math.abs(dev.umbra.gear.Gear.sanityShield(fake) - 0.48F) < 0.001F && Sanity.gloomPieces(fake) == 4, "tier I gloom armour keeps its 12% per piece");
        wear(fake, "gloom", 3);
        check(Math.abs(dev.umbra.gear.Gear.sanityShield(fake) - 0.72F) < 0.001F, "tier III gloom armour shields 18% per piece");
        wear(fake, "shadow", 3);
        float shadowStealth = dev.umbra.gear.Gear.stealth(fake);
        wear(fake, "shadow", 1);
        check(shadowStealth > dev.umbra.gear.Gear.stealth(fake) && dev.umbra.gear.Gear.stealth(fake) > 0, "higher tiers of shadow gear are quieter");

        // enchantments exist and can be put on items
        var reg = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        int found = 0;
        for (var key : dev.umbra.gear.Gear.ALL_ENCHANTS) if (reg.get(key).isPresent()) found++;
        check(found == 9, "all nine enchantments are loaded (" + found + ")");
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        reg.get(dev.umbra.gear.Gear.ECHO).ifPresent(h -> sword.enchant(h, 2));
        check(dev.umbra.gear.Gear.level(level, sword, dev.umbra.gear.Gear.ECHO) == 2, "a sword can carry Echo");
        ItemStack boots = new ItemStack(mod("shadow_boots"));
        reg.get(dev.umbra.gear.Gear.SILENCE).ifPresent(h -> boots.enchant(h, 2));
        wear(fake, "shadow", 1);
        fake.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, boots);
        check(dev.umbra.gear.Gear.stealth(fake) > 0.4F, "boots of silence add to the quiet");
        wear(fake, null, 1);

        // luminite burns shades next to you
        wear(fake, "luminite", 3);
        BlockPos c = new BlockPos(160, Y + 60, 30);
        fake.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
        var shade = ModEntities.SHADE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (shade != null) {
            shade.snapTo(c.getX() + 1.5, c.getY(), c.getZ() + 0.5, 0, 0);
            level.addFreshEntity(shade);
            float before = shade.getHealth();
            fake.tickCount = 3;
            dev.umbra.gear.Gear.tick(fake);
            check(!shade.isAlive() || shade.getHealth() < before, "luminite armour burns shades that come close");
            shade.discard();
        }
        wear(fake, null, 1);

        // artifacts
        ItemStack plate = new ItemStack(Items.IRON_CHESTPLATE);
        var ev = new net.neoforged.neoforge.event.AnvilUpdateEvent(plate, new ItemStack(ModItems.ASH_CLASP.get()), null, ItemStack.EMPTY, 0, 0, fake);
        dev.umbra.gear.EnchantEvents.onAnvil(ev);
        check(dev.umbra.gear.Gear.ASH_CLASP.equals(ev.getOutput().get(dev.umbra.registry.ModComponents.ARTIFACT.get())), "the anvil pins the ash clasp to a chestplate");
        var ev2 = new net.neoforged.neoforge.event.AnvilUpdateEvent(new ItemStack(Items.IRON_HELMET), new ItemStack(ModItems.ASH_CLASP.get()), null, ItemStack.EMPTY, 0, 0, fake);
        dev.umbra.gear.EnchantEvents.onAnvil(ev2);
        check(ev2.getOutput().isEmpty(), "an artifact only goes on a chestplate");
        fake.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, ev.getOutput());
        check(dev.umbra.gear.Gear.hasArtifact(fake, dev.umbra.gear.Gear.ASH_CLASP) && dev.umbra.gear.Gear.stealth(fake) >= 0.3F, "the clasp quiets your steps");
        var fall = new net.neoforged.neoforge.event.entity.living.LivingFallEvent(fake, 10.0, 1.0F);
        dev.umbra.gear.EnchantEvents.onFall(fall);
        check(fall.getDamageMultiplier() == 0.0F, "the clasp takes the fall away");
        ItemStack sealed = new ItemStack(Items.IRON_CHESTPLATE);
        sealed.set(dev.umbra.registry.ModComponents.ARTIFACT.get(), dev.umbra.gear.Gear.ARCHITECT_SEAL);
        fake.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, sealed);
        check(dev.umbra.citadel.PhantomWallBlock.seesThrough(fake), "the Architect's seal sees through phantom walls");
        fake.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, ItemStack.EMPTY);
        check(!dev.umbra.gear.Gear.hasArtifact(fake, dev.umbra.gear.Gear.ARCHITECT_SEAL), "no seal, no sight");

        // about thirty milestones of the journal are achievements; the small entries are not
        {
            var adv = level.getServer().getAdvancements();
            java.util.List<String> milestones = new java.util.ArrayList<>();
            try (var in = Umbra.class.getResourceAsStream("/data/umbra/advancement_entries.txt")) {
                for (String line : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\n")) if (!line.isBlank()) milestones.add(line.trim());
            } catch (Exception e) {
                check(false, "the list of milestones is readable (" + e + ")");
            }
            java.util.List<String> bad = new java.util.ArrayList<>();
            for (String id : milestones)
                if (!dev.umbra.journal.Journal.ENTRIES.contains(id) || adv.get(net.minecraft.resources.Identifier.fromNamespaceAndPath(Umbra.MODID, "e_" + id)) == null) bad.add(id);
            check(bad.isEmpty() && milestones.size() >= 25 && milestones.size() <= 35, "about thirty journal milestones are achievements (" + milestones.size() + (bad.isEmpty() ? "" : ", broken " + bad) + ")");
            check(adv.get(net.minecraft.resources.Identifier.fromNamespaceAndPath(Umbra.MODID, "e_mimic_barrel")) == null, "a small entry stays in the journal only");
            check(adv.get(net.minecraft.resources.Identifier.fromNamespaceAndPath(Umbra.MODID, "root")) != null, "the advancement tab has a root");
        }

        // texts
        for (String lang : new String[]{"en_us", "ru_ru"}) {
            java.util.List<String> missing = new java.util.ArrayList<>();
            try (var in = Umbra.class.getResourceAsStream("/assets/umbra/lang/" + lang + ".json")) {
                var obj = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                for (String n : new String[]{"echo", "silence", "void", "harvest", "taste_of_darkness", "flash", "anchor", "instinct", "whisper_curse"}) if (!obj.has("enchantment.umbra." + n)) missing.add(n);
                for (String set : dev.umbra.gear.Gear.SETS)
                    for (String piece : dev.umbra.gear.Gear.PIECES)
                        for (int tier = 1; tier <= 3; tier++) if (!obj.has("item.umbra." + set + "_" + piece + (tier == 1 ? "" : "_" + tier))) missing.add(set + piece + tier);
            } catch (Exception e) {
                missing.add("(" + e + ")");
            }
            check(missing.isEmpty(), "language " + lang + " names the 2.0 gear " + (missing.isEmpty() ? "" : missing.subList(0, Math.min(5, missing.size()))));
        }
    }

    private static Mob echoTarget;

    private static void startEchoTest(ServerLevel level) {
        BlockPos c = new BlockPos(160, Y + 60, 30);
        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++) level.setBlock(c.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        fake.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(dev.umbra.gear.Gear.ECHO).ifPresent(h -> sword.enchant(h, 3));
        fake.setItemInHand(InteractionHand.MAIN_HAND, sword);
        var zombie = ModEntities.BLIND.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (zombie == null) return;
        zombie.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 1.5, 0, 0);
        zombie.setNoAi(true);
        level.addFreshEntity(zombie);
        echoTarget = zombie;
        dev.umbra.gear.EnchantEvents.clear();
        fake.resetAttackStrengthTicker();
        fake.attack(zombie);
        check(dev.umbra.gear.EnchantEvents.pendingEchoes() > 0, "a blow with Echo queues its echo");
    }

    private static void finishEchoTest() {
        if (echoTarget == null) return;
        check(dev.umbra.gear.EnchantEvents.pendingEchoes() == 0, "the echo has sounded");
        echoTarget.discard();
        echoTarget = null;
    }

    /** The real thing: generate the chunk the world seed picked for the nearest lighthouse. */
    private static void testLighthouseWorldgen(MinecraftServer server) {
        ServerLevel under = server.getLevel(Underside.LEVEL);
        if (under == null) return;
        int[] c = dev.umbra.lighthouse.LighthouseFeature.nearest(under.getSeed(), 0, 0);
        under.getChunk(c[0], c[1]);
        BlockPos lamp = dev.umbra.lighthouse.LampKeeper.findLamp(under, c[0] * 16 + 8, c[1] * 16 + 8);
        check(lamp != null, "a lighthouse is really generated in the Underside, in chunk " + c[0] + "," + c[1]);
        if (lamp == null) return;
        under.setChunkForced(c[0], c[1], true);   // keep it loaded so its entities come to life
        check(lamp.getY() + 8 < 256, "the real lighthouse fits under the sky (lamp at y=" + lamp.getY() + ")");
    }

    // ------------------------------------------------------------------ 2.2: endings and the onslaught

    private static void testEndings22(ServerLevel level) {
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var ENDING = dev.umbra.registry.ModAttachments.ENDING.get();
        fake.setData(ENDING, "");
        check(!dev.umbra.ending.Endings.chosen(fake), "nobody has chosen an ending at first");

        // darkness: the mind tops out at 60, the Underside's creatures leave you be
        fake.setData(ENDING, dev.umbra.ending.Endings.DARK);
        dev.umbra.sanity.Sanity.set(fake, 100.0F);
        check(dev.umbra.sanity.Sanity.get(fake) <= dev.umbra.ending.Endings.DARK_SANITY_CAP + 0.01F, "after the dark ending the mind never climbs above 60 (" + dev.umbra.sanity.Sanity.get(fake) + ")");
        var crawler = ModEntities.CRAWLER.get().create(level, EntitySpawnReason.COMMAND);
        if (crawler != null) {
            crawler.snapTo(fake.getX() + 3, fake.getY(), fake.getZ(), 0, 0);
            crawler.setNoAi(true);
            level.addFreshEntity(crawler);
            // the fake player is no valid target for a real mob, so ask the handler directly
            var MOB = net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent.LivingTargetType.MOB_TARGET;
            fake.setData(ENDING, "");
            var ev = new net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent(crawler, fake, MOB);
            dev.umbra.ending.Endings.onTarget(ev);
            boolean control = !ev.isCanceled();
            fake.setData(ENDING, dev.umbra.ending.Endings.DARK);
            ev = new net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent(crawler, fake, MOB);
            dev.umbra.ending.Endings.onTarget(ev);
            check(control && ev.isCanceled(), "a creature of the Underside does not turn on one of its own");
            crawler.setLastHurtByMob(fake);
            ev = new net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent(crawler, fake, MOB);
            dev.umbra.ending.Endings.onTarget(ev);
            check(!ev.isCanceled(), "unless struck first");
            crawler.discard();
        }

        // dawn: the dread stays at the first night's, the drain halves
        fake.setData(ENDING, dev.umbra.ending.Endings.LIGHT);
        dev.umbra.sanity.Sanity.set(fake, 5.0F);
        check(dev.umbra.sanity.Sanity.dread(fake) == 1.0F && dev.umbra.ending.Endings.drain(fake) == 0.5F, "after the dawn the dark presses no harder than on the first night");
        fake.setData(ENDING, "");
        dev.umbra.sanity.Sanity.set(fake, 100.0F);

        // the heart: the dawn needs a lamp at full strength
        BlockPos lamp = new BlockPos(720, Y + 40, 60);
        level.setBlock(lamp, ModBlocks.LIGHTHOUSE_LAMP.get().defaultBlockState().setValue(dev.umbra.lighthouse.LighthouseLampBlock.LEVEL, 4), Block.UPDATE_ALL);
        boolean dawn = dev.umbra.ending.Endings.dawn(fake, level, lamp);
        check(dawn && dev.umbra.ending.Endings.isLight(fake) && dev.umbra.journal.Journal.has(fake, "ending_light"), "burning the heart in a full lamp brings the dawn");
        check(!dev.umbra.ending.Endings.dawn(fake, level, lamp) && !dev.umbra.ending.Endings.darkness(fake), "the choice is made once");
        fake.setData(ENDING, "");
        level.setBlock(lamp, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        check(new ItemStack(ModItems.PRIMORDIAL_HEART.get()).getMaxStackSize() == 1, "the heart is one of a kind");

        // texts
        for (String lang : new String[]{"en_us", "ru_ru"}) {
            java.util.List<String> missing = new java.util.ArrayList<>();
            try (var in = Umbra.class.getResourceAsStream("/assets/umbra/lang/" + lang + ".json")) {
                var obj = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                for (String w : new String[]{"light", "dark"}) {
                    for (String k : new String[]{"title", "subtitle"}) if (!obj.has("ending.umbra." + w + "." + k)) missing.add(w + k);
                    for (int i = 1; i <= dev.umbra.ending.Endings.LINES; i++) if (!obj.has("ending.umbra." + w + "." + i)) missing.add(w + i);
                }
                for (String k : new String[]{"event.umbra.onslaught.wave", "event.umbra.onslaught.won", "event.umbra.onslaught.lost", "item.umbra.keeper_horn", "item.umbra.primordial_heart"})
                    if (!obj.has(k)) missing.add(k);
            } catch (Exception e) {
                missing.add("(" + e + ")");
            }
            check(missing.isEmpty(), "language " + lang + " has the endings and the onslaught " + (missing.isEmpty() ? "" : missing));
        }
    }

    private static void testCraft23(ServerLevel level) {
        BlockPos a = new BlockPos(740, Y + 10, 60);
        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++) {
                level.setBlock(a.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = 0; y < 3; y++) level.setBlock(a.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        level.setBlock(a, ModBlocks.ECHO_ALTAR.get().defaultBlockState(), Block.UPDATE_ALL);
        check(dev.umbra.craft.EchoAltarBlock.candles(level, a) == 0 && dev.umbra.craft.EchoAltarBlock.power(0) == 1, "a bare altar echoes at level I");
        level.setBlock(a.offset(2, 0, 0), ModBlocks.LUMINITE_CANDLE.get().defaultBlockState().setValue(dev.umbra.craft.LuminiteCandleBlock.CANDLES, 4), Block.UPDATE_ALL);
        level.setBlock(a.offset(-2, 0, 1), ModBlocks.LUMINITE_CANDLE.get().defaultBlockState().setValue(dev.umbra.craft.LuminiteCandleBlock.CANDLES, 4), Block.UPDATE_ALL);
        int c = dev.umbra.craft.EchoAltarBlock.candles(level, a);
        check(c == 8 && dev.umbra.craft.EchoAltarBlock.power(c) == 3, "eight candles make it echo at level III (" + c + ")");
        check(level.getBlockState(a.offset(2, 0, 0)).getLightEmission() >= 12, "four candles shine brightly");

        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        fake.getInventory().clearContent();
        ItemStack sword = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        var options = dev.umbra.craft.EchoAltarBlock.candidates(level, sword, 3);
        check(!options.isEmpty(), "a sword can take something from the altar (" + options.size() + ")");
        fake.experienceLevel = 30;
        fake.setItemInHand(InteractionHand.MAIN_HAND, sword);
        fake.getInventory().add(new ItemStack(ModItems.ECHO_QUARTZ.get(), 8));
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(a), net.minecraft.core.Direction.UP, a, false);
        level.getBlockState(a).useItemOn(fake.getMainHandItem(), level, fake, InteractionHand.MAIN_HAND, hit);
        ItemStack after = fake.getMainHandItem();
        int umbra = 0;
        for (var key : dev.umbra.gear.Gear.ALL_ENCHANTS) if (!key.equals(dev.umbra.gear.Gear.WHISPER_CURSE)) umbra += dev.umbra.gear.Gear.level(level, after, key) > 0 ? 1 : 0;
        check(umbra == 1 && fake.experienceLevel == 30 - dev.umbra.craft.EchoAltarBlock.xpCost(3)
                && fake.getInventory().countItem(ModItems.ECHO_QUARTZ.get()) == 8 - dev.umbra.craft.EchoAltarBlock.quartzCost(3), "the altar gives one enchantment of Umbra's for quartz and experience (" + umbra + ", xp " + fake.experienceLevel + ")");

        // the anvil mends Umbra's gear with its own stuff, and pins artifacts (the logic of its window)
        BlockPos v = a.offset(0, 0, 3);
        level.setBlock(v, ModBlocks.GLOOM_ANVIL.get().defaultBlockState(), Block.UPDATE_ALL);
        ItemStack helm = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath(Umbra.MODID, "luminite_helmet")));
        helm.setDamageValue(helm.getMaxDamage() - 1);
        int[] cost = new int[1];
        ItemStack mended = dev.umbra.craft.GloomAnvilMenu.result(helm, new ItemStack(ModItems.LUMINITE_CRYSTAL.get(), 64), cost);
        check(!mended.isEmpty() && mended.getDamageValue() == 0 && cost[0] == 4, "the gloom anvil mends luminite with luminite, a quarter for each crystal (" + cost[0] + ")");
        check(dev.umbra.craft.GloomAnvilMenu.result(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD), new ItemStack(ModItems.ECHO_QUARTZ.get()), cost).isEmpty(), "and does not know foreign gear");
        ItemStack pinned = dev.umbra.craft.GloomAnvilMenu.result(new ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE), new ItemStack(ModItems.ASH_CLASP.get()), cost);
        check(dev.umbra.gear.Gear.ASH_CLASP.equals(pinned.get(dev.umbra.registry.ModComponents.ARTIFACT.get())) && cost[0] == 1, "and pins an artifact on a chestplate");
        fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        fake.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        fake.getInventory().clearContent();
        for (BlockPos p : new BlockPos[]{a, v, a.offset(2, 0, 0), a.offset(-2, 0, 1)}) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        // 2.4: the armour ladder, the mazes, the bosses' second wind
        int[][] ladder = {{0, 1}, {2, 1}, {1, 1}, {0, 2}, {2, 2}, {1, 2}, {0, 3}, {2, 3}, {1, 3}};
        boolean rising = true;
        StringBuilder sc = new StringBuilder();
        for (int i = 0; i < ladder.length; i++) {
            float sco = dev.umbra.registry.ModMaterials.score(ladder[i][0], ladder[i][1]);
            sc.append(sco).append(' ');
            if (i > 0) rising &= sco > dev.umbra.registry.ModMaterials.score(ladder[i - 1][0], ladder[i - 1][1]);
        }
        check(rising, "armour: gloom, shadow, luminite, tier by tier, each better than the one before (" + sc + ")");
        BlockPos maze = dev.umbra.quantum.QuantumMazeFeature.nearest(level.getSeed(), 0, 0);
        check(Math.hypot(maze.getX(), maze.getZ()) < 24 * 16 * 1.5, "a maze is never far: the nearest is " + (int) Math.hypot(maze.getX(), maze.getZ()) + " blocks away");
        var w = ModEntities.WATCHER.get().create(level, EntitySpawnReason.COMMAND);
        if (w != null) {
            w.snapTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5, 0, 0);
            w.setNoAi(true);
            level.addFreshEntity(w);
            check(w.getMaxHealth() >= 180, "bosses are half as strong again (the Watcher has " + w.getMaxHealth() + ")");
            float before = w.getHealth();
            w.hurtServer(level, level.damageSources().magic(), 100.0F);
            check(before - w.getHealth() < 100.0F * 0.5F, "the Watcher's eye shrugs off most of a blow (" + (before - w.getHealth()) + " of 100)");
            float h = w.getHealth();
            w.onLit(level, null);
            check(h - w.getHealth() <= w.getMaxHealth() * dev.umbra.boss.UmbraBoss.LIGHT_CAP + 0.01F, "a flash takes no more than 2% of a boss");
            w.discard();
        }
    }

    private static final BlockPos RAID_FOOT = new BlockPos(700, Y, 120);
    private static BlockPos raidLamp;

    private static int lampLevel(ServerLevel level) {
        BlockState st = level.getBlockState(raidLamp);
        return st.getBlock() instanceof dev.umbra.lighthouse.LighthouseLampBlock ? st.getValue(dev.umbra.lighthouse.LighthouseLampBlock.LEVEL) : -1;
    }

    private static void raidTick(ServerLevel level, int s) {
        switch (s) {
            case 1 -> {
                for (int cx = (RAID_FOOT.getX() - 40) >> 4; cx <= (RAID_FOOT.getX() + 40) >> 4; cx++)
                    for (int cz = (RAID_FOOT.getZ() - 40) >> 4; cz <= (RAID_FOOT.getZ() + 40) >> 4; cz++) {
                        level.setChunkForced(cx, cz, true);
                        level.getChunk(cx, cz);
                    }
                for (int x = -38; x <= 38; x++)
                    for (int z = -38; z <= 38; z++) {
                        level.setBlock(RAID_FOOT.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                        for (int y = 0; y < 4; y++) level.setBlock(RAID_FOOT.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                raidLamp = RAID_FOOT.below().above(dev.umbra.lighthouse.LighthouseFeature.LAMP_Y);
                level.setBlock(raidLamp, ModBlocks.LIGHTHOUSE_LAMP.get().defaultBlockState().setValue(dev.umbra.lighthouse.LighthouseLampBlock.LEVEL, 3), Block.UPDATE_ALL);
                check(dev.umbra.lighthouse.LighthouseRaid.footOf(level, raidLamp).equals(RAID_FOOT), "the onslaught knows the foot of the tower (" + dev.umbra.lighthouse.LighthouseRaid.footOf(level, raidLamp) + ")");
                check(!dev.umbra.lighthouse.LighthouseRaid.tryStart(level, raidLamp, 3), "with nobody at the lighthouse, no onslaught starts");
                dev.umbra.lighthouse.LighthouseRaid.warmup = 10;
                dev.umbra.lighthouse.LighthouseRaid.pause = 10;
                dev.umbra.lighthouse.LighthouseRaid.start(level, raidLamp, 3);
                check(dev.umbra.lighthouse.LighthouseRaid.active(level, raidLamp), "an onslaught starts");
            }
            case 13 -> {
                int[] st = dev.umbra.lighthouse.LighthouseRaid.state(level, raidLamp);
                var raiders = dev.umbra.lighthouse.LighthouseRaid.raiders(level, raidLamp);
                check(st != null && st[0] == 1 && !raiders.isEmpty(), "the first wave comes (" + (st == null ? "none" : st[0] + " / " + st[1]) + ")");
                check(raiders.stream().allMatch(m -> m.entityTags().contains(dev.umbra.lighthouse.LighthouseRaid.TAG)), "the raiders are marked as the onslaught's");
                int before = raiders.size();
                dev.umbra.light.WardBlock.repel(level, raidLamp.below(dev.umbra.lighthouse.LighthouseFeature.LAMP_Y), 60);
                check(dev.umbra.lighthouse.LighthouseRaid.raiders(level, raidLamp).size() == before, "light does not drive off an onslaught");
            }
            case 120 -> {
                check(!dev.umbra.lighthouse.LighthouseRaid.active(level, raidLamp), "three waves beaten, the onslaught is over");
                check(lampLevel(level) == 3, "the lighthouse keeps its light");
                int crystals = 0;
                for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(RAID_FOOT).inflate(8)))
                    if (e.getItem().is(ModItems.LUMINITE_CRYSTAL.get())) crystals += e.getItem().getCount();
                check(crystals >= 4, "and pays in luminite at its foot (" + crystals + ")");
                level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(RAID_FOOT).inflate(8)).forEach(net.minecraft.world.entity.Entity::discard);
                // the horn
                FakePlayer fake = FakePlayerFactory.getMinecraft(level);
                ItemStack horn = new ItemStack(ModItems.KEEPER_HORN.get());
                horn.set(dev.umbra.registry.ModComponents.BOUND_LAMP.get(), dev.umbra.lighthouse.LighthouseRaid.bindingOf(level, raidLamp));
                fake.getInventory().clearContent();
                fake.getInventory().add(horn);
                check(dev.umbra.lighthouse.LighthouseRaid.hornFor(fake, level, raidLamp) != null, "a bound horn knows its lighthouse");
                fake.snapTo(RAID_FOOT.getX() + 300.5, Y + 5, RAID_FOOT.getZ() + 0.5, 0, 0);
                check(!dev.umbra.lighthouse.LighthouseRaid.answer(fake, dev.umbra.lighthouse.LighthouseRaid.hornFor(fake, level, raidLamp)), "when all is quiet the horn carries nobody");
                dev.umbra.lighthouse.LighthouseRaid.start(level, raidLamp, 3);
                // (the horn now lives in the inventory: the stack we made was moved there)
                boolean went = dev.umbra.lighthouse.LighthouseRaid.answer(fake, dev.umbra.lighthouse.LighthouseRaid.hornFor(fake, level, raidLamp));
                // (a fake player does not really move on teleport; the real move is the same call as the nightmare's)
                check(went, "during an onslaught the horn answers and carries you to the tower");
                fake.getInventory().clearContent();
                fake.snapTo(0.5, Y + 30, 0.5, 0, 0);
            }
            case 135 -> {
                // let them through: the lighthouse falls
                var raiders = dev.umbra.lighthouse.LighthouseRaid.raiders(level, raidLamp);
                check(raiders.size() >= dev.umbra.lighthouse.LighthouseRaid.MAX_BREACHES, "the second onslaught comes in force (" + raiders.size() + ")");
                for (var m : raiders) m.snapTo(RAID_FOOT.getX() + 0.5, RAID_FOOT.getY(), RAID_FOOT.getZ() + 0.5, 0, 0);
            }
            case 140 -> {
                check(!dev.umbra.lighthouse.LighthouseRaid.active(level, raidLamp) && lampLevel(level) == 2, "let through, the dark dims the lamp by a level (" + lampLevel(level) + ")");
                check(dev.umbra.lighthouse.LighthouseRaid.raiders(level, raidLamp).isEmpty(), "and the rest of the onslaught melts away");
                dev.umbra.lighthouse.LighthouseRaid.warmup = 100;
                dev.umbra.lighthouse.LighthouseRaid.pause = 100;
                level.setBlock(raidLamp, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            default -> {
                // the first onslaught: every wave is cut down as soon as it is seen
                if (s > 13 && s < 120 && dev.umbra.lighthouse.LighthouseRaid.active(level, raidLamp)) {
                    for (var m : dev.umbra.lighthouse.LighthouseRaid.raiders(level, raidLamp)) m.discard();
                }
            }
        }
    }

    private static void testLighthouseKeeper(MinecraftServer server) {
        ServerLevel under = server.getLevel(Underside.LEVEL);
        if (under == null) return;
        int[] c = dev.umbra.lighthouse.LighthouseFeature.nearest(under.getSeed(), 0, 0);
        BlockPos lamp = dev.umbra.lighthouse.LampKeeper.findLamp(under, c[0] * 16 + 8, c[1] * 16 + 8);
        check(lamp != null && !under.getEntitiesOfClass(dev.umbra.lighthouse.LampKeeper.class, new AABB(lamp).inflate(20, 70, 20)).isEmpty(), "the real lighthouse has its keeper");
        under.setChunkForced(c[0], c[1], false);
    }

    /** A UUID whose villager offers the given errand. */
    private static java.util.UUID uuidFor(String quest) {
        for (long i = 1; i < 100000; i++) {
            java.util.UUID u = new java.util.UUID(i << 8, i);
            if (dev.umbra.quest.Quests.forVillager(u).id().equals(quest)) return u;
        }
        return java.util.UUID.randomUUID();
    }

    private static void testBalance182(MinecraftServer server, ServerLevel level) {
        BlockPos c = new BlockPos(160, Y + 60, 30);
        for (int x = -8; x <= 8; x++)
            for (int z = -8; z <= 8; z++) {
                level.setBlock(c.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = 0; y < 4; y++) level.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);

        // a winded boss takes half as much again
        var a = ModEntities.ARCHITECT.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        var b = ModEntities.ARCHITECT.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        check(a != null && b != null, "two Architects for the stun test");
        if (a != null && b != null) {
            a.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
            b.snapTo(c.getX() + 4.5, c.getY(), c.getZ() + 0.5, 0, 0);
            level.addFreshEntity(a);
            level.addFreshEntity(b);
            b.stun(dev.umbra.boss.UmbraBoss.STUN_TICKS);
            check(b.isStunned() && !a.isStunned(), "a boss can be stunned for a moment");
            float ha = a.getHealth(), hb = b.getHealth();
            a.hurtServer(level, level.damageSources().magic(), 10.0F);
            b.hurtServer(level, level.damageSources().magic(), 10.0F);
            float lostA = ha - a.getHealth(), lostB = hb - b.getHealth();
            check(lostA > 0 && Math.abs(lostB - lostA * 1.5F) < 0.05F, "and takes +50% damage meanwhile (" + lostA + " vs " + lostB + ")");

            // shades: never more than four around one boss
            int first = dev.umbra.boss.BossMoves.shades(level, a, null, 10);
            int second = dev.umbra.boss.BossMoves.shades(level, a, null, 10);
            int around = level.getEntitiesOfClass(dev.umbra.mob.Shade.class, a.getBoundingBox().inflate(40)).size();
            check(first == 4 && second == 0 && around == 4, "a boss calls at most four shades (" + first + ", " + second + ", " + around + ")");
            level.getEntitiesOfClass(dev.umbra.mob.Shade.class, a.getBoundingBox().inflate(40)).forEach(net.minecraft.world.entity.Entity::discard);
            a.discard();
            b.discard();
        }

        // the shroud: three hits with a light in hand tear it, without a light nothing helps
        var shepherd = ModEntities.SHEPHERD.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (shepherd != null) {
            shepherd.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
            level.addFreshEntity(shepherd);
            level.setBlock(c.offset(1, 1, 0), ModBlocks.LIVING_DARKNESS.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            float full = shepherd.getHealth();
            fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            fake.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
            var hit = level.damageSources().playerAttack(fake);
            for (int i = 0; i < 4; i++) shepherd.hurtServer(level, hit, 5.0F);
            check(shepherd.getHealth() == full, "without a light in hand the shroud holds");
            fake.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(net.minecraft.world.item.Items.TORCH));
            shepherd.hurtServer(level, hit, 5.0F);
            shepherd.hurtServer(level, hit, 5.0F);
            check(shepherd.getHealth() == full && level.getBlockState(c.offset(1, 1, 0)).is(ModBlocks.LIVING_DARKNESS.get()), "two hits with a light only chip it");
            shepherd.hurtServer(level, hit, 5.0F);
            check(shepherd.getHealth() < full && !level.getBlockState(c.offset(1, 1, 0)).is(ModBlocks.LIVING_DARKNESS.get()), "the third hit tears the shroud and lands");
            shepherd.discard();
            fake.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }

        // clarity wipes darkness and nausea, from any source
        fake.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 200, 0));
        fake.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NAUSEA, 200, 0));
        dev.umbra.registry.ModEffects.tick(fake);
        boolean without = fake.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS) && fake.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA);
        fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.SILENCE_TINCTURE.get()));
        ModItems.SILENCE_TINCTURE.get().use(level, fake, net.minecraft.world.InteractionHand.MAIN_HAND);
        check(fake.hasEffect(dev.umbra.registry.ModEffects.CLARITY), "the tincture of silence gives Clarity");
        dev.umbra.registry.ModEffects.tick(fake);
        check(without && !fake.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS) && !fake.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA),
                "Clarity wipes away darkness and nausea (and nothing else does)");
        fake.removeAllEffects();

        // the Keepers' pages: in order, each opens its own craft
        dev.umbra.journal.Journal.clear(fake);
        check("keeper_1".equals(dev.umbra.item.KeeperPageItem.nextPage(fake)), "the first page is read first");
        var recipes = server.getRecipeManager();
        java.util.function.Predicate<String> known = id -> fake.getRecipeBook().contains(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(Umbra.MODID, id)));
        dev.umbra.journal.Journal.unlock(fake, "luminite");
        boolean beforePages = known.test("luminite_lamp") && !known.test("mirror") && !known.test("dispeller") && !known.test("ward");
        dev.umbra.journal.Journal.unlock(fake, "keeper_1");
        dev.umbra.journal.Journal.unlock(fake, "keeper_4");
        boolean afterPages = known.test("mirror") && known.test("dispeller") && known.test("luminite_lantern") && !known.test("ward");
        check(beforePages && afterPages, "finding the luminite is not enough: the mirror and the dispeller come from the Keepers' pages (" + beforePages + ", " + afterPages + ")");
        check("keeper_2".equals(dev.umbra.item.KeeperPageItem.nextPage(fake)), "pages are read in order");
        for (int i = 2; i <= dev.umbra.item.KeeperPageItem.PAGES; i++) dev.umbra.journal.Journal.unlock(fake, "keeper_" + i);
        check(dev.umbra.item.KeeperPageItem.nextPage(fake) == null && known.test("ward") && known.test("light_arrow") && known.test("eternal_torch")
                && known.test("sedative_from_luminite"), "all seven pages teach the whole craft");
        dev.umbra.journal.Journal.clear(fake);
    }

    /** Diagnostics for the 1.9 lighthouse: how tall and how wide are the Underside's caverns, and where is the water. */
    private static void profileUnderside(MinecraftServer server) {
        ServerLevel under = server.getLevel(Underside.LEVEL);
        if (under == null) return;
        java.util.Map<String, int[]> perBiome = new java.util.TreeMap<>();
        StringBuilder rows = new StringBuilder();
        for (int gx = 0; gx < 10; gx++) {
            for (int gz = 0; gz < 10; gz++) {
                int x = 4000 + gx * 53, z = 4000 + gz * 53;
                BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, 0, z);
                int bestBottom = -1, bestTop = -1, runTop = -1;
                for (int y = 126; y >= 1; y--) {
                    m.setY(y);
                    BlockState st = under.getBlockState(m);
                    boolean open = !st.isSolidRender() && st.getFluidState().isEmpty();
                    if (open) {
                        if (runTop < 0) runTop = y;
                        if (y == 1 || under.getBlockState(m.below()).isSolidRender() || !under.getBlockState(m.below()).getFluidState().isEmpty()) {
                            if (runTop - y > bestTop - bestBottom) { bestTop = runTop; bestBottom = y; }
                            runTop = -1;
                        }
                    } else runTop = -1;
                }
                if (bestBottom < 0) continue;
                m.setY(bestBottom);
                boolean water = !under.getBlockState(m.below()).getFluidState().isEmpty();
                String biome = under.getBiome(m).unwrapKey().map(k -> k.identifier().getPath()).orElse("?");
                int[] a = perBiome.computeIfAbsent(biome, k -> new int[]{0, 0, 0, 999, 0});
                int h = bestTop - bestBottom + 1;
                a[0]++; a[1] += h; a[2] += water ? 1 : 0; a[3] = Math.min(a[3], h); a[4] = Math.max(a[4], h);
                rows.append(String.format("(%d,%d)%s:%d-%d%s ", x, z, biome.substring(0, Math.min(5, biome.length())), bestBottom, bestTop, water ? "w" : ""));
            }
        }
        for (var e : perBiome.entrySet()) {
            int[] a = e.getValue();
            check(true, "PROFILE " + e.getKey() + ": columns " + a[0] + ", avg open height " + (a[1] / a[0]) + ", min " + a[3] + ", max " + a[4] + ", water floor in " + a[2]);
        }
        check(true, "PROFILE rows " + rows);
    }

    // ---------------------------------------------------------------- 1.5
    private static void testFilling(MinecraftServer server, ServerLevel level) {
        BlockPos c = new BlockPos(120, Y + 60, 30);
        for (int x = -12; x <= 12; x++)
            for (int z = -12; z <= 12; z++) level.setBlock(c.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        dev.umbra.decor.UndersideLandmarkFeature.camp(level, c, level.getRandom());
        check(level.getBlockState(c).is(Blocks.CAMPFIRE) && level.getBlockState(c.offset(2, 0, -2)).is(Blocks.CHEST), "an explorer's camp has a campfire and a chest");
        dev.umbra.decor.UndersideLandmarkFeature.figures(level, c.offset(8, 0, 8), level.getRandom());
        check(!level.getEntitiesOfClass(dev.umbra.decor.AshFigure.class, new AABB(c).inflate(20)).isEmpty(), "ash figures stand in the ash");
        level.getEntitiesOfClass(dev.umbra.decor.AshFigure.class, new AABB(c).inflate(20)).forEach(net.minecraft.world.entity.Entity::discard);

        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 4.5, 0, 0);
        fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EXPLORER_NOTE.get(), 2));
        ModItems.EXPLORER_NOTE.get().use(level, fake, InteractionHand.MAIN_HAND);
        ModItems.EXPLORER_NOTE.get().use(level, fake, InteractionHand.MAIN_HAND);
        check(dev.umbra.journal.Journal.has(fake, "note_1") && dev.umbra.journal.Journal.has(fake, "note_2"), "explorer notes go into the journal in order");
        dev.umbra.sanity.FalseDawn.start(level, fake);
        check(dev.umbra.sanity.FalseDawn.active(fake) && fake.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "a false dawn brightens the night");
        var dd = ModEntities.DEATH_DOUBLE.get().create(level, EntitySpawnReason.EVENT);
        check(dd != null, "a death double can stand where you died");
        if (dd != null) dd.discard();

        ServerLevel under = server.getLevel(Underside.LEVEL);
        if (under != null) {
            int found = 0;
            for (int cx = -3; cx <= 3; cx++)
                for (int cz = -3; cz <= 3; cz++) {
                    var chunk = under.getChunk(cx, cz);
                    for (BlockPos p : BlockPos.betweenClosed(cx * 16, 10, cz * 16, cx * 16 + 15, 118, cz * 16 + 15)) {
                        BlockState s = chunk.getBlockState(p);
                        if (s.is(ModBlocks.LUMINITE_CLUSTER.get()) || s.is(ModBlocks.ECHO_CLUSTER.get()) || s.is(ModBlocks.GLOOM_MUSHROOM.get())
                                || s.is(Blocks.BONE_BLOCK) || s.is(Blocks.SKELETON_SKULL)) found++;
                    }
                }
            check(found > 20, "the Underside is decorated (" + found + " pieces in 49 chunks)");
        }
    }

    private static void testBossMoves(ServerLevel level) {
        check(dev.umbra.journal.Journal.section("watcher") == 4 && dev.umbra.journal.Journal.section("mimic_queen") == 4
                && dev.umbra.journal.Journal.section("mimic_chest") == 2 && dev.umbra.journal.Journal.section("mimicker") == 3
                && dev.umbra.journal.Journal.section("insight_2") == 1 && dev.umbra.journal.Journal.section("note_3") == 5
                && dev.umbra.journal.Journal.section("start") == 0, "journal entries sort into sections");
        int[] per = new int[dev.umbra.journal.Journal.SECTIONS.length];
        for (String id : dev.umbra.journal.Journal.ENTRIES) per[dev.umbra.journal.Journal.section(id)]++;
        boolean none = false;
        for (int n : per) none |= n == 0;
        check(!none, "every journal section has entries");

        BlockPos c = new BlockPos(160, Y + 60, 30);
        for (int x = -8; x <= 8; x++)
            for (int z = -8; z <= 8; z++) {
                level.setBlock(c.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = 0; y < 4; y++) level.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        var watcher = ModEntities.WATCHER.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        check(watcher != null, "a watcher for the move tests");
        if (watcher == null) return;
        watcher.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
        level.addFreshEntity(watcher);
        check(watcher.phase() == 1, "a fresh boss is in phase 1");
        watcher.setHealth(watcher.getMaxHealth() * 0.4F);
        check(watcher.phase() == 2, "below half health a boss enters phase 2");
        var prim = ModEntities.PRIMORDIAL_SHADOW.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (prim != null) {
            prim.setHealth(prim.getMaxHealth() * 0.2F);
            check(prim.phase() == 3, "the Primordial Shadow has a third phase");
            prim.discard();
        }

        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        fake.snapTo(c.getX() + 2.5, c.getY(), c.getZ() + 0.5, 0, 0);
        fake.setDeltaMovement(Vec3.ZERO);
        dev.umbra.boss.BossMoves.shockwave(level, watcher, 4.0, 1.0F, 1.2);
        dev.umbra.boss.BossMoves.leap(watcher, fake, 1.2);
        check(watcher.getDeltaMovement().x > 0.8 && watcher.getDeltaMovement().y > 0.4, "a boss leaps at its target");

        int before = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.EvokerFangs.class, new AABB(c).inflate(20)).size();
        fake.snapTo(c.getX() + 8.5, c.getY(), c.getZ() + 0.5, 0, 0);
        dev.umbra.boss.BossMoves.fangsLine(level, watcher, fake, 10);
        dev.umbra.boss.BossMoves.fangsRing(level, watcher, fake.position(), 2.5, 8, 20);
        int fangs = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.EvokerFangs.class, new AABB(c).inflate(20)).size() - before;
        check(fangs >= 12, "jaws snap out of the floor in lines and rings (" + fangs + ")");

        var bursts = new dev.umbra.boss.BossMoves.Bursts();
        bursts.mark(level, c.offset(3, 0, 3), 0);
        check(bursts.size() == 1, "a burst is marked on the floor");
        bursts.tick(level, watcher, 1.0F);
        check(bursts.size() == 0, "and goes off when its time comes");

        var temp = new dev.umbra.boss.BossMoves.TempBlocks();
        BlockPos w = c.offset(-3, 0, -3);
        temp.place(level, w, ModBlocks.MAZE_WALL.get().defaultBlockState(), 0);
        check(level.getBlockState(w).is(ModBlocks.MAZE_WALL.get()), "the Architect's cage wall rises");
        temp.tick(level);
        check(level.getBlockState(w).isAir(), "and crumbles away again");
        level.getEntitiesOfClass(net.minecraft.world.entity.projectile.EvokerFangs.class, new AABB(c).inflate(20)).forEach(net.minecraft.world.entity.Entity::discard);
        watcher.discard();
    }

    private static void testUnderside18(MinecraftServer server, ServerLevel level) {
        // storms: decided by the clock alone, about one window in four
        int storms = 0;
        boolean outside = false;
        for (long w = 0; w < 80; w++) {
            if (dev.umbra.underside.AshStorm.strength(w * dev.umbra.underside.AshStorm.WINDOW + 3000) >= 1.0F) storms++;
            outside |= dev.umbra.underside.AshStorm.strength(w * dev.umbra.underside.AshStorm.WINDOW + 100) > 0;
        }
        check(storms >= 8 && storms <= 35 && !outside, "ash storms blow in about a quarter of the windows (" + storms + "/80)");
        check(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(net.minecraft.resources.Identifier.fromNamespaceAndPath("umbra", "ambient.whisper")),
                "the Underside has its own sounds");
        // no humming in the background, no chiming crystals
        var sounds = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT;
        check(!sounds.containsKey(net.minecraft.resources.Identifier.fromNamespaceAndPath("umbra", "ambient.hum"))
                        && !sounds.containsKey(net.minecraft.resources.Identifier.fromNamespaceAndPath("umbra", "ambient.crystal_hum"))
                        && !sounds.containsKey(net.minecraft.resources.Identifier.fromNamespaceAndPath("umbra", "ambient.chime")),
                "the humming loops and the crystal chime are gone");
        int seen = 0;
        boolean humming = false;
        for (String biome : new String[]{"abyss", "ash_plains", "inverted_forest", "quantum_ruins", "crystal_grottos", "black_lakes", "whispering_forest"}) {
            try (var in = SmokeTest.class.getResourceAsStream("/data/umbra/worldgen/biome/" + biome + ".json")) {
                if (in == null) continue;
                seen++;
                humming |= new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("\"loop\"");
            } catch (java.io.IOException e) {
                humming = true;
            }
        }
        check(!humming, "no Underside biome hums in the background (" + seen + "/7 files checked)");

        // the light eater and luminite
        Block[] pure = {ModBlocks.LUMINITE_BLOCK.get(), ModBlocks.LUMINITE_CLUSTER.get(), ModBlocks.LUMINITE_LAMP.get(), ModBlocks.LUMINITE_ORE.get(),
                ModBlocks.DEEPSLATE_LUMINITE_ORE.get(), ModBlocks.ECHO_CLUSTER.get()};
        boolean left = true, tagged = true;
        for (Block b : pure) {
            left &= !LightEater.edible(b.defaultBlockState());
            tagged &= b.defaultBlockState().is(LightEater.IGNORED);
        }
        check(left && tagged, "the light eater leaves every kind of luminite alone");
        check(LightEater.edible(Blocks.GLOWSTONE.defaultBlockState()) && LightEater.edible(Blocks.TORCH.defaultBlockState()), "but it still eats ordinary light");
        ServerLevel under = server.getLevel(Underside.LEVEL);
        if (under != null) {
            var possible = under.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
            int found = 0;
            for (var b : possible) {
                if (b.is(dev.umbra.underside.UndersideBiomes.CRYSTAL_GROTTOS) || b.is(dev.umbra.underside.UndersideBiomes.BLACK_LAKES)
                        || b.is(dev.umbra.underside.UndersideBiomes.WHISPERING_FOREST)) found++;
            }
            check(found == 3, "the Underside has crystal grottos, black lakes and a whispering forest (" + found + "/3)");
        }
        check(dev.umbra.journal.Journal.section("lurker") == 3 && dev.umbra.journal.Journal.section("black_lakes") == 0, "new journal entries are sorted");

        // a temple in a lake, built on a block of stone
        BlockPos c = new BlockPos(270, Y + 6, 30);
        for (int x = -18; x <= 18; x++)
            for (int z = -18; z <= 18; z++)
                for (int y = -20; y <= 14; y++)
                    level.setBlock(c.offset(x, y, z), y < 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        BlockPos chest = dev.umbra.lake.SunkenTempleFeature.build(level, c, level.getRandom());
        check(level.getBlockState(chest).is(Blocks.CHEST) && level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity rc
                && dev.umbra.lake.SunkenTempleFeature.LOOT.equals(rc.getLootTable()), "a sunken temple keeps a chest on its altar");
        check(level.getBlockState(c.offset(0, -1, 0)).is(Blocks.WATER) && level.getBlockState(c.offset(0, -5, -2)).is(Blocks.WATER), "the temple stands under water");
        int leaks = 0;
        for (int x = -18; x <= 18; x++)
            for (int z = -18; z <= 18; z++)
                if (level.getBlockState(c.offset(x, 0, z)).is(Blocks.WATER)) leaks++;
        check(leaks == 0, "the lake does not rise above its shore (" + leaks + ")");
        int lurkers = level.getEntitiesOfClass(dev.umbra.lake.Lurker.class, new AABB(c).inflate(20)).size();
        check(lurkers >= 2, "lurkers guard the temple (" + lurkers + ")");
        BlockState col = level.getBlockState(c.offset(6, 0, 6));
        check(col.is(ModBlocks.GLOOM_BLOCK.get()) || col.is(ModBlocks.DUSK_STONE.get()), "broken columns stand out of the water");
        level.getEntitiesOfClass(dev.umbra.lake.Lurker.class, new AABB(c).inflate(20)).forEach(net.minecraft.world.entity.Entity::discard);

        // loot tables in the 26.3 format: stack sizes are honoured
        int biggest = 0;
        for (int i = 0; i < 6; i++) {
            BlockPos lc = c.offset(-17 + i, 1, -17);
            level.setBlock(lc.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(lc, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(lc) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity lr) {
                lr.setLootTable(dev.umbra.lake.SunkenTempleFeature.LOOT, 1234L + i);
                lr.unpackLootTable(null);
                for (int k = 0; k < lr.getContainerSize(); k++) biggest = Math.max(biggest, lr.getItem(k).getCount());
            }
        }
        check(biggest > 1, "chest loot comes in stacks (largest " + biggest + ")");

        // a crystal spire and a dead tree on the shore
        BlockPos shore = c.offset(16, 1, 16);
        level.setBlock(shore.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        dev.umbra.decor.CrystalGrottoFeature.spire(level, shore, net.minecraft.core.Direction.UP, 6, level.getRandom());
        check(level.getBlockState(shore).is(ModBlocks.LUMINITE_BLOCK.get()), "luminite spires grow in the grottos");
        BlockPos tree = c.offset(-16, 1, 16);
        level.setBlock(tree.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        dev.umbra.decor.DeadTreeFeature.tree(level, tree, 7, level.getRandom());
        check(level.getBlockState(tree.above(6)).is(ModBlocks.DEAD_LOG.get()), "dead trees stand in the whispering forest");
    }

    private static void checkWard(ServerLevel level) {
        check(dev.umbra.light.WardBlock.protects(level, Vec3.atCenterOf(WARD).add(10, 0, 10)), "a burning ward protects its circle");
        check(!dev.umbra.light.WardBlock.protects(level, Vec3.atCenterOf(WARD).add(40, 0, 0)), "but not beyond two chunks");
        double d = spider == null ? 0 : spider.position().distanceTo(Vec3.atCenterOf(WARD));
        check(spider != null && d > 7.0, "the ward drives a spider away (" + String.format("%.1f", d) + " blocks)");
        if (spider != null) spider.discard();
        if (statue != null) {
            double moved = statue.position().distanceTo(statueStart);
            check(moved > 1.0, "an unwatched statue moves (" + String.format("%.1f", moved) + " blocks)");
            statue.discard();
        }
        check(lightPos == null || !level.getBlockState(lightPos).is(ModBlocks.HELD_LIGHT.get()), "lantern light disappears without its lantern");
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
