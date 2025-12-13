package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ItemReward extends QuestReward {
    private final ItemStack itemStack;

    public ItemReward(ItemStack itemStack) {
        super(RewardType.ITEM);
        this.itemStack = itemStack;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public void giveReward(ServerPlayer player) {
        if (!isClaimed()) {
            player.addItem(itemStack.copy());
            setClaimed(true);
        }
    }
}

