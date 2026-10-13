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

import java.util.Map;

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
    }

    /** Cheap per-second checks of the player's surroundings. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 7) return;
        if (Underside.is(player.level())) Journal.unlock(player, "underside");
        if (Sanity.enabled() && Sanity.get(player) < Sanity.PARANOID) Journal.unlock(player, "sanity");
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        if (player.level().getBlockState(eye).is(ModBlocks.LIVING_DARKNESS.get())) Journal.unlock(player, "darkness");
        if (player.level().getBlockState(player.blockPosition().below()).is(ModBlocks.MAZE_WALL.get())) Journal.unlock(player, "maze");
        if (player.getInventory().contains(new ItemStack(ModItems.LUMINITE_CRYSTAL.get()))) Journal.unlock(player, "luminite");
    }

    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        Map<EntityType<?>, String> ids = Map.ofEntries(
                Map.entry(ModEntities.CRAWLER.get(), "crawler"),
                Map.entry(ModEntities.BLIND.get(), "blind"),
                Map.entry(ModEntities.LIGHT_EATER.get(), "light_eater"),
                Map.entry(ModEntities.MIMICKER.get(), "mimicker"),
                Map.entry(ModEntities.SHADE.get(), "shade"),
                Map.entry(ModEntities.WATCHER.get(), "watcher"),
                Map.entry(ModEntities.MIMIC_QUEEN.get(), "mimic_queen"),
                Map.entry(ModEntities.SHEPHERD.get(), "shepherd"),
                Map.entry(ModEntities.DOPPELGANGER.get(), "doppelganger"),
                Map.entry(ModEntities.ARCHITECT.get(), "architect"),
                Map.entry(ModEntities.PRIMORDIAL_SHADOW.get(), "primordial_shadow"));
        String id = ids.get(dead.getType());
        if (id != null) Journal.unlock(player, id);
    }
}
