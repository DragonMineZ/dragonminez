package com.yuseix.dragonminez.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import com.yuseix.dragonminez.common.stats.forms.FormsData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class SuperFormCommand {

	// Lista de habilidades válidas con sus límites de nivel
	private static final Map<String, Integer> VALID_FORMS_LIST = Map.of(
			"super_form", 16 //Este max level es el máximo valor que se puede ingresar, pero es modificado luego según la raza.
	);

	public SuperFormCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

		dispatcher.register(Commands.literal("dmzforms")
				.requires(commandSourceStack -> commandSourceStack.hasPermission(2))
				.then(Commands.literal("give")
						.then(Commands.argument("form_id", StringArgumentType.string())
								.suggests((commandContext, suggestionsBuilder) -> {
									for (String skill : VALID_FORMS_LIST.keySet()) {
										suggestionsBuilder.suggest(skill);
									}
									return suggestionsBuilder.buildFuture();
								})
								.executes(commandContext -> giveSkill(
										Collections.singleton(commandContext.getSource().getPlayerOrException()),
										StringArgumentType.getString(commandContext, "form_id"),
										1, // Nivel por defecto
										commandContext.getSource()
								))
								.then(Commands.argument("level", IntegerArgumentType.integer(1))
										.executes(commandContext -> giveSkill(
												Collections.singleton(commandContext.getSource().getPlayerOrException()),
												StringArgumentType.getString(commandContext, "form_id"),
												IntegerArgumentType.getInteger(commandContext, "level"),
												commandContext.getSource()
										))
										.then(Commands.argument("player", EntityArgument.players())
												.executes(commandContext -> giveSkill(
														EntityArgument.getPlayers(commandContext, "player"),
														StringArgumentType.getString(commandContext, "form_id"),
														IntegerArgumentType.getInteger(commandContext, "level"),
														commandContext.getSource()
												))
										)
								)
						)
				)
				.then(Commands.literal("set")
						.then(Commands.argument("form_id", StringArgumentType.string())
								.suggests((commandContext, suggestionsBuilder) -> {
									for (String skill : VALID_FORMS_LIST.keySet()) {
										suggestionsBuilder.suggest(skill);
									}
									return suggestionsBuilder.buildFuture();
								})
								.then(Commands.argument("level", IntegerArgumentType.integer(1))
										.executes(commandContext -> setSkill(
												Collections.singleton(commandContext.getSource().getPlayerOrException()),
												StringArgumentType.getString(commandContext, "form_id"),
												IntegerArgumentType.getInteger(commandContext, "level"),
												commandContext.getSource()
										))
										.then(Commands.argument("player", EntityArgument.players())
												.executes(commandContext -> setSkill(
														EntityArgument.getPlayers(commandContext, "player"),
														StringArgumentType.getString(commandContext, "form_id"),
														IntegerArgumentType.getInteger(commandContext, "level"),
														commandContext.getSource()
												))
										)
								)
						)
				)
				.then(Commands.literal("take")
						.then(Commands.argument("form_id", StringArgumentType.string())
								.suggests((commandContext, suggestionsBuilder) -> {
									for (String skill : VALID_FORMS_LIST.keySet()) {
										suggestionsBuilder.suggest(skill);
									}
									return suggestionsBuilder.buildFuture();
								})
								.executes(commandContext -> takeSkill(
										Collections.singleton(commandContext.getSource().getPlayerOrException()),
										StringArgumentType.getString(commandContext, "form_id"),
										commandContext.getSource()
								))
								.then(Commands.argument("player", EntityArgument.players())
										.executes(commandContext -> takeSkill(
												EntityArgument.getPlayers(commandContext, "player"),
												StringArgumentType.getString(commandContext, "form_id"),
												commandContext.getSource()
										))
								)
						)
				)
		);
	}

	// Comando para dar habilidades
	private static int giveSkill(Collection<ServerPlayer> players, String skillName, int level, CommandSourceStack source) {
		if (!VALID_FORMS_LIST.containsKey(skillName)) {
			for (ServerPlayer player : players) {
				if ((source.isPlayer() && player != source.getPlayer()) || !source.isPlayer())
					player.sendSystemMessage(Component.translatable("command.dmzforms.invalid_skill").append(skillName).append("\n")
							.append(Component.translatable("command.dmzforms.valid_skills").append(String.join(", ", VALID_FORMS_LIST.keySet()))));
			}
			return 0;
		}


		for (ServerPlayer player : players) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
				int maxLevel = switch (playerstats.getIntValue("race")) {
					case 0 -> 3;
					case 1 -> 4;
					case 2 -> 4;
					case 3 -> 4;
					case 4 -> 6;
					case 5 -> 6;
					default -> 6;
				};

				int finalLevel = Math.max(1, Math.min(level, maxLevel));

				FormsData skill = new FormsData("dmz.dmzforms." + skillName + ".name",
						finalLevel);

				playerstats.addFormSkill(skillName, skill);
				Component skillNameLang = Component.translatable(skill.getName());

				if ((source.isPlayer() && player != source.getPlayer()) || !source.isPlayer())
					source.sendSystemMessage(Component.translatable("command.dmzforms.give")
							.append(Component.translatable(skill.getName())).append(" ") // Solo muestra el nombre de la habilidad
							.append(Component.translatable("command.dmz.to")).append(player.getName()));
				player.sendSystemMessage(Component.translatable("command.dmzforms.set.target", skillNameLang, finalLevel));
			});
		}
		return players.size();
	}

	// Comando para establecer nivel de habilidades
	private static int setSkill(Collection<ServerPlayer> players, String skillName, int level, CommandSourceStack source) {
		if (!VALID_FORMS_LIST.containsKey(skillName)) {
			for (ServerPlayer player : players) {
				if ((source.isPlayer() && player != source.getPlayer()) || !source.isPlayer())
					source.sendSystemMessage(Component.translatable("command.dmzforms.invalid_skill").append(skillName).append("\n")
							.append(Component.translatable("command.dmzforms.valid_skills").append(String.join(", ", VALID_FORMS_LIST.keySet()))));
			}
			return 0;
		}

		for (ServerPlayer player : players) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {

				int maxLevel = switch (playerstats.getIntValue("race")) {
					case 0 -> 4;
					case 1 -> 4;
					case 2 -> 4;
					case 3 -> 4;
					case 4 -> 6;
					case 5 -> 6;
					default -> 4;
				};

				int finalLevel = Math.max(1, Math.min(level, maxLevel));
				// Si la habilidad ya existe, se actualiza el nivel, si no se crea una nueva
				FormsData skill = playerstats.getFormSkill(skillName);
				if (skill != null) {
					skill.setLevel(finalLevel);
					Component skillNameLang = Component.translatable(skill.getName());
					if ((source.isPlayer() && player != source.getPlayer()) || !source.isPlayer())
						source.sendSystemMessage(Component.translatable("command.dmzforms.set")
								.append(Component.translatable(skill.getName()))
								.append(" (Lv ").append(Component.literal(String.valueOf(finalLevel))).append(") ") // Muestra el nivel
								.append(Component.translatable("command.dmz.to")).append(player.getName()));

					player.sendSystemMessage(Component.translatable("command.dmzforms.set.target", skillNameLang, finalLevel));
				} else {
					// Si la habilidad no existe, la creamos y le asignamos el nivel
					skill = new FormsData(
							"dmz.dmzforms." + skillName + ".name",
							finalLevel);

					playerstats.addFormSkill(skillName, skill);
					Component skillNameLang = Component.translatable(skill.getName());

					if ((source.isPlayer() && player != source.getPlayer()) || !source.isPlayer())
						source.sendSystemMessage(Component.translatable("command.dmzforms.give")
								.append(Component.translatable(skill.getName())) // Solo muestra el nombre
								.append(" (Lv ").append(Component.literal(String.valueOf(finalLevel))).append(") ") // Muestra el nivel
								.append(Component.translatable("command.dmz.to")).append(player.getName()));

					player.sendSystemMessage(Component.translatable("command.dmzforms.set.target", skillNameLang, finalLevel));
				}
			});
		}
		return players.size();
	}

	// Comando para quitar habilidades
	private static int takeSkill(Collection<ServerPlayer> players, String skillName, CommandSourceStack source) {
		if (!VALID_FORMS_LIST.containsKey(skillName)) {
			for (ServerPlayer player : players) {
				if ((source.isPlayer() && player != source.getPlayer()) || !source.isPlayer())
					player.sendSystemMessage(Component.translatable("command.dmzforms.invalid_skill").append(skillName).append("\n")
							.append(Component.translatable("command.dmzforms.valid_skills").append(String.join(", ", VALID_FORMS_LIST.keySet()))));
			}
			return 0;
		}

		for (ServerPlayer player : players) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
				FormsData skill = playerstats.getFormSkill(skillName);
				Component skillNameLang = Component.translatable("dmz.dmzforms." + skillName + ".name");
				if (skill != null) {
					if ((source.isPlayer() && player != source.getPlayer()) || !source.isPlayer())
						source.sendSystemMessage(Component.translatable("command.dmzskills.take")
								.append(skill.getName()).append(" ") // Solo muestra el nombre de la habilidad
								.append(Component.translatable("command.dmz.to")).append(player.getName()));
					player.sendSystemMessage(Component.translatable("command.dmzskills.take.target", skillNameLang));
					playerstats.removeFormSkill(skillName);
				} else {
					if ((source.isPlayer() && player != source.getPlayer()) || !source.isPlayer())
						source.sendSystemMessage(Component.translatable("command.dmzskills.not_found").append(skillName));
				}
			});
		}
		return players.size();
	}
}