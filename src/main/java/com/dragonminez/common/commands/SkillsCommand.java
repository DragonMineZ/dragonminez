package com.dragonminez.common.commands;

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
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("set")
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.argument("skill", StringArgumentType.string()).suggests(SKILL_SUGGESTIONS)
										.then(Commands.argument("level", IntegerArgumentType.integer(0))
												.executes(ctx -> setSkill(
														ctx.getSource(),
														EntityArgument.getPlayer(ctx, "player"),
														StringArgumentType.getString(ctx, "skill"),
														IntegerArgumentType.getInteger(ctx, "level")
												))))))
				.then(Commands.literal("remove")
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.argument("skill", StringArgumentType.string()).suggests(SKILL_SUGGESTIONS)
										.executes(ctx -> removeSkill(
												ctx.getSource(),
												EntityArgument.getPlayer(ctx, "player"),
												StringArgumentType.getString(ctx, "skill")
										)))))
				.then(Commands.literal("list")
						.then(Commands.argument("player", EntityArgument.player())
								.executes(ctx -> listSkills(
										ctx.getSource(),
										EntityArgument.getPlayer(ctx, "player")
								)))));
	}

	private static int setSkill(CommandSourceStack source, ServerPlayer target, String skillName, int level) {
		if (!ConfigManager.getSkillsConfig().getSkills().containsKey(skillName.toLowerCase())) {
			source.sendFailure(Component.literal("Unknown skill: " + skillName));
			return 0;
		}

		StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(data -> {
			data.getSkills().setSkillLevel(skillName, level);
			NetworkHandler.sendToPlayer(new StatsSyncS2C(target), target);
			source.sendSuccess(() -> Component.literal("Set skill '" + skillName +
					"' to level " + level + " for " + target.getName().getString()), true);
		});

		return 1;
	}

	private static int removeSkill(CommandSourceStack source, ServerPlayer target, String skillName) {
		String lowerName = skillName.toLowerCase();

		if (!ConfigManager.getSkillsConfig().getSkills().containsKey(lowerName)) {
			source.sendFailure(Component.literal("§c[DMZ] Unknown skill: " + skillName));
			return 0;
		}

		StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(data -> {
			boolean hadSkill = data.getSkills().hasSkill(lowerName);
			int currentLevel = data.getSkills().getSkillLevel(lowerName);

			if (!hadSkill || currentLevel == 0) {
				source.sendFailure(Component.literal("§c[DMZ] " + target.getName().getString() +
						" doesn't have skill '" + skillName + "' or it's already at level 0"));
				return;
			}

			data.getSkills().removeSkill(skillName);
			NetworkHandler.sendToPlayer(new StatsSyncS2C(target), target);


			boolean isTransformationSkill = lowerName.equals("superform") ||
											lowerName.equals("godform") ||
											lowerName.equals("legendaryforms");

			if (isTransformationSkill) {
				source.sendSuccess(() -> Component.literal("§a[DMZ] Reset transformation skill '" + skillName +
						"' to level 0 for " + target.getName().getString()), true);
			} else {
				source.sendSuccess(() -> Component.literal("§a[DMZ] Removed skill '" + skillName +
						"' from " + target.getName().getString()), true);
			}
		});

		return 1;
	}

	private static int listSkills(CommandSourceStack source, ServerPlayer target) {
		StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(data -> {
			var skills = data.getSkills().getAllSkills();

			if (skills.isEmpty()) {
				source.sendSuccess(() -> Component.literal(target.getName().getString() +
						" has no skills"), false);
				return;
			}

			source.sendSuccess(() -> Component.literal("Skills for " +
					target.getName().getString() + ":"), false);

			boolean hasSkills = false;
			for (var entry : skills.entrySet()) {
				if (entry.getValue().getLevel() > 0) {
					hasSkills = true;
					source.sendSuccess(() -> Component.literal("  - " + entry.getKey() +
							": " + entry.getValue().getLevel()), false);
				}
			}

			if (!hasSkills) {
				source.sendSuccess(() -> Component.literal(target.getName().getString() +
						" has no learned skills"), false);
			}
		});

		return 1;
	}
}
