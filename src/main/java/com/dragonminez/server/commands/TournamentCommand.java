package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.server.world.tournament.Tournament;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class TournamentCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmztournament")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("ring")
						.then(Commands.argument("tournament", StringArgumentType.word())
								.suggests((c, b) -> SharedSuggestionProvider.suggest(
										ConfigManager.getTournaments().keySet(), b))
								.executes(ctx -> setRing(ctx, StringArgumentType.getString(ctx, "tournament")))))
				.then(Commands.literal("cooldown")
						.then(Commands.literal("clear")
								.then(Commands.argument("players", EntityArgument.players())
										.executes(ctx -> clearCooldown(ctx, null,
												EntityArgument.getPlayers(ctx, "players")))
										.then(Commands.argument("tournament", StringArgumentType.word())
												.suggests((c, b) -> SharedSuggestionProvider.suggest(
														ConfigManager.getTournaments().keySet(), b))
												.executes(ctx -> clearCooldown(ctx,
														StringArgumentType.getString(ctx, "tournament"),
														EntityArgument.getPlayers(ctx, "players"))))))));
	}

	private static int clearCooldown(CommandContext<CommandSourceStack> ctx, @Nullable String tournamentId,
									 Collection<ServerPlayer> targets) {
		if (tournamentId != null && !ConfigManager.getTournaments().containsKey(tournamentId)) {
			ctx.getSource().sendFailure(
					Component.translatable("command.dragonminez.tournament.cooldown.unknown", tournamentId));
			return 0;
		}

		int cleared = 0;
		for (ServerPlayer target : targets) {
			Tournament.Progress data = Tournament.Progress.get(target.serverLevel());
			cleared += tournamentId == null
					? data.clearAllCooldowns(target.getUUID())
					: (data.clearCooldown(target.getUUID(), tournamentId) ? 1 : 0);
		}

		final int total = cleared;
		if (total == 0) {
			ctx.getSource().sendFailure(Component.translatable("command.dragonminez.tournament.cooldown.none"));
			return 0;
		}

		ctx.getSource().sendSuccess(() -> Component.translatable(
				"command.dragonminez.tournament.cooldown.cleared", total, targets.size()), true);
		return total;
	}

	private static int setRing(CommandContext<CommandSourceStack> ctx, String tournamentId)
			throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = player.serverLevel();
		BlockPos pos = player.blockPosition();

		StructureStart start = Tournament.RingAnchor.arenaAt(level, pos);
		if (start == null) {
			ctx.getSource().sendFailure(Component.translatable("command.dragonminez.tournament.ring.no_structure"));
			return 0;
		}

		Vec3i offset = Tournament.RingAnchor.toTemplate(start.getBoundingBox(), Tournament.RingAnchor.rotationOf(start), pos);
		int offsetX = offset.getX();
		int offsetY = offset.getY();
		int offsetZ = offset.getZ();

		if (!ConfigManager.saveTournamentRing(tournamentId, offsetX, offsetY, offsetZ)) {
			ctx.getSource().sendFailure(Component.translatable("command.dragonminez.tournament.ring.fail", tournamentId));
			return 0;
		}

		String rotationName = Tournament.RingAnchor.rotationOf(start).name();
		ctx.getSource().sendSuccess(() -> Component.translatable("command.dragonminez.tournament.ring.success",
				tournamentId, offsetX, offsetY, offsetZ, rotationName), true);
		return 1;
	}

}
