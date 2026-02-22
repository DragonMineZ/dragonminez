package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FormsCommand {

	private static final SuggestionProvider<CommandSourceStack> FORM_SUGGESTIONS = (ctx, builder) -> {
		var config = ConfigManager.getSkillsConfig();
		List<String> validForms = new ArrayList<>(config.getFormSkills());
		validForms.addAll(config.getStackSkills());
		validForms.addAll(config.getFormSkills());
		return SharedSuggestionProvider.suggest(validForms, builder);
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzform")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.FORMS_LIST_SELF, DMZPermissions.FORMS_LIST_OTHERS))

				.then(Commands.literal("set")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.FORMS_SET_SELF, DMZPermissions.FORMS_SET_OTHERS))
						.then(Commands.argument("form", StringArgumentType.string()).suggests(FORM_SUGGESTIONS)
								.then(Commands.argument("level", IntegerArgumentType.integer(0))
										.executes(ctx -> setForm(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "form"), IntegerArgumentType.getInteger(ctx, "level")))
										.then(Commands.argument("targets", EntityArgument.players())
												.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.FORMS_SET_OTHERS))
												.executes(ctx -> setForm(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "form"), IntegerArgumentType.getInteger(ctx, "level")))))))

				.then(Commands.literal("add")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.FORMS_ADD_SELF, DMZPermissions.FORMS_ADD_OTHERS))
						.then(Commands.argument("form", StringArgumentType.string()).suggests(FORM_SUGGESTIONS)
								.executes(ctx -> setForm(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "form"), 1))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.FORMS_ADD_OTHERS))
										.executes(ctx -> setForm(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "form"), 1)))))

				.then(Commands.literal("remove")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.FORMS_REMOVE_SELF, DMZPermissions.FORMS_REMOVE_OTHERS))
						.then(Commands.argument("form", StringArgumentType.string()).suggests(FORM_SUGGESTIONS)
								.executes(ctx -> removeForm(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "form")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.FORMS_REMOVE_OTHERS))
										.executes(ctx -> removeForm(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "form"))))))
		);
	}

	private static int setForm(CommandSourceStack source, Collection<ServerPlayer> targets, String formName, int level) {
		boolean log = ConfigManager.getServerConfig().getGameplay().isCommandOutputOnConsole();
		String lowerName = formName.toLowerCase();
		var config = ConfigManager.getSkillsConfig();

		if (!config.getFormSkills().contains(lowerName) && !config.getStackSkills().contains(lowerName)) {
			source.sendFailure(Component.translatable("command.dragonminez.forms.unknown_form", formName));
			return 0;
		}

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getSkills().setSkillLevel(lowerName, level);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.forms.set_success", formName, level, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.forms.set_multiple", formName, level, targets.size()), log);
		}
		return targets.size();
	}

	private static int removeForm(CommandSourceStack source, Collection<ServerPlayer> targets, String formName) {
		boolean log = ConfigManager.getServerConfig().getGameplay().isCommandOutputOnConsole();
		String lowerName = formName.toLowerCase();
		var config = ConfigManager.getSkillsConfig();

		if (!config.getFormSkills().contains(lowerName) && !config.getStackSkills().contains(lowerName)) {
			source.sendFailure(Component.translatable("command.dragonminez.forms.unknown_form", formName));
			return 0;
		}

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (data.getSkills().hasSkill(lowerName)) {
					data.getSkills().removeSkill(lowerName);
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
				}
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.forms.remove_success", formName, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.forms.remove_multiple", formName, targets.size()), log);
		}
		return targets.size();
	}
}