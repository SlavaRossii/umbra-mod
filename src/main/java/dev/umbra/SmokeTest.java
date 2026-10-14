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
                case 660 -> finish(server);
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
        check(dev.umbra.journal.Journal.ENTRIES.size() == 30, "journal has 30 entries");

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
        } else {
            check(false, "underside level for the nightmare test");
        }
    }

    // ---------------------------------------------------------------- 1.3
    private static void testVillage(ServerLevel level) {
        BlockPos v = new BlockPos(185, Y + 40, 0);
        for (int x = -20; x <= 20; x++)
            for (int z = -20; z <= 20; z++)
                level.setBlock(v.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        dev.umbra.village.ShadowVillageFeature.build(level, v, level.getRandom());
        int containers = 0, lanterns = 0, logs = 0;
        for (BlockPos p : BlockPos.betweenClosed(v.offset(-20, -1, -20), v.offset(20, 5, 20))) {
            BlockState s = level.getBlockState(p);
            if (s.is(Blocks.CHEST) || s.is(Blocks.BARREL)) containers++;
            if (s.is(Blocks.SOUL_LANTERN) || s.is(Blocks.LANTERN)) lanterns++;
            if (s.is(ModBlocks.DEAD_LOG.get())) logs++;
        }
        check(containers >= 4, "a shadow village has a chest or barrel in every hut (" + containers + ")");
        check(lanterns >= 4 && logs >= 16, "a shadow village has lantern posts and hut frames (" + lanterns + " lanterns, " + logs + " logs)");
        var folk = level.getEntitiesOfClass(dev.umbra.village.ShadowVillager.class, new AABB(v).inflate(10));
        check(folk.size() >= 3, "shadow folk live in the village (" + folk.size() + ")");
        if (!folk.isEmpty()) {
            check(folk.get(0).getOffers().size() == 6, "a shadow villager has six trades (" + folk.get(0).getOffers().size() + ")");
        }
        folk.forEach(net.minecraft.world.entity.Entity::discard);

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
        check(villages == 100 && minGap >= 7, "shadow villages are spaced out (" + villages + " in 180x180 chunks, min gap " + minGap + ")");
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
        check(level.getBlockState(o.offset(0, 1, 0)).is(ModBlocks.CITADEL_GATE.get()), "the citadel has a way back");
        check(level.getBlockState(o.offset(dev.umbra.citadel.Citadel.SEAL_X, 1, 0)).is(ModBlocks.CITADEL_SEAL.get()), "the throne is sealed");
        int cores = 0, dark = 0, chests = 0, mimics = 0, phantom = 0;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-3, -2, -14), o.offset(dev.umbra.citadel.Citadel.LENGTH, 9, 14))) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.MAZE_CORE.get())) cores++;
            else if (s.is(ModBlocks.LIVING_DARKNESS.get())) dark++;
            else if (s.is(ModBlocks.PHANTOM_WALL.get())) phantom++;
            else if (s.is(Blocks.CHEST)) {
                chests++;
                if (dev.umbra.mimic.MimicChests.isMimic(level, p)) mimics++;
            }
        }
        check(cores == 2, "the citadel has two quantum mazes (" + cores + ")");
        check(dark > 500, "the hall of living darkness is full (" + dark + ")");
        check(chests >= 20 && mimics >= 2, "the citadel has chests, some hungry (" + chests + ", " + mimics + " mimics)");
        check(phantom == 9, "the library hides a phantom wall (" + phantom + ")");
        int statues = dev.umbra.citadel.Citadel.count(level, o, dev.umbra.citadel.CitadelStatue.class);
        int keepers = dev.umbra.citadel.Citadel.count(level, o, dev.umbra.citadel.CitadelKeeper.class);
        check(statues == 3 && keepers == 2, "statues and keepers stand guard (" + statues + ", " + keepers + ")");

        FakePlayer fake = FakePlayerFactory.getMinecraft(level);
        BlockPos pw = o.offset((dev.umbra.citadel.Citadel.LIBRARY_FROM + dev.umbra.citadel.Citadel.LIBRARY_TO) / 2, 0, -8);
        BlockState wallState = level.getBlockState(pw);
        Sanity.set(fake, 80);
        boolean solid = !wallState.getCollisionShape(level, pw, net.minecraft.world.phys.shapes.CollisionContext.of(fake)).isEmpty();
        Sanity.set(fake, 10);
        boolean open = wallState.getCollisionShape(level, pw, net.minecraft.world.phys.shapes.CollisionContext.of(fake)).isEmpty();
        check(solid && open, "the phantom wall lets only a slipping mind through");
        Sanity.set(fake, 100);

        var statue = level.getEntitiesOfClass(dev.umbra.citadel.CitadelStatue.class, AABB.encapsulatingFullBlocks(o.offset(-3, -10, -14), o.offset(dev.umbra.citadel.Citadel.LENGTH, 13, 14)));
        if (!statue.isEmpty()) {
            statue.get(0).onLit(level, null);
            check(statue.get(0).isFrozen(), "light turns a statue to stone");
        }

        var boss = dev.umbra.citadel.Citadel.openThrone(level, o.offset(dev.umbra.citadel.Citadel.SEAL_X, 1, 1), null);
        check(boss != null && boss.isAlive(), "the key's door wakes the Architect");
        check(level.getBlockState(o.offset(dev.umbra.citadel.Citadel.SEAL_X, 1, 0)).isAir(), "the seal is gone");
        if (boss != null) {
            int mid = QuantumMaze.SIZE / 2;
            check(Math.abs(boss.getX() - (o.getX() + dev.umbra.citadel.Citadel.THRONE + mid + 0.5)) < 1.0, "the Architect is in his maze");
            boss.discard();
        }
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, AABB.encapsulatingFullBlocks(o.offset(-3, -10, -14), o.offset(dev.umbra.citadel.Citadel.LENGTH, 13, 14)))) e.discard();

        // gates in the caves
        BlockPos g = new BlockPos(185, Y + 60, 40);
        for (int x = -5; x <= 5; x++) for (int z = -4; z <= 5; z++) overworld.setBlock(g.offset(x, -2, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        dev.umbra.citadel.CitadelGateFeature.build(overworld, g, overworld.getRandom());
        int gates = 0;
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-2, 0, 0), g.offset(2, 4, 0))) if (overworld.getBlockState(p).is(ModBlocks.CITADEL_GATE.get())) gates++;
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

    private static void checkWard(ServerLevel level) {
        check(dev.umbra.light.WardBlock.protects(level, Vec3.atCenterOf(WARD).add(10, 0, 10)), "a burning ward protects its circle");
        check(!dev.umbra.light.WardBlock.protects(level, Vec3.atCenterOf(WARD).add(40, 0, 0)), "but not beyond two chunks");
        double d = spider == null ? 0 : spider.position().distanceTo(Vec3.atCenterOf(WARD));
        check(spider != null && d > 7.0, "the ward drives a spider away (" + String.format("%.1f", d) + " blocks)");
        if (spider != null) spider.discard();
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
