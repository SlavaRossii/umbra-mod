package dev.umbra.registry;

import dev.umbra.Umbra;
import dev.umbra.boss.BossSummonItems;
import dev.umbra.boss.SummonSealItem;
import dev.umbra.light.DawnLanternItem;
import dev.umbra.item.MirrorItem;
import dev.umbra.item.QuantumCompassItem;
import dev.umbra.journal.JournalItem;
import dev.umbra.light.LightArrowItem;
import dev.umbra.item.PillItem;
import dev.umbra.light.DispellerItem;
import dev.umbra.light.LanternItem;
import dev.umbra.underside.ShadowShardItem;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.StandingAndWallBlockItem;

import java.util.List;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Umbra.MODID);

    public static final DeferredItem<BlockItem> QUANTUM_BLOCK = ITEMS.registerSimpleBlockItem("quantum_block", ModBlocks.QUANTUM_BLOCK);
    public static final DeferredItem<BlockItem> MAZE_WALL = ITEMS.registerSimpleBlockItem("maze_wall", ModBlocks.MAZE_WALL);

    // --- Luminite ---
    public static final DeferredItem<Item> LUMINITE_CRYSTAL = ITEMS.registerSimpleItem("luminite_crystal");
    public static final DeferredItem<BlockItem> LUMINITE_ORE = ITEMS.registerSimpleBlockItem("luminite_ore", ModBlocks.LUMINITE_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_LUMINITE_ORE = ITEMS.registerSimpleBlockItem("deepslate_luminite_ore", ModBlocks.DEEPSLATE_LUMINITE_ORE);
    public static final DeferredItem<BlockItem> LUMINITE_LAMP = ITEMS.registerSimpleBlockItem("luminite_lamp", ModBlocks.LUMINITE_LAMP);
    public static final DeferredItem<StandingAndWallBlockItem> ETERNAL_TORCH = ITEMS.registerItem("eternal_torch",
            p -> new StandingAndWallBlockItem(ModBlocks.ETERNAL_TORCH.get(), ModBlocks.ETERNAL_WALL_TORCH.get(), Direction.DOWN, p.useBlockDescriptionPrefix()));

    // --- Tools against the dark ---
    public static final DeferredItem<DispellerItem> DISPELLER = ITEMS.registerItem("dispeller", DispellerItem::new,
            p -> p.durability(40).repairable(LUMINITE_CRYSTAL.get()).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<LanternItem> LUMINITE_LANTERN = ITEMS.registerItem("luminite_lantern", LanternItem::new,
            p -> p.durability(LanternItem.CAPACITY).repairable(LUMINITE_CRYSTAL.get()));
    public static final DeferredItem<BlockItem> WARD = ITEMS.registerSimpleBlockItem("ward", ModBlocks.WARD, p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<MirrorItem> MIRROR = ITEMS.registerItem("mirror", MirrorItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<ShadowShardItem> SHADOW_SHARD = ITEMS.registerItem("shadow_shard", ShadowShardItem::new, p -> p.rarity(Rarity.UNCOMMON));

    // --- Sanity ---
    public static final DeferredItem<PillItem> SEDATIVE = ITEMS.registerItem("sedative",
            p -> new PillItem(p, 30.0F, 20 * 30, List.of()), p -> p.stacksTo(16));
    public static final DeferredItem<PillItem> ANTIPSYCHOTIC = ITEMS.registerItem("antipsychotic",
            p -> new PillItem(p, 100.0F, 20 * 120, List.of(
                    new MobEffectInstance(MobEffects.SLOWNESS, 20 * 15, 0),
                    new MobEffectInstance(MobEffects.HUNGER, 20 * 10, 0))),
            p -> p.stacksTo(8).rarity(Rarity.RARE));

    // --- The Underside ---
    public static final DeferredItem<BlockItem> ASH_STONE = ITEMS.registerSimpleBlockItem("ash_stone", ModBlocks.ASH_STONE);
    public static final DeferredItem<BlockItem> ASH_SOIL = ITEMS.registerSimpleBlockItem("ash_soil", ModBlocks.ASH_SOIL);
    public static final DeferredItem<BlockItem> DUSK_STONE = ITEMS.registerSimpleBlockItem("dusk_stone", ModBlocks.DUSK_STONE);
    public static final DeferredItem<BlockItem> DEAD_LOG = ITEMS.registerSimpleBlockItem("dead_log", ModBlocks.DEAD_LOG);
    public static final DeferredItem<BlockItem> GLOOM_ORE = ITEMS.registerSimpleBlockItem("gloom_ore", ModBlocks.GLOOM_ORE);
    public static final DeferredItem<BlockItem> ECHO_QUARTZ_ORE = ITEMS.registerSimpleBlockItem("echo_quartz_ore", ModBlocks.ECHO_QUARTZ_ORE);
    public static final DeferredItem<BlockItem> LUMINITE_BLOCK = ITEMS.registerSimpleBlockItem("luminite_block", ModBlocks.LUMINITE_BLOCK);
    public static final DeferredItem<BlockItem> GLOOM_BLOCK = ITEMS.registerSimpleBlockItem("gloom_block", ModBlocks.GLOOM_BLOCK);
    public static final DeferredItem<Item> RAW_GLOOM = ITEMS.registerSimpleItem("raw_gloom");
    public static final DeferredItem<Item> GLOOM_INGOT = ITEMS.registerSimpleItem("gloom_ingot");
    public static final DeferredItem<Item> ECHO_QUARTZ = ITEMS.registerSimpleItem("echo_quartz", p -> p.rarity(Rarity.UNCOMMON));
    /** 2.7: what each of the four seal bosses leaves behind; tier III armour is made with it. */
    public static final DeferredItem<Item> SEAL_SHARD = ITEMS.registerSimpleItem("seal_shard", p -> p.rarity(Rarity.RARE));

    // --- Gloom gear ---
    public static final DeferredItem<Item> GLOOM_SWORD = ITEMS.registerSimpleItem("gloom_sword", p -> p.sword(ModMaterials.GLOOM_TOOLS, 3.0F, -2.4F));
    public static final DeferredItem<Item> GLOOM_PICKAXE = ITEMS.registerSimpleItem("gloom_pickaxe", p -> p.pickaxe(ModMaterials.GLOOM_TOOLS, 1.0F, -2.8F));
    public static final DeferredItem<Item> GLOOM_AXE = ITEMS.registerSimpleItem("gloom_axe", p -> p.axe(ModMaterials.GLOOM_TOOLS, 5.0F, -3.0F));
    public static final DeferredItem<Item> GLOOM_SHOVEL = ITEMS.registerSimpleItem("gloom_shovel", p -> p.shovel(ModMaterials.GLOOM_TOOLS, 1.5F, -3.0F));
    /** All 36 armour pieces of the three sets (gloom_helmet, gloom_helmet_2, luminite_boots_3, ...), in creative-tab order. */
    public static final java.util.List<DeferredItem<Item>> ARMOR_PIECES = new java.util.ArrayList<>();

    static {
        net.minecraft.world.item.equipment.ArmorType[] types = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};
        for (int set = 0; set < 3; set++)
            for (int tier = 1; tier <= 3; tier++)
                for (int i = 0; i < 4; i++) {
                    final int fs = set, ft = tier, fi = i;
                    String name = dev.umbra.gear.Gear.SETS[set] + "_" + dev.umbra.gear.Gear.PIECES[i] + (tier == 1 ? "" : "_" + tier);
                    if (set == 0 && tier == 1) continue;   // the original gloom armour is registered by hand below
                    ARMOR_PIECES.add(ITEMS.registerSimpleItem(name, p -> p.humanoidArmor(ModMaterials.armor(fs, ft), types[fi])));
                }
    }

    /** A real shield (right click to block, like the vanilla one), with the aegis' own powers on top (EnchantEvents.onIncoming). */
    // 2.7: a ShieldItem, or first person treats it as a blocking sword and turns it on its edge out of sight
    public static final DeferredItem<net.minecraft.world.item.ShieldItem> LUMINITE_SHIELD = ITEMS.registerItem("luminite_shield", net.minecraft.world.item.ShieldItem::new, p -> p.stacksTo(1).durability(504).rarity(Rarity.UNCOMMON)
            .component(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS,
                    new net.minecraft.world.item.component.BlocksAttacks(0.25F, 1.0F,
                            java.util.List.of(new net.minecraft.world.item.component.BlocksAttacks.DamageReduction(90.0F, java.util.Optional.empty(), 0.0F, 1.0F)),
                            new net.minecraft.world.item.component.BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                            java.util.Optional.empty(),
                            java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK),
                            java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK)))
            .repairable(LUMINITE_CRYSTAL.get()));
    public static final DeferredItem<Item> ASH_CLASP = ITEMS.registerSimpleItem("ash_clasp", p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> ARCHITECT_SEAL = ITEMS.registerSimpleItem("architect_seal", p -> p.stacksTo(1).rarity(Rarity.EPIC));

    public static final DeferredItem<Item> GLOOM_HELMET = ITEMS.registerSimpleItem("gloom_helmet", p -> p.humanoidArmor(ModMaterials.GLOOM_ARMOR, ArmorType.HELMET));
    public static final DeferredItem<Item> GLOOM_CHESTPLATE = ITEMS.registerSimpleItem("gloom_chestplate", p -> p.humanoidArmor(ModMaterials.GLOOM_ARMOR, ArmorType.CHESTPLATE));
    public static final DeferredItem<Item> GLOOM_LEGGINGS = ITEMS.registerSimpleItem("gloom_leggings", p -> p.humanoidArmor(ModMaterials.GLOOM_ARMOR, ArmorType.LEGGINGS));
    public static final DeferredItem<Item> GLOOM_BOOTS = ITEMS.registerSimpleItem("gloom_boots", p -> p.humanoidArmor(ModMaterials.GLOOM_ARMOR, ArmorType.BOOTS));

    // --- Spawn eggs ---
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> LURKER_SPAWN_EGG = ITEMS.registerItem("lurker_spawn_egg", net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.LURKER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> CRAWLER_SPAWN_EGG = ITEMS.registerItem("crawler_spawn_egg", net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.CRAWLER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> ACOLYTE_SPAWN_EGG = ITEMS.registerItem("acolyte_spawn_egg", net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.ACOLYTE.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> DROWNED_KEEPER_SPAWN_EGG = ITEMS.registerItem("drowned_keeper_spawn_egg", net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.DROWNED_KEEPER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> WEEPER_SPAWN_EGG = ITEMS.registerItem("weeper_spawn_egg", net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.WEEPER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> BELLRINGER_SPAWN_EGG = ITEMS.registerItem("bellringer_spawn_egg", net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BELLRINGER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> BLIND_SPAWN_EGG = ITEMS.registerItem("blind_spawn_egg", net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BLIND.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> LIGHT_EATER_SPAWN_EGG = ITEMS.registerItem("light_eater_spawn_egg", net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.LIGHT_EATER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> MIMICKER_SPAWN_EGG = ITEMS.registerItem("mimicker_spawn_egg", net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.MIMICKER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> SHADOW_VILLAGER_SPAWN_EGG = ITEMS.registerItem("shadow_villager_spawn_egg", net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.SHADOW_VILLAGER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> CITADEL_STATUE_SPAWN_EGG = ITEMS.registerItem("citadel_statue_spawn_egg", net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.CITADEL_STATUE.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> CITADEL_KEEPER_SPAWN_EGG = ITEMS.registerItem("citadel_keeper_spawn_egg", net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.CITADEL_KEEPER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> SHADE_SPAWN_EGG = ITEMS.registerItem("shade_spawn_egg", net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.SHADE.get()));

    // --- Minibosses: seals, trophies, the key ---
    public static final DeferredItem<SummonSealItem> SEAL_OF_WATCHING = ITEMS.registerItem("seal_of_watching",
            p -> new SummonSealItem(p, ModEntities.WATCHER), p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<SummonSealItem> SEAL_OF_MIMICRY = ITEMS.registerItem("seal_of_mimicry",
            p -> new SummonSealItem(p, ModEntities.MIMIC_QUEEN), p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<SummonSealItem> SEAL_OF_HERDING = ITEMS.registerItem("seal_of_herding",
            p -> new SummonSealItem(p, ModEntities.SHEPHERD), p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<SummonSealItem> SEAL_OF_REFLECTION = ITEMS.registerItem("seal_of_reflection",
            p -> new SummonSealItem(p, ModEntities.DOPPELGANGER), p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> WATCHER_EYE = ITEMS.registerSimpleItem("watcher_eye", p -> p.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> MIMIC_CROWN = ITEMS.registerSimpleItem("mimic_crown", p -> p.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> SHEPHERD_CROOK = ITEMS.registerSimpleItem("shepherd_crook", p -> p.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> MIRROR_MASK = ITEMS.registerSimpleItem("mirror_mask", p -> p.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<BossSummonItems.Key> UNDERSIDE_KEY = ITEMS.registerItem("underside_key", BossSummonItems.Key::new, p -> p.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<BossSummonItems.Heart> ABYSS_HEART = ITEMS.registerItem("abyss_heart", BossSummonItems.Heart::new, p -> p.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<DawnLanternItem> DAWN_LANTERN = ITEMS.registerItem("dawn_lantern", DawnLanternItem::new,
            p -> p.durability(256).repairable(LUMINITE_CRYSTAL.get()).rarity(Rarity.EPIC).fireResistant());

    // --- Madness (1.2) ---
    public static final DeferredItem<BlockItem> LUMINITE_LANTERN_BLOCK = ITEMS.registerSimpleBlockItem("luminite_lantern_block", ModBlocks.LUMINITE_LANTERN_BLOCK);
    public static final DeferredItem<BlockItem> TROPHY_SHELF = ITEMS.registerSimpleBlockItem("trophy_shelf", ModBlocks.TROPHY_SHELF);
    /** The relic each boss leaves behind, for the trophy shelf. */
    public static final java.util.Map<String, DeferredItem<Item>> TROPHIES = new java.util.LinkedHashMap<>();

    static {
        for (String b : new String[]{"watcher", "mimic_queen", "shepherd", "doppelganger", "architect", "primordial_shadow"}) {
            TROPHIES.put(b, ITEMS.registerSimpleItem("trophy_" + b, p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
        }
    }

    public static final DeferredItem<BlockItem> ECHO_ALTAR = ITEMS.registerSimpleBlockItem("echo_altar", ModBlocks.ECHO_ALTAR, p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> GLOOM_ANVIL = ITEMS.registerSimpleBlockItem("gloom_anvil", ModBlocks.GLOOM_ANVIL, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> LUMINITE_CANDLE = ITEMS.registerSimpleBlockItem("luminite_candle", ModBlocks.LUMINITE_CANDLE);
    public static final DeferredItem<BlockItem> THICK_LUMINITE_CANDLE = ITEMS.registerSimpleBlockItem("thick_luminite_candle", ModBlocks.THICK_LUMINITE_CANDLE);
    public static final DeferredItem<BlockItem> MELTED_LUMINITE_CANDLE = ITEMS.registerSimpleBlockItem("melted_luminite_candle", ModBlocks.MELTED_LUMINITE_CANDLE);
    public static final DeferredItem<BlockItem> LUMINITE_CANDELABRUM = ITEMS.registerSimpleBlockItem("luminite_candelabrum", ModBlocks.LUMINITE_CANDELABRUM);
    public static final DeferredItem<BlockItem> WHISPER_TABLE = ITEMS.registerSimpleBlockItem("whisper_table", ModBlocks.WHISPER_TABLE, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<dev.umbra.madness.SilenceTinctureItem> SILENCE_TINCTURE = ITEMS.registerItem("silence_tincture",
            dev.umbra.madness.SilenceTinctureItem::new, p -> p.stacksTo(8).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<dev.umbra.madness.MadmanMaskItem> MADMAN_MASK = ITEMS.registerItem("madman_mask",
            dev.umbra.madness.MadmanMaskItem::new, p -> p.stacksTo(1).rarity(Rarity.RARE).equippable(net.minecraft.world.entity.EquipmentSlot.HEAD));
    // --- 2.5: weapons ---
    public static final DeferredItem<Item> LUMINITE_SPEAR = ITEMS.registerSimpleItem("luminite_spear", p -> p.sword(ModMaterials.GLOOM_TOOLS, 3.0F, -2.8F)
            .attributes(weapon("spear", 7.0, -2.8, 1.5)).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> SHEPHERD_SCYTHE = ITEMS.registerSimpleItem("shepherd_scythe", p -> p.sword(ModMaterials.GLOOM_TOOLS, 3.0F, -3.0F)
            .attributes(weapon("scythe", 8.0, -3.0, 0.5)).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> SHADOW_DAGGER = ITEMS.registerSimpleItem("shadow_dagger", p -> p.sword(ModMaterials.GLOOM_TOOLS, 3.0F, -1.4F)
            .attributes(weapon("dagger", 4.0, -1.4, -0.5)).rarity(Rarity.UNCOMMON));

    // --- 2.5: weapons for a distance ---
    public static final DeferredItem<net.minecraft.world.item.BowItem> LUMINITE_BOW = ITEMS.registerItem("luminite_bow",
            net.minecraft.world.item.BowItem::new, p -> p.durability(512).enchantable(1).repairable(LUMINITE_CRYSTAL.get()).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<dev.umbra.gear.RangedWeapons.ShadowKnifeItem> SHADOW_KNIFE = ITEMS.registerItem("shadow_knife",
            dev.umbra.gear.RangedWeapons.ShadowKnifeItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<dev.umbra.gear.RangedWeapons.EchoStaffItem> ECHO_STAFF = ITEMS.registerItem("echo_staff",
            dev.umbra.gear.RangedWeapons.EchoStaffItem::new, p -> p.stacksTo(1).durability(250).rarity(Rarity.RARE));

    // --- 2.6: Yan's gifts ---
    public static final DeferredItem<dev.umbra.lighthouse.YanErrands.MartaLanternItem> MARTA_LANTERN = ITEMS.registerItem("marta_lantern",
            dev.umbra.lighthouse.YanErrands.MartaLanternItem::new, p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<dev.umbra.lighthouse.YanErrands.YanSpyglassItem> YAN_SPYGLASS = ITEMS.registerItem("yan_spyglass",
            dev.umbra.lighthouse.YanErrands.YanSpyglassItem::new, p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<dev.umbra.lighthouse.YanErrands.KeeperBellItem> KEEPER_BELL = ITEMS.registerItem("keeper_bell",
            dev.umbra.lighthouse.YanErrands.KeeperBellItem::new, p -> p.stacksTo(1).rarity(Rarity.RARE));

    // --- 2.6: the Depths ---
    public static final DeferredItem<dev.umbra.depths.Depths.KeyItem> DEPTHS_KEY = ITEMS.registerItem("depths_key",
            dev.umbra.depths.Depths.KeyItem::new, p -> p.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> KEEPERS_PEARL = ITEMS.registerSimpleItem("keepers_pearl", p -> p.stacksTo(1).rarity(Rarity.EPIC));

    /** Damage and speed as vanilla counts them (on top of the hand's 1 and 4), and extra reach. */
    private static net.minecraft.world.item.component.ItemAttributeModifiers weapon(String name, double damage, double speed, double reach) {
        var b = net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_ID, damage, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Item.BASE_ATTACK_SPEED_ID, speed, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        if (reach != 0) b.add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, name + "_reach"), reach, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        return b.build();
    }

    public static final DeferredItem<dev.umbra.madness.DespairBladeItem> DESPAIR_BLADE = ITEMS.registerItem("despair_blade",
            dev.umbra.madness.DespairBladeItem::new, p -> p.sword(ModMaterials.GLOOM_TOOLS, 4.0F, -2.4F).rarity(Rarity.EPIC));

    // --- Underside decoration (1.5) ---
    public static final DeferredItem<BlockItem> LUMINITE_CLUSTER = ITEMS.registerSimpleBlockItem("luminite_cluster", ModBlocks.LUMINITE_CLUSTER);
    public static final DeferredItem<BlockItem> ECHO_CLUSTER = ITEMS.registerSimpleBlockItem("echo_cluster", ModBlocks.ECHO_CLUSTER);
    public static final DeferredItem<BlockItem> GLOOM_MUSHROOM = ITEMS.registerSimpleBlockItem("gloom_mushroom", ModBlocks.GLOOM_MUSHROOM);
    public static final DeferredItem<dev.umbra.item.ExplorerNoteItem> EXPLORER_NOTE = ITEMS.registerItem("explorer_note",
            dev.umbra.item.ExplorerNoteItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<BlockItem> LIGHTHOUSE_LAMP = ITEMS.registerSimpleBlockItem("lighthouse_lamp", ModBlocks.LIGHTHOUSE_LAMP, p -> p.rarity(Rarity.EPIC));
    public static final DeferredItem<dev.umbra.lighthouse.MartaMapItem> MARTA_MAP = ITEMS.registerItem("marta_map",
            dev.umbra.lighthouse.MartaMapItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    /** 2.7: Yan's own staff, his gift at the dawn. */
    public static final DeferredItem<dev.umbra.lighthouse.KeeperStaffItem> KEEPER_STAFF = ITEMS.registerItem("keeper_staff",
            dev.umbra.lighthouse.KeeperStaffItem::new, p -> p.stacksTo(1).durability(121).rarity(Rarity.EPIC).repairable(LUMINITE_CRYSTAL.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> FALLEN_KEEPER_SPAWN_EGG = ITEMS.registerItem("fallen_keeper_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.FALLEN_KEEPER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> LAMP_KEEPER_SPAWN_EGG = ITEMS.registerItem("lamp_keeper_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.LAMP_KEEPER.get()));
    public static final DeferredItem<dev.umbra.lighthouse.KeeperHornItem> KEEPER_HORN = ITEMS.registerItem("keeper_horn",
            dev.umbra.lighthouse.KeeperHornItem::new, p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<dev.umbra.ending.PrimordialHeartItem> PRIMORDIAL_HEART = ITEMS.registerItem("primordial_heart",
            dev.umbra.ending.PrimordialHeartItem::new, p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<dev.umbra.item.KeeperPageItem> KEEPER_PAGE = ITEMS.registerItem("keeper_page",
            dev.umbra.item.KeeperPageItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> ASH_FIGURE_SPAWN_EGG = ITEMS.registerItem("ash_figure_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.ASH_FIGURE.get()));

    // --- Polish ---
    public static final DeferredItem<JournalItem> JOURNAL = ITEMS.registerItem("journal", JournalItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<QuantumCompassItem> QUANTUM_COMPASS = ITEMS.registerItem("quantum_compass", QuantumCompassItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<LightArrowItem> LIGHT_ARROW = ITEMS.registerItem("light_arrow", LightArrowItem::new);

    private ModItems() {}
}
