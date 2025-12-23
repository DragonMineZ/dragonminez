package com.dragonminez.server.commands;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BonusCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzbonus")
                .requires(source -> source.hasPermission(2))

                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("add")
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .then(Commands.argument("operation", StringArgumentType.word())
                                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                        .then(Commands.argument("bonusName", StringArgumentType.word())
                                                                .executes(ctx -> addBonus(ctx.getSource(),
                                                                        EntityArgument.getPlayer(ctx, "player"),
                                                                        StringArgumentType.getString(ctx, "stat"),
                                                                        StringArgumentType.getString(ctx, "operation"),
                                                                        DoubleArgumentType.getDouble(ctx, "value"),
                                                                        StringArgumentType.getString(ctx, "bonusName"))))))))

                        .then(Commands.literal("clear")
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .executes(ctx -> clearStat(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "stat")))
                                        .then(Commands.argument("bonusName", StringArgumentType.word())
                                                .executes(ctx -> clearBonus(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "stat"),
                                                        StringArgumentType.getString(ctx, "bonusName")))))))
        );
    }

    private static int addBonus(CommandSourceStack source, ServerPlayer player, String stat, String operation, double value, String bonusName) {
        final String finalStat = stat.toUpperCase();

        if (!finalStat.equals("STR") && !finalStat.equals("SKP") && !finalStat.equals("RES") &&
            !finalStat.equals("VIT") && !finalStat.equals("PWR") && !finalStat.equals("ENE") && !finalStat.equals("ALL")) {
            source.sendFailure(Component.translatable("command.dragonminez.bonus.invalid_stat"));
            return 0;
        }

        if (!operation.equals("+") && !operation.equals("-") && !operation.equals("*")) {
            source.sendFailure(Component.translatable("command.dragonminez.bonus.invalid_operation"));
            return 0;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (finalStat.equals("ALL")) {
                data.getBonusStats().addBonus("STR", bonusName, operation, value);
                data.getBonusStats().addBonus("SKP", bonusName, operation, value);
                data.getBonusStats().addBonus("RES", bonusName, operation, value);
                data.getBonusStats().addBonus("VIT", bonusName, operation, value);
                data.getBonusStats().addBonus("PWR", bonusName, operation, value);
                data.getBonusStats().addBonus("ENE", bonusName, operation, value);
                source.sendSuccess(() -> Component.translatable("command.dragonminez.bonus.add.all.success",
                        bonusName, operation + value, player.getName().getString()), true);
            } else {
                data.getBonusStats().addBonus(finalStat, bonusName, operation, value);
                source.sendSuccess(() -> Component.translatable("command.dragonminez.bonus.add.success",
                        bonusName, finalStat, operation + value, player.getName().getString()), true);
            }

            NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
        });

        return 1;
    }

    private static int clearBonus(CommandSourceStack source, ServerPlayer player, String stat, String bonusName) {
        final String finalStat = stat.toUpperCase();

        if (!finalStat.equals("STR") && !finalStat.equals("SKP") && !finalStat.equals("RES") &&
            !finalStat.equals("VIT") && !finalStat.equals("PWR") && !finalStat.equals("ENE") && !finalStat.equals("ALL")) {
            source.sendFailure(Component.translatable("command.dragonminez.bonus.invalid_stat"));
            return 0;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (finalStat.equals("ALL")) {
                data.getBonusStats().removeBonus("STR", bonusName);
                data.getBonusStats().removeBonus("SKP", bonusName);
                data.getBonusStats().removeBonus("RES", bonusName);
                data.getBonusStats().removeBonus("VIT", bonusName);
                data.getBonusStats().removeBonus("PWR", bonusName);
                data.getBonusStats().removeBonus("ENE", bonusName);
                source.sendSuccess(() -> Component.translatable("command.dragonminez.bonus.clear.bonus.all.success",
                        bonusName, player.getName().getString()), true);
            } else {
                data.getBonusStats().removeBonus(finalStat, bonusName);
                source.sendSuccess(() -> Component.translatable("command.dragonminez.bonus.clear.bonus.success",
                        bonusName, finalStat, player.getName().getString()), true);
            }

            NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
        });

        return 1;
    }

    private static int clearStat(CommandSourceStack source, ServerPlayer player, String stat) {
        final String finalStat = stat.toUpperCase();

        if (!finalStat.equals("STR") && !finalStat.equals("SKP") && !finalStat.equals("RES") &&
            !finalStat.equals("VIT") && !finalStat.equals("PWR") && !finalStat.equals("ENE") && !finalStat.equals("ALL")) {
            source.sendFailure(Component.translatable("command.dragonminez.bonus.invalid_stat"));
            return 0;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (finalStat.equals("ALL")) {
                data.getBonusStats().clearAllStats();
                source.sendSuccess(() -> Component.translatable("command.dragonminez.bonus.clear.all.success",
                        player.getName().getString()), true);
            } else {
                data.getBonusStats().clearAll(finalStat);
                source.sendSuccess(() -> Component.translatable("command.dragonminez.bonus.clear.stat.success",
                        finalStat, player.getName().getString()), true);
            }

            NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
        });

        return 1;
    }
}

