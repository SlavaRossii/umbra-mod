package dev.umbra.registry;

import dev.umbra.Umbra;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/** Gloom steel: forged from ore found only in the Underside. Between diamond and netherite. */
public final class ModMaterials {
    public static final TagKey<Item> GLOOM_REPAIR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Umbra.MODID, "gloom_tool_materials"));

    public static final ToolMaterial GLOOM_TOOLS = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 9.0F, 3.5F, 18, GLOOM_REPAIR);

    public static final ResourceKey<EquipmentAsset> GLOOM_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Umbra.MODID, "gloom"));

    public static final ArmorMaterial GLOOM_ARMOR = new ArmorMaterial(
            15,
            Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 6, ArmorType.LEGGINGS, 5, ArmorType.BOOTS, 2, ArmorType.BODY, 8),
            16,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            0.0F,
            0.0F,
            GLOOM_REPAIR,
            GLOOM_ASSET);

    // --- 2.0: three sets of three tiers. Gloom tier I is the original GLOOM_ARMOR; the rest follow this table.
    public static final TagKey<Item> LUMINITE_REPAIR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Umbra.MODID, "luminite_armor_materials"));
    public static final TagKey<Item> SHADOW_REPAIR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Umbra.MODID, "shadow_armor_materials"));
    public static final ResourceKey<EquipmentAsset> LUMINITE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Umbra.MODID, "luminite"));
    public static final ResourceKey<EquipmentAsset> SHADOW_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Umbra.MODID, "shadow"));

    private static ArmorMaterial mat(int durability, int helmet, int chest, int legs, int boots, int enchantability, float toughness, float knockback,
                                     TagKey<Item> repair, ResourceKey<EquipmentAsset> asset) {
        return new ArmorMaterial(durability, Map.of(ArmorType.HELMET, helmet, ArmorType.CHESTPLATE, chest, ArmorType.LEGGINGS, legs, ArmorType.BOOTS, boots,
                ArmorType.BODY, chest + 2), enchantability, SoundEvents.ARMOR_EQUIP_NETHERITE, toughness, knockback, repair, asset);
    }

    /**
     * 2.4: one ladder from the easiest to the hardest to get, each step a little better than the one before:
     * gloom I, shadow I, luminite I (about iron), then the same at II (about diamond), then at III (past netherite).
     * The sets differ in what they do besides (Gear): gloom steadies the mind, shadow hides, luminite shines.
     * @param set 0 gloom, 1 luminite, 2 shadow; @param tier 1..3
     */
    public static ArmorMaterial armor(int set, int tier) {
        return switch (set) {
            case 0 -> switch (tier) {
                case 1 -> GLOOM_ARMOR;
                case 2 -> mat(28, 3, 7, 6, 3, 16, 1.5F, 0.0F, GLOOM_REPAIR, GLOOM_ASSET);
                default -> mat(38, 3, 8, 7, 3, 16, 3.0F, 0.10F, GLOOM_REPAIR, GLOOM_ASSET);
            };
            case 1 -> switch (tier) {
                case 1 -> mat(20, 2, 7, 5, 2, 22, 1.0F, 0.0F, LUMINITE_REPAIR, LUMINITE_ASSET);
                case 2 -> mat(34, 3, 8, 6, 3, 22, 2.5F, 0.0F, LUMINITE_REPAIR, LUMINITE_ASSET);
                default -> mat(44, 4, 9, 7, 4, 22, 4.0F, 0.12F, LUMINITE_REPAIR, LUMINITE_ASSET);
            };
            default -> switch (tier) {
                case 1 -> mat(18, 2, 6, 5, 2, 24, 0.5F, 0.0F, SHADOW_REPAIR, SHADOW_ASSET);
                case 2 -> mat(31, 3, 8, 6, 3, 24, 2.0F, 0.0F, SHADOW_REPAIR, SHADOW_ASSET);
                default -> mat(40, 4, 8, 7, 4, 24, 3.5F, 0.10F, SHADOW_REPAIR, SHADOW_ASSET);
            };
        };
    }

    /** Defence + toughness of a full set: what the ladder is measured by (tests). */
    public static float score(int set, int tier) {
        ArmorMaterial m = armor(set, tier);
        int sum = 0;
        for (ArmorType t : new ArmorType[]{ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS}) sum += m.defense().get(t);
        return sum + m.toughness() * 4;
    }

    private ModMaterials() {}
}
