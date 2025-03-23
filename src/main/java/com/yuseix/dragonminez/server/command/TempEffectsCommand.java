package com.yuseix.dragonminez.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class TempEffectsCommand {

	// Lista de efectos temporales válidos
	private static final Set<String> VALID_TEMP_EFFECTS = Set.of("mightfruit");

	public TempEffectsCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

		dispatcher.register(Commands.literal("dmztempeffects")
				.requires(commandSourceStack -> commandSourceStack.hasPermission(2))
				.then(Commands.literal("give")
						.then(Commands.argument("effect", StringArgumentType.string())
								.suggests((commandContext, suggestionsBuilder) -> {
									// Sugerir todos los efectos válidos al escribir el comando
									for (String effect : VALID_TEMP_EFFECTS) {
										suggestionsBuilder.suggest(effect);
									}
									return suggestionsBuilder.buildFuture(); // Completa la sugerencia
								})
								.then(Commands.argument("seconds", IntegerArgumentType.integer(1))
										.executes(commandContext -> giveTempEffect(
												Collections.singleton(commandContext.getSource().getPlayerOrException()),
												StringArgumentType.getString(commandContext, "effect"),
												IntegerArgumentType.getInteger(commandContext, "seconds"),
												commandContext.getSource())
										)
										.then(Commands.argument("player", EntityArgument.players())
												.executes(commandContext -> giveTempEffect(
														EntityArgument.getPlayers(commandContext, "player"),
														StringArgumentType.getString(commandContext, "effect"),
														IntegerArgumentType.getInteger(commandContext, "seconds"),
														commandContext.getSource())
												)
										)
								)
						)
				)
				.then(Commands.literal("take")
						.then(Commands.argument("effect", StringArgumentType.string())
								.suggests((commandContext, suggestionsBuilder) -> {
									// Sugerir todos los efectos válidos al escribir el comando
									for (String effect : VALID_TEMP_EFFECTS) {
										suggestionsBuilder.suggest(effect);
									}
									return suggestionsBuilder.buildFuture(); // Completa la sugerencia
								})
								.executes(commandContext -> takeTempEffect(
										Collections.singleton(commandContext.getSource().getPlayerOrException()),
										StringArgumentType.getString(commandContext, "effect"),
										commandContext.getSource())
								)
								.then(Commands.argument("player", EntityArgument.players())
										.executes(commandContext -> takeTempEffect(
												EntityArgument.getPlayers(commandContext, "player"),
												StringArgumentType.getString(commandContext, "effect"),
												commandContext.getSource())
										)
								)
						)
						.then(Commands.literal("all")
								.executes(commandContext -> takeAllTempEffects(
										Collections.singleton(commandContext.getSource().getPlayerOrException()),
										commandContext.getSource()
								))
								.then(Commands.argument("player", EntityArgument.players())
										.executes(commandContext -> takeAllTempEffects(
												EntityArgument.getPlayers(commandContext, "player"),
												commandContext.getSource()
										))
								)
						)
				)
		);
	}

	// Comando para dar efectos temporales
	private static int giveTempEffect(Collection<ServerPlayer> players, String effectName, int seconds, CommandSourceStack source) {
		// Verifica si el efecto es válido
		if (!VALID_TEMP_EFFECTS.contains(effectName)) {
			source.sendSystemMessage(Component.translatable("command.dmzeffects.invalid_effect")
					.append(effectName).append("\n")
					.append(Component.translatable("command.dmzeffects.valid_effects")
							.append(String.join(", ", VALID_TEMP_EFFECTS))));
			return 0; // No ejecuta la acción
		}

		int newSeconds = seconds * 20;
		boolean isTargetAll = players.size() > 1;

		for (ServerPlayer player : players) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
				playerstats.addDMZTemporalEffect(effectName, newSeconds);
			});
			player.sendSystemMessage(Component.translatable("command.dmzeffects.receivedtemp", effectName, String.valueOf(newSeconds)));
		}

		if (isTargetAll) {
			source.sendSystemMessage(Component.translatable("command.dmzeffects.give_all", effectName)
					.append(" ")
					.append(Component.translatable("command.dmzeffects.duration")
							.append(String.valueOf(seconds)).append("s")));
		} else {
			ServerPlayer target = players.iterator().next();
			source.sendSystemMessage(Component.translatable("command.dmzeffects.give", effectName)
					.append(" ")
					.append(Component.translatable("command.dmzeffects.duration")
							.append(String.valueOf(seconds)).append("s "))
					.append(Component.translatable("command.dmz.to"))
					.append(target.getName()));
		}

		return players.size();
	}

	// Comando para quitar efectos temporales
	private static int takeTempEffect(Collection<ServerPlayer> players, String effectName, CommandSourceStack source) {
		// Verifica si el efecto es válido
		if (!VALID_TEMP_EFFECTS.contains(effectName)) {
			source.sendSystemMessage(Component.translatable("command.dmzeffects.invalid_effect")
					.append(effectName).append("\n")
					.append(Component.translatable("command.dmzeffects.valid_effects")
							.append(String.join(", ", VALID_TEMP_EFFECTS))));
			return 0; // No ejecuta la acción
		}

		boolean isTargetAll = players.size() > 1;

		for (ServerPlayer player : players) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
				playerstats.removeTemporalEffect(effectName);
			});
			player.sendSystemMessage(Component.translatable("command.dmzeffects.removed", effectName));
		}

		if (isTargetAll) {
			source.sendSystemMessage(Component.translatable("command.dmzeffects.temp.take_from_all", effectName));
		} else {
			ServerPlayer target = players.iterator().next();
			source.sendSystemMessage(Component.translatable("command.dmzeffects.take", effectName)
					.append(" ")
					.append(Component.translatable("command.dmz.from"))
					.append(target.getName()));
		}

		return players.size();
	}

	// Comando para quitar todos los efectos temporales
	private static int takeAllTempEffects(Collection<ServerPlayer> players, CommandSourceStack source) {
		boolean isTargetAll = players.size() > 1;

		for (ServerPlayer player : players) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
				for (String effectName : VALID_TEMP_EFFECTS) {
					playerstats.removeTemporalEffect(effectName);
				}
			});
		}

		if (isTargetAll) {
			source.sendSystemMessage(Component.translatable("command.dmzeffects.temp.take_all_from_all"));
		} else {
			ServerPlayer target = players.iterator().next();
			source.sendSystemMessage(Component.translatable("command.dmzeffects.temp.take.all")
					.append(Component.translatable("command.dmz.from"))
					.append(target.getName()));
		}

		return players.size();
	}
}
