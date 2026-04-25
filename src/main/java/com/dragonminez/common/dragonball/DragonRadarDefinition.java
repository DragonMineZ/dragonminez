package com.dragonminez.common.dragonball;

import com.dragonminez.Reference;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class DragonRadarDefinition {
	private final String id;
	private final String itemRegistryName;
	private final Set<ResourceLocation> validDimensions;
	private final String ballSetId;
	private final String tooltipKey;
	private final int[] ranges;
	private final String recipeDefinitionId;
	private final String assetDefinitionId;
	private final String chipItemId;
	private final String cpuItemId;
	private final String chipModelItemId;
	private final String cpuModelItemId;
	private final String displayName;

	public DragonRadarDefinition(String id, String itemRegistryName, Set<ResourceLocation> validDimensions, String ballSetId, String tooltipKey, int[] ranges) {
		this(id, itemRegistryName, validDimensions, ballSetId, tooltipKey, ranges, null, null, null, null, null, null, null);
	}

	public DragonRadarDefinition(String id, String itemRegistryName, Set<ResourceLocation> validDimensions, String ballSetId, String tooltipKey, int[] ranges, String recipeDefinitionId, String assetDefinitionId) {
		this(id, itemRegistryName, validDimensions, ballSetId, tooltipKey, ranges, recipeDefinitionId, assetDefinitionId, null, null, null, null, null);
	}

	public DragonRadarDefinition(String id, String itemRegistryName, Set<ResourceLocation> validDimensions, String ballSetId, String tooltipKey, int[] ranges, String recipeDefinitionId, String assetDefinitionId, String displayName) {
		this(id, itemRegistryName, validDimensions, ballSetId, tooltipKey, ranges, recipeDefinitionId, assetDefinitionId, null, null, null, null, displayName);
	}

	public DragonRadarDefinition(String id, String itemRegistryName, Set<ResourceLocation> validDimensions, String ballSetId, String tooltipKey, int[] ranges, String recipeDefinitionId, String assetDefinitionId, String chipItemId, String cpuItemId, String displayName) {
		this(id, itemRegistryName, validDimensions, ballSetId, tooltipKey, ranges, recipeDefinitionId, assetDefinitionId, chipItemId, cpuItemId, null, null, displayName);
	}

	public DragonRadarDefinition(String id, String itemRegistryName, Set<ResourceLocation> validDimensions, String ballSetId, String tooltipKey, int[] ranges, String recipeDefinitionId, String assetDefinitionId, String chipItemId, String cpuItemId, String chipModelItemId, String cpuModelItemId, String displayName) {
		this.id = id;
		this.itemRegistryName = itemRegistryName;
		this.validDimensions = Set.copyOf(validDimensions);
		this.ballSetId = ballSetId == null || ballSetId.isBlank() ? null : ballSetId;
		this.tooltipKey = tooltipKey;
		this.ranges = ranges == null ? new int[0] : ranges.clone();
		this.recipeDefinitionId = recipeDefinitionId == null || recipeDefinitionId.isBlank() ? null : recipeDefinitionId;
		this.assetDefinitionId = assetDefinitionId == null || assetDefinitionId.isBlank() ? null : assetDefinitionId;
		this.chipItemId = chipItemId == null || chipItemId.isBlank() ? null : chipItemId;
		this.cpuItemId = cpuItemId == null || cpuItemId.isBlank() ? null : cpuItemId;
		this.chipModelItemId = chipModelItemId == null || chipModelItemId.isBlank() ? null : chipModelItemId;
		this.cpuModelItemId = cpuModelItemId == null || cpuModelItemId.isBlank() ? null : cpuModelItemId;
		this.displayName = displayName == null || displayName.isBlank() ? null : displayName;
	}

	public String getId() { return id; }
	public String getItemRegistryName() { return itemRegistryName; }
	public Set<ResourceLocation> getValidDimensions() { return validDimensions; }
	public boolean supportsDimension(ResourceKey<Level> dimension) { return validDimensions.contains(dimension.location()); }
	public boolean supportsBallSet(String setId) { return ballSetId != null && ballSetId.equals(setId); }
	public String getBallSetId() { return ballSetId; }
	public Set<String> getValidBallSetIds() { return ballSetId == null ? Set.of() : Set.of(ballSetId); }
	public String getTooltipKey() { return tooltipKey; }
	public int[] getRanges() { return ranges.clone(); }
	public Optional<String> getRecipeDefinitionId() { return Optional.ofNullable(recipeDefinitionId); }
	public Optional<String> getChipItemId() { return Optional.ofNullable(chipItemId); }
	public Optional<String> getCpuItemId() { return Optional.ofNullable(cpuItemId); }
	public Optional<String> getChipModelItemId() { return Optional.ofNullable(chipModelItemId); }
	public Optional<String> getCpuModelItemId() { return Optional.ofNullable(cpuModelItemId); }
	public Optional<String> getChipRegistryName() { return itemIdToRegistryName(chipItemId); }
	public Optional<String> getCpuRegistryName() { return itemIdToRegistryName(cpuItemId); }
	public DragonRadarRecipeDefinition resolveRecipeDefinition() {
		if (recipeDefinitionId != null) return DragonBallDefinitions.getRadarRecipe(recipeDefinitionId);
		if (chipItemId != null && cpuItemId != null) return new ShapedDragonRadarRecipeDefinition(id + "_auto_recipe", chipItemId, cpuItemId);
		return null;
	}
	public Optional<String> getAssetDefinitionId() { return Optional.ofNullable(assetDefinitionId); }
	public Optional<String> getDisplayName() { return Optional.ofNullable(displayName); }
	public DragonRadarAssetDefinition resolveAssetDefinition() { return assetDefinitionId == null ? null : DragonBallDefinitions.getRadarAsset(assetDefinitionId); }

	public JsonObject toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("id", id);
		root.addProperty("item_registry_name", itemRegistryName);
		JsonArray dimensions = new JsonArray();
		for (ResourceLocation dimension : validDimensions) dimensions.add(dimension.toString());
		root.add("dimensions", dimensions);
		if (ballSetId != null) root.addProperty("ball_set", ballSetId);
		root.addProperty("tooltip_key", tooltipKey);
		JsonArray rangesArray = new JsonArray();
		for (int range : ranges) rangesArray.add(range);
		root.add("ranges", rangesArray);
		if (recipeDefinitionId != null) root.addProperty("recipe_definition", recipeDefinitionId);
		if (assetDefinitionId != null) root.addProperty("asset_definition", assetDefinitionId);
		if (chipItemId != null) root.addProperty("chip_item", chipItemId);
		if (cpuItemId != null) root.addProperty("cpu_item", cpuItemId);
		if (chipModelItemId != null) root.addProperty("chip_model_item", chipModelItemId);
		if (cpuModelItemId != null) root.addProperty("cpu_model_item", cpuModelItemId);
		if (displayName != null) root.addProperty("display_name", displayName);
		return root;
	}

	private Optional<String> itemIdToRegistryName(String itemId) {
		if (itemId == null || itemId.isBlank()) return Optional.empty();

		ResourceLocation rl = ResourceLocation.tryParse(itemId);
		if (rl == null) return Optional.empty();

		if (!Reference.MOD_ID.equals(rl.getNamespace())) return Optional.empty();

		return Optional.of(rl.getPath());
	}

	public static DragonRadarDefinition fromJson(JsonObject root) {
		String id = root.get("id").getAsString();
		String itemRegistryName = root.get("item_registry_name").getAsString();
		Set<ResourceLocation> dimensions = new LinkedHashSet<>();
		for (JsonElement element : root.getAsJsonArray("dimensions")) dimensions.add(ResourceLocation.parse(element.getAsString()));

		String ballSetId = root.get("ball_set").getAsString();

		String tooltipKey = root.get("tooltip_key").getAsString();
		JsonArray rangesArray = root.getAsJsonArray("ranges");
		int[] ranges = new int[rangesArray.size()];
		for (int i = 0; i < rangesArray.size(); i++) ranges[i] = rangesArray.get(i).getAsInt();
		String recipeDefinitionId = root.has("recipe_definition") ? root.get("recipe_definition").getAsString() : null;
		String assetDefinitionId = root.has("asset_definition") ? root.get("asset_definition").getAsString() : null;
		String chipItemId = root.has("chip_item") ? root.get("chip_item").getAsString() : null;
		String cpuItemId = root.has("cpu_item") ? root.get("cpu_item").getAsString() : null;
		String chipModelItemId = root.has("chip_model_item") ? root.get("chip_model_item").getAsString() : null;
		String cpuModelItemId = root.has("cpu_model_item") ? root.get("cpu_model_item").getAsString() : null;
		String displayName = root.has("display_name") ? root.get("display_name").getAsString() : null;
		return new DragonRadarDefinition(id, itemRegistryName, dimensions, ballSetId, tooltipKey, ranges, recipeDefinitionId, assetDefinitionId, chipItemId, cpuItemId, chipModelItemId, cpuModelItemId, displayName);
	}
}
