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
    public static final DeferredItem<BlockItem> GLOOM_BLOCK = ITEMS.registerSimpleBlockItem("gloom_block", ModBlocks.GLOOM_BLOCK);
    public static final DeferredItem<Item> RAW_GLOOM = ITEMS.registerSimpleItem("raw_gloom");
    public static final DeferredItem<Item> GLOOM_INGOT = ITEMS.registerSimpleItem("gloom_ingot");
    public static final DeferredItem<Item> ECHO_QUARTZ = ITEMS.registerSimpleItem("echo_quartz", p -> p.rarity(Rarity.UNCOMMON));

    // --- Gloom gear ---
    public static final DeferredItem<Item> GLOOM_SWORD = ITEMS.registerSimpleItem("gloom_sword", p -> p.sword(ModMaterials.GLOOM_TOOLS, 3.0F, -2.4F));
    public static final DeferredItem<Item> GLOOM_PICKAXE = ITEMS.registerSimpleItem("gloom_pickaxe", p -> p.pickaxe(ModMaterials.GLOOM_TOOLS, 1.0F, -2.8F));
    public static final DeferredItem<Item> GLOOM_AXE = ITEMS.registerSimpleItem("gloom_axe", p -> p.axe(ModMaterials.GLOOM_TOOLS, 5.0F, -3.0F));
    public static final DeferredItem<Item> GLOOM_SHOVEL = ITEMS.registerSimpleItem("gloom_shovel", p -> p.shovel(ModMaterials.GLOOM_TOOLS, 1.5F, -3.0F));
    public static final DeferredItem<Item> GLOOM_HELMET = ITEMS.registerSimpleItem("gloom_helmet", p -> p.humanoidArmor(ModMaterials.GLOOM_ARMOR, ArmorType.HELMET));
    public static final DeferredItem<Item> GLOOM_CHESTPLATE = ITEMS.registerSimpleItem("gloom_chestplate", p -> p.humanoidArmor(ModMaterials.GLOOM_ARMOR, ArmorType.CHESTPLATE));
    public static final DeferredItem<Item> GLOOM_LEGGINGS = ITEMS.registerSimpleItem("gloom_leggings", p -> p.humanoidArmor(ModMaterials.GLOOM_ARMOR, ArmorType.LEGGINGS));
    public static final DeferredItem<Item> GLOOM_BOOTS = ITEMS.registerSimpleItem("gloom_boots", p -> p.humanoidArmor(ModMaterials.GLOOM_ARMOR, ArmorType.BOOTS));

    // --- Spawn eggs ---
    public static final DeferredItem<Item> CRAWLER_SPAWN_EGG = ITEMS.registerSimpleItem("crawler_spawn_egg", p -> p.spawnEgg(ModEntities.CRAWLER.get()));
    public static final DeferredItem<Item> BLIND_SPAWN_EGG = ITEMS.registerSimpleItem("blind_spawn_egg", p -> p.spawnEgg(ModEntities.BLIND.get()));
    public static final DeferredItem<Item> LIGHT_EATER_SPAWN_EGG = ITEMS.registerSimpleItem("light_eater_spawn_egg", p -> p.spawnEgg(ModEntities.LIGHT_EATER.get()));
    public static final DeferredItem<Item> MIMICKER_SPAWN_EGG = ITEMS.registerSimpleItem("mimicker_spawn_egg", p -> p.spawnEgg(ModEntities.MIMICKER.get()));
    public static final DeferredItem<Item> SHADOW_VILLAGER_SPAWN_EGG = ITEMS.registerSimpleItem("shadow_villager_spawn_egg", p -> p.spawnEgg(ModEntities.SHADOW_VILLAGER.get()));
    public static final DeferredItem<Item> CITADEL_STATUE_SPAWN_EGG = ITEMS.registerSimpleItem("citadel_statue_spawn_egg", p -> p.spawnEgg(ModEntities.CITADEL_STATUE.get()));
    public static final DeferredItem<Item> CITADEL_KEEPER_SPAWN_EGG = ITEMS.registerSimpleItem("citadel_keeper_spawn_egg", p -> p.spawnEgg(ModEntities.CITADEL_KEEPER.get()));
    public static final DeferredItem<Item> SHADE_SPAWN_EGG = ITEMS.registerSimpleItem("shade_spawn_egg", p -> p.spawnEgg(ModEntities.SHADE.get()));

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
    public static final DeferredItem<BlockItem> WHISPER_TABLE = ITEMS.registerSimpleBlockItem("whisper_table", ModBlocks.WHISPER_TABLE, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<dev.umbra.madness.SilenceTinctureItem> SILENCE_TINCTURE = ITEMS.registerItem("silence_tincture",
            dev.umbra.madness.SilenceTinctureItem::new, p -> p.stacksTo(8).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<dev.umbra.madness.MadmanMaskItem> MADMAN_MASK = ITEMS.registerItem("madman_mask",
            dev.umbra.madness.MadmanMaskItem::new, p -> p.stacksTo(1).rarity(Rarity.RARE).equippable(net.minecraft.world.entity.EquipmentSlot.HEAD));
    public static final DeferredItem<dev.umbra.madness.DespairBladeItem> DESPAIR_BLADE = ITEMS.registerItem("despair_blade",
            dev.umbra.madness.DespairBladeItem::new, p -> p.sword(ModMaterials.GLOOM_TOOLS, 3.0F, -2.4F).rarity(Rarity.EPIC));

    // --- Polish ---
    public static final DeferredItem<JournalItem> JOURNAL = ITEMS.registerItem("journal", JournalItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<QuantumCompassItem> QUANTUM_COMPASS = ITEMS.registerItem("quantum_compass", QuantumCompassItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<LightArrowItem> LIGHT_ARROW = ITEMS.registerItem("light_arrow", LightArrowItem::new);

    private ModItems() {}
}
