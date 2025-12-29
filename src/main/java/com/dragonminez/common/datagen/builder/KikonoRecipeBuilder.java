package com.dragonminez.common.datagen.builder;

import com.dragonminez.common.init.MainRecipes;
import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
		if(inputs.size() < 9) inputs.add(item);
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

	public void save(Consumer<FinishedRecipe> consumer, ResourceLocation id) {
		if (pattern == null || template == null) throw new IllegalStateException("Missing pattern or template");
		consumer.accept(new Result(id, this));
	}

	public static class Result implements FinishedRecipe {
		private final ResourceLocation id;
		private final KikonoRecipeBuilder builder;

		public Result(ResourceLocation id, KikonoRecipeBuilder builder) {
			this.id = id;
			this.builder = builder;
		}

		@Override
		public void serializeRecipeData(JsonObject json) {
			JsonObject patObj = new JsonObject();
			patObj.addProperty("item", ForgeRegistries.ITEMS.getKey(builder.pattern.asItem()).toString());
			json.add("pattern", patObj);

			JsonObject tempObj = new JsonObject();
			tempObj.addProperty("item", ForgeRegistries.ITEMS.getKey(builder.template.asItem()).toString());
			json.add("template", tempObj);

			for(int i=0; i<9; i++) {
				if(i < builder.inputs.size()) {
					ItemLike inputItem = builder.inputs.get(i);
					if (inputItem.asItem() != net.minecraft.world.item.Items.AIR) {
						JsonObject inObj = new JsonObject();
						inObj.addProperty("item", ForgeRegistries.ITEMS.getKey(inputItem.asItem()).toString());
						json.add("slot_" + (i + 1), inObj);
					}
				}
			}

			JsonObject out = new JsonObject();
			out.addProperty("item", ForgeRegistries.ITEMS.getKey(builder.result).toString());
			if(builder.count > 1) out.addProperty("count", builder.count);
			json.add("output", out);

			json.addProperty("crafting_time", builder.craftingTime);
			json.addProperty("energy_cost", builder.energyCost);
		}

		@Override
		public ResourceLocation getId() { return id; }
		@Override
		public RecipeSerializer<?> getType() { return MainRecipes.KIKONO_SERIALIZER.get(); }
		@Override
		public @Nullable JsonObject serializeAdvancement() { return null; }
		@Override
		public @Nullable ResourceLocation getAdvancementId() { return null; }
	}
}