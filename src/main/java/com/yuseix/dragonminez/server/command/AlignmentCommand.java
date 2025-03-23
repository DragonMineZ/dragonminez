package com.yuseix.dragonminez.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;

public class AlignmentCommand {

    public AlignmentCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("dmzalignment")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("points", IntegerArgumentType.integer())
                                .executes(commandContext -> setPuntos(
                                        Collections.singleton(commandContext.getSource().getPlayerOrException()),
                                        IntegerArgumentType.getInteger(commandContext, "points"),
                                        commandContext.getSource()))
                                .then(Commands.argument("player", EntityArgument.players())
                                        .executes(commandContext -> setPuntos(
                                                EntityArgument.getPlayers(commandContext, "player"),
                                                IntegerArgumentType.getInteger(commandContext, "points"),
                                                commandContext.getSource()))
                                )

                        ))
                .then(Commands.literal("add")
                        .then(Commands.argument("points", IntegerArgumentType.integer())
                                .executes(commandContext -> darPuntos(
                                        Collections.singleton(commandContext.getSource().getPlayerOrException()),
                                        IntegerArgumentType.getInteger(commandContext, "points"),
                                        commandContext.getSource()))
                                .then(Commands.argument("player", EntityArgument.players())
                                        .executes(commandContext -> darPuntos(
                                                EntityArgument.getPlayers(commandContext, "player"),
                                                IntegerArgumentType.getInteger(commandContext, "points"),
                                                commandContext.getSource()))
                                )

                        ))
                .then(Commands.literal("remove")
                        .then(Commands.argument("points", IntegerArgumentType.integer())
                                .executes(commandContext -> removePuntos(
                                        Collections.singleton(commandContext.getSource().getPlayerOrException()),
                                        IntegerArgumentType.getInteger(commandContext, "points"),
                                        commandContext.getSource()))
                                .then(Commands.argument("player", EntityArgument.players())
                                        .executes(commandContext -> removePuntos(
                                                EntityArgument.getPlayers(commandContext, "player"),
                                                IntegerArgumentType.getInteger(commandContext, "points"),
                                                commandContext.getSource()))
                                )

                        ))

        );

    }

    private static int setPuntos(Collection<ServerPlayer> pPlayers, int puntos, CommandSourceStack source) {
        for (ServerPlayer player : pPlayers) {
            if (puntos >= 100) puntos = 100;
            int finalPuntos = puntos;
            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> playerstats.setIntValue("alignment", finalPuntos));
            if ((source.isPlayer() && source.getPlayer() != player) || !source.isPlayer())
                source.sendSystemMessage(Component.translatable("command.dmzalignment.set", player.getName(), puntos));
            player.sendSystemMessage(Component.translatable("command.dmzalignment.set.target", puntos));
        }
        return pPlayers.size();
    }
    private static int darPuntos(Collection<ServerPlayer> pPlayers, int puntos, CommandSourceStack source) {
        for (ServerPlayer player : pPlayers) {
            if (puntos >= 100) puntos = 100;
            int finalPuntos = puntos;
            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> playerstats.addIntValue("alignment", finalPuntos));
            if ((source.isPlayer() && source.getPlayer() != player) || !source.isPlayer())
                source.sendSystemMessage(Component.translatable("command.dmzalignment.add", player.getName(), puntos));
            player.sendSystemMessage(Component.translatable("command.dmzalignment.add.target", puntos));
        }
        return pPlayers.size();
    }
    private static int removePuntos(Collection<ServerPlayer> pPlayers, int puntos, CommandSourceStack source) {
        for (ServerPlayer player : pPlayers) {
            if (puntos >= 100) puntos = 100;
            int finalPuntos = puntos;
            DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> playerstats.removeIntValue("alignment", finalPuntos));

            if ((source.isPlayer() && source.getPlayer() != player) || !source.isPlayer())
                source.sendSystemMessage(Component.translatable("command.dmzalignment.remove", player.getName(), puntos));
            player.sendSystemMessage(Component.translatable("command.dmzalignment.remove.target", puntos));
        }
        return pPlayers.size();
    }
}
