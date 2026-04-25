
package com.dragonminez.common.dragonball;

import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import java.util.function.Consumer;

public abstract class DragonRadarRecipeDefinition {
	private final String id;

	protected DragonRadarRecipeDefinition(String id) {
		this.id = id;
	}

	public String getId() { return id; }
	public abstract void buildRecipes(Consumer<FinishedRecipe> output, DragonRadarDefinition radarDefinition);
	protected abstract void writeTypeSpecificJson(JsonObject root);

	public final JsonObject toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("id", id);
		writeTypeSpecificJson(root);
		return root;
	}

	public static DragonRadarRecipeDefinition fromJson(JsonObject root) {
		String type = root.get("type").getAsString();
		if ("shaped".equals(type)) return ShapedDragonRadarRecipeDefinition.fromJson(root);
		throw new IllegalArgumentException("Unsupported dragon radar recipe type: " + type);
	}
}
