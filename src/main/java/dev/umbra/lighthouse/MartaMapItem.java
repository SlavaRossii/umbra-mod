package dev.umbra.lighthouse;

import dev.umbra.journal.Journal;
import dev.umbra.underside.Underside;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Marta's map: in her hand, with ash for ink. In the Underside it points to the lighthouse. */
public class MartaMapItem extends Item {
    public MartaMapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        Journal.unlock(sp, "marta_map");
        if (Underside.is(server)) {
            sp.sendOverlayMessage(LighthouseFeature.describeNearest(server, sp, "message.umbra.marta_map"));
        } else {
            sp.sendOverlayMessage(Component.translatable("message.umbra.marta_map_blank"));
        }
        sp.getCooldowns().addCooldown(sp.getItemInHand(hand), 20);
        return InteractionResult.SUCCESS;
    }
}
