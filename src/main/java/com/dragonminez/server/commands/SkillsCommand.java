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

import java.util.Collection;
import java.util.List;

public class SkillsCommand {

	private static final SuggestionProvider<CommandSourceStack> SKILL_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(ConfigManager.getSkillsConfig().getSkills().keySet(), builder);

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
		boolean log = ConfigManager.getServerConfig().getGameplay().isCommandOutputOnConsole();
		if (!ConfigManager.getSkillsConfig().getSkills().containsKey(skillName.toLowerCase())) {
			source.sendFailure(Component.translatable("command.dragonminez.skills.unknown_skill", skillName));
			return 0;
		}

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getSkills().setSkillLevel(skillName, level);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.skills.set_success", skillName, level, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.skills.set_multiple", skillName, level, targets.size()), log);
		}
		return targets.size();
	}

	private static int removeSkill(CommandSourceStack source, Collection<ServerPlayer> targets, String skillName) {
		boolean log = ConfigManager.getServerConfig().getGameplay().isCommandOutputOnConsole();
		String lowerName = skillName.toLowerCase();
		if (!ConfigManager.getSkillsConfig().getSkills().containsKey(lowerName)) {
			source.sendFailure(Component.translatable("command.dragonminez.skills.unknown_skill", skillName));
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
			source.sendSuccess(() -> Component.translatable("command.dragonminez.skills.remove_success", skillName, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.skills.remove_multiple", skillName, targets.size()), log);
		}
		return targets.size();
	}
}