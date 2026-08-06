package com.dragonminez.common.datagen.builder;

import com.dragonminez.server.recipes.KikonoRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

public class KikonoRecipeBuilder {
	private final Item result;
	private final int count;
	private final List<ItemLike> inputs = new ArrayList<>();
	private ItemLike template;
	private ItemLike pattern;
	private int craftingTime = 100;
	private int energyCost = 1000;

	public KikonoRecipeBuilder(ItemLike result, int count) {
		this.result = result.asItem();
		this.count = count;
	}

	public static KikonoRecipeBuilder kikonize(ItemLike result) {
		return new KikonoRecipeBuilder(result, 1);
	}

	public KikonoRecipeBuilder pattern(ItemLike item) {
		this.pattern = item;
		return this;
	}

	public KikonoRecipeBuilder template(ItemLike item) {
		this.template = item;
		return this;
	}

	public KikonoRecipeBuilder input(ItemLike item) {
		if (inputs.size() < 9) inputs.add(item);
		return this;
	}

	public KikonoRecipeBuilder time(int ticks) {
		this.craftingTime = ticks;
		return this;
	}

	public KikonoRecipeBuilder energy(int fe) {
		this.energyCost = fe;
		return this;
	}

	public void save(RecipeOutput output, ResourceLocation id) {
		if (pattern == null || template == null) throw new IllegalStateException("Missing pattern or template");

		NonNullList<Ingredient> slots = NonNullList.withSize(9, Ingredient.EMPTY);
		for (int i = 0; i < inputs.size() && i < 9; i++) {
			ItemLike inputItem = inputs.get(i);
			if (inputItem.asItem() != net.minecraft.world.item.Items.AIR) {
				slots.set(i, Ingredient.of(inputItem));
			}
		}

		ItemStack resultStack = new ItemStack(result, count);
		KikonoRecipe recipe = new KikonoRecipe(
				resultStack,
				slots,
				Ingredient.of(pattern),
				Ingredient.of(template),
				craftingTime,
				energyCost
		);
		output.accept(id, recipe, null);
	}
}
