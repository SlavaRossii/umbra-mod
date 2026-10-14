package dev.umbra;

import com.mojang.authlib.GameProfile;
import dev.umbra.citadel.CitadelStatue;
import dev.umbra.decor.DeathDouble;
import dev.umbra.journal.Journal;
import dev.umbra.mimic.MimicManager;
import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModEntities;
import dev.umbra.sanity.Sanity;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;

/**
 * Tests with a real survival player (the same login a client makes, only without a network): statues must thaw and
 * watch, and dying must not leave the player stuck, empty their data or duplicate their things.
 * Driven from {@link SmokeTest}: {@code s} counts ticks since this part began.
 */
final class SmokeLife {
    private static final int Y = 250;
    private static final UUID TESTER = UUID.fromString("5a0e7b42-6c1d-4f3a-9d58-0e3c7a1b2c4d");
    private static final BlockPos HOME = new BlockPos(5, 70, 5);

    private static CitadelStatue statue;
    private static LivingEntity bait;
    private static ServerPlayer player;
    private static Vec3 mark;
    private static BlockPos diedAt;
    private static int carried;

    private SmokeLife() {}

    static void tick(MinecraftServer server, ServerLevel level, int s) {
        // what the player's connection does every tick on a real server
        if (player != null && !player.isRemoved() && player.connection != null) {
            try {
                player.doTick();
            } catch (Throwable t) {
                SmokeTest.check(false, "ticking the survival player threw " + t);
                Umbra.LOGGER.error("UMBRA SMOKETEST: player tick", t);
                player = null;
            }
        }
        try {
            phase(server, level, s);
        } catch (Throwable t) {
            SmokeTest.check(false, "life test failed at step " + s + ": " + t);
            Umbra.LOGGER.error("UMBRA SMOKETEST: life test step {}", s, t);
        }
    }

