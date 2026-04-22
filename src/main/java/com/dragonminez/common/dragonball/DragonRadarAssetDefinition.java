
package com.dragonminez.common.dragonball;

import com.google.gson.JsonObject;

import java.util.Optional;

public class DragonRadarAssetDefinition {
	private final String id;
	private final String itemModelPath;
	private final String itemTexturePath;
	private final String radarBackgroundTexturePath;
	private final String radarDotTexturePath;
	private final String radarOverlayTexturePath;

	public DragonRadarAssetDefinition(String id, String itemModelPath, String itemTexturePath, String radarBackgroundTexturePath, String radarDotTexturePath, String radarOverlayTexturePath) {
		this.id = id;
		this.itemModelPath = blankToNull(itemModelPath);
		this.itemTexturePath = blankToNull(itemTexturePath);
		this.radarBackgroundTexturePath = blankToNull(radarBackgroundTexturePath);
		this.radarDotTexturePath = blankToNull(radarDotTexturePath);
		this.radarOverlayTexturePath = blankToNull(radarOverlayTexturePath);
	}

	public String getId() { return id; }
	public Optional<String> getItemModelPath() { return Optional.ofNullable(itemModelPath); }
	public Optional<String> getItemTexturePath() { return Optional.ofNullable(itemTexturePath); }
	public Optional<String> getRadarBackgroundTexturePath() { return Optional.ofNullable(radarBackgroundTexturePath); }
	public Optional<String> getRadarDotTexturePath() { return Optional.ofNullable(radarDotTexturePath); }
	public Optional<String> getRadarOverlayTexturePath() { return Optional.ofNullable(radarOverlayTexturePath); }

	public JsonObject toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("id", id);
		if (itemModelPath != null) root.addProperty("item_model", itemModelPath);
		if (itemTexturePath != null) root.addProperty("item_texture", itemTexturePath);
		if (radarBackgroundTexturePath != null) root.addProperty("radar_background_texture", radarBackgroundTexturePath);
		if (radarDotTexturePath != null) root.addProperty("radar_dot_texture", radarDotTexturePath);
		if (radarOverlayTexturePath != null) root.addProperty("radar_overlay_texture", radarOverlayTexturePath);
		return root;
	}

	public static DragonRadarAssetDefinition fromJson(JsonObject root) {
		return new DragonRadarAssetDefinition(
			root.get("id").getAsString(),
			root.has("item_model") ? root.get("item_model").getAsString() : null,
			root.has("item_texture") ? root.get("item_texture").getAsString() : null,
			root.has("radar_background_texture") ? root.get("radar_background_texture").getAsString() : null,
			root.has("radar_dot_texture") ? root.get("radar_dot_texture").getAsString() : null,
			root.has("radar_overlay_texture") ? root.get("radar_overlay_texture").getAsString() : null
		);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
