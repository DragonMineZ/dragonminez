package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public class SaiyanTailCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzsaiyantail")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.SAIYAN_TAIL_SELF, DMZPermissions.SAIYAN_TAIL_OTHERS))

				// set <amount> [targets]
				.then(Commands.literal("set")
						.then(Commands.argument("exists", BoolArgumentType.bool())
								.executes(ctx -> setSaiyanTail(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), BoolArgumentType.getBool(ctx, "exists")))
								.then(Commands.argument("targets", EntityArgument.players())
										.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.SAIYAN_TAIL_OTHERS))
										.executes(ctx -> setSaiyanTail(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), BoolArgumentType.getBool(ctx, "exists"))))))
		);
	}

	private static int setSaiyanTail(CommandSourceStack source, Collection<ServerPlayer> targets, boolean exists) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getCharacter().setHasSaiyanTail(exists);
				data.getCharacter().clearSelectedForm();
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}
		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.saiyantail.set.success", exists, targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.saiyantail.set.multiple", exists, targets.size()), log);
		}
		return targets.size();
	}
}