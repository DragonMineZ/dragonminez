package com.dragonminez.common.wish;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WishManager {
    private static final Map<String, List<Wish>> wishes = new HashMap<>();
    private static final Map<String, List<Wish>> CLIENT_WISHES = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String WISH_FOLDER = "dragonminez" + File.separator + "wishes";

    public static void init() {}

    public static void loadWishes(MinecraftServer server) {
        wishes.clear();

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
        Path wishDir = worldFolder.resolve(WISH_FOLDER);

        try {
            if (!Files.exists(wishDir)) {
                Files.createDirectories(wishDir);
                createDefaultWishes(wishDir, "shenron");
                createDefaultWishes(wishDir, "porunga");
            }

            loadWishesForDragon(wishDir, "shenron");
            loadWishesForDragon(wishDir, "porunga");

        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Failed to load wishes", e);
        }
    }

    private static void loadWishesForDragon(Path wishDir, String dragonName) {
        File wishFile = wishDir.resolve(dragonName + ".json").toFile();
        List<Wish> dragonWishes = new ArrayList<>();

        if (!wishFile.exists()) {
            createDefaultWishes(wishDir, dragonName);
        }

        try (FileReader reader = new FileReader(wishFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            JsonArray wishesArray = json.getAsJsonArray("wishes");

            for (int i = 0; i < wishesArray.size(); i++) {
                JsonObject wishObject = wishesArray.get(i).getAsJsonObject();
                String type = wishObject.get("type").getAsString();
                String name = wishObject.get("name").getAsString();
                String description = wishObject.get("description").getAsString();

                switch (type) {
                    case "item":
                        String itemId = wishObject.get("item_id").getAsString();
                        int count = wishObject.get("count").getAsInt();
                        dragonWishes.add(new ItemWish(name, description, itemId, count));
                        break;
                    case "command":
                        JsonArray commandsArray = wishObject.getAsJsonArray("commands");
                        String[] commands = new String[commandsArray.size()];
                        for (int j = 0; j < commandsArray.size(); j++) {
                            commands[j] = commandsArray.get(j).getAsString();
                        }
                        dragonWishes.add(new CommandWish(name, description, commands));
                        break;
                    case "tps":
                        int amount = wishObject.get("amount").getAsInt();
                        dragonWishes.add(new TPSWish(name, description, amount));
                        break;
                }
            }
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Could not load wishes for " + dragonName, e);
        }

        wishes.put(dragonName, dragonWishes);
    }

    private static void createDefaultWishes(Path wishDir, String dragonName) {
        File wishFile = wishDir.resolve(dragonName + ".json").toFile();
        JsonObject json = new JsonObject();
        JsonArray wishesArray = new JsonArray();

        if (dragonName.equals("shenron")) {
            wishesArray.add(new ItemWish("wish.shenron.senzu.name", "wish.shenron.senzu.desc", "dragonminez:senzu_bean", 16).toJson());
            wishesArray.add(new TPSWish("wish.shenron.tps.name", "wish.shenron.tps.desc", 5000).toJson());
            wishesArray.add(new CommandWish("wish.shenron.materials.name", "wish.shenron.materials.desc", "give %player% dragonminez:kikono_shard 32", "give %player% minecraft:iron_ingot 64").toJson());
			wishesArray.add(new ItemWish("wish.shenron.powerpole.name", "wish.shenron.powerpole.desc", "dragonminez:power_pole", 1).toJson());
			wishesArray.add(new CommandWish("wish.shenron.strongest.name", "wish.shenron.strongest.desc", "give %player% dragonminez:strongest_armor_chestplate 1", "give %player% dragonminez:strongest_armor_leggings 1", "give %player% dragonminez:strongest_armor_boots 1").toJson());
			wishesArray.add(new ItemWish("wish.shenron.mightfruit.name", "wish.shenron.mightfruit.desc", "dragonminez:might_tree_fruit", 16).toJson());
        } else if (dragonName.equals("porunga")) {
            wishesArray.add(new ItemWish("wish.porunga.senzu.name", "wish.porunga.senzu.desc", "dragonminez:senzu_bean", 32).toJson());
            wishesArray.add(new TPSWish("wish.porunga.tps.name", "wish.porunga.tps.desc", 15000).toJson());
            wishesArray.add(new CommandWish("wish.porunga.materials.name", "wish.porunga.materials.desc", "give %player% dragonminez:kikono_shard 64", "give %player% minecraft:iron_ingot 128").toJson());
			wishesArray.add(new CommandWish("wish.porunga.invincible.name", "wish.porunga.invincible.desc", "give %player% dragonminez:invencible_armor_helmet 1", "give %player% dragonminez:invencible_armor_chestplate 1", "give %player% dragonminez:invencible_armor_leggings 1", "give %player% dragonminez:invencible_armor_boots 1").toJson());
			wishesArray.add(new CommandWish("wish.porunga.invincible_blue.name", "wish.porunga.invincible_blue.desc", "give %player% dragonminez:invencible_blue_armor_helmet 1", "give %player% dragonminez:invencible_blue_armor_chestplate 1", "give %player% dragonminez:invencible_blue_armor_leggings 1", "give %player% dragonminez:invencible_blue_armor_boots 1").toJson());
			wishesArray.add(new ItemWish("wish.porunga.bravesword.name", "wish.porunga.bravesword.desc", "dragonminez:brave_sword", 1).toJson());
			wishesArray.add(new CommandWish("wish.porunga.pothala_yellow.name", "wish.porunga.pothala_yellow.desc", "give %player% dragonminez:pothala_left 1", "give %player% dragonminez:pothala_right 1").toJson());
			wishesArray.add(new CommandWish("wish.porunga.pothala_green.name", "wish.porunga.pothala_green.desc", "give %player% dragonminez:green_pothala_left 1", "give %player% dragonminez:green_pothala_right 1").toJson());
        }

        json.add("wishes", wishesArray);

        try (FileWriter writer = new FileWriter(wishFile)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Could not create default wishes for " + dragonName, e);
        }
    }

    public static Map<String, List<Wish>> getAllWishes() {
        return new HashMap<>(wishes);
    }

    public static List<Wish> getClientWishes(String dragonName) {
        return CLIENT_WISHES.getOrDefault(dragonName, new ArrayList<>());
    }

    public static void applySyncedWishes(Map<String, List<Wish>> wishes) {
        CLIENT_WISHES.clear();
        CLIENT_WISHES.putAll(wishes);
        LogUtil.info(Env.CLIENT, "Loaded {} wish list(s) from server", wishes.size());
    }
}
