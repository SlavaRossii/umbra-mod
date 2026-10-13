package dev.umbra.item;

import dev.umbra.sanity.Sanity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** Pills that restore sanity instantly. Stronger pills come with side effects. */
public class PillItem extends Item {
    private final float restore;
    private final int cooldown;
    private final List<MobEffectInstance> sideEffects;

    public PillItem(Properties properties, float restore, int cooldownTicks, List<MobEffectInstance> sideEffects) {
        super(properties);
        this.restore = restore;
        this.cooldown = cooldownTicks;
        this.sideEffects = sideEffects;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.PASS;
        if (!Sanity.enabled()) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            Sanity.set(player, Sanity.get(player) + restore);
            for (MobEffectInstance e : sideEffects) player.addEffect(new MobEffectInstance(e));
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6F, 1.4F);
            player.sendOverlayMessage(Component.translatable("message.umbra.pills", Math.round(Sanity.get(player))));
            if (!player.isCreative()) stack.shrink(1);
            player.getCooldowns().addCooldown(stack, cooldown);
        }
        return InteractionResult.SUCCESS;
    }
}
