package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.SkillsConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.EvasionAttackData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
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
import java.util.concurrent.atomic.AtomicInteger;

public class TechCommand {

	private static final String ALL_TECHNIQUES = "all";

	private static final SuggestionProvider<CommandSourceStack> TECH_SUGGESTIONS = (ctx, builder) -> {
		List<String> suggestions = new ArrayList<>(getAllTechniqueIds(ConfigManager.getSkillsConfig()));
		suggestions.add(0, ALL_TECHNIQUES);
		return SharedSuggestionProvider.suggest(suggestions, builder);
	};

	private static List<String> getAllTechniqueIds(SkillsConfig config) {
		List<String> ids = new ArrayList<>();
		config.getKiSkills().stream().filter(PredefinedTechniques.REGISTRY::containsKey).forEach(ids::add);
		config.getStrikeSkills().stream().filter(PredefinedTechniques.STRIKE_REGISTRY::containsKey).forEach(ids::add);
		config.getEvasionSkills().stream().filter(PredefinedTechniques.EVASION_REGISTRY::containsKey).forEach(ids::add);
		return ids;
	}

	private static TechniqueData createTechnique(String id) {
		if (PredefinedTechniques.REGISTRY.containsKey(id)) {
			KiAttackData clone = new KiAttackData();
			clone.load(PredefinedTechniques.REGISTRY.get(id).save());
			return clone;
		}
		if (PredefinedTechniques.STRIKE_REGISTRY.containsKey(id)) {
			StrikeAttackData clone = new StrikeAttackData();
			clone.load(PredefinedTechniques.STRIKE_REGISTRY.get(id).save());
			return clone;
		}
		if (PredefinedTechniques.EVASION_REGISTRY.containsKey(id)) {
			EvasionAttackData clone = new EvasionAttackData();
			clone.load(PredefinedTechniques.EVASION_REGISTRY.get(id).save());
			return clone;
		}
		return null;
	}

