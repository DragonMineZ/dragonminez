package com.dragonminez.server.commands;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RestoreCommand {

	private static final long CONFIRM_WINDOW_MS = 15_000L;
	private static final UUID CONSOLE_KEY = new UUID(0L, 0L);
	private static final Map<UUID, Long> PENDING = new HashMap<>();

	private static final String[] TARGET_FOLDERS = {"wishes", "sagas", "quests"};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzrestoreupdate")
				.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.ADMIN))
				.executes(ctx -> requestConfirmation(ctx.getSource()))
				.then(Commands.literal("confirm")
						.executes(ctx -> confirm(ctx.getSource())))
		);
	}

	private static UUID sourceKey(CommandSourceStack source) {
		return source.getEntity() instanceof ServerPlayer player ? player.getUUID() : CONSOLE_KEY;
	}

	private static int requestConfirmation(CommandSourceStack source) {
		PENDING.put(sourceKey(source), System.currentTimeMillis() + CONFIRM_WINDOW_MS);

		Component confirmButton = Component.translatable("command.dragonminez.restore.confirm.button")
				.withStyle(style -> style
						.withColor(ChatFormatting.GREEN)
						.withBold(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dmzrestoreupdate confirm"))
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
								Component.translatable("command.dragonminez.restore.confirm.hover"))));

		source.sendSystemMessage(Component.translatable("command.dragonminez.restore.warning").withStyle(ChatFormatting.RED));
		source.sendSystemMessage(Component.translatable("command.dragonminez.restore.prompt")
				.append(Component.literal(" ["))
				.append(confirmButton)
				.append(Component.literal("]")));
		return 1;
	}

	private static int confirm(CommandSourceStack source) {
		Long expiry = PENDING.remove(sourceKey(source));
		if (expiry == null || System.currentTimeMillis() > expiry) {
			source.sendFailure(Component.translatable("command.dragonminez.restore.expired"));
			return 0;
		}

		MinecraftServer server = source.getServer();
		Path worldFolder = server.getWorldPath(LevelResource.ROOT).resolve("dragonminez");

		source.sendSystemMessage(Component.translatable("command.dragonminez.restore.start"));

		try {
			for (String folder : TARGET_FOLDERS) {
				clearDirectory(worldFolder.resolve(folder));
			}
		} catch (IOException e) {
			source.sendFailure(Component.translatable("command.dragonminez.restore.error", e.getMessage()));
			LogUtil.error(Env.SERVER, "Failed to clear DragonMineZ data folders during restore", e);
			return 0;
		}

		return ReloadCommand.executeReload(source, "all");
	}

	private static void clearDirectory(Path dir) throws IOException {
		if (!Files.exists(dir)) return;
		try (var stream = Files.walk(dir)) {
			stream.filter(path -> !path.equals(dir))
					.sorted(Comparator.reverseOrder())
					.forEach(path -> {
						try {
							Files.delete(path);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
		} catch (RuntimeException e) {
			if (e.getCause() instanceof IOException io) throw io;
			throw e;
		}
	}
}
