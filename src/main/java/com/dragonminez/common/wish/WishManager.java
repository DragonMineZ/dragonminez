package com.dragonminez.common.wish;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.util.WishTypeAdapter;
import com.dragonminez.common.wish.wishtype.*;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WishManager {
    private static final Map<String, List<Wish>> wishes = new HashMap<>();
    private static final Map<String, List<Wish>> CLIENT_WISHES = new HashMap<>();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Wish.class, new WishTypeAdapter())
            .create();
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
        if (!wishFile.exists()) createDefaultWishes(wishDir, dragonName);
        try (FileReader reader = new FileReader(wishFile)) {
            Type listType = new TypeToken<ArrayList<Wish>>(){}.getType();
            dragonWishes = GSON.fromJson(reader, listType);
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Could not load wishes for " + dragonName, e);
        }

        wishes.put(dragonName, dragonWishes);
    }

    private static void createDefaultWishes(Path wishDir, String dragonName) {
        File wishFile = wishDir.resolve(dragonName + ".json").toFile();

        List<Wish> wishes = new ArrayList<>();
        if (dragonName.equals("shenron")) {
            wishes.add(new ItemWish("wish.shenron.senzu.name", "wish.shenron.senzu.desc", "dragonminez:senzu_bean", 16));
            wishes.add(new TPSWish("wish.shenron.tps.name", "wish.shenron.tps.desc", 5000));
            wishes.add(new ItemWish("wish.shenron.powerpole.name", "wish.shenron.powerpole.desc", "dragonminez:power_pole", 1));
            wishes.add(new ItemWish("wish.shenron.mightfruit.name", "wish.shenron.mightfruit.desc", "dragonminez:might_tree_fruit", 16));
            wishes.add(new ItemWish("wish.shenron.namekcpu.name", "wish.shenron.namekcpu.desc", "dragonminez:t2_radar_cpu", 1));
            wishes.add(new ItemWish("wish.shenron.saiyanship.name", "wish.shenron.saiyanship.desc", "dragonminez:saiyan_ship", 1));
            wishes.add(new PassiveResetWish("wish.shenron.racialskillreset.name", "wish.shenron.racialskillreset.desc"));
            wishes.add(new RestoreSaiyanTailWish("wish.shenron.restoresaiyantail.name", "wish.shenron.restoresaiyantail.desc"));

            List<Tuple<String, Integer>> materials = new ArrayList<>();
            materials.add(new Tuple<>("dragonminez:kikono_shard", 32));
            materials.add(new Tuple<>("minecraft:iron_ingot", 64));
            wishes.add(new MultiItemWish("wish.shenron.materials.name", "wish.shenron.materials.desc", materials));

            List<Tuple<String, Integer>> strongest = new ArrayList<>();
            strongest.add(new Tuple<>("dragonminez:strongest_armor_chestplate", 1));
            strongest.add(new Tuple<>("dragonminez:strongest_armor_leggings", 1));
            strongest.add(new Tuple<>("dragonminez:strongest_armor_boots", 1));
            wishes.add(new MultiItemWish("wish.shenron.strongest.name", "wish.shenron.strongest.desc", strongest));
        } else if (dragonName.equals("porunga")) {
            wishes.add(new ItemWish("wish.porunga.senzu.name", "wish.porunga.senzu.desc", "dragonminez:senzu_bean", 32));
            wishes.add(new TPSWish("wish.porunga.tps.name", "wish.porunga.tps.desc", 15000));
            wishes.add(new ItemWish("wish.porunga.bravesword.name", "wish.porunga.bravesword.desc", "dragonminez:brave_sword", 1));
            wishes.add(new PassiveResetWish("wish.porunga.racialskillreset.name", "wish.porunga.racialskillreset.desc"));

            List<Tuple<String, Integer>> materials = new ArrayList<>();
            materials.add(new Tuple<>("dragonminez:kikono_shard", 64));
            materials.add(new Tuple<>("minecraft:iron_ingot", 128));
            wishes.add(new MultiItemWish("wish.porunga.materials.name", "wish.porunga.materials.desc", materials));

            List<Tuple<String, Integer>> invincible = new ArrayList<>();
            invincible.add(new Tuple<>("dragonminez:invencible_armor_helmet", 1));
            invincible.add(new Tuple<>("dragonminez:invencible_armor_chestplate", 1));
            invincible.add(new Tuple<>("dragonminez:invencible_armor_leggings", 1));
            invincible.add(new Tuple<>("dragonminez:invencible_armor_boots", 1));
            wishes.add(new MultiItemWish("wish.porunga.invincible.name", "wish.porunga.invincible.desc", invincible));

            List<Tuple<String, Integer>> invincibleBlue = new ArrayList<>();
            invincibleBlue.add(new Tuple<>("dragonminez:invencible_blue_armor_helmet", 1));
            invincibleBlue.add(new Tuple<>("dragonminez:invencible_blue_armor_chestplate", 1));
            invincibleBlue.add(new Tuple<>("dragonminez:invencible_blue_armor_leggings", 1));
            invincibleBlue.add(new Tuple<>("dragonminez:invencible_blue_armor_boots", 1));
            wishes.add(new MultiItemWish("wish.porunga.invincible_blue.name", "wish.porunga.invincible_blue.desc", invincibleBlue));

            List<Tuple<String, Integer>> potaraYellow = new ArrayList<>();
            potaraYellow.add(new Tuple<>("dragonminez:pothala_left", 1));
            potaraYellow.add(new Tuple<>("dragonminez:pothala_right", 1));
            wishes.add(new MultiItemWish("wish.porunga.pothala_yellow.name", "wish.porunga.pothala_yellow.desc", potaraYellow));

            List<Tuple<String, Integer>> potaraGreen = new ArrayList<>();
            potaraGreen.add(new Tuple<>("dragonminez:green_pothala_left", 1));
            potaraGreen.add(new Tuple<>("dragonminez:green_pothala_right", 1));
            wishes.add(new MultiItemWish("wish.porunga.pothala_green.name", "wish.porunga.pothala_green.desc", potaraGreen));
        }

        try (FileWriter writer = new FileWriter(wishFile)) {
            Type listType = new TypeToken<ArrayList<Wish>>(){}.getType();
            GSON.toJson(wishes, listType, writer);
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
