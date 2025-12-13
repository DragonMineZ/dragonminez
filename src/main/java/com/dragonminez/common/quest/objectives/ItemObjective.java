package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemObjective extends QuestObjective {
    private final Item item;
    private final int count;

    public ItemObjective(String description, Item item, int count) {
        super(ObjectiveType.ITEM, description, count);
        this.item = item;
        this.count = count;
    }

    public Item getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean checkProgress(Object... params) {
        if (params.length > 0 && params[0] instanceof ItemStack stack) {
            if (stack.is(item)) {
                addProgress(stack.getCount());
                return isCompleted();
            }
        }
        return false;
    }
}

