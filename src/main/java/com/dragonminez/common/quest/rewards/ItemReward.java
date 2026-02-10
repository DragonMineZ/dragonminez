package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemReward extends QuestReward {
    private final String itemId;
    private final int count;

    public ItemReward(ItemStack itemStack) {
        super(RewardType.ITEM);
        this.itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        this.count = itemStack.getCount();
    }

    public String getItemId() {
        return itemId;
    }

    public int getCount() {
        return count;
    }

    @Override
    public void giveReward(ServerPlayer player) {
        if (!isClaimed()) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            player.addItem(new ItemStack(item, count));
            setClaimed(true);
        }
    }
}
