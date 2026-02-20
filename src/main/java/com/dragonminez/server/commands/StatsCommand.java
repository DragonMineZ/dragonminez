package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.StatsEvents;
import com.dragonminez.server.util.FusionLogic;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class StatsCommand {
	private static final SuggestionProvider<CommandSourceStack> STAT_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(Set.of("STR", "SKP", "RES", "VIT", "PWR", "ENE", "ALL"), builder);

	private static final SuggestionProvider<CommandSourceStack> VALUE_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(List.of("max", "100", "500", "1000", "5000", "10000"), builder);

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

				// reset [keepPercentage] [targets]
				.then(Commands.literal("reset")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.STATS_RESET_SELF, DMZPermissions.STATS_RESET_OTHERS))
						.executes(ctx -> resetStats(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), null))
						.then(Commands.argument("keepPercentage", StringArgumentType.word()).suggests(PERCENTAGE_SUGGESTIONS)
								.executes(ctx -> resetStats(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "keepPercentage")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_RESET_OTHERS))
										.executes(ctx -> resetStats(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "keepPercentage")))))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.STATS_RESET_OTHERS))
								.executes(ctx -> resetStats(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), null))))
		);
	}

	private static int modifyStats(CommandSourceStack source, String stat, String amountStr, Collection<ServerPlayer> targets, String mode) {
		boolean log = ConfigManager.getServerConfig().getGameplay().isCommandOutputOnConsole();
		String finalStat = stat.toUpperCase();
		if (!isValidStat(finalStat)) {
			source.sendFailure(Component.translatable("command.dragonminez.stats.invalid_stat", stat));
			return 0;
		}

		int value;
		int MAX_STAT_VALUE = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
		try {
			if (amountStr.equalsIgnoreCase("max")) value = MAX_STAT_VALUE;
			else if (amountStr.equalsIgnoreCase("min")) value = 5;
			else value = Integer.parseInt(amountStr);
		} catch (NumberFormatException e) {
			source.sendFailure(Component.translatable("command.dragonminez.stats.invalid_number", amountStr));
			return 0;
		}

		int successCount = 0;
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				float oldMaxHealth = data.getMaxHealth();
				int oldMaxEnergy = data.getMaxEnergy();
				int oldMaxStamina = data.getMaxStamina();

				if (finalStat.equals("ALL")) {
					for (String s : new String[]{"STR", "SKP", "RES", "VIT", "PWR", "ENE"}) {
						applyModification(data, s, value, mode);
					}
				} else {
					applyModification(data, finalStat, value, mode);
				}

				float newMaxHealth = data.getMaxHealth();
				if (newMaxHealth > oldMaxHealth) player.heal(newMaxHealth - oldMaxHealth);
				int newMaxEnergy = data.getMaxEnergy();
				if (newMaxEnergy > oldMaxEnergy) data.getResources().addEnergy(newMaxEnergy - oldMaxEnergy);
				int newMaxStamina = data.getMaxStamina();
				if (newMaxStamina > oldMaxStamina) data.getResources().addStamina(newMaxStamina - oldMaxStamina);

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
		int MAX_STAT_VALUE = ConfigManager.getServerConfig().getGameplay().getMaxStatValue();
		switch (mode) {
			case "set" -> data.getStats().setStat(stat, Math.min(value, MAX_STAT_VALUE));
			case "add" -> data.getStats().addStat(stat, value);
			case "remove" -> data.getStats().removeStat(stat, value);
		}
	}

	private static int resetStats(CommandSourceStack source, Collection<ServerPlayer> targets, String keepPercentageStr) {
		boolean log = ConfigManager.getServerConfig().getGameplay().isCommandOutputOnConsole();
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
		System.out.println("finalKeepPercentage: " + finalKeepPercentage + " | initial input: " + keepPercentageStr);

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				var stats = data.getStats();

				if (finalKeepPercentage != null) {
					int newStr = (stats.getStrength() * finalKeepPercentage) / 100;
					int newSkp = (stats.getStrikePower() * finalKeepPercentage) / 100;
					int newRes = (stats.getResistance() * finalKeepPercentage) / 100;
					int newVit = (stats.getVitality() * finalKeepPercentage) / 100;
					int newPwr = (stats.getKiPower() * finalKeepPercentage) / 100;
					int newEne = (stats.getEnergy() * finalKeepPercentage) / 100;
					int currentTPs = data.getResources().getTrainingPoints();
					int newTPs = (currentTPs * finalKeepPercentage) / 100;

					stats.setStrength(Math.max(5, newStr));
					stats.setStrikePower(Math.max(5, newSkp));
					stats.setResistance(Math.max(5, newRes));
					stats.setVitality(Math.max(5, newVit));
					stats.setKiPower(Math.max(5, newPwr));
					stats.setEnergy(Math.max(5, newEne));

					data.getResources().setTrainingPoints(newTPs);
				} else {
					stats.setStrength(5);
					stats.setStrikePower(5);
					stats.setResistance(5);
					stats.setVitality(5);
					stats.setKiPower(5);
					stats.setEnergy(5);
					data.getResources().setTrainingPoints(0);
				}

				if (data.getStatus().isFused()) FusionLogic.endFusion(player, data, false);
				data.getResources().setRacialSkillCount(0);
				data.getResources().setPowerRelease(0);
				data.getStatus().setAndroidUpgraded(false);
				data.getStatus().setInKaioPlanet(false);
				data.getSkills().removeAllSkills();
				data.getEffects().removeAllEffects();
				data.getCooldowns().clearCooldowns();
				data.getBonusStats().clearAllStats();
				data.getCharacter().clearActiveForm();
				data.getCharacter().clearActiveStackForm();
				data.getStatus().setCreatedCharacter(false);

				player.refreshDimensions();
				player.setHealth(20.0F);
				player.getAttribute(Attributes.MAX_HEALTH).removePermanentModifier(StatsEvents.DMZ_HEALTH_MODIFIER_UUID);
				player.setHealth(20.0F);
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

	private static boolean isValidStat(String stat) {
		return Set.of("STR", "SKP", "RES", "VIT", "PWR", "ENE", "ALL").contains(stat);
	}
}