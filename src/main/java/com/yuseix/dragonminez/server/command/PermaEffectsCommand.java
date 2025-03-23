package com.yuseix.dragonminez.server.command;

import com.mojang.brigadier.CommandDispatcher;
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

public class PermaEffectsCommand {

	// Lista de efectos permanentes válidos
	private static final Set<String> VALID_PERMA_EFFECTS = Set.of("majin");

	public PermaEffectsCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

		dispatcher.register(Commands.literal("dmzpermaeffects")
				.requires(commandSourceStack -> commandSourceStack.hasPermission(2))
				.then(Commands.literal("give")
						.then(Commands.argument("effect", StringArgumentType.string())
								.suggests((commandContext, suggestionsBuilder) -> {
									// Sugerir todos los efectos válidos al escribir el comando
									for (String effect : VALID_PERMA_EFFECTS) {
										suggestionsBuilder.suggest(effect);
									}
									return suggestionsBuilder.buildFuture();  // Completa la sugerencia
								})
								.executes(commandContext -> givePermaEffect(
										Collections.singleton(commandContext.getSource().getPlayerOrException()),
										StringArgumentType.getString(commandContext, "effect"),
										commandContext.getSource())
								)
								.then(Commands.argument("player", EntityArgument.players())
										.executes(commandContext -> givePermaEffect(
												EntityArgument.getPlayers(commandContext, "player"),
												StringArgumentType.getString(commandContext, "effect"),
												commandContext.getSource())
										))
						)
				)
				.then(Commands.literal("take")
						.then(Commands.argument("effect", StringArgumentType.string())
								.suggests((commandContext, suggestionsBuilder) -> {
									// Sugerir todos los efectos válidos al escribir el comando
									for (String effect : VALID_PERMA_EFFECTS) {
										suggestionsBuilder.suggest(effect);
									}
									return suggestionsBuilder.buildFuture();  // Completa la sugerencia
								})
								.executes(commandContext -> takePermaEffect(
										Collections.singleton(commandContext.getSource().getPlayerOrException()),
										StringArgumentType.getString(commandContext, "effect"),
										commandContext.getSource())
								)
								.then(Commands.argument("player", EntityArgument.players())
										.executes(commandContext -> takePermaEffect(
												EntityArgument.getPlayers(commandContext, "player"),
												StringArgumentType.getString(commandContext, "effect"),
												commandContext.getSource())
										)
								)
						)
						.then(Commands.literal("all")
								.executes(commandContext -> takeAllPermaEffects(
										Collections.singleton(commandContext.getSource().getPlayerOrException()), commandContext.getSource()
								))
								.then(Commands.argument("player", EntityArgument.players())
										.executes(commandContext -> takeAllPermaEffects(
												EntityArgument.getPlayers(commandContext, "player"),
												commandContext.getSource())
										)
								)
						)
				)
		);
	}

	// Comando para dar efectos permanentes
	private static int givePermaEffect(Collection<ServerPlayer> players, String effectName, CommandSourceStack source) {
		// Verifica si el efecto es válido
		if (!VALID_PERMA_EFFECTS.contains(effectName)) {
			// Si el efecto no es válido, muestra un mensaje de error con los efectos válidos
			for (ServerPlayer player : players) {
				if ((source.isPlayer() && player != source.getPlayer()) || !source.isPlayer())
					player.sendSystemMessage(Component.translatable("command.dmzeffects.invalid_effect").append(effectName).append("\n")
							.append(Component.translatable("command.dmzeffects.valid_effects").append(String.join(", ", VALID_PERMA_EFFECTS))));
			}
			return 0; // No ejecuta la acción
		}

		boolean isTargetAll = players.size() > 1;
		for (ServerPlayer player : players) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
				playerstats.addDMZPermanentEffect(effectName, true);
				if (effectName.equals("majin")) playerstats.setIntValue("alignment", 0);
				player.sendSystemMessage(Component.translatable("command.dmzeffects.received", effectName));
			});
		}

		if (isTargetAll) {
			source.sendSystemMessage(Component.translatable("command.dmzeffects.give_all", effectName));
		} else {
			ServerPlayer target = players.iterator().next();
			source.sendSystemMessage(Component.translatable("command.dmzeffects.give", effectName)
					.append(" ")
					.append(Component.translatable("command.dmz.to"))
					.append(target.getName()));
		}
		return players.size();
	}

	// Comando para quitar efectos permanentes
	private static int takePermaEffect(Collection<ServerPlayer> players, String effectName, CommandSourceStack source) {
		// Verifica si el efecto es válido
		if (!VALID_PERMA_EFFECTS.contains(effectName)) {
			// Si el efecto no es válido, muestra un mensaje de error con los efectos válidos
			for (ServerPlayer player : players) {
				if ((source.isPlayer() && player != source.getPlayer()) || !source.isPlayer())
					player.sendSystemMessage(Component.translatable("command.dmzeffects.invalid_effect").append(effectName).append("\n")
							.append(Component.translatable("command.dmzeffects.valid_effects").append(String.join(", ", VALID_PERMA_EFFECTS))));
			}
			return 0; // No ejecuta la acción
		}

		boolean isTargetAll = players.size() > 1;
		for (ServerPlayer player : players) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> playerstats.removePermanentEffect(effectName));
			player.sendSystemMessage(Component.translatable("command.dmzeffects.removed", effectName));
		}

		if (isTargetAll) {
			source.sendSystemMessage(Component.translatable("command.dmzeffects.take_all", effectName));
		} else {
			ServerPlayer target = players.iterator().next();
			source.sendSystemMessage(Component.translatable("command.dmzeffects.take", effectName)
					.append(" ")
					.append(Component.translatable("command.dmz.from"))
					.append(target.getName()));
		}
		return players.size();
	}

	// Comando para quitar todos los efectos permanentes
	private static int takeAllPermaEffects(Collection<ServerPlayer> players, CommandSourceStack source) {
		boolean isTargetAll = players.size() > 1;
		for (ServerPlayer player : players) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
				for (String effectName : VALID_PERMA_EFFECTS) playerstats.removePermanentEffect(effectName);
				player.sendSystemMessage(Component.translatable("command.dmzeffects.removed_all"));
			});
		}

		if (isTargetAll) {
			source.sendSystemMessage(Component.translatable("command.dmzeffects.perma.take_all_from_all"));
		} else {
			ServerPlayer target = players.iterator().next();
			source.sendSystemMessage(Component.translatable("command.dmzeffects.perma.take.all")
					.append(Component.translatable("command.dmz.from"))
					.append(target.getName()));
		}
		return players.size();
	}
}