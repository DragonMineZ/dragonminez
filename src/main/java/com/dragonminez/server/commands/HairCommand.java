package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AppearanceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public class HairCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzhair")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.HAIR_SELF, DMZPermissions.HAIR_OTHERS))
				.then(Commands.literal("resync")
						.executes(ctx -> resync(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.HAIR_OTHERS))
								.executes(ctx -> resync(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
				.then(Commands.literal("reset")
						.executes(ctx -> reset(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.HAIR_OTHERS))
								.executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
		);
	}

	private static int resync(CommandSourceStack source, Collection<ServerPlayer> targets) {
		int count = 0;
		for (ServerPlayer player : targets) {
			NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(player), player);
			count++;
		}
		int finalCount = count;
		source.sendSuccess(() -> Component.translatable("command.dragonminez.hair.resync", finalCount), true);
		return count;
	}

	private static int reset(CommandSourceStack source, Collection<ServerPlayer> targets) {
		int count = 0;
		for (ServerPlayer player : targets) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (data == null) continue;

			Character character = data.getCharacter();
			RaceCharacterConfig config = ConfigManager.getRaceCharacter(character.getRaceName());
			if (config != null) {
				character.setHairId(config.getDefaultHairType());
				if (config.getDefaultHairColor() != null) character.setHairColor(config.getDefaultHairColor());
			} else {
				character.setHairId(0);
			}
			character.setHairBase(new CustomHair());
			character.setHairSSJ(new CustomHair());
			character.setHairSSJ2(new CustomHair());
			character.setHairSSJ3(new CustomHair());

			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(player), player);
			count++;
		}
		int finalCount = count;
		source.sendSuccess(() -> Component.translatable("command.dragonminez.hair.reset", finalCount), true);
		return count;
	}
}
