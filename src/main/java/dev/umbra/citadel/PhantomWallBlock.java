package dev.umbra.citadel;

import dev.umbra.registry.ModItems;
import dev.umbra.sanity.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Looks exactly like a maze wall. To a slipping mind (sanity below {@link #BELOW}, or wearing the madman's mask)
 * it is not there at all, and up close it whispers.
 */
public class PhantomWallBlock extends Block {
    public static final float BELOW = 30.0F;

    public PhantomWallBlock(Properties properties) {
        super(properties);
    }

    public static boolean seesThrough(Player player) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MADMAN_MASK.get())) return true;
        if (dev.umbra.gear.Gear.hasArtifact(player, dev.umbra.gear.Gear.ARCHITECT_SEAL)) return true;
        return Sanity.enabled() && Sanity.get(player) < BELOW;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext ec && ec.getEntity() instanceof Player p && seesThrough(p)) return Shapes.empty();
        return Shapes.block();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Player p = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8.0, false);
        if (p != null && seesThrough(p) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SCULK_SOUL, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0.01, 0);
        }
    }
}
