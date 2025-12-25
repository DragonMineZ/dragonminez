package com.dragonminez.common.init.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

public class FoodItem extends Item {
    public FoodItem(int hunger, float saturation) {
        super(new Properties().stacksTo(32).food(
                new FoodProperties.Builder()
                        .nutrition(hunger)
                        .saturationMod(saturation)
                        .meat()
                        .alwaysEat()
                        .build()
        ));
    }
}
