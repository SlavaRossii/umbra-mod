package dev.umbra.nightmare;

import dev.umbra.UmbraConfig;
import dev.umbra.journal.Journal;
import dev.umbra.quantum.QuantumMaze;
import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModEntities;
import dev.umbra.registry.ModItems;
import dev.umbra.sanity.Sanity;
import dev.umbra.underside.Underside;
import dev.umbra.util.UmbraUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Nightmares. Sleep with a troubled mind (sanity below {@link #BELOW}) and sometimes you do not wake up at home:
 * you wake inside a sealed quantum maze far away in the Underside, with shades in the dark.
 * Find the light upstairs ({@link DreamExitBlock}) to really wake up: sanity comes back and you keep a shard.
 * If time runs out or you are about to die, you wake in a cold sweat instead. A nightmare never kills.
 */
public final class Nightmares {
    public static final float BELOW = 35.0F;
    public static final float CHANCE = 0.35F;
    public static final int DURATION = 20 * 60 * 3;
    public static final float ESCAPE_REWARD = 50.0F;
    public static final float FAIL_PENALTY = 15.0F;
    private static final int ARENA_Y = 150;
    private static final int ARENA_X = 3_000_000;
    private static final int ARENA_Z = 3_000_000;

    private static final Map<UUID, Integer> LEFT = new HashMap<>();

    private Nightmares() {}

    public static boolean inNightmare(ServerPlayer player) {
        return player.getData(ModAttachments.NIGHTMARE) != Long.MIN_VALUE;
    }

    /** Called on a full night's sleep. Returns true if the night turned into a nightmare. */
    public static boolean onSleep(ServerPlayer player) {
        if (!UmbraConfig.on(UmbraConfig.NIGHTMARES) || !Sanity.enabled() || player.isCreative()) return false;
        if (player.level().dimension() != Level.OVERWORLD || inNightmare(player)) return false;
        if (Sanity.get(player) >= BELOW || player.getRandom().nextFloat() >= CHANCE) return false;
        if (dev.umbra.light.WardBlock.protects(player)) return false;
        // after the wake-up code has finished
        player.level().getServer().execute(() -> enter(player));
        return true;
    }

    /** Where this player's nightmare is built: one slot per player, far from anything. */
    public static BlockPos arenaCorner(ServerPlayer player) {
        int slot = Math.floorMod(player.getUUID().hashCode(), 4000);
        return new BlockPos(ARENA_X + slot * 64, ARENA_Y, ARENA_Z);
    }

    public static boolean enter(ServerPlayer player) {
        ServerLevel under = player.level().getServer().getLevel(Underside.LEVEL);
        if (under == null) return false;
        BlockPos corner = arenaCorner(player);
        build(under, corner, player.getRandom());
        player.setData(ModAttachments.NIGHTMARE, player.blockPosition().asLong());
        LEFT.put(player.getUUID(), DURATION);
        int mid = QuantumMaze.SIZE / 2;
        Vec3 at = Vec3.atBottomCenterOf(corner.offset(1, 1, mid));
        player.teleport(new TeleportTransition(under, at, Vec3.ZERO, -90.0F, 0.0F, TeleportTransition.DO_NOTHING));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 12, 0, false, false));
        Sanity.add(player, -5.0F);
        player.sendSystemMessage(Component.translatable("message.umbra.nightmare.start").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        under.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_EMERGE, SoundSource.AMBIENT, 0.6F, 0.6F);
        Journal.unlock(player, "nightmare");
        return true;
    }

    /** A sealed box with a maze inside, its chests replaced by shades and one way out. */
    public static void build(ServerLevel level, BlockPos corner, RandomSource random) {
        int size = QuantumMaze.SIZE, height = QuantumMaze.HEIGHT;
        BlockState wall = ModBlocks.MAZE_WALL.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -2; x <= size + 1; x++)
            for (int z = -2; z <= size + 1; z++)
                for (int y = -9; y <= height + 4; y++) {
                    // solid all round: the maze carves its own corridors out of it, nothing to fall into
                    level.setBlock(corner.offset(x, y, z), wall, Block.UPDATE_CLIENTS);
                }
        for (Entity e : level.getEntities((Entity) null, AABB.encapsulatingFullBlocks(corner.offset(-2, -9, -2), corner.offset(size + 1, height + 4, size + 1)), e -> !(e instanceof ServerPlayer))) {
            e.discard();
        }
        QuantumMaze.build(level, corner, random);
        for (BlockPos p : BlockPos.betweenClosed(corner.offset(-2, -2, -2), corner.offset(size + 1, height + 2, size + 1))) {
            if (level.getBlockState(p).is(Blocks.CHEST)) {
                if (level.getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity c) c.setLootTable(null);
                level.setBlock(p, air, Block.UPDATE_CLIENTS);
            }
        }
        int mid = size / 2;
        level.setBlock(corner.offset(mid, QuantumMaze.floorBase(1) + 1, mid), ModBlocks.DREAM_EXIT.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 0; i < 4; i++) {
            BlockPos cell = corner.offset(1 + 2 * (5 + random.nextInt(6)), 1 + (i % 2) * QuantumMaze.floorBase(1), 1 + 2 * random.nextInt(11));
            ModEntities.SHADE.get().spawn(level, cell, EntitySpawnReason.EVENT);
        }
    }

    public static void tick(ServerPlayer player) {
        if (!inNightmare(player)) return;
        if (!Underside.is(player.level())) {
            // died, left by command, or the server restarted somewhere else: the dream is over
            player.setData(ModAttachments.NIGHTMARE, Long.MIN_VALUE);
            LEFT.remove(player.getUUID());
            return;
        }
        int left = LEFT.getOrDefault(player.getUUID(), DURATION) - 1;
        LEFT.put(player.getUUID(), left);
        if (left == 20 * 60) player.sendOverlayMessage(Component.translatable("message.umbra.nightmare.minute").withStyle(ChatFormatting.DARK_RED));
        if (left <= 0 || player.getHealth() < 4.0F) wake(player, false);
    }

    /** Lethal damage inside a nightmare wakes you up instead. */
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !inNightmare(player)) return;
        if (event.getAmount() >= player.getHealth() - 1.0F) {
            event.setCanceled(true);
            wake(player, false);
        }
    }

    public static void wake(ServerPlayer player, boolean escaped) {
        long packed = player.getData(ModAttachments.NIGHTMARE);
        if (packed == Long.MIN_VALUE) return;
        player.setData(ModAttachments.NIGHTMARE, Long.MIN_VALUE);
        LEFT.remove(player.getUUID());
        ServerLevel overworld = player.level().getServer().overworld();
        Vec3 home = Vec3.atBottomCenterOf(BlockPos.of(packed));
        player.teleport(new TeleportTransition(overworld, home, Vec3.ZERO, player.getYRot(), 0.0F, TeleportTransition.DO_NOTHING));
        player.removeEffect(MobEffects.DARKNESS);
        if (escaped) {
            Sanity.add(player, ESCAPE_REWARD);
            UmbraUtil.give(player, new ItemStack(ModItems.SHADOW_SHARD.get()));
            player.sendSystemMessage(Component.translatable("message.umbra.nightmare.escaped").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
            overworld.playSound(null, home.x, home.y, home.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
        } else {
            Sanity.add(player, -FAIL_PENALTY);
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 20 * 8, 0, false, false));
            player.sendSystemMessage(Component.translatable("message.umbra.nightmare.failed").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
