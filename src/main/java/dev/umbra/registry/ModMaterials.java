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

    private ModMaterials() {}
}
