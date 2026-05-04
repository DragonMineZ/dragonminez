package com.dragonminez.server.commands;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.network.S2C.SyncServerConfigS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class ConfigCommand {

	private static final SuggestionProvider<CommandSourceStack> CONFIG_FILE_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(ConfigManager.getAvailableConfigFiles(), builder);

	private static final SuggestionProvider<CommandSourceStack> SUBTYPE_OR_KEY_SUGGESTIONS = (ctx, builder) -> {
		String file = StringArgumentType.getString(ctx, "configFile");
		return SharedSuggestionProvider.suggest(ConfigManager.getKeysOrSubtypes(file, null), builder);
	};

	private static final SuggestionProvider<CommandSourceStack> KEY_INSIDE_SUBTYPE_SUGGESTIONS = (ctx, builder) -> {
		String file = StringArgumentType.getString(ctx, "configFile");
		String subtype = StringArgumentType.getString(ctx, "optionalSubtype_or_key");
		return SharedSuggestionProvider.suggest(ConfigManager.getKeysOrSubtypes(file, subtype), builder);
	};

	private static final SuggestionProvider<CommandSourceStack> VALUE_FLAT_SUGGESTIONS = (ctx, builder) -> {
		String file = StringArgumentType.getString(ctx, "configFile");
		String key = StringArgumentType.getString(ctx, "optionalSubtype_or_key");
		return SharedSuggestionProvider.suggest(ConfigManager.getValueSuggestions(file, null, key), builder);
	};

	private static final SuggestionProvider<CommandSourceStack> VALUE_SUBTYPE_SUGGESTIONS = (ctx, builder) -> {
		String file = StringArgumentType.getString(ctx, "configFile");
		String subtype = StringArgumentType.getString(ctx, "optionalSubtype_or_key");
		String key = StringArgumentType.getString(ctx, "configKey");
		return SharedSuggestionProvider.suggest(ConfigManager.getValueSuggestions(file, subtype, key), builder);
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzconfig")
				.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.ADMIN))
				.then(Commands.argument("configFile", StringArgumentType.word()).suggests(CONFIG_FILE_SUGGESTIONS)
						.then(Commands.argument("optionalSubtype_or_key", StringArgumentType.word()).suggests(SUBTYPE_OR_KEY_SUGGESTIONS)
								.then(Commands.argument("valueFlat", StringArgumentType.greedyString()).suggests(VALUE_FLAT_SUGGESTIONS)
										.executes(ctx -> executeConfigChange(ctx.getSource(), StringArgumentType.getString(ctx, "configFile"), null, StringArgumentType.getString(ctx, "optionalSubtype_or_key"), StringArgumentType.getString(ctx, "valueFlat")))
								)
								.then(Commands.argument("configKey", StringArgumentType.word()).suggests(KEY_INSIDE_SUBTYPE_SUGGESTIONS)
										.then(Commands.argument("valueSub", StringArgumentType.greedyString()).suggests(VALUE_SUBTYPE_SUGGESTIONS)
												.executes(ctx -> executeConfigChange(ctx.getSource(), StringArgumentType.getString(ctx, "configFile"), StringArgumentType.getString(ctx, "optionalSubtype_or_key"), StringArgumentType.getString(ctx, "configKey"), StringArgumentType.getString(ctx, "valueSub")))
										)
								)
						)
				)
		);
	}

	private static int executeConfigChange(CommandSourceStack source, String configFile, String optionalSubtype, String configKey, String value) {
		MinecraftServer server = source.getServer();
		boolean success;

		try {
			success = ConfigManager.updateConfigValue(configFile, optionalSubtype, configKey, value);

			if (success) {
				source.sendSuccess(() -> Component.translatable("command.dragonminez.config.success", configKey, value), true);
				syncSingleConfig(server, configFile);
				return 1;
			} else {
				source.sendFailure(Component.translatable("command.dragonminez.config.invalid"));
				return 0;
			}
		} catch (Exception e) {
			source.sendFailure(Component.translatable("command.dragonminez.config.error" + e.getMessage()));
			return 0;
		}
	}

	private static void syncSingleConfig(MinecraftServer server, String configFile) {
		try {
			ConfigManager.reloadSpecificConfig(configFile);
			String jsonPayload = ConfigManager.getSpecificConfigJson(configFile);

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				NetworkHandler.sendToPlayer(new SyncServerConfigS2C(configFile, jsonPayload), player);

				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
					String raceName = data.getCharacter().getRaceName();
					if (raceName != null && !raceName.isEmpty()) data.updateTransformationSkillLimits(raceName);
					else data.getSkills().refreshNonFormSkillMaxLevels();
					NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
				});
			}
		} catch (Exception e) {
			LogUtil.error(Env.SERVER, "Error syncing config to clients: " + e.getMessage());
		}
	}
}