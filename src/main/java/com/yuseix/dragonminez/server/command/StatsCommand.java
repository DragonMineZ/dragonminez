package com.yuseix.dragonminez.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yuseix.dragonminez.common.config.old.DMZGeneralConfig;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import com.yuseix.dragonminez.common.util.DMZDatos;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Collection;
import java.util.Collections;

public class StatsCommand {

    public StatsCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("dmzstats")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2))


                .then(Commands.literal("set")
                        .then(Commands.argument("stat", StringArgumentType.string())
                                .suggests((commandContext, suggestionsBuilder) -> {
                                    suggestionsBuilder.suggest("strength");
                                    suggestionsBuilder.suggest("defense");
                                    suggestionsBuilder.suggest("constitution");
                                    suggestionsBuilder.suggest("kipower");
                                    suggestionsBuilder.suggest("energy");
                                    suggestionsBuilder.suggest("all");
                                    return suggestionsBuilder.buildFuture();
                                })
                                .then(Commands.argument("quantity", StringArgumentType.string())
                                        .suggests((commandContext, suggestionsBuilder) -> {
                                            suggestionsBuilder.suggest("max");
                                            suggestionsBuilder.suggest("10");
                                            suggestionsBuilder.suggest("100");
                                            suggestionsBuilder.suggest("1000");
                                            return suggestionsBuilder.buildFuture();
                                        })
                                        .executes(commandContext -> setStat(commandContext, StringArgumentType.getString(commandContext, "stat"), StringArgumentType.getString(commandContext, "quantity"), Collections.singleton(commandContext.getSource().getPlayerOrException())))
                                        .then(Commands.argument("player", EntityArgument.players())
                                                .executes(commandContext -> setStat(commandContext, StringArgumentType.getString(commandContext, "stat"), StringArgumentType.getString(commandContext, "quantity"), EntityArgument.getPlayers(commandContext, "player")))
                                        )
                                )
                        )
                )

                .then(Commands.literal("add")
                        .then(Commands.argument("stat", StringArgumentType.string())
                                .suggests((commandContext, suggestionsBuilder) -> {
                                    suggestionsBuilder.suggest("strength");
                                    suggestionsBuilder.suggest("defense");
                                    suggestionsBuilder.suggest("constitution");
                                    suggestionsBuilder.suggest("kipower");
                                    suggestionsBuilder.suggest("energy");
                                    suggestionsBuilder.suggest("all");
                                    return suggestionsBuilder.buildFuture();
                                })
                                .then(Commands.argument("quantity", IntegerArgumentType.integer())
                                        .executes(commandContext -> addStat(commandContext, StringArgumentType.getString(commandContext, "stat"), IntegerArgumentType.getInteger(commandContext, "quantity"), Collections.singleton(commandContext.getSource().getPlayerOrException())))
                                        .then(Commands.argument("player", EntityArgument.players())
                                                .executes(commandContext -> addStat(commandContext, StringArgumentType.getString(commandContext, "stat"), IntegerArgumentType.getInteger(commandContext, "quantity"), EntityArgument.getPlayers(commandContext, "player")))
                                        )
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("stat", StringArgumentType.string())
                                .suggests((commandContext, suggestionsBuilder) -> {
                                    suggestionsBuilder.suggest("strength");
                                    suggestionsBuilder.suggest("defense");
                                    suggestionsBuilder.suggest("constitution");
                                    suggestionsBuilder.suggest("kipower");
                                    suggestionsBuilder.suggest("energy");
                                    suggestionsBuilder.suggest("all");
                                    return suggestionsBuilder.buildFuture();
                                })
                                .then(Commands.argument("quantity", IntegerArgumentType.integer())
                                        .executes(commandContext -> removeStat(commandContext, StringArgumentType.getString(commandContext, "stat"), IntegerArgumentType.getInteger(commandContext, "quantity"), Collections.singleton(commandContext.getSource().getPlayerOrException())))
                                        .then(Commands.argument("player", EntityArgument.players())
                                                .executes(commandContext -> removeStat(commandContext, StringArgumentType.getString(commandContext, "stat"), IntegerArgumentType.getInteger(commandContext, "quantity"), EntityArgument.getPlayers(commandContext, "player")))
                                        )
                                )
                        )
                )
        );

    }

    private int removeStat(CommandContext<CommandSourceStack> context, String stat, int cantidad, Collection<ServerPlayer> players) {
        CommandSourceStack source = context.getSource();
        for (ServerPlayer player : players) {
            DMZDatos dmzdatos = new DMZDatos();

            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(stats -> {
                int cantidadFinal = Math.min(cantidad, DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get());

                switch (stat) {
                    case "strength":
                        stats.removeStat("STR", cantidadFinal);
                        break;
                    case "defense":
                        stats.removeStat("DEF", cantidadFinal);
                        break;
                    case "constitution":
                        stats.removeStat("CON", cantidadFinal);
                        stats.setIntValue("curstam", dmzdatos.calcStamina(stats));
                        int nuevaMaxVida = dmzdatos.calcConstitution(stats);
                        player.setHealth((float) nuevaMaxVida);
                        break;
                    case "kipower":
                        stats.removeStat("PWR", cantidadFinal);
                        break;
                    case "energy":
                        stats.removeStat("ENE", cantidadFinal);
                        stats.setIntValue("curenergy", dmzdatos.calcEnergy(stats));
                        break;
                    case "all":
                        stats.removeStat("STR", cantidadFinal);
                        stats.removeStat("DEF", cantidadFinal);
                        stats.removeStat("CON", cantidadFinal);
                        stats.removeStat("PWR", cantidadFinal);
                        stats.removeStat("ENE", cantidadFinal);

                        stats.setIntValue("curstam", dmzdatos.calcStamina(stats));
                        nuevaMaxVida = dmzdatos.calcConstitution(stats);
                        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(nuevaMaxVida);
                        player.setHealth((float) nuevaMaxVida);

                        stats.setIntValue("curenergy", dmzdatos.calcEnergy(stats));
                        break;
                    default:
                        source.sendSystemMessage(Component.translatable("command.dmzstats.error").withStyle(ChatFormatting.RED));
                        return;
                }

                // Mensaje para el que ejecuta el comando
                if ((source.isPlayer() && source.getPlayer() != player) || !source.isPlayer()) {
                    if (!stat.equals("all")) {
                        source.sendSystemMessage(
                                Component.translatable("command.dmzstats.done").append(" ")
                                        .append(Component.translatable("command.dmzstats." + stat)).append(" ")
                                        .append(player.getName()).append(" ")
                                        .append(Component.translatable("command.dmzstats.decreased")).append(" ")
                                        .append(String.valueOf(cantidadFinal)).append(Component.literal("."))
                        );
                    } else source.sendSystemMessage(Component.translatable("command.dmzstats.done").append(" ")
                            .append(Component.translatable("command.dmzstats.removeall", player.getName(), cantidadFinal)));
                }
                String statName = "";
                switch (stat) {
                    case "strength" -> statName = "STR";
                    case "defense" -> statName = "DEF";
                    case "constitution" -> statName = "CON";
                    case "kipower" -> statName = "PWR";
                    case "energy" -> statName = "ENE";
                }

                // Mensaje para el jugador que recibe los puntos
                if (!stat.equals("all")) {
                    player.sendSystemMessage(Component.translatable("command.dmzstats.remove.target", statName, cantidadFinal));
                } else player.sendSystemMessage(Component.translatable("command.dmzstats.removeall.target", cantidadFinal));
            });
        }
        return players.size();
    }

    private int addStat(CommandContext<CommandSourceStack> context, String stat, int cantidad, Collection<ServerPlayer> players) {
        CommandSourceStack source = context.getSource();
        for (ServerPlayer player : players) {
            DMZDatos dmzdatos = new DMZDatos();

            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(stats -> {
                int cantidadFinal = Math.min(cantidad, DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get());

                switch (stat) {
                    case "strength":
                        stats.addStat("STR", cantidadFinal);
                        break;
                    case "defense":
                        stats.addStat("DEF", cantidadFinal);
                        break;
                    case "constitution":
                        stats.addStat("CON", cantidadFinal);
                        stats.setIntValue("curstam", dmzdatos.calcStamina(stats));
                        int nuevaMaxVida = dmzdatos.calcConstitution(stats);
                        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(nuevaMaxVida);
                        player.setHealth((float) nuevaMaxVida);
                        break;
                    case "kipower":
                        stats.addStat("PWR", cantidadFinal);
                        break;
                    case "energy":
                        stats.addStat("ENE", cantidadFinal);
                        stats.setIntValue("curenergy", dmzdatos.calcEnergy(stats));
                        break;
                    case "all":
                        stats.addStat("STR", cantidadFinal);
                        stats.addStat("DEF", cantidadFinal);
                        stats.addStat("CON", cantidadFinal);
                        stats.addStat("PWR", cantidadFinal);
                        stats.addStat("ENE", cantidadFinal);

                        stats.setIntValue("curstam", dmzdatos.calcStamina(stats));
                        nuevaMaxVida = dmzdatos.calcConstitution(stats);
                        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(nuevaMaxVida);
                        player.setHealth((float) nuevaMaxVida);

                        stats.setIntValue("curenergy", dmzdatos.calcEnergy(stats));
                        break;
                    default:
                        source.sendSystemMessage(Component.translatable("command.dmzstats.error").withStyle(ChatFormatting.RED));
                        return;
                }

                // Mensaje para el que ejecuta el comando
                if ((source.isPlayer() && source.getPlayer() != player) || !source.isPlayer()) {
                    if (!stat.equals("all")) {
                        source.sendSystemMessage(
                                Component.translatable("command.dmzstats.done").append(" ")
                                        .append(Component.translatable("command.dmzstats." + stat)).append(" ")
                                        .append(player.getName()).append(" ")
                                        .append(Component.translatable("command.dmzstats.increased")).append(" ")
                                        .append(String.valueOf(cantidadFinal)).append(Component.literal("."))
                        );
                    } else source.sendSystemMessage(Component.translatable("command.dmzstats.done").append(" ")
                            .append(Component.translatable("command.dmzstats.addall", player.getName(), cantidadFinal)));
                }

                String statName = "";
                switch (stat) {
                    case "strength" -> statName = "STR";
                    case "defense" -> statName = "DEF";
                    case "constitution" -> statName = "CON";
                    case "kipower" -> statName = "PWR";
                    case "energy" -> statName = "ENE";
                }

                // Mensaje para el jugador que recibe los puntos
                if (!stat.equals("all")) {
                    player.sendSystemMessage(Component.translatable("command.dmzstats.add.target", statName, cantidadFinal));
                } else player.sendSystemMessage(Component.translatable("command.dmzstats.addall.target", cantidadFinal));
            });
        }
        return players.size();
    }

    private int setStat(CommandContext<CommandSourceStack> context, String stat, String cantidad, Collection<ServerPlayer> players) {
        CommandSourceStack source = context.getSource();
        int cant = cantidad.equalsIgnoreCase("max") ? DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get() : Integer.parseInt(cantidad);
        if (cant < 5) cant = 5; // Mínimo de stats

        for (ServerPlayer player : players) {
            DMZDatos dmzdatos = new DMZDatos();
            int cantidadFinal = cant;

            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(stats -> {
                switch (stat) {
                    case "strength":
                        stats.setStat("STR", cantidadFinal);
                        break;
                    case "defense":
                        stats.setStat("DEF", cantidadFinal);
                        break;
                    case "constitution":
                        stats.setStat("CON", cantidadFinal);
                        stats.setIntValue("curstam", dmzdatos.calcStamina(stats));
                        int nuevaMaxVida = dmzdatos.calcConstitution(stats);
                        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(nuevaMaxVida);
                        player.setHealth((float) nuevaMaxVida);
                        break;
                    case "kipower":
                        stats.setStat("PWR", cantidadFinal);
                        break;
                    case "energy":
                        stats.setStat("ENE", cantidadFinal);
                        stats.setIntValue("curenergy", dmzdatos.calcEnergy(stats));
                        break;
                    case "all":
                        stats.setStat("STR", cantidadFinal);
                        stats.setStat("DEF", cantidadFinal);
                        stats.setStat("CON", cantidadFinal);
                        stats.setStat("PWR", cantidadFinal);
                        stats.setStat("ENE", cantidadFinal);

                        stats.setIntValue("curstam", dmzdatos.calcStamina(stats));
                        nuevaMaxVida = dmzdatos.calcConstitution(stats);
                        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(nuevaMaxVida);
                        player.setHealth((float) nuevaMaxVida);

                        stats.setIntValue("curenergy", dmzdatos.calcEnergy(stats));
                        break;
                    default:
                        source.sendSystemMessage(Component.translatable("command.dmzstats.error").withStyle(ChatFormatting.RED));
                        return;
                }

                // Mensaje para el que ejecuta el comando
                if ((source.isPlayer() && source.getPlayer() != player) || !source.isPlayer()) {
                    if (!stat.equals("all")) {
                        source.sendSystemMessage(
                                Component.translatable("command.dmzstats.done").append(" ")
                                        .append(Component.translatable("command.dmzstats." + stat)).append(" ")
                                        .append(player.getName()).append(" ")
                                        .append(Component.translatable("command.dmzstats.nowis")).append(" ")
                                        .append(String.valueOf(cantidadFinal)).append(Component.literal("."))
                        );
                    } else source.sendSystemMessage(Component.translatable("command.dmzstats.done").append(" ")
                            .append(Component.translatable("command.dmzstats.setall", player.getName(), cantidadFinal)));
                }
                String statName = "";
                switch (stat) {
                    case "strength" -> statName = "STR";
                    case "defense" -> statName = "DEF";
                    case "constitution" -> statName = "CON";
                    case "kipower" -> statName = "PWR";
                    case "energy" -> statName = "ENE";
                }

                if (!stat.equals("all")) {
                    // Mensaje para el jugador que recibe los puntos
                    player.sendSystemMessage(Component.translatable("command.dmzstats.set.target", statName, cantidadFinal));
                } else player.sendSystemMessage(Component.translatable("command.dmzstats.setall.target", cantidadFinal));
            });
        }
        return players.size();
    }
}
