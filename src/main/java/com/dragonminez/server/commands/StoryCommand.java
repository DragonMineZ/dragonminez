package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
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

public class StoryCommand {

    private static final SuggestionProvider<CommandSourceStack> SAGA_SUGGESTIONS = (context, builder) -> {
        List<String> sagaIds = new ArrayList<>(SagaManager.getAllSagas().keySet());
        return SharedSuggestionProvider.suggest(sagaIds, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> QUEST_SUGGESTIONS = (context, builder) -> {
        try {
            String sagaId = StringArgumentType.getString(context, "saga");
            Saga saga = SagaManager.getSaga(sagaId);
            if (saga != null) {
                List<String> questIds = saga.getQuests().stream()
                        .map(quest -> String.valueOf(quest.getId()))
                        .toList();
                return SharedSuggestionProvider.suggest(questIds, builder);
            }
        } catch (Exception e) {
        }
        return SharedSuggestionProvider.suggest(new String[]{"1", "2", "3"}, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzstory")
                .requires(source -> source.hasPermission(2))

                // /dmzstory finish <saga> <quest> [player]
                .then(Commands.literal("finish")
                        .then(Commands.argument("saga", StringArgumentType.word())
                                .suggests(SAGA_SUGGESTIONS)
                                .then(Commands.argument("quest", IntegerArgumentType.integer(1))
                                        .suggests(QUEST_SUGGESTIONS)
                                        .executes(context -> finishQuest(context, null))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> finishQuest(
                                                        context,
                                                        EntityArgument.getPlayer(context, "player")
                                                ))
                                        )
                                )
                        )
                )

                // /dmzstory remove <saga> <quest> [player]
                .then(Commands.literal("remove")
                        .then(Commands.argument("saga", StringArgumentType.word())
                                .suggests(SAGA_SUGGESTIONS)
                                .then(Commands.argument("quest", IntegerArgumentType.integer(1))
                                        .suggests(QUEST_SUGGESTIONS)
                                        .executes(context -> removeQuest(context, null))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> removeQuest(
                                                        context,
                                                        EntityArgument.getPlayer(context, "player")
                                                ))
                                        )
                                )
                        )
                )

                // /dmzstory list [player]
                .then(Commands.literal("list")
                        .executes(context -> listProgress(context, null))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> listProgress(
                                        context,
                                        EntityArgument.getPlayer(context, "player")
                                ))
                        )
                )
        );
    }

    private static int finishQuest(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        try {
            String sagaId = StringArgumentType.getString(context, "saga");
            int questId = IntegerArgumentType.getInteger(context, "quest");

            Saga saga = SagaManager.getSaga(sagaId);
            if (saga == null) {
                context.getSource().sendFailure(Component.translatable("command.dragonminez.story.saga_not_found", sagaId));
                return 0;
            }

            Quest quest = saga.getQuests().stream()
                    .filter(q -> q.getId() == questId)
                    .findFirst()
                    .orElse(null);

            if (quest == null) {
                context.getSource().sendFailure(Component.translatable("command.dragonminez.story.quest_not_found", questId, sagaId));
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
                    QuestData questData = stats.getQuestData();

                    List<QuestObjective> objectives = quest.getObjectives();
                    for (int i = 0; i < objectives.size(); i++) {
                        QuestObjective objective = objectives.get(i);
                        int required = objective.getRequired();

                        questData.setQuestObjectiveProgress(saga.getId(), quest.getId(), i, required);

                        player.sendSystemMessage(Component.translatable("command.dragonminez.story.objective_completed", i, required, required));
                    }

                    questData.completeQuest(saga.getId(), quest.getId());

                    QuestData.SagaProgress sagaProgress = questData.getSagaProgress(saga.getId());
                    QuestData.QuestProgress questProgress = sagaProgress.getQuestProgress(quest.getId());
                    questProgress.setCompleted(true);

                    boolean isCompleted = questData.isQuestCompleted(saga.getId(), quest.getId());
                    player.sendSystemMessage(Component.translatable("command.dragonminez.story.quest_completed_nbt", isCompleted));

                    NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);

                    player.sendSystemMessage(Component.translatable("command.dragonminez.story.quest_completed", quest.getTitle()));
                    player.sendSystemMessage(Component.translatable("command.dragonminez.story.objectives_completed", objectives.size()));
                });
                successCount++;
            }

            final int finalCount = successCount;
            final int totalPlayers = targetPlayers.size();
            context.getSource().sendSuccess(() ->
                    Component.translatable("command.dragonminez.story.saga_info", saga.getName()), false);
            context.getSource().sendSuccess(() ->
                    Component.translatable("command.dragonminez.story.players_affected", finalCount, totalPlayers), false);

            return successCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("command.dragonminez.story.error", e.getMessage()));
            return 0;
        }
    }

    private static int removeQuest(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        try {
            String sagaId = StringArgumentType.getString(context, "saga");
            int questId = IntegerArgumentType.getInteger(context, "quest");

            Saga saga = SagaManager.getSaga(sagaId);
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
                    QuestData questData = stats.getQuestData();
                    QuestData.SagaProgress sagaProgress = questData.getSagaProgress(sagaId);
                    QuestData.QuestProgress questProgress = sagaProgress.getQuestProgress(questId);

                    questProgress.setCompleted(false);

                    Quest questObj = saga.getQuests().stream()
                            .filter(q -> q.getId() == questId)
                            .findFirst()
                            .orElse(null);

                    if (questObj != null) {
                        for (int i = 0; i < questObj.getObjectives().size(); i++) {
                            questData.setQuestObjectiveProgress(sagaId, questId, i, 0);
                        }
                    }

                    NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
                });
                successCount++;
            }

            final int finalCount = successCount;
            final int totalPlayers = targetPlayers.size();
            context.getSource().sendSuccess(() ->
                    Component.translatable("command.dragonminez.story.quest_removed", sagaId, questId), false);
            context.getSource().sendSuccess(() ->
                    Component.translatable("command.dragonminez.story.players_affected", finalCount, totalPlayers), false);

            return successCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("command.dragonminez.story.error", e.getMessage()));
            return 0;
        }
    }

    private static int listProgress(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        try {
            ServerPlayer player = targetPlayer != null ? targetPlayer : context.getSource().getPlayerOrException();

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
                QuestData questData = stats.getQuestData();

                context.getSource().sendSuccess(() ->
                        Component.translatable("command.dragonminez.story.progress_header", player.getName().getString()), false);

                context.getSource().sendSuccess(() ->
                        Component.literal("§7§m                                    "), false);

                SagaManager.getAllSagas().forEach((sagaId, saga) -> {
                    QuestData.SagaProgress sagaProgress = questData.getSagaProgress(sagaId);
                    boolean unlocked = sagaProgress.isUnlocked();

                    context.getSource().sendSuccess(() ->
                            Component.translatable("command.dragonminez.story.saga_entry", saga.getName(), sagaId), false);

                    if (unlocked) {
                        for (Quest quest : saga.getQuests()) {
                            boolean completed = sagaProgress.isQuestCompleted(quest.getId());
                            String status = completed ? "§a✓" : "§c✗";
                            context.getSource().sendSuccess(() ->
                                    Component.translatable("command.dragonminez.story.quest_entry", status, quest.getId(), quest.getTitle()), false);
                        }
                    } else {
                        context.getSource().sendSuccess(() ->
                                Component.translatable("command.dragonminez.story.saga_locked"), false);
                    }
                });
            });

            return 1;

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
}
