package dev.umbra.sanity;

import dev.umbra.UmbraConfig;
import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModMaterials;
import dev.umbra.underside.Underside;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.core.component.DataComponents;
import dev.umbra.util.UmbraUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sanity, 0..100. Darkness, monsters and Umbra's events drain it; light, daytime, sleep, food and pills restore it.
 * Low sanity brings hallucinations ({@link Hallucinations}). Tuned to be scary but never a death sentence:
 * sanity itself never deals damage.
 *
 * Other features call {@link #add(Player, float)} with their own deltas (see {@link Shock}).
 */
public final class Sanity {
    public static final float MAX = 100.0F;
    /** Below this: whispers and fake sounds. */
    public static final float ANXIOUS = 70.0F;
    /** Below this: things at the edge of your vision. */
    public static final float PARANOID = 45.0F;
    /** Below this: the world starts to swim. */
    public static final float BREAKDOWN = 20.0F;

    /** Named one-off shocks so balance lives in one place. */
    public static final class Shock {
        public static final float SHADOW_DETACHED = -3.0F;
        public static final float SHADOW_CAUGHT = -15.0F;
        public static final float MIMIC_SPRUNG = -10.0F;
        public static final float STEPS_FINALE = -8.0F;
        public static final float SLEEP = 40.0F;
        public static final float FOOD = 2.0F;
        public static final float DISPELLER = 4.0F;
        public static final float SHADOW_DISPELLED = 6.0F;

        private Shock() {}
    }

    private static final Map<UUID, Float> LAST_SYNCED = new HashMap<>();

    private Sanity() {}

    public static boolean enabled() {
        return UmbraConfig.on(UmbraConfig.SANITY);
    }

    public static float get(Player player) {
        return player.getData(ModAttachments.SANITY);
    }

    public static void set(Player player, float value) {
        float v = Math.max(0.0F, Math.min(MAX, value));
        player.setData(ModAttachments.SANITY, v);
        if (player instanceof ServerPlayer sp) {
            Float last = LAST_SYNCED.get(sp.getUUID());
            if (last == null || Math.abs(last - v) >= 0.5F || v == 0.0F || v == MAX) {
                sp.syncData(ModAttachments.SANITY);
                LAST_SYNCED.put(sp.getUUID(), v);
            }
        }
    }

    /** Positive values heal, negative ones drain. Shocks bypass the difficulty multipliers. */
    public static void add(Player player, float delta) {
        if (!enabled() || player.isCreative() || player.isSpectator()) return;
        set(player, get(player) + delta);
    }

    public static void forget(ServerPlayer player) {
        LAST_SYNCED.remove(player.getUUID());
    }

    /** Called every tick for every server player. */
    public static void tick(ServerPlayer player) {
        if (!enabled() || player.isCreative() || player.isSpectator()) return;
        if (player.tickCount % 20 != 0) return;
        ServerLevel level = (ServerLevel) player.level();
        BlockPos eye = BlockPos.containing(player.getEyePosition());

        float drain = 0.0F;
        float regen = 0.0F;
        int light = UmbraUtil.effectiveLight(level, eye);
        boolean enclosed = UmbraUtil.skyLight(level, eye) == 0;

        if (light <= 3) drain += enclosed ? 0.40F : 0.25F;
        else if (light >= 10) regen += 0.20F;
        if (!enclosed && !UmbraUtil.isNight(level)) regen += 0.15F;
        if (level.getBlockState(eye).is(ModBlocks.LIVING_DARKNESS.get())) drain += 1.0F;

        int monsters = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(10.0)).size();
        drain += Math.min(0.4F, monsters * 0.1F);
        if (Underside.is(level)) drain += Underside.SANITY_DRAIN;
        // gloom armour steadies the mind: each piece takes 12% off the drain
        drain *= 1.0F - 0.12F * gloomPieces(player);

        float delta = regen * (float) UmbraConfig.num(UmbraConfig.SANITY_REGEN)
                - drain * (float) UmbraConfig.num(UmbraConfig.SANITY_DRAIN);
        if (delta != 0.0F) set(player, get(player) + delta);

        applyEffects(level, player, get(player));
    }

    private static void applyEffects(ServerLevel level, ServerPlayer player, float sanity) {
        if (sanity < BREAKDOWN) {
            if (player.getRandom().nextInt(30) == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 20 * 7, 0, false, false));
            }
            if (player.getRandom().nextInt(40) == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 4, 0, false, false));
            }
            if (sanity <= 0.0F) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
            }
        }
        Hallucinations.tick(level, player, sanity);
    }

    public static int gloomPieces(Player player) {
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            Equippable eq = player.getItemBySlot(slot).get(DataComponents.EQUIPPABLE);
            if (eq != null && eq.assetId().isPresent() && eq.assetId().get().equals(ModMaterials.GLOOM_ASSET)) n++;
        }
        return n;
    }

    /** Multiplier other features use to get nastier as sanity drops (1.0 at full sanity, up to 3.0). */
    public static float dread(Player player) {
        if (!enabled()) return 1.0F;
        float s = get(player);
        if (s >= ANXIOUS) return 1.0F;
        if (s >= PARANOID) return 1.5F;
        if (s >= BREAKDOWN) return 2.0F;
        return 3.0F;
    }
}
