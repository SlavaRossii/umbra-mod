package dev.umbra.item;

import dev.umbra.mimic.MimicBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Hold it, and mimics nearby show their true colours (to you only). */
public class MirrorItem extends Item {
    public static final int RADIUS = 10;

    public MirrorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) return;
        if (player.tickCount % 20 != 0) return;
        BlockPos c = player.blockPosition();
        for (var v : level.getEntitiesOfClass(dev.umbra.village.ShadowVillager.class, player.getBoundingBox().inflate(RADIUS), dev.umbra.village.ShadowVillager::isMimic)) {
            level.sendParticles(player, ParticleTypes.WITCH, true, true, v.getX(), v.getY() + 1.0, v.getZ(), 8, 0.3, 0.6, 0.3, 0.0);
        }
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-RADIUS, -RADIUS / 2, -RADIUS), c.offset(RADIUS, RADIUS / 2, RADIUS))) {
            var state = level.getBlockState(p);
            if (state.getBlock() instanceof MimicBlock
                    || (state.hasBlockEntity() && (dev.umbra.mimic.MimicChests.isMimic(level, p) || dev.umbra.mimic.MimicChests.decide(level, p.immutable())))) {
                level.sendParticles(player, ParticleTypes.WITCH, true, true, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.35, 0.35, 0.35, 0.0);
            }
        }
    }
}
