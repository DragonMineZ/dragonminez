package com.dragonminez.server.recipes;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** 11-slot crafting layout: 0-8 grid, 9 pattern, 10 template. */
public record KikonoRecipeInput(NonNullList<ItemStack> items) implements RecipeInput {
	public KikonoRecipeInput {
		if (items.size() < 11) {
			throw new IllegalArgumentException("KikonoRecipeInput requires 11 slots");
		}
	}

	@Override
	public ItemStack getItem(int index) {
		return items.get(index);
	}

	@Override
	public int size() {
		return items.size();
	}
}
