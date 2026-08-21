package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.util.gson.GsonUtils;
import com.dragonminez.common.wish.DefaultWishes;
import com.dragonminez.common.wish.Wish;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
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
				case "shenron" -> DefaultWishes.shenron();
				case "porunga" -> DefaultWishes.porunga();
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

	@Override public String getName() { return "DragonMineZ dragonballs wish datapack provider"; }
}
