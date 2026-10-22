package dev.umbra.altar;

import dev.umbra.Umbra;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 2.9: while the Queen's rite is under way the chests round the altar are hers: they do not open and do not break
 * (or the bait could be taken back at the last moment).
 */
@EventBusSubscriber(modid = Umbra.MODID)
public final class AltarEvents {
    private AltarEvents() {}

    /** Is this a chest held by a rite of mimicry running at an altar near it? */
    public static boolean sealedByRite(BlockGetter level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!s.is(Blocks.CHEST) && !s.is(Blocks.TRAPPED_CHEST)) return false;
        int r = Ritual.CHEST_RADIUS;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -1, -r), pos.offset(r, 1, r)))
            if (level.getBlockEntity(p) instanceof SummoningAltarBlockEntity altar && altar.riteRunning() && altar.ritual() == Ritual.MIMICRY)
                return true;
        return false;
    }

    @SubscribeEvent
    static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !sealedByRite(event.getLevel(), event.getPos())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        event.getLevel().playSound(null, event.getPos(), SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 0.6F);
        event.getEntity().sendOverlayMessage(Component.translatable("message.umbra.chest_sealed").withStyle(ChatFormatting.DARK_PURPLE));
    }

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide() || !sealedByRite(event.getLevel(), event.getPos())) return;
        event.setCanceled(true);
        if (event.getPlayer() != null)
            event.getPlayer().sendOverlayMessage(Component.translatable("message.umbra.chest_sealed").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
