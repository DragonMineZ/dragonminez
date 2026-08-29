package com.dragonminez.server.recipes;

import com.dragonminez.common.init.MainRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class KikonoRecipe implements Recipe<KikonoRecipeInput> {
	private final ItemStack output;
	private final NonNullList<Ingredient> recipeItems;
	private final Ingredient pattern;
	private final Ingredient template;
	private final int craftingTime;
	private final int energyCost;

	public KikonoRecipe(ItemStack output, NonNullList<Ingredient> recipeItems, Ingredient pattern, Ingredient template, int craftingTime, int energyCost) {
		this.output = output;
		this.recipeItems = recipeItems;
		this.pattern = pattern;
		this.template = template;
		this.craftingTime = craftingTime;
		this.energyCost = energyCost;
	}

	@Override
	public boolean matches(KikonoRecipeInput input, Level level) {
		if (level.isClientSide()) return false;
		if (!pattern.test(input.getItem(9))) return false;
		if (!template.test(input.getItem(10))) return false;
		for (int i = 0; i < recipeItems.size(); i++) {
			if (!recipeItems.get(i).test(input.getItem(i))) return false;
		}
		return true;
	}

	@Override
	public ItemStack assemble(KikonoRecipeInput input, HolderLookup.Provider registries) {
		return output.copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return output.copy();
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return MainRecipes.KIKONO_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return MainRecipes.KIKONO_TYPE.get();
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		NonNullList<Ingredient> all = NonNullList.create();
		all.addAll(recipeItems);
		all.add(pattern);
		all.add(template);
		return all;
	}

	public NonNullList<Ingredient> getInputs() {
		return this.recipeItems;
	}

	public Ingredient getPattern() {
		return this.pattern;
	}

	public Ingredient getTemplate() {
		return this.template;
	}

	public int getEnergyCost() {
		return this.energyCost;
	}

	public int getCraftingTime() {
		return this.craftingTime;
	}

	public static class Serializer implements RecipeSerializer<KikonoRecipe> {
		public static final Serializer INSTANCE = new Serializer();

		private static final MapCodec<KikonoRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
				ItemStack.CODEC.fieldOf("output").forGetter(r -> r.output),
				Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(r -> List.copyOf(r.recipeItems)),
				Ingredient.CODEC.fieldOf("pattern").forGetter(r -> r.pattern),
				Ingredient.CODEC.fieldOf("template").forGetter(r -> r.template),
				Codec.INT.optionalFieldOf("crafting_time", 100).forGetter(r -> r.craftingTime),
				Codec.INT.optionalFieldOf("energy_cost", 1000).forGetter(r -> r.energyCost)
		).apply(inst, (output, ingredients, pattern, template, time, energy) -> {
			NonNullList<Ingredient> list = NonNullList.withSize(9, Ingredient.EMPTY);
			for (int i = 0; i < Math.min(9, ingredients.size()); i++) {
				list.set(i, ingredients.get(i));
			}
			return new KikonoRecipe(output, list, pattern, template, time, energy);
		}));

		// Support legacy slot_1..slot_9 JSON by also accepting a custom codec path via xmap on full object is hard;
		// keep primary codec as ingredients list. Legacy recipes should be datafixed separately if needed.

		private static final StreamCodec<RegistryFriendlyByteBuf, KikonoRecipe> STREAM_CODEC = StreamCodec.composite(
				ItemStack.STREAM_CODEC, r -> r.output,
				Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), r -> new ArrayList<>(r.recipeItems),
				Ingredient.CONTENTS_STREAM_CODEC, r -> r.pattern,
				Ingredient.CONTENTS_STREAM_CODEC, r -> r.template,
				ByteBufCodecs.VAR_INT, r -> r.craftingTime,
				ByteBufCodecs.VAR_INT, r -> r.energyCost,
				(output, ingredients, pattern, template, time, energy) -> {
					NonNullList<Ingredient> list = NonNullList.withSize(9, Ingredient.EMPTY);
					for (int i = 0; i < Math.min(9, ingredients.size()); i++) list.set(i, ingredients.get(i));
					return new KikonoRecipe(output, list, pattern, template, time, energy);
				}
		);

		@Override
		public MapCodec<KikonoRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, KikonoRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
