package dev.umbra.ending;

import dev.umbra.boss.UmbraBoss;
import dev.umbra.gear.WeaponEvents;
import dev.umbra.journal.Journal;
import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * 2.6: New Game+. After an ending, Yan offers to turn the circle (speak to him crouching). The world's cycle grows by
 * one, your ending is undone so it can be chosen again, and the dark grows stronger:
 * <ul>
 *   <li>creatures of the dark: +25% health and damage per cycle;</li>
 *   <li>bosses: +50% health and +25% damage per cycle, and they call two shades every fifteen seconds;</li>
 *   <li>bosses leave more: echo quartz and luminite by the cycle, and a chance of a second relic.</li>
 * </ul>
 */
public final class Cycle {
    private Cycle() {}

    private static final Identifier HEALTH = Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, "cycle_health");
    private static final Identifier DAMAGE = Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, "cycle_damage");
    private static final String TAG = "umbra_cycle";

    public static int get(MinecraftServer server) {
        ServerLevel o = server.overworld();
        return o == null ? 0 : o.getData(ModAttachments.CYCLE);
    }

    public static int get(net.minecraft.world.level.Level level) {
        return level.getServer() == null ? 0 : get(level.getServer());
    }

    /** Turns the circle for this player's world. Returns the new cycle. */
    public static int begin(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        int next = get(server) + 1;
        server.overworld().setData(ModAttachments.CYCLE, next);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.setData(ModAttachments.ENDING, "");
            Journal.unlock(p, "new_cycle");
            p.sendSystemMessage(Component.translatable("message.umbra.cycle", next).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.AMBIENT, 0.6F, 0.5F);
        }
        return next;
    }

    /** Strength of a creature of the dark in cycle n: the multiplier on top of its own. */
    public static double health(boolean boss, int n) {
        return boss ? 0.5 * n : 0.25 * n;
    }

    public static double damage(int n) {
        return 0.25 * n;
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof LivingEntity e) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (e.entityTags().contains(TAG) || !WeaponEvents.dark(e)) return;
        int n = get(level);
        if (n <= 0) return;
        apply(e, n);
    }

    /** Makes a creature as strong as cycle n wants (once). */
    public static void apply(LivingEntity e, int n) {
        boolean boss = e instanceof UmbraBoss;
        var hp = e.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.addOrReplacePermanentModifier(new AttributeModifier(HEALTH, health(boss, n), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        var dmg = e.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.addOrReplacePermanentModifier(new AttributeModifier(DAMAGE, damage(n), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        e.setHealth(e.getMaxHealth());
        e.addTag(TAG);
    }

    /** In the cycles after the first, a boss calls shades every fifteen seconds (from UmbraBoss). */
    public static void bossTick(ServerLevel level, UmbraBoss boss) {
        if (boss.tickCount % 300 != 150 || get(level) <= 0) return;
        dev.umbra.boss.BossMoves.shades(level, boss, boss.getTarget(), 2);
    }

    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof UmbraBoss boss) || !(boss.level() instanceof ServerLevel level)) return;
        int n = get(level);
        if (n <= 0) return;
        var drops = event.getDrops();
        drops.add(new ItemEntity(level, boss.getX(), boss.getY() + 0.5, boss.getZ(), new ItemStack(ModItems.ECHO_QUARTZ.get(), 4 * n)));
        drops.add(new ItemEntity(level, boss.getX(), boss.getY() + 0.5, boss.getZ(), new ItemStack(ModItems.LUMINITE_CRYSTAL.get(), 2 * n)));
        String id = dev.umbra.journal.Bestiary.ids().get(boss.getType());
        var trophy = id == null ? null : ModItems.TROPHIES.get(id);
        if (trophy != null && level.getRandom().nextFloat() < Math.min(1.0F, 0.25F * n)) {
            drops.add(new ItemEntity(level, boss.getX(), boss.getY() + 0.5, boss.getZ(), new ItemStack(trophy.get())));
        }
    }
}
