package dev.umbra.madness;

import dev.umbra.sanity.Sanity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Tincture of silence: for five minutes nothing can take your sanity. It does not give any back. */
public class SilenceTinctureItem extends Item {
    public static final int DURATION = 20 * 60 * 5;

    public SilenceTinctureItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            Sanity.calm(player, DURATION);
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.7F, 0.6F);
            player.sendOverlayMessage(Component.translatable("item.umbra.silence_tincture.drunk"));
            if (!player.isCreative()) {
                stack.shrink(1);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                dev.umbra.util.UmbraUtil.give(player, bottle);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
