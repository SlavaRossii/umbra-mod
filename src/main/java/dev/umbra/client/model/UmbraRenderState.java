package dev.umbra.client.model;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Render state shared by all of Umbra's custom models. */
public class UmbraRenderState extends LivingEntityRenderState {
    /** Attack swing progress 0..1. */
    public float attack;
    /** Health fraction 0..1 (bosses change shape with it). */
    public float health = 1.0F;
    /** The entity is invisible (a hidden Queen): its glow is not drawn either. */
    public boolean hidden;
    /** Creature-specific switch (charging, frozen...). */
    public boolean flag;
    /** Creature-specific variant (statue pose, boss phase...). */
    public int variant;
    /** Creature-specific clock (the Watcher's stops while nobody looks at it). */
    public float clock;
    /** Creature-specific timers: ticks since something began, or -1 (the Watcher: weeping, being lit). */
    public float timer = -1.0F;
    public float timer2 = -1.0F;
    /** Block light where it stands, 0..15. */
    public float bright;
}
