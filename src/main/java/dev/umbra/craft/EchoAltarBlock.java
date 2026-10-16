package dev.umbra.craft;

import dev.umbra.gear.Gear;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * The echo altar: Umbra's own enchanting. Bring a piece of gear, pay in echo quartz and experience, and the altar
 * gives it one of Umbra's enchantments that fits. Luminite candles around it (within 3 blocks) make the echo deeper:
 * 4 candles for level II, 8 for level III. The echo sometimes answers with a whisper: one time in six it also leaves
 * Whisper's Curse (with 12 candles burning, never).
 */
public class EchoAltarBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(3, 4, 3, 13, 10, 13), Block.box(1, 10, 1, 15, 13, 15));
    public static final int CURSE_ONE_IN = 6;
    public static final int SAFE_CANDLES = 12;

    public EchoAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Burning luminite candles within 3 blocks. */
    public static int candles(Level level, BlockPos pos) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 2, 3))) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.LUMINITE_CANDLE.get())) n += s.getValue(LuminiteCandleBlock.CANDLES);
        }
        return n;
    }

    public static int power(int candles) {
        return 1 + Math.min(2, candles / 4);
    }

    /** Echo quartz the altar asks for an enchantment of {@code level}. */
    public static int quartzCost(int level) {
        return 1 + level;
    }

    /** Experience levels it asks. */
    public static int xpCost(int level) {
        return 3 * level + 2;
    }

    /** Umbra's enchantments that could go on {@code stack} at {@code level} (new ones, or ones it holds lower). */
    public static List<Holder<Enchantment>> candidates(Level level, ItemStack stack, int power) {
        List<Holder<Enchantment>> out = new ArrayList<>();
        var reg = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var existing = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (ResourceKey<Enchantment> key : Gear.ALL_ENCHANTS) {
            if (key.equals(Gear.WHISPER_CURSE)) continue;
            var h = reg.get(key);
            if (h.isEmpty()) continue;
            Holder<Enchantment> holder = h.get();
            if (!holder.value().canEnchant(stack)) continue;
            int have = existing.getLevel(holder);
            int want = Math.min(power, holder.value().getMaxLevel());
            if (have > 0) {
                if (have < want) out.add(holder);
            } else if (EnchantmentHelper.isEnchantmentCompatible(existing.keySet(), holder)) {
                out.add(holder);
            }
        }
        return out;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            int c = candles(level, pos), p = power(c);
            player.sendOverlayMessage(Component.translatable("block.umbra.echo_altar.status", c, p, quartzCost(p), xpCost(p)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || stack.is(ModItems.ECHO_QUARTZ.get()) || stack.is(ModItems.LUMINITE_CANDLE.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        int c = candles(level, pos);
        int power = power(c);
        List<Holder<Enchantment>> options = candidates(level, stack, power);
        if (options.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("block.umbra.echo_altar.nothing").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        int quartz = quartzCost(power), xp = xpCost(power);
        if (!player.isCreative()) {
            if (player.experienceLevel < xp || player.getInventory().countItem(ModItems.ECHO_QUARTZ.get()) < quartz) {
                player.sendOverlayMessage(Component.translatable("block.umbra.echo_altar.price", quartz, xp).withStyle(ChatFormatting.DARK_PURPLE));
                return InteractionResult.FAIL;
            }
            take(player, quartz);
            player.giveExperienceLevels(-xp);
        }
        RandomSource random = server.getRandom();
        Holder<Enchantment> pick = options.get(random.nextInt(options.size()));
        stack.enchant(pick, Math.min(power, pick.value().getMaxLevel()));
        boolean cursed = false;
        if (c < SAFE_CANDLES && random.nextInt(CURSE_ONE_IN) == 0) {
            var curse = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Gear.WHISPER_CURSE);
            if (curse.isPresent() && curse.get().value().canEnchant(stack) && EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(curse.get()) == 0) {
                stack.enchant(curse.get(), 1);
                cursed = true;
            }
        }
        server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 0.6F + 0.2F * power);
        server.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.7F);
        server.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 14, 0.3, 0.3, 0.3, 0.02);
        player.sendOverlayMessage(Component.translatable(cursed ? "block.umbra.echo_altar.cursed" : "block.umbra.echo_altar.done",
                pick.value().description()).withStyle(cursed ? ChatFormatting.DARK_RED : ChatFormatting.LIGHT_PURPLE));
        return InteractionResult.SUCCESS;
    }

    private static void take(Player player, int count) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && count > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.ECHO_QUARTZ.get())) {
                int n = Math.min(count, s.getCount());
                s.shrink(n);
                count -= n;
            }
        }
    }
}
