package com.dragonminez.client.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@OnlyIn(Dist.CLIENT)
public final class DebugFileWriter {

	private DebugFileWriter() {
	}

	public static void write(String playerName, String text) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) return;
		if (text == null) {
			minecraft.player.displayClientMessage(Component.translatable("command.dragonminez.debug.error", "invalid data"), false);
			return;
		}

		String safeName = playerName.replaceAll("[^A-Za-z0-9_\\-]", "_");
		if (safeName.isEmpty()) safeName = "player";

		Path debugDir = FMLPaths.GAMEDIR.get().resolve("dmzdebug");
		Path outputPath = debugDir.resolve(safeName + ".json");
		try {
			Files.createDirectories(debugDir);
			Files.writeString(outputPath, text, StandardCharsets.UTF_8);
		} catch (IOException exception) {
			minecraft.player.displayClientMessage(Component.translatable("command.dragonminez.debug.error", exception.getMessage()), false);
			return;
		}

		String folder = debugDir.toAbsolutePath().normalize().toString();
		Component clickablePath = Component.literal(folder)
				.withStyle(style -> style
						.withColor(ChatFormatting.AQUA)
						.withUnderlined(true)
						.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, folder))
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("command.dragonminez.debug.open_folder"))));
		minecraft.player.displayClientMessage(Component.translatable("command.dragonminez.debug.success", playerName, clickablePath), false);
	}
}
