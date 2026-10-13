package dev.umbra.light;

import dev.umbra.mimic.MimicBlock;
import dev.umbra.registry.ModBlocks;
import dev.umbra.sanity.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Dispeller: a luminite lantern that releases a cone of light.
 * Burns away living darkness, dispels shadows and hallucinations, makes mimics flinch.
 * Uses one charge per flash; recharge it on an anvil with luminite.
 */
public class DispellerItem extends Item {
    public static final double RANGE = 14.0;
    /** cos(35°): half-angle of the cone */
    public static final double CONE_COS = 0.82;
    private static final int COOLDOWN = 30;

    public DispellerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            flash(server, player, player.getEyePosition(), player.getLookAngle());
            stack.hurtAndBreak(1, player, hand);
            player.getCooldowns().addCooldown(stack, COOLDOWN);
            Sanity.add(player, Sanity.Shock.DISPELLER);
        }
        return InteractionResult.SUCCESS;
    }

    /** The flash itself; usable from tests and future light weapons. Returns how many darkness blocks burned. */
    public static int flash(ServerLevel level, Player source, Vec3 origin, Vec3 direction) {
        Vec3 dir = direction.normalize();
        int burned = 0;
        int r = (int) Math.ceil(RANGE);
        BlockPos center = BlockPos.containing(origin);
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            Vec3 to = Vec3.atCenterOf(p).subtract(origin);
            double len = to.length();
            if (len > RANGE || len < 0.5) continue;
            if (dir.dot(to.scale(1.0 / len)) < CONE_COS) continue;
            var state = level.getBlockState(p);
            if (state.is(ModBlocks.LIVING_DARKNESS.get())) {
                level.removeBlock(p, false);
                burned++;
                if (burned % 4 == 0) level.sendParticles(ParticleTypes.SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
            } else if (state.getBlock() instanceof MimicBlock && len < 10) {
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.0);
                level.playSound(null, p, SoundEvents.SILVERFISH_DEATH, SoundSource.BLOCKS, 0.5F, 1.8F);
            }
        }

        AABB area = new AABB(origin, origin).inflate(RANGE);
        for (Entity e : level.getEntities((Entity) null, area, e -> e instanceof LightSensitive)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(origin);
            double len = to.length();
            if (len > RANGE || (len > 1.0 && dir.dot(to.scale(1.0 / len)) < CONE_COS)) continue;
            if (source != null && !source.hasLineOfSight(e)) continue;
            ((LightSensitive) e).onLit(level, source);
        }

        for (int i = 1; i <= 10; i++) {
            Vec3 p = origin.add(dir.scale(i * 1.3));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3, 0.15 * i, 0.15 * i, 0.15 * i, 0.01);
        }
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.6F);
        return burned;
    }
}
