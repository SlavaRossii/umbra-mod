package dev.umbra;

import com.mojang.authlib.GameProfile;
import dev.umbra.boss.UmbraBoss;
import dev.umbra.decor.DeathDouble;
import dev.umbra.journal.Bestiary;
import dev.umbra.journal.Journal;
import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModEntities;
import dev.umbra.registry.ModItems;
import dev.umbra.sanity.HallucinationEntity;
import dev.umbra.sanity.PhantomEntity;
import dev.umbra.util.Private;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The server test: two survival players on one dedicated server, logged in the way clients log in. What one mind
 * imagines must reach that player's connection only (checked on the packets themselves); a boss stands up to a
 * party and falls to all of it; deaths and homes keep apart.
 */
final class SmokeMulti {
    private static final int Y = 250;
    private static final int X0 = 600;
    private static final UUID ALICE = UUID.fromString("0b6f9a3e-1d2c-4e5f-8a7b-6c5d4e3f2a10");
    private static final UUID BOB = UUID.fromString("7c1e2d3f-4a5b-4c6d-9e8f-0a1b2c3d4e5f");

    private static ServerPlayer alice, bob;
    private static EmbeddedChannel aliceNet, bobNet;
    private static HallucinationEntity figure;
    private static PhantomEntity phantom;
    private static LivingEntity pig;

    private SmokeMulti() {}

    static void tick(MinecraftServer server, ServerLevel level, int s) {
        for (ServerPlayer p : new ServerPlayer[]{alice, bob}) {
            if (p != null && !p.isRemoved() && p.connection != null) {
                try {
                    p.doTick();
                } catch (Throwable t) {
                    SmokeTest.check(false, "ticking a server player threw " + t);
                    Umbra.LOGGER.error("UMBRA SMOKETEST: multi player tick", t);
                }
            }
        }
        try {
            phase(server, level, s);
        } catch (Throwable t) {
            SmokeTest.check(false, "server test failed at step " + s + ": " + t);
            Umbra.LOGGER.error("UMBRA SMOKETEST: server test step {}", s, t);
        }
    }

