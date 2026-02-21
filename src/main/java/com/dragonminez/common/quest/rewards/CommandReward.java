package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CommandReward extends QuestReward {
    private final String command;
    private String translationKey;

    public CommandReward(String command, String translationKey) {
        super(RewardType.COMMAND);
        this.command = command;
        this.translationKey = translationKey;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public void giveReward(ServerPlayer player) {
        if (!isClaimed()) {
            String commandToExecute = command.replace("%player%", player.getName().getString());
            player.getServer().getCommands().performPrefixedCommand(player.getServer().createCommandSourceStack().withPermission(4), commandToExecute);
            setClaimed(true);
        }
    }

    @Override
    public Component getDescription() {
        if (translationKey != null && !translationKey.isEmpty()) {
            return Component.translatable(translationKey);
        } else {
            return Component.translatable("gui.dragonminez.quests.rewards.command", command);
        }
    }
}

