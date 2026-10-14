package dev.umbra.boss;

import dev.umbra.quantum.QuantumMaze;
import dev.umbra.registry.ModEntities;
import dev.umbra.underside.Underside;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The two items that call the great bosses. Both only work in the Underside. */
public final class BossSummonItems {
    private BossSummonItems() {}

    /** Ключ Изнанки: raises the Architect's maze in front of you, with him inside. */
    public static class Key extends Item {
        public Key(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            if (!Underside.is(server)) {
                player.sendOverlayMessage(Component.translatable("message.umbra.only_underside"));
                return InteractionResult.FAIL;
            }
            // the key pulls towards the sealed door of the nearest Quantum Citadel
            if (dev.umbra.citadel.Citadel.inPocket(player)) {
                player.sendOverlayMessage(Component.translatable("message.umbra.key.inside"));
            } else {
                player.sendOverlayMessage(dev.umbra.citadel.CitadelGateFeature.describeNearest(server, player, "message.umbra.key.pull"));
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** Builds the arena maze ahead of the player and puts the Architect inside. */
    public static Architect summonArchitect(ServerLevel level, Player player) {
        Vec3 dir = UmbraUtil.horizontal(player.getLookAngle());
        double dist = QuantumMaze.SIZE / 2.0 + 4;
        BlockPos center = BlockPos.containing(player.position().add(dir.x * dist, 0, dir.z * dist));
        BlockPos corner = center.offset(-QuantumMaze.SIZE / 2, -1, -QuantumMaze.SIZE / 2);
        QuantumMaze.build(level, corner, level.getRandom());
        int mid = QuantumMaze.SIZE / 2;
        BlockPos spawn = corner.offset(mid, 1, mid + 2);
        Architect boss = ModEntities.ARCHITECT.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (boss == null) return null;
        boss.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0, 0);
        boss.setTarget(player);
        level.addFreshEntity(boss);
        level.playSound(null, center, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, spawn.getX() + 0.5, spawn.getY() + 1, spawn.getZ() + 0.5, 80, 1, 1.5, 1, 0.05);
        return boss;
    }

    /** Сердце бездны: calls the Primordial Shadow. */
    public static class Heart extends Item {
        public Heart(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            if (!Underside.is(server)) {
                player.sendOverlayMessage(Component.translatable("message.umbra.only_underside"));
                return InteractionResult.FAIL;
            }
            var boss = SummonSealItem.summon(server, player, ModEntities.PRIMORDIAL_SHADOW.get());
            if (boss == null) {
                player.sendOverlayMessage(Component.translatable("message.umbra.no_room"));
                return InteractionResult.FAIL;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (!player.isCreative()) stack.shrink(1);
            return InteractionResult.SUCCESS;
        }
    }
}
