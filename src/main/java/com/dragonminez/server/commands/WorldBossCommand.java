package com.dragonminez.server.commands;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.WorldBossResultsS2C;
import com.dragonminez.common.worldboss.WorldBossResults;
import com.dragonminez.server.world.worldboss.WorldBossManager;
import com.dragonminez.server.world.worldboss.WorldBossResultsCache;
import com.dragonminez.server.world.worldboss.WorldBossSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class WorldBossCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzworldboss")
				.then(Commands.literal("results")
						.executes(WorldBossCommand::results))
				.then(Commands.literal("locate")
						.requires(source -> source.hasPermission(2))
						.executes(WorldBossCommand::locate))
				.then(Commands.literal("tp")
						.requires(source -> source.hasPermission(2))
						.executes(WorldBossCommand::teleport))
				.then(Commands.literal("reset")
						.requires(source -> source.hasPermission(2))
						.executes(WorldBossCommand::reset))
				.then(Commands.literal("ready")
						.requires(source -> source.hasPermission(2))
						.executes(WorldBossCommand::ready)));
	}

	private static int results(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		WorldBossResults results = WorldBossResultsCache.get(player.getUUID());
		if (results == null) {
			ctx.getSource().sendFailure(Component.translatable("worldboss.dragonminez.results.none"));
			return 0;
		}
		NetworkHandler.sendToPlayer(new WorldBossResultsS2C(results), player);
		return 1;
	}

	private static int locate(CommandContext<CommandSourceStack> ctx) {
		BlockPos lair = WorldBossManager.getLair(ctx.getSource().getServer());
		if (lair == null) {
			ctx.getSource().sendFailure(Component.literal("No lair has been chosen yet. Enter the Otherworld once."));
			return 0;
		}

		WorldBossSavedData data = WorldBossSavedData.get(ctx.getSource().getServer());
		WorldBossSavedData.Entry entry = data.peek(WorldBossManager.JANEMBA);
		long remaining = WorldBossManager.getRespawnRemainingTicks(ctx.getSource().getServer());

		ctx.getSource().sendSuccess(() -> Component.literal(
				"Janemba lair: " + lair.getX() + " " + lair.getY() + " " + lair.getZ()
						+ " | resolved=" + (entry != null && entry.lairResolved)
						+ " | arena=" + (entry != null && entry.arenaBuilt)
						+ " | alive=" + (entry != null && entry.bossId != null)
						+ " | respawn in " + (remaining / 20L) + "s"), false);
		return 1;
	}

	private static int teleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		BlockPos lair = WorldBossManager.getLair(ctx.getSource().getServer());
		if (lair == null) {
			ctx.getSource().sendFailure(Component.literal("No lair has been chosen yet. Enter the Otherworld once."));
			return 0;
		}

		ServerLevel otherworld = WorldBossManager.otherworld(ctx.getSource().getServer());
		if (otherworld == null) {
			ctx.getSource().sendFailure(Component.literal("The Otherworld is not loaded."));
			return 0;
		}

		player.teleportTo(otherworld, lair.getX() + 0.5D, lair.getY() + 2.0D, lair.getZ() + 0.5D,
				player.getYRot(), player.getXRot());
		ctx.getSource().sendSuccess(() -> Component.literal("Teleported to the Janemba lair."), false);
		return 1;
	}

	private static int reset(CommandContext<CommandSourceStack> ctx) {
		WorldBossSavedData data = WorldBossSavedData.get(ctx.getSource().getServer());
		WorldBossSavedData.Entry entry = data.entry(WorldBossManager.JANEMBA);

		ServerLevel otherworld = WorldBossManager.otherworld(ctx.getSource().getServer());
		if (otherworld != null && entry.bossId != null && otherworld.getEntity(entry.bossId) != null) {
			otherworld.getEntity(entry.bossId).discard();
		}

		entry.lair = null;
		entry.lairResolved = false;
		entry.arenaBuilt = false;
		entry.bossId = null;
		entry.nextRespawnTick = 0L;
		data.markDirty();

		ctx.getSource().sendSuccess(() -> Component.literal("World boss state cleared. A new lair will be chosen."), false);
		return 1;
	}

	private static int ready(CommandContext<CommandSourceStack> ctx) {
		WorldBossSavedData data = WorldBossSavedData.get(ctx.getSource().getServer());
		WorldBossSavedData.Entry entry = data.entry(WorldBossManager.JANEMBA);
		entry.nextRespawnTick = 0L;
		data.markDirty();

		ctx.getSource().sendSuccess(() -> Component.literal("Respawn cooldown cleared."), false);
		return 1;
	}
}
