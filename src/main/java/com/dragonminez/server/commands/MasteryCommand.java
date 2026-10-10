package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AppearanceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.FormMasteries;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MasteryCommand {

	private interface TargetAction {
		int run(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException;
	}

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
				.then(Commands.literal("current")
						.then(Commands.literal("form")
								.then(withTargets(Commands.argument("value", DoubleArgumentType.doubleArg()),
										(ctx, targets) -> setCurrentMastery(ctx, targets, add, false))))
						.then(Commands.literal("stack")
								.then(withTargets(Commands.argument("value", DoubleArgumentType.doubleArg()),
										(ctx, targets) -> setCurrentMastery(ctx, targets, add, true)))))
				.then(Commands.literal("ALL")
						.then(Commands.literal("form")
								.then(withTargets(Commands.argument("value", DoubleArgumentType.doubleArg()),
										(ctx, targets) -> setAllMastery(ctx, targets, add, true, false))))
						.then(Commands.literal("stack")
								.then(withTargets(Commands.argument("value", DoubleArgumentType.doubleArg()),
										(ctx, targets) -> setAllMastery(ctx, targets, add, false, true))))
						.then(Commands.literal("all")
								.then(withTargets(Commands.argument("value", DoubleArgumentType.doubleArg()),
										(ctx, targets) -> setAllMastery(ctx, targets, add, true, true)))))
				.then(Commands.argument("group", StringArgumentType.word())
						.suggests(SUGGEST_GROUPS)
						.then(Commands.argument("form", StringArgumentType.word())
								.suggests(SUGGEST_FORMS)
								.then(withTargets(Commands.argument("value", DoubleArgumentType.doubleArg()),
										(ctx, targets) -> setMastery(ctx, targets, add)))));
	}

	private static <T extends ArgumentBuilder<CommandSourceStack, T>> T withTargets(T node, TargetAction action) {
		return node
				.executes(ctx -> action.run(ctx, List.of(ctx.getSource().getPlayerOrException())))
				.then(Commands.argument("targets", EntityArgument.players())
						.executes(ctx -> action.run(ctx, EntityArgument.getPlayers(ctx, "targets"))));
	}

	private static final SuggestionProvider<CommandSourceStack> SUGGEST_GROUPS = (context, builder) -> {
		Set<String> groups = new LinkedHashSet<>();
		for (String race : suggestionRaces(context.getSource())) {
			groups.addAll(ConfigManager.getAllFormsForRace(race).keySet());
		}
		groups.addAll(ConfigManager.getAllStackForms().keySet());
		return SharedSuggestionProvider.suggest(groups, builder);
	};

	private static final SuggestionProvider<CommandSourceStack> SUGGEST_FORMS = (context, builder) -> {
		Set<String> forms = new LinkedHashSet<>();
		try {
			String groupName = StringArgumentType.getString(context, "group");
			FormConfig stackGroup = ConfigManager.getStackFormGroup(groupName);
			if (stackGroup != null) {
				forms.addAll(stackGroup.getForms().keySet());
			} else {
				for (String race : suggestionRaces(context.getSource())) {
					FormConfig groupConfig = ConfigManager.getFormGroup(race, groupName);
					if (groupConfig != null) forms.addAll(groupConfig.getForms().keySet());
				}
			}
		} catch (Exception ignored) {}
		return SharedSuggestionProvider.suggest(forms, builder);
	};

	private static List<String> suggestionRaces(CommandSourceStack source) {
		if (source.getEntity() instanceof ServerPlayer player) {
			String race = StatsProvider.get(StatsCapability.INSTANCE, player)
					.map(data -> data.getCharacter().getRaceName())
					.orElse(null);
			if (race != null && !race.isEmpty()) return List.of(race);
		}
		return ConfigManager.getLoadedRaces();
	}

	private static int setMastery(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, boolean add) {
		String group = StringArgumentType.getString(ctx, "group");
		String form = StringArgumentType.getString(ctx, "form");
		double value = DoubleArgumentType.getDouble(ctx, "value");
		boolean stack = ConfigManager.getStackFormGroup(group) != null;
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();

		for (ServerPlayer target : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(data -> apply(target, data, group, form, value, add, stack));
		}

		String modeKey = add ? "add" : "set";
		if (targets.size() == 1) {
			ServerPlayer target = targets.iterator().next();
			ctx.getSource().sendSuccess(() -> Component.translatable("command.dragonminez.mastery." + modeKey + ".success",
					value, group, form, target.getName().getString()), log);
		} else {
			ctx.getSource().sendSuccess(() -> Component.translatable("command.dragonminez.mastery." + modeKey + ".multiple",
					value, group, form, targets.size()), log);
		}
		return targets.size();
	}

	private static int setCurrentMastery(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, boolean add, boolean stack) {
		double value = DoubleArgumentType.getDouble(ctx, "value");
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		String modeKey = add ? "add" : "set";
		int applied = 0;

		for (ServerPlayer target : targets) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, target).orElse(null);
			if (data == null) continue;
			Character character = data.getCharacter();
			boolean hasActive = stack ? character.hasActiveStackForm() : character.hasActiveForm();
			if (!hasActive) {
				ctx.getSource().sendFailure(Component.translatable(
						stack ? "command.dragonminez.mastery.no_active_stack" : "command.dragonminez.mastery.no_active_form",
						target.getName().getString()));
				continue;
			}

			String group = stack ? character.getActiveStackFormGroup() : character.getActiveFormGroup();
			String form = stack ? character.getActiveStackForm() : character.getActiveForm();
			apply(target, data, group, form, value, add, stack);
			applied++;
			ctx.getSource().sendSuccess(() -> Component.translatable("command.dragonminez.mastery." + modeKey + ".success",
					value, group, form, target.getName().getString()), log);
		}
		return applied;
	}

	private static int setAllMastery(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, boolean add, boolean doForms, boolean doStacks) {
		double value = DoubleArgumentType.getDouble(ctx, "value");
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();

		for (ServerPlayer target : targets) {
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
		}

		String scope = doForms && doStacks ? "all" : (doStacks ? "stack" : "form");
		String modeKey = add ? "add" : "set";
		String who = targets.size() == 1 ? targets.iterator().next().getName().getString() : targets.size() + " players";
		ctx.getSource().sendSuccess(() -> Component.translatable(
				"command.dragonminez.mastery." + modeKey + ".all", value, scope, who), log);
		return targets.size();
	}

	private static void applyGroup(FormMasteries masteries, String group, FormConfig groupConfig, double value, boolean add) {
		for (var formEntry : groupConfig.getForms().entrySet()) {
			double maxMastery = formEntry.getValue().getMaxMastery();
			if (add) masteries.addMastery(group, formEntry.getKey(), value, maxMastery);
			else masteries.setMastery(group, formEntry.getKey(), value, maxMastery);
		}
	}

	private static void apply(ServerPlayer target, StatsData data, String group, String form, double value, boolean add, boolean stack) {
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
	}
}
