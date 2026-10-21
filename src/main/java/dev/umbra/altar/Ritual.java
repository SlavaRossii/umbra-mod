package dev.umbra.altar;

import dev.umbra.registry.ModEntities;
import dev.umbra.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 2.8: what each seal asks of the altar before it calls its master. Every rite needs the seal on the altar, the right
 * offerings in its dish, and something of the place around it; some also ask something of you while it lasts.
 * All of them wake only in the Underside.
 */
public final class Ritual {
    /** One kind of offering and how many of it. */
    public record Offering(Supplier<? extends Item> item, int count) {
        public boolean matches(ItemStack stack) {
            return stack.is(item.get());
        }
    }

    public final String id;
    public final Supplier<? extends Item> seal;
    public final Supplier<? extends EntityType<? extends Mob>> boss;
    public final List<Offering> offerings;
    /** Ticks the rite lasts once begun. */
    public final int length;

    private Ritual(String id, Supplier<? extends Item> seal, Supplier<? extends EntityType<? extends Mob>> boss, int length, Offering... offerings) {
        this.id = id;
        this.seal = seal;
        this.boss = boss;
        this.length = length;
        this.offerings = List.of(offerings);
    }

    public static final Ritual WATCHING = new Ritual("watching", ModItems.SEAL_OF_WATCHING, ModEntities.WATCHER, 200,
            new Offering(() -> Items.SPIDER_EYE, 3), new Offering(ModItems.ECHO_QUARTZ, 1));
    public static final Ritual MIMICRY = new Ritual("mimicry", ModItems.SEAL_OF_MIMICRY, ModEntities.MIMIC_QUEEN, 160,
            new Offering(() -> Items.GOLD_INGOT, 2), new Offering(ModItems.SHADOW_SHARD, 2));
    public static final Ritual HERDING = new Ritual("herding", ModItems.SEAL_OF_HERDING, ModEntities.SHEPHERD, 160,
            new Offering(() -> Items.WHITE_WOOL, 3), new Offering(() -> Items.BONE, 2), new Offering(ModItems.SHADOW_SHARD, 2));
    public static final Ritual REFLECTION = new Ritual("reflection", ModItems.SEAL_OF_REFLECTION, ModEntities.DOPPELGANGER, 160,
            new Offering(ModItems.MIRROR, 1), new Offering(() -> Items.GLASS, 2));
    public static final List<Ritual> ALL = List.of(WATCHING, MIMICRY, HERDING, REFLECTION);

    /** The rite a seal belongs to, or null. */
    public static Ritual of(ItemStack seal) {
        if (seal.isEmpty()) return null;
        for (Ritual r : ALL) if (seal.is(r.seal.get())) return r;
        return null;
    }

    // ------------------------------------------------------------------ the place round the altar
    /** Light sources within eight blocks: the Watcher comes only to the dark. (The altar's own glow does not count.) */
    public static final int DARK_RADIUS = 8;
    /** Full chests within three blocks the Queen needs to come: four. */
    public static final int CHEST_RADIUS = 3, CHESTS = 4;
    /** Lit soul campfires within four blocks the Shepherd's flock gathers round: three. */
    public static final int FIRE_RADIUS = 4, FIRES = 3;
    /** Still water within two blocks for a reflection: four sources. */
    public static final int WATER_RADIUS = 2, WATERS = 4;
    /** What the Doppelganger takes from you: three hearts. */
    public static final float BLOOD = 6.0F;

