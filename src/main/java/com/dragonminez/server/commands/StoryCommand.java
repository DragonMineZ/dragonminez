package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.quest.*;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StoryCommand {

    private static final SuggestionProvider<CommandSourceStack> SAGA_SUGGESTIONS = (context, builder) -> {
        List<String> sagaIds = new ArrayList<>(QuestRegistry.getAllSagas().keySet());
        return SharedSuggestionProvider.suggest(sagaIds, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> RESET_SUGGESTIONS = (context, builder) -> {
        List<String> sagaIds = new ArrayList<>(QuestRegistry.getAllSagas().keySet());
        sagaIds.add("all");
        return SharedSuggestionProvider.suggest(sagaIds, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> QUEST_SUGGESTIONS = (context, builder) -> {
        try {
            String sagaId = StringArgumentType.getString(context, "saga");
            Saga saga = QuestRegistry.getSaga(sagaId);
            if (saga != null) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("all");
                suggestions.addAll(saga.getQuests().stream()
                        .map(quest -> String.valueOf(quest.getId()))
                        .toList());
                return SharedSuggestionProvider.suggest(suggestions, builder);
            }
        } catch (Exception ignored) {
        }
        return SharedSuggestionProvider.suggest(new String[]{"all", "1", "2", "3"}, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> SIDEQUEST_SUGGESTIONS = (context, builder) -> {
        Map<String, Quest> all = QuestRegistry.getAllQuests();
        return SharedSuggestionProvider.suggest(new ArrayList<>(all.keySet()), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> SIDEQUEST_RESET_SUGGESTIONS = (context, builder) -> {
        List<String> ids = new ArrayList<>(QuestRegistry.getAllQuests().keySet());
        ids.add("all");
        return SharedSuggestionProvider.suggest(ids, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzstory")
                .requires(source -> DMZPermissions.check(source, DMZPermissions.STORY_LIST_SELF, DMZPermissions.STORY_LIST_OTHERS))

                // finish <saga> <quest> [player]
                .then(Commands.literal("finish")
                        .requires(source -> DMZPermissions.check(source, DMZPermissions.STORY_FINISH_SELF, DMZPermissions.STORY_FINISH_OTHERS))
                        .then(Commands.argument("saga", StringArgumentType.word())
                                .suggests(SAGA_SUGGESTIONS)
                                .then(Commands.argument("quest", StringArgumentType.word())
                                        .suggests(QUEST_SUGGESTIONS)
                                        .executes(context -> finishQuest(context, null))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STORY_FINISH_OTHERS))
                                                .executes(context -> finishQuest(
                                                        context,
                                                        EntityArgument.getPlayer(context, "player")
                                                ))
                                        )
                                )
                        )
                )

                // remove <saga> <quest> [player]
                .then(Commands.literal("remove")
                        .requires(source -> DMZPermissions.check(source, DMZPermissions.STORY_REMOVE_SELF, DMZPermissions.STORY_REMOVE_OTHERS))
                        .then(Commands.argument("saga", StringArgumentType.word())
                                .suggests(SAGA_SUGGESTIONS)
                                .then(Commands.argument("quest", IntegerArgumentType.integer(1))
                                        .suggests(QUEST_SUGGESTIONS)
                                        .executes(context -> removeQuest(context, null))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STORY_REMOVE_OTHERS))
                                                .executes(context -> removeQuest(
                                                        context,
                                                        EntityArgument.getPlayer(context, "player")
                                                ))
                                        )
                                )
                        )
                )

                // reset <saga> [player]
                .then(Commands.literal("reset")
                        .requires(source -> DMZPermissions.check(source, DMZPermissions.STORY_RESET_SELF, DMZPermissions.STORY_RESET_OTHERS))
                        .then(Commands.argument("saga", StringArgumentType.word())
                                .suggests(RESET_SUGGESTIONS)
                                .executes(context -> resetSaga(context, null))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STORY_RESET_OTHERS))
                                        .executes(context -> resetSaga(
                                                context,
                                                EntityArgument.getPlayer(context, "player")
                                        ))
                                )
                        )
                )

                // sidequest <list|accept|finish|reset>
                .then(Commands.literal("sidequest")
                        .requires(source -> DMZPermissions.check(source, DMZPermissions.SIDEQUEST_LIST_SELF, DMZPermissions.SIDEQUEST_LIST_OTHERS))

                        .then(Commands.literal("list")
                                .executes(context -> sideQuestList(context, null))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.SIDEQUEST_LIST_OTHERS))
                                        .executes(context -> sideQuestList(context, EntityArgument.getPlayer(context, "player")))
                                )
                        )

                        .then(Commands.literal("accept")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.SIDEQUEST_ACCEPT_SELF, DMZPermissions.SIDEQUEST_ACCEPT_OTHERS))
                                .then(Commands.argument("sidequest", StringArgumentType.word())
                                        .suggests(SIDEQUEST_SUGGESTIONS)
                                        .executes(context -> sideQuestAccept(context, null))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.SIDEQUEST_ACCEPT_OTHERS))
                                                .executes(context -> sideQuestAccept(context, EntityArgument.getPlayer(context, "player")))
                                        )
                                )
                        )

                        .then(Commands.literal("finish")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.SIDEQUEST_FINISH_SELF, DMZPermissions.SIDEQUEST_FINISH_OTHERS))
                                .then(Commands.argument("sidequest", StringArgumentType.word())
                                        .suggests(SIDEQUEST_RESET_SUGGESTIONS)
                                        .executes(context -> sideQuestFinish(context, null))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.SIDEQUEST_FINISH_OTHERS))
                                                .executes(context -> sideQuestFinish(context, EntityArgument.getPlayer(context, "player")))
                                        )
                                )
                        )

                        .then(Commands.literal("reset")
                                .requires(source -> DMZPermissions.check(source, DMZPermissions.SIDEQUEST_RESET_SELF, DMZPermissions.SIDEQUEST_RESET_OTHERS))
                                .then(Commands.argument("sidequest", StringArgumentType.word())
                                        .suggests(SIDEQUEST_RESET_SUGGESTIONS)
                                        .executes(context -> sideQuestReset(context, null))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.SIDEQUEST_RESET_OTHERS))
                                                .executes(context -> sideQuestReset(context, EntityArgument.getPlayer(context, "player")))
                                        )
                                )
                        )
                )

                // questnpc spawn <npcId> [model]
                .then(Commands.literal("questnpc")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("npcId", StringArgumentType.word())
                                        .executes(context -> spawnQuestNPC(context, null))
                                        .then(Commands.argument("model", StringArgumentType.word())
                                                .executes(context -> spawnQuestNPC(context, StringArgumentType.getString(context, "model")))
                                        )
                                )
                        )
                )
        );
    }

    private static int finishQuest(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
        try {
            String sagaId = StringArgumentType.getString(context, "saga");
            String questArg = StringArgumentType.getString(context, "sidequest"); // Leemos como String

            Saga saga = QuestRegistry.getSaga(sagaId);
            if (saga == null) {
                context.getSource().sendFailure(Component.translatable("command.dragonminez.story.saga_not_found", sagaId));
                return 0;
            }

            List<ServerPlayer> targetPlayers = getTargetPlayers(context, targetPlayer);

            if (targetPlayers.isEmpty()) {
                context.getSource().sendFailure(Component.translatable("command.dragonminez.story.no_valid_players"));
                return 0;
            }

            int successCount = 0;

            if (questArg.equalsIgnoreCase("all")) {
                for (ServerPlayer player : targetPlayers) {
                    StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
                        boolean changed = false;
                        for (Quest quest : saga.getQuests()) {
                            performQuestCompletion(stats.getPlayerQuestData(), saga, quest, player);
                            changed = true;
                        }
                        if (changed) NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
                    });
                    successCount++;
                }

                context.getSource().sendSuccess(() -> Component.translatable("command.dragonminez.story.complete_all" + saga.getName()), log);

            } else {
                try {
                    int questId = Integer.parseInt(questArg);
                    Quest quest = saga.getQuests().stream()
                            .filter(q -> q.getId() == questId)
                            .findFirst()
                            .orElse(null);

                    if (quest == null) {
                        context.getSource().sendFailure(Component.translatable("command.dragonminez.story.quest_not_found", questId, sagaId));
                        return 0;
                    }

                    for (ServerPlayer player : targetPlayers) {
                        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
                            performQuestCompletion(stats.getPlayerQuestData(), saga, quest, player);
                            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
                        });
                        successCount++;
                    }
                } catch (NumberFormatException e) {
                    context.getSource().sendFailure(Component.literal("Invalid Quest ID. Use a number or 'all'."));
                    return 0;
                }
            }

            final int finalCount = successCount;
            final int totalPlayers = targetPlayers.size();

            if (!questArg.equalsIgnoreCase("all")) {
                context.getSource().sendSuccess(() ->
                        Component.translatable("command.dragonminez.story.saga_info", saga.getName()), false);
            }
            context.getSource().sendSuccess(() ->
                    Component.translatable("command.dragonminez.story.players_affected", finalCount, totalPlayers), false);

            return successCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("command.dragonminez.story.error", e.getMessage()));
            return 0;
        }
    }

    private static void performQuestCompletion(PlayerQuestData pqd, Saga saga, Quest quest, ServerPlayer player) {
        List<QuestObjective> objectives = quest.getObjectives();
        for (int i = 0; i < objectives.size(); i++) {
            QuestObjective objective = objectives.get(i);
            int required = objective.getRequired();
            pqd.setObjectiveProgress(saga.getId(), quest.getId(), i, required);
        }

        pqd.completeQuest(saga.getId(), quest.getId());

        player.sendSystemMessage(Component.translatable("command.dragonminez.story.quest_completed", quest.getTitle()));
    }

    private static int removeQuest(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
        try {
            String sagaId = StringArgumentType.getString(context, "saga");
            int questId = IntegerArgumentType.getInteger(context, "quest");

            Saga saga = QuestRegistry.getSaga(sagaId);
            if (saga == null) {
                context.getSource().sendFailure(Component.translatable("command.dragonminez.story.saga_not_found", sagaId));
                return 0;
            }

            List<ServerPlayer> targetPlayers = getTargetPlayers(context, targetPlayer);

            if (targetPlayers.isEmpty()) {
                context.getSource().sendFailure(Component.translatable("command.dragonminez.story.no_valid_players"));
                return 0;
            }

            int successCount = 0;
            for (ServerPlayer player : targetPlayers) {
                StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
                    PlayerQuestData pqd = stats.getPlayerQuestData();
                    String questKey = PlayerQuestData.sagaQuestKey(sagaId, questId);

                    // Reset completion status
                    pqd.resetQuest(questKey);

                    Quest questObj = saga.getQuests().stream()
                            .filter(q -> q.getId() == questId)
                            .findFirst()
                            .orElse(null);

                    if (questObj != null) {
                        for (int i = 0; i < questObj.getObjectives().size(); i++) {
                            pqd.setObjectiveProgress(questKey, i, 0);
                        }
                    }

                    NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
                });
                successCount++;
            }

            final int finalCount = successCount;
            final int totalPlayers = targetPlayers.size();
            context.getSource().sendSuccess(() ->
                    Component.translatable("command.dragonminez.story.quest_removed", sagaId, questId), log);
            context.getSource().sendSuccess(() ->
                    Component.translatable("command.dragonminez.story.players_affected", finalCount, totalPlayers), log);

            return successCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("command.dragonminez.story.error", e.getMessage()));
            return 0;
        }
    }

    private static int resetSaga(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
        try {
            String sagaId = StringArgumentType.getString(context, "saga");

            List<ServerPlayer> targetPlayers = getTargetPlayers(context, targetPlayer);

            if (targetPlayers.isEmpty()) {
                context.getSource().sendFailure(Component.translatable("command.dragonminez.story.no_valid_players"));
                return 0;
            }

            int successCount = 0;
            for (ServerPlayer player : targetPlayers) {
                StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
                    PlayerQuestData pqd = stats.getPlayerQuestData();

                    if (sagaId.equalsIgnoreCase("all")) pqd.resetAll();
                    else pqd.resetSaga(sagaId);
                    NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
                });
                successCount++;
            }

            final int finalCount = successCount;
            final int totalPlayers = targetPlayers.size();
            context.getSource().sendSuccess(() ->
                    Component.translatable("command.dragonminez.story.saga_reset", sagaId), log);
            context.getSource().sendSuccess(() ->
                    Component.translatable("command.dragonminez.story.players_affected", finalCount, totalPlayers), log);

            return successCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("command.dragonminez.story.error", e.getMessage()));
            return 0;
        }
    }

    private static List<ServerPlayer> getTargetPlayers(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        List<ServerPlayer> players = new ArrayList<>();

        if (targetPlayer != null) {
            players.addAll(PartyManager.getAllPartyMembers(targetPlayer));
        } else {
            try {
                ServerPlayer executor = context.getSource().getPlayerOrException();
                players.addAll(PartyManager.getAllPartyMembers(executor));
            } catch (Exception ignored) {
            }
        }

        return players;
    }

    // ---- Side Quest Handlers ----

    private static int sideQuestList(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        try {
            ServerPlayer player = targetPlayer != null ? targetPlayer : context.getSource().getPlayerOrException();

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                PlayerQuestData pqd = data.getPlayerQuestData();
                Map<String, Quest> allQuests = QuestRegistry.getAllQuests();

                if (allQuests.isEmpty()) {
                    context.getSource().sendSystemMessage(Component.translatable("command.dragonminez.story.sidequest.no_quests"));
                    return;
                }

                context.getSource().sendSystemMessage(Component.translatable("command.dragonminez.story.sidequest.list_header", player.getName()));

                for (Map.Entry<String, Quest> entry : allQuests.entrySet()) {
                    String id = entry.getKey();
                    Quest sideQuest = entry.getValue();
                    String status;

                    if (pqd.isQuestCompleted(id)) {
                        status = "\u00A7a[COMPLETED]";
                    } else if (pqd.isQuestAccepted(id)) {
                        status = "\u00A7e[IN PROGRESS]";
                    } else {
                        status = "\u00A77[NOT STARTED]";
                    }

                    context.getSource().sendSystemMessage(
                            Component.literal(status + " \u00A7r" + id + " - " + sideQuest.getName())
                    );

                    if (pqd.isQuestAccepted(id)) {
                        List<QuestObjective> objectives = sideQuest.getObjectives();
                        for (int i = 0; i < objectives.size(); i++) {
                            QuestObjective obj = objectives.get(i);
                            int progress = pqd.getObjectiveProgress(id, i);
                            int required = obj.getRequired();
                            String objStatus = progress >= required ? "\u00A7a\u2714" : "\u00A7e" + progress + "/" + required;
                            context.getSource().sendSystemMessage(
                                    Component.literal("  " + objStatus + " \u00A7r" + obj.getDescription())
                            );
                        }
                    }
                }
            });

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("command.dragonminez.story.sidequest.error", e.getMessage()));
            return 0;
        }
    }

    private static int sideQuestAccept(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
        try {
            String questId = StringArgumentType.getString(context, "sidequest");
            ServerPlayer player = targetPlayer != null ? targetPlayer : context.getSource().getPlayerOrException();

            Quest sideQuest = QuestRegistry.getQuest(questId);
            if (sideQuest == null) {
                context.getSource().sendFailure(Component.translatable("command.dragonminez.story.sidequest.not_found", questId));
                return 0;
            }

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                PlayerQuestData pqd = data.getPlayerQuestData();

                if (pqd.isQuestAccepted(questId)) {
                    context.getSource().sendFailure(Component.translatable("command.dragonminez.story.sidequest.already_accepted", questId));
                    return;
                }

                pqd.acceptQuest(questId);
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);

                context.getSource().sendSuccess(() ->
                        Component.translatable("command.dragonminez.story.sidequest.accepted", questId, player.getName()), log);
            });

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("command.dragonminez.story.sidequest.error", e.getMessage()));
            return 0;
        }
    }

    private static int sideQuestFinish(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
        try {
            String questId = StringArgumentType.getString(context, "sidequest");
            ServerPlayer player = targetPlayer != null ? targetPlayer : context.getSource().getPlayerOrException();

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                PlayerQuestData pqd = data.getPlayerQuestData();

                if (questId.equalsIgnoreCase("all")) {
                    Map<String, Quest> allQuests = QuestRegistry.getAllQuests();
                    for (Map.Entry<String, Quest> entry : allQuests.entrySet()) {
                        performSideQuestCompletion(pqd, entry.getKey(), entry.getValue());
                    }
                    context.getSource().sendSuccess(() ->
                            Component.translatable("command.dragonminez.story.sidequest.finished_all", player.getName()), log);
                } else {
                    Quest sideQuest = QuestRegistry.getQuest(questId);
                    if (sideQuest == null) {
                        context.getSource().sendFailure(Component.translatable("command.dragonminez.story.sidequest.not_found", questId));
                        return;
                    }
                    performSideQuestCompletion(pqd, questId, sideQuest);
                    context.getSource().sendSuccess(() ->
                            Component.translatable("command.dragonminez.story.sidequest.finished", questId, player.getName()), log);
                }

                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            });

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("command.dragonminez.story.sidequest.error", e.getMessage()));
            return 0;
        }
    }

    private static void performSideQuestCompletion(PlayerQuestData pqd, String questId, Quest sideQuest) {
        if (!pqd.isQuestAccepted(questId)) {
            pqd.acceptQuest(questId);
        }

        List<QuestObjective> objectives = sideQuest.getObjectives();
        for (int i = 0; i < objectives.size(); i++) {
            pqd.setObjectiveProgress(questId, i, objectives.get(i).getRequired());
        }

        pqd.completeQuest(questId);
    }

    private static int sideQuestReset(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
        try {
            String questId = StringArgumentType.getString(context, "sidequest");
            ServerPlayer player = targetPlayer != null ? targetPlayer : context.getSource().getPlayerOrException();

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                PlayerQuestData pqd = data.getPlayerQuestData();

                if (questId.equalsIgnoreCase("all")) {
                    pqd.resetAll();
                    context.getSource().sendSuccess(() ->
                            Component.translatable("command.dragonminez.story.sidequest.reset_all", player.getName()), log);
                } else {
                    pqd.resetQuest(questId);
                    context.getSource().sendSuccess(() ->
                            Component.translatable("command.dragonminez.story.sidequest.reset", questId, player.getName()), log);
                }

                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            });

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("command.dragonminez.story.sidequest.error", e.getMessage()));
            return 0;
        }
    }

    private static int spawnQuestNPC(CommandContext<CommandSourceStack> context, String modelOverride) {
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

            npc.setPos(player.getX(), player.getY(), player.getZ());
            player.serverLevel().addFreshEntity(npc);

            context.getSource().sendSuccess(() ->
                    Component.translatable("command.dragonminez.story.questnpc.spawned", npcId), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("command.dragonminez.story.questnpc.error", e.getMessage()));
            return 0;
        }
    }
}
