package dev.umbra.gear;

import dev.umbra.journal.Bestiary;
import dev.umbra.light.LightSensitive;
import dev.umbra.registry.ModComponents;
import dev.umbra.registry.ModItems;
import dev.umbra.sanity.Sanity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Behaviour of the mod's enchantments and of the artifacts that are pinned to a chestplate. */
public final class EnchantEvents {
    private record Echo(ServerLevel level, UUID target, UUID owner, long due, float damage) {}

    private static final List<Echo> ECHOES = new ArrayList<>();
    private static boolean echoing;

    private EnchantEvents() {}

    /** Forget everything (tests). */
    public static void clear() {
        ECHOES.clear();
        echoing = false;
    }

    public static int pendingEchoes() {
        return ECHOES.size();
    }

    // ---- Echo: a melee blow sounds again a moment later

    public static void onDamage(LivingDamageEvent.Post event) {
        if (echoing) return;
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof ServerPlayer player) || src.getDirectEntity() != player) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        int lvl = Gear.level(level, player.getMainHandItem(), Gear.ECHO);
        if (lvl <= 0 || event.getHealthDamage() <= 0) return;
        ECHOES.add(new Echo(level, event.getEntity().getUUID(), player.getUUID(), level.getGameTime() + 5, event.getHealthDamage() * 0.35F * lvl));
    }

    /** A blow that lands a little later (Echo, Shadow Cut): after the target's moment of invulnerability. */
    public static void later(ServerLevel level, LivingEntity target, Player owner, int ticks, float damage) {
        ECHOES.add(new Echo(level, target.getUUID(), owner.getUUID(), level.getGameTime() + ticks, damage));
    }

    /** Resonance: a melee blow rings on in every monster within three blocks of the one struck (a third as hard). */
    public static void onResonance(LivingDamageEvent.Post event) {
        if (echoing) return;
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof ServerPlayer player) || src.getDirectEntity() != player) return;
        if (!(event.getEntity().level() instanceof ServerLevel level) || event.getHealthDamage() <= 0) return;
        if (Gear.level(level, player.getMainHandItem(), Gear.RESONANCE) <= 0) return;
        LivingEntity hit = event.getEntity();
        echoing = true;
        try {
            for (var m : level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, hit.getBoundingBox().inflate(3.0), m -> m != hit && m.isAlive())) {
                m.hurtServer(level, level.damageSources().playerAttack(player), event.getHealthDamage() / 3.0F);
                level.sendParticles(ParticleTypes.SONIC_BOOM, m.getX(), m.getY() + m.getBbHeight() * 0.5, m.getZ(), 1, 0, 0, 0, 0);
            }
        } finally {
            echoing = false;
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (ECHOES.isEmpty()) return;
        Iterator<Echo> it = ECHOES.iterator();
        List<Echo> due = new ArrayList<>();
        while (it.hasNext()) {
            Echo e = it.next();
            if (e.level.getGameTime() >= e.due) {
                due.add(e);
                it.remove();
            }
        }
        for (Echo e : due) {
            if (!(e.level.getEntity(e.target) instanceof LivingEntity target) || !target.isAlive()) continue;
            Player owner = e.level.getPlayerByUUID(e.owner);
            echoing = true;
            try {
                target.hurtServer(e.level, owner != null ? e.level.damageSources().playerAttack(owner) : e.level.damageSources().magic(), e.damage);
            } finally {
                echoing = false;
            }
            e.level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 6, 0.2, 0.3, 0.2, 0.02);
            e.level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 0.6F);
        }
    }

    // ---- Luminite Aegis: in either hand it halves a creature's blow and sears the one who struck

    public static boolean hasAegis(Player p) {
        return p.getMainHandItem().is(ModItems.LUMINITE_SHIELD.get()) || p.getOffhandItem().is(ModItems.LUMINITE_SHIELD.get());
    }

    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !hasAegis(player)) return;
        if (!(event.getSource().getEntity() instanceof net.minecraft.world.entity.Mob mob)) return;
        event.setAmount(event.getAmount() * 0.5F);
        boolean main = player.getMainHandItem().is(ModItems.LUMINITE_SHIELD.get());
        player.getItemInHand(main ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND)
                .hurtAndBreak(2, player, main ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        mob.igniteForSeconds(3);
        mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 100));
        ((ServerLevel) player.level()).playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 0.8F, 1.3F);
    }

    // ---- Harvest: the dark leaves more behind

    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity dead = event.getEntity();
        if (!Bestiary.ids().containsKey(dead.getType()) || !(dead.level() instanceof ServerLevel level)) return;
        int lvl = Gear.level(level, player.getMainHandItem(), Gear.HARVEST);
        if (lvl <= 0) return;
        var r = level.getRandom();
        if (r.nextFloat() < 0.25F * lvl) event.getDrops().add(new ItemEntity(level, dead.getX(), dead.getY() + 0.5, dead.getZ(), new ItemStack(ModItems.SHADOW_SHARD.get())));
        if (r.nextFloat() < 0.08F * lvl) event.getDrops().add(new ItemEntity(level, dead.getX(), dead.getY() + 0.5, dead.getZ(), new ItemStack(ModItems.ECHO_QUARTZ.get())));
        if (r.nextFloat() < 0.06F * lvl) event.getDrops().add(new ItemEntity(level, dead.getX(), dead.getY() + 0.5, dead.getZ(), new ItemStack(ModItems.LUMINITE_CRYSTAL.get())));
    }

    // ---- Taste of Darkness: kills of the dark feed you, at a price

    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!Bestiary.ids().containsKey(event.getEntity().getType())) return;
        int lvl = Gear.level(player.level(), player.getMainHandItem(), Gear.TASTE);
        if (lvl <= 0) return;
        player.heal(2.0F * lvl);
        if (Sanity.enabled()) Sanity.add(player, -1.0F * lvl);
    }

    // ---- Flash: arrows light up what they hit

    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow)) return;
        if (!(arrow.level() instanceof ServerLevel level) || !(event.getRayTraceResult() instanceof EntityHitResult hit)) return;
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null) return;
        int lvl = Gear.level(level, weapon, Gear.FLASH);
        if (lvl <= 0 || level.getRandom().nextFloat() >= 0.34F * lvl) return;
        if (hit.getEntity() instanceof LightSensitive ls) {
            ls.onLit(level, arrow.getOwner() instanceof Player p ? p : null);
            level.sendParticles(ParticleTypes.END_ROD, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 12, 0.2, 0.2, 0.2, 0.05);
        }
    }

    // ---- the ash clasp: soft landings

    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && Gear.hasArtifact(player, Gear.ASH_CLASP)) event.setDamageMultiplier(0.0F);
    }

    // ---- pinning an artifact to a chestplate at the anvil

    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft(), right = event.getRight();
        String artifact = right.is(ModItems.ASH_CLASP.get()) ? Gear.ASH_CLASP : right.is(ModItems.ARCHITECT_SEAL.get()) ? Gear.ARCHITECT_SEAL : right.is(ModItems.KEEPERS_PEARL.get()) ? Gear.KEEPERS_PEARL : null;
        if (artifact == null || !left.is(ItemTags.CHEST_ARMOR)) return;
        ItemStack out = left.copy();
        out.set(ModComponents.ARTIFACT.get(), artifact);
        event.setOutput(out);
        event.setXpCost(3);
        event.setMaterialCost(1);
    }

    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        String inf = stack.get(ModComponents.INFUSION.get());
        if (inf != null) event.getToolTip().add(Component.translatable("infusion.umbra." + inf).withStyle(ChatFormatting.GOLD));
        else if (dev.umbra.craft.Infusions.relicOf(stack) != null)
            event.getToolTip().add(Component.translatable("infusion.umbra.how").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        String a = stack.get(ModComponents.ARTIFACT.get());
        if (a != null) {
            event.getToolTip().add(Component.translatable("artifact.umbra." + a).withStyle(ChatFormatting.GOLD));
        } else if (stack.is(ModItems.ASH_CLASP.get()) || stack.is(ModItems.ARCHITECT_SEAL.get()) || stack.is(ModItems.KEEPERS_PEARL.get())) {
            String id = stack.is(ModItems.ASH_CLASP.get()) ? Gear.ASH_CLASP : stack.is(ModItems.ARCHITECT_SEAL.get()) ? Gear.ARCHITECT_SEAL : Gear.KEEPERS_PEARL;
            event.getToolTip().add(Component.translatable("artifact.umbra." + id + ".effect").withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.translatable("artifact.umbra.how").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
