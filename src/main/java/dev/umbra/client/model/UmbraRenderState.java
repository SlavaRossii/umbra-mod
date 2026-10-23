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
    /** 2.10: a boss: what it is doing ({@code UmbraBoss.ACT_*}) and for how many ticks it has been at it. */
    public int action;
    public float actionAge = 1000.0F;
    /** 2.10: how hard it has just been hit, 1 at the blow, 0 a moment later (it flinches). */
    public float hurt;
    /** 2.10: a boss, which dies slowly and its own way (no tipping over). */
    public boolean boss;

    /** 0..1 over the first {@code len} ticks of {@code action} if that is what it is doing now, else -1. */
    public float act(int action, float len) {
        return this.action == action && actionAge >= 0 && actionAge < len ? actionAge / len : -1.0F;
    }
}
