package com.dragonminez.common.wish.wishes;

import com.dragonminez.common.wish.Wish;
import net.minecraft.server.level.ServerPlayer;

public class CommandWish extends Wish {
	private final String[] commands;

	public CommandWish(String name, String description, String... commands) {
		super(name, description, "command");
		this.commands = commands;
	}

	@Override
	public void grant(ServerPlayer player) {
		for (String command : commands) {
			String parsedCommand = command.replace("%player%", commandSafeName(player));
			player.getServer().getCommands().performPrefixedCommand(player.getServer().createCommandSourceStack().withPermission(4), parsedCommand);
		}
	}


	private static String commandSafeName(ServerPlayer player) {
		String name = player.getGameProfile().getName();
		return name != null && name.matches("[A-Za-z0-9_]{1,16}") ? name : player.getStringUUID();
	}
}
