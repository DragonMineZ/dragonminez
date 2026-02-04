package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MasteryCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzmastery")
				.requires(source -> source.hasPermission(2))

				// set <group> <form> <value> [targets]
				.then(Commands.literal("set")
						.then(Commands.argument("group", StringArgumentType.word())
								.then(Commands.argument("form", StringArgumentType.word())
										.then(Commands.argument("value", DoubleArgumentType.doubleArg(0))
												.executes(ctx -> setMastery(ctx.getSource(), StringArgumentType.getString(ctx, "group"), StringArgumentType.getString(ctx, "form"), DoubleArgumentType.getDouble(ctx, "value"), List.of(ctx.getSource().getPlayerOrException()), false))
												.then(Commands.argument("targets", EntityArgument.players())
														.executes(ctx -> setMastery(ctx.getSource(), StringArgumentType.getString(ctx, "group"), StringArgumentType.getString(ctx, "form"), DoubleArgumentType.getDouble(ctx, "value"), EntityArgument.getPlayers(ctx, "targets"), false)))))))

				// add <group> <form> <value> [targets]
				.then(Commands.literal("add")
						.then(Commands.argument("group", StringArgumentType.word())
								.then(Commands.argument("form", StringArgumentType.word())
										.then(Commands.argument("value", DoubleArgumentType.doubleArg())
												.executes(ctx -> setMastery(ctx.getSource(), StringArgumentType.getString(ctx, "group"), StringArgumentType.getString(ctx, "form"), DoubleArgumentType.getDouble(ctx, "value"), List.of(ctx.getSource().getPlayerOrException()), true))
												.then(Commands.argument("targets", EntityArgument.players())
														.executes(ctx -> setMastery(ctx.getSource(), StringArgumentType.getString(ctx, "group"), StringArgumentType.getString(ctx, "form"), DoubleArgumentType.getDouble(ctx, "value"), EntityArgument.getPlayers(ctx, "targets"), true)))))))
		);
	}

	private static int setMastery(CommandSourceStack source, String group, String form, double value, Collection<ServerPlayer> targets, boolean add) {
		int count = 0;
		AtomicReference<Double> MAX_MASTERY = new AtomicReference<>(100.0);

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				var masteries = data.getCharacter().getFormMasteries();
				FormConfig.FormData formData = ConfigManager.getForm(data.getCharacter().getRaceName(), group, form);
				if (formData != null) MAX_MASTERY.set(formData.getMaxMastery());

				if (add) {
					masteries.addMastery(group, form, value, MAX_MASTERY.get());
				} else {
					masteries.setMastery(group, form, value);
				}
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
			count++;
		}

		String modeKey = add ? "add" : "set";
		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.mastery." + modeKey + ".success", group, form, value, targets.iterator().next().getName().getString()), true);
		} else {
			int finalCount = count;
			source.sendSuccess(() -> Component.translatable("command.dragonminez.mastery." + modeKey + ".multiple", group, form, value, finalCount), true);
		}
		return count;
	}
}