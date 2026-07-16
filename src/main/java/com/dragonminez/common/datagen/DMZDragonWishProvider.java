package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.util.gson.GsonUtils;
import com.dragonminez.common.util.types.items.GenericItemDTO;
import com.dragonminez.common.wish.Wish;
import com.dragonminez.common.wish.wishes.*;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DMZDragonWishProvider implements DataProvider {
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
		root.add("wishes", GsonUtils.GSON.toJsonTree(wishes));
		Path path = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(Reference.MOD_ID).resolve("dragonballs").resolve(dragon.getBallSetId()).resolve("definitions").resolve("wishes.json");
		return DataProvider.saveStable(cachedOutput, GsonUtils.GSON.toJsonTree(root), path);
	}

	public static List<Wish> buildShenron() {
		List<Wish> defaultWishes = new ArrayList<>();
		defaultWishes.add(new ItemListWish("wish.shenron.senzu.name", "wish.shenron.senzu.desc", MainItems.SENZU_BEAN, 16));
		defaultWishes.add(new TPSWish("wish.shenron.tps.name", "wish.shenron.tps.desc", 5000));
		defaultWishes.add(new ItemListWish("wish.shenron.powerpole.name", "wish.shenron.powerpole.desc", MainItems.POWER_POLE));
		defaultWishes.add(new ItemListWish("wish.shenron.mightfruit.name", "wish.shenron.mightfruit.desc", MainItems.MIGHT_TREE_FRUIT, 16));
		defaultWishes.add(new ItemListWish("wish.shenron.namekcpu.name", "wish.shenron.namekcpu.desc", MainItems.T2_RADAR_CPU, 4));
		defaultWishes.add(new ItemListWish("wish.shenron.saiyanship.name", "wish.shenron.saiyanship.desc", MainItems.NAVE_SAIYAN_ITEM));

		defaultWishes.add(new PassiveResetWish("wish.shenron.racialskillreset.name", "wish.shenron.racialskillreset.desc"));
		defaultWishes.add(new ReCustomizeWish("wish.shenron.customization.name", "wish.shenron.customization.desc"));
		defaultWishes.add(new ChangeDifficultyWish("wish.shenron.changedifficulty.name", "wish.shenron.changedifficulty.desc"));
		defaultWishes.add(new ResetStoryWish("wish.shenron.resetstory.name", "wish.shenron.resetstory.desc"));

		List<GenericItemDTO> materials = new ArrayList<>();
		materials.add(new GenericItemDTO(MainItems.KIKONO_SHARD.getId(), 32));
		materials.add(new GenericItemDTO(Items.IRON_INGOT, 64));
		defaultWishes.add(new ItemListWish("wish.shenron.materials.name", "wish.shenron.materials.desc", materials));
		defaultWishes.add(new ItemListWish("wish.shenron.strongest.name", "wish.shenron.strongest.desc", MainItems.STRONGEST_ARMOR));
		return defaultWishes;
	}

	public static List<Wish> buildPorunga() {
		List<Wish> defaultWishes = new ArrayList<>();
		defaultWishes.add(new ItemListWish("wish.porunga.senzu.name", "wish.porunga.senzu.desc", MainItems.SENZU_BEAN, 32));
		defaultWishes.add(new TPSWish("wish.porunga.tps.name", "wish.porunga.tps.desc", 15000));
		defaultWishes.add(new ItemListWish("wish.porunga.bravesword.name", "wish.porunga.bravesword.desc",  MainItems.BRAVE_SWORD));
		defaultWishes.add(new PassiveResetWish("wish.porunga.racialskillreset.name", "wish.porunga.racialskillreset.desc"));
		defaultWishes.add(new ReCustomizeWish("wish.porunga.customization.name", "wish.porunga.customization.desc"));
		defaultWishes.add(new RelocateStatsWish("wish.porunga.relocatestats.name", "wish.porunga.relocatestats.desc"));
		defaultWishes.add(new ChangeDifficultyWish("wish.porunga.changedifficulty.name", "wish.porunga.changedifficulty.desc"));
		defaultWishes.add(new ResetStoryWish("wish.porunga.resetstory.name", "wish.porunga.resetstory.desc"));

		List<GenericItemDTO> materials = new ArrayList<>();
		materials.add(new GenericItemDTO(MainItems.KIKONO_SHARD.getId(), 64));
		materials.add(new GenericItemDTO(Items.IRON_INGOT, 128));
		defaultWishes.add(new ItemListWish("wish.porunga.materials.name", "wish.porunga.materials.desc", materials));

		defaultWishes.add(new ItemListWish("wish.porunga.invincible.name", "wish.porunga.invincible.desc", MainItems.INVENCIBLE_ARMOR));
		defaultWishes.add(new ItemListWish("wish.porunga.invincible_blue.name", "wish.porunga.invincible_blue.desc", MainItems.INVENCIBLE_BLUE_ARMOR));
		defaultWishes.add(new ItemListWish("wish.porunga.pothala_yellow.name", "wish.porunga.pothala_yellow.desc", MainItems.POTHALA_PAIR));
		defaultWishes.add(new ItemListWish("wish.porunga.pothala_green.name", "wish.porunga.pothala_green.desc", MainItems.GREEN_POTHALA_PAIR));
		return defaultWishes;
	}

	@Override public String getName() { return "DragonMineZ dragonballs wish datapack provider"; }
}
