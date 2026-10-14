package dev.umbra.madness;

import dev.umbra.mimic.MimicBlock;
import dev.umbra.mimic.MimicChests;
import dev.umbra.sanity.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Madman's mask (worn on the head): invisible things glow, mimics breathe visibly.
 * It sees with your madness: while worn, sanity slowly drains.
 */
public class MadmanMaskItem extends Item {
    public static final int SEE = 20;
    public static final float DRAIN = 0.15F;

    public MadmanMaskItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer player) || slot != EquipmentSlot.HEAD) return;
        if (player.tickCount % 20 != 3) return;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SEE), e -> e.isInvisible() && e != player)) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
        }
        for (var v : level.getEntitiesOfClass(dev.umbra.village.ShadowVillager.class, player.getBoundingBox().inflate(SEE), dev.umbra.village.ShadowVillager::isMimic)) {
            v.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
        }
        BlockPos c = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-8, -4, -8), c.offset(8, 4, 8))) {
            var state = level.getBlockState(p);
            if (state.getBlock() instanceof MimicBlock || (state.hasBlockEntity() && MimicChests.isMimic(level, p))) {
                level.sendParticles(player, ParticleTypes.SCULK_SOUL, true, true, p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.0);
            }
        }
        if (!player.isCreative()) Sanity.add(player, -DRAIN);
    }
}
