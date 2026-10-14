package dev.umbra.registry;

import dev.umbra.Umbra;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sounds of the Underside (procedural, made by tools/gen_sounds.py). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Umbra.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> DRIP = reg("ambient.drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> BELL = reg("ambient.bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEPS_ABOVE = reg("ambient.steps_above");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEPS_BEHIND = reg("ambient.steps_behind");
    public static final DeferredHolder<SoundEvent, SoundEvent> KNOCK = reg("ambient.knock");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISPER = reg("ambient.whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> BREATH = reg("ambient.breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> CREAK = reg("ambient.creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> WAIL = reg("ambient.wail");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUST = reg("ambient.gust");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPLASH = reg("ambient.splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEARTBEAT_FAR = reg("ambient.heartbeat_far");

    private ModSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Umbra.MODID, name)));
    }
}
