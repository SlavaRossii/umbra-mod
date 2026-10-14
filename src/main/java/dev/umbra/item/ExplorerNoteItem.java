package dev.umbra.item;

import dev.umbra.journal.Journal;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * A crumpled page from those who came before. Reading it copies it into your journal
 * (the next page of their story you have not read yet).
 */
public class ExplorerNoteItem extends Item {
    public static final int NOTES = 8;

    public ExplorerNoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        for (int i = 1; i <= NOTES; i++) {
            String id = "note_" + i;
            if (!Journal.has(sp, id)) {
                Journal.unlock(sp, id);
                if (i == NOTES && !Journal.has(sp, "marta_map") && sp.getInventory().countItem(dev.umbra.registry.ModItems.MARTA_MAP.get()) == 0) {
                    // the last note has a map folded into it
                    if (!sp.getInventory().add(new net.minecraft.world.item.ItemStack(dev.umbra.registry.ModItems.MARTA_MAP.get()))) {
                        sp.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(sp.level(), sp.getX(), sp.getY(), sp.getZ(), new net.minecraft.world.item.ItemStack(dev.umbra.registry.ModItems.MARTA_MAP.get())));
                    }
                    sp.sendOverlayMessage(Component.translatable("message.umbra.note_map"));
                }
                if (!player.isCreative()) player.getItemInHand(hand).shrink(1);
                return InteractionResult.SUCCESS;
            }
        }
        sp.sendOverlayMessage(Component.translatable("item.umbra.explorer_note.known"));
        return InteractionResult.SUCCESS;
    }
}