    private static void phase(MinecraftServer server, ServerLevel level, int s) {
        switch (s) {
            case 1 -> {
                for (int cx = (X0 >> 4) - 1; cx <= ((X0 + 32) >> 4) + 1; cx++)
                    for (int cz = -1; cz <= 2; cz++) {
                        level.setChunkForced(cx, cz, true);
                        level.getChunk(cx, cz);
                    }
                for (int x = X0; x <= X0 + 32; x++)
                    for (int z = 0; z <= 24; z++)
                        level.setBlock(new BlockPos(x, Y - 1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            case 3 -> {
                Connection ac = new Connection(PacketFlow.SERVERBOUND), bc = new Connection(PacketFlow.SERVERBOUND);
                aliceNet = new EmbeddedChannel(ac);
                bobNet = new EmbeddedChannel(bc);
                alice = join(server, level, ALICE, "UmbraAlice", ac);
                bob = join(server, level, BOB, "UmbraBob", bc);
                for (ServerPlayer p : new ServerPlayer[]{alice, bob}) p.setGameMode(GameType.SURVIVAL);
                alice.teleportTo(level, X0 + 8.5, Y, 8.5, Set.of(), 90.0F, 0.0F, true);
                bob.teleportTo(level, X0 + 12.5, Y, 8.5, Set.of(), -90.0F, 0.0F, true);
                SmokeTest.check(level.players().contains(alice) && level.players().contains(bob), "two players are on the server at once");
            }
            case 30 -> {
                drain(aliceNet);
                drain(bobNet);
                // what Alice imagines, and a pig both of them see
                figure = ModEntities.HALLUCINATION.get().create(level, EntitySpawnReason.EVENT);
                phantom = ModEntities.PHANTOMS.get(0).get().create(level, EntitySpawnReason.EVENT);
                pig = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
                if (figure == null || phantom == null || pig == null) {
                    SmokeTest.check(false, "entities for the private-vision test");
                    return;
                }
                figure.setOwner(alice);
                figure.snapTo(X0 + 10.5, Y, 14.5, 0, 0);
                figure.setNoAi(true);
                phantom.setup(PhantomEntity.Mode.WATCH, alice);
                phantom.snapTo(X0 + 10.5, Y, 2.5, 0, 0);
                phantom.setNoAi(true);
                pig.snapTo(X0 + 10.5, Y, 11.5, 0, 0);
                level.addFreshEntity(figure);
                level.addFreshEntity(phantom);
                level.addFreshEntity(pig);
                SmokeTest.check(figure.broadcastToPlayer(alice) && !figure.broadcastToPlayer(bob), "a hallucination is shown to the one who has it, and to nobody else");
                SmokeTest.check(phantom.broadcastToPlayer(alice) && !phantom.broadcastToPlayer(bob), "so is a phantom");
                SmokeTest.check(pig.broadcastToPlayer(alice) && pig.broadcastToPlayer(bob), "a real pig is shown to both");
                Private.sound(alice, SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.0F, 0.5F);
            }
            case 40 -> {
                List<Object> a = drain(aliceNet), b = drain(bobNet);
                boolean controlA = added(a, pig), controlB = added(b, pig);
                SmokeTest.check(controlA && controlB, "both players' connections are told about the pig (packets seen: " + a.size() + "/" + b.size() + ")");
                if (controlA && controlB) {
                    SmokeTest.check(added(a, figure) && added(a, phantom), "Alice's client is sent her hallucinations");
                    SmokeTest.check(!added(b, figure) && !added(b, phantom), "Bob's client never learns of Alice's hallucinations");
                }
                SmokeTest.check(heard(a), "Alice hears what is whispered to her");
                SmokeTest.check(!heard(b), "Bob does not hear Alice's whisper");
                SmokeTest.check(ModAttachments.ownerOnly(alice, alice) && !ModAttachments.ownerOnly(alice, bob), "a player's sanity and journal are synced to that player only");
                for (Entity e : new Entity[]{figure, phantom, pig}) if (e != null) e.discard();
            }
            // ---- deaths keep apart
            case 45 -> {
                DeathDouble.onDeath(new LivingDeathEvent(alice, level.damageSources().magic()));
                DeathDouble.onDeath(new LivingDeathEvent(bob, level.damageSources().magic()));
                DeathDouble.onDeath(new LivingDeathEvent(alice, level.damageSources().magic()));
                List<DeathDouble> ds = level.getEntitiesOfClass(DeathDouble.class, new AABB(alice.blockPosition()).inflate(32));
                long a = ds.stream().filter(d -> ALICE.equals(d.owner())).count(), b = ds.stream().filter(d -> BOB.equals(d.owner())).count();
                SmokeTest.check(a == 1 && b == 1, "each player keeps one death double of their own (" + a + " / " + b + ")");
                ds.forEach(Entity::discard);
            }
            // ---- a boss against a party
            case 50 -> {
                UmbraBoss boss = ModEntities.WATCHER.get().create(level, EntitySpawnReason.COMMAND);
                if (boss == null) {
                    SmokeTest.check(false, "a boss for the party test");
                    return;
                }
                boss.snapTo(X0 + 10.5, Y, 20.5, 0, 0);
                boss.setNoAi(true);
                level.addFreshEntity(boss);
                double base = boss.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
                boss.setHealth(boss.getMaxHealth() * 0.5F);
                boss.scaleToParty(level);
                SmokeTest.check(Math.abs(boss.getMaxHealth() - base * (1.0 + UmbraBoss.partyBonus(2))) < 0.5, "a boss gains health for a second fighter (" + boss.getMaxHealth() + " of base " + base + ")");
                SmokeTest.check(Math.abs(boss.getHealth() / boss.getMaxHealth() - 0.5F) < 0.02, "and keeps its wounds");
                SmokeTest.check(UmbraBoss.partyBonus(1) == 0.0 && UmbraBoss.partyBonus(9) == UmbraBoss.MAX_EXTRA, "alone it is as before; a crowd does not make it endless");
                boolean bobHadMap = bob.getInventory().countItem(ModItems.MARTA_MAP.get()) > 0;
                boss.die(level.damageSources().playerAttack(alice));
                SmokeTest.check(Bestiary.kills(alice, "watcher") == 1 && Bestiary.kills(bob, "watcher") == 1, "the boss falls to both of them");
                SmokeTest.check(Journal.has(alice, "watcher") && Journal.has(bob, "watcher"), "both of them write it down");
                SmokeTest.check(bobHadMap || bob.getInventory().countItem(ModItems.MARTA_MAP.get()) == 1, "the one who did not strike last gets his own copy of Marta's map");
                boss.discard();
            }
            // ---- a shared home fills up only so far
            case 55 -> {
                BlockPos home = new BlockPos(X0 + 20, Y, 8);
                for (int i = 0; i < 6; i++) level.setBlock(home.offset(i, 0, 3), Blocks.CRAFTING_TABLE.defaultBlockState(), Block.UPDATE_CLIENTS);
                int placed = 0;
                for (int round = 0; round < 4; round++) {
                    int room = dev.umbra.mimic.MimicManager.MAX_AT_HOME - dev.umbra.mimic.MimicManager.mimicsAround(level, home);
                    if (room > 0) placed += dev.umbra.mimic.MimicManager.substitute(level, home, Math.min(2, room), level.getRandom());
                }
                int there = dev.umbra.mimic.MimicManager.mimicsAround(level, home);
                SmokeTest.check(there > 0 && there <= dev.umbra.mimic.MimicManager.MAX_AT_HOME, "a home shared by many hides at most " + dev.umbra.mimic.MimicManager.MAX_AT_HOME + " mimics (" + there + ")");
                for (BlockPos p : BlockPos.betweenClosed(home.offset(-12, -6, -12), home.offset(12, 6, 12))) {
                    if (level.getBlockState(p).getBlock() instanceof dev.umbra.mimic.MimicBlock) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            case 60 -> {
                for (ServerPlayer p : new ServerPlayer[]{alice, bob}) if (p != null) server.getPlayerList().remove(p);
                alice = bob = null;
                drain(aliceNet);
                drain(bobNet);
            }
            default -> { }
        }
    }

    private static ServerPlayer join(MinecraftServer server, ServerLevel level, UUID id, String name, Connection connection) {
        GameProfile profile = new GameProfile(id, name);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer p = new ServerPlayer(server, level, profile, cookie.clientInformation());
        server.getPlayerList().placeNewPlayer(connection, p, cookie);
        return p;
    }

    /** Everything the server has written to this "client" since the last look, bundles opened. */
    private static List<Object> drain(EmbeddedChannel channel) {
        List<Object> out = new ArrayList<>();
        if (channel == null) return out;
        Object o;
        while ((o = channel.readOutbound()) != null) flatten(o, out);
        return out;
    }

    private static void flatten(Object o, List<Object> out) {
        if (o instanceof BundlePacket<?> bundle) {
            for (Packet<?> p : bundle.subPackets()) flatten(p, out);
        } else {
            out.add(o);
        }
    }

    private static boolean added(List<Object> packets, Entity e) {
        if (e == null) return false;
        for (Object o : packets) if (o instanceof ClientboundAddEntityPacket add && add.getId() == e.getId()) return true;
        return false;
    }

    private static boolean heard(List<Object> packets) {
        Object want = SoundEvents.ENDERMAN_STARE;
        if (want instanceof net.minecraft.core.Holder<?> h) want = h.value();
        for (Object o : packets) if (o instanceof ClientboundSoundPacket snd && snd.getSound().value() == want) return true;
        return false;
    }
}
