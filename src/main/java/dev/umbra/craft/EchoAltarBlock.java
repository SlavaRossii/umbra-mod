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
 * gives it one of Umbra's enchantments that fits. Luminite candles of any kind around it (within 4 blocks) make the
 * echo deeper: 4 candles for II, 12 for III, 24 for IV, and 64 for the echo of the abyss, which lifts an enchantment
 * one level past its limit and may add Resonance. The echo sometimes answers with a whisper: one time in six it
 * also leaves Whisper's Curse (with 12 candles burning, never).
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
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RADIUS, -1, -RADIUS), pos.offset(RADIUS, 3, RADIUS))) {
            BlockState s = level.getBlockState(p);
            n += LuminiteCandleBlock.worth(s);
        }
        return n;
    }

    public static final int RADIUS = 4;
    /** Candles for each level of the echo: II, III, IV, and the abyss (V). */
    public static final int[] STEPS = {4, 12, 24, 64};
    public static final int ABYSS = 5;

    public static int power(int candles) {
        int p = 1;
        for (int s : STEPS) if (candles >= s) p++;
        return p;
    }

    /** Candles still missing for the next level of the echo (0 at the abyss). */
    public static int next(int candles) {
        for (int st : STEPS) if (candles < st) return st - candles;
        return 0;
    }

    /** Echo quartz the altar asks at this power. */
    public static int quartzCost(int power) {
        return power >= ABYSS ? 8 : 1 + power;
    }

    /** Experience it asks, said in levels: as much experience as it takes to climb from nothing to that level. */
    public static int xpCost(int power) {
        return power >= ABYSS ? 30 : 3 * power + 2;
    }

    /** Experience points it takes to climb from level 0 to {@code level}, as in vanilla. */
    public static int pointsFor(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    /** Points of experience the altar takes: the price in levels, counted from zero. */
    public static int xpPoints(int power) {
        return pointsFor(xpCost(power));
    }

    /** All the experience a player carries, in points. */
    public static int pointsOf(Player player) {
        return pointsFor(player.experienceLevel) + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    /** Takes points, not levels: from a high level the same price costs far fewer levels, as in vanilla. */
    public static void takePoints(Player player, int points) {
        int left = Math.max(0, pointsOf(player) - points);
        int level = 0;
        while (pointsFor(level + 1) <= left) level++;
        player.experienceLevel = level;
        player.experienceProgress = 0.0F;
        player.experienceProgress = (left - pointsFor(level)) / (float) player.getXpNeededForNextLevel();
        player.totalExperience = Math.max(0, player.totalExperience - points);
        player.giveExperienceLevels(0);     // let the client hear of it
    }

    /** The level an enchantment gets at this power: up to its limit, and one past it from the abyss. */
    public static int levelFor(Holder<Enchantment> holder, int power) {
        int max = holder.value().getMaxLevel();
        return power >= ABYSS ? max + 1 : Math.min(power, max);
    }

    /** Umbra's enchantments that could go on {@code stack} at {@code level} (new ones, or ones it holds lower). */
    public static List<Holder<Enchantment>> candidates(Level level, ItemStack stack, int power) {
        List<Holder<Enchantment>> out = new ArrayList<>();
        var reg = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var existing = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (ResourceKey<Enchantment> key : Gear.ALL_ENCHANTS) {
            if (key.equals(Gear.WHISPER_CURSE) || key.equals(Gear.RESONANCE)) continue;
            var h = reg.get(key);
            if (h.isEmpty()) continue;
            Holder<Enchantment> holder = h.get();
            if (!holder.value().canEnchant(stack)) continue;
            int have = existing.getLevel(holder);
            int want = levelFor(holder, power);
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
            player.sendOverlayMessage(Component.translatable("block.umbra.echo_altar.status", c, p, quartzCost(p), xpPoints(p), next(c)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || stack.is(ModItems.ECHO_QUARTZ.get()) || (stack.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof LuminiteCandleBlock)) return InteractionResult.TRY_WITH_EMPTY_HAND;
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
            if (pointsOf(player) < xpPoints(power) || player.getInventory().countItem(ModItems.ECHO_QUARTZ.get()) < quartz) {
                player.sendOverlayMessage(Component.translatable("block.umbra.echo_altar.price", quartz, xpPoints(power)).withStyle(ChatFormatting.DARK_PURPLE));
                return InteractionResult.FAIL;
            }
            take(player, quartz);
            takePoints(player, xpPoints(power));
        }
        RandomSource random = server.getRandom();
        Holder<Enchantment> pick = options.get(random.nextInt(options.size()));
        stack.enchant(pick, levelFor(pick, power));
        // the echo of the abyss: sometimes the blow learns to ring on
        if (power >= ABYSS && random.nextInt(4) == 0) {
            var res = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Gear.RESONANCE);
            if (res.isPresent() && res.get().value().canEnchant(stack) && EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(res.get()) == 0) {
                stack.enchant(res.get(), 1);
                player.sendSystemMessage(Component.translatable("block.umbra.echo_altar.resonance").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
            }
        }
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
