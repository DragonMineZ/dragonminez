package com.yuseix.dragonminez.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;

public class ZPointsCommand {

    public ZPointsCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("dmzpoints")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("points", IntegerArgumentType.integer())
                                .executes(commandContext -> {
                                    CommandSourceStack source = commandContext.getSource();
                                    if (source.isPlayer()) {
                                        return setPuntos(
                                                Collections.singleton(source.getPlayerOrException()),
                                                IntegerArgumentType.getInteger(commandContext, "points"),
                                                source
                                        );
                                    } else {
                                        throw new SimpleCommandExceptionType(Component.literal("Please specify a player first.")).create();
                                    }
                                })
                                .then(Commands.argument("player", EntityArgument.players())
                                        .executes(commandContext -> setPuntos(
                                                EntityArgument.getPlayers(commandContext, "player"),
                                                IntegerArgumentType.getInteger(commandContext, "points"),
                                                commandContext.getSource()
                                        ))
                                )
                        ))
                .then(Commands.literal("add")
                        .then(Commands.argument("points", IntegerArgumentType.integer())
                                .executes(commandContext -> {
                                    CommandSourceStack source = commandContext.getSource();
                                    if (source.isPlayer()) {
                                        return darPuntos(
                                                Collections.singleton(source.getPlayerOrException()),
                                                IntegerArgumentType.getInteger(commandContext, "points"),
                                                source
                                        );
                                    } else {
                                        throw new SimpleCommandExceptionType(Component.literal("Please specify a player first.")).create();
                                    }
                                })
                                .then(Commands.argument("player", EntityArgument.players())
                                        .executes(commandContext -> darPuntos(
                                                EntityArgument.getPlayers(commandContext, "player"),
                                                IntegerArgumentType.getInteger(commandContext, "points"),
                                                commandContext.getSource()
                                        ))
                                )
                        ))
                .then(Commands.literal("remove")
                        .then(Commands.argument("points", IntegerArgumentType.integer())
                                .executes(commandContext -> {
                                    CommandSourceStack source = commandContext.getSource();
                                    if (source.isPlayer()) {
                                        return removePuntos(
                                                Collections.singleton(source.getPlayerOrException()),
                                                IntegerArgumentType.getInteger(commandContext, "points"),
                                                source
                                        );
                                    } else {
                                        throw new SimpleCommandExceptionType(Component.literal("Please specify a player first.")).create();
                                    }
                                })
                                .then(Commands.argument("player", EntityArgument.players())
                                        .executes(commandContext -> removePuntos(
                                                EntityArgument.getPlayers(commandContext, "player"),
                                                IntegerArgumentType.getInteger(commandContext, "points"),
                                                commandContext.getSource()
                                        ))
                                )
                        ))
        );
    }

    private static int setPuntos(Collection<ServerPlayer> pPlayers, int puntos, CommandSourceStack source) {
        for (ServerPlayer player : pPlayers) {
            if (source.isPlayer() && pPlayers.size() == 1) {
                source.sendSystemMessage(Component.translatable("command.dmzpoints.self.set", puntos));
            } else {
                source.sendSystemMessage(Component.translatable("command.dmzpoints.set", player.getName(), puntos));
            }

            if (player != source.getPlayer()) {
                player.sendSystemMessage(Component.translatable("command.dmzpoints.set.target", puntos));
            }

            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> playerstats.setIntValue("tps", puntos));
        }
        return pPlayers.size();
    }

    private static int darPuntos(Collection<ServerPlayer> pPlayers, int puntos, CommandSourceStack source) throws CommandSyntaxException {
        for (ServerPlayer player : pPlayers) {
            if (pPlayers.size() == 1 && source.isPlayer()) {
                source.sendSystemMessage(Component.translatable("command.dmzpoints.self.add", puntos));
            } else {
                source.sendSystemMessage(Component.translatable("command.dmzpoints.add", puntos, player.getName()));
            }

            if (player != source.getPlayer()) {
                player.sendSystemMessage(Component.translatable("command.dmzpoints.add.target", puntos));
            }

            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> playerstats.addIntValue("tps", puntos));
        }
        return pPlayers.size();
    }

    private static int removePuntos(Collection<ServerPlayer> pPlayers, int puntos, CommandSourceStack source) throws CommandSyntaxException {
        for (ServerPlayer player : pPlayers) {
            if (pPlayers.size() == 1 && source.isPlayer()) {
                source.sendSystemMessage(Component.translatable("command.dmzpoints.self.remove", puntos));
            } else {
                source.sendSystemMessage(Component.translatable("command.dmzpoints.remove", puntos, player.getName()));
            }

            if (player != source.getPlayer()) {
                player.sendSystemMessage(Component.translatable("command.dmzpoints.remove.target", puntos));
            }

            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> playerstats.removeIntValue("tps", puntos));
        }
        return pPlayers.size();
    }
}
