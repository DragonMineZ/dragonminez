package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import lombok.Getter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.regex.Pattern;

public class CommandReward extends QuestReward {
	private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

	@Getter
	private final String command;
	@Getter
	private final String translationKey;

	public CommandReward(String command, String translationKey) {
		super(RewardType.COMMAND);
		this.command = command;
		this.translationKey = translationKey;
	}

	@Override
	public void giveReward(ServerPlayer player) {
		runAs(player, command);
	}

	public static void runAs(ServerPlayer player, String rawCommand) {
		if (player == null || rawCommand == null || rawCommand.isBlank()) return;
		String commandToExecute = rawCommand.replace("%player%", commandTarget(player));
		CommandSourceStack source = player.getServer().createCommandSourceStack()
				.withPermission(4)
				.withEntity(player)
				.withLevel(player.serverLevel())
				.withPosition(player.position())
				.withRotation(player.getRotationVector());
		player.getServer().getCommands().performPrefixedCommand(source, commandToExecute);
	}

	private static String commandTarget(ServerPlayer player) {
		String name = player.getGameProfile().getName();
		return name != null && SAFE_NAME.matcher(name).matches() ? name : player.getStringUUID();
	}

	@Override
	public Component getDescription() {
		if (translationKey != null && !translationKey.isEmpty()) {
			return Component.translatable(translationKey);
		} else {
			String display = command.startsWith("/") ? command : "/" + command;
			return Component.literal(display);
		}
	}
}

