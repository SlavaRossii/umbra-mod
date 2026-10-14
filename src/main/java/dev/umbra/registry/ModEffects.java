package dev.umbra.registry;

import dev.umbra.Umbra;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Umbra's own status effects. */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Umbra.MODID);

    /**
     * Clarity (from the tincture of silence): a calm mind. Nothing drains sanity, and the dark that clouds the eyes
     * (Darkness) or turns the stomach (Nausea) is wiped off at once, wherever it came from.
     */
    public static final DeferredHolder<MobEffect, MobEffect> CLARITY = MOB_EFFECTS.register("clarity", () -> new Clarity());

    private ModEffects() {}

    private static final class Clarity extends MobEffect {
        Clarity() {
            super(MobEffectCategory.BENEFICIAL, 0xBFD9FF);
        }
    }

    public static boolean hasClarity(net.minecraft.world.entity.LivingEntity e) {
        return e.hasEffect(CLARITY);
    }

    /** Every tick for survival players: clears what clarity protects against. */
    public static void tick(ServerPlayer player) {
        if (!hasClarity(player)) return;
        if (player.hasEffect(MobEffects.DARKNESS)) player.removeEffect(MobEffects.DARKNESS);
        if (player.hasEffect(MobEffects.NAUSEA)) player.removeEffect(MobEffects.NAUSEA);
    }
}
