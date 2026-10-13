package dev.umbra.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.umbra.darkness.DarknessManager;
import dev.umbra.mimic.MimicManager;
import dev.umbra.quantum.QuantumMaze;
import dev.umbra.shadow.ShadowManager;
import dev.umbra.steps.StepsManager;
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
        dispatcher.register(Commands.literal("umbra")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("shadow").executes(UmbraCommand::shadow))
                .then(Commands.literal("steps").executes(UmbraCommand::steps))
                .then(Commands.literal("mimic").executes(UmbraCommand::mimic))
                .then(Commands.literal("darkness").executes(UmbraCommand::darkness))
                .then(Commands.literal("maze").executes(UmbraCommand::maze))
                .then(Commands.literal("home").executes(UmbraCommand::home))
                .then(Commands.literal("status").executes(UmbraCommand::status)));
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
        BlockPos center = BlockPos.containing(player.position().add(dir.x * 14, 0, dir.z * 14));
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
        String text = "shadow[" + ShadowManager.debug(player) + "] steps[" + StepsManager.debug(player) + "] home="
                + (home == null ? "-" : home.toShortString());
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static void reply(CommandContext<CommandSourceStack> ctx, String key) {
        ctx.getSource().sendSuccess(() -> Component.translatable(key), false);
    }
}
