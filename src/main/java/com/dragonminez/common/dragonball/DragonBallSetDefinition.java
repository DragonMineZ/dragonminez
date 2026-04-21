
package com.dragonminez.common.dragonball;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntSupplier;

public class DragonBallSetDefinition {
	private final String id;
	private final Set<ResourceLocation> validDimensions;
	private final IntSupplier copiesSupplier;
	private final IntSupplier spawnRangeSupplier;
	private final int summonRadius;
	private final Map<Integer, String> blockRegistryNamesByStar;
	private final String assetDefinitionId;
	private final String displayName;
	private final Map<Integer, RegistryObject<Block>> registeredBlocksByStar = new LinkedHashMap<>();

	public DragonBallSetDefinition(String id,
			Set<ResourceLocation> validDimensions,
			IntSupplier copiesSupplier,
			IntSupplier spawnRangeSupplier,
			int summonRadius,
			Map<Integer, String> blockRegistryNamesByStar) {
		this(id, validDimensions, copiesSupplier, spawnRangeSupplier, summonRadius, blockRegistryNamesByStar, null, null);
	}

	public DragonBallSetDefinition(String id,
			Set<ResourceLocation> validDimensions,
			IntSupplier copiesSupplier,
			IntSupplier spawnRangeSupplier,
			int summonRadius,
			Map<Integer, String> blockRegistryNamesByStar,
			String assetDefinitionId,
			String displayName) {
		this.id = id;
		this.validDimensions = Set.copyOf(validDimensions);
		this.copiesSupplier = copiesSupplier;
		this.spawnRangeSupplier = spawnRangeSupplier;
		this.summonRadius = summonRadius;
		this.blockRegistryNamesByStar = Map.copyOf(blockRegistryNamesByStar);
		this.assetDefinitionId = assetDefinitionId == null || assetDefinitionId.isBlank() ? null : assetDefinitionId;
		this.displayName = displayName == null || displayName.isBlank() ? null : displayName;
	}

	public String getId() { return id; }
	public Set<ResourceLocation> getValidDimensions() { return validDimensions; }
	public boolean supportsDimension(ResourceKey<Level> dimension) { return validDimensions.contains(dimension.location()); }
	public int getCopies() { return Math.max(1, copiesSupplier.getAsInt()); }
	public int getSpawnRange() { return Math.max(1, spawnRangeSupplier.getAsInt()); }
	public int getSummonRadius() { return summonRadius; }
	public Map<Integer, String> getBlockRegistryNamesByStar() { return Collections.unmodifiableMap(blockRegistryNamesByStar); }
	public Set<Integer> getStars() { return Collections.unmodifiableSet(new LinkedHashSet<>(blockRegistryNamesByStar.keySet())); }
	public String getBlockRegistryNameForStar(int star) { return blockRegistryNamesByStar.get(star); }
	public Optional<String> getAssetDefinitionId() { return Optional.ofNullable(assetDefinitionId); }
	public Optional<String> getDisplayName() { return Optional.ofNullable(displayName); }
	public DragonBallSetAssetDefinition resolveAssetDefinition() { return assetDefinitionId == null ? null : DragonBallDefinitions.getBallSetAsset(assetDefinitionId); }
	public void setRegisteredBlock(int star, RegistryObject<Block> block) { registeredBlocksByStar.put(star, block); }
	public RegistryObject<Block> getRegisteredBlockObjectForStar(int star) { return registeredBlocksByStar.get(star); }
	public Block getBlockForStar(int star) { RegistryObject<Block> registryObject = registeredBlocksByStar.get(star); return registryObject == null ? null : registryObject.get(); }
	public Integer getStarForBlock(Block block) {
		for (Map.Entry<Integer, RegistryObject<Block>> entry : registeredBlocksByStar.entrySet()) {
			if (entry.getValue().get() == block) return entry.getKey();
		}
		return null;
	}

	public JsonObject toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("id", id);
		JsonArray dimensions = new JsonArray();
		for (ResourceLocation dimension : validDimensions) dimensions.add(dimension.toString());
		root.add("dimensions", dimensions);
		root.addProperty("copies", getCopies());
		root.addProperty("spawn_range", getSpawnRange());
		root.addProperty("summon_radius", summonRadius);
		if (assetDefinitionId != null) root.addProperty("asset_definition", assetDefinitionId);
		if (displayName != null) root.addProperty("display_name", displayName);
		JsonObject blocks = new JsonObject();
		blockRegistryNamesByStar.entrySet().stream().sorted(Map.Entry.comparingByKey())
			.forEach(entry -> blocks.addProperty(String.valueOf(entry.getKey()), entry.getValue()));
		root.add("blocks", blocks);
		return root;
	}

	public static DragonBallSetDefinition fromJson(JsonObject root) {
		String id = root.get("id").getAsString();
		Set<ResourceLocation> dimensions = new java.util.LinkedHashSet<>();
		for (JsonElement element : root.getAsJsonArray("dimensions")) dimensions.add(ResourceLocation.parse(element.getAsString()));
		int copies = root.has("copies") ? root.get("copies").getAsInt() : 5;
		int spawnRange = root.get("spawn_range").getAsInt();
		int summonRadius = root.get("summon_radius").getAsInt();
		Map<Integer, String> blockRegistryNamesByStar = new LinkedHashMap<>();
		JsonObject blocks = root.getAsJsonObject("blocks");
		for (String key : blocks.keySet()) blockRegistryNamesByStar.put(Integer.parseInt(key), blocks.get(key).getAsString());
		String assetDefinitionId = root.has("asset_definition") ? root.get("asset_definition").getAsString() : null;
		String displayName = root.has("display_name") ? root.get("display_name").getAsString() : null;
		return new DragonBallSetDefinition(id, dimensions, () -> copies, () -> spawnRange, summonRadius, blockRegistryNamesByStar, assetDefinitionId, displayName);
	}
}
