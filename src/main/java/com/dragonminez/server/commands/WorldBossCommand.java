package com.dragonminez.server.commands;

import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.WorldBossResultsS2C;
import com.dragonminez.common.worldboss.WorldBossResults;
import com.dragonminez.server.world.worldboss.WorldBossLair;
import com.dragonminez.server.world.worldboss.WorldBossManager;
import com.dragonminez.server.world.worldboss.WorldBossRewards;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class WorldBossCommand {

	private interface BossAction {
		int run(CommandContext<CommandSourceStack> ctx, WorldBossLair lair) throws CommandSyntaxException;
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzworldboss")
				.then(Commands.literal("results")
						.executes(WorldBossCommand::results))
				.then(bossCommand("locate", WorldBossCommand::locate))
				.then(bossCommand("tp", WorldBossCommand::teleport))
				.then(bossCommand("reset", WorldBossCommand::reset))
				.then(bossCommand("ready", WorldBossCommand::ready)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> bossCommand(String name, BossAction action) {
		return Commands.literal(name)
				.requires(source -> source.hasPermission(2))
				.executes(ctx -> run(ctx, WorldBossEntity.JANEMBA, action))
				.then(Commands.argument("boss", StringArgumentType.word())
						.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
								WorldBossManager.lairs().stream().map(WorldBossLair::key), builder))
						.executes(ctx -> run(ctx, StringArgumentType.getString(ctx, "boss"), action)));
	}

	private static int run(CommandContext<CommandSourceStack> ctx, String key, BossAction action) throws CommandSyntaxException {
		WorldBossLair lair = WorldBossManager.lair(key);
		if (lair == null) {
			ctx.getSource().sendFailure(Component.literal("Unknown world boss: " + key));
			return 0;
		}
		return action.run(ctx, lair);
	}

	private static int results(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		WorldBossResults results = WorldBossRewards.getResults(player.getUUID());
		if (results == null) {
			ctx.getSource().sendFailure(Component.translatable("worldboss.dragonminez.results.none"));
			return 0;
		}
		NetworkHandler.sendToPlayer(new WorldBossResultsS2C(results), player);
		return 1;
	}

	private static int locate(CommandContext<CommandSourceStack> ctx, WorldBossLair lair) {
		BlockPos pos = WorldBossManager.getLair(ctx.getSource().getServer(), lair.key());
		if (pos == null) {
			ctx.getSource().sendFailure(Component.literal("No lair has been chosen yet for " + lair.displayName()
					+ ". Visit " + lair.dimension().location() + " once."));
			return 0;
		}

		WorldBossManager.Data data = WorldBossManager.Data.get(ctx.getSource().getServer());
		WorldBossManager.Data.Entry entry = data.peek(lair.key());
		long remaining = WorldBossManager.getRespawnRemainingTicks(ctx.getSource().getServer(), lair.key());

		ctx.getSource().sendSuccess(() -> Component.literal(
				lair.displayName() + " lair: " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
						+ " (" + lair.dimension().location() + ")"
						+ " | resolved=" + (entry != null && entry.lairResolved)
						+ " | arena=" + (entry != null && entry.arenaBuilt)
						+ " | alive=" + (entry != null && entry.bossId != null)
						+ " | respawn in " + (remaining / 20L) + "s"), false);
		return 1;
	}

	private static int teleport(CommandContext<CommandSourceStack> ctx, WorldBossLair lair) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		BlockPos pos = WorldBossManager.getLair(ctx.getSource().getServer(), lair.key());
		if (pos == null) {
			ctx.getSource().sendFailure(Component.literal("No lair has been chosen yet for " + lair.displayName()
					+ ". Visit " + lair.dimension().location() + " once."));
			return 0;
		}

		ServerLevel level = WorldBossManager.levelFor(ctx.getSource().getServer(), lair.key());
		if (level == null) {
			ctx.getSource().sendFailure(Component.literal(lair.dimension().location() + " is not loaded."));
			return 0;
		}

		player.teleportTo(level, pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D,
				player.getYRot(), player.getXRot());
		ctx.getSource().sendSuccess(() -> Component.literal("Teleported to the " + lair.displayName() + " lair."), false);
		return 1;
	}

	private static int reset(CommandContext<CommandSourceStack> ctx, WorldBossLair lair) {
		WorldBossManager.Data data = WorldBossManager.Data.get(ctx.getSource().getServer());
		WorldBossManager.Data.Entry entry = data.entry(lair.key());

		ServerLevel level = WorldBossManager.levelFor(ctx.getSource().getServer(), lair.key());
		WorldBossManager.endFight(lair.key(), level);
		if (level != null && entry.bossId != null) {
			Entity boss = level.getEntity(entry.bossId);
			if (boss != null) boss.discard();
		}

		entry.lair = null;
		entry.lairResolved = false;
		entry.arenaBuilt = false;
		entry.bossId = null;
		entry.nextRespawnTick = 0L;
		data.markDirty();

		ctx.getSource().sendSuccess(() -> Component.literal("World boss state cleared for " + lair.displayName() + "."), false);
		return 1;
	}

	private static int ready(CommandContext<CommandSourceStack> ctx, WorldBossLair lair) {
		WorldBossManager.Data data = WorldBossManager.Data.get(ctx.getSource().getServer());
		WorldBossManager.Data.Entry entry = data.entry(lair.key());
		entry.nextRespawnTick = 0L;
		data.markDirty();

		ctx.getSource().sendSuccess(() -> Component.literal("Respawn cooldown cleared for " + lair.displayName() + "."), false);
		return 1;
	}
}
