package dev.umbra.light;

import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Luminite lantern: hold it (either hand) and it lights the way, slowly burning its charge.
 * Use: switch on/off. Sneak + use: feed it a luminite crystal from your inventory.
 * Charge is the durability bar; an empty lantern does not break, it just goes dark.
 */
public class LanternItem extends Item {
    /** Seconds of light on a full charge (20 minutes). */
    public static final int CAPACITY = 1200;
    /** Seconds of light one crystal gives back. */
    public static final int PER_CRYSTAL = 400;

    public LanternItem(Properties properties) {
        super(properties);
    }

    public static boolean isLit(ItemStack stack) {
        return stack.getItem() instanceof LanternItem && Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
                && stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    public static boolean carriesLit(Player player) {
        return isLit(player.getMainHandItem()) || isLit(player.getOffhandItem());
    }

    public static int charge(ItemStack stack) {
        return stack.getMaxDamage() - 1 - stack.getDamageValue();
    }

    private static void setLit(ItemStack stack, boolean lit) {
        if (lit) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        else stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            if (stack.getDamageValue() == 0) {
                player.sendOverlayMessage(Component.translatable("item.umbra.luminite_lantern.full"));
                return InteractionResult.FAIL;
            }
            if (!consumeCrystal(player)) {
                player.sendOverlayMessage(Component.translatable("item.umbra.luminite_lantern.no_crystal"));
                return InteractionResult.FAIL;
            }
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - PER_CRYSTAL));
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
            return InteractionResult.SUCCESS;
        }
        if (charge(stack) <= 0) {
            player.sendOverlayMessage(Component.translatable("item.umbra.luminite_lantern.empty"));
            setLit(stack, false);
            return InteractionResult.FAIL;
        }
        boolean lit = !isLit(stack);
        setLit(stack, lit);
        level.playSound(null, player.blockPosition(), lit ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.CANDLE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.3F);
        return InteractionResult.SUCCESS;
    }

    private static boolean consumeCrystal(Player player) {
        if (player.isCreative()) return true;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.LUMINITE_CRYSTAL.get())) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player) || !isLit(stack)) return;
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) return;
        // the lantern in the main hand does the work if both hands hold one
        if (slot == EquipmentSlot.OFFHAND && isLit(player.getMainHandItem())) return;
        if (player.tickCount % 20 == 0 && !player.isCreative()) {
            stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, stack.getDamageValue() + 1));
            if (charge(stack) <= 0) {
                setLit(stack, false);
                level.playSound(null, player.blockPosition(), SoundEvents.CANDLE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.8F);
                return;
            }
        }
        if (player.tickCount % 4 == 0) placeLight(level, player);
    }

    /** Puts the invisible light at head height (or at the feet when the head is in something). */
    public static void placeLight(ServerLevel level, Player player) {
        BlockPos head = BlockPos.containing(player.getEyePosition());
        for (BlockPos p : new BlockPos[]{head, player.blockPosition()}) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.HELD_LIGHT.get())) return;
            if (s.isAir()) {
                level.setBlock(p, ModBlocks.HELD_LIGHT.get().defaultBlockState(), Block.UPDATE_ALL);
                return;
            }
            if (s.is(net.minecraft.world.level.block.Blocks.WATER) && level.getFluidState(p).isSource()) {
                level.setBlock(p, ModBlocks.HELD_LIGHT.get().defaultBlockState().setValue(dev.umbra.light.HeldLightBlock.WATERLOGGED, true), Block.UPDATE_ALL);
                return;
            }
        }
    }
}
