package com.dragonminez.server.commands;

import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallSetDefinition;
import com.dragonminez.server.events.DragonBallsHandler;
import com.dragonminez.server.world.data.DragonBallSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DragonBallsCommand {
	private static final int MAX_LISTED_POSITIONS = 8;

	private static final SuggestionProvider<CommandSourceStack> SET_SUGGESTIONS = (context, builder) ->
			SharedSuggestionProvider.suggest(DragonBallDefinitions.getBallSets().stream().map(DragonBallSetDefinition::getId), builder);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzdragonballs")
				.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.DRAGONBALLS_STATUS)
						|| DMZPermissions.hasPermission(source, DMZPermissions.DRAGONBALLS_REGENERATE))
				.then(Commands.literal("status")
						.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.DRAGONBALLS_STATUS))
						.executes(context -> statusAll(context.getSource()))
						.then(Commands.argument("set", StringArgumentType.string())
								.suggests(SET_SUGGESTIONS)
								.executes(context -> statusSet(context.getSource(), StringArgumentType.getString(context, "set")))))
				.then(Commands.literal("regenerate")
						.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.DRAGONBALLS_REGENERATE))
						.then(Commands.argument("set", StringArgumentType.string())
								.suggests(SET_SUGGESTIONS)
								.executes(context -> regenerate(context.getSource(), StringArgumentType.getString(context, "set"))))));
	}

	private static int statusAll(CommandSourceStack source) {
		int shown = 0;
		for (DragonBallSetDefinition definition : DragonBallDefinitions.getBallSets()) {
			int placed = 0;
			int pending = 0;
			int dragons = 0;
			for (ServerLevel level : getLevels(source, definition)) {
				DragonBallSavedData data = DragonBallSavedData.get(level);
				for (int star : definition.getStars()) {
					placed += data.getActiveBalls(definition.getId(), star).size();
					pending += data.getPendingBalls(definition.getId(), star).size();
				}
				dragons += getWaitingDragons(data, definition.getId()).size();
			}
			int copies = definition.getCopies();
			int expected = copies * definition.getStars().size();
			ChatFormatting color = placed + pending >= expected ? ChatFormatting.GREEN : ChatFormatting.RED;
			int finalPlaced = placed;
			int finalPending = pending;
			int finalDragons = dragons;
			source.sendSuccess(() -> Component.translatable("command.dragonminez.dragonballs.summary",
					setLink(definition.getId()),
					finalPlaced,
					finalPending,
					Component.literal((finalPlaced + finalPending) + "/" + expected).withStyle(color),
					copies,
					finalDragons), false);
			shown++;
		}
		return shown;
	}

	private static int statusSet(CommandSourceStack source, String setId) {
		DragonBallSetDefinition definition = DragonBallDefinitions.getBallSet(setId);
		if (definition == null) {
			source.sendFailure(Component.translatable("command.dragonminez.dragonballs.unknown_set", setId));
			return 0;
		}
		List<ServerLevel> levels = getLevels(source, definition);
		if (levels.isEmpty()) {
			source.sendFailure(Component.translatable("command.dragonminez.dragonballs.no_level", setId));
			return 0;
		}

		int copies = definition.getCopies();
		for (ServerLevel level : levels) {
			DragonBallSavedData data = DragonBallSavedData.get(level);
			source.sendSuccess(() -> Component.translatable("command.dragonminez.dragonballs.header",
					Component.literal(definition.getId()).withStyle(ChatFormatting.YELLOW),
					Component.literal(level.dimension().location().toString()).withStyle(ChatFormatting.AQUA),
					copies), false);

			for (int star : definition.getStars().stream().sorted().toList()) {
				List<BlockPos> active = data.getActiveBalls(setId, star);
				List<BlockPos> pending = data.getPendingBalls(setId, star);
				MutableComponent positions = Component.empty();
				int listed = 0;
				for (BlockPos pos : active) {
					if (listed++ >= MAX_LISTED_POSITIONS) break;
					positions.append(" ").append(positionLink(source, level, pos, true));
				}
				for (BlockPos pos : pending) {
					if (listed++ >= MAX_LISTED_POSITIONS) break;
					positions.append(" ").append(positionLink(source, level, pos, false));
				}
				int hidden = active.size() + pending.size() - MAX_LISTED_POSITIONS;
				if (hidden > 0) positions.append(Component.literal(" +" + hidden).withStyle(ChatFormatting.DARK_GRAY));
				ChatFormatting color = active.size() + pending.size() >= copies ? ChatFormatting.GREEN : ChatFormatting.RED;
				source.sendSuccess(() -> Component.translatable("command.dragonminez.dragonballs.star",
						Component.literal(String.valueOf(star)).withStyle(color),
						active.size(),
						pending.size(),
						positions), false);
			}

			long now = level.getGameTime();
			for (Map.Entry<UUID, DragonBallSavedData.Summon> entry : getWaitingDragons(data, setId)) {
				long secondsLeft = Math.max(0L, (entry.getValue().expiresAt() - now) / 20L);
				boolean loaded = level.getEntity(entry.getKey()) != null;
				source.sendSuccess(() -> Component.translatable(loaded
								? "command.dragonminez.dragonballs.dragon"
								: "command.dragonminez.dragonballs.dragon_unloaded",
						Component.literal(String.format("%d:%02d", secondsLeft / 60, secondsLeft % 60)).withStyle(ChatFormatting.YELLOW)), false);
			}
		}
		return 1;
	}

	private static int regenerate(CommandSourceStack source, String setId) {
		DragonBallSetDefinition definition = DragonBallDefinitions.getBallSet(setId);
		if (definition == null) {
			source.sendFailure(Component.translatable("command.dragonminez.dragonballs.unknown_set", setId));
			return 0;
		}
		ServerLevel level = DragonBallsHandler.getHomeLevel(source.getServer(), definition);
		if (level == null) {
			source.sendFailure(Component.translatable("command.dragonminez.dragonballs.no_level", setId));
			return 0;
		}

		int added = DragonBallsHandler.regenerateSet(level, setId);
		if (added == 0) {
			source.sendSuccess(() -> Component.translatable("command.dragonminez.dragonballs.complete",
					Component.literal(setId).withStyle(ChatFormatting.YELLOW), definition.getCopies()), true);
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("command.dragonminez.dragonballs.regenerated",
				added, Component.literal(setId).withStyle(ChatFormatting.YELLOW), definition.getCopies()), true);
		return added;
	}

	private static List<ServerLevel> getLevels(CommandSourceStack source, DragonBallSetDefinition definition) {
		List<ServerLevel> levels = new ArrayList<>();
		for (ResourceLocation dimension : definition.getValidDimensions()) {
			ServerLevel level = source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
			if (level != null) levels.add(level);
		}
		return levels;
	}

	private static List<Map.Entry<UUID, DragonBallSavedData.Summon>> getWaitingDragons(DragonBallSavedData data, String setId) {
		return data.getSummons().entrySet().stream().filter(entry -> setId.equals(entry.getValue().setId())).toList();
	}

	private static Component setLink(String setId) {
		return ComponentUtils.wrapInSquareBrackets(Component.literal(setId)).withStyle(style -> style
				.withColor(ChatFormatting.YELLOW)
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dmzdragonballs status " + setId))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("command.dragonminez.dragonballs.details_hover"))));
	}

	private static Component positionLink(CommandSourceStack source, ServerLevel level, BlockPos pos, boolean placed) {
		int tpY = placed ? pos.getY() + 1 : (int) source.getPosition().y;
		String text = placed ? pos.getX() + ", " + pos.getY() + ", " + pos.getZ() : pos.getX() + ", ?, " + pos.getZ();
		return ComponentUtils.wrapInSquareBrackets(Component.literal(text)).withStyle(style -> style
				.withColor(placed ? ChatFormatting.GREEN : ChatFormatting.GRAY)
				.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in " + level.dimension().location() + " run tp @s " + pos.getX() + " " + tpY + " " + pos.getZ()))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(placed
						? "command.dragonminez.dragonballs.placed_hover"
						: "command.dragonminez.dragonballs.pending_hover"))));
	}
}
