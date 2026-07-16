package com.dragonminez.common.wish;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.datagen.DMZDragonWishProvider;
import com.dragonminez.common.diagnostics.JsonKeys;
import com.dragonminez.common.diagnostics.JsonLoadReport;
import com.dragonminez.common.diagnostics.JsonSchema;
import com.dragonminez.common.util.gson.GsonUtils;
import com.dragonminez.common.util.gson.WishTypeAdapter;
import com.dragonminez.common.wish.wishes.*;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WishManager {
	public static void init() {}

	public static void loadWishes(MinecraftServer server) {
		if (server == null) {
			LogUtil.warn(Env.COMMON, "Cannot load wishes: server is null");
			return;
		}

		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		if (overworld == null) {
			LogUtil.warn(Env.COMMON, "Cannot load wishes: overworld is null");
			return;
		}

		JsonLoadReport.clear("wishes");
		Path worldFolder = overworld.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
		Path dragonminezFolder = worldFolder.resolve("dragonminez");
		Path wishDir = dragonminezFolder.resolve("wishes");

		Map<String, List<Wish>> merged = new LinkedHashMap<>(DragonWishRegistry.getServerWishes());
		try {
			if (!Files.exists(wishDir)) {
				Files.createDirectories(wishDir);
			}

			try (var stream = Files.list(wishDir)) {
				List<String> filenames = stream.map(path -> path.getFileName().toString()).toList();
				if (!filenames.contains("shenron.json")) {
					createDefaultShenronWishes(wishDir);
				}
				if (!filenames.contains("porunga.json")) {
					createDefaultPorungaWishes(wishDir);
				}
			}

			try (var stream = Files.list(wishDir)) {
				stream.filter(path -> path.getFileName().toString().endsWith(".json"))
						.sorted()
						.forEach(path -> loadWishConfig(path, merged));
			}
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to load wishes", e);
		}

		DragonWishRegistry.setServerWishes(merged);
		LogUtil.info(Env.COMMON, "Loaded {} dragon wish list(s) after merging datapacks and config files", merged.size());
	}

	private static void loadWishConfig(Path path, Map<String, List<Wish>> merged) {
		try {
			JsonArray rootArray = GsonUtils.GSON.fromJson(Files.readString(path), JsonArray.class);
			List<Wish> wishes = new ArrayList<>();
			for (JsonElement element : rootArray) {
				validateWish("wishes/" + path.getFileName(), element);
				wishes.add(GsonUtils.GSON.fromJson(element, Wish.class));
			}
			String dragonId = path.getFileName().toString().replace(".json", "");
			merged.put(dragonId, List.copyOf(wishes));
			LogUtil.info(Env.COMMON, "Loaded dragon wishes from config file {}", path.getFileName());
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Failed to load dragon wish config '{}': {}", path.getFileName(), e.toString());
			JsonLoadReport.error("wishes", "wishes/" + path.getFileName(), "Malformed wish JSON, file skipped: " + JsonLoadReport.rootCause(e));
		}
	}

	private static void validateWish(String file, JsonElement element) {
		if (element == null || !element.isJsonObject()) return;
		JsonObject obj = element.getAsJsonObject();
		String type = obj.has("type") && !obj.get("type").isJsonNull() ? obj.get("type").getAsString() : null;
		Class<? extends Wish> target = WishTypeAdapter.classForType(type);
		if (target == null) {
			JsonKeys.reportBadType("wishes", file, "wish", type);
		} else {
			JsonSchema.check("wishes", file, "wish", obj, target);
		}
	}

	private static void createDefaultShenronWishes(Path wishDir) {
		createDefaultDragonWishes(wishDir, "Shenron", DMZDragonWishProvider.buildShenron());
	}

	private static void createDefaultPorungaWishes(Path wishDir) {
		createDefaultDragonWishes(wishDir, "Porunga", DMZDragonWishProvider.buildPorunga());
	}

	private static void createDefaultDragonWishes(Path wishDir, String wishFileName, List<Wish> defaultWishes) {
		File wishFile = wishDir.resolve(wishFileName.toLowerCase() + ".json").toFile();
		try (FileWriter writer = new FileWriter(wishFile)) {
			Type listType = new TypeToken<ArrayList<Wish>>() {
			}.getType();
			GsonUtils.GSON.toJson(defaultWishes, listType, writer);
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Could not create default wishes for "+ wishFileName, e);
		}
	}

	public static Map<String, List<Wish>> getAllWishes() {
		return DragonWishRegistry.getServerWishes();
	}

	public static List<Wish> getClientWishes(String dragonName) {
		return DragonWishRegistry.getClientWishes().getOrDefault(dragonName, new ArrayList<>());
	}

	public static void applySyncedWishes(Map<String, List<Wish>> wishes) {
		DragonWishRegistry.setClientWishes(wishes); LogUtil.info(Env.CLIENT, "Loaded {} wish list(s) from server", wishes.size());
	}
}
