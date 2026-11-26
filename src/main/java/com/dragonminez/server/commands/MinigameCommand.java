package com.dragonminez.server.commands;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.training.MinigameLogics;
import com.dragonminez.common.training.MinigameSessionManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
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

public class MinigameCommand {
	private static final String ALL = "all";

	private static final SuggestionProvider<CommandSourceStack> PLAY_SUGGESTIONS =
			(ctx, builder) -> SharedSuggestionProvider.suggest(MinigameLogics.IDS, builder);

	private static final SuggestionProvider<CommandSourceStack> LEARN_SUGGESTIONS = (ctx, builder) -> {
		List<String> options = new ArrayList<>(MinigameLogics.IDS);
		options.add(ALL);
		return SharedSuggestionProvider.suggest(options, builder);
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzminigame")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.MINIGAME_SELF, DMZPermissions.MINIGAME_OTHERS))

				.then(Commands.literal("play")
						.then(Commands.argument("minigame", StringArgumentType.word()).suggests(PLAY_SUGGESTIONS)
								.executes(ctx -> play(ctx.getSource(), StringArgumentType.getString(ctx, "minigame"),
										List.of(ctx.getSource().getPlayerOrException()), false))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.MINIGAME_OTHERS))
										.executes(ctx -> play(ctx.getSource(), StringArgumentType.getString(ctx, "minigame"),
												EntityArgument.getPlayers(ctx, "targets"), false))
										.then(Commands.argument("masterBonus", BoolArgumentType.bool())
												.executes(ctx -> play(ctx.getSource(), StringArgumentType.getString(ctx, "minigame"),
														EntityArgument.getPlayers(ctx, "targets"), BoolArgumentType.getBool(ctx, "masterBonus")))))))

				.then(Commands.literal("learn")
						.then(Commands.argument("minigame", StringArgumentType.word()).suggests(LEARN_SUGGESTIONS)
								.executes(ctx -> learn(ctx.getSource(), StringArgumentType.getString(ctx, "minigame"),
										List.of(ctx.getSource().getPlayerOrException())))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.MINIGAME_OTHERS))
										.executes(ctx -> learn(ctx.getSource(), StringArgumentType.getString(ctx, "minigame"),
												EntityArgument.getPlayers(ctx, "targets"))))))
		);
	}

	private static int play(CommandSourceStack source, String minigame, Collection<ServerPlayer> targets, boolean masterBonus) {
		String id = minigame.toLowerCase();
		if (!MinigameLogics.isValidId(id)) {
			source.sendFailure(Component.translatable("command.dragonminez.minigame.unknown", minigame));
			return 0;
		}
		int opened = 0;
		for (ServerPlayer player : targets) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (data == null || !data.getStatus().isHasCreatedCharacter()) continue;
			MinigameSessionManager.grantCommandSession(player, id, masterBonus);
			opened++;
		}
		if (opened == 0) {
			source.sendFailure(Component.translatable("command.dragonminez.minigame.no_targets"));
			return 0;
		}
		int count = opened;
		Component name = Component.translatable("gui.dragonminez.minigame." + id);
		source.sendSuccess(() -> Component.translatable(masterBonus ? "command.dragonminez.minigame.play.bonus" : "command.dragonminez.minigame.play",
				name, count), true);
		return opened;
	}

	private static int learn(CommandSourceStack source, String minigame, Collection<ServerPlayer> targets) {
		String id = minigame.toLowerCase();
		boolean all = ALL.equals(id);
		if (!all && !MinigameLogics.isValidId(id)) {
			source.sendFailure(Component.translatable("command.dragonminez.minigame.unknown", minigame));
			return 0;
		}
		List<String> ids = all ? MinigameLogics.IDS : List.of(id);
		int changed = 0;
		for (ServerPlayer player : targets) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (data == null) continue;
			for (String game : ids) {
				data.getCharacter().addKnownMinigame(game);
				data.getCharacter().clearMinigameLearnRuns(game);
			}
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			changed++;
		}
		if (changed == 0) {
			source.sendFailure(Component.translatable("command.dragonminez.minigame.no_targets"));
			return 0;
		}
		int count = changed;
		Component name = all ? Component.translatable("command.dragonminez.minigame.all") : Component.translatable("gui.dragonminez.minigame." + id);
		source.sendSuccess(() -> Component.translatable("command.dragonminez.minigame.learn", name, count), true);
		return changed;
	}
}
