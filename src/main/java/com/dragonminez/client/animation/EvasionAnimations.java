package com.dragonminez.client.animation;

import com.dragonminez.Reference;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public final class EvasionAnimations {
	private static final ResourceLocation ANIMATION_FILE =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/races/evs.animation.json");
	private static final String DEFAULT_CLIP = "evs.default";

	private static final Set<String> AVAILABLE = new HashSet<>();
	private static boolean loaded = false;

	private EvasionAnimations() {}

	public static void reload(ResourceManager resourceManager) {
		AVAILABLE.clear();
		loaded = true;
		try {
			var resourceOptional = resourceManager.getResource(ANIMATION_FILE);
			if (resourceOptional.isEmpty()) return;

			try (var stream = resourceOptional.get().open();
				 var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
				JsonObject animations = root.getAsJsonObject("animations");
				if (animations == null) return;
				for (String key : animations.keySet()) AVAILABLE.add(key);
			}
		} catch (Exception ignored) {}
	}

	private static void ensureLoaded() {
		if (loaded) return;
		var minecraft = Minecraft.getInstance();
		if (minecraft != null) reload(minecraft.getResourceManager());
	}

	public static String resolve(String candidate) {
		ensureLoaded();
		return AVAILABLE.contains(candidate) ? candidate : DEFAULT_CLIP;
	}
}
