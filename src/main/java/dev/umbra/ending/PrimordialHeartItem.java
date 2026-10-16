package dev.umbra.ending;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The heart of the Primordial Shadow: the choice of the ending. Burn it in a lighthouse lamp at full strength
 * (use it on the lamp) for the dawn; take it into yourself somewhere dark (use it in the air) for the darkness.
 */
public class PrimordialHeartItem extends Item {
    public PrimordialHeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (Endings.chosen(sp)) {
            sp.sendOverlayMessage(Component.translatable("item.umbra.primordial_heart.chosen").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        if (!Endings.darkEnough(sp)) {
            sp.sendOverlayMessage(Component.translatable("item.umbra.primordial_heart.too_bright").withStyle(ChatFormatting.DARK_PURPLE));
            return InteractionResult.FAIL;
        }
        if (Endings.darkness(sp)) {
            if (!sp.isCreative()) player.getItemInHand(hand).shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}
