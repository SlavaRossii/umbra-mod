package dev.umbra.citadel;

import dev.umbra.registry.ModEntities;
import dev.umbra.sanity.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;

/**
 * A floor tile that looks like the rest of the floor, only a little more cracked. Step on it and something
 * happens, once: an ambush from the ceiling, a sudden darkness, a bell that wakes the whole wing, spikes from
 * the floor (2.5) or a volley of arrows out of the walls (2.5).
 */
public class TrapPlateBlock extends Block {
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    public static final int KINDS = 5;

    public TrapPlateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TRIGGERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRIGGERED);
    }

    public static int kind(BlockPos pos) {
        return Math.floorMod(pos.getX() * 7 + pos.getZ() * 13 + pos.getY(), KINDS);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server && entity instanceof Player p && !p.isSpectator() && !state.getValue(TRIGGERED)) {
            trigger(server, pos, p);
        }
        super.stepOn(level, pos, state, entity);
    }

    /** Springs the trap at {@code pos} on {@code victim}. Returns whether anything happened. */
    public static boolean trigger(ServerLevel level, BlockPos pos, Player victim) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(dev.umbra.registry.ModBlocks.TRAP_PLATE.get()) || state.getValue(TRIGGERED)) return false;
        level.setBlock(pos, state.setValue(TRIGGERED, true), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 1.0F, 0.5F);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 20, 0.4, 0.1, 0.4, 0.02);
        switch (kind(pos)) {
            case 0 -> {
                // ambush: three crawlers drop around the victim
                for (int i = 0; i < 3; i++) {
                    Mob m = ModEntities.CRAWLER.get().create(level, EntitySpawnReason.TRIGGERED);
                    if (m == null) continue;
                    double a = i * 2.1 + level.getRandom().nextDouble();
                    m.snapTo(victim.getX() + Math.cos(a) * 3.0, victim.getY() + 1.0, victim.getZ() + Math.sin(a) * 3.0, 0, 0);
                    if (!level.noCollision(m)) m.snapTo(victim.getX(), victim.getY(), victim.getZ() + 2.0, 0, 0);
                    m.setTarget(victim);
                    level.addFreshEntity(m);
                }
                level.playSound(null, pos, SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.5F);
            }
            case 1 -> {
                // the lights go out
                victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 8, 0));
                Sanity.add(victim, -6.0F);
                level.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.5F, 0.7F);
                level.playSound(null, pos, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.5F);
            }
            case 3 -> {
                // spikes: the floor bites
                victim.hurtServer(level, level.damageSources().stalagmite(), 5.0F);
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 4, 2));
                level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 0.2, victim.getZ(), 30, 0.5, 0.1, 0.5, 0.3);
                level.playSound(null, pos, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.BLOCKS, 1.5F, 0.6F);
            }
            case 4 -> {
                // a volley out of the walls, from four sides
                for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI / 2 + 0.4;
                    double sx = victim.getX() + Math.cos(a) * 4.5, sz = victim.getZ() + Math.sin(a) * 4.5, sy = victim.getEyeY() - 0.3;
                    var arrow = new net.minecraft.world.entity.projectile.arrow.Arrow(level, sx, sy, sz, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
                    arrow.pickup = net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.DISALLOWED;
                    arrow.shoot(victim.getX() - sx, victim.getEyeY() - 0.4 - sy, victim.getZ() - sz, 1.6F, 2.0F);
                    level.addFreshEntity(arrow);
                }
                level.playSound(null, pos, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.5F, 0.8F);
            }
            default -> {
                // a bell nobody rang: everything in the wing turns towards the victim
                level.playSound(null, pos, SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 3.0F, 0.5F);
                for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(40.0, 8.0, 40.0), e -> e instanceof CitadelStatue || e instanceof CitadelKeeper)) {
                    m.setTarget(victim);
                }
            }
        }
        return true;
    }
}
