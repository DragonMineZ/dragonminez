package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import net.minecraft.server.level.ServerPlayer;

public class CommandReward extends QuestReward {
    private final String command;

    public CommandReward(String command) {
        super(RewardType.COMMAND);
        this.command = command;
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
}

