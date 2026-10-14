package dev.umbra.madness;

import dev.umbra.registry.ModItems;
import net.minecraft.world.item.Items;

import java.util.List;

/** All Whisper Table rituals. Add one here, plus its lang lines and a hint in the insight's journal entry. */
public final class Rituals {
    public static final List<Ritual> ALL = List.of(
            new Ritual("silence_tincture", "insight_1", 45.0F, () -> Items.GLASS_BOTTLE,
                    List.of(new Ritual.Ingredient(ModItems.SHADOW_SHARD, 1), new Ritual.Ingredient(ModItems.LUMINITE_CRYSTAL, 1)),
                    ModItems.SILENCE_TINCTURE),
            new Ritual("madman_mask", "insight_2", 30.0F, () -> Items.CARVED_PUMPKIN,
                    List.of(new Ritual.Ingredient(ModItems.SHADOW_SHARD, 4), new Ritual.Ingredient(ModItems.ECHO_QUARTZ, 1)),
                    ModItems.MADMAN_MASK),
            new Ritual("despair_blade", "insight_3", 15.0F, ModItems.GLOOM_SWORD,
                    List.of(new Ritual.Ingredient(ModItems.SHADOW_SHARD, 4), new Ritual.Ingredient(ModItems.ECHO_QUARTZ, 2)),
                    ModItems.DESPAIR_BLADE));

    private Rituals() {}
}
