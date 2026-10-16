package dev.umbra.lighthouse;

import dev.umbra.citadel.CitadelGateFeature;
import dev.umbra.journal.Journal;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModComponents;
import dev.umbra.registry.ModEntities;
import dev.umbra.registry.ModItems;
import dev.umbra.sanity.Sanity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The onslaught: sometimes, when a lighthouse burns brighter, the dark comes for it in three waves. While it lasts
 * the lamp does not drive them off: they go for the foot of the tower. Hold them and the lighthouse pays; let
 * {@link #MAX_BREACHES} of them through, or walk away, and the lamp dies down by one level.
 * <p>
 * It only starts with someone at the lighthouse (inside its circle). A Keeper's horn bound to the lamp tells you
 * wherever you are, and carries you back to the tower while the onslaught lasts.
 */
public final class LighthouseRaid {
    public static final String TAG = "umbra_onslaught";
    public static final int WAVES = 3;
    public static final int MAX_BREACHES = 5;
    /** How close (to the foot of the tower) a player must be to count as defending. */
    public static final int DEFEND_RANGE = 64;
    /** Seconds without a defender before the lighthouse is lost. */
    public static final int ABANDON_SECONDS = 60;
    /** Chances for an onslaught when the lamp grows to level 2, 3, 4. */
    private static final float[] ON_GROW = {0.0F, 0.0F, 0.35F, 0.5F, 0.75F};
    /** Per lamp pulse (1 s) with someone inside the circle at night: roughly once in 45 minutes spent there. */
    public static final int NIGHT_ONE_IN = 2700;

    /** Test knobs: ticks of warning before the first wave and of quiet between waves. */
    public static int warmup = 100, pause = 100;

    private static final Map<String, Raid> RAIDS = new HashMap<>();

    private static final class Raid {
        final ServerLevel level;
        final BlockPos lamp, foot;
        final int lampLevel;
        final ServerBossEvent bar;
        final List<UUID> raiders = new ArrayList<>();
        int wave, waveSize, breaches, quietTicks, timer, abandoned;

        Raid(ServerLevel level, BlockPos lamp, BlockPos foot, int lampLevel) {
            this.level = level;
            this.lamp = lamp;
            this.foot = foot;
            this.lampLevel = lampLevel;
            this.bar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("event.umbra.onslaught.coming"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_6);
            this.timer = warmup;
        }
    }

    private LighthouseRaid() {}

    private static String key(Level level, BlockPos lamp) {
        return level.dimension().identifier() + "|" + lamp.asLong();
    }

    public static boolean active(Level level, BlockPos lamp) {
        return RAIDS.containsKey(key(level, lamp));
    }

    public static int count() {
        return RAIDS.size();
    }

    /** For tests: state of the onslaught on this lamp, or null. {wave, raiders alive, breaches}. */
    public static int[] state(Level level, BlockPos lamp) {
        Raid r = RAIDS.get(key(level, lamp));
        if (r == null) return null;
        return new int[]{r.wave, alive(r).size(), r.breaches};
    }

    /** The raiders of this lamp's onslaught that are still standing. */
    public static List<Mob> raiders(Level level, BlockPos lamp) {
        Raid r = RAIDS.get(key(level, lamp));
        return r == null ? List.of() : alive(r);
    }

    // ------------------------------------------------------------------ starting

    /** The lamp just grew to {@code lvl}: maybe the dark noticed. */
    public static void onGrow(ServerLevel level, BlockPos lamp, int lvl, RandomSource random) {
        if (lvl < 2 || lvl >= ON_GROW.length || random.nextFloat() >= ON_GROW[lvl]) return;
        tryStart(level, lamp, lvl);
    }

    /** At night, with someone inside the circle, the dark sometimes tries its luck. */
    public static void onPulse(ServerLevel level, BlockPos lamp, int lvl, RandomSource random) {
        if (lvl < 2 || active(level, lamp) || !dev.umbra.util.UmbraUtil.isNight(level) && !dev.umbra.underside.Underside.is(level)) return;
        if (random.nextInt(NIGHT_ONE_IN) != 0) return;
        tryStart(level, lamp, lvl);
    }

    /** Starts only if a player stands inside the lamp's circle (survival). Returns whether it started. */
    public static boolean tryStart(ServerLevel level, BlockPos lamp, int lvl) {
        if (active(level, lamp) || !dev.umbra.UmbraConfig.on(dev.umbra.UmbraConfig.ONSLAUGHTS)) return false;
        int r = Math.max(24, LighthouseLampBlock.radius(lvl));
        BlockPos foot = footOf(level, lamp);
        boolean someone = false;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.isCreative()) continue;
            if (horizontal(p.position(), foot) <= r) someone = true;
        }
        if (!someone) return false;
        start(level, lamp, lvl);
        return true;
    }

    /** No questions asked (tests and /umbra onslaught). */
    public static void start(ServerLevel level, BlockPos lamp, int lvl) {
        if (active(level, lamp)) return;
        BlockPos foot = footOf(level, lamp);
        Raid raid = new Raid(level, lamp, foot, lvl);
        RAIDS.put(key(level, lamp), raid);
        level.playSound(null, foot, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 4.0F, 0.5F);
        level.playSound(null, lamp, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 3.0F, 0.5F);
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (defending(raid, p)) {
                p.sendSystemMessage(Component.translatable("event.umbra.onslaught.start").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                Journal.unlock(p, "onslaught");
            } else if (hornFor(p, level, lamp) != null) {
                p.sendSystemMessage(Component.translatable("event.umbra.onslaught.horn", lamp.getX(), lamp.getZ()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.2F, 0.7F);
            }
        }
    }

    /** The foot of the tower: the platform the lamp stands {@link LighthouseFeature#LAMP_Y} above, or the ground below it. */
    public static BlockPos footOf(Level level, BlockPos lamp) {
        BlockPos built = lamp.below(LighthouseFeature.LAMP_Y);
        // the platform reaches 8 blocks out; the tower ring only 4, the gallery 6
        if (!level.getBlockState(built.east(7)).isAir() && level.getBlockState(built.east(7).above()).isAir()) return built.above();
        BlockPos p = lamp.below();
        for (int i = 0; i < 128 && p.getY() > level.getMinY() && level.getBlockState(p).isAir(); i++) p = p.below();
        return p.above();
    }

    // ------------------------------------------------------------------ running

    public static void onServerTick(ServerTickEvent.Post event) {
        if (RAIDS.isEmpty()) return;
        Iterator<Map.Entry<String, Raid>> it = RAIDS.entrySet().iterator();
        List<Runnable> after = new ArrayList<>();
        while (it.hasNext()) {
            Raid r = it.next().getValue();
            String done = tick(r);
            if (done != null) {
                it.remove();
                after.add(() -> finish(r, done.equals("won")));
            }
        }
        after.forEach(Runnable::run);
    }

    /** Returns "won" or "lost" when the onslaught is over. */
    private static String tick(Raid r) {
        ServerLevel level = r.level;
        if (!(level.getBlockState(r.lamp).getBlock() instanceof LighthouseLampBlock)) return "lost";
        // who is defending
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (defending(r, p)) r.bar.addPlayer(p);
            else r.bar.removePlayer(p);
        }
        if (level.getGameTime() % 20 == 0) {
            r.abandoned = r.bar.getPlayers().isEmpty() ? r.abandoned + 1 : 0;
            if (r.abandoned >= ABANDON_SECONDS) return "lost";
        }
        if (r.timer > 0) {
            r.timer--;
            if (r.timer == 0) {
                if (r.wave >= WAVES) return "won";
                spawnWave(r);
            }
            return null;
        }
        List<Mob> alive = alive(r);
        // breaches: a raider at the foot of the tower gnaws at its light
        for (Mob m : alive) {
            if (horizontal(m.position(), r.foot) <= 3.5 && Math.abs(m.getY() - r.foot.getY()) < 6) {
                r.breaches++;
                level.sendParticles(ParticleTypes.SQUID_INK, m.getX(), m.getY() + 1, m.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
                level.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.0F, 0.6F);
                m.discard();
                if (r.breaches >= MAX_BREACHES) return "lost";
            }
        }
        // the dark goes for the tower; whoever stands in its way gets fought
        if (level.getGameTime() % 40 == 0) {
            for (Mob m : alive) {
                if (!m.isAlive()) continue;
                boolean busy = m.getTarget() != null && m.getTarget().isAlive() && m.distanceToSqr(m.getTarget()) < 10 * 10;
                if (!busy) {
                    m.setTarget(null);
                    m.getNavigation().moveTo(r.foot.getX() + 0.5, r.foot.getY(), r.foot.getZ() + 0.5, 1.1);
                }
            }
        }
        alive = alive(r);
        r.bar.setProgress(r.waveSize == 0 ? 0.0F : Math.max(0.0F, alive.size() / (float) r.waveSize));
        if (alive.isEmpty()) {
            if (r.wave >= WAVES) return "won";
            r.timer = pause;
            r.bar.setName(Component.translatable("event.umbra.onslaught.pause", r.wave, WAVES));
            r.bar.setProgress(0.0F);
        }
        return null;
    }

    private static List<Mob> alive(Raid r) {
        List<Mob> out = new ArrayList<>();
        for (UUID id : r.raiders) if (r.level.getEntity(id) instanceof Mob m && m.isAlive()) out.add(m);
        return out;
    }

    private static final List<Supplier<? extends EntityType<? extends Mob>>> FIRST = List.of(ModEntities.CRAWLER, ModEntities.SHADE, ModEntities.BLIND);
    private static final List<Supplier<? extends EntityType<? extends Mob>>> LATER = List.of(ModEntities.CRAWLER, ModEntities.SHADE, ModEntities.BLIND, ModEntities.LIGHT_EATER);

    private static void spawnWave(Raid r) {
        r.wave++;
        r.raiders.clear();
        ServerLevel level = r.level;
        RandomSource random = level.getRandom();
        int size = 3 + 2 * r.wave + r.lampLevel;
        int spawned = 0;
        for (int i = 0; i < size; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double d = 26 + random.nextInt(8);
            int x = (int) Math.floor(r.foot.getX() + Math.cos(a) * d), z = (int) Math.floor(r.foot.getZ() + Math.sin(a) * d);
            BlockPos at = ground(level, x, z, r.foot.getY());
            if (at == null) continue;
            var type = (r.wave == 1 ? FIRST : LATER).get(random.nextInt(r.wave == 1 ? FIRST.size() : LATER.size())).get();
            Mob m = type.create(level, EntitySpawnReason.EVENT);
            if (m == null) continue;
            m.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            m.addTag(TAG);
            m.setPersistenceRequired();
            level.addFreshEntity(m);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY() + 1, m.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
            m.getNavigation().moveTo(r.foot.getX() + 0.5, r.foot.getY(), r.foot.getZ() + 0.5, 1.1);
            r.raiders.add(m.getUUID());
            spawned++;
        }
        r.waveSize = spawned;
        r.bar.setName(Component.translatable("event.umbra.onslaught.wave", r.wave, WAVES));
        r.bar.setProgress(1.0F);
        level.playSound(null, r.foot, SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.6F + 0.1F * r.wave);
        // nobody came? The wave is over at once (an empty ring of void, say)
        if (spawned == 0) r.timer = 1;
    }

    /** Somewhere to stand near {@code y}: first try the column around the tower's height, then the world's ground. */
    private static BlockPos ground(ServerLevel level, int x, int z, int y) {
        if (!level.isLoaded(new BlockPos(x, y, z))) return null;
        for (int dy = 8; dy >= -12; dy--) {
            BlockPos p = new BlockPos(x, y + dy, z);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && !level.getBlockState(p.below()).isAir()
                    && level.getBlockState(p.below()).getFluidState().isEmpty()) return p;
        }
        if (dev.umbra.underside.Underside.is(level)) {
            BlockPos g = CitadelGateFeature.findSolidGround(level, x, z);
            if (g != null && Math.abs(g.getY() - y) < 24) return g;
        }
        return null;
    }

    private static void finish(Raid r, boolean won) {
        ServerLevel level = r.level;
        for (Mob m : alive(r)) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY() + 1, m.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
            m.discard();
        }
        List<ServerPlayer> defenders = new ArrayList<>(r.bar.getPlayers());
        r.bar.removeAllPlayers();
        if (won) {
            level.playSound(null, r.foot, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 3.0F, 1.2F);
            level.sendParticles(ParticleTypes.END_ROD, r.lamp.getX() + 0.5, r.lamp.getY() + 1.0, r.lamp.getZ() + 0.5, 80, 0.6, 3.0, 0.6, 0.1);
            drop(level, r.foot, new ItemStack(ModItems.LUMINITE_CRYSTAL.get(), 4 + 2 * r.lampLevel));
            drop(level, r.foot, new ItemStack(ModItems.SHADOW_SHARD.get(), 2 + r.lampLevel));
            drop(level, r.foot, new ItemStack(ModItems.ECHO_QUARTZ.get(), 1 + level.getRandom().nextInt(2)));
            if (level.getRandom().nextFloat() < 0.35F) drop(level, r.foot, new ItemStack(ModItems.KEEPER_PAGE.get()));
            for (ServerPlayer p : defenders) {
                p.sendSystemMessage(Component.translatable("event.umbra.onslaught.won").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                Sanity.add(p, 15.0F);
                Journal.unlock(p, "onslaught_won");
            }
        } else {
            BlockState state = level.getBlockState(r.lamp);
            if (state.getBlock() instanceof LighthouseLampBlock) {
                int lvl = state.getValue(LighthouseLampBlock.LEVEL);
                level.setBlock(r.lamp, state.setValue(LighthouseLampBlock.LEVEL, Math.max(0, lvl - 1)).setValue(LighthouseLampBlock.FILL, 0), Block.UPDATE_ALL);
                if (lvl - 1 > 0) level.scheduleTick(r.lamp, state.getBlock(), 20);
            }
            level.playSound(null, r.lamp, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 3.0F, 0.5F);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, r.lamp.getX() + 0.5, r.lamp.getY() + 1.0, r.lamp.getZ() + 0.5, 60, 0.6, 1.0, 0.6, 0.03);
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                if (defenders.contains(p) || hornFor(p, level, r.lamp) != null) {
                    p.sendSystemMessage(Component.translatable("event.umbra.onslaught.lost").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                }
            }
        }
    }

    private static void drop(ServerLevel level, BlockPos at, ItemStack stack) {
        ItemEntity e = new ItemEntity(level, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 2.5, stack);
        e.setDefaultPickUpDelay();
        level.addFreshEntity(e);
    }

    private static boolean defending(Raid r, ServerPlayer p) {
        return p.level() == r.level && !p.isSpectator() && horizontal(p.position(), r.foot) <= DEFEND_RANGE;
    }

    private static double horizontal(Vec3 a, BlockPos b) {
        double dx = a.x - (b.getX() + 0.5), dz = a.z - (b.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static void clearAll() {
        for (Raid r : RAIDS.values()) r.bar.removeAllPlayers();
        RAIDS.clear();
    }

    // ------------------------------------------------------------------ the Keeper's horn

    /** "dimension|x|y|z" of the lamp a horn is bound to. */
    public static String bindingOf(Level level, BlockPos lamp) {
        return level.dimension().identifier() + "|" + lamp.getX() + "|" + lamp.getY() + "|" + lamp.getZ();
    }

    /** A horn in this player's inventory bound to that lamp, or null. */
    public static ItemStack hornFor(ServerPlayer p, Level level, BlockPos lamp) {
        String want = bindingOf(level, lamp);
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.KEEPER_HORN.get()) && want.equals(s.get(ModComponents.BOUND_LAMP.get()))) return s;
        }
        return null;
    }

    /** Blowing a bound horn: during an onslaught it carries you to the foot of the tower. */
    public static boolean answer(ServerPlayer player, ItemStack horn) {
        String bound = horn.get(ModComponents.BOUND_LAMP.get());
        if (bound == null) {
            player.sendOverlayMessage(Component.translatable("item.umbra.keeper_horn.unbound"));
            return false;
        }
        String[] parts = bound.split("\\|");
        if (parts.length != 4) return false;
        MinecraftServer server = player.level().getServer();
        ServerLevel target = null;
        for (ServerLevel l : server.getAllLevels()) if (l.dimension().identifier().toString().equals(parts[0])) target = l;
        if (target == null) return false;
        BlockPos lamp;
        try {
            lamp = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return false;
        }
        if (!active(target, lamp)) {
            player.sendOverlayMessage(Component.translatable("item.umbra.keeper_horn.quiet", lamp.getX(), lamp.getZ()));
            return false;
        }
        Raid r = RAIDS.get(key(target, lamp));
        // step out at the door of the keeper's house side of the platform
        Vec3 at = Vec3.atBottomCenterOf(r.foot.south(6));
        player.teleport(new TeleportTransition(target, at, Vec3.ZERO, 180.0F, 0.0F, TeleportTransition.PLAY_PORTAL_SOUND));
        target.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 30, 0.3, 0.8, 0.3, 0.05);
        player.sendOverlayMessage(Component.translatable("item.umbra.keeper_horn.arrived").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }
}
