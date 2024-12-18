package com.yuseix.dragonminez.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.yuseix.dragonminez.storyline.StorylineManager;
import com.yuseix.dragonminez.storyline.missions.Quest;
import com.yuseix.dragonminez.storyline.player.PlayerStorylineProvider;
import com.yuseix.dragonminez.storyline.sagas.Saga;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class StorylineCommand {

	private final StorylineManager storylineManager = new StorylineManager();

	public StorylineCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("storyline")
				// /storyline set quest <id> <true|false>
				.then(Commands.literal("set")
						.then(Commands.literal("quest")
								.then(Commands.argument("id", StringArgumentType.string())
										.suggests(this::suggestQuestIds)
										.then(Commands.argument("completed", BoolArgumentType.bool())
												.executes(context -> {
													String questId = StringArgumentType.getString(context, "id");
													boolean completed = BoolArgumentType.getBool(context, "completed");

													return setQuestCompletion(context.getSource(), questId, completed);
												})
										)
								)
						)
				)

				// /storyline get saga <id>
				.then(Commands.literal("get")
						.then(Commands.literal("allSagas")
								.executes(context -> {
									if (context.getSource().getPlayer() == null) {
										return 0;
									}
									context.getSource().getPlayer().getCapability(PlayerStorylineProvider.CAPABILITY).ifPresent(playerStoryline -> {
										for (Saga saga : playerStoryline.getAllSagas().values()) {
											context.getSource().sendSuccess(() -> Component.translatable("command.dmzstoryline.saga_info", saga.getName(), saga.getId()), false);
										}
									});
									return 1;
								})
						)
						.then(Commands.literal("saga")
								.then(Commands.argument("id", StringArgumentType.string())
										.suggests(this::suggestSagaIds)
										.executes(context -> {
											String sagaId = StringArgumentType.getString(context, "id");
											return getSagaInfo(context.getSource(), sagaId);
										})
								)
						)
						.then(Commands.literal("allQuests")
								.executes(context -> getAllQuests(context.getSource()))
						)
						.then(Commands.literal("quest")
								.then(Commands.argument("id", StringArgumentType.string())
										.suggests(this::suggestQuestIds)
										.executes(context -> {
											String questId = StringArgumentType.getString(context, "id");
											return getQuestInfo(context.getSource(), questId);
										})
								)
						)
				)
				.then(Commands.literal("debug")
						.then(Commands.literal("forcestart")
								.executes(context -> {
									if (context.getSource().getPlayer() == null) {
										return 0;
									}
									context.getSource().getPlayer().getCapability(PlayerStorylineProvider.CAPABILITY).ifPresent(StorylineManager::initializeSagas);
									context.getSource().sendSuccess(() -> Component.literal("Forced saga initialization."), true);
									return 1;
								})
						)
						.then(Commands.literal("reset_progress")
								.executes(context -> {
									if (context.getSource().getPlayer() == null) {
										return 0;
									}
									context.getSource().getPlayer().getCapability(PlayerStorylineProvider.CAPABILITY).ifPresent(StorylineManager::resetProgress);
									context.getSource().sendSuccess(() -> Component.literal("All Storyline progress has been reset."), true);
									return 1;
								})
						)
				)
		);
	}

	private int setQuestCompletion(CommandSourceStack source, String questId, boolean completed) {
		AtomicInteger result = new AtomicInteger(0);

		if (source.getPlayer() == null) {
			return 0;
		}

		// Access the capability of the player
		source.getPlayer().getCapability(PlayerStorylineProvider.CAPABILITY).ifPresent(playerStoryline -> {
			// Iterate through all sagas to find the quest
			for (Saga saga : playerStoryline.getAllSagas().values()) {
				Quest quest = saga.getQuestbyId(questId);

				if (quest == null) {
					source.sendFailure(Component.translatable("command.dmzstoryline.no_quest", questId, source.getPlayer().getDisplayName().getString()));
					result.set(0);
					return;
				}

				// Set completion status
				if (completed) {
					quest.completeQuest();
					source.sendSuccess(() -> Component.translatable("command.dmzstoryline.quest_completed", questId), true);
				} else {
					quest.setCompleted(false);
					source.sendSuccess(() -> Component.translatable("command.dmzstoryline.quest_incompleted", questId), true);
				}

				result.set(1); // Indicate success
				return;
			}

			// If no quest was found
			source.sendFailure(Component.translatable("command.dmzstoryline.no_found_quest", questId));
			result.set(0); // Indicate failure
		});

		return result.get(); // Return the result
	}


	private int getSagaInfo(CommandSourceStack source, String sagaId) {

		AtomicInteger result = new AtomicInteger(0);

		if (source.getPlayer() == null) {
			return 0;
		}

		source.getPlayer().getCapability(PlayerStorylineProvider.CAPABILITY).ifPresent(playerStoryline -> {
			Saga saga = playerStoryline.getSaga(sagaId);

			if (saga == null) {
				source.sendFailure(Component.translatable("command.dmzstoryline.no_found_saga", sagaId));
				result.set(0); // Set the result to 0 if the saga is not found
				return;
			}

			source.sendSuccess(() -> Component.translatable("command.dmzstoryline.saga_info", saga.getName(), sagaId), false);

			for (Quest quest : saga.getQuests()) {
				String status = quest.isCompleted() ? "COMPLETED" : "INCOMPLETE";
				source.sendSuccess(() -> Component.translatable("command.dmzstoryline.quest_info", quest.getId(), quest.getDescription(), status), false);
			}

			result.set(1); // Set the result to 1 if everything is successful
		});

		return result.get(); // Return the stored result
	}

	private int getAllQuests(CommandSourceStack source) {

		AtomicInteger result = new AtomicInteger(0);

		if (source.getPlayer() == null) {
			return 0;
		}

		source.getPlayer().getCapability(PlayerStorylineProvider.CAPABILITY).ifPresent(playerStoryline -> {
			for (Saga saga : playerStoryline.getAllSagas().values()) {
				for (Quest quest : saga.getQuests()) {
					String status = quest.isCompleted() ? "COMPLETED" : "INCOMPLETE";
					source.sendSuccess(() -> Component.translatable("command.dmzstoryline.quest_info", quest.getId(), quest.getDescription(), status), false);
				}
			}
			result.set(1);
		});

		return result.get();
	}

	private int getQuestInfo(CommandSourceStack source, String questId) {

		AtomicInteger result = new AtomicInteger(0);

		if (source.getPlayer() == null) {
			return 0;
		}

		source.getPlayer().getCapability(PlayerStorylineProvider.CAPABILITY).ifPresent(playerStoryline -> {

			for (Saga saga : playerStoryline.getAllSagas().values()) {
				Quest quest = saga.getQuestbyId(questId);
				String status = quest.isCompleted() ? "COMPLETED" : "INCOMPLETE";
				source.sendSuccess(() -> Component.translatable("command.dmzstoryline.quest_info", quest.getId(), quest.getDescription(), status), false);

			}


			result.set(1); // Set the result to 1 if everything is successful
		});

		return result.get(); // Return the stored result
	}

	// Provide suggestions for quest IDs
	private CompletableFuture<Suggestions> suggestQuestIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		//El nuevo StorylineManager no se usa como tal, pero se puede usar para obtener todas las sagas y quests porque al inicializarlo se inicializan todas las sagas
		//Estas sagas, a su vez, tienen todas las quests que se pueden completar
		for (Saga saga : storylineManager.getAllSagas().values()) {
			for (Quest quest : saga.getQuests()) {
				builder.suggest(quest.getId());
			}
		}
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestSagaIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		for (Saga saga : storylineManager.getAllSagas().values()) {
			builder.suggest(saga.getId());
		}
		return builder.buildFuture();
	}
}
