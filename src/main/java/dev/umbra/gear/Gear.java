package dev.umbra.gear;

import dev.umbra.Umbra;
import dev.umbra.journal.Bestiary;
import dev.umbra.mob.Shade;
import dev.umbra.registry.ModComponents;
import dev.umbra.sanity.Sanity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.List;

/**
 * Everything that armour sets, artifacts and the mod's enchantments do for a player. The sets (gloom, luminite, shadow)
 * each come in tiers I-III; a piece is recognised by its item id ({@code luminite_helmet_2} is tier II).
 */
public final class Gear {
    public static final int GLOOM = 0, LUMINITE = 1, SHADOW = 2;
    public static final String[] SETS = {"gloom", "luminite", "shadow"};
    public static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};
    public static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static final String ASH_CLASP = "ash_clasp";
    public static final String ARCHITECT_SEAL = "architect_seal";

    public static final ResourceKey<Enchantment> ECHO = key("echo");
    public static final ResourceKey<Enchantment> SILENCE = key("silence");
    public static final ResourceKey<Enchantment> VOID = key("void");
    public static final ResourceKey<Enchantment> HARVEST = key("harvest");
    public static final ResourceKey<Enchantment> TASTE = key("taste_of_darkness");
    public static final ResourceKey<Enchantment> FLASH = key("flash");
    public static final ResourceKey<Enchantment> ANCHOR = key("anchor");
    public static final ResourceKey<Enchantment> INSTINCT = key("instinct");
    public static final ResourceKey<Enchantment> WHISPER_CURSE = key("whisper_curse");
    public static final List<ResourceKey<Enchantment>> ALL_ENCHANTS = List.of(ECHO, SILENCE, VOID, HARVEST, TASTE, FLASH, ANCHOR, INSTINCT, WHISPER_CURSE);

    private Gear() {}

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Umbra.MODID, name));
    }

    // ------------------------------------------------------------------ armour sets

    /** {set, tier} of an armour piece of the mod, or null. */
    public static int[] piece(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!Umbra.MODID.equals(id.getNamespace())) return null;
        String path = id.getPath();
        int tier = 1;
        if (path.endsWith("_2")) {
            tier = 2;
            path = path.substring(0, path.length() - 2);
        } else if (path.endsWith("_3")) {
            tier = 3;
            path = path.substring(0, path.length() - 2);
        }
        for (int s = 0; s < SETS.length; s++)
            for (String p : PIECES) if (path.equals(SETS[s] + "_" + p)) return new int[]{s, tier};
        return null;
    }

    /** Sum of the tiers of the pieces of one set that are worn (0..12). */
    public static int tierSum(Player player, int set) {
        int n = 0;
        for (EquipmentSlot slot : ARMOR) {
            int[] p = piece(player.getItemBySlot(slot));
            if (p != null && p[0] == set) n += p[1];
        }
        return n;
    }

    public static int pieces(Player player, int set) {
        int n = 0;
        for (EquipmentSlot slot : ARMOR) {
            int[] p = piece(player.getItemBySlot(slot));
            if (p != null && p[0] == set) n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ enchantments

    public static int level(Player player, ResourceKey<Enchantment> key, EquipmentSlot slot) {
        return level(player.level(), player.getItemBySlot(slot), key);
    }

    public static int level(net.minecraft.world.level.Level world, ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack.isEmpty()) return 0;
        var holder = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key);
        return holder.isPresent() ? EnchantmentHelper.getItemEnchantmentLevel(holder.get(), stack) : 0;
    }

    /** Total levels of an enchantment over the four armour slots. */
    public static int armorLevels(Player player, ResourceKey<Enchantment> key) {
        int n = 0;
        for (EquipmentSlot slot : ARMOR) n += level(player, key, slot);
        return n;
    }

    // ------------------------------------------------------------------ artifacts

    /** The artifact pinned to the chestplate, or "". */
    public static String artifact(Player player) {
        String a = player.getItemBySlot(EquipmentSlot.CHEST).get(ModComponents.ARTIFACT.get());
        return a == null ? "" : a;
    }

    public static boolean hasArtifact(Player player, String id) {
        return artifact(player).equals(id);
    }

    // ------------------------------------------------------------------ the numbers

    /** Share of sanity drain that the gear takes away (0..0.85): gloom pieces by tier, and the Void. */
    public static float sanityShield(Player player) {
        float shield = 0;
        for (EquipmentSlot slot : ARMOR) {
            int[] p = piece(player.getItemBySlot(slot));
            if (p != null && p[0] == GLOOM) shield += 0.09F + 0.03F * p[1];
        }
        shield += 0.06F * armorLevels(player, VOID);
        return Math.min(0.85F, shield);
    }

    /** How much quieter the player is (0..0.8): shadow gear, boots of silence, the ash clasp. */
    public static float stealth(Player player) {
        float s = 0.04F * tierSum(player, SHADOW);
        s += 0.2F * level(player, SILENCE, EquipmentSlot.FEET);
        if (hasArtifact(player, ASH_CLASP)) s += 0.3F;
        return Math.min(0.8F, s);
    }

    /** Once a second, for every player. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 3) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        // luminite: shades near you burn, the dark is a little less dark, the full set does not know darkness
        int lum = tierSum(player, LUMINITE);
        if (lum > 0) {
            double r = 2.0 + 0.4 * lum;
            for (Shade shade : level.getEntitiesOfClass(Shade.class, player.getBoundingBox().inflate(r), e -> e.isAlive() && e.distanceTo(player) <= r)) {
                shade.hurtServer(level, level.damageSources().magic(), 2.0F + 0.25F * lum);
            }
            if (Sanity.enabled() && level.getMaxLocalRawBrightness(player.blockPosition()) <= 4) Sanity.add(player, 0.03F * lum);
            if (pieces(player, LUMINITE) == 4) player.removeEffect(MobEffects.DARKNESS);
        }
        // instinct: the things of the dark show through walls
        int instinct = level(player, INSTINCT, EquipmentSlot.HEAD);
        if (instinct > 0) {
            double r = 8.0 + 4.0 * instinct;
            var ids = Bestiary.ids();
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(r), x -> ids.containsKey(x.getType()) && x.isAlive())) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 50, 0, false, false));
            }
        }
        // the whisper's curse: it takes a little of your mind for every cursed thing you carry
        if (Sanity.enabled()) {
            int cursed = 0;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot == EquipmentSlot.BODY || slot == EquipmentSlot.SADDLE) continue;
                if (level(player, WHISPER_CURSE, slot) > 0) cursed++;
            }
            if (cursed > 0) Sanity.add(player, -0.04F * cursed);
        }
    }
}
