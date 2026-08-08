package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.StatsEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
import java.util.Set;

public class StatsCommand {
	private static final SuggestionProvider<CommandSourceStack> STAT_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(Set.of("STR", "SKP", "RES", "VIT", "PWR", "ENE", "ALL"), builder);

	private static final SuggestionProvider<CommandSourceStack> VALUE_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(List.of("100", "500", "1000", "5000", "10000", "min"), builder);

	private static final SuggestionProvider<CommandSourceStack> PERCENTAGE_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(List.of("10", "25", "50", "75"), builder);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzstats")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_INFO_SELF, DMZPermissions.STATS_INFO_OTHERS))

				// set <stat> <value> [targets]
				.then(Commands.literal("set")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_SET_SELF, DMZPermissions.STATS_SET_OTHERS))
						.then(Commands.argument("stat", StringArgumentType.word()).suggests(STAT_SUGGESTIONS)
								.then(Commands.argument("amount", StringArgumentType.word()).suggests(VALUE_SUGGESTIONS)
										.executes(ctx -> modifyStats(ctx.getSource(), StringArgumentType.getString(ctx, "stat"), StringArgumentType.getString(ctx, "amount"), List.of(ctx.getSource().getPlayerOrException()), "set"))
										.then(Commands.argument("targets", EntityArgument.players())
												.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_SET_OTHERS))
												.executes(ctx -> modifyStats(ctx.getSource(), StringArgumentType.getString(ctx, "stat"), StringArgumentType.getString(ctx, "amount"), EntityArgument.getPlayers(ctx, "targets"), "set"))))))

				// add <stat> <value> [targets]
				.then(Commands.literal("add")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_ADD_SELF, DMZPermissions.STATS_ADD_OTHERS))
						.then(Commands.argument("stat", StringArgumentType.word()).suggests(STAT_SUGGESTIONS)
								.then(Commands.argument("amount", StringArgumentType.word()).suggests(VALUE_SUGGESTIONS)
										.executes(ctx -> modifyStats(ctx.getSource(), StringArgumentType.getString(ctx, "stat"), StringArgumentType.getString(ctx, "amount"), List.of(ctx.getSource().getPlayerOrException()), "add"))
										.then(Commands.argument("targets", EntityArgument.players())
												.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_ADD_OTHERS))
												.executes(ctx -> modifyStats(ctx.getSource(), StringArgumentType.getString(ctx, "stat"), StringArgumentType.getString(ctx, "amount"), EntityArgument.getPlayers(ctx, "targets"), "add"))))))

				// remove <stat> <value> [targets]
				.then(Commands.literal("remove")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_ADD_SELF, DMZPermissions.STATS_ADD_OTHERS))
						.then(Commands.argument("stat", StringArgumentType.word()).suggests(STAT_SUGGESTIONS)
								.then(Commands.argument("amount", StringArgumentType.word()).suggests(VALUE_SUGGESTIONS)
										.executes(ctx -> modifyStats(ctx.getSource(), StringArgumentType.getString(ctx, "stat"), StringArgumentType.getString(ctx, "amount"), List.of(ctx.getSource().getPlayerOrException()), "remove"))
										.then(Commands.argument("targets", EntityArgument.players())
												.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_ADD_OTHERS))
												.executes(ctx -> modifyStats(ctx.getSource(), StringArgumentType.getString(ctx, "stat"), StringArgumentType.getString(ctx, "amount"), EntityArgument.getPlayers(ctx, "targets"), "remove"))))))

				// relocate [targets]
				.then(Commands.literal("relocate")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_RESET_SELF, DMZPermissions.STATS_RESET_OTHERS))
						.executes(ctx -> relocateStats(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_RESET_OTHERS))
								.executes(ctx -> relocateStats(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))

				// reset [targets] [keepPercentage] [keepSkills]
				.then(Commands.literal("reset")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_RESET_SELF, DMZPermissions.STATS_RESET_OTHERS))
						.executes(ctx -> resetStats(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), null, false))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_RESET_OTHERS))
								.executes(ctx -> resetStats(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), null, false))
								.then(Commands.argument("keepPercentage", StringArgumentType.word()).suggests(PERCENTAGE_SUGGESTIONS)
										.executes(ctx -> resetStats(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "keepPercentage"), false))
										.then(Commands.argument("keepSkills", BoolArgumentType.bool())
												.executes(ctx -> resetStats(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "keepPercentage"), BoolArgumentType.getBool(ctx, "keepSkills")))))))
		);
	}

	private static int modifyStats(CommandSourceStack source, String stat, String amountStr, Collection<ServerPlayer> targets, String mode) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		String finalStat = stat.toUpperCase();
		if (!isValidStat(finalStat)) {
			source.sendFailure(Component.translatable("command.dragonminez.stats.invalid_stat", stat));
			return 0;
		}

		// Raise RangedAttribute maxes to the configured maxValue before writing bases.
		com.dragonminez.common.stats.GenericAttributes.ensureAttributeCeilings();

		int value;
		try {
			if (amountStr.equalsIgnoreCase("min")) value = 0;
			else value = Integer.parseInt(amountStr);
		} catch (NumberFormatException e) {
			source.sendFailure(Component.translatable("command.dragonminez.stats.invalid_number", amountStr));
			return 0;
		}

		int successCount = 0;
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				float oldHealthBonus = data.getHealthBonus();
				float oldMaxEnergy = data.getMaxEnergy();
				float oldMaxStamina = data.getMaxStamina();

				if (finalStat.equals("ALL")) {
					for (String s : new String[]{"STR", "SKP", "RES", "VIT", "PWR", "ENE"}) applyModification(data, s, value, mode);
				} else applyModification(data, finalStat, value, mode);

				// Keep attribute mirrors in sync after large absolute sets (VIT/ENE fields → attributes).
				data.reapplyStatAttributes();

				float newHealthBonus = data.getHealthBonus();
				float healthDiff = newHealthBonus - oldHealthBonus;

				if (healthDiff != 0) {
					StatsEvents.applyHealthBonus(player);
					if (healthDiff > 0) player.heal(healthDiff);
				}

				// Max ki/stamina like health: fill on absolute set of the driving stat; else grant delta.
				if ("set".equals(mode) && (finalStat.equals("ALL") || finalStat.equals("ENE"))) {
					data.getResources().setCurrentEnergy(data.getMaxEnergy());
				} else {
					data.getResources().grantMaxPoolIncrease(oldMaxEnergy, data.getMaxEnergy(), true);
				}
				if ("set".equals(mode) && (finalStat.equals("ALL") || finalStat.equals("RES"))) {
					data.getResources().setCurrentStamina(data.getMaxStamina());
				} else {
					data.getResources().grantMaxPoolIncrease(oldMaxStamina, data.getMaxStamina(), false);
				}

				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
			successCount++;
		}

		if (successCount == 1 && targets.size() == 1) {
			ServerPlayer single = targets.iterator().next();
			source.sendSuccess(() -> Component.translatable("command.dragonminez.stats." + mode + ".success", finalStat, amountStr, single.getName().getString()), log);
		} else {
			int finalSuccess = successCount;
			source.sendSuccess(() -> Component.translatable("command.dragonminez.stats." + mode + ".multiple", finalSuccess, finalStat, amountStr), log);
		}

		return successCount;
	}

	private static void applyModification(StatsData data, String stat, int value, String mode) {
		int current = data.getCurrentStatValue(stat);
		switch (mode) {
			case "set" -> {
				// Admin absolute set: each stat independently clamped to [0, maxValue].
				// Do not use the shared total budget — that overflowed at Integer.MAX_VALUE
				// when maxValue is 1e9 (STR+SKP ate the pool; RES got ~147M; VIT/PWR/ENE starved).
				int target = data.clampStatToConfiguredMax(value);
				data.getStats().setStat(stat, target);
			}
			case "add" -> {
				int increase = data.getMaxAllowedIncreaseForStat(stat, value);
				if (increase > 0) data.getStats().addStat(stat, increase);
			}
			case "remove" -> data.getStats().removeStat(stat, value);
		}
	}

	private static int resetStats(CommandSourceStack source, Collection<ServerPlayer> targets, String keepPercentageStr, boolean keepSkills) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		Integer keepPercentage = null;
		if (keepPercentageStr != null && !keepPercentageStr.isEmpty()) {
			try {
				keepPercentage = Integer.parseInt(keepPercentageStr);
				if (keepPercentage <= -1 || keepPercentage >= 101) {
					source.sendFailure(Component.translatable("command.dragonminez.stats.invalid_number", keepPercentageStr));
					return 0;
				}
			} catch (NumberFormatException e) {
				source.sendFailure(Component.translatable("command.dragonminez.stats.invalid_number", keepPercentageStr));
				return 0;
			}
		}

		final Integer finalKeepPercentage = keepPercentage;

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.resetPlayerProgress(player, finalKeepPercentage, keepSkills, false);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.stats.reset.success", targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.stats.reset.multiple", targets.size()), log);
		}
		return targets.size();
	}

	private static int relocateStats(CommandSourceStack source, Collection<ServerPlayer> targets) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();

		int successCount = 0;
		for (ServerPlayer player : targets) {
			var result = StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
				int gained = data.relocateStats(player);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
				return gained;
			}).orElse(0);
			if (result > 0) successCount++;
		}

		if (targets.size() == 1) {
			ServerPlayer single = targets.iterator().next();
			source.sendSuccess(() -> Component.translatable("command.dragonminez.stats.relocate.success", single.getName().getString()), log);
		} else {
			int finalSuccess = successCount;
			source.sendSuccess(() -> Component.translatable("command.dragonminez.stats.relocate.multiple", finalSuccess), log);
		}
		return successCount;
	}

	private static boolean isValidStat(String stat) {
		return Set.of("STR", "SKP", "RES", "VIT", "PWR", "ENE", "ALL").contains(stat);
	}
}