    public static int lights(ServerLevel level, BlockPos altar) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(altar.offset(-DARK_RADIUS, -4, -DARK_RADIUS), altar.offset(DARK_RADIUS, 6, DARK_RADIUS))) {
            if (p.equals(altar)) continue;
            if (level.getBlockState(p).getLightEmission() > 0) n++;
        }
        return n;
    }

    public static List<BlockPos> fullChests(ServerLevel level, BlockPos altar) {
        List<BlockPos> list = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(altar.offset(-CHEST_RADIUS, -1, -CHEST_RADIUS), altar.offset(CHEST_RADIUS, 1, CHEST_RADIUS))) {
            BlockState s = level.getBlockState(p);
            if (!s.is(Blocks.CHEST) && !s.is(Blocks.TRAPPED_CHEST)) continue;
            if (level.getBlockEntity(p) instanceof Container c && !c.isEmpty()) list.add(p.immutable());
        }
        return list;
    }

    public static List<BlockPos> soulFires(ServerLevel level, BlockPos altar) {
        List<BlockPos> list = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(altar.offset(-FIRE_RADIUS, -1, -FIRE_RADIUS), altar.offset(FIRE_RADIUS, 1, FIRE_RADIUS))) {
            BlockState s = level.getBlockState(p);
            if (s.is(Blocks.SOUL_CAMPFIRE) && s.getValue(CampfireBlock.LIT)) list.add(p.immutable());
        }
        return list;
    }

    public static int waters(ServerLevel level, BlockPos altar) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(altar.offset(-WATER_RADIUS, -1, -WATER_RADIUS), altar.offset(WATER_RADIUS, 0, WATER_RADIUS))) {
            var f = level.getFluidState(p);
            if (f.is(Fluids.WATER) && f.isSource()) n++;
        }
        return n;
    }

    /** What is still wrong about the place, as a line for the player, or null when the place is right. */
    public Component placeProblem(ServerLevel level, BlockPos altar, ServerPlayer caller) {
        return switch (id) {
            case "watching" -> {
                int n = lights(level, altar);
                yield n > 0 ? Component.translatable("message.umbra.altar.need.dark", n) : null;
            }
            case "mimicry" -> {
                int n = fullChests(level, altar).size();
                yield n < CHESTS ? Component.translatable("message.umbra.altar.need.chests", n, CHESTS) : null;
            }
            case "herding" -> {
                int n = soulFires(level, altar).size();
                yield n < FIRES ? Component.translatable("message.umbra.altar.need.fires", n, FIRES) : null;
            }
            case "reflection" -> {
                int n = waters(level, altar);
                if (n < WATERS) yield Component.translatable("message.umbra.altar.need.water", n, WATERS);
                yield caller != null && !caller.isCreative() && caller.getHealth() <= BLOOD + 1.0F ? Component.translatable("message.umbra.altar.need.blood") : null;
            }
            default -> null;
        };
    }

    /** The rite has begun: what it takes at once. */
    public void onBegin(ServerLevel level, BlockPos altar, ServerPlayer caller) {
        if (this == REFLECTION && caller != null && !caller.isCreative()) {
            caller.hurtServer(level, level.damageSources().magic(), BLOOD);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, caller.getX(), caller.getY() + 1.0, caller.getZ(), 8, 0.3, 0.4, 0.3, 0.1);
            level.playSound(null, altar, SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
    }

    /** The rite is done: what the place gives up. */
    public void onComplete(ServerLevel level, BlockPos altar) {
        if (this == MIMICRY) {
            int taken = 0;
            for (BlockPos p : fullChests(level, altar)) {
                if (level.getBlockEntity(p) instanceof Container c) c.clearContent();
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 16, 0.3, 0.3, 0.3, 0.02);
                level.playSound(null, p, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
                if (++taken >= CHESTS) break;
            }
        } else if (this == HERDING) {
            for (BlockPos p : soulFires(level, altar)) {
                level.setBlock(p, level.getBlockState(p).setValue(CampfireBlock.LIT, false), Block.UPDATE_ALL);
                level.sendParticles(ParticleTypes.SOUL, p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5, 10, 0.2, 0.3, 0.2, 0.03);
                level.playSound(null, p, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.7F);
            }
        }
    }

    /** Lines in the voice of the hero, shown under the boss's name as it comes. */
    public Component subtitle() {
        return Component.translatable("message.umbra.altar.subtitle." + id);
    }

    public Vec3 centre(BlockPos altar) {
        return Vec3.atCenterOf(altar).add(0, 0.6, 0);
    }
}
