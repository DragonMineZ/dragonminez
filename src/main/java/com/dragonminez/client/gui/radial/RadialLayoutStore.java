package com.dragonminez.client.gui.radial;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class RadialLayoutStore {

	private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("dragonminez").resolve("radial_layout.json");
	private static final Map<String, List<String>> ORDER = new HashMap<>();
	private static boolean loaded = false;

	private RadialLayoutStore() {
	}

	private static void ensureLoaded() {
		if (loaded) return;
		loaded = true;
		try {
			if (!Files.exists(FILE)) return;
			JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
				List<String> keys = new ArrayList<>();
				for (JsonElement element : entry.getValue().getAsJsonArray()) keys.add(element.getAsString());
				ORDER.put(entry.getKey(), keys);
			}
		} catch (Exception ignored) {
		}
	}

	public static List<String> getOrder(String categoryKey) {
		ensureLoaded();
		return ORDER.getOrDefault(categoryKey, Collections.emptyList());
	}

	public static void setOrder(String categoryKey, List<String> keys) {
		ensureLoaded();
		ORDER.put(categoryKey, new ArrayList<>(keys));
	}

	public static void save() {
		ensureLoaded();
		try {
			Files.createDirectories(FILE.getParent());
			JsonObject root = new JsonObject();
			for (Map.Entry<String, List<String>> entry : ORDER.entrySet()) {
				JsonArray array = new JsonArray();
				for (String key : entry.getValue()) array.add(key);
				root.add(entry.getKey(), array);
			}
			Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(root));
		} catch (Exception ignored) {
		}
	}

	public static List<RadialNode> applyOrder(String categoryKey, List<RadialNode> nodes) {
		List<String> order = getOrder(categoryKey);
		if (order.isEmpty()) return nodes;

		List<RadialNode> remaining = new ArrayList<>(nodes);
		List<RadialNode> result = new ArrayList<>(nodes.size());
		for (String key : order) {
			Iterator<RadialNode> it = remaining.iterator();
			while (it.hasNext()) {
				RadialNode node = it.next();
				if (key.equals(node.orderKey())) {
					result.add(node);
					it.remove();
					break;
				}
			}
		}
		result.addAll(remaining);
		return result;
	}
}
