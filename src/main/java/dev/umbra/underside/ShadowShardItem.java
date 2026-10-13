package dev.umbra.underside;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** A shard of a dispelled shadow. Used on a dusk stone frame it tears open a rift to the Underside. */
public class ShadowShardItem extends Item {
    public ShadowShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        if (!RiftFrame.isFrame(level.getBlockState(clicked))) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos inside = clicked.relative(ctx.getClickedFace());
        if (RiftFrame.tryOpen(level, inside)) {
            level.playSound(null, clicked, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.7F, 0.6F);
            Player player = ctx.getPlayer();
            if (player != null && !player.isCreative()) ctx.getItemInHand().shrink(1);
            return InteractionResult.SUCCESS;
        }
        Player player = ctx.getPlayer();
        if (player != null) player.sendOverlayMessage(Component.translatable("message.umbra.bad_frame"));
        return InteractionResult.FAIL;
    }
}
