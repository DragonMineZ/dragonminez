package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public class EffectsCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzeffect")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.EFFECTS_LIST_SELF, DMZPermissions.EFFECTS_LIST_OTHERS))

				// give <effect> <duration> [targets]
				.then(Commands.literal("give")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.EFFECTS_GIVE_SELF, DMZPermissions.EFFECTS_GIVE_OTHERS))
						.then(Commands.argument("effect", StringArgumentType.string())
								.then(Commands.argument("duration", IntegerArgumentType.integer(-1))
										.executes(ctx -> giveEffect(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "effect"), IntegerArgumentType.getInteger(ctx, "duration")))
										.then(Commands.argument("targets", EntityArgument.players())
												.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.EFFECTS_GIVE_OTHERS))
												.executes(ctx -> giveEffect(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "effect"), IntegerArgumentType.getInteger(ctx, "duration")))))))

				// remove <effect> [targets]
				.then(Commands.literal("remove")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.EFFECTS_REMOVE_SELF, DMZPermissions.EFFECTS_REMOVE_OTHERS))
						.then(Commands.argument("effect", StringArgumentType.string())
								.executes(ctx -> removeEffect(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "effect")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.EFFECTS_REMOVE_OTHERS))
										.executes(ctx -> removeEffect(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "effect"))))))

				// clear [targets]
				.then(Commands.literal("clear")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.EFFECTS_CLEAR_SELF, DMZPermissions.EFFECTS_CLEAR_OTHERS))
						.executes(ctx -> clearEffects(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.EFFECTS_CLEAR_OTHERS))
								.executes(ctx -> clearEffects(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
		);
	}

	private static int giveEffect(CommandSourceStack source, Collection<ServerPlayer> targets, String effectName, int duration) {
		boolean log = ConfigManager.getServerConfig().getGameplay().isCommandOutputOnConsole();
		double power = getEffectPower(effectName);
		if (power == 0.0) {
			source.sendFailure(Component.translatable("command.dragonminez.effects.unknown_effect", effectName));
			return 0;
		}

		int durationInTicks = duration == -1 ? -1 : duration * 20;
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getEffects().addEffect(effectName, power, durationInTicks);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}

		String durationText = duration == -1 ? "permanent" : duration + " seconds";

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.effects.give_success", effectName, power, targets.iterator().next().getName().getString(), durationText), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.effects.give_multiple", effectName, power, targets.size(), durationText), log);
		}
		return targets.size();
	}

	private static int removeEffect(CommandSourceStack source, Collection<ServerPlayer> targets, String effectName) {
		boolean log = ConfigManager.getServerConfig().getGameplay().isCommandOutputOnConsole();
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (data.getEffects().hasEffect(effectName)) {
					data.getEffects().removeEffect(effectName);
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
				}
			});
		}
		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.effects.remove_success", effectName, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.effects.remove_multiple", effectName, targets.size()), log);
		}
		return targets.size();
	}

	private static int clearEffects(CommandSourceStack source, Collection<ServerPlayer> targets) {
		boolean log = ConfigManager.getServerConfig().getGameplay().isCommandOutputOnConsole();
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getEffects().clear();
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}
		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.effects.clear_success", targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.effects.clear_multiple", targets.size()), log);
		}
		return targets.size();
	}

	private static double getEffectPower(String effectName) {
		var serverConfig = ConfigManager.getServerConfig();
		if (serverConfig == null) {
			return 0.0;
		}

		return switch (effectName.toLowerCase()) {
			case "mightfruit" -> serverConfig.getGameplay().getMightFruitPower() - 1.0;
			case "majin" -> serverConfig.getGameplay().getMajinPower() - 1.0;
			default -> 0.0;
		};
	}
}