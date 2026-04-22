package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DMZDragonDefinitionProvider implements DataProvider {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final PackOutput output;
	public DMZDragonDefinitionProvider(PackOutput output) { this.output = output; }

	@Override
	public CompletableFuture<?> run(CachedOutput cachedOutput) {
		CompletableFuture<?> future = CompletableFuture.completedFuture(null);

		Map<String, DragonBallSetDefinition> setsById = new LinkedHashMap<>();
		for (DragonBallSetDefinition definition : DragonBallDefinitions.getBootstrapBallSets()) {
			setsById.put(definition.getId(), definition);
			future = CompletableFuture.allOf(future, save(cachedOutput, definition.toJson(), "dragonballs/" + definition.getId() + "/definitions/ballset.json"));
		}

		for (DragonRadarDefinition definition : DragonBallDefinitions.getBootstrapRadars()) {
			if (definition.getBallSetId() != null && setsById.containsKey(definition.getBallSetId())) {
				future = CompletableFuture.allOf(future, save(cachedOutput, definition.toJson(), "dragonballs/" + definition.getBallSetId() + "/definitions/radar.json"));
			}
		}

		for (DragonDefinition definition : DragonBallDefinitions.getBootstrapDragons()) {
			if (definition.getBallSetId() != null && setsById.containsKey(definition.getBallSetId())) {
				future = CompletableFuture.allOf(future, save(cachedOutput, definition.toJson(), "dragonballs/" + definition.getBallSetId() + "/definitions/dragon.json"));
			}
		}

		for (DragonBallSetAssetDefinition definition : DragonBallDefinitions.getBootstrapBallSetAssets()) {
			String setId = findSetForBallAsset(definition.getId());
			if (setId != null) {
				future = CompletableFuture.allOf(future, save(cachedOutput, definition.toJson(), "dragonballs/" + setId + "/assets/ballset.json"));
			}
		}

		for (DragonRadarAssetDefinition definition : DragonBallDefinitions.getBootstrapRadarAssets()) {
			String setId = findSetForRadarAsset(definition.getId());
			if (setId != null) {
				future = CompletableFuture.allOf(future, save(cachedOutput, definition.toJson(), "dragonballs/" + setId + "/assets/radar.json"));
			}
		}

		for (DragonAssetDefinition definition : DragonBallDefinitions.getBootstrapDragonAssets()) {
			String setId = findSetForDragonAsset(definition.getId());
			if (setId != null) {
				future = CompletableFuture.allOf(future, save(cachedOutput, definition.toJson(), "dragonballs/" + setId + "/assets/dragon.json"));
			}
		}

		return future;
	}

	private String findSetForBallAsset(String assetId) {
		for (DragonBallSetDefinition set : DragonBallDefinitions.getBootstrapBallSets()) {
			if (set.getAssetDefinitionId().isPresent() && set.getAssetDefinitionId().get().equals(assetId)) {
				return set.getId();
			}
		}
		return null;
	}

	private String findSetForRadarAsset(String assetId) {
		for (DragonRadarDefinition radar : DragonBallDefinitions.getBootstrapRadars()) {
			if (radar.getAssetDefinitionId().isPresent() && radar.getAssetDefinitionId().get().equals(assetId)) {
				return radar.getBallSetId();
			}
		}
		return null;
	}

	private String findSetForDragonAsset(String assetId) {
		for (DragonDefinition dragon : DragonBallDefinitions.getBootstrapDragons()) {
			if (dragon.getAssetDefinitionId().isPresent() && dragon.getAssetDefinitionId().get().equals(assetId)) {
				return dragon.getBallSetId();
			}
		}
		return null;
	}

	private CompletableFuture<?> save(CachedOutput cachedOutput, JsonObject root, String relativePath) {
		Path path = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(Reference.MOD_ID).resolve(relativePath);
		return DataProvider.saveStable(cachedOutput, GSON.toJsonTree(root), path);
	}

	@Override public String getName() { return "DragonMineZ dragonballs definition datapack provider"; }
}
