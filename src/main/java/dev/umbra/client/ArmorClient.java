package dev.umbra.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Gives Umbra's armour its own 3D models (see {@link ArmorModels}): one model per set and slot. */
final class ArmorClient {
    private ArmorClient() {}

    private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};

    static void register(RegisterClientExtensionsEvent event) {
        for (String set : ArmorModels.SETS) {
            List<Item> items = new ArrayList<>();
            for (String piece : PIECES)
                for (String tier : new String[]{"", "_2", "_3"}) {
                    var id = Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, set + "_" + piece + tier);
                    BuiltInRegistries.ITEM.getOptional(id).ifPresent(items::add);
                }
            if (items.isEmpty()) continue;
            event.registerItem(new Extension(set), items.toArray(new Item[0]));
        }
    }

    private static final class Extension implements IClientItemExtensions {
        private final String set;
        private final Map<EquipmentSlot, HumanoidModel<HumanoidRenderState>> models = new EnumMap<>(EquipmentSlot.class);

        Extension(String set) {
            this.set = set;
        }

        @Override
        public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType type, Model original) {
            var eq = stack.get(DataComponents.EQUIPPABLE);
            if (eq == null) return original;
            EquipmentSlot slot = eq.slot();
            if (slot == EquipmentSlot.LEGS && type != EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS) return original;
            if (slot != EquipmentSlot.LEGS && type != EquipmentClientInfo.LayerType.HUMANOID) return original;
            return models.computeIfAbsent(slot, this::bake);
        }

        private HumanoidModel<HumanoidRenderState> bake(EquipmentSlot slot) {
            boolean inner = slot == EquipmentSlot.LEGS;
            HumanoidModel<HumanoidRenderState> m = new HumanoidModel<>(ArmorModels.create(set, inner).bakeRoot());
            m.head.visible = m.hat.visible = slot == EquipmentSlot.HEAD;
            m.body.visible = slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS;
            m.rightArm.visible = m.leftArm.visible = slot == EquipmentSlot.CHEST;
            m.rightLeg.visible = m.leftLeg.visible = slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
            return m;
        }
    }
}