	private static final SuggestionProvider<CommandSourceStack> UNLOCKED_TECH_SUGGESTIONS = (ctx, builder) -> {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) return builder.buildFuture();
		java.util.List<String> ids = new java.util.ArrayList<>();
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data ->
				ids.addAll(data.getTechniques().getUnlockedTechniques().keySet()));
		return SharedSuggestionProvider.suggest(ids, builder);
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmztech")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.TECH_LIST_SELF, DMZPermissions.TECH_LIST_OTHERS))

				.then(Commands.literal("add")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.TECH_ADD_SELF, DMZPermissions.TECH_ADD_OTHERS))
						.then(Commands.argument("technique", StringArgumentType.string()).suggests(TECH_SUGGESTIONS)
								.executes(ctx -> addTechnique(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "technique")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.TECH_ADD_OTHERS))
										.executes(ctx -> addTechnique(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "technique"))))))

				.then(Commands.literal("remove")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.TECH_REMOVE_SELF, DMZPermissions.TECH_REMOVE_OTHERS))
						.then(Commands.argument("technique", StringArgumentType.string()).suggests(TECH_SUGGESTIONS)
								.executes(ctx -> removeTechnique(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "technique")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.TECH_REMOVE_OTHERS))
										.executes(ctx -> removeTechnique(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "technique"))))))

				.then(Commands.literal("experience")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.TECH_EXP_SELF, DMZPermissions.TECH_EXP_OTHERS))
						.then(experienceMode("add", ExperienceMode.ADD))
						.then(experienceMode("set", ExperienceMode.SET))
						.then(experienceMode("remove", ExperienceMode.REMOVE)))
		);
	}

	private enum ExperienceMode { ADD, SET, REMOVE }

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> experienceMode(String literal, ExperienceMode mode) {
		return Commands.literal(literal)
				.then(Commands.argument("technique", StringArgumentType.string()).suggests(UNLOCKED_TECH_SUGGESTIONS)
						.then(Commands.argument("amount", IntegerArgumentType.integer(0))
								.executes(ctx -> experienceTechnique(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()),
										StringArgumentType.getString(ctx, "technique"), IntegerArgumentType.getInteger(ctx, "amount"), mode))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.TECH_EXP_OTHERS))
										.executes(ctx -> experienceTechnique(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"),
												StringArgumentType.getString(ctx, "technique"), IntegerArgumentType.getInteger(ctx, "amount"), mode)))));
	}

	private static int addTechnique(CommandSourceStack source, Collection<ServerPlayer> targets, String techniqueId) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		String id = techniqueId.toLowerCase();

		if (id.equals(ALL_TECHNIQUES)) return addAllTechniques(source, targets, log);

		if (isUnknownTechnique(id)) {
			source.sendFailure(Component.translatable("command.dragonminez.tech.unknown_technique", techniqueId));
			return 0;
		}

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				TechniqueData technique = createTechnique(id);
				if (technique != null) data.getTechniques().unlockTechnique(technique);
				NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.tech.add_success", techniqueId, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.tech.add_multiple", techniqueId, targets.size()), log);
		}
		return targets.size();
	}

	private static int addAllTechniques(CommandSourceStack source, Collection<ServerPlayer> targets, boolean log) {
		List<String> ids = getAllTechniqueIds(ConfigManager.getSkillsConfig());
		if (ids.isEmpty()) {
			source.sendFailure(Component.translatable("command.dragonminez.tech.unknown_technique", ALL_TECHNIQUES));
			return 0;
		}

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				var techniques = data.getTechniques();
				for (String id : ids) {
					if (techniques.getUnlockedTechniques().containsKey(id)) continue;
					TechniqueData technique = createTechnique(id);
					if (technique != null) techniques.unlockTechnique(technique);
				}
				NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.tech.add_all_success", ids.size(), targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.tech.add_all_multiple", ids.size(), targets.size()), log);
		}
		return targets.size();
	}

	private static int removeAllTechniques(CommandSourceStack source, Collection<ServerPlayer> targets, boolean log) {
		List<String> ids = getAllTechniqueIds(ConfigManager.getSkillsConfig());
		if (ids.isEmpty()) {
			source.sendFailure(Component.translatable("command.dragonminez.tech.unknown_technique", ALL_TECHNIQUES));
			return 0;
		}

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				var techniques = data.getTechniques();
				for (String id : ids) techniques.removeTechnique(id);
				NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.tech.remove_all_success", ids.size(), targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.tech.remove_all_multiple", ids.size(), targets.size()), log);
		}
		return targets.size();
	}

	private static int removeTechnique(CommandSourceStack source, Collection<ServerPlayer> targets, String techniqueId) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		String id = techniqueId.toLowerCase();

		if (id.equals(ALL_TECHNIQUES)) return removeAllTechniques(source, targets, log);

		if (isUnknownTechnique(id)) {
			source.sendFailure(Component.translatable("command.dragonminez.tech.unknown_technique", techniqueId));
			return 0;
		}

		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				var techniques = data.getTechniques();
				techniques.getUnlockedTechniques().remove(id);
				String[] slots = techniques.getEquippedSlots();
				for (int i = 0; i < slots.length; i++) {
					if (id.equals(slots[i])) slots[i] = "";
				}
				NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.tech.remove_success", techniqueId, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.tech.remove_multiple", techniqueId, targets.size()), log);
		}
		return targets.size();
	}

	private static int experienceTechnique(CommandSourceStack source, Collection<ServerPlayer> targets, String techniqueId, int amount, ExperienceMode mode) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();

		AtomicInteger applied = new AtomicInteger();
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				var tech = data.getTechniques().getUnlockedTechniques().get(techniqueId);
				if (tech == null) return;
				switch (mode) {
					case ADD -> tech.addExperience(tech.getExperience() + amount);
					case SET -> tech.setExperience(amount);
					case REMOVE -> tech.setExperience(Math.max(0, tech.getExperience() - amount));
				}
				applied.incrementAndGet();
				NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
			});
		}

		if (applied.get() == 0) {
			source.sendFailure(Component.translatable("command.dragonminez.tech.unknown_technique", techniqueId));
			return 0;
		}

		if (applied.get() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.tech.experience_success", techniqueId, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.tech.experience_multiple", techniqueId, applied.get()), log);
		}
		return applied.get();
	}

	private static boolean isUnknownTechnique(String id) {
		var config = ConfigManager.getSkillsConfig();
		boolean isKi = config.getKiSkills().contains(id) && PredefinedTechniques.REGISTRY.containsKey(id);
		boolean isStrike = config.getStrikeSkills().contains(id) && PredefinedTechniques.STRIKE_REGISTRY.containsKey(id);
		boolean isEvasion = config.getEvasionSkills().contains(id) && PredefinedTechniques.EVASION_REGISTRY.containsKey(id);
		return !isKi && !isStrike && !isEvasion;
	}
}

