package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class BonusCommand {

	private static final SuggestionProvider<CommandSourceStack> STAT_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(Set.of("STR", "SKP", "RES", "VIT", "PWR", "ENE", "ALL"), builder);

	private static final SuggestionProvider<CommandSourceStack> OPERATOR_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(Set.of("+", "-", "*"), builder);

	private static final SuggestionProvider<CommandSourceStack> BONUS_NAME_SUGGESTIONS = (ctx, builder) -> {
		try {
			ServerPlayer player = ctx.getSource().getPlayerOrException();
			String stat = StringArgumentType.getString(ctx, "stat").toUpperCase();

			return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
				CompoundTag tag = data.getBonusStats().save();
				List<String> bonusNames = new ArrayList<>();

				if (tag.contains(stat)) {
					ListTag list = tag.getList(stat, 10);
					for (int i = 0; i < list.size(); i++) {
						CompoundTag bonusTag = list.getCompound(i);
						if (bonusTag.contains("Name")) {
							bonusNames.add(bonusTag.getString("Name"));
						}
					}
				}
				return SharedSuggestionProvider.suggest(bonusNames, builder);
			}).orElse(SharedSuggestionProvider.suggest(new String[]{}, builder));
		} catch (Exception e) {
			return SharedSuggestionProvider.suggest(new String[]{}, builder);
		}
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzbonus")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.BONUS_ADD_SELF, DMZPermissions.BONUS_ADD_OTHERS))

				// add <stat> <operation> <value> <bonusName> [targets]
				.then(Commands.literal("add")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.BONUS_ADD_SELF, DMZPermissions.BONUS_ADD_OTHERS))
						.then(Commands.argument("stat", StringArgumentType.word()).suggests(STAT_SUGGESTIONS)
								.then(Commands.argument("operation", StringArgumentType.word()).suggests(OPERATOR_SUGGESTIONS)
										.then(Commands.argument("value", DoubleArgumentType.doubleArg())
												.then(Commands.argument("bonusName", StringArgumentType.word())
														.executes(ctx -> addBonus(ctx.getSource(), StringArgumentType.getString(ctx, "stat"), StringArgumentType.getString(ctx, "operation"), DoubleArgumentType.getDouble(ctx, "value"), StringArgumentType.getString(ctx, "bonusName"), List.of(ctx.getSource().getPlayerOrException())))
														.then(Commands.argument("targets", EntityArgument.players())
																.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.BONUS_ADD_OTHERS))
																.executes(ctx -> addBonus(ctx.getSource(), StringArgumentType.getString(ctx, "stat"), StringArgumentType.getString(ctx, "operation"), DoubleArgumentType.getDouble(ctx, "value"), StringArgumentType.getString(ctx, "bonusName"), EntityArgument.getPlayers(ctx, "targets")))))))))

				// remove <stat> <bonusName> [targets]
				.then(Commands.literal("remove")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.BONUS_CLEAR_SELF, DMZPermissions.BONUS_CLEAR_OTHERS))
						.then(Commands.argument("stat", StringArgumentType.word()).suggests(STAT_SUGGESTIONS)
								.then(Commands.argument("bonusName", StringArgumentType.word()).suggests(BONUS_NAME_SUGGESTIONS)
										.executes(ctx -> removeBonus(ctx.getSource(), StringArgumentType.getString(ctx, "stat"), StringArgumentType.getString(ctx, "bonusName"), List.of(ctx.getSource().getPlayerOrException())))
										.then(Commands.argument("targets", EntityArgument.players())
												.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.BONUS_CLEAR_OTHERS))
												.executes(ctx -> removeBonus(ctx.getSource(), StringArgumentType.getString(ctx, "stat"), StringArgumentType.getString(ctx, "bonusName"), EntityArgument.getPlayers(ctx, "targets")))))))

				// clear <stat> [targets] (Elimina TODOS los bonus de la stat)
				.then(Commands.literal("clear")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.BONUS_CLEAR_SELF, DMZPermissions.BONUS_CLEAR_OTHERS))
						.then(Commands.argument("stat", StringArgumentType.word()).suggests(STAT_SUGGESTIONS)
								.executes(ctx -> clearStat(ctx.getSource(), StringArgumentType.getString(ctx, "stat"), List.of(ctx.getSource().getPlayerOrException())))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.BONUS_CLEAR_OTHERS))
										.executes(ctx -> clearStat(ctx.getSource(), StringArgumentType.getString(ctx, "stat"), EntityArgument.getPlayers(ctx, "targets"))))))
		);
	}

	private static int addBonus(CommandSourceStack source, String stat, String operation, double value, String bonusName, Collection<ServerPlayer> targets) {
		boolean log = ConfigManager.getServerConfig().getGameplay().isCommandOutputOnConsole();
		String finalStat = stat.toUpperCase();

		if (!isValidStat(finalStat) && !finalStat.equals("ALL")) {
			source.sendFailure(Component.translatable("command.dragonminez.bonus.invalid_stat"));
			return 0;
		}

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (finalStat.equals("ALL")) {
					for (String s : new String[]{"STR", "SKP", "RES", "VIT", "PWR", "ENE"}) {
						data.getBonusStats().addBonus(s, bonusName, operation, value);
					}
				} else {
					data.getBonusStats().addBonus(finalStat, bonusName, operation, value);
				}
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.bonus.add.success", bonusName, finalStat, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.bonus.add.multiple", bonusName, targets.size(), finalStat), log);
		}
		return targets.size();
	}

	private static int removeBonus(CommandSourceStack source, String stat, String bonusName, Collection<ServerPlayer> targets) {
		boolean log = ConfigManager.getServerConfig().getGameplay().isCommandOutputOnConsole();
		String finalStat = stat.toUpperCase();

		if (!isValidStat(finalStat)) {
			source.sendFailure(Component.translatable("command.dragonminez.bonus.invalid_stat"));
			return 0;
		}

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getBonusStats().removeBonus(finalStat, bonusName);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.bonus.remove.success", bonusName, finalStat, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.bonus.remove.multiple", bonusName, targets.size(), finalStat), log);
		}
		return targets.size();
	}

	private static int clearStat(CommandSourceStack source, String stat, Collection<ServerPlayer> targets) {
		boolean log = ConfigManager.getServerConfig().getGameplay().isCommandOutputOnConsole();
		String finalStat = stat.toUpperCase();

		if (!isValidStat(finalStat) && !finalStat.equals("ALL")) {
			source.sendFailure(Component.translatable("command.dragonminez.bonus.invalid_stat"));
			return 0;
		}

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (finalStat.equals("ALL")) {
					data.getBonusStats().clearAllStats();
				} else {
					data.getBonusStats().clearAll(finalStat);
				}
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.bonus.clear.success", finalStat, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.bonus.clear.multiple", finalStat, targets.size()), log);
		}
		return targets.size();
	}

	private static boolean isValidStat(String stat) {
		return Set.of("STR", "SKP", "RES", "VIT", "PWR", "ENE").contains(stat);
	}
}