
package com.dragonminez.common.dragonball;

import com.google.gson.JsonObject;

import java.util.Optional;

public class DragonAssetDefinition {
	private final String id;
	private final String rendererType;
	private final String modelPath;
	private final String texturePath;
	private final String animationPath;

	public DragonAssetDefinition(String id, String rendererType, String modelPath, String texturePath, String animationPath) {
		this.id = id;
		this.rendererType = blankToNull(rendererType);
		this.modelPath = blankToNull(modelPath);
		this.texturePath = blankToNull(texturePath);
		this.animationPath = blankToNull(animationPath);
	}

	public String getId() { return id; }
	public Optional<String> getRendererType() { return Optional.ofNullable(rendererType); }
	public Optional<String> getModelPath() { return Optional.ofNullable(modelPath); }
	public Optional<String> getTexturePath() { return Optional.ofNullable(texturePath); }
	public Optional<String> getAnimationPath() { return Optional.ofNullable(animationPath); }

	public JsonObject toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("id", id);
		if (rendererType != null) root.addProperty("renderer_type", rendererType);
		if (modelPath != null) root.addProperty("model", modelPath);
		if (texturePath != null) root.addProperty("texture", texturePath);
		if (animationPath != null) root.addProperty("animation", animationPath);
		return root;
	}

	public static DragonAssetDefinition fromJson(JsonObject root) {
		return new DragonAssetDefinition(
			root.get("id").getAsString(),
			root.has("renderer_type") ? root.get("renderer_type").getAsString() : null,
			root.has("model") ? root.get("model").getAsString() : null,
			root.has("texture") ? root.get("texture").getAsString() : null,
			root.has("animation") ? root.get("animation").getAsString() : null
		);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
