package dev.umbra.boss;

import dev.umbra.sanity.Hallucinations;
import dev.umbra.util.UmbraUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/** A seal that calls a boss a few blocks in front of you. Consumed on use. */
public class SummonSealItem extends Item {
    private final Supplier<? extends EntityType<? extends Mob>> boss;

    public SummonSealItem(Properties properties, Supplier<? extends EntityType<? extends Mob>> boss) {
        super(properties);
        this.boss = boss;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        ItemStack stack = player.getItemInHand(hand);
        Mob mob = summon(server, player, boss.get());
        if (mob == null) {
            player.sendOverlayMessage(Component.translatable("message.umbra.no_room"));
            return InteractionResult.FAIL;
        }
        if (!player.isCreative()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    /** Spawns {@code type} on solid ground about six blocks ahead of the player. */
    public static <T extends Mob> T summon(ServerLevel level, Player player, EntityType<T> type) {
        Vec3 ahead = player.position().add(UmbraUtil.horizontal(player.getLookAngle()).scale(6.0));
        BlockPos spot = Hallucinations.findStandable(level, BlockPos.containing(ahead), 6);
        if (spot == null) return null;
        T mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (mob == null) return null;
        mob.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getYRot() + 180.0F, 0.0F);
        mob.setTarget(player);
        level.addFreshEntity(mob);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, spot.getX() + 0.5, spot.getY() + 1, spot.getZ() + 0.5, 60, 0.6, 1.2, 0.6, 0.02);
        level.playSound(null, spot, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.0F, 0.8F);
        return mob;
    }
}
