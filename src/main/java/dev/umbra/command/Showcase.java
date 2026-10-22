package dev.umbra.command;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * /umbra showcase &lt;what&gt;: builds one of Umbra's structures (or lines up its creatures, or lays out its blocks) far
 * off in the world you stand in, and puts you where it is best seen, flying. For testers, and for the CI screenshots
 * that look at every model and building on each build.
 */
public final class Showcase {
    private Showcase() {}

    public static final List<String> ALL = new ArrayList<>(List.of(
            "lighthouse", "village_ring", "village_street", "village_hamlet", "workshop", "maze", "temple", "citadel_gate", "rift",
            "camp", "broken_rift", "figures",
            "citadel_1", "citadel_2", "citadel_3", "citadel_4", "citadel_5",
            "depths_landing", "depths_gallery", "depths_pillars", "depths_gates", "depths_sanctum",
            "mobs_1", "mobs_2", "mobs_3", "mobs_4", "blocks_1", "blocks_2", "altar", "altar_seal", "altar_rite", "treasure", "knives"));

    private static final int FLAGS = Block.UPDATE_CLIENTS;

    public static boolean show(ServerPlayer player, String what) {
        ServerLevel over = player.level().getServer().overworld();
        int i = ALL.indexOf(what);
        if (i < 0) return false;
        var r = over.getRandom();
        BlockPos base = ground(over, 4000 + i * 400, 4000);
        switch (what) {
            case "lighthouse" -> {
                dev.umbra.lighthouse.LighthouseFeature.build(over, base, r);
                look(player, over, base.offset(-38, 26, -38), base.above(22));
            }
            case "village_ring", "village_street", "village_hamlet" -> {
                var layout = dev.umbra.village.VillageBuilder.Layout.valueOf(what.substring(8).toUpperCase(java.util.Locale.ROOT));
                dev.umbra.village.VillageBuilder.build(over, base.above(), r, layout);
                look(player, over, base.offset(-30, 22, -30), base);
            }
            case "workshop" -> {
                dev.umbra.village.KeepersWorkshopFeature.build(over, base.offset(-4, 1, -3), r);
                look(player, over, base.offset(-10, 7, -12), base.above(2));
            }
            case "maze" -> {
                dev.umbra.quantum.QuantumMaze.build(over, base.offset(-11, 0, -11), r);
                look(player, over, base.offset(-20, 20, -20), base);
            }
            case "temple" -> {
                // it is dug into the bed of a black lake: give it rock to be dug into
                BlockState stone = dev.umbra.registry.ModBlocks.ASH_STONE.get().defaultBlockState();
                BlockPos c = new BlockPos(base.getX(), 120, base.getZ());
                for (BlockPos p : BlockPos.betweenClosed(c.offset(-22, -18, -22), c.offset(22, 0, 22))) over.setBlock(p, stone, FLAGS);
                BlockPos chest = dev.umbra.lake.SunkenTempleFeature.build(over, c, r);
                look(player, over, c.offset(-16, 10, -16), chest == null ? c.below(4) : chest);
            }
            case "citadel_gate" -> {
                dev.umbra.citadel.CitadelGateFeature.build(over, base, r);
                look(player, over, base.offset(0, 6, 14), base.above(3));
            }
            case "rift" -> {
                dev.umbra.underside.RiftFrame.build(over, base.above());
                look(player, over, base.offset(0, 3, -8), base.above(2));
            }
            case "camp" -> {
                dev.umbra.decor.UndersideLandmarkFeature.camp(over, base.above(), r);
                look(player, over, base.offset(-6, 5, -6), base.above());
            }
            case "broken_rift" -> {
                dev.umbra.decor.UndersideLandmarkFeature.brokenRift(over, base.above(), r);
                look(player, over, base.offset(-6, 5, -6), base.above());
            }
            case "figures" -> {
                dev.umbra.decor.UndersideLandmarkFeature.figures(over, base.above(), r);
                look(player, over, base.offset(-7, 5, -7), base.above());
            }
            case "citadel_1", "citadel_2", "citadel_3", "citadel_4", "citadel_5" -> {
                BlockPos o = new BlockPos(30000, 160, 30000);
                if (what.equals("citadel_1")) dev.umbra.citadel.Citadel.build(over, o, r);
                int k = what.charAt(8) - '1';
                BlockPos eye = o.offset(4 + k * 45, 2, 0);
                look(player, over, eye, eye.offset(20, -1, 0));
            }
            case "altar", "altar_seal", "altar_rite" -> {
                // 2.8: three altars: asleep, with a seal and its offerings, and in the middle of a rite
                BlockPos a = base.above();
                for (BlockPos p : BlockPos.betweenClosed(a.offset(-4, 0, -4), a.offset(8, 4, 4))) over.setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS);
                var block = dev.umbra.registry.ModBlocks.SUMMONING_ALTAR.get().defaultBlockState();
                for (int k = 0; k < 3; k++) over.setBlock(a.offset(k * 3, 0, 0), block, Block.UPDATE_ALL);
                if (over.getBlockEntity(a.offset(3, 0, 0)) instanceof dev.umbra.altar.SummoningAltarBlockEntity laid) {
                    laid.placeSeal(new net.minecraft.world.item.ItemStack(dev.umbra.registry.ModItems.SEAL_OF_WATCHING.get()), player);
                    laid.offer(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SPIDER_EYE, 3), player);
                    laid.offer(new net.minecraft.world.item.ItemStack(dev.umbra.registry.ModItems.ECHO_QUARTZ.get()), player);
                    over.setBlock(a.offset(3, 0, 0), block.setValue(dev.umbra.altar.SummoningAltarBlock.GLOW, 1), Block.UPDATE_ALL);
                }
                if (over.getBlockEntity(a.offset(6, 0, 0)) instanceof dev.umbra.altar.SummoningAltarBlockEntity rite) {
                    rite.placeSeal(new net.minecraft.world.item.ItemStack(dev.umbra.registry.ModItems.SEAL_OF_HERDING.get()), player);
                    rite.offer(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WOOL.white(), 3), player);
                    rite.offer(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE, 2), player);
                    rite.offer(new net.minecraft.world.item.ItemStack(dev.umbra.registry.ModItems.SHADOW_SHARD.get(), 2), player);
                    rite.exhibitRite(over, 0.5F, 220);
                    for (BlockPos f : new BlockPos[]{a.offset(8, 0, 1), a.offset(8, 0, -1), a.offset(6, 0, 2)})
                        over.setBlock(f, Blocks.SOUL_CAMPFIRE.defaultBlockState(), Block.UPDATE_ALL);
                }
                switch (what) {
                    case "altar_seal" -> look(player, over, a.offset(3, 1, -2), a.offset(3, 0, 0));
                    case "altar_rite" -> look(player, over, a.offset(6, 1, -2), a.offset(6, 0, 0));
                    default -> look(player, over, a.offset(3, 2, -4), a.offset(3, 0, 0));
                }
            }
            case "treasure" -> {
                // 2.9: the Keepers' sea-chest, shut and opened, and a disc playing in a jukebox
                BlockPos a = base.above();
                for (BlockPos p : BlockPos.betweenClosed(a.offset(-4, 0, -5), a.offset(8, 4, 4))) over.setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS);
                var chest = dev.umbra.registry.ModBlocks.KEEPERS_CHEST.get().defaultBlockState().setValue(dev.umbra.depths.KeepersChest.FACING, net.minecraft.core.Direction.NORTH);
                over.setBlock(a, chest, Block.UPDATE_ALL);
                over.setBlock(a.offset(3, 0, 0), chest.setValue(dev.umbra.depths.KeepersChest.OPEN, true), Block.UPDATE_ALL);
                over.setBlock(a.offset(6, 0, 0), Blocks.JUKEBOX.defaultBlockState(), Block.UPDATE_ALL);
                if (over.getBlockEntity(a.offset(6, 0, 0)) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity jb)
                    jb.setTheItem(new net.minecraft.world.item.ItemStack(dev.umbra.registry.ModItems.MUSIC_DISCS.get("drowned_keeper").get()));
                look(player, over, a.offset(3, 2, -4), a.offset(3, 0, 0));
            }
            case "knives" -> {
                // 2.9: thrown knives in the air, point first, and one stuck in a wall
                BlockPos a = base.above();
                for (BlockPos p : BlockPos.betweenClosed(a.offset(-5, 0, -5), a.offset(5, 4, 4))) over.setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS);
                for (int y = 0; y < 3; y++)
                    for (int x = -2; x <= 2; x++) over.setBlock(a.offset(x, y, 3), Blocks.STONE_BRICKS.defaultBlockState(), FLAGS);
                double cx = a.getX() + 0.5, cy = a.getY() + 1.4, cz = a.getZ() + 0.5;
                float[][] flights = {{90, 0, -1.2F, 0}, {-90, 15, 1.2F, 0.4F}, {0, -10, 0, 0.9F}, {150, 30, 0.6F, -0.8F}};
                for (float[] k : flights)
                    over.addFreshEntity(dev.umbra.gear.ThrownKnife.display(over, cx + k[2], cy + k[3] * 0.5, cz + k[3], k[0], k[1]));
                over.addFreshEntity(dev.umbra.gear.ThrownKnife.display(over, cx - 1.0, cy - 0.2, a.getZ() + 2.9, 0, -8));
                look(player, over, a.offset(0, 1, -3), a.offset(0, 1, 2));
            }
            default -> {
                if (what.startsWith("depths_")) return depths(player, what);
                if (what.startsWith("mobs_")) return mobs(player, over, base, what.charAt(5) - '1');
                if (what.startsWith("blocks_")) return blocks(player, over, base, what.charAt(7) - '1');
            }
        }
        return true;
    }

    private static boolean depths(ServerPlayer player, String what) {
        ServerLevel under = player.level().getServer().getLevel(dev.umbra.underside.Underside.LEVEL);
        if (under == null) return false;
        if (!dev.umbra.depths.Depths.isBuilt(under)) dev.umbra.depths.Depths.build(under, under.getRandom());
        BlockPos o = dev.umbra.depths.Depths.ORIGIN;
        switch (what) {
            case "depths_landing" -> look(player, under, o.offset(0, 4, 3), o.offset(0, 2, -6));
            case "depths_gallery" -> look(player, under, o.offset(-7, 6, -24), o.offset(6, 1, -32));
            case "depths_pillars" -> look(player, under, o.offset(-9, 8, -53), o.offset(6, 2, -66));
            case "depths_gates" -> look(player, under, o.offset(0, 2, -113), o.offset(0, 2, -125));
            default -> look(player, under, o.offset(-8, 9, -144), o.offset(2, 1, -158));
        }
        return true;
    }

    /** Every creature of the mod in a row, a few at a time, facing you. */
    private static boolean mobs(ServerPlayer player, ServerLevel level, BlockPos base, int group) {
        List<EntityType<?>> types = new ArrayList<>();
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            if (!BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals(dev.umbra.Umbra.MODID)) continue;
            Entity e = t.create(level, EntitySpawnReason.COMMAND);
            if (e instanceof LivingEntity) types.add(t);
            if (e != null) e.discard();
        }
        int per = (types.size() + 3) / 4;
        List<EntityType<?>> mine = types.subList(Math.min(types.size(), group * per), Math.min(types.size(), (group + 1) * per));
        double x = 0;
        for (EntityType<?> t : mine) {
            Entity e = t.create(level, EntitySpawnReason.COMMAND);
            if (!(e instanceof LivingEntity living)) continue;
            double w = Math.max(1.2, living.getBbWidth()) + 1.6;
            e.snapTo(base.getX() + x + w / 2, base.getY() + 1, base.getZ() + 0.5, 180, 0);
            living.setYHeadRot(180);
            living.yBodyRot = 180;
            if (e instanceof Mob m) {
                m.setNoAi(true);
                m.setPersistenceRequired();
            }
            e.addTag("shot");
            level.addFreshEntity(e);
            x += w;
        }
        look(player, level, base.offset((int) (x / 2), 2, -(int) Math.max(6, x * 0.5)), base.offset((int) (x / 2), 1, 0));
        return true;
    }

    /** The mod's blocks set out on the ground in rows. */
    private static boolean blocks(ServerPlayer player, ServerLevel level, BlockPos base, int half) {
        List<Block> all = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(b);
            if (id.getNamespace().equals(dev.umbra.Umbra.MODID) && !id.getPath().equals("held_light")) all.add(b);
        }
        int per = (all.size() + 1) / 2;
        List<Block> mine = all.subList(Math.min(all.size(), half * per), Math.min(all.size(), (half + 1) * per));
        int cols = 9;
        for (int i = 0; i < mine.size(); i++) {
            BlockPos p = base.offset((i % cols) * 2, 1, (i / cols) * 2);
            level.setBlock(p.below(), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), FLAGS);
            BlockState s = mine.get(i).defaultBlockState();
            if (s.canSurvive(level, p)) level.setBlock(p, s, Block.UPDATE_ALL);
        }
        int rows = (mine.size() + cols - 1) / cols;
        look(player, level, base.offset(cols - 1, 7, -7), base.offset(cols - 1, 0, rows));
        return true;
    }

    private static BlockPos ground(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
    }

    /** Puts the player at {@code eye} (flying) looking at {@code at}. */
    private static void look(ServerPlayer player, ServerLevel level, BlockPos eye, BlockPos at) {
        Vec3 from = Vec3.atCenterOf(eye), to = Vec3.atCenterOf(at), d = to.subtract(from);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleport(new TeleportTransition(level, from.subtract(0, player.getEyeHeight(), 0), Vec3.ZERO, yaw, pitch, TeleportTransition.DO_NOTHING));
    }
}
