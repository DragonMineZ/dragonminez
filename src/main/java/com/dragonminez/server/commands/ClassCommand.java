package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public class ClassCommand {

	private static final SuggestionProvider<CommandSourceStack> CLASS_SUGGESTIONS = (ctx, builder) -> {
		TreeSet<String> classes = new TreeSet<>();
		for (String race : ConfigManager.getLoadedRaces()) {
			RaceStatsConfig cfg = ConfigManager.getRaceStats(race);
			if (cfg != null) classes.addAll(cfg.getAllClasses());
		}
		return SharedSuggestionProvider.suggest(classes, builder);
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzclass")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.CLASS_SET_SELF, DMZPermissions.CLASS_SET_OTHERS))
				.then(Commands.argument("class", StringArgumentType.word()).suggests(CLASS_SUGGESTIONS)
						.executes(ctx -> setClass(ctx.getSource(), StringArgumentType.getString(ctx, "class"), List.of(ctx.getSource().getPlayerOrException())))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.CLASS_SET_OTHERS))
								.executes(ctx -> setClass(ctx.getSource(), StringArgumentType.getString(ctx, "class"), EntityArgument.getPlayers(ctx, "targets")))))
		);
	}

	private static boolean isValidClass(StatsData data, String characterClass) {
		RaceStatsConfig cfg = ConfigManager.getRaceStats(data.getCharacter().getRaceName());
		return cfg != null && cfg.getAllClasses().contains(characterClass);
	}

	private static int setClass(CommandSourceStack source, String rawClass, Collection<ServerPlayer> targets) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		String characterClass = rawClass.toLowerCase();

		if (targets.size() == 1) {
			ServerPlayer player = targets.iterator().next();
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (data == null) return 0;
			if (!isValidClass(data, characterClass)) {
				source.sendFailure(Component.translatable("command.dragonminez.class.invalid", rawClass, player.getName().getString()));
				return 0;
			}
			Character character = data.getCharacter();
			if (characterClass.equals(character.getCharacterClass())) {
				source.sendFailure(Component.translatable("command.dragonminez.class.already", player.getName().getString(), rawClass));
				return 0;
			}
			applyClass(player, data, characterClass);
			if (player == source.getEntity()) {
				source.sendSuccess(() -> Component.translatable("command.dragonminez.class.set.self", rawClass), log);
			} else {
				source.sendSuccess(() -> Component.translatable("command.dragonminez.class.set.other", player.getName().getString(), rawClass), log);
			}
			return 1;
		}

		int success = 0;
		for (ServerPlayer player : targets) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (data == null || !isValidClass(data, characterClass)) continue;
			if (characterClass.equals(data.getCharacter().getCharacterClass())) continue;
			applyClass(player, data, characterClass);
			success++;
		}
		int finalSuccess = success;
		source.sendSuccess(() -> Component.translatable("command.dragonminez.class.set.multiple", rawClass, finalSuccess), log);
		return success;
	}

	private static void applyClass(ServerPlayer player, StatsData data, String characterClass) {
		float[] snapshot = data.snapshotMultiplierResources();
		data.getCharacter().setCharacterClass(characterClass);
		data.restoreMultiplierGains(player, snapshot);
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}
}
