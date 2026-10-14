package dev.umbra.mimic;

import dev.umbra.Umbra;
import dev.umbra.UmbraConfig;
import dev.umbra.light.WardBlock;
import dev.umbra.sanity.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Chest mimics. A loot chest in a mineshaft, a dungeon, a village or a quantum maze may be something else.
 * Which chests are mimics is decided by the world seed and position, so nothing needs saving: the first
 * time anyone comes near (or touches it) an unopened chest that rolls "mimic" gets the marker loot table.
 *
 * Open or break it and it bites; hit it first (or light it with the Dispeller) and it gives up its hoard.
 */
public final class MimicChests {
    public static final ResourceKey<LootTable> MARKER = key("chests/mimic");
    public static final ResourceKey<LootTable> HOARD = key("chests/mimic_hoard");
    /** Percent of eligible loot chests that are mimics. */
    public static final int CHANCE = 12;
    private static final int SCAN_RADIUS_CHUNKS = 1;

    private MimicChests() {}

    private static ResourceKey<LootTable> key(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Umbra.MODID, path));
    }

    private static RandomizableContainerBlockEntity container(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof RandomizableContainerBlockEntity c ? c : null;
    }

    public static boolean isMimic(Level level, BlockPos pos) {
        RandomizableContainerBlockEntity c = container(level, pos);
        return c != null && MARKER.equals(c.getLootTable());
    }

    /** Loot chests that have never been opened and come from vanilla structures or our mazes. */
    private static boolean eligible(ResourceKey<LootTable> loot) {
        if (loot == null) return false;
        Identifier id = loot.identifier();
        if (id.getNamespace().equals("minecraft")) return id.getPath().startsWith("chests/");
        return id.getNamespace().equals(Umbra.MODID) && (id.getPath().equals("chests/maze_common") || id.getPath().equals("chests/maze_common_underside") || id.getPath().equals("chests/shadow_village"));
    }

    private static boolean rolls(ServerLevel level, BlockPos pos) {
        long h = level.getSeed() ^ (pos.asLong() * 0x9E3779B97F4A7C15L);
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return Math.floorMod(h, 100) < CHANCE;
    }

    /** Turns the chest at {@code pos} into a mimic if the dice say so. Returns whether it is one now. */
    public static boolean decide(ServerLevel level, BlockPos pos) {
        if (!UmbraConfig.on(UmbraConfig.MIMICS)) return false;
        RandomizableContainerBlockEntity c = container(level, pos);
        if (c == null) return false;
        if (MARKER.equals(c.getLootTable())) return true;
        if (!eligible(c.getLootTable()) || !rolls(level, pos)) return false;
        if (WardBlock.protects(level, Vec3.atCenterOf(pos))) return false;
        c.setLootTable(MARKER);
        c.setChanged();
        return true;
    }

    /** Forces a mimic chest (tests, commands). */
    public static void plant(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        RandomizableContainerBlockEntity c = container(level, pos);
        if (c != null) c.setLootTable(MARKER, level.getRandom().nextLong());
    }

    /** Every couple of seconds: decide nearby chests and let the mimics breathe, so a careful player can hear them. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 40 != 7 || player.isCreative()) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        int pcx = player.blockPosition().getX() >> 4, pcz = player.blockPosition().getZ() >> 4;
        for (int cx = pcx - SCAN_RADIUS_CHUNKS; cx <= pcx + SCAN_RADIUS_CHUNKS; cx++) {
            for (int cz = pcz - SCAN_RADIUS_CHUNKS; cz <= pcz + SCAN_RADIUS_CHUNKS; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values().toArray(new BlockEntity[0])) {
                    if (!(be instanceof RandomizableContainerBlockEntity)) continue;
                    BlockPos pos = be.getBlockPos();
                    if (pos.distSqr(player.blockPosition()) > 24 * 24) continue;
                    if (!decide(level, pos)) continue;
                    double d = Math.sqrt(pos.distSqr(player.blockPosition()));
                    if (d < 10 && player.getRandom().nextInt(3) == 0) {
                        level.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 0.35F, 1.6F);
                    }
                }
            }
        }
    }

    /** Hit first: the mimic gives up. The chest stays, now full of what it swallowed. */
    public static void expose(ServerLevel level, BlockPos pos) {
        RandomizableContainerBlockEntity c = container(level, pos);
        if (c == null || !MARKER.equals(c.getLootTable())) return;
        c.setLootTable(HOARD, level.getRandom().nextLong());
        c.setChanged();
        MimicNotes.note(level, pos, "chest");
        Vec3 v = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.SQUID_INK, v.x, v.y, v.z, 25, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, pos, SoundEvents.SILVERFISH_DEATH, SoundSource.BLOCKS, 1.0F, 0.5F);
        ExperienceOrb.award(level, v, 15);
        level.addFreshEntity(new ItemEntity(level, v.x, v.y + 0.6, v.z, new ItemStack(Items.EMERALD, 2 + level.getRandom().nextInt(3))));
    }

    /** Opened or broken: it bites and is gone. */
    public static void spring(ServerLevel level, BlockPos pos) {
        MimicNotes.note(level, pos, "chest");
        MimicBlock.bite(level, pos);
        RandomizableContainerBlockEntity c = container(level, pos);
        if (c != null) c.setLootTable(null);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        Vec3 v = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.SQUID_INK, v.x, v.y, v.z, 40, 0.5, 0.5, 0.5, 0.1);
        level.playSound(null, pos, SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.0F, 0.6F);
        for (int i = 0; i < 3; i++) EntityTypes.SILVERFISH.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(pos).inflate(6.0))) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 5, 0));
            Sanity.add(p, Sanity.Shock.MIMIC_SPRUNG);
        }
    }

    // --- events ---

    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity().isCreative()) return;
        if (!decide(level, event.getPos())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        spring(level, event.getPos());
    }

    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (!decide(level, event.getPos())) return;
        expose(level, event.getPos());
    }
}
