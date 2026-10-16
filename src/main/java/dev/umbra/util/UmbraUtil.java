package dev.umbra.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public final class UmbraUtil {
    private UmbraUtil() {}

    /** Night in a dimension that has a day cycle. */
    public static boolean isNight(ServerLevel level) {
        return level.dimensionType().hasSkyLight() && level.isDarkOutside();
    }

    public static int blockLight(Level level, BlockPos pos) {
        return level.getBrightness(LightLayer.BLOCK, pos);
    }

    /** Raw sky light (ignores time of day): 0 means the spot is enclosed from the sky. */
    public static int skyLight(Level level, BlockPos pos) {
        return level.getBrightness(LightLayer.SKY, pos);
    }

    /** Light the player would actually see right now (block light or daylight). */
    public static int effectiveLight(Level level, BlockPos pos) {
        return level.getMaxLocalRawBrightness(pos);
    }

    public static boolean holdsLight(Player player) {
        return isLight(player.getItemInHand(InteractionHand.MAIN_HAND)) || isLight(player.getItemInHand(InteractionHand.OFF_HAND));
    }

    private static boolean isLight(ItemStack stack) {
        if (dev.umbra.light.LanternItem.isLit(stack)) return true;
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem bi && bi.getBlock().defaultBlockState().getLightEmission() >= 10;
    }

    /** Into the inventory, or at the player's feet if it is full. */
    public static void give(Player player, ItemStack stack) {
        if (player.getInventory().add(stack) || stack.isEmpty()) return;
        player.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), stack));
    }

    public static Vec3 horizontal(Vec3 v) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        double len = h.length();
        return len < 1.0E-6 ? Vec3.ZERO : h.scale(1.0 / len);
    }

    /** A sound for this player's ears only. */
    public static void privateSound(net.minecraft.server.level.ServerPlayer player, net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound,
                                    net.minecraft.sounds.SoundSource source, float volume, float pitch) {
        if (player.connection == null) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(sound, source, player.getX(), player.getY(), player.getZ(),
                volume, pitch, player.getRandom().nextLong()));
    }
}
