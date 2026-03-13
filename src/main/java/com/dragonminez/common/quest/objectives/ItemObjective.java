package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemObjective extends QuestObjective {
    private final String itemId;
    private final int count;

    public ItemObjective(String description, Item item, int count) {
        super(ObjectiveType.ITEM, description, count);
        this.itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        this.count = count;
    }

    public String getItemId() {
        return itemId;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean checkProgress(Object... params) {
        if (params.length > 0 && params[0] instanceof ItemStack stack) {
            Item requiredItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            if (stack.is(requiredItem)) {
                addProgress(stack.getCount());
                return isCompleted();
            }
        }
        return false;
    }
}
