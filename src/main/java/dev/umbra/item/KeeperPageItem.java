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
 * A page from the Keepers of the Vein, the miners who guarded the luminite long before the expedition came.
 * Reading it copies the next page you have not read into the journal, and with it the craft that page teaches.
 */
public class KeeperPageItem extends Item {
    public static final int PAGES = 7;

    public KeeperPageItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        String next = nextPage(sp);
        if (next == null) {
            sp.sendOverlayMessage(Component.translatable("item.umbra.keeper_page.known"));
            return InteractionResult.SUCCESS;
        }
        Journal.unlock(sp, next);
        if (!player.isCreative()) player.getItemInHand(hand).shrink(1);
        return InteractionResult.SUCCESS;
    }

    /** The first page of the Keepers this player has not read, or null. */
    public static String nextPage(ServerPlayer player) {
        for (int i = 1; i <= PAGES; i++) {
            String id = "keeper_" + i;
            if (!Journal.has(player, id)) return id;
        }
        return null;
    }
}
