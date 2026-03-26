package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class PointsCommand {
	private static final DecimalFormat POINTS_FORMAT = createPointsFormat();

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzpoints")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.POINTS_INFO_SELF, DMZPermissions.POINTS_INFO_OTHERS))

				// set <amount> [targets]
				.then(Commands.literal("set")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.POINTS_SET_SELF, DMZPermissions.POINTS_SET_OTHERS))
						.then(Commands.argument("amount", FloatArgumentType.floatArg(0, Float.MAX_VALUE - 1))
								.executes(ctx -> setPoints(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "amount")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.POINTS_SET_OTHERS))
										.executes(ctx -> setPoints(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount"))))))

				// add <amount> [targets]
				.then(Commands.literal("add")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.POINTS_ADD_SELF, DMZPermissions.POINTS_ADD_OTHERS))
						.then(Commands.argument("amount", FloatArgumentType.floatArg(0, Float.MAX_VALUE - 1))
								.executes(ctx -> addPoints(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "amount")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.POINTS_ADD_OTHERS))
										.executes(ctx -> addPoints(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount"))))))

				// remove <amount> [targets]
				.then(Commands.literal("remove")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.POINTS_REMOVE_SELF, DMZPermissions.POINTS_REMOVE_OTHERS))
						.then(Commands.argument("amount", FloatArgumentType.floatArg(0, Float.MAX_VALUE - 1))
								.executes(ctx -> removePoints(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "amount")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.POINTS_REMOVE_OTHERS))
										.executes(ctx -> removePoints(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount"))))))
		);
	}

	private static int setPoints(CommandSourceStack source, Collection<ServerPlayer> targets, float amount) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		int normalizedAmount = normalizePoints(amount);
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getResources().setTrainingPoints(normalizedAmount);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}
		String formatted = formatPoints(normalizedAmount);
		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.points.set.success", formatted, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.points.set.multiple", formatted, targets.size()), log);
		}
		return targets.size();
	}

	private static int addPoints(CommandSourceStack source, Collection<ServerPlayer> targets, float amount) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		int normalizedAmount = normalizePoints(amount);
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				float currentPoints = data.getResources().getTrainingPoints();
				float newPoints = Math.min(Float.MAX_VALUE - 1, currentPoints + normalizedAmount);
				data.getResources().setTrainingPoints(newPoints);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}
		String formatted = formatPoints(normalizedAmount);
		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.points.add.success", formatted, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.points.add.multiple", formatted, targets.size()), log);
		}
		return targets.size();
	}

	private static int removePoints(CommandSourceStack source, Collection<ServerPlayer> targets, float amount) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		int normalizedAmount = normalizePoints(amount);
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				float currentPoints = data.getResources().getTrainingPoints();
				float newPoints = Math.max(0, currentPoints - normalizedAmount);
				data.getResources().setTrainingPoints(newPoints);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}
		String formatted = formatPoints(normalizedAmount);
		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.points.remove.success", formatted, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.points.remove.multiple", formatted, targets.size()), log);
		}
		return targets.size();
	}

	private static int normalizePoints(float value) {
		return (int) Math.max(0, Math.min(Float.MAX_VALUE - 1, Math.round(value)));
	}

	private static String formatPoints(int value) {
		return POINTS_FORMAT.format(value);
	}

	private static DecimalFormat createPointsFormat() {
		DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
		symbols.setGroupingSeparator('.');
		DecimalFormat format = new DecimalFormat("#,###", symbols);
		format.setGroupingUsed(true);
		return format;
	}
}