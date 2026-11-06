package com.dragonminez.common.datagen.builder;

import com.dragonminez.server.recipes.PatternRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.*;

public class PatternRecipeBuilder {
	private final Item result;
	private final int count;
	private final List<String> rows = new ArrayList<>();
	private final Map<Character, Ingredient> key = new LinkedHashMap<>();
	private String group = "";

	public PatternRecipeBuilder(ItemLike result, int count) {
		this.result = result.asItem();
		this.count = count;
	}

	public static PatternRecipeBuilder shaped(ItemLike result) {
		return new PatternRecipeBuilder(result, 1);
	}

	public static PatternRecipeBuilder shaped(ItemLike result, int count) {
		return new PatternRecipeBuilder(result, count);
	}

	public PatternRecipeBuilder define(char symbol, ItemLike item) {
		return define(symbol, Ingredient.of(item));
	}

	public PatternRecipeBuilder define(char symbol, TagKey<Item> tag) {
		return define(symbol, Ingredient.of(tag));
	}

	public PatternRecipeBuilder define(char symbol, Ingredient ingredient) {
		if (symbol == ' ') throw new IllegalArgumentException("Symbol ' ' is reserved");
		if (key.containsKey(symbol)) throw new IllegalArgumentException("Symbol '" + symbol + "' is already defined");
		key.put(symbol, ingredient);
		return this;
	}

	public PatternRecipeBuilder pattern(String row) {
		if (!rows.isEmpty() && row.length() != rows.get(0).length()) {
			throw new IllegalArgumentException("Pattern rows must be the same width");
		}
		rows.add(row);
		return this;
	}

	public PatternRecipeBuilder group(String group) {
		this.group = group;
		return this;
	}

	public void save(RecipeOutput output, ResourceLocation id) {
		validate(id);
		int width = rows.getFirst().length();
		int height = rows.size();
		NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
		for (int row = 0; row < height; row++) {
			for (int column = 0; column < width; column++) {
				char symbol = rows.get(row).charAt(column);
				if (symbol != ' ') ingredients.set(row * width + column, key.get(symbol));
			}
		}
		PatternRecipe recipe = new PatternRecipe(group, width, height, ingredients, new ItemStack(result, count), true);
		output.accept(id, recipe, null);
	}

	private void validate(ResourceLocation id) {
		if (rows.isEmpty()) throw new IllegalStateException("No pattern is defined for pattern recipe " + id);
		if (rows.size() > PatternRecipe.MAX_SIZE || rows.get(0).length() > PatternRecipe.MAX_SIZE) {
			throw new IllegalStateException("Pattern recipe " + id + " exceeds " + PatternRecipe.MAX_SIZE + "x" + PatternRecipe.MAX_SIZE);
		}
		Set<Character> unused = new HashSet<>(key.keySet());
		for (String row : rows) {
			for (char c : row.toCharArray()) {
				if (c == ' ') continue;
				if (!key.containsKey(c)) throw new IllegalStateException("Pattern in recipe " + id + " uses undefined symbol '" + c + "'");
				unused.remove(c);
			}
		}
		if (!unused.isEmpty()) throw new IllegalStateException("Ingredients are defined but not used in pattern for recipe " + id + ": " + unused);
	}

}
