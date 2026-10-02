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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class WorldBossCommand {

	private interface BossAction {
		int run(CommandContext<CommandSourceStack> ctx, WorldBossLair lair) throws CommandSyntaxException;
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzworldboss")
				.then(Commands.literal("results")
						.executes(WorldBossCommand::results))
				.then(bossCommand("locate", WorldBossCommand::locate).executes(WorldBossCommand::locateAll))
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

	private static int locateAll(CommandContext<CommandSourceStack> ctx) {
		int found = 0;
		for (WorldBossLair lair : WorldBossManager.lairs()) found += locate(ctx, lair);
		return found;
	}

	private static int locate(CommandContext<CommandSourceStack> ctx, WorldBossLair lair) {
		CommandSourceStack source = ctx.getSource();
		MinecraftServer server = source.getServer();
		BlockPos pos = WorldBossManager.getLair(server, lair.key());
		if (pos == null) {
			source.sendFailure(notChosen(lair));
			return 0;
		}

		Component name = Component.literal(lair.displayName()).withStyle(ChatFormatting.YELLOW);
		Component coords = ComponentUtils.wrapInSquareBrackets(Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ()))
				.withStyle(style -> style
						.withColor(ChatFormatting.GREEN)
						.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/dmzworldboss tp " + lair.key()))
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("command.dragonminez.worldboss.click_tp"))));

		Component found;
		if (source.getLevel().dimension().equals(lair.dimension())) {
			Vec3 from = source.getPosition();
			double dx = from.x - (pos.getX() + 0.5D);
			double dz = from.z - (pos.getZ() + 0.5D);
			int distance = Mth.floor(Math.sqrt(dx * dx + dz * dz));
			found = Component.translatable("command.dragonminez.worldboss.locate.found", name, coords, distance);
		} else {
			found = Component.translatable("command.dragonminez.worldboss.locate.found_elsewhere", name, coords,
					Component.literal(lair.dimension().location().toString()).withStyle(ChatFormatting.AQUA));
		}
		Component status = status(server, lair);
		source.sendSuccess(() -> found, false);
		source.sendSuccess(() -> status, false);
		return 1;
	}

	private static Component status(MinecraftServer server, WorldBossLair lair) {
		WorldBossManager.Data.Entry entry = WorldBossManager.Data.get(server).peek(lair.key());
		long remaining = WorldBossManager.getRespawnRemainingTicks(server, lair.key());
		MutableComponent state;
		if (entry != null && entry.bossId != null) {
			state = Component.translatable("command.dragonminez.worldboss.status.alive").withStyle(ChatFormatting.GREEN);
		} else if (remaining > 0L) {
			state = Component.translatable("command.dragonminez.worldboss.status.respawn", formatTicks(remaining)).withStyle(ChatFormatting.GOLD);
		} else {
			state = Component.translatable("command.dragonminez.worldboss.status.waiting").withStyle(ChatFormatting.AQUA);
		}
		if (entry == null || !entry.lairResolved || !entry.arenaBuilt) {
			state.append(Component.translatable("command.dragonminez.worldboss.status.lair_pending").withStyle(ChatFormatting.DARK_GRAY));
		}
		return Component.translatable("command.dragonminez.worldboss.locate.status", state).withStyle(ChatFormatting.GRAY);
	}

	private static String formatTicks(long ticks) {
		long seconds = ticks / 20L;
		long hours = seconds / 3600L;
		long minutes = (seconds % 3600L) / 60L;
		long rest = seconds % 60L;
		if (hours > 0L) return String.format("%dh %02dm", hours, minutes);
		if (minutes > 0L) return String.format("%dm %02ds", minutes, rest);
		return rest + "s";
	}

	private static Component notChosen(WorldBossLair lair) {
		return Component.translatable("command.dragonminez.worldboss.not_chosen", lair.displayName(), lair.dimension().location().toString());
	}

	private static int teleport(CommandContext<CommandSourceStack> ctx, WorldBossLair lair) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		BlockPos pos = WorldBossManager.getLair(ctx.getSource().getServer(), lair.key());
		if (pos == null) {
			ctx.getSource().sendFailure(notChosen(lair));
			return 0;
		}

		ServerLevel level = WorldBossManager.levelFor(ctx.getSource().getServer(), lair.key());
		if (level == null) {
			ctx.getSource().sendFailure(Component.translatable("command.dragonminez.worldboss.not_loaded", lair.dimension().location().toString()));
			return 0;
		}

		BlockPos target = lair.teleportTarget(level, pos);
		player.teleportTo(level, target.getX() + 0.5D, target.getY() + 2.0D, target.getZ() + 0.5D,
				player.getYRot(), player.getXRot());
		ctx.getSource().sendSuccess(() -> Component.translatable("command.dragonminez.worldboss.tp.success",
				Component.literal(lair.displayName()).withStyle(ChatFormatting.YELLOW)), false);
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
