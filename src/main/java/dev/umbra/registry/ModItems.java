package dev.umbra.registry;

import dev.umbra.Umbra;
import dev.umbra.item.MirrorItem;
import dev.umbra.item.PillItem;
import dev.umbra.light.DispellerItem;
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
            p -> p.durability(64).repairable(LUMINITE_CRYSTAL.get()).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<MirrorItem> MIRROR = ITEMS.registerItem("mirror", MirrorItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<Item> SHADOW_SHARD = ITEMS.registerSimpleItem("shadow_shard", p -> p.rarity(Rarity.UNCOMMON));

    // --- Sanity ---
    public static final DeferredItem<PillItem> SEDATIVE = ITEMS.registerItem("sedative",
            p -> new PillItem(p, 30.0F, 20 * 30, List.of()), p -> p.stacksTo(16));
    public static final DeferredItem<PillItem> ANTIPSYCHOTIC = ITEMS.registerItem("antipsychotic",
            p -> new PillItem(p, 100.0F, 20 * 120, List.of(
                    new MobEffectInstance(MobEffects.SLOWNESS, 20 * 15, 0),
                    new MobEffectInstance(MobEffects.HUNGER, 20 * 10, 0))),
            p -> p.stacksTo(8).rarity(Rarity.RARE));

    private ModItems() {}
}
