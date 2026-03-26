package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DebugCommand {
	private static final SuggestionProvider<CommandSourceStack> DEBUG_VALUE_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(List.of("ALL", "STATS", "CHARACTER", "TECHNIQUES"), builder);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzdebug")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.ADMIN, DMZPermissions.ADMIN))
				.executes(ctx -> writeDebugFile(ctx.getSource(), ctx.getSource().getPlayerOrException(), DebugScope.ALL))
				.then(Commands.argument("value", StringArgumentType.word()).suggests(DEBUG_VALUE_SUGGESTIONS)
						.executes(ctx -> writeDebugFile(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "value"))))
				.then(Commands.argument("target", EntityArgument.player())
						.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.ADMIN))
						.executes(ctx -> writeDebugFile(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), DebugScope.ALL))
						.then(Commands.argument("value", StringArgumentType.word()).suggests(DEBUG_VALUE_SUGGESTIONS)
								.executes(ctx -> writeDebugFile(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), StringArgumentType.getString(ctx, "value")))))
		);
	}

	private static int writeDebugFile(CommandSourceStack source, ServerPlayer target, String rawScope) {
		DebugScope scope = DebugScope.from(rawScope);
		if (scope == null) {
			source.sendFailure(Component.translatable("command.dragonminez.debug.invalid_value", rawScope));
			return 0;
		}
		return writeDebugFile(source, target, scope);
	}

	private static int writeDebugFile(CommandSourceStack source, ServerPlayer target, DebugScope scope) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		return StatsProvider.get(StatsCapability.INSTANCE, target)
				.map(data -> {
					try {
						Path outputPath = getOutputPath(target);
						Files.createDirectories(outputPath.getParent());
						Files.writeString(outputPath, buildDebugText(target, data, scope), StandardCharsets.UTF_8);
						Path debugDir = outputPath.getParent();
						Component clickablePath = Component.literal(debugDir.toString())
								.withStyle(style -> style
										.withColor(ChatFormatting.AQUA)
										.withUnderlined(true)
										.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, debugDir.toString()))
										.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open folder"))));
						source.sendSuccess(() -> Component.translatable("command.dragonminez.debug.success", target.getName().getString(), clickablePath), log);
						return 1;
					} catch (IOException exception) {
						source.sendFailure(Component.translatable("command.dragonminez.debug.error", exception.getMessage()));
						return 0;
					}
				})
				.orElseGet(() -> {
					source.sendFailure(Component.translatable("command.dragonminez.debug.no_data", target.getName().getString()));
					return 0;
				});
	}

	private static Path getOutputPath(ServerPlayer target) {
		Path debugDir = FMLPaths.GAMEDIR.get().resolve("dmzdebug");
		String fileName = target.getGameProfile().getName() + ".log";
		return debugDir.resolve(fileName);
	}

	private static String buildDebugText(ServerPlayer target, StatsData data, DebugScope scope) {
		CompoundTag root = data.save();
		StringBuilder builder = new StringBuilder();
		String playerName = target.getGameProfile().getName();

		builder.append("##############\n");
		builder.append("## DMZ DEBUG ##\n");
		builder.append("## ").append(playerName).append(" ##\n");
		builder.append("##############\n\n");

		if (scope.includesStats()) {
			appendSection(builder, "Stats", root.getCompound("Stats"));
			appendSection(builder, "BonusStats", root.getCompound("BonusStats"));
			appendSection(builder, "Resources", root.getCompound("Resources"));
			appendSection(builder, "Status", root.getCompound("Status"));
			appendSection(builder, "Cooldowns", root.getCompound("Cooldowns"));
			appendSection(builder, "Effects", root.getCompound("Effects"));
			appendSection(builder, "Skills", root.getCompound("Skills"));
		}

		if (scope.includesCharacter()) {
			appendSection(builder, "Character", root.getCompound("Character"));
			appendSection(builder, "Training", root.getCompound("Training"));
		}

		if (scope.includesTechniques()) {
			appendSection(builder, "Techniques", root.getCompound("Techniques"));
		}

		return builder.toString();
	}

	private static void appendSection(StringBuilder builder, String title, CompoundTag tag) {
		builder.append("# ").append(title).append(":\n");
		if (tag.isEmpty()) {
			builder.append("(empty)\n\n");
			return;
		}
		appendCompound(builder, tag, 0);
		builder.append("\n");
	}

	private static void appendCompound(StringBuilder builder, CompoundTag tag, int indent) {
		List<String> keys = new ArrayList<>(tag.getAllKeys());
		keys.sort(String::compareTo);
		for (String key : keys) {
			appendTag(builder, key, tag.get(key), indent);
		}
	}

	private static void appendTag(StringBuilder builder, String key, Tag tag, int indent) {
		String pad = " ".repeat(indent);
		if (tag instanceof CompoundTag nested) {
			builder.append(pad).append(key).append(":\n");
			if (nested.isEmpty()) {
				builder.append(pad).append("  (empty)\n");
			} else {
				appendCompound(builder, nested, indent + 2);
			}
			return;
		}

		if (tag instanceof ListTag listTag) {
			builder.append(pad).append(key).append(":\n");
			if (listTag.isEmpty()) {
				builder.append(pad).append("  (empty)\n");
				return;
			}
			for (int i = 0; i < listTag.size(); i++) {
				Tag listElement = listTag.get(i);
				if (listElement instanceof CompoundTag listCompound) {
					builder.append(pad).append("  - ").append(i).append(":\n");
					appendCompound(builder, listCompound, indent + 4);
				} else {
					builder.append(pad).append("  - ").append(listElement.getAsString()).append("\n");
				}
			}
			return;
		}

		builder.append(pad).append(key).append(": ").append(tag == null ? "null" : tag.getAsString()).append("\n");
	}

	private enum DebugScope {
		ALL,
		STATS,
		CHARACTER,
		TECHNIQUES;

		private static DebugScope from(String value) {
			try {
				return DebugScope.valueOf(value.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException exception) {
				return null;
			}
		}

		private boolean includesStats() {
			return this == ALL || this == STATS;
		}

		private boolean includesCharacter() {
			return this == ALL || this == CHARACTER;
		}

		private boolean includesTechniques() {
			return this == ALL || this == TECHNIQUES;
		}
	}
}

