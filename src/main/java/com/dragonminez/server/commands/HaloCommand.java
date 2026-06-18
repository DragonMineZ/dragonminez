package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public class HaloCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzhalo")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.HALO_SELF, DMZPermissions.HALO_OTHERS))

				.then(Commands.literal("on")
						.executes(ctx -> setHalo(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), true))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.HALO_OTHERS))
								.executes(ctx -> setHalo(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), true))))

				.then(Commands.literal("off")
						.executes(ctx -> setHalo(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), false))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.HALO_OTHERS))
								.executes(ctx -> setHalo(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), false))))
		);
	}

	private static int setHalo(CommandSourceStack source, Collection<ServerPlayer> targets, boolean enabled) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		String state = enabled ? "on" : "off";
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getStatus().setForceHalo(enabled);
				NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
			});
		}
		if (targets.size() == 1) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.halo." + state + ".success", targets.iterator().next().getName().getString()), log);
		} else {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.halo." + state + ".multiple", targets.size()), log);
		}
		return targets.size();
	}
}
