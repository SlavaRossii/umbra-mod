package dev.umbra.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.umbra.darkness.DarknessManager;
import dev.umbra.mimic.MimicManager;
import dev.umbra.quantum.MazeController;
import dev.umbra.quantum.MazePattern;
import dev.umbra.quantum.MazePatterns;
import dev.umbra.quantum.QuantumMaze;
import dev.umbra.sanity.Hallucinations;
import dev.umbra.sanity.Sanity;
import dev.umbra.shadow.ShadowManager;
import dev.umbra.steps.StepsManager;
import dev.umbra.underside.RiftFrame;
import dev.umbra.underside.RiftTravel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** /umbra … — debug and showcase commands (need cheats / op level 2). */
public final class UmbraCommand {
    private UmbraCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> patterns = Commands.literal("mazepattern");
        for (MazePattern p : MazePatterns.ALL) {
            patterns.then(Commands.literal(p.id()).executes(ctx -> mazePattern(ctx, p.id())));
        }
        dispatcher.register(Commands.literal("umbra")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(patterns)
                .then(Commands.literal("sanity").then(Commands.argument("value", FloatArgumentType.floatArg(0, 100)).executes(UmbraCommand::sanity)))
                .then(Commands.literal("hallucinate").executes(UmbraCommand::hallucinate))
                .then(Commands.literal("underside").executes(UmbraCommand::underside))
                .then(Commands.literal("rift").executes(UmbraCommand::rift))
                .then(Commands.literal("shadow").executes(UmbraCommand::shadow))
                .then(Commands.literal("steps").executes(UmbraCommand::steps))
                .then(Commands.literal("mimic").executes(UmbraCommand::mimic))
                .then(Commands.literal("darkness").executes(UmbraCommand::darkness))
                .then(Commands.literal("maze").executes(UmbraCommand::maze))
                .then(Commands.literal("home").executes(UmbraCommand::home))
                .then(Commands.literal("status").executes(UmbraCommand::status))
                .then(Commands.literal("nightmare").executes(UmbraCommand::nightmare))
                .then(Commands.literal("citadel").executes(UmbraCommand::citadel))
                .then(Commands.literal("storm").executes(UmbraCommand::storm))
                .then(Commands.literal("temple").executes(UmbraCommand::temple))
                .then(Commands.literal("forest").executes(UmbraCommand::forest))
                .then(journal())
                .then(village()));
    }

    /** /umbra journal all | reset — for testers. */
    private static LiteralArgumentBuilder<CommandSourceStack> journal() {
        return Commands.literal("journal")
                .then(Commands.literal("all").executes(ctx -> {
                    int n = dev.umbra.journal.Journal.unlockAll(ctx.getSource().getPlayerOrException());
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.umbra.journal.all", n), false);
                    return n;
                }))
                .then(Commands.literal("reset").executes(ctx -> {
                    dev.umbra.journal.Journal.clear(ctx.getSource().getPlayerOrException());
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.umbra.journal.reset"), false);
                    return 1;
                }));
    }

    private static int shadow(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean ok = ShadowManager.forceSpawn(player);
        reply(ctx, ok ? "command.umbra.shadow" : "command.umbra.failed");
        return ok ? 1 : 0;
    }

    private static int steps(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        StepsManager.forceStart(ctx.getSource().getPlayerOrException());
        reply(ctx, "command.umbra.steps");
        return 1;
    }

    private static int mimic(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        HitResult hit = player.pick(6.0, 0.0F, false);
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK
                && MimicManager.tryMimic((ServerLevel) player.level(), bhr.getBlockPos())) {
            reply(ctx, "command.umbra.mimic");
            return 1;
        }
        reply(ctx, "command.umbra.mimic_failed");
        return 0;
    }

    private static int darkness(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Vec3 ahead = player.position().add(player.getLookAngle().scale(6.0));
        int n = DarknessManager.seedCluster((ServerLevel) player.level(), BlockPos.containing(ahead), 30);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.umbra.darkness", n), false);
        return n;
    }

    private static int maze(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        Vec3 dir = player.getLookAngle();
        double dist = QuantumMaze.SIZE / 2.0 + 4;
        BlockPos center = BlockPos.containing(player.position().add(dir.x * dist, 0, dir.z * dist));
        BlockPos corner = center.offset(-QuantumMaze.SIZE / 2, -1, -QuantumMaze.SIZE / 2);
        QuantumMaze.build(level, corner, level.getRandom());
        reply(ctx, "command.umbra.maze");
        return 1;
    }

    private static int home(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MimicManager.setHome(player, player.blockPosition());
        reply(ctx, "command.umbra.home");
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BlockPos home = MimicManager.getHome(player);
        String text = "sanity=" + Math.round(Sanity.get(player)) + " shadow[" + ShadowManager.debug(player) + "] steps["
                + StepsManager.debug(player) + "] home=" + (home == null ? "-" : home.toShortString());
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int mazePattern(CommandContext<CommandSourceStack> ctx, String id) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean ok = MazeController.force((ServerLevel) player.level(), player.blockPosition(), id);
        ctx.getSource().sendSuccess(() -> ok
                ? Component.translatable("command.umbra.mazepattern", Component.translatable("maze.umbra.pattern." + id))
                : Component.translatable("command.umbra.no_maze"), false);
        return ok ? 1 : 0;
    }

    private static int sanity(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        float v = FloatArgumentType.getFloat(ctx, "value");
        Sanity.set(player, v);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.umbra.sanity", Math.round(v)), false);
        return 1;
    }

    private static int hallucinate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Hallucinations.fakeSound((ServerLevel) player.level(), player, player.getRandom());
        boolean ok = Hallucinations.spawnFigure((ServerLevel) player.level(), player, player.getRandom());
        reply(ctx, ok ? "command.umbra.hallucinate" : "command.umbra.failed");
        return ok ? 1 : 0;
    }

    private static int underside(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var transition = RiftTravel.destination((ServerLevel) player.level(), player, player.blockPosition());
        if (transition == null) {
            reply(ctx, "command.umbra.failed");
            return 0;
        }
        player.teleport(transition);
        return 1;
    }

    private static int rift(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Vec3 dir = player.getLookAngle();
        BlockPos base = BlockPos.containing(player.position().add(dir.x * 3, 0, dir.z * 3));
        RiftFrame.build(player.level(), base);
        reply(ctx, "command.umbra.rift");
        return 1;
    }

    private static int nightmare(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (player.level().dimension() != net.minecraft.world.level.Level.OVERWORLD || dev.umbra.nightmare.Nightmares.inNightmare(player)) {
            reply(ctx, "command.umbra.nightmare_failed");
            return 0;
        }
        return dev.umbra.nightmare.Nightmares.enter(player) ? 1 : 0;
    }

    /** Into the nearest citadel (built if needed), from anywhere. */
    private static int citadel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel under = player.level().getServer().getLevel(dev.umbra.underside.Underside.LEVEL);
        if (under == null) return 0;
        int[] c = dev.umbra.citadel.CitadelGateFeature.nearest(under.getSeed(), (int) player.getX(), (int) player.getZ());
        BlockPos gate = dev.umbra.citadel.CitadelGateFeature.findSolidGround(under, c[0] * 16 + 8, c[1] * 16 + 8).above(2);
        player.teleport(dev.umbra.citadel.Citadel.enter(under, player, gate));
        return 1;
    }

    /** Toggles an ash storm for this player (they must stand in the ash plains to feel it). */
    private static int storm(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean on = !player.getData(dev.umbra.registry.ModAttachments.STORM_TEST);
        player.setData(dev.umbra.registry.ModAttachments.STORM_TEST, on);
        reply(ctx, on ? "command.umbra.storm.on" : "command.umbra.storm.off");
        return 1;
    }

    /** Builds a sunken temple 30 blocks ahead, on the floor below. */
    private static int temple(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        Vec3 dir = dev.umbra.util.UmbraUtil.horizontal(player.getLookAngle());
        BlockPos ahead = BlockPos.containing(player.position().add(dir.scale(30))).above(10);
        BlockPos f = dev.umbra.decor.UndersideDecorFeature.floor(level, ahead, 60);
        if (f == null) {
            reply(ctx, "command.umbra.failed");
            return 0;
        }
        dev.umbra.lake.SunkenTempleFeature.build(level, f, level.getRandom());
        return 1;
    }

    /** As if you had run through the whispering forest. */
    private static int forest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        dev.umbra.underside.UndersideAmbience.alert((ServerLevel) player.level(), player);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> village() {
        LiteralArgumentBuilder<CommandSourceStack> v = Commands.literal("village");
        for (var layout : dev.umbra.village.VillageBuilder.Layout.values()) {
            v.then(Commands.literal(layout.name().toLowerCase(java.util.Locale.ROOT)).executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                Vec3 dir = dev.umbra.util.UmbraUtil.horizontal(player.getLookAngle());
                BlockPos at = BlockPos.containing(player.position().add(dir.scale(34)));
                dev.umbra.village.VillageManager.buildHere((ServerLevel) player.level(), at, layout);
                return 1;
            }));
        }
        return v;
    }

    private static void reply(CommandContext<CommandSourceStack> ctx, String key) {
        ctx.getSource().sendSuccess(() -> Component.translatable(key), false);
    }
}
