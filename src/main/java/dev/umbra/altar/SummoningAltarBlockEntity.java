package dev.umbra.altar;

import dev.umbra.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 2.8: the sacrificial altar's memory: the seal on it (slot 0), the offerings in its dish (slots 1..6), and the rite,
 * if one is under way: when it began (game time, so the client can draw it without being told every tick) and who began
 * it. The rite is checked every tick on the server: the place must stay as the seal wants it, the caller must stay near
 * (and, for the Watcher, keep looking at it); when it runs its course the seal and the offerings are spent and the boss
 * steps out of the altar.
 */
public class SummoningAltarBlockEntity extends BlockEntity {
    public static final int SLOTS = 7;
    /** How far the caller may step away before the rite breaks. */
    public static final double REACH = 12.0;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private long riteStart = -1;
    private int rite = -1;
    private ServerPlayer caller;
    /** A display only (the showcase): it shows a rite but never runs one. */
    private boolean exhibit;

    public SummoningAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUMMONING_ALTAR.get(), pos, state);
    }

    public ItemStack seal() {
        return items.get(0);
    }

    public Ritual ritual() {
        return Ritual.of(seal());
    }

    /** The offerings in the dish (copies are not made: do not change them). */
    public List<ItemStack> offerings() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 1; i < SLOTS; i++) if (!items.get(i).isEmpty()) list.add(items.get(i));
        return list;
    }

    public boolean riteRunning() {
        return riteStart >= 0;
    }

    /** Game time the rite began (client: for drawing it), or -1. */
    public long riteStart() {
        return riteStart;
    }

    /** How many of an offering the dish already holds. */
    public int has(Ritual.Offering o) {
        int n = 0;
        for (int i = 1; i < SLOTS; i++) if (o.matches(items.get(i))) n += items.get(i).getCount();
        return n;
    }

    /** What the dish still lacks, as "3 x Spider Eye, 1 x Echo Quartz", or null when nothing. */
    public Component missing() {
        Ritual r = ritual();
        if (r == null) return null;
        Component out = null;
        for (Ritual.Offering o : r.offerings) {
            int need = o.count() - has(o);
            if (need <= 0) continue;
            Component line = Component.translatable("message.umbra.altar.item", need, new ItemStack(o.item().get()).getHoverName());
            out = out == null ? line : Component.empty().append(out).append(", ").append(line);
        }
        return out;
    }

    // ------------------------------------------------------------------ what the player does
    /** Lays a seal on the altar (one of the stack). */
    public boolean placeSeal(ItemStack stack, Player player) {
        if (!seal().isEmpty() || Ritual.of(stack) == null) return false;
        items.set(0, stack.copyWithCount(1));
        if (!player.isCreative()) stack.shrink(1);
        changed();
        return true;
    }

    /** Puts as much of {@code stack} into the dish as the seal still wants; returns how many went in. */
    public int offer(ItemStack stack, Player player) {
        Ritual r = ritual();
        if (r == null || riteRunning()) return 0;
        for (Ritual.Offering o : r.offerings) {
            if (!o.matches(stack)) continue;
            int need = o.count() - has(o);
            if (need <= 0) return 0;
            int put = Math.min(need, stack.getCount());
            for (int i = 1; i < SLOTS && put > 0; i++) {
                ItemStack in = items.get(i);
                if (in.isEmpty()) {
                    items.set(i, stack.copyWithCount(put));
                    if (!player.isCreative()) stack.shrink(put);
                    changed();
                    return put;
                }
                if (ItemStack.isSameItemSameComponents(in, stack)) {
                    in.grow(put);
                    if (!player.isCreative()) stack.shrink(put);
                    changed();
                    return put;
                }
            }
        }
        return 0;
    }

    /** Gives back the seal and the offerings (into the hands, or at the feet). */
    public void takeBack(Player player) {
        if (riteRunning()) return;
        for (int i = SLOTS - 1; i >= 0; i--) {
            ItemStack s = items.get(i);
            if (s.isEmpty()) continue;
            if (!player.getInventory().add(s.copy())) Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), s.copy());
            items.set(i, ItemStack.EMPTY);
        }
        changed();
    }

    /** Tries to begin the rite; tells the player what is still missing if it cannot. */
    public boolean begin(ServerLevel level, ServerPlayer player) {
        Ritual r = ritual();
        if (r == null) {
            player.sendOverlayMessage(Component.translatable("message.umbra.altar.empty"));
            return false;
        }
        if (riteRunning()) return false;
        if (!level.dimension().equals(dev.umbra.underside.Underside.LEVEL)) {
            player.sendOverlayMessage(Component.translatable("message.umbra.altar.not_here"));
            return false;
        }
        Component lack = missing();
        if (lack != null) {
            player.sendOverlayMessage(Component.translatable("message.umbra.altar.wants", lack));
            return false;
        }
        Component place = r.placeProblem(level, worldPosition, player);
        if (place != null) {
            player.sendOverlayMessage(place);
            return false;
        }
        caller = player;
        rite = 0;
        riteStart = level.getGameTime();
        r.onBegin(level, worldPosition, player);
        level.setBlock(worldPosition, getBlockState().setValue(SummoningAltarBlock.GLOW, 2), Block.UPDATE_ALL);
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.5F);
        level.playSound(null, worldPosition, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.6F, 0.6F);
        player.sendOverlayMessage(Component.translatable("message.umbra.altar.begin." + r.id));
        changed();
        return true;
    }

    // ------------------------------------------------------------------ the rite, tick by tick
    public static void serverTick(Level level, BlockPos pos, BlockState state, SummoningAltarBlockEntity altar) {
        if (level instanceof ServerLevel server && altar.riteRunning() && !altar.exhibit) altar.riteTick(server);
    }

    /** For the showcase: shows a rite {@code at} of the way through {@code after} ticks from now, and never finishes it. */
    public void exhibitRite(ServerLevel level, float at, int after) {
        Ritual r = ritual();
        if (r == null) return;
        exhibit = true;
        riteStart = level.getGameTime() + after - (long) (at * r.length);
        level.setBlock(worldPosition, getBlockState().setValue(SummoningAltarBlock.GLOW, 2), Block.UPDATE_ALL);
        changed();
    }

    private void riteTick(ServerLevel level) {
        Ritual r = ritual();
        if (r == null || caller == null || !caller.isAlive() || caller.level() != level
                || caller.distanceToSqr(Vec3.atCenterOf(worldPosition)) > REACH * REACH) {
            fail(level, "message.umbra.altar.fail.left");
            return;
        }
        rite++;
        // the place must stay as it was (looked at every half second)
        if (rite % 10 == 0) {
            Component place = r.placeProblem(level, worldPosition, null);
            if (place != null) {
                fail(level, "message.umbra.altar.fail.place");
                caller.sendOverlayMessage(place);
                return;
            }
        }
        // the Watcher's rite: you must not look away
        if (r == Ritual.WATCHING && !looksAt(caller, r.centre(worldPosition))) {
            if (rite > 20) {
                caller.sendOverlayMessage(Component.translatable("message.umbra.altar.look"));
                level.playSound(null, worldPosition, SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 0.5F, 1.4F);
            }
            rite = 0;
            riteStart = level.getGameTime();
            changed();
            return;
        }
        // the heart of it: faster and faster
        int beat = rite < r.length / 2 ? 20 : rite < r.length * 3 / 4 ? 10 : 5;
        if (rite % beat == 0) level.playSound(null, worldPosition, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 1.0F, 0.7F + 0.5F * rite / r.length);
        Vec3 c = r.centre(worldPosition);
        double a = rite * 0.35;
        double rad = 1.6 * (1.0 - (double) rite / r.length) + 0.2;
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x + Math.cos(a) * rad, c.y + 0.2 + rite * 0.006, c.z + Math.sin(a) * rad, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x - Math.cos(a) * rad, c.y + 0.1, c.z - Math.sin(a) * rad, 2, 0.05, 0.05, 0.05, 0.02);
        if (rite >= r.length) complete(level, r);
    }

    public static boolean looksAt(Player p, Vec3 at) {
        Vec3 to = at.subtract(p.getEyePosition()).normalize();
        return p.getViewVector(1.0F).dot(to) > 0.88;
    }

    private void fail(ServerLevel level, String why) {
        if (caller != null) caller.sendOverlayMessage(Component.translatable(why));
        for (int i = 1; i < SLOTS; i++) items.set(i, ItemStack.EMPTY);       // the offerings burn
        Vec3 c = Vec3.atCenterOf(worldPosition).add(0, 0.6, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 30, 0.3, 0.2, 0.3, 0.02);
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.6F);
        stop(level, 1);
    }

    private void complete(ServerLevel level, Ritual r) {
        ServerPlayer who = caller;
        r.onComplete(level, worldPosition);
        for (int i = 0; i < SLOTS; i++) items.set(i, ItemStack.EMPTY);
        stop(level, 0);
        Mob boss = summon(level, r, who);
        Vec3 c = r.centre(worldPosition);
        level.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y + 0.4, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y + 0.5, c.z, 60, 0.6, 0.6, 0.6, 0.08);
        level.playSound(null, worldPosition, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.4F, 0.8F);
        level.playSound(null, worldPosition, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6F, 1.3F);
        if (boss != null) announce(level, boss, r);
    }

    private void stop(ServerLevel level, int glow) {
        rite = -1;
        riteStart = -1;
        caller = null;
        if (level.getBlockState(worldPosition).getBlock() instanceof SummoningAltarBlock)
            level.setBlock(worldPosition, level.getBlockState(worldPosition).setValue(SummoningAltarBlock.GLOW, seal().isEmpty() ? 0 : glow), Block.UPDATE_ALL);
        changed();
    }

    /** The boss steps out of the altar towards whoever called it. */
    public Mob summon(ServerLevel level, Ritual r, ServerPlayer who) {
        Vec3 toward = who != null ? who.position().subtract(Vec3.atCenterOf(worldPosition)) : new Vec3(0, 0, 1);
        Vec3 dir = toward.horizontalDistanceSqr() < 0.01 ? new Vec3(0, 0, 1) : new Vec3(toward.x, 0, toward.z).normalize();
        BlockPos start = BlockPos.containing(Vec3.atCenterOf(worldPosition).add(dir.scale(2.5)));
        BlockPos spot = dev.umbra.sanity.Hallucinations.findStandable(level, start, 4);
        if (spot == null) spot = worldPosition.above();
        Mob mob = r.boss.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (mob == null) return null;
        float yaw = (float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG);
        mob.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw, 0.0F);
        if (who != null) mob.setTarget(who);
        level.addFreshEntity(mob);
        lastSummoned = mob;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, spot.getX() + 0.5, spot.getY() + 1, spot.getZ() + 0.5, 60, 0.6, 1.2, 0.6, 0.02);
        return mob;
    }

    /** Its name across the screen of everyone near, and a line under it. */
    private void announce(ServerLevel level, Mob boss, Ritual r) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) > 48 * 48) continue;
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(boss.getDisplayName().copy().withStyle(net.minecraft.ChatFormatting.DARK_RED)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(r.subtitle().copy().withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC)));
        }
    }

    /** For tests: the rite as if this player had begun it, in this level, whatever level it is. */
    public void beginForTest(ServerLevel level, ServerPlayer player) {
        caller = player;
        rite = 0;
        riteStart = level.getGameTime();
        changed();
    }

    public void tickForTest(ServerLevel level) {
        if (riteRunning()) riteTick(level);
    }

    public int riteTicks() {
        return rite;
    }

    // ------------------------------------------------------------------ the client's little show
    public static void clientTick(Level level, BlockPos pos, BlockState state, SummoningAltarBlockEntity altar) {
        if (state.getValue(SummoningAltarBlock.GLOW) == 0) return;
        var r = level.getRandom();
        if (altar.riteRunning()) {
            // embers rise from the dish and spiral up round the seal
            double a = (level.getGameTime() % 360) * 0.4 + r.nextDouble();
            level.addParticle(ParticleTypes.WITCH, pos.getX() + 0.5 + Math.cos(a) * 0.45, pos.getY() + 0.95, pos.getZ() + 0.5 + Math.sin(a) * 0.45, 0, 0.04, 0);
            if (r.nextInt(3) == 0)
                level.addParticle(ParticleTypes.SOUL, pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 0.9, pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0.03, 0);
        } else if (r.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 0.95, pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0.01, 0);
        }
    }

    // ------------------------------------------------------------------ memory and the client
    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items.clear();
        ContainerHelper.loadAllItems(in, items);
        riteStart = in.getLongOr("rite_start", -1L);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items, true);
        out.putLong("rite_start", riteStart);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    /** For the self-test: what the last altar taken down let fall, and the boss the last rite called. */
    public static final List<ItemStack> lastDrops = new ArrayList<>();
    public static Mob lastSummoned;

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        lastDrops.clear();
        if (level != null) {
            for (ItemStack s : items) {
                if (s.isEmpty()) continue;
                var drop = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, s.copy());
                drop.setDefaultPickUpDelay();
                if (level.addFreshEntity(drop)) lastDrops.add(s.copy());
            }
            items.clear();
        }
        super.preRemoveSideEffects(pos, state);
    }
}
