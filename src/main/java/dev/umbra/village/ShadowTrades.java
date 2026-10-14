package dev.umbra.village;

import dev.umbra.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * What the shadow folk trade. Currency: shadow shards (and echo quartz for the good stuff).
 * Every villager gets two "buy" offers (you earn shards) and four "sell" offers.
 */
public final class ShadowTrades {
    private record Offer(Supplier<? extends ItemLike> cost, int costCount, Supplier<? extends ItemLike> cost2, int cost2Count,
                         Supplier<? extends ItemLike> result, int resultCount, int maxUses) {}

    private static Offer o(Supplier<? extends ItemLike> cost, int n, Supplier<? extends ItemLike> result, int m, int uses) {
        return new Offer(cost, n, null, 0, result, m, uses);
    }

    private static Offer o2(Supplier<? extends ItemLike> cost, int n, Supplier<? extends ItemLike> cost2, int n2, Supplier<? extends ItemLike> result, int m, int uses) {
        return new Offer(cost, n, cost2, n2, result, m, uses);
    }

    /** They take your junk and pay in shards. */
    private static final List<Offer> BUYS = List.of(
            o(() -> Items.ROTTEN_FLESH, 24, ModItems.SHADOW_SHARD, 1, 8),
            o(() -> ModItems.DEAD_LOG.get(), 16, ModItems.SHADOW_SHARD, 1, 8),
            o(() -> ModItems.ASH_SOIL.get(), 32, ModItems.SHADOW_SHARD, 1, 8),
            o(() -> ModItems.RAW_GLOOM.get(), 3, ModItems.SHADOW_SHARD, 2, 6),
            o(() -> Items.BONE, 16, ModItems.SHADOW_SHARD, 1, 8),
            o(() -> ModItems.LUMINITE_CRYSTAL.get(), 2, ModItems.SHADOW_SHARD, 1, 6));

    /** What they sell. */
    private static final List<Offer> SELLS = List.of(
            o(ModItems.SHADOW_SHARD, 1, () -> Items.BREAD, 6, 12),
            o(ModItems.SHADOW_SHARD, 2, () -> Items.COOKED_BEEF, 4, 8),
            o(ModItems.SHADOW_SHARD, 2, ModItems.SEDATIVE, 3, 6),
            o(ModItems.SHADOW_SHARD, 5, ModItems.ANTIPSYCHOTIC, 1, 3),
            o(ModItems.SHADOW_SHARD, 3, ModItems.LUMINITE_CRYSTAL, 2, 6),
            o(ModItems.SHADOW_SHARD, 3, ModItems.ETERNAL_TORCH, 6, 6),
            o(ModItems.SHADOW_SHARD, 2, ModItems.LIGHT_ARROW, 8, 6),
            o(ModItems.SHADOW_SHARD, 6, ModItems.GLOOM_INGOT, 1, 4),
            o(ModItems.ECHO_QUARTZ, 1, ModItems.SHADOW_SHARD, 4, 4),
            o2(ModItems.SHADOW_SHARD, 8, ModItems.ECHO_QUARTZ, 1, ModItems.QUANTUM_COMPASS, 1, 1),
            o2(ModItems.SHADOW_SHARD, 10, ModItems.ECHO_QUARTZ, 2, ModItems.WARD, 1, 1),
            o(ModItems.SHADOW_SHARD, 4, ModItems.SILENCE_TINCTURE, 1, 2),
            o(ModItems.SHADOW_SHARD, 6, () -> Items.GOLDEN_APPLE, 1, 2),
            o(ModItems.SHADOW_SHARD, 12, ModItems.ECHO_QUARTZ, 1, 2));

    private ShadowTrades() {}

    public static void fill(MerchantOffers offers, RandomSource random) {
        add(offers, BUYS, 2, random);
        add(offers, SELLS, 4, random);
    }

    private static void add(MerchantOffers offers, List<Offer> pool, int n, RandomSource random) {
        List<Offer> copy = new ArrayList<>(pool);
        Collections.shuffle(copy, new java.util.Random(random.nextLong()));
        for (int i = 0; i < n && i < copy.size(); i++) {
            Offer o = copy.get(i);
            Optional<ItemCost> b = o.cost2() == null ? Optional.empty() : Optional.of(new ItemCost(o.cost2().get(), o.cost2Count()));
            offers.add(new MerchantOffer(new ItemCost(o.cost().get(), o.costCount()), b,
                    new ItemStack(o.result().get(), o.resultCount()), o.maxUses(), 1, 0.05F));
        }
    }

    public static int poolSize() {
        return BUYS.size() + SELLS.size();
    }
}
