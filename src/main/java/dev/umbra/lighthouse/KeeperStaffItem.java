package dev.umbra.lighthouse;

import dev.umbra.gear.WeaponEvents;
import dev.umbra.light.LightSensitive;
import dev.umbra.light.WardBlock;
import dev.umbra.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 2.7: the Keeper's staff, Yan's own, given at the dawn to the one who becomes the keeper after him.
 * <ul>
 *   <li>Use: a beam of the lighthouse's light, {@link #RANGE} blocks, through everything of the dark in its way:
 *       {@link #BEAM} damage, and the flash a Dispeller gives (mimics show, shades scatter). One charge.</li>
 *   <li>Sneak + use: the Keeper's circle, a ward of {@link #CIRCLE_RADIUS} blocks round you for a minute. Twenty charges.</li>
 *   <li>Charges come back at the lamp of the lighthouse (use the staff on it while it burns), or with luminite at
 *       the anvil.</li>
 * </ul>
 */
public class KeeperStaffItem extends Item {
    public static final double RANGE = 24.0;
    public static final float BEAM = 7.0F;
    public static final int BEAM_COOLDOWN = 12;
    public static final int CIRCLE_RADIUS = 10, CIRCLE_TICKS = 20 * 60, CIRCLE_COST = 20, CIRCLE_COOLDOWN = 20 * 30;

    public KeeperStaffItem(Properties properties) {
        super(properties);
    }

    private static int charge(ItemStack stack) {
        return stack.getMaxDamage() - 1 - stack.getDamageValue();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        boolean circle = player.isShiftKeyDown();
        int cost = circle ? CIRCLE_COST : 1;
        if (!player.isCreative() && charge(stack) < cost) {
            player.sendOverlayMessage(Component.translatable("item.umbra.keeper_staff.empty").withStyle(ChatFormatting.GOLD));
            return InteractionResult.FAIL;
        }
        if (circle) {
            WardBlock.circle(server, player.blockPosition(), CIRCLE_RADIUS, CIRCLE_TICKS);
            WardBlock.repel(server, player.blockPosition(), CIRCLE_RADIUS);
            for (int a = 0; a < 48; a++) {
                double r = a * Math.PI * 2 / 48;
                server.sendParticles(ParticleTypes.END_ROD, player.getX() + Math.cos(r) * CIRCLE_RADIUS, player.getY() + 0.2, player.getZ() + Math.sin(r) * CIRCLE_RADIUS, 1, 0, 0.1, 0, 0);
            }
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.2F);
            player.getCooldowns().addCooldown(stack, CIRCLE_COOLDOWN);
        } else {
            beam(server, player);
            player.getCooldowns().addCooldown(stack, BEAM_COOLDOWN);
        }
        if (!player.isCreative()) stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, stack.getDamageValue() + cost));
        return InteractionResult.SUCCESS;
    }

    /** The beam: everything of the dark on the line is hit. Returns how many. */
    public static int beam(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition(), look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(RANGE));
        var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) end = hit.getLocation();
        double len = end.distanceTo(eye);
        for (double d = 1.0; d < len; d += 0.6)
            level.sendParticles(ParticleTypes.END_ROD, eye.x + look.x * d, eye.y + look.y * d - 0.2, eye.z + look.z * d, 1, 0.02, 0.02, 0.02, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.8F);
        List<LivingEntity> struck = new ArrayList<>();
        Vec3 to = end;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, to).inflate(1.0), x -> x != player && x.isAlive())) {
            AABB box = e.getBoundingBox().inflate(0.4);
            if (box.clip(eye, to).isEmpty() && !box.contains(eye)) continue;
            if (!WeaponEvents.dark(e) && !(e instanceof LightSensitive)) continue;
            struck.add(e);
        }
        for (LivingEntity e : struck) {
            e.hurtServer(level, level.damageSources().indirectMagic(player, player), BEAM);
            if (e instanceof LightSensitive ls) ls.onLit(level, player);
            level.sendParticles(ParticleTypes.FLAME, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 6, 0.2, 0.3, 0.2, 0.02);
        }
        return struck.size();
    }

    /** Use it on the burning lamp of the lighthouse: it fills up again. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (!state.is(ModBlocks.LIGHTHOUSE_LAMP.get())) return InteractionResult.PASS;
        if (state.getValue(LighthouseLampBlock.LEVEL) <= 0) {
            if (context.getPlayer() != null && !level.isClientSide()) context.getPlayer().sendOverlayMessage(Component.translatable("item.umbra.keeper_staff.lamp_dead"));
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            context.getItemInHand().setDamageValue(0);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.5F);
            if (context.getPlayer() != null) context.getPlayer().sendOverlayMessage(Component.translatable("item.umbra.keeper_staff.filled").withStyle(ChatFormatting.GOLD));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        // it gives light in the hand, like the lantern it grew out of
        if (entity instanceof Player p && (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) && p.tickCount % 4 == 0)
            dev.umbra.light.LanternItem.placeLight(level, p);
    }

    public static boolean holds(Player p) {
        return p.getMainHandItem().getItem() instanceof KeeperStaffItem || p.getOffhandItem().getItem() instanceof KeeperStaffItem;
    }
}
