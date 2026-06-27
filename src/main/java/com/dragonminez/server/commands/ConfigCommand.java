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
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class ConfigCommand {

	private static final SuggestionProvider<CommandSourceStack> CONFIG_FILE_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(ConfigManager.getAvailableConfigFiles(), builder);

	private static final SuggestionProvider<CommandSourceStack> KEY_OR_SUBTYPE_SUGGESTIONS = (ctx, builder) -> {
		String file = StringArgumentType.getString(ctx, "configFile");
		return SharedSuggestionProvider.suggest(ConfigManager.getKeysOrSubtypes(file, null), builder);
	};

	private static final SuggestionProvider<CommandSourceStack> REST_SUGGESTIONS = (ctx, builder) -> {
		String file = StringArgumentType.getString(ctx, "configFile");
		String first = StringArgumentType.getString(ctx, "keyOrSubtype");
		String remaining = builder.getRemaining();
		int lastSpace = remaining.lastIndexOf(' ');
		SuggestionsBuilder offset = builder.createOffset(builder.getStart() + lastSpace + 1);

		List<String> options;
		if (ConfigManager.isSubtype(file, first)) {
			if (lastSpace < 0) options = ConfigManager.getKeysOrSubtypes(file, first);
			else {
				String nestedKey = remaining.substring(0, lastSpace).trim();
				options = ConfigManager.getValueSuggestions(file, first, nestedKey);
			}
		} else options = ConfigManager.getValueSuggestions(file, null, first);
		return SharedSuggestionProvider.suggest(options, offset);
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzconfig")
				.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.ADMIN))
				.then(Commands.argument("configFile", StringArgumentType.word()).suggests(CONFIG_FILE_SUGGESTIONS)
						.then(Commands.argument("keyOrSubtype", StringArgumentType.word()).suggests(KEY_OR_SUBTYPE_SUGGESTIONS)
								.then(Commands.argument("rest", StringArgumentType.greedyString()).suggests(REST_SUGGESTIONS)
										.executes(ctx -> executeConfigChange(
												ctx.getSource(),
												StringArgumentType.getString(ctx, "configFile"),
												StringArgumentType.getString(ctx, "keyOrSubtype"),
												StringArgumentType.getString(ctx, "rest")))
								)
						)
				)
		);
	}

	private static int executeConfigChange(CommandSourceStack source, String configFile, String keyOrSubtype, String rest) {
		MinecraftServer server = source.getServer();

		String subtype = null;
		String key = keyOrSubtype;
		String value = rest.trim();

		int sp = value.indexOf(' ');
		if (sp > 0 && ConfigManager.isSubtype(configFile, keyOrSubtype)) {
			subtype = keyOrSubtype;
			key = value.substring(0, sp).trim();
			value = value.substring(sp + 1).trim();
		}

		final String displayKey = subtype != null ? subtype + "." + key : key;
		final String displayValue = value;

		try {
			boolean success = ConfigManager.updateConfigValue(configFile, subtype, key, value);

			if (success) {
				source.sendSuccess(() -> Component.translatable("command.dragonminez.config.success", displayKey, displayValue), true);
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
			if (jsonPayload == null || jsonPayload.isBlank()) return;

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