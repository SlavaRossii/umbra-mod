package dev.umbra.madness;

import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Supplier;

/**
 * A craft only a slipping mind understands, done at the Whisper Table.
 * Hold {@code base} and use the table; {@code extras} come from your inventory.
 * Works only after the {@code insight} and only while sanity is at most {@code maxSanity}.
 */
public record Ritual(String id, String insight, float maxSanity, Supplier<? extends Item> base,
                     List<Ingredient> extras, Supplier<? extends Item> result) {
    public record Ingredient(Supplier<? extends Item> item, int count) {}
}
