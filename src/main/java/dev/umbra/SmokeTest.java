package dev.umbra;

import dev.umbra.darkness.DarknessManager;
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
                case 300 -> finish(server);
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
        for (int cx = -2; cx <= 8; cx++) {
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
        check(true, "setup");
    }

    private static void testMaze(ServerLevel level) {
        QuantumMaze.build(level, MAZE, level.getRandom());
        int walls = 0, quantum = 0;
        for (int x = 0; x < QuantumMaze.SIZE; x++)
            for (int z = 0; z < QuantumMaze.SIZE; z++)
                for (int y = 0; y < QuantumMaze.HEIGHT; y++) {
                    BlockState s = level.getBlockState(MAZE.offset(x, y, z));
                    if (s.is(ModBlocks.MAZE_WALL.get())) walls++;
                    if (s.is(ModBlocks.QUANTUM_BLOCK.get())) quantum++;
                }
        check(walls > 300, "maze walls built (" + walls + ")");
        check(quantum > 0, "maze has quantum walls (" + quantum + ")");
        check(level.getBlockState(MAZE.offset(QuantumMaze.SIZE / 2, 1, QuantumMaze.SIZE / 2)).is(Blocks.CHEST), "maze chest in the middle");
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
