package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AppearanceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.FormMasteries;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class MasteryCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzmastery")
				.then(modeBranch("set", false))
				.then(modeBranch("add", true))
		);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> modeBranch(String mode, boolean add) {
		var permission = add ? DMZPermissions.MASTERY_ADD : DMZPermissions.MASTERY_SET;

		return Commands.literal(mode)
				.requires(source -> DMZPermissions.hasPermission(source, permission))
				.then(Commands.argument("target", EntityArgument.player())
						// <group> <form> <value>
						.then(Commands.argument("group", StringArgumentType.word())
								.suggests(SUGGEST_GROUPS)
								.then(Commands.argument("form", StringArgumentType.word())
										.suggests(SUGGEST_FORMS)
										.then(Commands.argument("value", DoubleArgumentType.doubleArg())
												.executes(ctx -> setMastery(ctx, add)))))
						// current <stack|form> <value>
						.then(Commands.literal("current")
								.then(Commands.literal("form")
										.then(Commands.argument("value", DoubleArgumentType.doubleArg())
												.executes(ctx -> setCurrentMastery(ctx, add, false))))
								.then(Commands.literal("stack")
										.then(Commands.argument("value", DoubleArgumentType.doubleArg())
												.executes(ctx -> setCurrentMastery(ctx, add, true)))))
						// ALL <form|stack|all> <value>
						.then(Commands.literal("ALL")
								.then(Commands.literal("form")
										.then(Commands.argument("value", DoubleArgumentType.doubleArg())
												.executes(ctx -> setAllMastery(ctx, add, true, false))))
								.then(Commands.literal("stack")
										.then(Commands.argument("value", DoubleArgumentType.doubleArg())
												.executes(ctx -> setAllMastery(ctx, add, false, true))))
								.then(Commands.literal("all")
										.then(Commands.argument("value", DoubleArgumentType.doubleArg())
												.executes(ctx -> setAllMastery(ctx, add, true, true))))));
	}

	private static final SuggestionProvider<CommandSourceStack> SUGGEST_GROUPS = (context, builder) -> {
		try {
			ServerPlayer player = getTarget(context);
			String race = StatsProvider.get(StatsCapability.INSTANCE, player)
					.map(data -> data.getCharacter().getRaceName())
					.orElse("human");

			List<String> groups = new ArrayList<>(ConfigManager.getAllFormsForRace(race).keySet());
			groups.addAll(ConfigManager.getAllStackForms().keySet());
			return SharedSuggestionProvider.suggest(groups, builder);
		} catch (Exception ignored) {}
		return SharedSuggestionProvider.suggest(new ArrayList<>(), builder);
	};

	private static final SuggestionProvider<CommandSourceStack> SUGGEST_FORMS = (context, builder) -> {
		try {
			String groupName = StringArgumentType.getString(context, "group");
			if (groupName != null) {
				FormConfig groupConfig = ConfigManager.getStackFormGroup(groupName);
				if (groupConfig == null) {
					ServerPlayer player = getTarget(context);
					String race = StatsProvider.get(StatsCapability.INSTANCE, player)
							.map(data -> data.getCharacter().getRaceName()).orElse("human");
					groupConfig = ConfigManager.getFormGroup(race, groupName);
				}
				if (groupConfig != null) return SharedSuggestionProvider.suggest(groupConfig.getForms().keySet(), builder);
			}
		} catch (Exception ignored) {}
		return SharedSuggestionProvider.suggest(new ArrayList<>(), builder);
	};

	private static ServerPlayer getTarget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		EntitySelector selector = context.getArgument("target", EntitySelector.class);
		return selector.findSinglePlayer(context.getSource());
	}

	private static int setMastery(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
		String group = StringArgumentType.getString(ctx, "group");
		String form = StringArgumentType.getString(ctx, "form");
		double value = DoubleArgumentType.getDouble(ctx, "value");

		boolean stack = ConfigManager.getStackFormGroup(group) != null;
		return apply(ctx.getSource(), target, group, form, value, add, stack);
	}

	private static int setCurrentMastery(CommandContext<CommandSourceStack> ctx, boolean add, boolean stack) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
		double value = DoubleArgumentType.getDouble(ctx, "value");

		Character character = StatsProvider.get(StatsCapability.INSTANCE, target).map(data -> data.getCharacter()).orElse(null);
		if (character == null) return 0;

		boolean hasActive = stack ? character.hasActiveStackForm() : character.hasActiveForm();
		if (!hasActive) {
			ctx.getSource().sendFailure(Component.translatable(
					stack ? "command.dragonminez.mastery.no_active_stack" : "command.dragonminez.mastery.no_active_form",
					target.getName().getString()));
			return 0;
		}

		String group = stack ? character.getActiveStackFormGroup() : character.getActiveFormGroup();
		String form = stack ? character.getActiveStackForm() : character.getActiveForm();
		return apply(ctx.getSource(), target, group, form, value, add, stack);
	}

	private static int setAllMastery(CommandContext<CommandSourceStack> ctx, boolean add, boolean doForms, boolean doStacks) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
		double value = DoubleArgumentType.getDouble(ctx, "value");
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();

		StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(data -> {
			if (doForms) {
				var masteries = data.getCharacter().getFormMasteries();
				for (var groupEntry : ConfigManager.getAllFormsForRace(data.getCharacter().getRaceName()).entrySet()) {
					applyGroup(masteries, groupEntry.getKey(), groupEntry.getValue(), value, add);
				}
			}
			if (doStacks) {
				var masteries = data.getCharacter().getStackFormMasteries();
				for (var groupEntry : ConfigManager.getAllStackForms().entrySet()) {
					applyGroup(masteries, groupEntry.getKey(), groupEntry.getValue(), value, add);
				}
			}

			if (doForms) NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(target), target);
			if (doStacks) NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(target), target);
		});

		String scope = doForms && doStacks ? "all" : (doStacks ? "stack" : "form");
		String modeKey = add ? "add" : "set";
		ctx.getSource().sendSuccess(() -> Component.translatable(
				"command.dragonminez.mastery." + modeKey + ".all", value, scope, target.getName().getString()), log);

		return 1;
	}

	private static void applyGroup(FormMasteries masteries, String group, FormConfig groupConfig, double value, boolean add) {
		for (var formEntry : groupConfig.getForms().entrySet()) {
			double maxMastery = formEntry.getValue().getMaxMastery();
			if (add) masteries.addMastery(group, formEntry.getKey(), value, maxMastery);
			else masteries.setMastery(group, formEntry.getKey(), value, maxMastery);
		}
	}

	private static int apply(CommandSourceStack source, ServerPlayer target, String group, String form, double value, boolean add, boolean stack) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();

		StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(data -> {
			var masteries = stack ? data.getCharacter().getStackFormMasteries() : data.getCharacter().getFormMasteries();

			double maxMastery = 100.0;
			FormConfig.FormData formData = stack
					? ConfigManager.getStackForm(group, form)
					: ConfigManager.getForm(data.getCharacter().getRaceName(), group, form);
			if (formData != null) maxMastery = formData.getMaxMastery();

			if (add) masteries.addMastery(group, form, value, maxMastery);
			else masteries.setMastery(group, form, value, maxMastery);

			if (stack) NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(target), target);
			else NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(target), target);
		});

		String modeKey = add ? "add" : "set";
		source.sendSuccess(() -> Component.translatable("command.dragonminez.mastery." + modeKey + ".success", value, group, form, target.getName().getString()), log);

		return 1;
	}
}
