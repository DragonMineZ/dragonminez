package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.racial.RacialReset;
import com.dragonminez.common.racial.capture.CaptureRequest;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public class RacialSkillCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzracial")
				.then(Commands.literal("reset")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.RACIAL_RESET_SELF, DMZPermissions.RACIAL_RESET_OTHERS))
						.executes(context -> resetRacialSkills(context.getSource(), List.of(context.getSource().getPlayerOrException())))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.RACIAL_RESET_OTHERS))
								.executes(context -> resetRacialSkills(context.getSource(), EntityArgument.getPlayers(context, "targets")))
						)
				)
				.then(Commands.literal("reply")
						.then(Commands.argument("answer", StringArgumentType.word())
								.suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(List.of("accept", "reject"), builder))
								.executes(RacialSkillCommand::replyCapture)
						)
				)
		);
	}

	private static int replyCapture(CommandContext<CommandSourceStack> context) {
		try {
			ServerPlayer player = context.getSource().getPlayerOrException();
			boolean accepted = "accept".equalsIgnoreCase(StringArgumentType.getString(context, "answer"));
			CaptureRequest.reply(player, accepted);
			return 1;
		} catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
			return 0;
		}
	}

	private static int resetRacialSkills(CommandSourceStack source, Collection<ServerPlayer> targets) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				RacialReset.reset(player, data);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}

		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.racial.reset.success", targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.racial.reset.multiple", targets.size()), log);
		}

		return targets.size();
	}
}