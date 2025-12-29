package com.dragonminez.server.recipes;

import com.dragonminez.common.init.MainRecipes;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class KikonoRecipe implements Recipe<SimpleContainer> {
	private final ResourceLocation id;
	private final ItemStack output;
	private final NonNullList<Ingredient> recipeItems;
	private final Ingredient pattern;
	private final Ingredient template;
	private final int craftingTime;
	private final int energyCost;

	public KikonoRecipe(ResourceLocation id, ItemStack output, NonNullList<Ingredient> recipeItems, Ingredient pattern, Ingredient template, int craftingTime, int energyCost) {
		this.id = id;
		this.output = output;
		this.recipeItems = recipeItems;
		this.pattern = pattern;
		this.template = template;
		this.craftingTime = craftingTime;
		this.energyCost = energyCost;
	}


	@Override
	public boolean matches(SimpleContainer pContainer, Level pLevel) {
		if(pLevel.isClientSide()) return false;
		if (!pattern.test(pContainer.getItem(9))) return false;
		if (!template.test(pContainer.getItem(10))) return false;
		for (int i = 0; i < recipeItems.size(); i++) {
			if (!recipeItems.get(i).test(pContainer.getItem(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack assemble(SimpleContainer pContainer, RegistryAccess pRegistryAccess) {
		return output.copy();
	}

	@Override
	public boolean canCraftInDimensions(int pWidth, int pHeight) {
		return true;
	}

	@Override
	public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
		return output.copy();
	}

	@Override
	public ResourceLocation getId() {
		return id;
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
		NonNullList<Ingredient> allIngredients = NonNullList.create();
		allIngredients.addAll(recipeItems);
		allIngredients.add(pattern);
		allIngredients.add(template);
		return allIngredients;
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

		@Override
		public KikonoRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
			ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "output"));
			Ingredient pattern = Ingredient.fromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "pattern"));
			Ingredient template = Ingredient.fromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "template"));
			int time = GsonHelper.getAsInt(pSerializedRecipe, "crafting_time", 100);
			int energy = GsonHelper.getAsInt(pSerializedRecipe, "energy_cost", 1000);

			NonNullList<Ingredient> inputs = NonNullList.withSize(9, Ingredient.EMPTY);
			for (int i = 0; i < 9; i++) {
				if (pSerializedRecipe.has("slot_" + (i + 1))) {
					inputs.set(i, Ingredient.fromJson(pSerializedRecipe.get("slot_" + (i + 1))));
				}
			}

			return new KikonoRecipe(pRecipeId, output, inputs, pattern, template, time, energy);
		}

		@Override
		public @Nullable KikonoRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
			ItemStack output = pBuffer.readItem();
			Ingredient pattern = Ingredient.fromNetwork(pBuffer);
			Ingredient template = Ingredient.fromNetwork(pBuffer);
			int time = pBuffer.readInt();
			int energy = pBuffer.readInt();

			NonNullList<Ingredient> inputs = NonNullList.withSize(9, Ingredient.EMPTY);
			for (int i = 0; i < 9; i++) {
				inputs.set(i, Ingredient.fromNetwork(pBuffer));
			}

			return new KikonoRecipe(pRecipeId, output, inputs, pattern, template, time, energy);
		}

		@Override
		public void toNetwork(FriendlyByteBuf pBuffer, KikonoRecipe pRecipe) {
			pBuffer.writeItemStack(pRecipe.output, false);
			pRecipe.pattern.toNetwork(pBuffer);
			pRecipe.template.toNetwork(pBuffer);
			pBuffer.writeInt(pRecipe.craftingTime);
			pBuffer.writeInt(pRecipe.energyCost);

			for (Ingredient ing : pRecipe.recipeItems) {
				ing.toNetwork(pBuffer);
			}
		}
	}
}