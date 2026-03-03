package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.Cooldowns;
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

public class RacialSkillCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzracial")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.RACIAL_RESET_SELF, DMZPermissions.RACIAL_RESET_OTHERS))
				.then(Commands.literal("reset")
						.requires(source -> DMZPermissions.check(source, DMZPermissions.RACIAL_RESET_SELF, DMZPermissions.RACIAL_RESET_OTHERS))
						.executes(context -> resetRacialSkills(context.getSource(), List.of(context.getSource().getPlayerOrException())))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.RACIAL_RESET_OTHERS))
								.executes(context -> resetRacialSkills(context.getSource(), EntityArgument.getPlayers(context, "targets")))
						)
				)
		);
	}

	private static int resetRacialSkills(CommandSourceStack source, Collection<ServerPlayer> targets) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		for (ServerPlayer player : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				String[] statBoosts = new String[0];
				statBoosts = switch (data.getCharacter().getRace()) {
					case "namekian" ->
							ConfigManager.getServerConfig().getRacialSkills().getNamekianAssimilationBoosts();
					case "majin" -> ConfigManager.getServerConfig().getRacialSkills().getMajinAbsorptionBoosts();
					case "saiyan" -> ConfigManager.getServerConfig().getRacialSkills().getSaiyanZenkaiBoosts();
					default -> statBoosts;
				};

				for (String stat : statBoosts) {
					for (int i = data.getResources().getRacialSkillCount(); i >= 0; i--) {
						data.getBonusStats().clearBonus(stat, "Absorption_");
						data.getBonusStats().clearBonus(stat, "Assimilation_");
						data.getBonusStats().clearBonus(stat, "Zenkai_");
					}
				}

				data.getCooldowns().removeCooldown(Cooldowns.ZENKAI);
				data.getResources().setRacialSkillCount(0);

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