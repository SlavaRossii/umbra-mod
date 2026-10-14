package dev.umbra.light;

import dev.umbra.boss.UmbraBoss;
import dev.umbra.registry.ModItems;
import dev.umbra.shadow.ShadowEntity;
import dev.umbra.underside.Underside;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The Ward. Feed it luminite crystals (use one on it; it holds up to {@link #MAX_CHARGE}).
 * While it burns, nothing dark comes near: within {@link #RADIUS} blocks (2 chunks; half that in the Underside)
 * monsters do not spawn and are driven out, your shadow cannot detach, footsteps, mimics and living darkness
 * stay away, and the mind rests. One crystal lasts about one in-game day.
 * Bosses do not care.
 */
public class WardBlock extends Block implements DarknessProof {
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 8);
    public static final int MAX_CHARGE = 8;
    public static final int RADIUS = 32;
    public static final int UNDERSIDE_RADIUS = 16;
    /** Ticks per crystal: one Minecraft day. */
    public static final int TICKS_PER_CRYSTAL = 24000;
    private static final int PULSE = 20;

    /** level → (ward position → last game time it was seen burning). */
    private static final Map<ResourceKey<Level>, Map<Long, long[]>> ACTIVE = new HashMap<>();   // {last seen, radius}

    public WardBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHARGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    public static int radius(Level level) {
        return Underside.is(level) ? UNDERSIDE_RADIUS : RADIUS;
    }

    /** Is {@code pos} inside a burning ward? */
    public static boolean protects(Level level, Vec3 pos) {
        Map<Long, long[]> wards = ACTIVE.get(level.dimension());
        if (wards == null || wards.isEmpty()) return false;
        long now = level.getGameTime();
        Iterator<Map.Entry<Long, long[]>> it = wards.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, long[]> e = it.next();
            if (now - e.getValue()[0] > PULSE * 3 || now < e.getValue()[0]) {
                it.remove();
                continue;
            }
            int r = (int) e.getValue()[1];
            BlockPos w = BlockPos.of(e.getKey());
            double dx = pos.x - (w.getX() + 0.5), dz = pos.z - (w.getZ() + 0.5), dy = pos.y - (w.getY() + 0.5);
            if (dx * dx + dz * dz <= (double) r * r && Math.abs(dy) <= r) return true;
        }
        return false;
    }

    public static boolean protects(Entity entity) {
        return protects(entity.level(), entity.position());
    }

    /** Forget everything (tests). */
    public static void clear() {
        ACTIVE.clear();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, PULSE);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.LUMINITE_CRYSTAL.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        int charge = state.getValue(CHARGE);
        if (charge >= MAX_CHARGE) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("block.umbra.ward.full"));
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            server.setBlock(pos, state.setValue(CHARGE, charge + 1), Block.UPDATE_ALL);
            if (!player.isCreative()) stack.shrink(1);
            server.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.5F);
            mark(server, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable("block.umbra.ward.charge", state.getValue(CHARGE), MAX_CHARGE));
        }
        return InteractionResult.SUCCESS;
    }

    private static void mark(ServerLevel level, BlockPos pos) {
        mark(level, pos, radius(level));
    }

    /** Declares a burning protector (the ward, or the lighthouse lamp with its own, bigger radius) for the next few seconds. */
    public static void mark(ServerLevel level, BlockPos pos, int radius) {
        ACTIVE.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(pos.asLong(), new long[]{level.getGameTime(), radius});
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, PULSE);
        int charge = state.getValue(CHARGE);
        if (charge <= 0) return;
        mark(level, pos);
        // one crystal per TICKS_PER_CRYSTAL on average
        if (random.nextInt(TICKS_PER_CRYSTAL / PULSE) == 0) {
            level.setBlock(pos, state.setValue(CHARGE, charge - 1), Block.UPDATE_ALL);
            if (charge == 1) level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        repel(level, pos);
        if (random.nextInt(3) == 0) {
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 2, 0.2, 0.3, 0.2, 0.01);
        }
    }

    /** Drives every dark thing (except bosses) out of the circle; things made of shadow simply burn. */
    public static int repel(ServerLevel level, BlockPos pos) {
        return repel(level, pos, radius(level));
    }

    public static int repel(ServerLevel level, BlockPos pos, int r) {
        Vec3 c = Vec3.atCenterOf(pos);
        int affected = 0;
        for (Entity e : level.getEntities((Entity) null, new AABB(pos).inflate(r), e -> e instanceof Enemy || e instanceof LightSensitive)) {
            if (e instanceof UmbraBoss || !e.isAlive()) continue;
            if (e instanceof dev.umbra.village.ShadowVillager sv && !sv.isMimic()) continue;
            if (e instanceof ShadowEntity s && s.isAlly()) continue;
            if (e.position().distanceToSqr(c) > (double) r * r) continue;
            affected++;
            if (e instanceof LightSensitive ls && (e instanceof ShadowEntity || e instanceof dev.umbra.mob.Shade || level.getRandom().nextInt(5) == 0)) {
                ls.onLit(level, null);
                if (!e.isAlive()) continue;
            }
            if (e instanceof Mob mob) mob.setTarget(null);
            Vec3 away = e.position().subtract(c);
            away = new Vec3(away.x, 0, away.z);
            if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
            Vec3 push = away.normalize().scale(0.7);
            e.push(push.x, 0.15, push.z);
            if (e instanceof Mob mob && mob.getNavigation() != null) {
                Vec3 flee = e.position().add(away.normalize().scale(r - away.length() + 6));
                mob.getNavigation().moveTo(flee.x, flee.y, flee.z, 1.2);
            }
        }
        return affected;
    }

    /** No natural monster spawns inside a burning ward. */
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Enemy) || event.getEntity() instanceof UmbraBoss) return;
        var reason = event.getSpawnType();
        if (reason != net.minecraft.world.entity.EntitySpawnReason.NATURAL
                && reason != net.minecraft.world.entity.EntitySpawnReason.CHUNK_GENERATION
                && reason != net.minecraft.world.entity.EntitySpawnReason.PATROL
                && reason != net.minecraft.world.entity.EntitySpawnReason.REINFORCEMENT) return;
        Level level = event.getEntity().level();
        if (protects(level, new Vec3(event.getX(), event.getY(), event.getZ()))) event.setSpawnCancelled(true);
    }
}
