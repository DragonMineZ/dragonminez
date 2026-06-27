
package com.dragonminez.common.dragonball;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainItems;
import com.google.gson.JsonObject;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.FinishedRecipe;
import java.util.function.Consumer;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public class ShapedDragonRadarRecipeDefinition extends DragonRadarRecipeDefinition {
	private final String chipItemId;
	private final String cpuItemId;

	public ShapedDragonRadarRecipeDefinition(String id, String chipItemId, String cpuItemId) {
		super(id);
		this.chipItemId = chipItemId;
		this.cpuItemId = cpuItemId;
	}

	public String getChipItemId() { return chipItemId; }
	public String getCpuItemId() { return cpuItemId; }

	@Override
	public void buildRecipes(Consumer<FinishedRecipe> output, DragonRadarDefinition radarDefinition) {
		Item chipItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(chipItemId));
		Item cpuItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(cpuItemId));
		if (chipItem == null || cpuItem == null) throw new IllegalStateException("Missing radar recipe ingredient for radar '" + radarDefinition.getId() + "'");
		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, MainItems.getDragonRadarItemOrThrow(radarDefinition.getId()).get(), 1)
			.pattern("OCO")
			.pattern("PGP")
			.pattern("CPC")
			.define('O', Items.OBSERVER)
			.define('G', cpuItem)
			.define('C', chipItem)
			.define('P', MainItems.RADAR_PIECE.get())
			.unlockedBy(getHasName(chipItem), has(chipItem))
			.group(Reference.MOD_ID)
			.save(output);
	}

	@Override
	protected void writeTypeSpecificJson(JsonObject root) {
		root.addProperty("type", "shaped");
		root.addProperty("chip_item", chipItemId);
		root.addProperty("cpu_item", cpuItemId);
	}


	private static String getHasName(Item item) {
		ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
		return "has_" + (key == null ? "item" : key.getPath());
	}

	private static InventoryChangeTrigger.TriggerInstance has(Item item) {
		return InventoryChangeTrigger.TriggerInstance.hasItems(item);
	}

	public static ShapedDragonRadarRecipeDefinition fromJson(JsonObject root) {
		String id = root.has("id") ? root.get("id").getAsString() : "generated";
		return new ShapedDragonRadarRecipeDefinition(id, root.get("chip_item").getAsString(), root.get("cpu_item").getAsString());
	}
}
