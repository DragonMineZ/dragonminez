package com.dragonminez.common.dragonball;

import com.google.gson.JsonObject;

import java.util.Optional;

public class DragonBallSetAssetDefinition {
	private final String id;
	private final String flatTexturePrefixPath;
	private final String inventoryTextureFormat;
	private final String geoModelPath;
	private final String animationPath;
	private final String geoTexturePrefixPath;
	private final String blockModelTemplatePath;
	private final String itemModelTemplatePath;

	public DragonBallSetAssetDefinition(String id, String flatTexturePrefixPath, String inventoryTextureFormat, String geoModelPath, String animationPath, String geoTexturePrefixPath, String blockModelTemplatePath, String itemModelTemplatePath) {
		this.id = id;
		this.flatTexturePrefixPath = blankToNull(flatTexturePrefixPath);
		this.inventoryTextureFormat = blankToNull(inventoryTextureFormat);
		this.geoModelPath = blankToNull(geoModelPath);
		this.animationPath = blankToNull(animationPath);
		this.geoTexturePrefixPath = blankToNull(geoTexturePrefixPath);
		this.blockModelTemplatePath = blankToNull(blockModelTemplatePath);
		this.itemModelTemplatePath = blankToNull(itemModelTemplatePath);
	}

	public String getId() { return id; }
	public Optional<String> getFlatTexturePrefixPath() { return Optional.ofNullable(flatTexturePrefixPath); }
	public Optional<String> getInventoryTextureFormat() { return Optional.ofNullable(inventoryTextureFormat); }
	public Optional<String> getGeoModelPath() { return Optional.ofNullable(geoModelPath); }
	public Optional<String> getAnimationPath() { return Optional.ofNullable(animationPath); }
	public Optional<String> getGeoTexturePrefixPath() { return Optional.ofNullable(geoTexturePrefixPath); }
	public Optional<String> getBlockModelTemplatePath() { return Optional.ofNullable(blockModelTemplatePath); }
	public Optional<String> getItemModelTemplatePath() { return Optional.ofNullable(itemModelTemplatePath); }

	public Optional<String> getFlatTexturePathForStar(int star) {
		return flatTexturePrefixPath == null ? Optional.empty() : Optional.of(flatTexturePrefixPath + star);
	}

	public Optional<String> getInventoryTexturePathForStar(int star) {
		return inventoryTextureFormat == null ? Optional.empty() : Optional.of(String.format(inventoryTextureFormat, star));
	}

	public Optional<String> getGeoTexturePathForStar(int star) {
		return geoTexturePrefixPath == null ? Optional.empty() : Optional.of(geoTexturePrefixPath + star + ".png");
	}

	public JsonObject toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("id", id);
		if (flatTexturePrefixPath != null) root.addProperty("flat_texture_prefix", flatTexturePrefixPath);
		if (inventoryTextureFormat != null) root.addProperty("inventory_texture_format", inventoryTextureFormat);
		if (geoModelPath != null) root.addProperty("geo_model", geoModelPath);
		if (animationPath != null) root.addProperty("animation", animationPath);
		if (geoTexturePrefixPath != null) root.addProperty("geo_texture_prefix", geoTexturePrefixPath);
		if (blockModelTemplatePath != null) root.addProperty("block_model_template", blockModelTemplatePath);
		if (itemModelTemplatePath != null) root.addProperty("item_model_template", itemModelTemplatePath);
		return root;
	}

	public static DragonBallSetAssetDefinition fromJson(JsonObject root) {
		String inventoryFormat = root.has("inventory_texture_format") ? root.get("inventory_texture_format").getAsString() : null;
		return new DragonBallSetAssetDefinition(
			root.get("id").getAsString(),
			root.has("flat_texture_prefix") ? root.get("flat_texture_prefix").getAsString() : null,
			inventoryFormat,
			root.has("geo_model") ? root.get("geo_model").getAsString() : null,
			root.has("animation") ? root.get("animation").getAsString() : null,
			root.has("geo_texture_prefix") ? root.get("geo_texture_prefix").getAsString() : null,
			root.has("block_model_template") ? root.get("block_model_template").getAsString() : null,
			root.has("item_model_template") ? root.get("item_model_template").getAsString() : null
		);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
