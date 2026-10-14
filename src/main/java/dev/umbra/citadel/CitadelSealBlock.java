package dev.umbra.citadel;

import dev.umbra.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The sealed door before the Architect's maze. Only the Underside Key opens it. */
public class CitadelSealBlock extends Block {
    public CitadelSealBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.UNDERSIDE_KEY.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel server && Citadel.openThrone(server, pos, player) != null && !player.isCreative()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.umbra.citadel.sealed").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return InteractionResult.SUCCESS;
    }
}
