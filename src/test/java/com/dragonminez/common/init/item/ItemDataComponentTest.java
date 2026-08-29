package com.dragonminez.common.init.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemDataComponentTest {
    @Test
    void customDataSurvivesItemStackCopy() {
        ItemStack stack = new ItemStack(Items.STONE);

        WeightItem.setWeight(stack, 275);
        PothalaPairItem.setPairId(stack, 0x5A17C0DE);

        ItemStack copy = stack.copy();
        assertEquals(275, WeightItem.getWeight(copy));
        assertEquals(0x5A17C0DE, PothalaPairItem.getPairId(copy));
    }
}
