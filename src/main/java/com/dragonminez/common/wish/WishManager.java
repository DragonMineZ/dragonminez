package com.dragonminez.common.wish;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.util.WishTypeAdapter;
import com.dragonminez.common.wish.wishes.*;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WishManager {
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(Wish.class, new WishTypeAdapter())
			.setPrettyPrinting()
			.create();

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
			JsonArray rootArray = GSON.fromJson(Files.readString(path), JsonArray.class);
			List<Wish> wishes = new ArrayList<>();
			for (JsonElement element : rootArray) {
				wishes.add(GSON.fromJson(element, Wish.class));
			}
			String dragonId = path.getFileName().toString().replace(".json", "");
			merged.put(dragonId, List.copyOf(wishes));
			LogUtil.info(Env.COMMON, "Loaded dragon wishes from config file {}", path.getFileName());
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Failed to load dragon wish config '{}': {}", path.getFileName(), e.toString());
		}
	}

	private static void createDefaultShenronWishes(Path wishDir) {
		File wishFile = wishDir.resolve("shenron.json").toFile();
		List<Wish> defaultWishes = new ArrayList<>();

		defaultWishes.add(new ItemWish("wish.shenron.senzu.name", "wish.shenron.senzu.desc", "dragonminez:senzu_bean", 16));
		defaultWishes.add(new TPSWish("wish.shenron.tps.name", "wish.shenron.tps.desc", 5000));
		defaultWishes.add(new ItemWish("wish.shenron.powerpole.name", "wish.shenron.powerpole.desc", "dragonminez:power_pole", 1));
		defaultWishes.add(new ItemWish("wish.shenron.mightfruit.name", "wish.shenron.mightfruit.desc", "dragonminez:might_tree_fruit", 16));
		defaultWishes.add(new ItemWish("wish.shenron.namekcpu.name", "wish.shenron.namekcpu.desc", "dragonminez:t2_radar_cpu", 4));
		defaultWishes.add(new ItemWish("wish.shenron.saiyanship.name", "wish.shenron.saiyanship.desc", "dragonminez:saiyan_ship", 1));
		defaultWishes.add(new PassiveResetWish("wish.shenron.racialskillreset.name", "wish.shenron.racialskillreset.desc"));
		defaultWishes.add(new ReCustomizeWish("wish.shenron.customization.name", "wish.shenron.customization.desc"));

		List<Tuple<String, Integer>> materials = new ArrayList<>();
		materials.add(new Tuple<>("dragonminez:kikono_shard", 32));
		materials.add(new Tuple<>("minecraft:iron_ingot", 64));
		defaultWishes.add(new MultiItemWish("wish.shenron.materials.name", "wish.shenron.materials.desc", materials));

		List<Tuple<String, Integer>> strongest = new ArrayList<>();
		strongest.add(new Tuple<>("dragonminez:strongest_armor_chestplate", 1));
		strongest.add(new Tuple<>("dragonminez:strongest_armor_leggings", 1));
		strongest.add(new Tuple<>("dragonminez:strongest_armor_boots", 1));
		defaultWishes.add(new MultiItemWish("wish.shenron.strongest.name", "wish.shenron.strongest.desc", strongest));

		try (FileWriter writer = new FileWriter(wishFile)) {
			Type listType = new TypeToken<ArrayList<Wish>>() {
			}.getType();
			GSON.toJson(defaultWishes, listType, writer);
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Could not create default wishes for Shenron", e);
		}

	}

	private static void createDefaultPorungaWishes(Path wishDir) {
		File wishFile = wishDir.resolve("porunga.json").toFile();
		List<Wish> defaultWishes = new ArrayList<>();

		defaultWishes.add(new ItemWish("wish.shenron.senzu.name", "wish.shenron.senzu.desc", "dragonminez:senzu_bean", 16));
		defaultWishes.add(new TPSWish("wish.shenron.tps.name", "wish.shenron.tps.desc", 5000));
		defaultWishes.add(new ItemWish("wish.shenron.powerpole.name", "wish.shenron.powerpole.desc", "dragonminez:power_pole", 1));
		defaultWishes.add(new ItemWish("wish.shenron.mightfruit.name", "wish.shenron.mightfruit.desc", "dragonminez:might_tree_fruit", 16));
		defaultWishes.add(new ItemWish("wish.shenron.namekcpu.name", "wish.shenron.namekcpu.desc", "dragonminez:t2_radar_cpu", 4));
		defaultWishes.add(new ItemWish("wish.shenron.saiyanship.name", "wish.shenron.saiyanship.desc", "dragonminez:saiyan_ship", 1));
		defaultWishes.add(new PassiveResetWish("wish.shenron.racialskillreset.name", "wish.shenron.racialskillreset.desc"));
		defaultWishes.add(new ReCustomizeWish("wish.shenron.customization.name", "wish.shenron.customization.desc"));
		defaultWishes.add(new RelocateStatsWish("wish.shenron.relocatestats.name", "wish.shenron.relocatestats.desc"));

		List<Tuple<String, Integer>> materials = new ArrayList<>();
		materials.add(new Tuple<>("dragonminez:kikono_shard", 32));
		materials.add(new Tuple<>("minecraft:iron_ingot", 64));
		defaultWishes.add(new MultiItemWish("wish.shenron.materials.name", "wish.shenron.materials.desc", materials));

		List<Tuple<String, Integer>> strongest = new ArrayList<>();
		strongest.add(new Tuple<>("dragonminez:strongest_armor_chestplate", 1));
		strongest.add(new Tuple<>("dragonminez:strongest_armor_leggings", 1));
		strongest.add(new Tuple<>("dragonminez:strongest_armor_boots", 1));
		defaultWishes.add(new MultiItemWish("wish.shenron.strongest.name", "wish.shenron.strongest.desc", strongest));

		try (FileWriter writer = new FileWriter(wishFile)) {
			Type listType = new TypeToken<ArrayList<Wish>>() {
			}.getType();
			GSON.toJson(defaultWishes, listType, writer);
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Could not create default wishes for Porunga", e);
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
