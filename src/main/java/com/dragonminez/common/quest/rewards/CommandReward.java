package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CommandReward extends QuestReward {
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
		String commandToExecute = command.replace("%player%", player.getName().getString());
		player.getServer().getCommands().performPrefixedCommand(player.getServer().createCommandSourceStack().withPermission(4), commandToExecute);
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

