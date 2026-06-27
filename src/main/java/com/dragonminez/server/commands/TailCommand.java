package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AppearanceSyncS2C;
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
import net.minecraft.sounds.SoundSource;

import java.util.Collection;
import java.util.List;

public class TailCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmztail")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.TAIL_SELF, DMZPermissions.TAIL_OTHERS))

				.then(Commands.literal("cut")
						.executes(ctx -> setTail(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), false))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.TAIL_OTHERS))
								.executes(ctx -> setTail(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), false))))

				.then(Commands.literal("grow")
						.executes(ctx -> setTail(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), true))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.TAIL_OTHERS))
								.executes(ctx -> setTail(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), true))))
		);
	}

	private static boolean canHaveTail(StatsData data) {
		String race = data.getCharacter().getRaceName();
		if ("saiyan".equalsIgnoreCase(race)) return true;
		RaceCharacterConfig config = ConfigManager.getRaceCharacter(race);
		return config != null && Boolean.TRUE.equals(config.getHasSaiyanTail());
	}

	private static int setTail(CommandSourceStack source, Collection<ServerPlayer> targets, boolean grow) {
		String action = grow ? "grow" : "cut";

		if (targets.size() == 1) {
			ServerPlayer player = targets.iterator().next();
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (data == null) return 0;
			if (!canHaveTail(data)) {
				source.sendFailure(Component.translatable("command.dragonminez.tail.no_race", player.getName().getString()));
				return 0;
			}
			Character character = data.getCharacter();
			if (character.isHasSaiyanTail() == grow) {
				source.sendFailure(Component.translatable("command.dragonminez.tail.already_" + action, player.getName().getString()));
				return 0;
			}
			applyTail(player, character, grow);
			if (player == source.getEntity()) {
				source.sendSuccess(() -> Component.translatable("command.dragonminez.tail." + action + ".self"), false);
			} else {
				source.sendSuccess(() -> Component.translatable("command.dragonminez.tail." + action + ".other", player.getName().getString()), true);
			}
			return 1;
		}

		int success = 0;
		for (ServerPlayer player : targets) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (data == null || !canHaveTail(data)) continue;
			Character character = data.getCharacter();
			if (character.isHasSaiyanTail() == grow) continue;
			applyTail(player, character, grow);
			success++;
		}
		int finalSuccess = success;
		source.sendSuccess(() -> Component.translatable("command.dragonminez.tail." + action + ".multiple", finalSuccess), true);
		return success;
	}

	private static void applyTail(ServerPlayer player, Character character, boolean grow) {
		character.setHasSaiyanTail(grow);
		NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(player), player);
		if (!grow) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					MainSounds.KATANA_SLASH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
		}
	}
}
