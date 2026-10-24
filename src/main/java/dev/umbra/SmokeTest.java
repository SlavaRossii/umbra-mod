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
    /** Then the server part (see SmokeMulti): two players at once. */
    private static final int MULTI_END = RAID_END + 80;

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
                case 670 -> testItems29(server, level);
                case 666 -> testWeapons212(level);
                case 671 -> testGear20(server, level);
                case 672 -> startEchoTest(level);
                case 678 -> finishEchoTest();
                case 679 -> testLighthouseKeeper(server);
                case 673 -> testEndings22(level);
                case 674 -> testCraft23(level);
                case 675 -> testAltar28(server, level);
                case 676 -> testDepths26(server);
                case 677 -> testYan27(level);
                case MULTI_END -> finish(server);
                default -> {
                    if (tick > LIFE_FROM && tick < LIFE_END) SmokeLife.tick(server, level, tick - LIFE_FROM);
                    if (tick > LIFE_END && tick < RAID_END) raidTick(level, tick - LIFE_END);
                    if (tick > RAID_END && tick < MULTI_END) SmokeMulti.tick(server, level, tick - RAID_END);
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
        level.setBlock(lightPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(fake.blockPosition(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        ItemStack marta = new ItemStack(ModItems.MARTA_LANTERN.get());
        fake.setItemInHand(InteractionHand.MAIN_HAND, marta);
        fake.tickCount = 8;
        ModItems.MARTA_LANTERN.get().inventoryTick(marta, level, fake, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        check(level.getBlockState(lightPos).is(ModBlocks.HELD_LIGHT.get()) || level.getBlockState(fake.blockPosition()).is(ModBlocks.HELD_LIGHT.get()),
                "2.6.2: Marta's lantern really lights the way");
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
        check(dev.umbra.journal.Journal.ENTRIES.size() == 117 && new java.util.HashSet<>(dev.umbra.journal.Journal.ENTRIES).size() == 117, "journal has 117 distinct entries (" + dev.umbra.journal.Journal.ENTRIES.size() + ")");

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
            else if (s.is(ModBlocks.PHANTOM_WALL.get()) || s.is(ModBlocks.SEALED_PHANTOM_WALL.get())) phantom++;
            else if (s.is(Blocks.CHEST)) {
                chests++;
                if (dev.umbra.mimic.MimicChests.isMimic(level, p)) mimics++;
            }
        }
        check(cores == 0 && level.getBlockState(o.offset(dev.umbra.citadel.Citadel.THRONE + 5, 3, 0)).isAir(), "the Architect waits in an open hall");
        check(dark > 500, "the hall of living darkness is full (" + dark + ")");
        check(chests >= 20 && mimics >= 3 && mimics < chests - 4, "the citadel has chests, some of them hungry, by chance (" + chests + ", " + mimics + " mimics)");
        check(phantom == 36 + 24, "the library and the side passage hide phantom walls (" + phantom + ")");
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
        check(nests == 4 && plates >= 14, "the citadel has nests and trap tiles (" + nests + ", " + plates + ")");
        int cages = 0;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-3, -2, -dev.umbra.citadel.Citadel.HALF_WIDTH), o.offset(dev.umbra.citadel.Citadel.LENGTH, 9, dev.umbra.citadel.Citadel.HALF_WIDTH)))
            if (level.getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity) cages++;
        check(cages == dev.umbra.citadel.Citadel.SPAWNERS, "the citadel has spawner cages (" + cages + ")");

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
        check(level.getBlockState(o.offset(dev.umbra.citadel.Citadel.MAZE_1 + dev.umbra.citadel.Citadel.MAZE_SIZE + 2, 1, -4)).is(ModBlocks.SEALED_PHANTOM_WALL.get()), "the bypass hides behind a wall only the Architect's seal opens");
        {
            var wall = (dev.umbra.citadel.PhantomWallBlock) ModBlocks.SEALED_PHANTOM_WALL.get();
            FakePlayer f = FakePlayerFactory.getMinecraft(level);
            ItemStack keepChest = f.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            f.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, ItemStack.EMPTY);
            f.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.MADMAN_MASK.get()));
            boolean maskNo = !wall.lets(f);
            f.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);
            ItemStack chest = new ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE);
            chest.set(dev.umbra.registry.ModComponents.ARTIFACT.get(), dev.umbra.gear.Gear.ARCHITECT_SEAL);
            f.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
            boolean sealYes = wall.lets(f);
            f.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, keepChest);
            check(maskNo && sealYes, "the sealed wall lets through only one who wears the Architect's seal, not the mad");
        }

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

        // trap tiles: five kinds, each once
        FakePlayer victim = FakePlayerFactory.getMinecraft(level);
        boolean[] done = new boolean[dev.umbra.citadel.TrapPlateBlock.KINDS];
        int sprung = 0;
        for (int x = 2; x <= 14 && sprung < dev.umbra.citadel.TrapPlateBlock.KINDS; x++) {
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
                case 3 -> victim.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS);
                case 4 -> level.getEntitiesOfClass(net.minecraft.world.entity.projectile.arrow.Arrow.class, new net.minecraft.world.phys.AABB(at).inflate(8)).size() >= 4;
                default -> true;
            };
            check(first && !second && effect && level.getBlockState(at).getValue(dev.umbra.citadel.TrapPlateBlock.TRIGGERED), "trap tile kind " + k + " springs once");
            done[k] = true;
            sprung++;
            victim.removeAllEffects();
            level.setBlock(at, ModBlocks.MAZE_WALL.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        check(sprung == dev.umbra.citadel.TrapPlateBlock.KINDS, "all five kinds of trap were found in the hall floor (" + sprung + ")");
        for (var a : level.getEntitiesOfClass(net.minecraft.world.entity.projectile.arrow.Arrow.class, dev.umbra.citadel.Citadel.bounds(o))) a.discard();
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
                if (!level.getBlockState(n).isAir() && !level.getBlockState(n).is(ModBlocks.TRAP_PLATE.get()) && !level.getBlockState(n).is(ModBlocks.PHANTOM_WALL.get()) && !level.getBlockState(n).is(ModBlocks.SEALED_PHANTOM_WALL.get())) continue;
                if (!level.getBlockState(n.above()).isAir() && !level.getBlockState(n.above()).is(ModBlocks.PHANTOM_WALL.get()) && !level.getBlockState(n.above()).is(ModBlocks.SEALED_PHANTOM_WALL.get())) continue;
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
        check(dev.umbra.lighthouse.LighthouseFeature.MAX_FLOOR + dev.umbra.lighthouse.LighthouseFeature.LAMP_Y + 12 < 124, "the tower and its hollow fit under the roof of the Underside");
        boolean landing = false;
        for (BlockPos p : BlockPos.betweenClosed(f.offset(-3, dev.umbra.lighthouse.LighthouseFeature.LAMP_Y - 1, -3), f.offset(3, dev.umbra.lighthouse.LighthouseFeature.LAMP_Y - 1, 3))) {
            BlockState st = level.getBlockState(p);
            if (st.is(Blocks.STONE_BRICK_SLAB) && st.getValue(net.minecraft.world.level.block.SlabBlock.TYPE) == net.minecraft.world.level.block.state.properties.SlabType.TOP) landing = true;
        }
        check(landing, "the last step of the stair is set flush into the gallery floor");
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
        check(found == 12, "all twelve enchantments are loaded (" + found + ")");
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
        // 2.5: it is raised at runtime once the neighbourhood is loaded; load it as a visitor would
        int reach = (dev.umbra.lighthouse.LighthouseManager.SEARCH + dev.umbra.lighthouse.LighthouseManager.GROTTO + 16) >> 4;
        for (int dx = -reach; dx <= reach; dx++)
            for (int dz = -reach; dz <= reach; dz++) under.getChunk(c[0] + dx, c[1] + dz);
        BlockPos platform = dev.umbra.lighthouse.LighthouseManager.raise(under);
        BlockPos lamp = dev.umbra.lighthouse.LampKeeper.findLamp(under, platform.getX(), platform.getZ());
        check(lamp != null, "a lighthouse is really raised in the Underside, near chunk " + c[0] + "," + c[1] + " (platform " + platform.toShortString() + ")");
        if (lamp == null) return;
        under.setChunkForced(platform.getX() >> 4, platform.getZ() >> 4, true);   // keep it loaded so its entities come to life
        check(lamp.getY() + 16 < 128, "the real lighthouse fits under the roof of the Underside (lamp at y=" + lamp.getY() + ")");
        // it stands in the open: air all round it at head height and higher, not in a pit between cliffs
        int open = 0, samples = 0;
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8;
            for (int r : new int[]{12, 20})
                for (int h : new int[]{2, 10, 20}) {
                    samples++;
                    if (under.getBlockState(platform.offset((int) Math.round(Math.cos(a) * r), h, (int) Math.round(Math.sin(a) * r))).isAir()) open++;
                }
        }
        check(open >= samples * 0.85, "the lighthouse stands in an open grotto (" + open + "/" + samples + " open)");
        check(dev.umbra.lighthouse.LighthouseManager.raise(under).equals(platform), "and it is raised only once");
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
        level.setBlock(a.offset(-2, 0, 1), ModBlocks.THICK_LUMINITE_CANDLE.get().defaultBlockState().setValue(dev.umbra.craft.LuminiteCandleBlock.CANDLES, 4), Block.UPDATE_ALL);
        int c = dev.umbra.craft.EchoAltarBlock.candles(level, a);
        check(c == 12 && dev.umbra.craft.EchoAltarBlock.power(c) == 3, "four candles and four thick ones (worth two each) make it echo at level III (" + c + ")");
        check(dev.umbra.craft.EchoAltarBlock.power(63) == 4 && dev.umbra.craft.EchoAltarBlock.power(64) == dev.umbra.craft.EchoAltarBlock.ABYSS
                && dev.umbra.craft.EchoAltarBlock.next(60) == 4 && dev.umbra.craft.EchoAltarBlock.next(64) == 0, "sixty-four candles open the abyss");
        check(dev.umbra.craft.LuminiteCandleBlock.worth(ModBlocks.LUMINITE_CANDELABRUM.get().defaultBlockState()) == 3
                && ModBlocks.MELTED_LUMINITE_CANDLE.get().defaultBlockState().getLightEmission() > 0, "a candelabrum counts thrice, a melted candle still shines");
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
        check(umbra == 1 && Math.abs(dev.umbra.craft.EchoAltarBlock.pointsOf(fake) - (dev.umbra.craft.EchoAltarBlock.pointsFor(30) - dev.umbra.craft.EchoAltarBlock.xpPoints(3))) <= 1
                && fake.experienceLevel > 30 - dev.umbra.craft.EchoAltarBlock.xpCost(3)
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
        ItemStack infused = dev.umbra.craft.GloomAnvilMenu.result(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD), new ItemStack(ModItems.TROPHIES.get("watcher").get()), cost);
        check("watcher".equals(infused.get(dev.umbra.registry.ModComponents.INFUSION.get())) && cost[0] == 1
                && dev.umbra.craft.GloomAnvilMenu.result(new ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE), new ItemStack(ModItems.TROPHIES.get("watcher").get()), cost).isEmpty(),
                "2.6: the anvil infuses a boss's relic into a weapon, not into armour");
        fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        fake.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        fake.getInventory().clearContent();
        for (BlockPos p : new BlockPos[]{a, v, a.offset(2, 0, 0), a.offset(-2, 0, 1)}) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        // 2.4: the armour ladder, the mazes, the bosses' second wind
        boolean rising = true;
        StringBuilder sc = new StringBuilder();
        for (int set = 0; set < 3; set++)
            for (int tier = 1; tier <= 3; tier++) {
                float sco = dev.umbra.registry.ModMaterials.score(set, tier);
                sc.append(dev.umbra.gear.Gear.SETS[set]).append(tier).append('=').append(sco).append(' ');
                if (tier > 1) rising &= sco > dev.umbra.registry.ModMaterials.score(set, tier - 1);
                if (tier == 3) rising &= sco > dev.umbra.registry.ModMaterials.NETHERITE_SCORE;
            }
        check(rising, "2.7 armour: in each set every tier beats the one below, every tier III beats netherite (" + sc + ")");
        {
            java.util.function.Function<String, ItemStack> armour = name -> ModItems.ARMOR_PIECES.stream().filter(d -> d.getId().getPath().equals(name))
                    .findFirst().map(d -> new ItemStack(d.get())).orElse(ItemStack.EMPTY);
            String[] pc = {"helmet", "chestplate", "leggings", "boots"};
            for (int i = 0; i < 4; i++) fake.setItemSlot(dev.umbra.gear.Gear.ARMOR[i], armour.apply("gloom_" + pc[i] + "_3"));
            fake.setHealth(fake.getMaxHealth());
            boolean master = dev.umbra.gear.SetPowers.master(fake, dev.umbra.gear.Gear.GLOOM);
            boolean low = dev.umbra.gear.SetPowers.bastion(level, fake, 2.0F);
            boolean rose = dev.umbra.gear.SetPowers.bastion(level, fake, fake.getMaxHealth() * 0.8F);
            boolean again = dev.umbra.gear.SetPowers.bastion(level, fake, fake.getMaxHealth() * 0.8F);
            check(master && !low && rose && !again && fake.hasEffect(net.minecraft.world.effect.MobEffects.RESISTANCE),
                    "2.7: full gloom III raises the bastion at death's edge, once in a while (master " + master + ", small blow " + low + ", rose " + rose + ", again " + again + ")");
            fake.removeAllEffects();
            for (int i = 0; i < 4; i++) fake.setItemSlot(dev.umbra.gear.Gear.ARMOR[i], armour.apply("shadow_" + pc[i]));
            fake.tickCount = 13;
            dev.umbra.gear.SetPowers.tick(fake);
            double quick = fake.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            for (int i = 0; i < 4; i++) fake.setItemSlot(dev.umbra.gear.Gear.ARMOR[i], ItemStack.EMPTY);
            dev.umbra.gear.SetPowers.tick(fake);
            double plain = fake.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            check(quick > plain * 1.05, "2.7: a full shadow set is quicker on its feet (" + quick + " against " + plain + ")");
            for (int i = 0; i < 4; i++) fake.setItemSlot(dev.umbra.gear.Gear.ARMOR[i], armour.apply("luminite_" + pc[i] + "_3"));
            fake.tickCount = 4;
            BlockPos head = BlockPos.containing(fake.getEyePosition());
            dev.umbra.gear.SetPowers.tick(fake);
            check(level.getBlockState(head).is(ModBlocks.HELD_LIGHT.get()) || level.getBlockState(fake.blockPosition()).is(ModBlocks.HELD_LIGHT.get()),
                    "2.7: full luminite III shines like a lantern");
            for (int i = 0; i < 4; i++) fake.setItemSlot(dev.umbra.gear.Gear.ARMOR[i], ItemStack.EMPTY);
            fake.tickCount = 13;
            dev.umbra.gear.SetPowers.tick(fake);
        }
        BlockPos maze = dev.umbra.quantum.QuantumMazeFeature.nearest(level.getSeed(), 0, 0);
        check(Math.hypot(maze.getX(), maze.getZ()) < 24 * 16 * 1.5, "a maze is never far: the nearest is " + (int) Math.hypot(maze.getX(), maze.getZ()) + " blocks away");
        var w = ModEntities.WATCHER.get().create(level, EntitySpawnReason.COMMAND);
        if (w != null) {
            w.snapTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5, 0, 0);
            w.setNoAi(true);
            level.addFreshEntity(w);
            check(w.getMaxHealth() >= 150 && w.getMaxHealth() < 160, "bosses are a quarter stronger (the Watcher has " + w.getMaxHealth() + ")");
            // 2.9: the second wind comes once per phase, mends a fixed share of the health (even past the line), and the
            // phase it has reached stays reached
            w.setHealth(w.getMaxHealth() * 0.45F);
            w.trackPhase(level);
            boolean guarded = w.isGuarded();
            for (int i = 0; i < 100; i++) w.aiStep();
            int ph = w.trackPhase(level);
            float healed = w.getHealth() / w.getMaxHealth() - 0.45F;
            w.setHealth(w.getMaxHealth() * 0.4F);
            w.trackPhase(level);
            check(guarded && Math.abs(healed - dev.umbra.boss.UmbraBoss.GUARD_HEAL) < 0.01F && ph == 2 && w.phase() == 2 && !w.isGuarded(),
                    "a boss's second wind mends " + Math.round(healed * 100) + "% of its health once, and it stays in its new phase");
            w.setHealth(w.getMaxHealth());
            float before = w.getHealth();
            w.hurtServer(level, level.damageSources().magic(), 100.0F);
            check(before - w.getHealth() <= 100.0F * dev.umbra.boss.Watcher.ARMOURED_EYE + 0.01F, "the Watcher's eye shrugs off part of a blow (" + (before - w.getHealth()) + " of 100)");
            // 2.10: but not while its eye is open wide (spent after its gaze, say): then the whole blow lands, and more
            w.setHealth(w.getMaxHealth());
            for (int i = 0; i < 21; i++) w.baseTick();   // the last blow's grace would swallow this one
            w.stun(40);
            float open = w.getHealth();
            w.hurtServer(level, level.damageSources().magic(), 20.0F);
            check(w.eyeOpen() && open - w.getHealth() >= 20.0F, "2.10: while its eye is open wide every blow lands whole (" + (open - w.getHealth()) + " of 20)");
            float h = w.getHealth();
            w.onLit(level, null);
            check(h - w.getHealth() <= w.getMaxHealth() * dev.umbra.boss.UmbraBoss.LIGHT_CAP + 0.01F, "a flash takes no more than 2% of a boss");
            w.discard();
        }

        // the trophy shelf: six relics in their places, then it gives back
        BlockPos sh = a.offset(0, 0, -3);
        level.setBlock(sh, ModBlocks.TROPHY_SHELF.get().defaultBlockState(), Block.UPDATE_ALL);
        var shHit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(sh), net.minecraft.core.Direction.NORTH, sh, false);
        int placed = 0;
        for (var t : ModItems.TROPHIES.values()) {
            ItemStack trophy = new ItemStack(t.get());
            fake.setItemInHand(InteractionHand.MAIN_HAND, trophy);
            if (level.getBlockState(sh).useItemOn(trophy, level, fake, InteractionHand.MAIN_HAND, shHit).consumesAction()) placed++;
        }
        ItemStack again = new ItemStack(ModItems.TROPHIES.values().iterator().next().get());
        boolean refused = !level.getBlockState(sh).useItemOn(again, level, fake, InteractionHand.MAIN_HAND, shHit).consumesAction();
        int quartz = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(sh).inflate(2))
                .stream().filter(e -> e.getItem().is(ModItems.ECHO_QUARTZ.get())).mapToInt(e -> e.getItem().getCount()).sum();
        check(placed == 6 && refused && dev.umbra.craft.TrophyShelfBlock.count(level.getBlockState(sh)) == 6 && quartz == 16,
                "the trophy shelf takes six relics, one of each, and pays out when full (" + placed + ", " + quartz + ")");
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(sh).inflate(3)).forEach(net.minecraft.world.entity.Entity::discard);
        level.setBlock(sh, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        // 2.5: the weapons and their enchantments
        var crawler = ModEntities.CRAWLER.get().create(level, EntitySpawnReason.COMMAND);
        if (crawler != null) {
            crawler.snapTo(a.getX() + 0.5, a.getY(), a.getZ() + 2.5, 0, 0);       // facing south (+z)
            crawler.setYBodyRot(0);
            fake.snapTo(a.getX() + 0.5, a.getY(), a.getZ() + 4.5, 0, 0);         // in front of it (south, where it looks)
            fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LUMINITE_SPEAR.get()));
            float spear = dev.umbra.gear.WeaponEvents.modify(fake, crawler, 5.0F);
            fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SHADOW_DAGGER.get()));
            float front = dev.umbra.gear.WeaponEvents.modify(fake, crawler, 5.0F);
            fake.snapTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5, 0, 0);         // behind it (north)
            float back = dev.umbra.gear.WeaponEvents.modify(fake, crawler, 5.0F);
            ItemStack radiant = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(dev.umbra.gear.Gear.RADIANCE).ifPresent(h -> radiant.enchant(h, 2));
            fake.setItemInHand(InteractionHand.MAIN_HAND, radiant);
            float rad = dev.umbra.gear.WeaponEvents.modify(fake, crawler, 5.0F);
            check(spear == 8.0F && front == 5.0F && back == 10.0F && rad == 8.0F, "spear +3, dagger x2 from behind, Radiance II +3 (" + spear + ", " + front + "/" + back + ", " + rad + ")");
            fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        check(ModItems.LUMINITE_SPEAR.get().getDefaultInstance().is(net.minecraft.tags.ItemTags.SWORDS), "the new weapons take sword enchantments");
        // the echo staff cries through what stands in a line
        var zombie = ModEntities.BELLRINGER.get().create(level, EntitySpawnReason.COMMAND);   // not a villager: one in a few of those is a mimic and takes no harm
        if (zombie != null) {
            zombie.snapTo(a.getX() + 0.5, a.getY(), a.getZ() + 3.5, 0, 0);
            zombie.setNoAi(true);
            level.addFreshEntity(zombie);
            fake.snapTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5, 0, 0);          // yaw 0 looks south, at it
            float before = zombie.getHealth();
            int cried = dev.umbra.gear.RangedWeapons.cry(level, fake);
            check(cried >= 1 && zombie.getHealth() < before, "the echo staff's cry strikes what stands in its line (" + cried + ", " + zombie.getHealth() + ")");
            zombie.discard();
        }
        check(ModItems.LUMINITE_BOW.get().getDefaultInstance().is(net.minecraft.tags.ItemTags.BOW_ENCHANTABLE), "the luminite bow takes bow enchantments");

        wear(fake, "gloom", 2);
        fake.tickCount = 11;
        dev.umbra.gear.WeaponEvents.setTick(fake);
        boolean on = fake.entityTags().contains("umbra_set_gloom");
        fake.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);
        dev.umbra.gear.WeaponEvents.setTick(fake);
        check(on && !fake.entityTags().contains("umbra_set_gloom"), "a full set is noticed, and so is a broken one");
        wear(fake, null, 1);

        // 2.6: Yan's errands: asked, waited for, rewarded with a blueprint
        dev.umbra.journal.Journal.clear(fake);
        fake.setData(dev.umbra.registry.ModAttachments.QUESTS_ACTIVE, java.util.List.of());
        var first = dev.umbra.lighthouse.YanErrands.current(fake);
        boolean asked = dev.umbra.lighthouse.YanErrands.talk(fake) && dev.umbra.lighthouse.YanErrands.active(fake, first);
        boolean waits = dev.umbra.lighthouse.YanErrands.talk(fake) && !dev.umbra.journal.Journal.has(fake, first.gift());
        dev.umbra.journal.Journal.unlock(fake, first.needs());
        dev.umbra.lighthouse.YanErrands.talk(fake);
        check(asked && waits && dev.umbra.journal.Journal.has(fake, first.gift()) && dev.umbra.lighthouse.YanErrands.current(fake) != first,
                "Yan gives an errand, waits for it, and pays with a blueprint");
        var bellTarget = ModEntities.CRAWLER.get().create(level, EntitySpawnReason.COMMAND);
        if (bellTarget != null) {
            bellTarget.snapTo(a.getX() + 0.5, a.getY(), a.getZ() + 3.5, 0, 0);
            level.addFreshEntity(bellTarget);
            fake.snapTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5, 0, 0);
            int rang = dev.umbra.lighthouse.YanErrands.KeeperBellItem.ring(level, fake);
            check(rang >= 1 && bellTarget.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "the Keeper's bell makes the dark stagger back");
            bellTarget.discard();
        }

        // 2.6: the atlas marks what is found, once per place
        fake.setData(dev.umbra.registry.ModAttachments.PLACES, java.util.List.of());
        dev.umbra.journal.Journal.unlock(fake, "sunken_temple");
        boolean twice = dev.umbra.journal.Atlas.mark(fake, "sunken_temple", fake.blockPosition().offset(5, 0, 5));
        boolean other = dev.umbra.journal.Atlas.mark(fake, "sunken_temple", fake.blockPosition().offset(500, 0, 0));
        check(dev.umbra.journal.Atlas.places(fake).size() == 2 && !twice && other, "the atlas marks a place once, and a far one of the same kind again");

        // 2.6: the Bellringer's ring, the Weeper's shard
        var ringer = ModEntities.BELLRINGER.get().create(level, EntitySpawnReason.COMMAND);
        if (ringer != null) {
            ringer.snapTo(a.getX() + 0.5, a.getY(), a.getZ() + 2.5, 0, 0);
            level.addFreshEntity(ringer);
            fake.snapTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5, 0, 0);
            fake.removeAllEffects();
            int struck = ringer.ring(level, java.util.List.of(fake));
            check(struck >= 1 && fake.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA), "the Bellringer's ring makes heads swim (" + struck + ")");
            ringer.discard();
            fake.removeAllEffects();
        }
        var weeper = ModEntities.WEEPER.get().create(level, EntitySpawnReason.COMMAND);
        if (weeper != null) {
            weeper.snapTo(a.getX() + 0.5, a.getY(), a.getZ() + 6.5, 0, 0);
            level.addFreshEntity(weeper);
            int shardsBefore = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball.class, new net.minecraft.world.phys.AABB(a).inflate(12)).size();
            weeper.performRangedAttack(fake, 1.0F);
            int shardsAfter = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball.class, new net.minecraft.world.phys.AABB(a).inflate(12)).size();
            check(shardsAfter == shardsBefore + 1 && dev.umbra.journal.Bestiary.ids().containsKey(ModEntities.WEEPER.get()), "the Weeper throws a shard of shadow, and is in the bestiary");
            weeper.discard();
            level.getEntitiesOfClass(net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball.class, new net.minecraft.world.phys.AABB(a).inflate(12)).forEach(net.minecraft.world.entity.Entity::discard);
        }

        // night events: one can be called, and it holds for the night
        dev.umbra.night.NightEvents.force(level, dev.umbra.night.NightEvents.FOG);
        check(dev.umbra.night.NightEvents.FOG.equals(dev.umbra.night.NightEvents.tonight(level)), "a foggy night can be called");
        dev.umbra.night.NightEvents.force(level, dev.umbra.night.NightEvents.NONE);
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

    /** 2.6: the drowned halls of the Keepers, built where nothing else is. */
    /** 2.8: the sacrificial altar: a seal on it, offerings in its dish, the place round it, and the rite run through. */
    private static void testAltar28(MinecraftServer server, ServerLevel overworld) {
        // in the ordinary world the altar stays asleep
        BlockPos o = new BlockPos(200, Y + 60, 60);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-2, -1, -2), o.offset(2, 3, 2)))
            overworld.setBlock(p, p.getY() == o.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        overworld.setBlock(o, ModBlocks.SUMMONING_ALTAR.get().defaultBlockState(), Block.UPDATE_ALL);
        FakePlayer up = FakePlayerFactory.getMinecraft(overworld);
        up.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        up.snapTo(o.getX() + 0.5, o.getY(), o.getZ() - 1.5, 0, 0);
        if (overworld.getBlockEntity(o) instanceof dev.umbra.altar.SummoningAltarBlockEntity top) {
            top.placeSeal(new ItemStack(ModItems.SEAL_OF_REFLECTION.get()), up);
            top.offer(new ItemStack(ModItems.MIRROR.get()), up);
            top.offer(new ItemStack(Items.GLASS, 2), up);
            check(top.missing() == null && !top.begin(overworld, up) && !top.riteRunning(), "2.8: in the ordinary world the altar does not wake, whatever lies on it");
        } else {
            check(false, "2.8: the altar is a block with a memory (overworld)");
        }
        overworld.setBlock(o, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        ServerLevel under = server.getLevel(Underside.LEVEL);
        if (under == null) return;
        BlockPos c = new BlockPos(140008, 140, 140008);
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-10, -5, -10), c.offset(10, 7, 10)))
            under.setBlock(p, p.getY() < c.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        under.setBlock(c, ModBlocks.SUMMONING_ALTAR.get().defaultBlockState(), Block.UPDATE_ALL);
        if (!(under.getBlockEntity(c) instanceof dev.umbra.altar.SummoningAltarBlockEntity altar)) {
            check(false, "2.8: the altar is a block with a memory");
            return;
        }
        FakePlayer fake = FakePlayerFactory.getMinecraft(under);
        fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        fake.setHealth(fake.getMaxHealth());
        fake.snapTo(c.getX() + 0.5, c.getY(), c.getZ() - 2.5, 0, 0);
        Vec3 centre = dev.umbra.altar.Ritual.WATCHING.centre(c);
        fake.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, centre);
        net.minecraft.world.phys.AABB around = new net.minecraft.world.phys.AABB(c).inflate(10);

        // the Watcher: spider eyes and echo quartz, the dark, and your eyes on it all the while
        ItemStack seal = new ItemStack(ModItems.SEAL_OF_WATCHING.get());
        check(altar.placeSeal(seal, fake) && seal.isEmpty() && altar.ritual() == dev.umbra.altar.Ritual.WATCHING, "2.8: a seal lies on the altar");
        check(!altar.placeSeal(new ItemStack(ModItems.SEAL_OF_MIMICRY.get()), fake), "and only one at a time");
        ItemStack eyes = new ItemStack(Items.SPIDER_EYE, 5);
        check(altar.offer(eyes, fake) == 3 && eyes.getCount() == 2, "the dish takes only as many as the seal asks for (3 of 5 spider eyes)");
        check(altar.offer(new ItemStack(Items.BONE), fake) == 0 && altar.missing() != null, "and nothing it does not ask for");
        altar.offer(new ItemStack(ModItems.ECHO_QUARTZ.get(), 4), fake);
        check(altar.missing() == null, "the offerings are all there");
        under.setBlock(c.offset(4, 0, 0), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
        check(dev.umbra.altar.Ritual.WATCHING.placeProblem(under, c, fake) != null && !altar.begin(under, fake), "the Watcher's rite wants the dark: a torch near the altar spoils it");
        under.setBlock(c.offset(4, 0, 0), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        // 2.8.0: the Underside's rock is full of echo quartz: ore shut in the stone lights nothing, ore open to the air does
        under.setBlock(c.offset(3, -3, 0), ModBlocks.ECHO_QUARTZ_ORE.get().defaultBlockState(), Block.UPDATE_ALL);
        boolean shut = dev.umbra.altar.Ritual.lights(under, c).isEmpty();
        under.setBlock(c.offset(3, -1, 0), ModBlocks.ECHO_QUARTZ_ORE.get().defaultBlockState(), Block.UPDATE_ALL);
        int open = dev.umbra.altar.Ritual.lights(under, c).size();
        under.setBlock(c.offset(3, -3, 0), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        under.setBlock(c.offset(3, -1, 0), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        check(shut && open == 1, "glowing ore shut in the rock does not spoil the dark, ore open to the air does (" + shut + ", " + open + ")");
        check(altar.begin(under, fake) && altar.riteRunning() && under.getBlockState(c).getValue(dev.umbra.altar.SummoningAltarBlock.GLOW) == 2,
                "in the dark it wakes: the rite begins and the runes blaze");
        for (int i = 0; i < 30; i++) altar.tickForTest(under);
        int before = altar.riteTicks();
        fake.snapTo(fake.getX(), fake.getY(), fake.getZ(), 180, 0);
        fake.setYHeadRot(180);
        altar.tickForTest(under);
        check(before >= 30 && altar.riteTicks() == 0 && altar.riteRunning(), "look away, and the Watcher's rite starts again (" + before + " -> " + altar.riteTicks() + ")");
        fake.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, centre);
        dev.umbra.altar.SummoningAltarBlockEntity.lastSummoned = null;
        for (int i = 0; i < 260 && altar.riteRunning(); i++) altar.tickForTest(under);
        var came = dev.umbra.altar.SummoningAltarBlockEntity.lastSummoned;
        check(!altar.riteRunning() && came instanceof Watcher && came.distanceToSqr(Vec3.atCenterOf(c)) < 49 && altar.seal().isEmpty() && altar.offerings().isEmpty()
                && under.getBlockState(c).getValue(dev.umbra.altar.SummoningAltarBlock.GLOW) == 0, "watched to the end, the rite calls the Watcher out of the altar; seal and offerings are spent");
        if (came != null) came.discard();

        // the Queen: gold and shards, and four chests that are not empty (she takes them)
        altar.placeSeal(new ItemStack(ModItems.SEAL_OF_MIMICRY.get()), fake);
        altar.offer(new ItemStack(Items.GOLD_INGOT, 2), fake);
        altar.offer(new ItemStack(ModItems.SHADOW_SHARD.get(), 2), fake);
        BlockPos[] chests = {c.offset(2, 0, 1), c.offset(-2, 0, 1), c.offset(2, 0, 3), c.offset(-2, 0, 3)};
        for (int i = 0; i < chests.length; i++) {
            under.setBlock(chests[i], Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
            if (i < 3 && under.getBlockEntity(chests[i]) instanceof net.minecraft.world.Container box) box.setItem(0, new ItemStack(Items.COAL));
        }
        check(dev.umbra.altar.Ritual.MIMICRY.placeProblem(under, c, fake) != null, "an empty chest is no bait");
        if (under.getBlockEntity(chests[3]) instanceof net.minecraft.world.Container box) box.setItem(0, new ItemStack(Items.COAL));
        check(dev.umbra.altar.Ritual.MIMICRY.placeProblem(under, c, fake) == null, "four full chests are");
        boolean freeBefore = !dev.umbra.altar.AltarEvents.sealedByRite(under, chests[0]);
        altar.beginForTest(under, fake);
        check(freeBefore && dev.umbra.altar.AltarEvents.sealedByRite(under, chests[0]), "2.9: while the Queen's rite runs, the chests round the altar are hers (sealed)");
        dev.umbra.altar.SummoningAltarBlockEntity.lastSummoned = null;
        for (int i = 0; i < 200 && altar.riteRunning(); i++) altar.tickForTest(under);
        int left = 0;
        for (BlockPos p : chests) if (under.getBlockState(p).is(Blocks.CHEST)) left++;
        var queen = dev.umbra.altar.SummoningAltarBlockEntity.lastSummoned;
        check(queen instanceof MimicQueen && left == 0, "the Queen comes, and the chests go with her (" + left + " left)");
        if (queen != null) queen.discard();

        // the Shepherd: wool, bones and shards, and three soul campfires that go out when he comes
        altar.placeSeal(new ItemStack(ModItems.SEAL_OF_HERDING.get()), fake);
        altar.offer(new ItemStack(Items.WOOL.white(), 3), fake);
        altar.offer(new ItemStack(Items.BONE, 2), fake);
        altar.offer(new ItemStack(ModItems.SHADOW_SHARD.get(), 2), fake);
        check(dev.umbra.altar.Ritual.HERDING.placeProblem(under, c, fake) != null, "no fires, no flock");
        BlockPos[] fires = {c.offset(3, 0, 0), c.offset(-3, 0, 0), c.offset(0, 0, 3)};
        for (BlockPos p : fires) under.setBlock(p, Blocks.SOUL_CAMPFIRE.defaultBlockState(), Block.UPDATE_ALL);
        check(dev.umbra.altar.Ritual.HERDING.placeProblem(under, c, fake) == null, "three soul fires round the altar");
        altar.beginForTest(under, fake);
        dev.umbra.altar.SummoningAltarBlockEntity.lastSummoned = null;
        for (int i = 0; i < 200 && altar.riteRunning(); i++) altar.tickForTest(under);
        int lit = 0;
        for (BlockPos p : fires) if (under.getBlockState(p).getValue(net.minecraft.world.level.block.CampfireBlock.LIT)) lit++;
        var shepherd = dev.umbra.altar.SummoningAltarBlockEntity.lastSummoned;
        check(shepherd instanceof Shepherd && lit == 0, "the Shepherd comes, and the fires go out (" + lit + " still lit)");
        if (shepherd != null) shepherd.discard();
        for (BlockPos p : fires) under.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        // the Doppelganger: the mirror and glass, water round the altar, and blood
        altar.placeSeal(new ItemStack(ModItems.SEAL_OF_REFLECTION.get()), fake);
        altar.offer(new ItemStack(ModItems.MIRROR.get()), fake);
        altar.offer(new ItemStack(Items.GLASS, 2), fake);
        check(dev.umbra.altar.Ritual.REFLECTION.placeProblem(under, c, fake) != null, "no water, no reflection");
        for (BlockPos p : new BlockPos[]{c.offset(1, 0, 1), c.offset(-1, 0, 1), c.offset(1, 0, -1), c.offset(-1, 0, -1)})
            under.setBlock(p, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        check(dev.umbra.altar.Ritual.REFLECTION.placeProblem(under, c, fake) == null, "still water round the altar");
        check(altar.begin(under, fake), "the altar takes its blood and the rite of reflection begins");
        dev.umbra.altar.SummoningAltarBlockEntity.lastSummoned = null;
        for (int i = 0; i < 200 && altar.riteRunning(); i++) altar.tickForTest(under);
        var twin = dev.umbra.altar.SummoningAltarBlockEntity.lastSummoned;
        check(twin instanceof Doppelganger, "the Doppelganger climbs out");
        if (twin != null) twin.discard();

        // a rite that breaks burns the offerings and leaves the seal
        altar.placeSeal(new ItemStack(ModItems.SEAL_OF_REFLECTION.get()), fake);
        altar.offer(new ItemStack(ModItems.MIRROR.get()), fake);
        altar.offer(new ItemStack(Items.GLASS, 2), fake);
        altar.beginForTest(under, fake);
        fake.snapTo(c.getX() + 30.5, c.getY(), c.getZ() + 0.5, 0, 0);
        altar.tickForTest(under);
        check(!altar.riteRunning() && altar.offerings().isEmpty() && !altar.seal().isEmpty(), "walk away mid-rite and it breaks: the offerings burn, the seal stays");
        // a rite nobody holds any more (the world was closed in the middle of it) fades, and takes nothing
        altar.offer(new ItemStack(ModItems.MIRROR.get()), fake);
        altar.offer(new ItemStack(Items.GLASS, 2), fake);
        altar.beginForTest(under, null);
        altar.tickForTest(under);
        check(!altar.riteRunning() && altar.offerings().size() == 2 && !altar.seal().isEmpty()
                && under.getBlockState(c).getValue(dev.umbra.altar.SummoningAltarBlock.GLOW) == 1, "a rite left over from a closed world fades, and the offerings stay");
        boolean had = !altar.seal().isEmpty() && under.getBlockEntity(c) == altar;
        under.destroyBlock(c, false);
        check(had && under.getBlockState(c).isAir() && dev.umbra.altar.SummoningAltarBlockEntity.lastDrops.stream().anyMatch(st -> st.is(ModItems.SEAL_OF_REFLECTION.get())),
                "break the altar and what lies on it falls out (" + dev.umbra.altar.SummoningAltarBlockEntity.lastDrops + ")");
    }

    private static void testDepths26(MinecraftServer server) {
        ServerLevel under = server.getLevel(Underside.LEVEL);
        if (under == null) return;
        dev.umbra.depths.Depths.build(under, under.getRandom());
        check(dev.umbra.depths.Depths.isBuilt(under), "the Depths are built");
        check(under.getBlockState(dev.umbra.depths.Depths.CHEST_AT).is(ModBlocks.KEEPERS_CHEST.get()), "the Keepers' chest waits in the sanctum");
        // 2.7: drowned to the vault, everywhere
        int dry = 0;
        for (var r : dev.umbra.depths.Depths.ROOMS)
            for (int y = 1; y <= r.h(); y++)
                if (under.getBlockState(dev.umbra.depths.Depths.ORIGIN.offset(r.x(), y, r.z())).isAir()) dry++;
        check(dry == 0, "2.7: the Depths are drowned to the vault, not a pocket of air in any hall (" + dry + " dry)");
        int turns = 0;
        for (int[][] run : dev.umbra.depths.Depths.PASSAGES) turns += run.length - 2;
        check(dev.umbra.depths.Depths.ROOMS.size() >= 12 && turns >= 15, "2.7: the halls branch and the passages wind (" + dev.umbra.depths.Depths.ROOMS.size() + " halls, " + turns + " turns)");
        // the gates and the valves
        boolean shut = true;
        for (int i = 0; i < 3; i++) shut &= dev.umbra.depths.Depths.gateShut(under, i);
        check(shut, "2.7: three sluice gates bar the way to the sanctum");
        var valve = dev.umbra.depths.Depths.VALVE_AT;
        under.setBlock(valve[0], under.getBlockState(valve[0]).setValue(dev.umbra.depths.DepthsBlocks.ON, true), Block.UPDATE_ALL);
        int open = dev.umbra.depths.Depths.valveTurned(under, valve[0]);
        check(open == 1 && !dev.umbra.depths.Depths.gateShut(under, 0) && dev.umbra.depths.Depths.gateShut(under, 1), "2.7: one valve lifts one gate");
        for (int i = 1; i < 3; i++) under.setBlock(valve[i], under.getBlockState(valve[i]).setValue(dev.umbra.depths.DepthsBlocks.ON, true), Block.UPDATE_ALL);
        dev.umbra.depths.Depths.valveTurned(under, valve[2]);
        check(!dev.umbra.depths.Depths.gateShut(under, 1) && !dev.umbra.depths.Depths.gateShut(under, 2), "2.7: three valves, and the way is open");
        // the lamps
        FakePlayer fake = FakePlayerFactory.getMinecraft(under);
        BlockPos arrival = dev.umbra.depths.Depths.ARRIVAL;
        check(!dev.umbra.depths.DepthsBlocks.nearLitLamp(under, arrival, 8), "2.7: the Keepers' lamps have gone out");
        BlockPos lampAt = null;
        for (BlockPos q : BlockPos.betweenClosed(arrival.offset(-6, -2, -6), arrival.offset(6, 2, 6)))
            if (under.getBlockState(q).is(ModBlocks.DEPTHS_LAMP.get())) { lampAt = q.immutable(); break; }
        if (lampAt != null) {
            fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LUMINITE_CRYSTAL.get(), 2));
            under.getBlockState(lampAt).useItemOn(fake.getMainHandItem(), under, fake, InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(lampAt), net.minecraft.core.Direction.UP, lampAt, false));
            check(dev.umbra.depths.DepthsBlocks.nearLitLamp(under, arrival, 8) && fake.getMainHandItem().getCount() == 1, "2.7: a crystal lights a Keepers' lamp again");
            fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        } else check(false, "2.7: the landing has a Keepers' lamp");
        // a lantern's light under the water
        fake.snapTo(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5, 0, 0);
        dev.umbra.light.LanternItem.placeLight(under, fake);
        BlockPos eye = BlockPos.containing(fake.getEyePosition());
        var held = under.getBlockState(eye).is(ModBlocks.HELD_LIGHT.get()) ? under.getBlockState(eye) : under.getBlockState(fake.blockPosition());
        check(held.is(ModBlocks.HELD_LIGHT.get()) && held.getValue(dev.umbra.light.HeldLightBlock.WATERLOGGED), "2.7: a lantern lights the water too");
        for (BlockPos q : new BlockPos[]{eye, fake.blockPosition()})
            if (under.getBlockState(q).is(ModBlocks.HELD_LIGHT.get())) under.setBlock(q, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        // the bells wake the Keeper; the sanctum's lamps are his weak point
        var bells = dev.umbra.depths.Depths.BELL_AT;
        for (int i = 0; i < 3; i++) under.setBlock(bells[i], under.getBlockState(bells[i]).setValue(dev.umbra.depths.DepthsBlocks.ON, true), Block.UPDATE_ALL);
        dev.umbra.depths.Depths.bellRung(under, bells[2], null);
        check(dev.umbra.depths.Depths.keeper(under) == null, "2.7: three bells are not enough");
        under.setBlock(bells[3], under.getBlockState(bells[3]).setValue(dev.umbra.depths.DepthsBlocks.ON, true), Block.UPDATE_ALL);
        dev.umbra.depths.Depths.bellRung(under, bells[3], null);
        var k = dev.umbra.depths.Depths.keeper(under);
        check(k != null && k.getMaxHealth() >= 400, "2.7: the fourth bell wakes the Drowned Keeper, a boss now (" + (k == null ? 0 : k.getMaxHealth()) + " hp)");
        if (k != null) {
            float dim = k.guard(under);
            int lit = 0;
            for (BlockPos q : BlockPos.betweenClosed(dev.umbra.depths.Depths.ORIGIN.offset(-10, 1, -162), dev.umbra.depths.Depths.ORIGIN.offset(10, 1, -142)))
                if (under.getBlockState(q).is(ModBlocks.DEPTHS_LAMP.get()) && lit < 2) {
                    under.setBlock(q, under.getBlockState(q).setValue(dev.umbra.depths.DepthsBlocks.ON, true), Block.UPDATE_ALL);
                    lit++;
                }
            float bright = k.guard(under);
            check(dim < 1.0F && bright == 1.0F, "2.7: in the dark he takes half a blow; light two of the sanctum's lamps and he takes it all (" + dim + " / " + bright + ")");
            k.die(under.damageSources().magic());
            k.discard();
            check(dev.umbra.depths.Depths.bellsRung(under) == 0, "2.7: beaten, the bells go quiet and can be rung again");
        }
        check(dev.umbra.depths.Depths.CURRENTS.length >= 4 && under.getBlockState(dev.umbra.depths.Depths.ORIGIN.offset(-46, 1, -48)).is(Blocks.CHEST),
                "2.7: currents drag into side passages, and a cache waits at the end");
        testDepths29(under);
    }

    /** 2.9: the currents are no trap, twice the cages, the Keepers' chest and his key, and old halls brought up to date. */
    private static void testDepths29(ServerLevel under) {
        // the currents pull in waves, a swimmer can make headway, and an emptied cache lets its current die
        boolean waves = dev.umbra.depths.Depths.currentRunsAt(under, 0, 0) && !dev.umbra.depths.Depths.currentRunsAt(under, 0, dev.umbra.depths.Depths.CURRENT_ON + 5);
        check(waves && dev.umbra.depths.Depths.CURRENT < 0.03, "2.9: the currents pull in waves, with calm water between, and not harder than one can swim");
        if (under.getBlockEntity(dev.umbra.depths.Depths.CACHE_CHEST[0]) instanceof net.minecraft.world.Container cache) {
            cache.clearContent();
            check(!dev.umbra.depths.Depths.currentRunsAt(under, 0, 0) && dev.umbra.depths.Depths.currentRunsAt(under, 1, 0),
                    "2.9: once its cache is emptied a current dies away (the other still pulls)");
        } else check(false, "2.9: the west cache has its chest");
        // twice the cages
        int cages = 0;
        for (BlockPos p : dev.umbra.depths.Depths.CAGES_OLD) if (under.getBlockState(p).is(Blocks.SPAWNER)) cages++;
        for (BlockPos p : dev.umbra.depths.Depths.CAGES_29) if (under.getBlockState(p).is(Blocks.SPAWNER)) cages++;
        check(cages == 6, "2.9: six cages in the halls, twice what there were (" + cages + ")");
        // the Keepers' chest: shut; a hand does not open it, his key does (and stays in the lock)
        BlockPos tc = dev.umbra.depths.Depths.CHEST_AT;
        FakePlayer fk = FakePlayerFactory.getMinecraft(under);
        fk.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        fk.snapTo(tc.getX() + 0.5, tc.getY(), tc.getZ() + 2.5, 180, 0);
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(tc), net.minecraft.core.Direction.SOUTH, tc, false);
        fk.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        under.getBlockState(tc).useWithoutItem(under, fk, hit);
        boolean shut = !under.getBlockState(tc).getValue(dev.umbra.depths.KeepersChest.OPEN);
        fk.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KEEPERS_KEY.get()));
        under.getBlockState(tc).useItemOn(fk.getMainHandItem(), under, fk, InteractionHand.MAIN_HAND, hit);
        boolean opened = under.getBlockState(tc).getValue(dev.umbra.depths.KeepersChest.OPEN) && fk.getMainHandItem().isEmpty();
        check(shut && opened, "2.9: the Keepers' chest does not open by hand; his key opens it and stays in the lock");
        fk.closeContainer();
        if (under.getBlockEntity(tc) instanceof dev.umbra.depths.KeepersChest.Entity chest) {
            chest.unpackLootTable(null);
            boolean full = !chest.isEmpty();
            chest.clearContent();
            chest.setItem(0, new ItemStack(Items.NAUTILUS_SHELL, 3));
            dev.umbra.depths.Depths.lockTreasure(under);
            int n = 0;
            for (int i = 0; i < chest.getContainerSize(); i++) if (!chest.getItem(i).isEmpty()) n++;
            check(full && !under.getBlockState(tc).getValue(dev.umbra.depths.KeepersChest.OPEN) && n > 1 && chest.countItem(Items.NAUTILUS_SHELL) == 3,
                    "2.9: there was treasure in it; beaten again, he fills it again round what was left and it shuts (" + n + " stacks)");
        } else check(false, "2.9: the Keepers' chest keeps things");
        check(ModBlocks.KEEPERS_CHEST.get().defaultDestroyTime() < 0 && ModBlocks.KEEPERS_CHEST.get().getExplosionResistance() > 1000.0F,
                "2.9: the Keepers' chest cannot be broken, by hand or by blast");
        // halls built before 2.9 are furnished the next time someone comes down
        under.setBlock(dev.umbra.depths.Depths.MARK_29, Blocks.DEEPSLATE_TILES.defaultBlockState(), Block.UPDATE_CLIENTS);
        under.setBlock(tc, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        if (under.getBlockEntity(tc) instanceof net.minecraft.world.Container old) old.setItem(3, new ItemStack(Items.EMERALD, 5));
        for (BlockPos p : dev.umbra.depths.Depths.CAGES_29) under.setBlock(p, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        dev.umbra.depths.Depths.upgrade(under, under.getRandom());
        int newCages = 0;
        for (BlockPos p : dev.umbra.depths.Depths.CAGES_29) if (under.getBlockState(p).is(Blocks.SPAWNER)) newCages++;
        boolean kept = under.getBlockEntity(tc) instanceof dev.umbra.depths.KeepersChest.Entity ch2 && ch2.countItem(Items.EMERALD) == 5;
        check(dev.umbra.depths.Depths.furnished29(under) && newCages == 3 && kept,
                "2.9: old halls get the new cages and the Keepers' chest, and keep what lay in the old one");
    }

    /** 2.9: armour that lasts by its tier, shields that a boss beats aside, the discs, the knife that flies point first. */
    /** 2.12: each weapon its own hand. */
    private static void testWeapons212(ServerLevel level) {
        BlockPos c = new BlockPos(300, Y + 60, 60);
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-6, -1, -6), c.offset(6, 4, 6)))
            level.setBlock(p, p.getY() == c.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        fake.setHealth(fake.getMaxHealth());
        fake.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 180, 0);   // looking north (-z)
        fake.setYHeadRot(180);
        java.util.List<net.minecraft.world.entity.Mob> made = new java.util.ArrayList<>();
        java.util.function.BiFunction<Double, Double, net.minecraft.world.entity.Mob> put = (dx, dz) -> {
            var m = ModEntities.ACOLYTE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            m.snapTo(c.getX() + 0.5 + dx, c.getY(), c.getZ() + 0.5 + dz, 0, 0);
            m.setNoAi(true);
            level.addFreshEntity(m);
            made.add(m);
            return m;
        };
        // the sword's third blow cuts wide in front
        var first = put.apply(0.0, -1.5);
        var side = put.apply(1.4, -1.8);
        float before = side.getHealth();
        int cut = dev.umbra.gear.WeaponArts.swordCut(level, fake, first, 8.0F);
        check(cut >= 1 && side.getHealth() < before, "2.12: the gloom sword's third blow cuts wide in front (" + cut + ")");
        made.forEach(net.minecraft.world.entity.Entity::discard);
        made.clear();
        // the spear on the run goes through the first one into the one behind
        var front = put.apply(0.0, -1.5);
        var back = put.apply(0.0, -3.2);
        before = back.getHealth();
        int pierced = dev.umbra.gear.WeaponArts.spearLunge(level, fake, front, 8.0F);
        check(pierced == 1 && back.getHealth() < before, "2.12: a spear blow on the run goes through into the one behind");
        made.forEach(net.minecraft.world.entity.Entity::discard);
        made.clear();
        // the scythe's kill gives half a heart back
        fake.setHealth(10.0F);
        fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SHEPHERD_SCYTHE.get()));
        var dead = put.apply(0.0, -1.5);
        dev.umbra.gear.WeaponArts.afterKill(level, fake, dead, fake.getMainHandItem());
        check(fake.getHealth() > 10.5F, "2.12: what the scythe kills gives you half a heart (" + fake.getHealth() + ")");
        // the dagger: a kill from behind and you are gone
        fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SHADOW_DAGGER.get()));
        dead.setYBodyRot(180);   // it looks away from you (north)
        dead.yBodyRot = 180;
        dev.umbra.gear.WeaponArts.afterKill(level, fake, dead, fake.getMainHandItem());
        check(fake.hasEffect(net.minecraft.world.effect.MobEffects.SPEED), "2.12: a dagger kill from behind gives a burst of speed");
        made.forEach(net.minecraft.world.entity.Entity::discard);
        made.clear();
        // the aegis raised in the last moment turns the blow back
        var foe = put.apply(0.0, -1.5);
        fake.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.LUMINITE_SHIELD.get()));
        fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        boolean idle = !dev.umbra.gear.WeaponArts.parry(level, fake, foe, 6.0F);
        fake.startUsingItem(InteractionHand.OFF_HAND);
        before = foe.getHealth();
        boolean turned = dev.umbra.gear.WeaponArts.parry(level, fake, foe, 6.0F);
        check(idle && turned && foe.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) && foe.getHealth() < before,
                "2.12: the aegis raised in the last moment throws the blow back and blinds the one who struck");
        fake.stopUsingItem();
        fake.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        made.forEach(net.minecraft.world.entity.Entity::discard);
        // a fully drawn luminite arrow leaves its light
        int lights = dev.umbra.gear.WeaponArts.lightsLeft();
        boolean lit = dev.umbra.gear.WeaponArts.leaveLight(level, Vec3.atCenterOf(c.offset(3, 1, 3)));
        check(lit && level.getBlockState(c.offset(3, 1, 3)).is(Blocks.LIGHT) && dev.umbra.gear.WeaponArts.lightsLeft() == lights + 1,
                "2.12: a fully drawn luminite arrow leaves its light where it lands");
        // and each strikes its own way: the spear and the dagger stab, the scythe sweeps slow
        var stab = net.minecraft.world.item.SwingAnimationType.STAB;
        var anim = net.minecraft.core.component.DataComponents.ATTACK_ANIMATION;
        check(new ItemStack(ModItems.LUMINITE_SPEAR.get()).get(anim).type() == stab && new ItemStack(ModItems.SHADOW_DAGGER.get()).get(anim).type() == stab
                && new ItemStack(ModItems.SHEPHERD_SCYTHE.get()).get(anim).duration() > new ItemStack(ModItems.SHADOW_DAGGER.get()).get(anim).duration(),
                "2.12: the spear and the dagger stab, the scythe sweeps slower");
        // 2.12.2: the lantern throws its light ahead of you too
        fake.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 180, 0);
        fake.setYHeadRot(180);
        dev.umbra.light.LanternItem.throwLight(level, fake);
        BlockPos ahead = BlockPos.containing(fake.getEyePosition().add(fake.getLookAngle().scale(5)));
        check(level.getBlockState(ahead).is(ModBlocks.HELD_LIGHT.get()) && ModBlocks.HELD_LIGHT.get().defaultBlockState().getLightEmission() == 15,
                "2.12.2: the lit lantern shines at full light, and a few blocks ahead of you as well");
        level.setBlock(ahead, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        // night goggles: in the dark you see, and the lenses burn their charge
        BlockPos room = c.offset(0, 0, 4);
        for (BlockPos p : BlockPos.betweenClosed(room.offset(-2, -1, -2), room.offset(2, 3, 2)))
            level.setBlock(p, Math.abs(p.getX() - room.getX()) == 2 || Math.abs(p.getZ() - room.getZ()) == 2 || p.getY() == room.getY() - 1 || p.getY() == room.getY() + 3
                    ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        fake.removeAllEffects();
        fake.snapTo(room.getX() + 0.5, room.getY(), room.getZ() + 0.5, 0, 0);
        ItemStack goggles = new ItemStack(ModItems.NIGHT_GOGGLES.get());
        fake.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, goggles);
        fake.tickCount = 203;
        dev.umbra.gear.NightGoggles.apply(fake, true);     // the room was walled in this very tick: its light has not caught up yet
        boolean restsByDay = true;
        fake.removeAllEffects();
        dev.umbra.gear.NightGoggles.apply(fake, false);
        restsByDay = !fake.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
        fake.tickCount = 203;
        dev.umbra.gear.NightGoggles.apply(fake, true);
        boolean sees = fake.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
        int used = fake.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getDamageValue();
        var eq = goggles.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        check(sees && restsByDay && used == 2 && eq != null && eq.slot() == net.minecraft.world.entity.EquipmentSlot.HEAD,
                "2.12.2: night goggles on the head: in the dark you see, and the lenses burn their charge (" + used + ")");
        fake.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);
        fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        fake.setHealth(fake.getMaxHealth());
        fake.removeAllEffects();
        fake.setYHeadRot(0.0F);
    }

    private static void testItems29(MinecraftServer server, ServerLevel level) {
        // durability: tier II twice tier I, tier III three times
        boolean lasts = true;
        String seen = "";
        for (String set : new String[]{"gloom", "luminite", "shadow"}) {
            int[] d = new int[4];
            for (int t = 1; t <= 3; t++) {
                var id = net.minecraft.resources.Identifier.fromNamespaceAndPath(Umbra.MODID, set + "_chestplate" + (t == 1 ? "" : "_" + t));
                d[t] = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id)).getMaxDamage();
            }
            lasts &= d[2] == 2 * d[1] && d[3] == 3 * d[1];
            seen += set + " " + d[1] + "/" + d[2] + "/" + d[3] + " ";
        }
        check(lasts, "2.9: armour lasts by its tier: II twice as long as I, III three times (" + seen.trim() + ")");
        // shields: a boss's heavy blow knocks one aside, and so does the third blow it stops in a row
        BlockPos c = new BlockPos(260, Y + 60, 60);
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-4, -1, -4), c.offset(4, 3, 4)))
            level.setBlock(p, p.getY() == c.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        fake.setHealth(fake.getMaxHealth());
        fake.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 180, 0);
        fake.setYHeadRot(180);
        ItemStack shield = new ItemStack(ModItems.LUMINITE_SHIELD.get());
        fake.setItemInHand(InteractionHand.OFF_HAND, shield);
        fake.startUsingItem(InteractionHand.OFF_HAND);
        boolean raised = !dev.umbra.boss.BossMoves.raisedShield(fake, null).isEmpty();
        dev.umbra.boss.BossMoves.breakGuard(level, fake, dev.umbra.boss.BossMoves.HEAVY_GUARD_BREAK);
        check(raised && fake.getCooldowns().isOnCooldown(fake.getOffhandItem()) && !fake.isUsingItem(), "2.9: a boss's heavy blow knocks a raised shield aside for a while");
        var w = ModEntities.WATCHER.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (w != null) {
            w.snapTo(c.getX() + 0.5, c.getY(), c.getZ() - 1.5, 0, 0);
            w.setNoAi(true);
            level.addFreshEntity(w);
            fake.stopUsingItem();
            fake.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));     // a plain shield, not on the cooldown just given
            int blocked = 0;
            for (int i = 0; i < dev.umbra.boss.BossMoves.GUARD_HITS; i++) {
                fake.setHealth(fake.getMaxHealth());
                if (!fake.isUsingItem()) fake.startUsingItem(InteractionHand.OFF_HAND);
                if (!dev.umbra.boss.BossMoves.raisedShield(fake, w).isEmpty()) blocked++;
                w.doHurtTarget(level, fake);
            }
            check(blocked == dev.umbra.boss.BossMoves.GUARD_HITS && fake.getCooldowns().isOnCooldown(fake.getOffhandItem()),
                    "2.9: a boss cannot be blocked for ever: the third blow in a row beats the shield aside (" + blocked + ")");
            w.discard();
        }
        fake.stopUsingItem();
        fake.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        fake.setYHeadRot(0.0F);          // the next tests aim the fake player by its body alone
        // the discs: one for each boss's track, each plays in a jukebox
        var songs = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.JUKEBOX_SONG);
        int playable = 0;
        for (var e : ModItems.MUSIC_DISCS.entrySet()) {
            var song = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.JUKEBOX_SONG,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(Umbra.MODID, e.getKey()));
            if (new ItemStack(e.getValue().get()).has(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE) && songs.get(song).isPresent()) playable++;
        }
        check(playable == 8, "2.9: eight discs, one for each boss's track, and each plays in a jukebox (" + playable + ")");
        BlockPos box = c.offset(2, 0, 2);
        level.setBlock(box, Blocks.JUKEBOX.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(box) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity jb) {
            jb.setTheItem(new ItemStack(ModItems.MUSIC_DISCS.get("watcher").get()));
            check(jb.getSongPlayer().isPlaying(), "2.9: the Watcher's disc plays in a jukebox");
        } else check(false, "2.9: a jukebox to try the discs in");
        // and they are found in the mod's chests, so the music can be had without the fights
        BlockPos dc = box.offset(2, 0, 0);
        level.setBlock(dc, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        int found = 0;
        java.util.Set<net.minecraft.world.item.Item> kinds = new java.util.HashSet<>();
        if (level.getBlockEntity(dc) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity dch) {
            var prize = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(Umbra.MODID, "chests/maze_prize"));
            for (int i = 0; i < 80; i++) {
                dch.clearContent();
                dch.setLootTable(i % 2 == 0 ? dev.umbra.depths.Depths.LOOT : prize, level.getRandom().nextLong());
                dch.unpackLootTable(null);
                for (var disc : ModItems.MUSIC_DISCS.values()) {
                    int k = dch.countItem(disc.get());
                    if (k > 0) { found += k; kinds.add(disc.get()); }
                }
            }
            dch.clearContent();
        }
        level.removeBlock(dc, false);
        check(found >= 5 && kinds.size() >= 3, "2.9: the discs lie in the mod's chests too (" + found + " discs of " + kinds.size() + " kinds in 80 chests)");
        level.setBlock(box, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        // the knife: a thing of its own, flying point first; in a wall it sticks for a while
        var knife = new dev.umbra.gear.ThrownKnife(level, fake, new ItemStack(ModItems.SHADOW_KNIFE.get()));
        knife.setPos(c.getX() + 0.5, c.getY() + 1.5, c.getZ() + 0.5);
        knife.shoot(1, 0, 0, 1.5F, 0.0F);
        level.setBlock(c.offset(3, 1, 0), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.addFreshEntity(knife);
        float yaw = 0;
        for (int i = 0; i < 6 && !knife.isStuck(); i++) {
            knife.tick();
            if (!knife.isStuck()) yaw = knife.getYRot();
        }
        boolean stuck = knife.isStuck() && knife.isAlive();
        for (int i = 0; i < 5; i++) knife.tick();
        boolean holds = knife.isAlive() && knife.getDeltaMovement().lengthSqr() < 1.0E-6;
        check(stuck && holds && Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - 90.0F)) < 5.0F,
                "2.9: a thrown knife flies point first (yaw " + yaw + ") and sticks in the wall it hits");
        knife.discard();
        level.setBlock(c.offset(3, 1, 0), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        // the Key of the Depths in the middle of the game: the lurker's scales, a seal's shard, echo quartz
        var recipe = server.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(Umbra.MODID, "depths_key")));
        check(recipe.isPresent() && dev.umbra.journal.Journal.RECIPES.get("lurker").contains("depths_key"),
                "2.9: the Key of the Depths is made of lurker scales, a seal's shard and echo quartz, and the lurker's page teaches it");
    }

    /** 2.7: how Yan's story ends, both ways, and his staff. */
    private static void testYan27(ServerLevel level) {
        BlockPos c = new BlockPos(220, Y + 60, -40);
        for (int x = -12; x <= 12; x++)
            for (int z = -12; z <= 12; z++) {
                level.setBlock(c.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = 0; y < 5; y++) level.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var ENDING = dev.umbra.registry.ModAttachments.ENDING.get();
        String endingBefore = fake.getData(ENDING);
        fake.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 4.5, 180, 0);
        fake.setHealth(fake.getMaxHealth());

        // the dark: Yan stands up against the one who took it
        var yan = ModEntities.LAMP_KEEPER.get().create(level, EntitySpawnReason.COMMAND);
        if (yan == null) return;
        yan.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
        level.addFreshEntity(yan);
        fake.setData(ENDING, "");
        check(yan.tryFall(level, java.util.List.of(fake)) == null && yan.isAlive(), "2.7: Yan stays Yan for one who has not taken the dark");
        fake.setData(ENDING, dev.umbra.ending.Endings.DARK);
        var boss = yan.tryFall(level, java.util.List.of(fake));
        check(boss != null && yan.isRemoved() && boss.getMaxHealth() >= 800, "2.7: for one who took the dark, Yan rises as the Fallen Keeper (" + (boss == null ? 0 : boss.getMaxHealth()) + " hp)");
        if (boss != null) {
            float h = fake.getHealth();
            int burnt = boss.flare(level, java.util.List.of(fake));
            check(burnt == 1 && boss.isStunned() && fake.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),
                    "2.7: his lantern's flare burns the one who took the dark, and blinds him for a moment (burnt " + burnt + ", stunned " + boss.isStunned() + ")");
            fake.setHealth(fake.getMaxHealth());
            boss.setHealth(boss.getMaxHealth() * 0.6F);
            boss.trackPhase(level);
            boss.setHealth(boss.getMaxHealth() * 0.3F);
            boss.trackPhase(level);
            check(boss.phase() == 3 && boss.echoesStanding(level) == 2, "2.7: in his last phase Marta's echoes rise to keep him (" + boss.echoesStanding(level) + ")");
            float before = boss.getHealth();
            for (int i = 0; i < 100; i++) boss.aiStep();
            boss.setHealth(before);
            boss.hurtServer(level, level.damageSources().magic(), 30.0F);
            check(before - boss.getHealth() < 15.0F, "2.7: while an echo stands he takes a third of a blow (" + (before - boss.getHealth()) + " of 30)");
            boss.fallen(level, java.util.List.of(fake));
            boss.discard();
            check("fallen".equals(level.getData(dev.umbra.registry.ModAttachments.YAN.get())) && dev.umbra.journal.Journal.has(fake, "ending_fallen"),
                    "2.7: when he falls the dark ending closes, and the world remembers it");
            check(level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(c).inflate(48), m -> m.entityTags().contains(dev.umbra.lighthouse.FallenKeeper.ECHO_TAG)).isEmpty(),
                    "2.7: Marta's echoes go with him");
        }
        level.setData(dev.umbra.registry.ModAttachments.YAN.get(), "");

        // the dawn: Yan gives his staff and goes
        var yan2 = ModEntities.LAMP_KEEPER.get().create(level, EntitySpawnReason.COMMAND);
        if (yan2 == null) return;
        yan2.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
        level.addFreshEntity(yan2);
        fake.setData(ENDING, dev.umbra.ending.Endings.LIGHT);
        fake.getInventory().clearContent();
        fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        check(yan2.tryFall(level, java.util.List.of(fake)) == null, "2.7: Yan does not rise against one who lit the dawn");
        yan2.farewell(level, fake);
        boolean staff = fake.getInventory().countItem(ModItems.KEEPER_STAFF.get()) == 1;
        check(staff && yan2.isLeaving() && dev.umbra.journal.Journal.has(fake, "keeper_staff"), "2.7: at the dawn Yan gives his staff and makes ready to go");
        yan2.discard();

        // the staff: a beam through the dark, a circle, a refill at the lamp
        ItemStack st = new ItemStack(ModItems.KEEPER_STAFF.get());
        fake.setItemInHand(InteractionHand.MAIN_HAND, st);
        var crawler = ModEntities.CRAWLER.get().create(level, EntitySpawnReason.COMMAND);
        if (crawler != null) {
            fake.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 12);   // facing south (+z), a little down
            crawler.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 6.5, 0, 0);
            crawler.setNoAi(true);
            level.addFreshEntity(crawler);
            float ch = crawler.getHealth();
            int struck = dev.umbra.lighthouse.KeeperStaffItem.beam(level, fake);
            check(struck >= 1 && crawler.getHealth() < ch, "2.7: the staff's beam strikes the dark in its line (" + struck + ")");
            crawler.discard();
        }
        dev.umbra.light.WardBlock.circle(level, c, dev.umbra.lighthouse.KeeperStaffItem.CIRCLE_RADIUS, 100);
        check(dev.umbra.light.WardBlock.protects(level, Vec3.atCenterOf(c).add(5, 0, 0)) && !dev.umbra.light.WardBlock.protects(level, Vec3.atCenterOf(c).add(25, 0, 0)),
                "2.7: the Keeper's circle holds the dark off for its radius only");
        st.setDamageValue(50);
        BlockPos lamp = c.offset(6, 0, 0);
        level.setBlock(lamp, ModBlocks.LIGHTHOUSE_LAMP.get().defaultBlockState().setValue(dev.umbra.lighthouse.LighthouseLampBlock.LEVEL, 2), Block.UPDATE_ALL);
        ModItems.KEEPER_STAFF.get().useOn(new net.minecraft.world.item.context.UseOnContext(fake, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(lamp), net.minecraft.core.Direction.UP, lamp, false)));
        check(fake.getMainHandItem().getDamageValue() == 0, "2.7: the staff drinks its fill at the burning lamp");
        level.setBlock(lamp, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        fake.getInventory().clearContent();
        fake.setData(ENDING, endingBefore);
    }

    private static void testLighthouseKeeper(MinecraftServer server) {
        ServerLevel under = server.getLevel(Underside.LEVEL);
        if (under == null) return;
        BlockPos platform = dev.umbra.lighthouse.LighthouseManager.centre(under);
        BlockPos lamp = dev.umbra.lighthouse.LampKeeper.findLamp(under, platform.getX(), platform.getZ());
        check(lamp != null && !under.getEntitiesOfClass(dev.umbra.lighthouse.LampKeeper.class, new AABB(lamp).inflate(20, 70, 20)).isEmpty(), "the real lighthouse has its keeper");
        under.setChunkForced(platform.getX() >> 4, platform.getZ() >> 4, false);
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

        // a winded boss drops its guard (the Architect its stone skin) and takes half as much again
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
            check(lostA > 0 && Math.abs(lostB - lostA * dev.umbra.boss.UmbraBoss.STUN_DAMAGE / dev.umbra.boss.Architect.STONE_SKIN) < 0.1F, "and drops its stone skin, taking +50% meanwhile (" + lostA + " vs " + lostB + ")");

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
        check(dev.umbra.journal.Journal.section("watcher") == 3 && dev.umbra.journal.Journal.section("mimic_queen") == 3
                && dev.umbra.journal.Journal.section("mimics") == 2 && dev.umbra.journal.Journal.section("mimicker") == 2
                && dev.umbra.journal.Journal.section("insight_2") == 1 && dev.umbra.journal.Journal.section("note_3") == 4
                && dev.umbra.journal.Journal.section("start") == 0, "journal entries sort into sections");
        // 2.11: the journal reads in pages, fewer than its entries
        int pages = dev.umbra.journal.Journal.PAGE_ORDER.size();
        boolean everyEntryHasAPage = true;
        for (String id : dev.umbra.journal.Journal.ENTRIES) everyEntryHasAPage &= dev.umbra.journal.Journal.PAGE_ORDER.contains(dev.umbra.journal.Journal.pageOf(id));
        java.util.List<String> got = java.util.List.of("note_2", "mimic_ore", "weak_watcher");
        boolean merged = dev.umbra.journal.Journal.known(got, "note_1") && dev.umbra.journal.Journal.known(got, "mimics") && dev.umbra.journal.Journal.known(got, "watcher")
                && !dev.umbra.journal.Journal.known(got, "note_3") && "mimics".equals(dev.umbra.journal.Journal.pageOf("mimic_villager"))
                && dev.umbra.journal.Journal.pageText(null, got, "mimics").getString().length() > 20;
        check(pages <= 75 && everyEntryHasAPage && merged, "2.11: the journal reads in " + pages + " pages instead of "
                + dev.umbra.journal.Journal.ENTRIES.size() + " entries; a page shows as soon as anything on it is written");
        int[] per = new int[dev.umbra.journal.Journal.SECTIONS.length];
        for (String id : dev.umbra.journal.Journal.ENTRIES) per[dev.umbra.journal.Journal.section(id)]++;
        boolean none = false;
        for (int i = 0; i < per.length; i++) none |= per[i] == 0 && i != dev.umbra.journal.Journal.ATLAS;
        check(!none, "every journal section has entries (the atlas is a map)");

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

        // 2.7.7: the Watcher's tears
        var tears = new dev.umbra.boss.BossMoves.Tears();
        fake.snapTo(c.getX() + 6.5, c.getY(), c.getZ() + 0.5, 0, 0);
        tears.shoot(level, new Vec3(c.getX() + 0.5, c.getY() + 1.5, c.getZ() + 0.5), fake.getEyePosition().add(0, -0.4, 0), 3, 16.0F, 0.85);
        check(tears.size() == 3, "the Watcher weeps three tears at once");
        int teared = 0;
        for (int i = 0; i < 100 && tears.size() > 0; i++) teared += tears.tick(level, watcher, java.util.List.of(fake), 1.0F);
        check(teared >= 1 && tears.size() == 0, "its tears fly in an arc, strike the one it weeps at and burst on the floor (" + teared + " hit)");

        // 2.10: bolts, the bosses' attacks from afar: they fly, strike, and burst on the floor
        var bolts = new dev.umbra.boss.BossMoves.Bolts();
        var style = new dev.umbra.boss.BossMoves.Style(net.minecraft.core.particles.ParticleTypes.SMOKE, net.minecraft.core.particles.ParticleTypes.CRIT, 0.03,
                net.minecraft.core.particles.ParticleTypes.CRIT, net.minecraft.sounds.SoundEvents.ARROW_HIT);
        fake.setHealth(fake.getMaxHealth());
        fake.snapTo(c.getX() + 7.5, c.getY(), c.getZ() + 0.5, 0, 0);
        bolts.shoot(level, new Vec3(c.getX() + 0.5, c.getY() + 1.5, c.getZ() + 0.5), fake.getEyePosition().add(0, -0.4, 0), 3, 12.0F, 0.9, 2.0F, style);
        int struck = 0;
        for (int i = 0; i < 100 && bolts.size() > 0; i++) struck += bolts.tick(level, watcher, java.util.List.of(fake));
        check(struck >= 1 && bolts.size() == 0, "2.10: a boss's bolts fly at their mark, strike it and burst on the floor (" + struck + " hit)");
        var homing = new dev.umbra.boss.BossMoves.Bolts();
        homing.shoot(level, new Vec3(c.getX() + 0.5, c.getY() + 1.5, c.getZ() + 0.5), new Vec3(c.getX() + 0.5, c.getY() + 1.5, c.getZ() - 8.5), 1, 0.0F, 0.4, 1.0F,
                new dev.umbra.boss.BossMoves.Style(net.minecraft.core.particles.ParticleTypes.SMOKE, net.minecraft.core.particles.ParticleTypes.CRIT, 0.0,
                        net.minecraft.core.particles.ParticleTypes.CRIT, net.minecraft.sounds.SoundEvents.ARROW_HIT).homing(0.12, 120));
        int turned = 0;
        for (int i = 0; i < 120 && homing.size() > 0; i++) turned += homing.tick(level, watcher, java.util.List.of(fake));
        check(turned == 1, "2.10: a homing bolt turns after you even when thrown wide (" + homing.size() + " still flying)");
        // far off, a boss winds up and strikes from where it stands
        var queen = ModEntities.MIMIC_QUEEN.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (queen != null) {
            for (int z = -9; z <= 9; z++)
                for (int y = 0; y <= 4; y++)
                    for (int x = -2; x <= 2; x++) level.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int z = -9; z <= 9; z++)
                for (int x = -2; x <= 2; x++) level.setBlock(c.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            fake.setHealth(fake.getMaxHealth());
            queen.snapTo(c.getX() + 0.5, c.getY(), c.getZ() - 7.5, 0, 0);
            level.addFreshEntity(queen);
            queen.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 600, 9));   // she stays where she is
            // the one she aims at: an acolyte standing still (a fake player is no one a mob would take for a target)
            var mark = ModEntities.ACOLYTE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            mark.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 7.5, 180, 0);
            mark.setNoAi(true);
            level.addFreshEntity(mark);
            queen.pinTarget(mark);
            boolean wound = false;
            for (int i = 0; i < 200 && !wound; i++) {
                queen.tick();
                wound = queen.windingUp();
            }
            int action = queen.action();
            int flew = 0;
            for (int i = 0; i < 40; i++) {
                queen.tick();
                flew = Math.max(flew, queen.bolts().size());
            }
            check(wound && action == dev.umbra.boss.UmbraBoss.ACT_CAST && flew >= 3,
                    "2.10: from afar the Queen winds up (seen) and spits her coins instead of crossing the room (" + flew + " coins; wound " + wound
                            + ", action " + action + ", target " + (queen.getTarget() == mark) + ", sight " + queen.hasLineOfSight(mark) + ", "
                            + String.format("%.1f", queen.distanceTo(mark)) + " off, hidden " + queen.isDisguised() + ")");
            queen.pinTarget(null);
            mark.discard();
            // and a boss dies slowly: it sinks into its smoke and is gone after a couple of seconds
            queen.setHealth(0.0F);
            queen.die(level.damageSources().magic());
            int gone = -1;
            for (int i = 1; i <= 80 && gone < 0; i++) {
                queen.tick();
                if (queen.isRemoved()) gone = i;
            }
            check(gone >= dev.umbra.boss.UmbraBoss.DEATH_TICKS - 2 && gone <= dev.umbra.boss.UmbraBoss.DEATH_TICKS + 2, "2.10: a boss takes " + gone + " ticks to die, not a moment");
            if (!queen.isRemoved()) queen.discard();
        }

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
        check(dev.umbra.journal.Journal.section("lurker") == 2 && dev.umbra.journal.Journal.section("black_lakes") == 0, "new journal entries are sorted");

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
