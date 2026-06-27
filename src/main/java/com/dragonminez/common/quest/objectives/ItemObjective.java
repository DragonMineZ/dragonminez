package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

@Getter
public class ItemObjective extends QuestObjective {
    private final String itemId;
    private final int count;

    public ItemObjective(Item item, int count) {
        super(ObjectiveType.ITEM, count);
        this.itemId = ForgeRegistries.ITEMS.getKey(item).toString();
        this.count = count;
    }

	@Override
    public boolean checkProgress(Object... params) {
        if (params.length > 0 && params[0] instanceof ItemStack stack) {
            Item requiredItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemId));
            if (stack.is(requiredItem)) {
                addProgress(stack.getCount());
                return isCompleted();
            }
        }
        return false;
    }
}
