package dev.umbra.lighthouse;

import dev.umbra.journal.Journal;
import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModItems;
import dev.umbra.sanity.Sanity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 2.6: Yan's errands. Once his lamp burns he asks three things, one after another; each done gives a blueprint
 * (a journal entry that teaches a recipe nobody else knows):
 * <ol>
 *   <li>read four of Marta's notes          → Marta's lantern: a light that never goes out and steadies the mind;</li>
 *   <li>hold the lighthouse in an onslaught → Yan's spyglass: what you look at through it shows its kind of dark;</li>
 *   <li>go into the citadel                 → the Keeper's bell: its ring makes the things of the dark stagger back.</li>
 * </ol>
 */
public final class YanErrands {
    private YanErrands() {}

    /** id, the journal entry that marks it done, and the blueprint (journal entry) it gives. */
    public record Errand(String id, String needs, String gift) {}

    public static final List<Errand> ALL = List.of(
            new Errand("yan_notes", "note_4", "yan_gift_lantern"),
            new Errand("yan_onslaught", "onslaught_won", "yan_gift_spyglass"),
            new Errand("yan_citadel", "citadel", "yan_gift_bell"));

    public static final List<String> GIFTS = ALL.stream().map(Errand::gift).toList();

    /** The errand Yan is on with this player now (the first not done), or null when all are done. */
    public static Errand current(Player player) {
        for (Errand e : ALL) if (!Journal.has(player, e.gift())) return e;
        return null;
    }

    public static boolean active(Player player, Errand e) {
        return player.getData(ModAttachments.QUESTS_ACTIVE).contains(e.id() + "@0");
    }

    /**
     * Yan was spoken to while his lamp burns. Returns true if he spoke about an errand (so his usual line is skipped).
     */
    public static boolean talk(ServerPlayer player) {
        Errand e = current(player);
        if (e == null) return false;
        if (!active(player, e)) {
            List<String> list = new ArrayList<>(player.getData(ModAttachments.QUESTS_ACTIVE));
            list.add(e.id() + "@0");
            player.setData(ModAttachments.QUESTS_ACTIVE, List.copyOf(list));
            player.sendSystemMessage(Component.translatable("errand.umbra." + e.id() + ".ask").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return true;
        }
        if (!Journal.has(player, e.needs())) {
            player.sendSystemMessage(Component.translatable("errand.umbra." + e.id() + ".wait").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return true;
        }
        List<String> list = new ArrayList<>(player.getData(ModAttachments.QUESTS_ACTIVE));
        list.remove(e.id() + "@0");
        player.setData(ModAttachments.QUESTS_ACTIVE, List.copyOf(list));
        Journal.unlock(player, e.gift());
        player.sendSystemMessage(Component.translatable("errand.umbra." + e.id() + ".thanks").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.9F);
        return true;
    }

    // ------------------------------------------------------------------ the gifts

    /** Marta's lantern: a light in the hand that never goes out (see UmbraUtil#holdsLight) and keeps the mind. */
    public static boolean holdsMartasLantern(Player player) {
        return player.getMainHandItem().is(ModItems.MARTA_LANTERN.get()) || player.getOffhandItem().is(ModItems.MARTA_LANTERN.get());
    }

    /** Once a second: Marta's lantern steadies the mind, Yan's spyglass shows what you look at. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 17 || !(player.level() instanceof ServerLevel level)) return;
        if (holdsMartasLantern(player) && Sanity.enabled()) Sanity.add(player, 0.08F);
        if (player.isScoping() && player.getUseItem().is(ModItems.YAN_SPYGLASS.get())) {
            Vec3 eye = player.getEyePosition(), look = player.getViewVector(1.0F);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(64.0),
                    x -> x != player && dev.umbra.gear.WeaponEvents.dark(x))) {
                Vec3 to = e.getEyePosition().subtract(eye);
                if (to.length() < 64 && look.dot(to.normalize()) > 0.97) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
            }
        }
    }

    public static class MartaLanternItem extends Item {
        public MartaLanternItem(Properties properties) {
            super(properties);
        }
    }

    public static class YanSpyglassItem extends SpyglassItem {
        public YanSpyglassItem(Properties properties) {
            super(properties);
        }
    }

    /** The Keeper's bell: things of the dark within 12 blocks stagger back, slowed and weakened; half a minute to ring again. */
    public static class KeeperBellItem extends Item {
        public static final int COOLDOWN = 600;
        public static final double RADIUS = 12.0;

        public KeeperBellItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level instanceof ServerLevel server) {
                ring(server, player);
                player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN);
            }
            return InteractionResult.SUCCESS;
        }

        public static int ring(ServerLevel level, Player player) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, 1.4F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 1.6F);
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2, player.getZ(), 40, 2.0, 1.0, 2.0, 0.05);
            int n = 0;
            for (Mob m : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RADIUS), x -> x.isAlive() && dev.umbra.gear.WeaponEvents.dark(x))) {
                Vec3 away = m.position().subtract(player.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1.0E-4) m.knockback(1.2, -away.x, -away.z);
                m.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
                m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                if (m instanceof dev.umbra.boss.UmbraBoss) continue;
                m.setTarget(null);
                n++;
            }
            return n;
        }
    }

    public static ItemStack icon() {
        return new ItemStack(ModItems.MARTA_LANTERN.get());
    }
}
