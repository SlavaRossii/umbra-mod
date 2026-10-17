package dev.umbra.depths;

import dev.umbra.journal.Journal;
import dev.umbra.registry.ModAttachments;
import dev.umbra.registry.ModBlocks;
import dev.umbra.registry.ModEntities;
import dev.umbra.registry.ModItems;
import dev.umbra.underside.Underside;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 2.6: the Depths, the drowned halls of the Keepers. One sealed place for the whole world, deep in the rock of the
 * Underside far from everything (like the citadel's pocket), reached with the Key of the Depths from any water of the
 * Underside, and left the same way. Built on the first visit:
 * <ol>
 *   <li>the landing: a stair of stone out of a black pool, Keepers' lamps on the walls;</li>
 *   <li>the drowned gallery: water to the chin, a strip of air under the vault, and things in the water;</li>
 *   <li>the hall of pillars: dry, with cages that call the dark and Weepers in the gloom;</li>
 *   <li>the sanctum: the Drowned Keeper, and what the Keepers kept: a chest and their pearl.</li>
 * </ol>
 */
public final class Depths {
    private Depths() {}

    public static final BlockPos ORIGIN = new BlockPos(120000, 140, 0);
    public static final int LENGTH = 96, HALF = 14, HEIGHT = 12;
    public static final int GALLERY_FROM = 18, PILLARS_FROM = 46, SANCTUM_FROM = 72;
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, "chests/depths"));
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    public static AABB bounds() {
        return new AABB(ORIGIN.getX() - 4, ORIGIN.getY() - 4, ORIGIN.getZ() - HALF - 2, ORIGIN.getX() + LENGTH + 2, ORIGIN.getY() + HEIGHT + 3, ORIGIN.getZ() + HALF + 2);
    }

    public static boolean inside(Player p) {
        return Underside.is(p.level()) && bounds().contains(p.position());
    }

    public static boolean isBuilt(ServerLevel level) {
        return level.getBlockState(ORIGIN.offset(-3, -3, 0)).is(Blocks.DEEPSLATE_TILES) && level.getBlockState(ORIGIN.offset(2, 1, 0)).isAir();
    }

    /** Into the Depths (built on the first visit); the way back is remembered. */
    public static void enter(ServerLevel underside, ServerPlayer player) {
        if (!isBuilt(underside)) build(underside, underside.getRandom());
        player.setData(ModAttachments.DEPTHS_RETURN, player.blockPosition().asLong());
        Journal.unlock(player, "depths");
        player.sendSystemMessage(Component.translatable("message.umbra.depths.enter").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        player.teleport(new TeleportTransition(underside, Vec3.atBottomCenterOf(ORIGIN.offset(3, 1, 0)), Vec3.ZERO, -90.0F, 0.0F, TeleportTransition.PLAY_PORTAL_SOUND));
    }

    /** Back to the water you came down through. */
    public static void leave(ServerLevel underside, ServerPlayer player) {
        long back = player.getData(ModAttachments.DEPTHS_RETURN);
        BlockPos to = back == Long.MIN_VALUE ? dev.umbra.lighthouse.LighthouseManager.centre(underside).above() : BlockPos.of(back).above();
        player.teleport(new TeleportTransition(underside, Vec3.atBottomCenterOf(to), Vec3.ZERO, player.getYRot(), 0.0F, TeleportTransition.PLAY_PORTAL_SOUND));
    }

    // ------------------------------------------------------------------ building

    public static void build(ServerLevel level, RandomSource random) {
        BlockPos o = ORIGIN;
        BlockState shell = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState moss = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState lamp = ModBlocks.LUMINITE_LAMP.get().defaultBlockState();
        // the shell, then the hollow of each hall
        fill(level, o.offset(-3, -3, -HALF - 1), o.offset(LENGTH + 1, HEIGHT + 2, HALF + 1), shell);
        for (int x = 0; x <= LENGTH; x++)
            for (int z = -HALF; z <= HALF; z++) {
                int w = width(x);
                if (Math.abs(z) > w) continue;
                level.setBlock(o.offset(x, 0, z), random.nextInt(7) == 0 ? moss : floor, FLAGS);
                int top = x < GALLERY_FROM ? 8 : x < PILLARS_FROM ? 6 : x < SANCTUM_FROM ? 10 : 11;
                for (int y = 1; y <= top; y++) level.setBlock(o.offset(x, y, z), air, FLAGS);
            }
        // 1. the landing: a black pool you came out of, a stair up out of it
        for (int x = 1; x <= 6; x++)
            for (int z = -4; z <= 4; z++)
                for (int y = -2; y <= 0; y++) level.setBlock(o.offset(x + 6, y, z), water, FLAGS);
        // 2. the drowned gallery: water up to y 4, a strip of air above, the way is lit from below
        for (int x = GALLERY_FROM; x < PILLARS_FROM; x++)
            for (int z = -width(x); z <= width(x); z++) {
                for (int y = 1; y <= 4; y++) level.setBlock(o.offset(x, y, z), water, FLAGS);
                if ((x + z) % 9 == 0) level.setBlock(o.offset(x, 0, z), Blocks.SEA_LANTERN.defaultBlockState(), FLAGS);
            }
        // 3. the hall of pillars
        for (int x = PILLARS_FROM + 4; x < SANCTUM_FROM - 2; x += 6)
            for (int z : new int[]{-6, 6})
                fill(level, o.offset(x, 1, z), o.offset(x + 1, 10, z + 1), random.nextBoolean() ? moss : shell);
        // 4. the sanctum: a dais, the chest, the pearl on a pedestal
        fill(level, o.offset(LENGTH - 8, 1, -4), o.offset(LENGTH - 2, 1, 4), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        BlockPos chest = o.offset(LENGTH - 4, 2, 0);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, net.minecraft.core.Direction.WEST), FLAGS);
        RandomizableContainer.setBlockEntityLootTable(level, random, chest, LOOT);
        // lamps of the Keepers in the walls, every few blocks, both sides
        for (int x = 2; x < LENGTH; x += 5) {
            int w = width(x);
            level.setBlock(o.offset(x, 5, -w - 1), lamp, FLAGS);
            level.setBlock(o.offset(x, 5, w + 1), lamp, FLAGS);
        }
        // the dark that lives here
        cage(level, ModEntities.LURKER.get(), o.offset(PILLARS_FROM - 6, 0, -width(PILLARS_FROM - 6)));
        cage(level, ModEntities.SHADE.get(), o.offset(PILLARS_FROM + 10, 0, -7));
        cage(level, ModEntities.CRAWLER.get(), o.offset(PILLARS_FROM + 16, 0, 7));
        for (int i = 0; i < 3; i++) spawn(level, ModEntities.WEEPER.get(), o.offset(PILLARS_FROM + 6 + i * 6, 1, (i % 2 == 0 ? -3 : 3)), 0);
        for (int i = 0; i < 2; i++) spawn(level, ModEntities.LURKER.get(), o.offset(GALLERY_FROM + 8 + i * 10, 1, 0), 0);
        spawn(level, ModEntities.CITADEL_KEEPER.get(), o.offset(SANCTUM_FROM + 6, 1, 0), 9.0);   // the Drowned Keeper
    }

    /** Half-width of the halls along x: the landing narrow, the gallery a corridor, the halls wide. */
    static int width(int x) {
        if (x < GALLERY_FROM) return 5;
        if (x < PILLARS_FROM) return 3;
        if (x < SANCTUM_FROM) return 10;
        return 8;
    }

    private static void fill(ServerLevel level, BlockPos a, BlockPos b, BlockState s) {
        for (BlockPos p : BlockPos.betweenClosed(a, b)) level.setBlock(p, s, FLAGS);
    }

    private static void cage(ServerLevel level, EntityType<?> type, BlockPos at) {
        level.setBlock(at, Blocks.SPAWNER.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity c) {
            c.setEntityId(type, level.getRandom());
            c.setChanged();
        }
    }

    /** Spawns a guard; {@code healthTimes} > 0 makes it a mini-boss with that many times its health. */
    private static void spawn(ServerLevel level, EntityType<? extends Mob> type, BlockPos at, double healthTimes) {
        Mob m = type.create(level, EntitySpawnReason.STRUCTURE);
        if (m == null) return;
        m.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        m.setPersistenceRequired();
        if (healthTimes > 0) {
            var hp = m.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) hp.addOrReplacePermanentModifier(new AttributeModifier(Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, "drowned_keeper"),
                    healthTimes - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            var dmg = m.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmg != null) dmg.addOrReplacePermanentModifier(new AttributeModifier(Identifier.fromNamespaceAndPath(dev.umbra.Umbra.MODID, "drowned_keeper_dmg"),
                    0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            m.setHealth(m.getMaxHealth());
            m.setCustomName(Component.translatable("entity.umbra.drowned_keeper"));
            m.setCustomNameVisible(true);
            m.addTag("umbra_drowned_keeper");
        }
        level.addFreshEntity(m);
    }

    // ------------------------------------------------------------------ the key

    /** The Key of the Depths: used standing in water in the Underside it takes you down; in the Depths, back up. */
    public static class KeyItem extends Item {
        public KeyItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
            if (inside(sp)) {
                leave(server, sp);
                return InteractionResult.SUCCESS;
            }
            if (!Underside.is(server) || !sp.isInWater()) {
                sp.sendOverlayMessage(Component.translatable("message.umbra.depths.water").withStyle(ChatFormatting.DARK_AQUA));
                return InteractionResult.FAIL;
            }
            server.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
            enter(server, sp);
            sp.getCooldowns().addCooldown(sp.getItemInHand(hand), 60);
            return InteractionResult.SUCCESS;
        }
    }

    /** The Keepers' pearl, pinned to a chestplate: breath under water, and the dark weighs less on the mind. */
    public static void pearlTick(ServerPlayer player) {
        if (player.tickCount % 20 != 5 || !dev.umbra.gear.Gear.hasArtifact(player, dev.umbra.gear.Gear.KEEPERS_PEARL)) return;
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING, 60, 0, true, false, true));
    }

    /** For the journal's sense of where things are. */
    public static boolean near(ServerLevel level, BlockPos p) {
        return Underside.is(level) && bounds().inflate(16).contains(Vec3.atCenterOf(p));
    }

    static ServerLevel underside(net.minecraft.server.MinecraftServer server) {
        return server.getLevel(Underside.LEVEL);
    }
}
