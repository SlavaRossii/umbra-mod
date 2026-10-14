package dev.umbra.madness;

import dev.umbra.journal.Journal;
import dev.umbra.sanity.Sanity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Whisper Table. Use it with an empty hand to hear what you can make; use it holding the base item of a
 * ritual to make it. The whispers only form words while your sanity is low enough.
 */
public class WhisperTableBlock extends Block {
    public WhisperTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) listen(sp);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        Ritual ritual = null;
        for (Ritual r : Rituals.ALL) if (stack.is(r.base().get())) ritual = r;
        if (ritual == null) {
            listen(sp);
            return InteractionResult.SUCCESS;
        }
        perform(server, sp, pos, hand, ritual);
        return InteractionResult.SUCCESS;
    }

    /** Lists what the player knows and whether the whispers are loud enough right now. */
    public static void listen(ServerPlayer player) {
        boolean any = false;
        for (Ritual r : Rituals.ALL) {
            if (!Journal.has(player, r.insight())) continue;
            any = true;
            Component need = Component.translatable("ritual.umbra." + r.id() + ".need");
            player.sendSystemMessage(Component.translatable("ritual.umbra.line",
                    Component.translatable(r.result().get().getDescriptionId()), Math.round(r.maxSanity()), need).withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (!any) player.sendSystemMessage(Component.translatable("ritual.umbra.nothing").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    /** Returns null on success, or the lang key of the reason it failed. */
    public static String check(ServerPlayer player, Ritual r) {
        if (!Journal.has(player, r.insight())) return "ritual.umbra.unknown";
        if (Sanity.enabled() && Sanity.get(player) > r.maxSanity() && !player.isCreative()) return "ritual.umbra.too_sane";
        Inventory inv = player.getInventory();
        for (Ritual.Ingredient i : r.extras()) {
            if (player.isCreative()) break;
            if (inv.countItem(i.item().get()) < i.count()) return "ritual.umbra.missing";
        }
        return null;
    }

    public static boolean perform(ServerLevel level, ServerPlayer player, BlockPos pos, InteractionHand hand, Ritual r) {
        String fail = check(player, r);
        if (fail != null) {
            player.sendOverlayMessage(Component.translatable(fail).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            level.playSound(null, pos, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 0.6F, 0.6F);
            return false;
        }
        if (!player.isCreative()) {
            for (Ritual.Ingredient i : r.extras()) take(player.getInventory(), i);
            player.getItemInHand(hand).shrink(1);
        }
        ItemStack out = new ItemStack(r.result().get());
        dev.umbra.util.UmbraUtil.give(player, out);
        level.playSound(null, pos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 0.5F, 1.6F);
        level.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.02);
        Sanity.add(player, -3.0F);
        return true;
    }

    private static void take(Inventory inv, Ritual.Ingredient ingredient) {
        int left = ingredient.count();
        for (int slot = 0; slot < inv.getContainerSize() && left > 0; slot++) {
            ItemStack s = inv.getItem(slot);
            if (!s.is(ingredient.item().get())) continue;
            int n = Math.min(left, s.getCount());
            s.shrink(n);
            left -= n;
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SCULK_SOUL, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.05, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.01, 0);
        }
    }
}
