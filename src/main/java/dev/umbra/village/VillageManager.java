package dev.umbra.village;

import dev.umbra.registry.ModAttachments;
import dev.umbra.sanity.Hallucinations;
import dev.umbra.sanity.PhantomEntity;
import dev.umbra.underside.Underside;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Shadow villages are built at runtime, the first time a player comes near the chosen chunk of a cell
 * (see {@link ShadowVillageFeature#chosenChunk}). That lets them be far bigger than a world-gen feature may write.
 * The chunk remembers its village (the bell position) in an attachment, so it is built once.
 * Every now and then the bell rings by itself, and something stands under it.
 */
public final class VillageManager {
    public static final int TRIGGER = 56;
    private static final ResourceKey<Biome> ABYSS = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, "abyss"));

    private VillageManager() {}

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 40 != 13 || !(player.level() instanceof ServerLevel level) || !Underside.is(level)) return;
        if (dev.umbra.citadel.Citadel.inPocket(player) || dev.umbra.nightmare.Nightmares.inNightmare(player)) return;
        int pcx = player.blockPosition().getX() >> 4, pcz = player.blockPosition().getZ() >> 4;
        int c = ShadowVillageFeature.CELL;
        for (int gx = Math.floorDiv(pcx, c) - 1; gx <= Math.floorDiv(pcx, c) + 1; gx++)
            for (int gz = Math.floorDiv(pcz, c) - 1; gz <= Math.floorDiv(pcz, c) + 1; gz++) {
                int[] chunk = chosenOf(level.getSeed(), gx, gz);
                double dx = chunk[0] * 16 + 8 - player.getX(), dz = chunk[1] * 16 + 8 - player.getZ();
                if (dx * dx + dz * dz > TRIGGER * TRIGGER || !level.hasChunk(chunk[0], chunk[1])) continue;
                LevelChunk lc = level.getChunk(chunk[0], chunk[1]);
                long bell = lc.getData(ModAttachments.VILLAGE);
                if (bell == Long.MIN_VALUE) {
                    tryBuild(level, lc, chunk, player);
                } else if (bell != Long.MAX_VALUE && player.getRandom().nextInt(150) == 0) {
                    ring(level, BlockPos.of(bell), player);
                }
            }
    }

    public static int[] chosenOf(long seed, int gx, int gz) {
        return ShadowVillageFeature.chosenChunkOf(seed, gx, gz);
    }

    private static void tryBuild(ServerLevel level, LevelChunk lc, int[] chunk, ServerPlayer player) {
        BlockPos column = new BlockPos(chunk[0] * 16 + 8, (int) player.getY(), chunk[1] * 16 + 8);
        // everything within 36 blocks must be loaded
        for (int dx = -36; dx <= 36; dx += 36)
            for (int dz = -36; dz <= 36; dz += 36)
                if (!level.isLoaded(column.offset(dx, 0, dz))) return;
        if (level.getBiome(column).is(ABYSS)) {
            lc.setData(ModAttachments.VILLAGE, Long.MAX_VALUE); // no village in the abyss
            return;
        }
        BlockPos centre = ShadowVillageFeature.groundFloor(level, column, Math.min(120, column.getY() + 20), Math.max(10, column.getY() - 40), column.getY());
        if (centre == null) centre = ShadowVillageFeature.groundFloor(level, column, 120, 10, column.getY());
        if (centre == null) {
            lc.setData(ModAttachments.VILLAGE, Long.MAX_VALUE);
            return;
        }
        RandomSource random = RandomSource.create(level.getSeed() ^ ((long) chunk[0] << 32) ^ chunk[1]);
        var layout = VillageBuilder.Layout.values()[random.nextInt(VillageBuilder.Layout.values().length)];
        BlockPos bell = VillageBuilder.build(level, centre, random, layout);
        lc.setData(ModAttachments.VILLAGE, bell.asLong());
        dev.umbra.journal.Atlas.mark(player, "shadow_village", bell);
        // 2.6: the Bellringer keeps the bell
        BlockPos stand = Hallucinations.findStandable(level, bell.offset(2, 0, 1), 5);
        var ringer = dev.umbra.registry.ModEntities.BELLRINGER.get().create(level, net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
        if (ringer != null && stand != null) {
            ringer.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, random.nextFloat() * 360, 0);
            ringer.setPersistenceRequired();
            level.addFreshEntity(ringer);
        }
    }

    /** /umbra village: builds one right here. */
    public static BlockPos buildHere(ServerLevel level, BlockPos at, VillageBuilder.Layout layout) {
        BlockPos centre = ShadowVillageFeature.findFloor(level, at.above(3), at.getY() + 3, at.getY() - 10);
        return VillageBuilder.build(level, centre == null ? at : centre, level.getRandom(), layout);
    }

    /** The bell rings on its own. Look where it rang. */
    public static void ring(ServerLevel level, BlockPos bell, ServerPlayer player) {
        level.playSound(null, bell, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 0.7F);
        level.playSound(null, bell, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.0F, 0.6F);
        BlockPos ground = Hallucinations.findStandable(level, bell.below(), 6);
        if (ground == null) return;
        var type = dev.umbra.registry.ModEntities.PHANTOMS.get(player.getRandom().nextInt(dev.umbra.registry.ModEntities.PHANTOMS.size())).get();
        PhantomEntity p = type.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (p == null) return;
        p.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 0, 0);
        p.setup(PhantomEntity.Mode.WATCH, player);
        level.addFreshEntity(p);
    }
}
