package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.SkillsConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
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

public class SkillsCommand {

	private static final String ALL_SKILLS = "all";

	private static final SuggestionProvider<CommandSourceStack> SKILL_SUGGESTIONS = (ctx, builder) -> {
		List<String> suggestions = new ArrayList<>(getGenericSkills(ConfigManager.getSkillsConfig()));
		suggestions.add(0, ALL_SKILLS);
		return SharedSuggestionProvider.suggest(suggestions, builder);
	};

	private static List<String> getGenericSkills(SkillsConfig config) {
		return config.getSkills().keySet().stream()
				.filter(s -> !config.getKiSkills().contains(s) && !config.getStackSkills().contains(s) && !config.getFormSkills().contains(s) && !config.getStrikeSkills().contains(s) && !config.getEvasionSkills().contains(s))
				.sorted()
				.toList();
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzskill")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.SKILLS_LIST_SELF, DMZPermissions.SKILLS_LIST_OTHERS))

				// set <skill> <level> [targets]
				.then(Commands.literal("set")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.SKILLS_SET_SELF, DMZPermissions.SKILLS_SET_OTHERS))
						.then(Commands.argument("skill", StringArgumentType.string()).suggests(SKILL_SUGGESTIONS)
								.then(Commands.argument("level", IntegerArgumentType.integer(0))
										.executes(ctx -> setSkill(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "skill"), IntegerArgumentType.getInteger(ctx, "level")))
										.then(Commands.argument("targets", EntityArgument.players())
												.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.SKILLS_SET_OTHERS))
												.executes(ctx -> setSkill(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "skill"), IntegerArgumentType.getInteger(ctx, "level")))))))

				// add <skill> [targets]
				.then(Commands.literal("add")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.SKILLS_ADD_SELF, DMZPermissions.SKILLS_ADD_OTHERS))
						.then(Commands.argument("skill", StringArgumentType.string()).suggests(SKILL_SUGGESTIONS)
								.executes(ctx -> setSkill(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "skill"), 1))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.SKILLS_ADD_OTHERS))
										.executes(ctx -> setSkill(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "skill"), 1)))))

				// remove <skill> [targets]
				.then(Commands.literal("remove")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.SKILLS_REMOVE_SELF, DMZPermissions.SKILLS_REMOVE_OTHERS))
						.then(Commands.argument("skill", StringArgumentType.string()).suggests(SKILL_SUGGESTIONS)
								.executes(ctx -> removeSkill(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "skill")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.SKILLS_REMOVE_OTHERS))
										.executes(ctx -> removeSkill(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "skill"))))))
		);
	}

	private static int setSkill(CommandSourceStack source, Collection<ServerPlayer> targets, String skillName, int level) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		String lowerName = skillName.toLowerCase();
		var config = ConfigManager.getSkillsConfig();

		if (lowerName.equals(ALL_SKILLS)) return setAllSkills(source, targets, level, log);

		if (config.getKiSkills().contains(lowerName) || config.getStackSkills().contains(lowerName) || config.getFormSkills().contains(lowerName) || config.getStrikeSkills().contains(lowerName) || config.getEvasionSkills().contains(lowerName) || !config.getSkills().containsKey(lowerName)) {
			source.sendFailure(Component.translatable("command.dragonminez.skills.unknown_skill", skillName));
			return 0;
		}

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getSkills().setSkillLevel(lowerName, level);
				NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.skills.set_success", skillName, level, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.skills.set_multiple", skillName, level, targets.size()), log);
		}
		return targets.size();
	}

	private static int setAllSkills(CommandSourceStack source, Collection<ServerPlayer> targets, int level, boolean log) {
		List<String> skills = getGenericSkills(ConfigManager.getSkillsConfig());
		if (skills.isEmpty()) {
			source.sendFailure(Component.translatable("command.dragonminez.skills.unknown_skill", ALL_SKILLS));
			return 0;
		}

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				for (String skill : skills) data.getSkills().setSkillLevel(skill, level);
				NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.skills.set_all_success", skills.size(), level, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.skills.set_all_multiple", skills.size(), level, targets.size()), log);
		}
		return targets.size();
	}

	private static int removeSkill(CommandSourceStack source, Collection<ServerPlayer> targets, String skillName) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		String lowerName = skillName.toLowerCase();
		var config = ConfigManager.getSkillsConfig();

		if (lowerName.equals(ALL_SKILLS)) return removeAllSkills(source, targets, log);

		if (config.getKiSkills().contains(lowerName) || config.getStackSkills().contains(lowerName) || config.getFormSkills().contains(lowerName) || config.getStrikeSkills().contains(lowerName) || config.getEvasionSkills().contains(lowerName) || !config.getSkills().containsKey(lowerName)) {
			source.sendFailure(Component.translatable("command.dragonminez.skills.unknown_skill", skillName));
			return 0;
		}

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (data.getSkills().hasSkill(lowerName)) {
					data.getSkills().removeSkill(lowerName);
					NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
				}
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.skills.remove_success", skillName, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.skills.remove_multiple", skillName, targets.size()), log);
		}
		return targets.size();
	}

	private static int removeAllSkills(CommandSourceStack source, Collection<ServerPlayer> targets, boolean log) {
		List<String> skills = getGenericSkills(ConfigManager.getSkillsConfig());
		if (skills.isEmpty()) {
			source.sendFailure(Component.translatable("command.dragonminez.skills.unknown_skill", ALL_SKILLS));
			return 0;
		}

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				for (String skill : skills) if (data.getSkills().hasSkill(skill)) data.getSkills().removeSkill(skill);
				NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.skills.remove_all_success", skills.size(), targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.skills.remove_all_multiple", skills.size(), targets.size()), log);
		}
		return targets.size();
	}
}