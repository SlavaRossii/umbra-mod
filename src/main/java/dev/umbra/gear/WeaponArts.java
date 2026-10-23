package dev.umbra.gear;

import dev.umbra.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 2.12: each weapon its own hand. Not more numbers, a character: what a weapon does that the others do not, shown by
 * a little light, a sound of its own and a moment that feels good when it lands. Quiet otherwise.
 * <ul>
 *   <li>Gloom sword: three full blows in a row, the third a wide cut in front of you (a violet trail).</li>
 *   <li>Gloom axe: a full swing staggers (slows the one struck a moment), the ground cracks round it.</li>
 *   <li>Despair blade: the lower your mind, the darker it bleeds (and the deeper it cuts, as before).</li>
 *   <li>Luminite spear: a blow on the run is a lunge: it goes through the first one into the one behind it.</li>
 *   <li>Shepherd's scythe: its full sweep draws an arc of souls; whatever it kills gives you half a heart.</li>
 *   <li>Shadow dagger: a kill from behind and you are gone, a burst of speed.</li>
 *   <li>Luminite bow: a fully drawn arrow leaves its light where it lands for a few seconds.</li>
 *   <li>Luminite aegis: raised in the last moment before a blow, it throws the blow back and blinds the one who struck.</li>
 * </ul>
 */
public final class WeaponArts {
    private WeaponArts() {}

    public static final int COMBO = 3, COMBO_WINDOW = 40;
    public static final float SWORD_CUT = 0.5F, SPEAR_PIERCE = 0.7F, SCYTHE_HEAL = 1.0F, PARRY_BACK = 0.5F;
    public static final int AXE_STAGGER = 30, DAGGER_RUSH = 30, ARROW_LIGHT = 80, PARRY_TICKS = 6, PARRY_BLIND = 40;
    private static final int VIOLET = 0x9A5CE0, CRIMSON = 0x8A1020;

    /** Full blows of the gloom sword in a row: [count, game time of the last]. */
    private static final Map<UUID, long[]> combo = new HashMap<>();

