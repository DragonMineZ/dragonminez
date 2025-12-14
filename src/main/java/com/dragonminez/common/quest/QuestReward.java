package com.dragonminez.common.quest;

public abstract class QuestReward {
    private final RewardType type;
    private boolean claimed;

    public QuestReward(RewardType type) {
        this.type = type;
        this.claimed = false;
    }

    public RewardType getType() {
        return type;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }

    public abstract void giveReward(net.minecraft.server.level.ServerPlayer player);

    public enum RewardType {
        ITEM,
        COMMAND,
        TPS
    }
}

