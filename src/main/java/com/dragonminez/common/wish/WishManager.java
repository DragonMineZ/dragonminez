package com.dragonminez.common.wish;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.util.WishTypeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WishManager {
	private static final Gson GSON = new GsonBuilder()
		.registerTypeAdapter(Wish.class, new WishTypeAdapter())
		.setPrettyPrinting()
		.create();

	private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dragonminez");
	private static final Path WISHES_DIR = CONFIG_DIR.resolve("wishes");

	public static void init() {}

	public static void loadWishes(MinecraftServer server) {
		Map<String, List<Wish>> merged = new LinkedHashMap<>(DragonWishRegistry.getServerWishes());

		if (Files.exists(WISHES_DIR)) {
			try (var stream = Files.list(WISHES_DIR)) {
				stream.filter(path -> path.getFileName().toString().endsWith(".json"))
					.sorted()
					.forEach(path -> loadWishConfig(path, merged));
			} catch (IOException e) {
				LogUtil.error(Env.COMMON, "Failed to read dragon wish config directory '{}': {}", WISHES_DIR, e.toString());
			}
		}

		DragonWishRegistry.setServerWishes(merged);
		LogUtil.info(Env.COMMON, "Loaded {} dragon wish list(s) after merging datapacks and config files", merged.size());
	}

	private static void loadWishConfig(Path path, Map<String, List<Wish>> merged) {
		try {
			JsonObject root = GSON.fromJson(Files.readString(path), JsonObject.class);
			if (root == null || !root.has("dragon") || !root.has("wishes")) return;
			String dragonId = root.get("dragon").getAsString();
			JsonArray wishesArray = root.getAsJsonArray("wishes");
			List<Wish> wishes = new ArrayList<>();
			for (var element : wishesArray) {
				wishes.add(GSON.fromJson(element, Wish.class));
			}
			merged.put(dragonId, List.copyOf(wishes));
			LogUtil.info(Env.COMMON, "Loaded dragon wishes from config file {}", path.getFileName());
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Failed to load dragon wish config '{}': {}", path.getFileName(), e.toString());
		}
	}

	public static Map<String, List<Wish>> getAllWishes() { return DragonWishRegistry.getServerWishes(); }
	public static List<Wish> getClientWishes(String dragonName) { return DragonWishRegistry.getClientWishes().getOrDefault(dragonName, new ArrayList<>()); }
	public static void applySyncedWishes(Map<String, List<Wish>> wishes) { DragonWishRegistry.setClientWishes(wishes); LogUtil.info(Env.CLIENT, "Loaded {} wish list(s) from server", wishes.size()); }
}
