package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.util.WishTypeAdapter;
import com.dragonminez.common.wish.Wish;
import com.dragonminez.common.wish.wishes.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.util.Tuple;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DMZDragonWishProvider implements DataProvider {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Wish.class, new WishTypeAdapter()).create();
	private final PackOutput output;
	public DMZDragonWishProvider(PackOutput output) { this.output = output; }

	@Override
	public CompletableFuture<?> run(CachedOutput cachedOutput) {
		CompletableFuture<?> future = CompletableFuture.completedFuture(null);
		for (DragonDefinition dragon : DragonBallDefinitions.getBootstrapDragons()) {
			List<Wish> wishes = switch (dragon.getId()) {
				case "shenron" -> buildShenron();
				case "porunga" -> buildPorunga();
				default -> List.of();
			};
			if (dragon.getBallSetId() != null) {
				future = CompletableFuture.allOf(future, save(cachedOutput, dragon, wishes));
			}
		}
		return future;
	}

	private CompletableFuture<?> save(CachedOutput cachedOutput, DragonDefinition dragon, List<Wish> wishes) {
		JsonObject root = new JsonObject();
		root.addProperty("dragon", dragon.getId());
		root.add("wishes", GSON.toJsonTree(wishes));
		Path path = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(Reference.MOD_ID).resolve("dragonballs").resolve(dragon.getBallSetId()).resolve("definitions").resolve("wishes.json");
		return DataProvider.saveStable(cachedOutput, GSON.toJsonTree(root), path);
	}

	private static List<Wish> buildShenron() {
		List<Wish> defaultWishes = new ArrayList<>();
		defaultWishes.add(new ItemWish("wish.shenron.senzu.name", "wish.shenron.senzu.desc", "dragonminez:senzu_bean", 16));
		defaultWishes.add(new TPSWish("wish.shenron.tps.name", "wish.shenron.tps.desc", 5000));
		defaultWishes.add(new ItemWish("wish.shenron.powerpole.name", "wish.shenron.powerpole.desc", "dragonminez:power_pole", 1));
		defaultWishes.add(new ItemWish("wish.shenron.mightfruit.name", "wish.shenron.mightfruit.desc", "dragonminez:might_tree_fruit", 16));
		defaultWishes.add(new ItemWish("wish.shenron.namekcpu.name", "wish.shenron.namekcpu.desc", "dragonminez:t2_radar_cpu", 4));
		defaultWishes.add(new ItemWish("wish.shenron.saiyanship.name", "wish.shenron.saiyanship.desc", "dragonminez:saiyan_ship", 1));
		defaultWishes.add(new PassiveResetWish("wish.shenron.racialskillreset.name", "wish.shenron.racialskillreset.desc"));
		defaultWishes.add(new ReCustomizeWish("wish.shenron.customization.name", "wish.shenron.customization.desc"));
		List<Tuple<String, Integer>> materials = new ArrayList<>(); materials.add(new Tuple<>("dragonminez:kikono_shard", 32)); materials.add(new Tuple<>("minecraft:iron_ingot", 64)); defaultWishes.add(new MultiItemWish("wish.shenron.materials.name", "wish.shenron.materials.desc", materials));
		List<Tuple<String, Integer>> strongest = new ArrayList<>(); strongest.add(new Tuple<>("dragonminez:strongest_armor_chestplate", 1)); strongest.add(new Tuple<>("dragonminez:strongest_armor_leggings", 1)); strongest.add(new Tuple<>("dragonminez:strongest_armor_boots", 1)); defaultWishes.add(new MultiItemWish("wish.shenron.strongest.name", "wish.shenron.strongest.desc", strongest));
		return defaultWishes;
	}

	private static List<Wish> buildPorunga() {
		List<Wish> defaultWishes = new ArrayList<>();
		defaultWishes.add(new ItemWish("wish.porunga.senzu.name", "wish.porunga.senzu.desc", "dragonminez:senzu_bean", 32));
		defaultWishes.add(new TPSWish("wish.porunga.tps.name", "wish.porunga.tps.desc", 15000));
		defaultWishes.add(new ItemWish("wish.porunga.bravesword.name", "wish.porunga.bravesword.desc", "dragonminez:brave_sword", 1));
		defaultWishes.add(new PassiveResetWish("wish.porunga.racialskillreset.name", "wish.porunga.racialskillreset.desc"));
		defaultWishes.add(new ReCustomizeWish("wish.porunga.customization.name", "wish.porunga.customization.desc"));
		List<Tuple<String, Integer>> materials = new ArrayList<>(); materials.add(new Tuple<>("dragonminez:kikono_shard", 64)); materials.add(new Tuple<>("minecraft:iron_ingot", 128)); defaultWishes.add(new MultiItemWish("wish.porunga.materials.name", "wish.porunga.materials.desc", materials));
		List<Tuple<String, Integer>> invincible = new ArrayList<>(); invincible.add(new Tuple<>("dragonminez:invencible_armor_helmet", 1)); invincible.add(new Tuple<>("dragonminez:invencible_armor_chestplate", 1)); invincible.add(new Tuple<>("dragonminez:invencible_armor_leggings", 1)); invincible.add(new Tuple<>("dragonminez:invencible_armor_boots", 1)); defaultWishes.add(new MultiItemWish("wish.porunga.invincible.name", "wish.porunga.invincible.desc", invincible));
		List<Tuple<String, Integer>> invincibleBlue = new ArrayList<>(); invincibleBlue.add(new Tuple<>("dragonminez:invencible_blue_armor_helmet", 1)); invincibleBlue.add(new Tuple<>("dragonminez:invencible_blue_armor_chestplate", 1)); invincibleBlue.add(new Tuple<>("dragonminez:invencible_blue_armor_leggings", 1)); invincibleBlue.add(new Tuple<>("dragonminez:invencible_blue_armor_boots", 1)); defaultWishes.add(new MultiItemWish("wish.porunga.invincible_blue.name", "wish.porunga.invincible_blue.desc", invincibleBlue));
		List<Tuple<String, Integer>> potaraYellow = new ArrayList<>(); potaraYellow.add(new Tuple<>("dragonminez:pothala_left", 1)); potaraYellow.add(new Tuple<>("dragonminez:pothala_right", 1)); defaultWishes.add(new MultiItemWish("wish.porunga.pothala_yellow.name", "wish.porunga.pothala_yellow.desc", potaraYellow));
		List<Tuple<String, Integer>> potaraGreen = new ArrayList<>(); potaraGreen.add(new Tuple<>("dragonminez:green_pothala_left", 1)); potaraGreen.add(new Tuple<>("dragonminez:green_pothala_right", 1)); defaultWishes.add(new MultiItemWish("wish.porunga.pothala_green.name", "wish.porunga.pothala_green.desc", potaraGreen));
		return defaultWishes;
	}

	@Override public String getName() { return "DragonMineZ dragonballs wish datapack provider"; }
}
