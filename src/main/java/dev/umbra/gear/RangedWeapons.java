package dev.umbra.gear;

import dev.umbra.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 2.5: weapons for a distance.
 * <ul>
 *   <li>Luminite bow: a bow (vanilla's own logic), its arrows hit the things of the Underside half as hard again
 *       and make them glow for a while.</li>
 *   <li>Shadow knives: thrown on use (a thrown item that looks like the knife), 5 damage, twice from behind,
 *       half of them can be picked up again where they fell.</li>
 *   <li>Echo staff: a sonic cry along the look, 16 blocks, through every creature in the line, through armour;
 *       one echo quartz a cry, three seconds to breathe.</li>
 * </ul>
 */
public final class RangedWeapons {
    private RangedWeapons() {}

    public static final float BOW_BONUS = 1.5F, KNIFE_DAMAGE = 5.0F, STAFF_DAMAGE = 9.0F;
    public static final int STAFF_RANGE = 16, STAFF_COOLDOWN = 60, KNIFE_COOLDOWN = 8;

    // ------------------------------------------------------------------ the knife

    public static class ShadowKnifeItem extends Item {
        public ShadowKnifeItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level instanceof ServerLevel server) {
                Snowball knife = new Snowball(server, player, stack.copyWithCount(1));
                knife.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.9F, 0.4F);
                server.addFreshEntity(knife);
                server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.6F, 1.6F);
                player.getCooldowns().addCooldown(stack, KNIFE_COOLDOWN);
                if (!player.isCreative()) stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static boolean isKnife(net.minecraft.world.entity.Entity e) {
        return e instanceof Snowball s && s.getItem().is(ModItems.SHADOW_KNIFE.get());
    }

    public static void onImpact(ProjectileImpactEvent event) {
        if (!isKnife(event.getProjectile()) || !(event.getProjectile().level() instanceof ServerLevel level)) return;
        Snowball knife = (Snowball) event.getProjectile();
        Vec3 at = event.getRayTraceResult().getLocation();
        if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target
                && knife.getOwner() instanceof LivingEntity owner && target != owner) {
            float dmg = KNIFE_DAMAGE;
            if (WeaponEvents.behind(target, owner)) {
                dmg *= 2.0F;
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
            }
            target.hurtServer(level, level.damageSources().thrown(knife, owner), dmg);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.7F, 1.4F);
        } else {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 0.7F, 1.3F);
        }
        // half of them can be picked up again (never in creative: it did not cost anything)
        if (!(knife.getOwner() instanceof Player p && p.isCreative()) && level.getRandom().nextBoolean()) {
            level.addFreshEntity(new ItemEntity(level, at.x, at.y + 0.1, at.z, new ItemStack(ModItems.SHADOW_KNIFE.get())));
        }
    }

    // ------------------------------------------------------------------ the bow

    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) return;
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || !weapon.is(ModItems.LUMINITE_BOW.get())) return;
        LivingEntity target = event.getEntity();
        if (!WeaponEvents.dark(target)) return;
        event.setAmount(event.getAmount() * BOW_BONUS);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 5, 0));
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 8, 0.3, 0.4, 0.3, 0.03);
        }
    }

    // ------------------------------------------------------------------ the staff

    public static class EchoStaffItem extends Item {
        public EchoStaffItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            if (!player.isCreative() && player.getInventory().countItem(ModItems.ECHO_QUARTZ.get()) <= 0) {
                player.sendOverlayMessage(Component.translatable("message.umbra.echo_staff.empty").withStyle(ChatFormatting.DARK_AQUA));
                return InteractionResult.FAIL;
            }
            int hits = cry(server, player);
            if (!player.isCreative()) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack s = player.getInventory().getItem(i);
                    if (s.is(ModItems.ECHO_QUARTZ.get())) {
                        s.shrink(1);
                        break;
                    }
                }
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            }
            player.getCooldowns().addCooldown(stack, STAFF_COOLDOWN);
            return InteractionResult.SUCCESS;
        }
    }

    /** The cry: every living thing within a block of the line takes the blow, through armour. Returns how many. */
    public static int cry(ServerLevel level, Player player) {
        Vec3 from = player.getEyePosition();
        Vec3 dir = player.getViewVector(1.0F).normalize();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 1.3F);
        java.util.Set<LivingEntity> struck = new java.util.HashSet<>();
        for (int i = 1; i <= STAFF_RANGE; i++) {
            Vec3 p = from.add(dir.scale(i));
            if (!level.getBlockState(net.minecraft.core.BlockPos.containing(p)).getCollisionShape(level, net.minecraft.core.BlockPos.containing(p)).isEmpty()) break;
            if (i % 2 == 0) level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(1.0), x -> x != player && x.isAlive())) {
                if (struck.add(e)) e.hurtServer(level, level.damageSources().sonicBoom(player), STAFF_DAMAGE);
            }
        }
        return struck.size();
    }
}
