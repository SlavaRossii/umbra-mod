package dev.umbra.journal;

import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModEntities;
import dev.umbra.registry.ModItems;
import dev.umbra.sanity.Sanity;
import dev.umbra.underside.Underside;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;


/** Watches for first encounters and writes them into the journal. */
public final class JournalEvents {
    private JournalEvents() {}

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.getData(ModAttachments.GOT_JOURNAL)) {
            player.setData(ModAttachments.GOT_JOURNAL, true);
            ItemStack book = new ItemStack(ModItems.JOURNAL.get());
            if (!player.getInventory().add(book)) {
                player.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), book));
            }
        }
        Journal.unlock(player, "start");
        Journal.awardAll(player);
    }

    /** Cheap per-second checks of the player's surroundings. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 7) return;
        if (Underside.is(player.level())) {
            Journal.unlock(player, "underside");
            var biome = player.level().getBiome(player.blockPosition());
            if (biome.is(dev.umbra.underside.UndersideBiomes.CRYSTAL_GROTTOS)) Journal.unlock(player, "crystal_grottos");
            if (biome.is(dev.umbra.underside.UndersideBiomes.BLACK_LAKES)) Journal.unlock(player, "black_lakes");
            if (biome.is(dev.umbra.underside.UndersideBiomes.WHISPERING_FOREST)) Journal.unlock(player, "whispering_forest");
            if (dev.umbra.underside.AshStorm.at(player) > 0.5F) Journal.unlock(player, "ash_storm");
        }
        if (Underside.is(player.level())) {
            int[] lh = dev.umbra.lighthouse.LighthouseFeature.nearest(((net.minecraft.server.level.ServerLevel) player.level()).getSeed(), (int) player.getX(), (int) player.getZ());
            double dx = lh[0] * 16 + 8 - player.getX(), dz = lh[1] * 16 + 8 - player.getZ();
            if (dx * dx + dz * dz < 28 * 28) Journal.unlock(player, "lighthouse");
        }
        spot(player);
        if (Sanity.enabled() && Sanity.get(player) < Sanity.PARANOID) Journal.unlock(player, "sanity");
        checkInsights(player);
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        if (player.level().getBlockState(eye).is(ModBlocks.LIVING_DARKNESS.get())) Journal.unlock(player, "darkness");
        if (player.level().getBlockState(player.blockPosition().below()).is(ModBlocks.MAZE_WALL.get())) Journal.unlock(player, "maze");
        if (player.getInventory().contains(new ItemStack(ModItems.LUMINITE_CRYSTAL.get()))) Journal.unlock(player, "luminite");
    }

    /** Seeing a creature (not just killing it) opens its page: a silhouette and a name. */
    public static void spot(ServerPlayer player) {
        var ids = Bestiary.ids();
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(24.0), x -> ids.containsKey(x.getType()))) {
            String id = ids.get(e.getType());
            if (id.equals("mimicker") || e.isInvisible() || Journal.has(player, id)) continue;
            if (player.hasLineOfSight(e)) Journal.unlock(player, id);
        }
    }

    /** Insights come when sanity falls below their thresholds. */
    public static void checkInsights(ServerPlayer player) {
        if (!Sanity.enabled()) return;
        float s = Sanity.get(player);
        for (int i = 0; i < Journal.INSIGHTS.length; i++) {
            if (s < Journal.INSIGHT_AT[i]) Journal.unlock(player, Journal.INSIGHTS[i]);
        }
    }

    /** Opening the drowned temple's chest. */
    public static void onOpen(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getBlockEntity(event.getPos()) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity c
                && dev.umbra.lake.SunkenTempleFeature.LOOT.equals(c.getLootTable())) {
            Journal.unlock(player, "sunken_temple");
        }
    }

    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        String id = Bestiary.ids().get(dead.getType());
        if (id == null) return;
        // on a server, a boss falls to the whole party: everyone who fought it gets the page and the kill
        java.util.Set<ServerPlayer> credited = new java.util.LinkedHashSet<>();
        credited.add(killer);
        if (dead instanceof dev.umbra.boss.UmbraBoss boss && dead.level() instanceof net.minecraft.server.level.ServerLevel level) credited.addAll(boss.opponents(level));
        for (ServerPlayer player : credited) {
            if (Bestiary.BOSSES.contains(id) && !id.equals("architect") && !id.equals("primordial_shadow")) {
                // the first of the four minibosses leaves Marta's map behind, one for each who has none
                boolean first = true;
                for (String b : new String[]{"watcher", "mimic_queen", "shepherd", "doppelganger"}) if (Bestiary.kills(player, b) > 0) first = false;
                if (first) {
                    if (player == killer) {
                        var level = dead.level();
                        level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, dead.getX(), dead.getY() + 1.0, dead.getZ(), new ItemStack(ModItems.MARTA_MAP.get())));
                    } else {
                        dev.umbra.util.UmbraUtil.give(player, new ItemStack(ModItems.MARTA_MAP.get()));
                    }
                }
            }
            Bestiary.addKill(player, id);
            Journal.unlock(player, id);
        }
    }
}