    /** A sound by its name (the field types of sounds change between versions; the names stay). Quietly nothing if gone. */
    static void sound(ServerLevel level, Vec3 at, String name, float volume, float pitch) {
        SoundEvent s = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.withDefaultNamespace(name));
        if (s != null) level.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, volume, pitch);
    }

    /** How charged the swing was and whether you ran, as it began (the game resets both before the blow lands). */
    private static final Map<UUID, float[]> swing = new HashMap<>();   // scale, sprinting (0/1), game time

    public static void onAttack(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer p)
            swing.put(p.getUUID(), new float[]{p.getAttackStrengthScale(0.5F), p.isSprinting() ? 1 : 0, p.level().getGameTime() % 100000});
    }

    private static float[] swingNow(ServerPlayer player) {
        float[] s = swing.get(player.getUUID());
        return s != null && (long) s[2] == player.level().getGameTime() % 100000 ? s : null;
    }

    /** Was this a full swing? (the one that has just landed; or, outside an attack, the charge now) */
    public static boolean full(ServerPlayer player) {
        float[] s = swingNow(player);
        return (s != null ? s[0] : player.getAttackStrengthScale(0.5F)) > 0.9F;
    }

    /** Was the one who struck running as the swing began? */
    public static boolean sprinting(ServerPlayer player) {
        float[] s = swingNow(player);
        return s != null ? s[1] > 0 : player.isSprinting();
    }

    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getSource().getDirectEntity() != player) return;
        if (player.level() instanceof ServerLevel level) afterKill(level, player, event.getEntity(), player.getMainHandItem());
    }

    /** What may be cut besides the one struck (not you, not your tamed ones, not the villagers). */
    static boolean fair(LivingEntity e, ServerPlayer player, LivingEntity target) {
        return e != target && e != player && e.isAlive() && e instanceof Mob
                && (e instanceof net.minecraft.world.entity.monster.Enemy || WeaponEvents.dark(e));
    }

    /** A blow has landed: the weapon's own moment. Called by {@link WeaponEvents#onDamage}; {@code full} = a full swing. */
    static void afterHit(ServerLevel level, ServerPlayer player, LivingEntity target, ItemStack hand, float dealt, boolean full, boolean sprinting) {
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
        if (hand.is(ModItems.GLOOM_SWORD.get())) {
            sound(level, at, "entity.player.attack.strong", 0.35F, 0.75F);
            long now = level.getGameTime();
            long[] c = combo.computeIfAbsent(player.getUUID(), k -> new long[2]);
            if (!full || now - c[1] > COMBO_WINDOW) c[0] = 0;
            c[1] = now;
            if (full && ++c[0] >= COMBO) {
                c[0] = 0;
                swordCut(level, player, target, dealt);
            }
        } else if (hand.is(ModItems.GLOOM_AXE.get())) {
            if (full) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, AXE_STAGGER, 2));
                BlockPos under = target.blockPosition().below();
                var state = level.getBlockState(under);
                if (!state.isAir()) {
                    for (int i = 0; i < 16; i++) {
                        double a = i * Math.PI / 8;
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), target.getX() + Math.cos(a) * 1.1, target.getY() + 0.1,
                                target.getZ() + Math.sin(a) * 1.1, 2, 0.05, 0.02, 0.05, 0.1);
                    }
                }
                sound(level, at, "entity.zombie.attack_wooden_door", 0.35F, 0.55F);
            }
        } else if (hand.is(ModItems.DESPAIR_BLADE.get())) {
            float despair = dev.umbra.madness.DespairBladeItem.bonus(player) / dev.umbra.madness.DespairBladeItem.MAX_BONUS;
            if (despair > 0.4F) {
                level.sendParticles(new DustParticleOptions(CRIMSON, 1.2F), at.x, at.y, at.z, 4 + (int) (despair * 10), 0.3, 0.35, 0.3, 0.0);
                sound(level, at, "entity.warden.heartbeat", 0.25F + despair * 0.4F, 1.4F - despair * 0.4F);
            }
        } else if (hand.is(ModItems.LUMINITE_SPEAR.get())) {
            sound(level, at, "block.amethyst_block.hit", 0.5F, 1.3F);
            if (sprinting && full) spearLunge(level, player, target, dealt);
        } else if (hand.is(ModItems.SHEPHERD_SCYTHE.get())) {
            if (full) soulArc(level, player);
        } else if (hand.is(ModItems.SHADOW_DAGGER.get())) {
            sound(level, at, "entity.player.attack.crit", 0.3F, 1.7F);
        }
    }

    /** The third blow: a wide cut in front, half as hard, on everything in reach of it. Returns how many it cut. */
    public static int swordCut(ServerLevel level, ServerPlayer player, LivingEntity target, float dealt) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 c = player.position().add(look.scale(1.6)).add(0, 1.0, 0);
        int n = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(2.2, 1.2, 2.2), e -> fair(e, player, target))) {
            if (e.hurtServer(level, level.damageSources().playerAttack(player), Math.max(1.0F, dealt * SWORD_CUT))) n++;
        }
        // the trail: a violet half-moon at chest height
        double yaw = Math.atan2(look.z, look.x);
        for (int i = -6; i <= 6; i++) {
            double a = yaw + i * Math.PI / 14;
            level.sendParticles(new DustParticleOptions(VIOLET, 1.0F), player.getX() + Math.cos(a) * 2.0, player.getY() + 1.1 + i * 0.02, player.getZ() + Math.sin(a) * 2.0,
                    1, 0.02, 0.02, 0.02, 0.0);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        sound(level, c, "entity.player.attack.sweep", 0.9F, 0.75F);
        return n;
    }

    /** A blow on the run goes on through the first one into whoever stands behind it, in a line of light. Returns how many. */
    public static int spearLunge(ServerLevel level, ServerPlayer player, LivingEntity target, float dealt) {
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 from = target.position().add(0, target.getBbHeight() * 0.5, 0);
        int n = 0;
        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
        for (double d = 0.5; d <= 3.0; d += 0.5) {
            Vec3 p = from.add(dir.scale(d));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(p, p).inflate(0.6), e -> fair(e, player, target))) {
                if (hit.add(e) && e.hurtServer(level, level.damageSources().playerAttack(player), Math.max(1.0F, dealt * SPEAR_PIERCE))) n++;
            }
        }
        sound(level, from, "item.trident.riptide_1", 0.5F, 1.5F);
        return n;
    }

    static void soulArc(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        double yaw = Math.atan2(look.z, look.x);
        for (int i = -7; i <= 7; i++) {
            double a = yaw + i * Math.PI / 12;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX() + Math.cos(a) * 2.6, player.getY() + 0.9, player.getZ() + Math.sin(a) * 2.6, 1, 0.03, 0.05, 0.03, 0.0);
        }
    }

    /** Something has died by your hand. */
    public static void afterKill(ServerLevel level, ServerPlayer player, LivingEntity dead, ItemStack hand) {
        if (hand.is(ModItems.SHEPHERD_SCYTHE.get())) {
            // a soul goes from it to you
            Vec3 a = dead.position().add(0, dead.getBbHeight() * 0.5, 0), b = player.position().add(0, 1.0, 0);
            for (int i = 0; i <= 6; i++) {
                Vec3 p = a.lerp(b, i / 6.0);
                level.sendParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
            player.heal(SCYTHE_HEAL);
            sound(level, b, "particle.soul_escape", 0.8F, 0.9F);
        } else if (hand.is(ModItems.SHADOW_DAGGER.get()) && WeaponEvents.behind(dead, player)) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, DAGGER_RUSH, 1, false, false, true));
            level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 0.8, player.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
            sound(level, player.position(), "entity.phantom.flap", 0.5F, 1.6F);
        }
    }

    // ---- the aegis: a blow met in the last moment goes back where it came from

    /** Has the aegis just been raised (in time to turn a blow)? */
    public static boolean parrying(ServerPlayer player) {
        return player.isUsingItem() && player.getUseItem().is(ModItems.LUMINITE_SHIELD.get()) && player.getTicksUsingItem() <= PARRY_TICKS + 5;
    }

    /** Called when a blow on {@code player} comes from {@code attacker}; returns whether it was turned. */
    public static boolean parry(ServerLevel level, ServerPlayer player, LivingEntity attacker, float amount) {
        if (!parrying(player) || attacker == player || attacker.distanceTo(player) > 6.0F) return false;
        attacker.hurtServer(level, level.damageSources().thorns(player), Math.max(1.0F, amount * PARRY_BACK));
        attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, PARRY_BLIND, 0));
        Vec3 push = attacker.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(0.6);
        attacker.push(push.x, 0.2, push.z);
        attacker.hurtMarked = true;
        Vec3 at = player.position().add(player.getLookAngle().scale(0.8)).add(0, 1.2, 0);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 10, 0.3, 0.3, 0.3, 0.3);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.15);
        sound(level, at, "block.bell.use", 0.7F, 1.8F);
        return true;
    }

    // ---- the bow: a fully drawn arrow leaves its light where it lands

    private static final List<Object[]> lights = new ArrayList<>(); // level, pos, until

    public static boolean leaveLight(ServerLevel level, Vec3 where) {
        BlockPos p = BlockPos.containing(where);
        for (BlockPos q : new BlockPos[]{p, p.above(), p.relative(net.minecraft.core.Direction.UP, 2)}) {
            if (level.getBlockState(q).isAir()) {
                level.setBlock(q, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12), 3);
                lights.add(new Object[]{level, q, level.getGameTime() + ARROW_LIGHT});
                level.sendParticles(ParticleTypes.END_ROD, where.x, where.y, where.z, 10, 0.2, 0.2, 0.2, 0.05);
                return true;
            }
        }
        return false;
    }

    public static void onImpact(net.neoforged.neoforge.event.entity.ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow) || !(arrow.level() instanceof ServerLevel level)) return;
        var weapon = arrow.getWeaponItem();
        if (weapon == null || !weapon.is(ModItems.LUMINITE_BOW.get()) || !arrow.isCritArrow()) return;
        leaveLight(level, event.getRayTraceResult().getLocation());
    }

    public static void levelTick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || lights.isEmpty()) return;
        long now = level.getGameTime();
        lights.removeIf(o -> {
            if (o[0] != level || (long) o[2] > now) return false;
            BlockPos q = (BlockPos) o[1];
            if (level.getBlockState(q).is(Blocks.LIGHT)) level.removeBlock(q, false);
            return true;
        });
    }

    /** For the tests: how many arrow lights are still burning. */
    public static int lightsLeft() {
        return lights.size();
    }
}
