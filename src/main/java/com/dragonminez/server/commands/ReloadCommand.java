package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.alignment.NpcAlignmentRules;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.*;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.wish.WishManager;
import com.dragonminez.server.storage.StorageManager;
import com.dragonminez.server.world.npc.NPCPlacementManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;

public class ReloadCommand {
	private static final SuggestionProvider<CommandSourceStack> RELOAD_VALUE_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(List.of("all", "config", "story", "wishes"), builder);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzreload")
				.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.RELOAD))
				.executes(ctx -> executeReload(ctx.getSource(), "all"))
				.then(Commands.argument("target", StringArgumentType.word()).suggests(RELOAD_VALUE_SUGGESTIONS)
						.executes(ctx -> executeReload(ctx.getSource(), StringArgumentType.getString(ctx, "target"))))
		);
	}

	private static int executeReload(CommandSourceStack source, String rawScope) {
		ReloadScope scope = ReloadScope.from(rawScope);
		if (scope == null) {
			source.sendFailure(Component.literal("Invalid reload section: " + rawScope));
			return 0;
		}

		MinecraftServer server = source.getServer();
		source.sendSystemMessage(Component.translatable("command.dragonminez.reload.start"));

		try {
			if (scope.includesConfig()) {
				ConfigManager.clearServerSync();
				ConfigManager.reload();
				StorageManager.reload();
				NpcAlignmentRules.load(server);
				NPCPlacementManager.load(server);
				NPCPlacementManager.spawnForLoadedLevels(server);
			}

			if (scope.includesStory()) {
				QuestRegistry.loadAll(server);
			}

			if (scope.includesWishes()) {
				WishManager.loadWishes(server);
			}

			List<String> availableConfigs = null;
			if (scope.includesConfig()) {
				availableConfigs = ConfigManager.getAvailableConfigFiles();
			}

			int syncedPlayers = 0;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (scope.includesConfig()) {
					for (String file : availableConfigs) {
						String jsonPayload = ConfigManager.getSpecificConfigJson(file);
						if (jsonPayload == null || jsonPayload.isBlank()) continue;
						NetworkHandler.sendToPlayer(new SyncServerConfigS2C(file, jsonPayload), player);
					}

					StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
						String raceName = data.getCharacter().getRaceName();
						if (raceName != null && !raceName.isEmpty()) data.updateTransformationSkillLimits(raceName);
						else data.getSkills().refreshNonFormSkillMaxLevels();
						NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
					});
				}

				if (scope.includesStory()) {
					NetworkHandler.sendToPlayer(
							new SyncQuestRegistryS2C(QuestRegistry.getAllSagas(), QuestRegistry.getAllQuests()),
							player
					);
				}

				if (scope.includesWishes()) {
					NetworkHandler.sendToPlayer(new SyncWishesS2C(WishManager.getAllWishes()), player);
				}
				syncedPlayers++;
			}

			int finalSyncedPlayers = syncedPlayers;
			source.sendSuccess(() -> Component.translatable("command.dragonminez.reload.success"), true);
			source.sendSuccess(() -> Component.translatable("command.dragonminez.reload.sync", finalSyncedPlayers), true);

		} catch (Exception e) {
			source.sendFailure(Component.translatable("command.dragonminez.reload.error", e.getMessage()));
			e.printStackTrace();
		}

		return 1;
	}

	private enum ReloadScope {
		ALL,
		CONFIG,
		STORY,
		WISHES;

		private static ReloadScope from(String value) {
			try {
				return ReloadScope.valueOf(value.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException exception) {
				return null;
			}
		}

		private boolean includesConfig() {
			return this == ALL || this == CONFIG;
		}

		private boolean includesStory() {
			return this == ALL || this == STORY;
		}

		private boolean includesWishes() {
			return this == ALL || this == WISHES;
		}
	}
}