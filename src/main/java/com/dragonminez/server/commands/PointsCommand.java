package com.dragonminez.server.commands;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PointsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzpoints")
                .requires(source -> DMZPermissions.check(source, DMZPermissions.POINTS_INFO_SELF, DMZPermissions.POINTS_INFO_OTHERS))

                // set <amount> [player]
                .then(Commands.literal("set")
                        .requires(source -> DMZPermissions.check(source, DMZPermissions.POINTS_SET_SELF, DMZPermissions.POINTS_SET_OTHERS))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> setPoints(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "amount")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.POINTS_SET_OTHERS))
                                        .executes(ctx -> setPoints(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount"))))))

                // add <amount> [player]
                .then(Commands.literal("add")
                        .requires(source -> DMZPermissions.check(source, DMZPermissions.POINTS_ADD_SELF, DMZPermissions.POINTS_ADD_OTHERS))
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(ctx -> addPoints(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "amount")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.POINTS_ADD_OTHERS))
                                        .executes(ctx -> addPoints(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount"))))))

                // remove <amount> [player]
                .then(Commands.literal("remove")
                        .requires(source -> DMZPermissions.check(source, DMZPermissions.POINTS_REMOVE_SELF, DMZPermissions.POINTS_REMOVE_OTHERS))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> removePoints(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "amount")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.POINTS_REMOVE_OTHERS))
                                        .executes(ctx -> removePoints(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount"))))))
        );
    }

    private static int setPoints(CommandSourceStack source, ServerPlayer player, int amount) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            data.getResources().setTrainingPoints(amount);
            NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
        });

        source.sendSuccess(() -> Component.translatable("command.dragonminez.points.set.success",
                amount, player.getName().getString()), true);
        return 1;
    }

    private static int addPoints(CommandSourceStack source, ServerPlayer player, int amount) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            data.getResources().addTrainingPoints(amount);
            NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
        });

        String key = amount >= 0 ? "command.dragonminez.points.add.success" : "command.dragonminez.points.remove.success";
        source.sendSuccess(() -> Component.translatable(key,
                Math.abs(amount), player.getName().getString()), true);
        return 1;
    }

    private static int removePoints(CommandSourceStack source, ServerPlayer player, int amount) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            int currentPoints = data.getResources().getTrainingPoints();
            int newPoints = Math.max(0, currentPoints - amount);
            data.getResources().setTrainingPoints(newPoints);
            NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
        });

        source.sendSuccess(() -> Component.translatable("command.dragonminez.points.remove.success",
                amount, player.getName().getString()), true);
        return 1;
    }
}
