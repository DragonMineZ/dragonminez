package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
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

public class AlignmentCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzalignment")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.ALIGNMENT_INFO_SELF, DMZPermissions.ALIGNMENT_INFO_OTHERS))

				.then(Commands.literal("set")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.ALIGNMENT_SET_SELF, DMZPermissions.ALIGNMENT_SET_OTHERS))
						.then(Commands.argument("amount", IntegerArgumentType.integer(0, 100))
								.executes(ctx -> setAlignment(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(ctx, "amount")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.ALIGNMENT_SET_OTHERS))
										.executes(ctx -> setAlignment(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "amount"))))))

				.then(Commands.literal("add")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.ALIGNMENT_ADD_SELF, DMZPermissions.ALIGNMENT_ADD_OTHERS))
						.then(Commands.argument("amount", IntegerArgumentType.integer(1))
								.executes(ctx -> addAlignment(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(ctx, "amount")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.ALIGNMENT_ADD_OTHERS))
										.executes(ctx -> addAlignment(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "amount"))))))

				.then(Commands.literal("remove")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.ALIGNMENT_REMOVE_SELF, DMZPermissions.ALIGNMENT_REMOVE_OTHERS))
						.then(Commands.argument("amount", IntegerArgumentType.integer(1))
								.executes(ctx -> removeAlignment(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(ctx, "amount")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.ALIGNMENT_REMOVE_OTHERS))
										.executes(ctx -> removeAlignment(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "amount"))))))
		);
	}

	private static int setAlignment(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getResources().setAlignment(amount);
				NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
			});
		}
		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.alignment.set.success", amount, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.alignment.set.multiple", amount, targets.size()), log);
		}
		return targets.size();
	}

	private static int addAlignment(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getResources().addAlignment(amount);
				NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
			});
		}
		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.alignment.add.success", amount, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.alignment.add.multiple", amount, targets.size()), log);
		}
		return targets.size();
	}

	private static int removeAlignment(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getResources().removeAlignment(amount);
				NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
			});
		}
		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.alignment.remove.success", amount, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.alignment.remove.multiple", amount, targets.size()), log);
		}
		return targets.size();
	}
}
