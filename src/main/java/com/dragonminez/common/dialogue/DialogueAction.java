package com.dragonminez.common.dialogue;

import com.dragonminez.common.quest.QuestService;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;

/**
 * A server-side effect a dialogue choice can trigger. JSON shape:
 * {@code {"type": "ALIGNMENT", "amount": -5}}, {@code {"type": "TPS", "amount": 100}},
 * {@code {"type": "COMMAND", "command": "give %player% minecraft:apple"}},
 * {@code {"type": "START_QUEST", "quest": "my_sidequest"}}, {@code {"type": "OPEN_QUESTS"}}.
 */
@Getter
public class DialogueAction {

	public enum Type {
		ALIGNMENT,
		TPS,
		COMMAND,
		START_QUEST,
		OPEN_QUESTS
	}

	private final Type type;
	private final int amount;
	private final String value;

	public DialogueAction(Type type, int amount, String value) {
		this.type = type;
		this.amount = amount;
		this.value = value != null ? value : "";
	}

	/** Executes this action. OPEN_QUESTS is handled by DialogueService, not here. */
	public void execute(ServerPlayer player) {
		switch (type) {
			case ALIGNMENT -> StatsProvider.get(StatsCapability.INSTANCE, player)
					.ifPresent(data -> data.getResources().addAlignment(amount));
			case TPS -> {
				if (amount > 0) StatsProvider.get(StatsCapability.INSTANCE, player)
						.ifPresent(data -> data.getResources().addTrainingPoints(amount, false));
			}
			case COMMAND -> {
				if (!value.isBlank()) {
					String parsed = value.replace("%player%", player.getName().getString());
					player.getServer().getCommands().performPrefixedCommand(
							player.getServer().createCommandSourceStack().withPermission(4), parsed);
				}
			}
			case START_QUEST -> {
				if (!value.isBlank()) QuestService.startQuest(player, value);
			}
			case OPEN_QUESTS -> {
			}
		}
	}
}
