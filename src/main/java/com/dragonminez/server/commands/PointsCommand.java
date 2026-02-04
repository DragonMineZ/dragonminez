package com.dragonminez.server.commands;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public class PointsCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzpoints")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.POINTS_INFO_SELF, DMZPermissions.POINTS_INFO_OTHERS))

				// set <amount> [targets]
				.then(Commands.literal("set")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.POINTS_SET_SELF, DMZPermissions.POINTS_SET_OTHERS))
						.then(Commands.argument("amount", IntegerArgumentType.integer(0))
								.executes(ctx -> setPoints(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(ctx, "amount")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.POINTS_SET_OTHERS))
										.executes(ctx -> setPoints(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "amount"))))))

				// add <amount> [targets]
				.then(Commands.literal("add")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.POINTS_ADD_SELF, DMZPermissions.POINTS_ADD_OTHERS))
						.then(Commands.argument("amount", IntegerArgumentType.integer())
								.executes(ctx -> addPoints(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(ctx, "amount")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.POINTS_ADD_OTHERS))
										.executes(ctx -> addPoints(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "amount"))))))

				// remove <amount> [targets]
				.then(Commands.literal("remove")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.POINTS_REMOVE_SELF, DMZPermissions.POINTS_REMOVE_OTHERS))
						.then(Commands.argument("amount", IntegerArgumentType.integer(0))
								.executes(ctx -> removePoints(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(ctx, "amount")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.POINTS_REMOVE_OTHERS))
										.executes(ctx -> removePoints(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "amount"))))))
		);
	}

	private static int setPoints(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getResources().setTrainingPoints(amount);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}
		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.points.set.success", amount, targets.iterator().next().getName().getString()), true);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.points.set.multiple", amount, targets.size()), true);
		}
		return targets.size();
	}

	private static int addPoints(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getResources().addTrainingPoints(amount);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}
		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.points.add.success", amount, targets.iterator().next().getName().getString()), true);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.points.add.multiple", amount, targets.size()), true);
		}
		return targets.size();
	}

	private static int removePoints(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				int currentPoints = data.getResources().getTrainingPoints();
				int newPoints = Math.max(0, currentPoints - amount);
				data.getResources().setTrainingPoints(newPoints);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}
		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.points.remove.success", amount, targets.iterator().next().getName().getString()), true);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.points.remove.multiple", amount, targets.size()), true);
		}
		return targets.size();
	}
}