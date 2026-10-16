package dev.umbra.lighthouse;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Keeper's horn. Use it on a lighthouse lamp to bind it there. When the dark comes for that lighthouse the horn
 * tells you wherever you are; blow it then and it carries you to the foot of the tower.
 */
public class KeeperHornItem extends Item {
    public KeeperHornItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            ItemStack horn = player.getItemInHand(hand);
            if (LighthouseRaid.answer(sp, horn)) return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(dev.umbra.registry.ModComponents.BOUND_LAMP.get()) || super.isFoil(stack);
    }
}