    private static void phase(MinecraftServer server, ServerLevel level, int s) {
        switch (s) {
            // ---- a flash turns a statue to stone for five seconds, and then it moves again
            case 1 -> {
                for (int x = 150; x <= 210; x++)
                    for (int z = 0; z <= 40; z++)
                        level.setBlock(new BlockPos(x, Y - 1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                statue = statue(level, 160.5, 5.5);
                bait = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
                if (statue == null || bait == null) {
                    SmokeTest.check(false, "statue and bait for the thaw test");
                    return;
                }
                bait.snapTo(176.5, Y, 5.5, 0, 0);
                level.addFreshEntity(bait);
                statue.setTarget(bait);
                statue.onLit(level, null);
                mark = statue.position();
            }
            case 30 -> {
                if (statue == null) return;
                SmokeTest.check(statue.isFrozen() && statue.position().distanceTo(mark) < 0.3, "a flashed statue stays stone");
            }
            case 125 -> {
                if (statue == null) return;
                double moved = statue.position().distanceTo(mark);
                SmokeTest.check(moved > 3.0 && !statue.isFrozen(), "and moves again after five seconds (" + String.format("%.1f", moved) + " blocks)");
                statue.discard();
                bait.discard();
            }

            // ---- a survival player looks at a statue, and away from it
            case 130 -> {
                server.getPlayerList().setViewDistance(3);
                server.getPlayerList().setSimulationDistance(3);
                player = join(server, level);
                player.setGameMode(GameType.SURVIVAL);
                player.setInvulnerable(true);
                player.teleportTo(level, 180.5, Y, 20.5, Set.of(), -90.0F, 0.0F, true);
                look(-90.0F);
                SmokeTest.check(level.players().contains(player) && player.gameMode() == GameType.SURVIVAL, "a survival player joins as on a real login");
                statue = statue(level, 202.5, 20.5);
                if (statue == null) {
                    SmokeTest.check(false, "statue for the watching test");
                    return;
                }
                statue.setTarget(player);
                mark = statue.position();
            }
            case 165 -> {
                if (statue == null || player == null) return;
                SmokeTest.check(statue.isFrozen() && statue.position().distanceTo(mark) < 0.5, "a statue does not move while a survival player looks at it");
            }
            case 166 -> look(90.0F);
            case 195 -> {
                if (statue == null) return;
                double moved = statue.position().distanceTo(mark);
                SmokeTest.check(moved > 5.0 && !statue.isFrozen(), "it runs the moment the player looks away (" + String.format("%.1f", moved) + " blocks)");
            }
            case 196 -> look(-90.0F);
            case 205 -> {
                if (statue == null) return;
                SmokeTest.check(statue.isFrozen(), "looked at again, it freezes on the spot");
                mark = statue.position();
            }
            case 215 -> {
                if (statue == null) return;
                SmokeTest.check(statue.position().distanceTo(mark) < 0.3, "and does not creep closer");
            }
            case 216 -> {
                if (statue == null) return;
                look(90.0F);
                statue.snapTo(202.5, Y, 20.5, 0, 0);
                statue.getNavigation().stop();
                statue.onLit(level, player);
                mark = statue.position();
            }
            case 245 -> {
                if (statue == null) return;
                SmokeTest.check(statue.isFrozen() && statue.position().distanceTo(mark) < 0.3, "a flashed statue stays stone even with nobody watching");
            }
            case 335 -> {
                if (statue == null) return;
                double moved = statue.position().distanceTo(mark);
                SmokeTest.check(moved > 3.0, "and thaws on its own while a player is near (" + String.format("%.1f", moved) + " blocks)");
                statue.discard();
            }

            // ---- death and respawn
            case 340 -> {
                if (player == null) return;
                player.setInvulnerable(false);
                player.setHealth(player.getMaxHealth());
                var inv = player.getInventory();
                inv.add(new ItemStack(Items.DIAMOND, 5));
                inv.add(new ItemStack(Items.IRON_SWORD));
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                Journal.unlock(player, "shadow");
                player.setData(ModAttachments.FREED, true);
                MimicManager.setHome(player, HOME);
                Sanity.set(player, 40);
                carried = count(player);
                SmokeTest.check(carried >= 8, "the player carries things (" + carried + ")");
            }
            case 341 -> {
                if (player == null) return;
                diedAt = player.blockPosition();
                player.hurtServer(level, level.damageSources().magic(), 1000.0F);
                SmokeTest.check(player.isDeadOrDying(), "the player dies");
                SmokeTest.check(count(player) == 0, "death empties the pockets (" + count(player) + " left)");
                int ground = ground(level, diedAt);
                SmokeTest.check(ground == carried, "and everything lies on the ground exactly once (" + ground + " of " + carried + ")");
                SmokeTest.check(level.getEntitiesOfClass(DeathDouble.class, new AABB(diedAt).inflate(6)).size() == 1, "a dark double stands where they fell");
            }
            case 346 -> {
                if (player == null) return;
                ServerPlayer old = player;
                ServerPlayer back = respawn(server, old);
                if (back == null) return;
                player = back;
                SmokeTest.check(back != old && back.isAlive() && !back.isDeadOrDying(), "a dead player can respawn");
                SmokeTest.check(count(back) == 0, "and comes back empty-handed (" + count(back) + " items)");
                int ground = ground(level, diedAt);
                SmokeTest.check(ground == carried, "nothing was duplicated or lost on the way (" + ground + " of " + carried + " on the ground)");
                SmokeTest.check(Journal.has(back, "shadow") && back.getData(ModAttachments.FREED) && HOME.equals(MimicManager.getHome(back)),
                        "the journal, the freed shadow and the home survive death");
                SmokeTest.check(Math.abs(Sanity.get(back) - Sanity.MAX) < 0.01F, "sanity is full again (" + Sanity.get(back) + ")");
            }
            case 350 -> {
                if (player == null) return;
                // a lost bed resets the spawn point in the middle of a respawn; that must not throw
                boolean threw = false;
                try {
                    player.setRespawnPosition(null, false);
                } catch (Throwable t) {
                    threw = true;
                    Umbra.LOGGER.error("UMBRA SMOKETEST: spawn reset", t);
                }
                SmokeTest.check(!threw && HOME.equals(MimicManager.getHome(player)), "resetting the spawn point is harmless and keeps the home");
            }

            // ---- leaving on the death screen and coming back must not bring the things back
            case 355 -> {
                if (player == null) return;
                level.getEntitiesOfClass(ItemEntity.class, new AABB(diedAt).inflate(12)).forEach(Entity::discard);
                player.getInventory().add(new ItemStack(Items.EMERALD, 7));
                player.getInventory().add(new ItemStack(Items.GOLDEN_APPLE, 2));
                carried = count(player);
            }
            case 356 -> {
                if (player == null) return;
                diedAt = player.blockPosition();
                player.hurtServer(level, level.damageSources().magic(), 1000.0F);
                int ground = ground(level, diedAt);
                SmokeTest.check(player.isDeadOrDying() && ground == carried, "a second death drops everything once (" + ground + " of " + carried + ")");
            }
            case 358 -> {
                if (player == null) return;
                server.getPlayerList().remove(player);
                player = join(server, level);
                int ground = ground(level, diedAt);
                SmokeTest.check(count(player) == 0, "quitting on the death screen and coming back brings no items (" + count(player) + ")");
                SmokeTest.check(ground == carried, "and the pile on the ground stays the only copy (" + ground + " of " + carried + ")");
            }
            case 362 -> {
                if (player == null) return;
                ServerPlayer back = respawn(server, player);
                if (back == null) return;
                player = back;
                SmokeTest.check(back.isAlive() && count(back) == 0, "that player can still respawn afterwards");
            }
            case 366 -> {
                if (player != null) server.getPlayerList().remove(player);
                level.getEntitiesOfClass(ItemEntity.class, new AABB(diedAt).inflate(12)).forEach(Entity::discard);
                player = null;
            }
            default -> { }
        }
    }

    /** The login a client makes, over a channel that goes nowhere. */
    private static ServerPlayer join(MinecraftServer server, ServerLevel level) {
        GameProfile profile = new GameProfile(TESTER, "UmbraTester");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer p = new ServerPlayer(server, level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, p, cookie);
        return p;
    }

    /** What a client's "Respawn" button makes the server do. */
    private static ServerPlayer respawn(MinecraftServer server, ServerPlayer dead) {
        try {
            return server.getPlayerList().respawn(dead, false, Entity.RemovalReason.KILLED);
        } catch (Throwable t) {
            SmokeTest.check(false, "respawning threw " + t);
            Umbra.LOGGER.error("UMBRA SMOKETEST: respawn", t);
            return null;
        }
    }

    private static CitadelStatue statue(ServerLevel level, double x, double z) {
        CitadelStatue st = ModEntities.CITADEL_STATUE.get().create(level, EntitySpawnReason.COMMAND);
        if (st == null) return null;
        st.snapTo(x, Y, z, 0, 0);
        st.setPersistenceRequired();
        level.addFreshEntity(st);
        return st;
    }

    private static void look(float yaw) {
        if (player == null) return;
        player.setYRot(yaw);
        player.setXRot(0.0F);
        player.setYHeadRot(yaw);
        player.yRotO = yaw;
        player.xRotO = 0.0F;
        player.yHeadRotO = yaw;
    }

    private static int count(ServerPlayer p) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) n += inv.getItem(i).getCount();
        return n;
    }

    private static int ground(ServerLevel level, BlockPos at) {
        int n = 0;
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(8))) n += e.getItem().getCount();
        return n;
    }
}
