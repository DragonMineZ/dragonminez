package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.mojang.brigadier.CommandDispatcher;
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

public class TechCommand {

	private static final SuggestionProvider<CommandSourceStack> TECH_SUGGESTIONS = (ctx, builder) -> {
		var config = ConfigManager.getSkillsConfig();
		var validTechs = config.getKiSkills().stream()
				.filter(PredefinedTechniques.REGISTRY::containsKey)
				.toList();
		return SharedSuggestionProvider.suggest(validTechs, builder);
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
		);
	}

	private static int addTechnique(CommandSourceStack source, Collection<ServerPlayer> targets, String techniqueId) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		String id = techniqueId.toLowerCase();

		if (isUnknownKiTechnique(id)) {
			source.sendFailure(Component.translatable("command.dragonminez.tech.unknown_technique", techniqueId));
			return 0;
		}

		KiAttackData template = PredefinedTechniques.REGISTRY.get(id);
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				KiAttackData clone = new KiAttackData();
				clone.load(template.save());
				data.getTechniques().unlockTechnique(clone);
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

	private static int removeTechnique(CommandSourceStack source, Collection<ServerPlayer> targets, String techniqueId) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		String id = techniqueId.toLowerCase();

		if (isUnknownKiTechnique(id)) {
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

	private static boolean isUnknownKiTechnique(String id) {
		var config = ConfigManager.getSkillsConfig();
		return !config.getKiSkills().contains(id) || !PredefinedTechniques.REGISTRY.containsKey(id);
	}
}

