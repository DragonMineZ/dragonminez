package com.yuseix.dragonminez.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class DMZTempEffectsCommand {

	// Lista de efectos temporales válidos
	private static final Set<String> VALID_TEMP_EFFECTS = Set.of("mightfruit");

	public DMZTempEffectsCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

		dispatcher.register(Commands.literal("dmztempeffects")
				.requires(commandSourceStack -> commandSourceStack.hasPermission(2))
				.then(Commands.literal("give")
						.then(Commands.argument("effect", StringArgumentType.string())
								.suggests((commandContext, suggestionsBuilder) -> {
									// Sugerir todos los efectos válidos al escribir el comando
									for (String effect : VALID_TEMP_EFFECTS) {
										suggestionsBuilder.suggest(effect);
									}
									return suggestionsBuilder.buildFuture();  // Completa la sugerencia
								})
								.then(Commands.argument("seconds", IntegerArgumentType.integer(1))
										.executes(commandContext -> giveTempEffect(
												Collections.singleton(commandContext.getSource().getPlayerOrException()),
												StringArgumentType.getString(commandContext, "effect"),
												IntegerArgumentType.getInteger(commandContext, "seconds"))
										)
										.then(Commands.argument("player", EntityArgument.players())
												.executes(commandContext -> giveTempEffect(
														EntityArgument.getPlayers(commandContext, "player"),
														StringArgumentType.getString(commandContext, "effect"),
														IntegerArgumentType.getInteger(commandContext, "seconds"))
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
									return suggestionsBuilder.buildFuture();  // Completa la sugerencia
								})
								.executes(commandContext -> takeTempEffect(
										Collections.singleton(commandContext.getSource().getPlayerOrException()),
										StringArgumentType.getString(commandContext, "effect"))
								)
								.then(Commands.argument("player", EntityArgument.players())
										.executes(commandContext -> takeTempEffect(
												EntityArgument.getPlayers(commandContext, "player"),
												StringArgumentType.getString(commandContext, "effect"))
										)
								)
						)
						.then(Commands.literal("all")
								.executes(commandContext -> takeAllTempEffects(
										Collections.singleton(commandContext.getSource().getPlayerOrException())
								))
								.then(Commands.argument("player", EntityArgument.players())
										.executes(commandContext -> takeAllTempEffects(
												EntityArgument.getPlayers(commandContext, "player")
										))
								)
						)
				)
		);
	}

	// Comando para dar efectos temporales
	private static int giveTempEffect(Collection<ServerPlayer> players, String effectName, int seconds) {
		// Verifica si el efecto es válido
		if (!VALID_TEMP_EFFECTS.contains(effectName)) {
			// Si el efecto no es válido, muestra un mensaje de error
			for (ServerPlayer player : players) {
				player.sendSystemMessage(Component.translatable("command.dmzeffects.invalid_effect").append(effectName).append("\n")
						.append(Component.translatable("command.dmzeffects.valid_effects").append(String.join(", ", VALID_TEMP_EFFECTS))));
			}
			return 0; // No ejecuta la acción
		}

		int newSeconds = seconds * 20;

		for (ServerPlayer player : players) {
			player.sendSystemMessage(Component.translatable("command.dmzeffects.give").append(effectName).append(" ")
					.append(Component.translatable("command.dmzeffects.duration").append(String.valueOf(seconds)).append("s "))
					.append(Component.translatable("command.dmz.to")).append(player.getName()));
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> playerstats.addDMZTemporalEffect(effectName, newSeconds));
		}
		return players.size();
	}

	// Comando para quitar efectos temporales
	private static int takeTempEffect(Collection<ServerPlayer> players, String effectName) {
		// Verifica si el efecto es válido
		if (!VALID_TEMP_EFFECTS.contains(effectName)) {
			// Si el efecto no es válido, muestra un mensaje de error
			for (ServerPlayer player : players) {
				player.sendSystemMessage(Component.translatable("command.dmzeffects.invalid_effect").append(effectName).append("\n")
						.append(Component.translatable("command.dmzeffects.valid_effects").append(String.join(", ", VALID_TEMP_EFFECTS))));
			}
			return 0; // No ejecuta la acción
		}

		for (ServerPlayer player : players) {
			player.sendSystemMessage(Component.translatable("command.dmzeffects.take").append(effectName + " ")
					.append(Component.translatable("command.dmz.to")).append(player.getName()));
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> playerstats.removeTemporalEffect(effectName));
		}
		return players.size();
	}

	// Comando para quitar todos los efectos temporales
	private static int takeAllTempEffects(Collection<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			player.sendSystemMessage(Component.translatable("command.dmzeffects.temp.take.all")
					.append(Component.translatable("command.dmz.to")).append(player.getName()));
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
				for (String effectName : VALID_TEMP_EFFECTS) {
					playerstats.removeTemporalEffect(effectName);
				}
			});
		}
		return players.size();
	}
}