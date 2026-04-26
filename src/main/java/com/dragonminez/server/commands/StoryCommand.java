package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestService;
import com.dragonminez.common.quest.QuestTextFormatter;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StoryCommand {

	private static final SuggestionProvider<CommandSourceStack> QUEST_SUGGESTIONS = (context, builder) ->
			SharedSuggestionProvider.suggest(new ArrayList<>(QuestRegistry.getAllQuests().keySet()), builder);

	private static final SuggestionProvider<CommandSourceStack> QUEST_OR_ALL_SUGGESTIONS = (context, builder) -> {
		List<String> suggestions = new ArrayList<>(QuestRegistry.getAllQuests().keySet());
		suggestions.add("all");
		return SharedSuggestionProvider.suggest(suggestions, builder);
	};

	private static final SuggestionProvider<CommandSourceStack> SAGA_OR_ALL_SUGGESTIONS = (context, builder) -> {
		List<String> suggestions = new ArrayList<>(QuestRegistry.getAllSagas().keySet());
		suggestions.add("all");
		return SharedSuggestionProvider.suggest(suggestions, builder);
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzquest")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.QUEST_LIST_SELF, DMZPermissions.QUEST_LIST_OTHERS))

				.then(Commands.literal("list")
						.executes(context -> listQuests(context, null))
						.then(Commands.argument("player", EntityArgument.player())
								.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.QUEST_LIST_OTHERS))
								.executes(context -> listQuests(context, EntityArgument.getPlayer(context, "player")))))

				.then(Commands.literal("finish")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.QUEST_FINISH_SELF, DMZPermissions.QUEST_FINISH_OTHERS))
						.then(Commands.argument("quest", StringArgumentType.word())
								.suggests(QUEST_OR_ALL_SUGGESTIONS)
								.executes(context -> finishQuest(context, null))
								.then(Commands.argument("player", EntityArgument.player())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.QUEST_FINISH_OTHERS))
										.executes(context -> finishQuest(context, EntityArgument.getPlayer(context, "player"))))))

				.then(Commands.literal("reset")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.QUEST_RESET_SELF, DMZPermissions.QUEST_RESET_OTHERS))
						.then(Commands.argument("quest", StringArgumentType.word())
								.suggests(QUEST_OR_ALL_SUGGESTIONS)
								.executes(context -> resetQuest(context, null))
								.then(Commands.argument("player", EntityArgument.player())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.QUEST_RESET_OTHERS))
										.executes(context -> resetQuest(context, EntityArgument.getPlayer(context, "player"))))))

				.then(Commands.literal("resetsaga")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.QUEST_RESETSAGA_SELF, DMZPermissions.QUEST_RESETSAGA_OTHERS))
						.then(Commands.argument("saga", StringArgumentType.word())
								.suggests(SAGA_OR_ALL_SUGGESTIONS)
								.executes(context -> resetSaga(context, null))
								.then(Commands.argument("player", EntityArgument.player())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.QUEST_RESETSAGA_OTHERS))
										.executes(context -> resetSaga(context, EntityArgument.getPlayer(context, "player"))))))

				.then(Commands.literal("questnpc")
						.requires(source -> source.hasPermission(2))
						.then(Commands.literal("spawn")
								.then(Commands.argument("npcId", StringArgumentType.word())
										.executes(context -> spawnQuestNPC(context, null, null))
										.then(Commands.argument("model", StringArgumentType.word())
												.executes(context -> spawnQuestNPC(context, StringArgumentType.getString(context, "model"), null))
												.then(Commands.argument("texture", StringArgumentType.word())
														.executes(context -> spawnQuestNPC(context, StringArgumentType.getString(context, "model"),
																StringArgumentType.getString(context, "texture"))))))))
		);
	}

	private static int listQuests(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
		try {
			ServerPlayer player = targetPlayer != null ? targetPlayer : context.getSource().getPlayerOrException();

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				PlayerQuestData pqd = data.getPlayerQuestData();
				List<Map.Entry<String, Quest>> quests = new ArrayList<>(QuestRegistry.getAllQuests().entrySet());
				quests.sort(Map.Entry.comparingByKey(Comparator.naturalOrder()));

				if (quests.isEmpty()) {
					context.getSource().sendFailure(Component.literal("No quests are loaded."));
					return;
				}

				context.getSource().sendSystemMessage(Component.literal("Quest progress for " + player.getGameProfile().getName() + ":"));
				for (Map.Entry<String, Quest> entry : quests) {
					String questKey = entry.getKey();
					Quest quest = entry.getValue();
					Component status = formatQuestStatus(pqd, questKey);
					Component title = Component.translatable(quest.getTitle());
					context.getSource().sendSystemMessage(
							status.copy()
									.append(Component.literal(" "))
									.append(title)
									.append(Component.literal(" [" + questKey + "]"))
					);

					if (pqd.isQuestAccepted(questKey) && !pqd.isQuestCompleted(questKey)) {
						for (int i = 0; i < quest.getObjectives().size(); i++) {
							QuestObjective objective = quest.getObjectives().get(i);
							int progress = pqd.getObjectiveProgress(questKey, i);
							int required = quest.getObjectiveRequired(pqd, questKey, i);
							String progressText = progress >= required ? "[DONE]" : "[" + progress + "/" + required + "]";
							context.getSource().sendSystemMessage(
									Component.literal("  " + progressText + " ").append(QuestTextFormatter.describeObjective(objective))
							);
						}
					}
				}
			});

			return 1;
		} catch (Exception e) {
			context.getSource().sendFailure(Component.literal("Failed to list quests: " + e.getMessage()));
			return 0;
		}
	}

	private static int finishQuest(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		try {
			String questKey = StringArgumentType.getString(context, "quest");
			List<ServerPlayer> targets = getTargetPlayers(context, targetPlayer);
			if (targets.isEmpty()) {
				context.getSource().sendFailure(Component.literal("No valid players found."));
				return 0;
			}

			List<Map.Entry<String, Quest>> questsToFinish = new ArrayList<>();
			if ("all".equalsIgnoreCase(questKey)) {
				questsToFinish.addAll(QuestRegistry.getAllQuests().entrySet());
			} else {
				Quest quest = QuestRegistry.getQuest(questKey);
				if (quest == null) {
					context.getSource().sendFailure(Component.literal("Quest not found: " + questKey));
					return 0;
				}
				questsToFinish.add(Map.entry(questKey, quest));
			}

			int successCount = 0;
			for (ServerPlayer player : targets) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
					PlayerQuestData pqd = stats.getPlayerQuestData();
					for (Map.Entry<String, Quest> entry : questsToFinish) {
						completeQuest(pqd, entry.getKey(), entry.getValue(), player);
					}
					QuestService.syncQuestState(player);
				});
				successCount++;
			}

			int affected = successCount;
			context.getSource().sendSuccess(() ->
					Component.literal(("all".equalsIgnoreCase(questKey) ? "Finished all quests" : "Finished quest " + questKey)
							+ " for " + affected + " player(s)."), log);
			return successCount;
		} catch (Exception e) {
			context.getSource().sendFailure(Component.literal("Failed to finish quest: " + e.getMessage()));
			return 0;
		}
	}

	private static int resetQuest(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		try {
			String questKey = StringArgumentType.getString(context, "quest");
			List<ServerPlayer> targets = getTargetPlayers(context, targetPlayer);
			if (targets.isEmpty()) {
				context.getSource().sendFailure(Component.literal("No valid players found."));
				return 0;
			}

			if (!"all".equalsIgnoreCase(questKey) && QuestRegistry.getQuest(questKey) == null) {
				context.getSource().sendFailure(Component.literal("Quest not found: " + questKey));
				return 0;
			}

			int successCount = 0;
			for (ServerPlayer player : targets) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
					PlayerQuestData pqd = stats.getPlayerQuestData();
					if ("all".equalsIgnoreCase(questKey)) {
						for (String acceptedQuestKey : pqd.getAcceptedQuestIds()) {
							Quest acceptedQuest = QuestRegistry.getQuest(acceptedQuestKey);
							if (acceptedQuest != null) {
								postForcedResetFailEvent(player, pqd, acceptedQuestKey, acceptedQuest);
							}
						}
						pqd.resetAll();
					} else {
						Quest quest = QuestRegistry.getQuest(questKey);
						if (quest != null && postForcedResetFailEvent(player, pqd, questKey, quest)) {
							resetQuestEntry(pqd, questKey);
						}
					}
					QuestService.syncQuestState(player);
				});
				successCount++;
			}

			int affected = successCount;
			context.getSource().sendSuccess(() ->
					Component.literal(("all".equalsIgnoreCase(questKey) ? "Reset all quests" : "Reset quest " + questKey)
							+ " for " + affected + " player(s)."), log);
			return successCount;
		} catch (Exception e) {
			context.getSource().sendFailure(Component.literal("Failed to reset quest: " + e.getMessage()));
			return 0;
		}
	}

	private static int resetSaga(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		try {
			String sagaId = StringArgumentType.getString(context, "saga");
			List<ServerPlayer> targets = getTargetPlayers(context, targetPlayer);
			if (targets.isEmpty()) {
				context.getSource().sendFailure(Component.literal("No valid players found."));
				return 0;
			}

			if (!"all".equalsIgnoreCase(sagaId) && QuestRegistry.getSaga(sagaId) == null) {
				context.getSource().sendFailure(Component.literal("Saga not found: " + sagaId));
				return 0;
			}

			int successCount = 0;
			for (ServerPlayer player : targets) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
					PlayerQuestData pqd = stats.getPlayerQuestData();
					if ("all".equalsIgnoreCase(sagaId)) {
						for (String acceptedQuestKey : pqd.getAcceptedQuestIds()) {
							Quest acceptedQuest = QuestRegistry.getQuest(acceptedQuestKey);
							if (acceptedQuest != null) {
								postForcedResetFailEvent(player, pqd, acceptedQuestKey, acceptedQuest);
							}
						}
						pqd.resetAll();
					} else {
						String prefix = sagaId + ":";
						for (String questKey : collectKnownQuestKeys(pqd)) {
							if (!questKey.startsWith(prefix)) {
								continue;
							}

							Quest quest = QuestRegistry.getQuest(questKey);
							if (quest == null || postForcedResetFailEvent(player, pqd, questKey, quest)) {
								resetQuestEntry(pqd, questKey);
							}
						}
					}
					QuestService.syncQuestState(player);
				});
				successCount++;
			}

			int affected = successCount;
			context.getSource().sendSuccess(() ->
					Component.literal(("all".equalsIgnoreCase(sagaId) ? "Reset all sagas" : "Reset saga " + sagaId)
							+ " for " + affected + " player(s)."), log);
			return successCount;
		} catch (Exception e) {
			context.getSource().sendFailure(Component.literal("Failed to reset saga: " + e.getMessage()));
			return 0;
		}
	}

	private static Component formatQuestStatus(PlayerQuestData pqd, String questKey) {
		if (pqd.isQuestCompleted(questKey)) {
			return Component.literal("[SUCCESS]");
		}
		if (pqd.isQuestAccepted(questKey)) {
			return Component.literal("[ACTIVE]");
		}
		if (pqd.getQuestStatus(questKey) == PlayerQuestData.QuestStatus.FAILED) {
			return Component.literal("[FAILED]");
		}
		return Component.literal("[NOT STARTED]");
	}

	private static void completeQuest(PlayerQuestData pqd, String questKey, Quest quest, ServerPlayer player) {
		if (!pqd.isQuestAccepted(questKey)) {
			pqd.acceptQuest(questKey);
		}

		quest.initializeObjectiveRequirements(pqd, questKey, PartyManager.getAllPartyMembers(player).size());
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			pqd.setObjectiveProgress(questKey, i, quest.getObjectiveRequired(pqd, questKey, i));
		}

		QuestService.ResolvedQuest resolved = QuestService.resolveQuest(questKey);
		Saga saga = resolved != null ? resolved.saga() : null;
		DMZEvent.QuestCompletedEvent completeEvent = new DMZEvent.QuestCompletedEvent(
				player,
				questKey,
				saga,
				quest,
				PartyManager.getAllPartyMembers(player)
		);
		if (MinecraftForge.EVENT_BUS.post(completeEvent)) {
			return;
		}

		pqd.completeQuest(questKey);
		if (questKey.equals(pqd.getTrackedQuestId())) {
			pqd.setTrackedQuestId(null);
		}
	}

	private static boolean postForcedResetFailEvent(ServerPlayer player, PlayerQuestData pqd, String questKey, Quest quest) {
		if (!pqd.isQuestAccepted(questKey) || pqd.isQuestCompleted(questKey)) {
			return true;
		}

		QuestService.ResolvedQuest resolved = QuestService.resolveQuest(questKey);
		DMZEvent.QuestFailEvent failEvent = new DMZEvent.QuestFailEvent(
				player,
				questKey,
				resolved != null ? resolved.saga() : null,
				quest,
				PartyManager.getAllPartyMembers(player),
				DMZEvent.QuestFailEvent.FailureReason.FORCED_RESET
		);
		return !MinecraftForge.EVENT_BUS.post(failEvent);
	}

	private static Set<String> collectKnownQuestKeys(PlayerQuestData pqd) {
		Set<String> questKeys = new LinkedHashSet<>();
		questKeys.addAll(pqd.getAcceptedQuestIds());
		questKeys.addAll(pqd.getCompletedQuestIds());
		questKeys.addAll(pqd.getFailedQuestIds());
		return questKeys;
	}

	private static void resetQuestEntry(PlayerQuestData pqd, String questKey) {
		pqd.resetQuest(questKey);
		if (questKey.equals(pqd.getTrackedQuestId())) {
			pqd.setTrackedQuestId(null);
		}
	}

	private static List<ServerPlayer> getTargetPlayers(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
		List<ServerPlayer> players = new ArrayList<>();
		if (targetPlayer != null) {
			players.addAll(PartyManager.getAllPartyMembers(targetPlayer));
			return players;
		}

		try {
			ServerPlayer executor = context.getSource().getPlayerOrException();
			players.addAll(PartyManager.getAllPartyMembers(executor));
		} catch (Exception ignored) {
		}

		return players;
	}

	private static int spawnQuestNPC(CommandContext<CommandSourceStack> context, String modelOverride, String textureOverride) {
		try {
			String npcId = StringArgumentType.getString(context, "npcId");
			ServerPlayer player = context.getSource().getPlayerOrException();

			QuestNPCEntity npc = MainEntities.QUEST_NPC.get().create(player.level());
			if (npc == null) {
				context.getSource().sendFailure(Component.literal("Failed to create QuestNPCEntity"));
				return 0;
			}

			npc.setNpcId(npcId);
			if (modelOverride != null && !modelOverride.isEmpty()) {
				npc.setNpcModel(modelOverride);
			}
			if (textureOverride != null && !textureOverride.isEmpty()) {
				npc.setNpcTexture(textureOverride);
			}

			npc.setPos(player.getX(), player.getY(), player.getZ());
			player.serverLevel().addFreshEntity(npc);

			context.getSource().sendSuccess(() -> Component.literal("Spawned quest NPC " + npcId + "."), true);
			return 1;
		} catch (Exception e) {
			context.getSource().sendFailure(Component.literal("Failed to spawn quest NPC: " + e.getMessage()));
			return 0;
		}
	}
}
