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

public class SkillsCommand {

	private static final SuggestionProvider<CommandSourceStack> SKILL_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(ConfigManager.getSkillsConfig().getSkills().keySet(), builder);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzskill")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.SKILLS_LIST_SELF, DMZPermissions.SKILLS_LIST_OTHERS))

				// set <skill> <level> [player]
				.then(Commands.literal("set")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.SKILLS_SET_SELF, DMZPermissions.SKILLS_SET_OTHERS))
						.then(Commands.argument("skill", StringArgumentType.string()).suggests(SKILL_SUGGESTIONS)
								.then(Commands.argument("level", IntegerArgumentType.integer(0))
										.executes(ctx -> setSkill(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "skill"), IntegerArgumentType.getInteger(ctx, "level")))
										.then(Commands.argument("player", EntityArgument.player())
												.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.SKILLS_SET_OTHERS))
												.executes(ctx -> setSkill(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "skill"), IntegerArgumentType.getInteger(ctx, "level")))))))

				// add <skill> [player]
				.then(Commands.literal("add")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.SKILLS_ADD_SELF, DMZPermissions.SKILLS_ADD_OTHERS))
						.then(Commands.argument("skill", StringArgumentType.string()).suggests(SKILL_SUGGESTIONS)
								.executes(ctx -> setSkill(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "skill"), 1))
								.then(Commands.argument("player", EntityArgument.player())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.SKILLS_ADD_OTHERS))
										.executes(ctx -> setSkill(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "skill"), 1)))))

				// remove <skill> [player]
				.then(Commands.literal("remove")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.SKILLS_REMOVE_SELF, DMZPermissions.SKILLS_REMOVE_OTHERS))
						.then(Commands.argument("skill", StringArgumentType.string()).suggests(SKILL_SUGGESTIONS)
								.executes(ctx -> removeSkill(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "skill")))
								.then(Commands.argument("player", EntityArgument.player())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.SKILLS_REMOVE_OTHERS))
										.executes(ctx -> removeSkill(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "skill"))))))
		);
	}

	private static int setSkill(CommandSourceStack source, ServerPlayer target, String skillName, int level) {
		if (!ConfigManager.getSkillsConfig().getSkills().containsKey(skillName.toLowerCase())) {
			source.sendFailure(Component.translatable("command.dragonminez.skills.unknown_skill", skillName));
			return 0;
		}

		StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(data -> {
			data.getSkills().setSkillLevel(skillName, level);
			NetworkHandler.sendToPlayer(new StatsSyncS2C(target), target);
			source.sendSuccess(() -> Component.translatable("command.dragonminez.skills.set_success", skillName, level, target.getName().getString()), true);
		});

		return 1;
	}

	private static int removeSkill(CommandSourceStack source, ServerPlayer target, String skillName) {
		String lowerName = skillName.toLowerCase();

		if (!ConfigManager.getSkillsConfig().getSkills().containsKey(lowerName)) {
			source.sendFailure(Component.translatable("command.dragonminez.skills.unknown_skill", skillName));
			return 0;
		}

		StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(data -> {
			boolean hadSkill = data.getSkills().hasSkill(lowerName);
			int currentLevel = data.getSkills().getSkillLevel(lowerName);

			if (!hadSkill || currentLevel == 0) {
				source.sendFailure(Component.translatable("command.dragonminez.skills.no_skill", target.getName().getString(), skillName));
				return;
			}

			data.getSkills().removeSkill(skillName);
			NetworkHandler.sendToPlayer(new StatsSyncS2C(target), target);


			boolean isTransformationSkill = lowerName.equals("superform") ||
											lowerName.equals("godform") ||
											lowerName.equals("legendaryforms");

			if (isTransformationSkill) {
				source.sendSuccess(() -> Component.translatable("command.dragonminez.skills.reset_success", skillName, target.getName().getString()), true);
			} else {
				source.sendSuccess(() -> Component.translatable("command.dragonminez.skills.remove_success", skillName, target.getName().getString()), true);
			}
		});

		return 1;
	}
}
