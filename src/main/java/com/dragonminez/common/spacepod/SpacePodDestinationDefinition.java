package com.dragonminez.common.spacepod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public record SpacePodDestinationDefinition(
		String id,
		String name,
		boolean translate,
		String dimension,
		@Nullable Integer iconIndex,
		@Nullable String iconTexture,
		SpacePodUnlockExpression unlockRules
) {
	public static SpacePodDestinationDefinition fromJson(JsonObject object) {
		String id = getRequiredString(object, "id");
		String name = getRequiredString(object, "name");
		boolean translate = !object.has("translate") || object.get("translate").getAsBoolean();
		String dimension = getRequiredString(object, "dimension");

		Integer iconIndex = object.has("icon_index") && !object.get("icon_index").isJsonNull()
				? object.get("icon_index").getAsInt()
				: null;
		String iconTexture = object.has("icon_texture") && !object.get("icon_texture").isJsonNull()
				? object.get("icon_texture").getAsString()
				: null;

		if (iconIndex == null && (iconTexture == null || iconTexture.isBlank())) {
			throw new IllegalArgumentException("Destination '" + id + "' must define icon_index or icon_texture");
		}

		JsonElement unlockElement = object.get("unlock_rules");
		SpacePodUnlockExpression unlockRules = SpacePodUnlockExpression.fromJson(unlockElement);

		return new SpacePodDestinationDefinition(id, name, translate, dimension, iconIndex, iconTexture, unlockRules);
	}

	private static String getRequiredString(JsonObject object, String key) {
		if (!object.has(key) || object.get(key).isJsonNull()) {
			throw new IllegalArgumentException("Missing required field '" + key + "'");
		}
		return object.get(key).getAsString();
	}
}
