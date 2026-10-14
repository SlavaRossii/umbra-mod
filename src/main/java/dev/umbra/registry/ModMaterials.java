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
            35,
            Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 7, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3, ArmorType.BODY, 9),
            16,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            2.5F,
            0.05F,
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

    /** @param set 0 gloom, 1 luminite, 2 shadow; @param tier 1..3 */
    public static ArmorMaterial armor(int set, int tier) {
        return switch (set) {
            case 0 -> switch (tier) {
                case 1 -> GLOOM_ARMOR;
                case 2 -> mat(38, 3, 8, 6, 3, 16, 3.0F, 0.07F, GLOOM_REPAIR, GLOOM_ASSET);
                default -> mat(42, 4, 8, 7, 4, 16, 3.5F, 0.10F, GLOOM_REPAIR, GLOOM_ASSET);
            };
            case 1 -> switch (tier) {
                case 1 -> mat(24, 2, 6, 5, 2, 22, 0.5F, 0.0F, LUMINITE_REPAIR, LUMINITE_ASSET);
                case 2 -> mat(28, 3, 6, 5, 3, 22, 1.0F, 0.0F, LUMINITE_REPAIR, LUMINITE_ASSET);
                default -> mat(33, 3, 7, 6, 3, 22, 1.5F, 0.0F, LUMINITE_REPAIR, LUMINITE_ASSET);
            };
            default -> switch (tier) {
                case 1 -> mat(20, 2, 5, 4, 2, 24, 0.0F, 0.0F, SHADOW_REPAIR, SHADOW_ASSET);
                case 2 -> mat(25, 3, 5, 4, 2, 24, 0.5F, 0.0F, SHADOW_REPAIR, SHADOW_ASSET);
                default -> mat(30, 3, 6, 5, 3, 24, 1.0F, 0.0F, SHADOW_REPAIR, SHADOW_ASSET);
            };
        };
    }

    private ModMaterials() {}
}
