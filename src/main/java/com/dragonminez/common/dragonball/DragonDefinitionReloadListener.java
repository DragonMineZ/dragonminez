package com.dragonminez.common.dragonball;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DragonDefinitionReloadListener extends SimpleJsonResourceReloadListener {
	public static final String ROOT_DIRECTORY = "dragonminez/dragonballs";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final DragonDefinitionReloadListener INSTANCE = new DragonDefinitionReloadListener();

	private DragonDefinitionReloadListener() { super(GSON, ROOT_DIRECTORY); }

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> ignored, ResourceManager resourceManager, ProfilerFiller profiler) {
		DragonBallPackManager.LoadedDefinitions external = DragonBallPackManager.loadAll();

		Map<String, DragonBallSetDefinition> ballSets = new LinkedHashMap<>();
		for (DragonBallSetDefinition def : DragonBallDefinitions.getBootstrapBallSets()) ballSets.put(def.getId(), def);
		external.ballSets.values().forEach(def -> ballSets.put(def.getId(), def));

		Map<String, DragonRadarDefinition> radars = new LinkedHashMap<>();
		for (DragonRadarDefinition def : DragonBallDefinitions.getBootstrapRadars()) radars.put(def.getId(), def);
		external.radars.values().forEach(def -> radars.put(def.getId(), def));

		Map<String, DragonDefinition> dragons = new LinkedHashMap<>();
		for (DragonDefinition def : DragonBallDefinitions.getBootstrapDragons()) dragons.put(def.getId(), def);
		external.dragons.values().forEach(def -> dragons.put(def.getId(), def));

		Map<String, DragonRadarRecipeDefinition> radarRecipes = new LinkedHashMap<>();
		for (DragonRadarRecipeDefinition def : DragonBallDefinitions.getBootstrapRadarRecipes()) radarRecipes.put(def.getId(), def);
		external.radarRecipes.values().forEach(def -> radarRecipes.put(def.getId(), def));

		Map<String, DragonBallSetAssetDefinition> ballSetAssets = new LinkedHashMap<>();
		for (DragonBallSetAssetDefinition def : DragonBallDefinitions.getBootstrapBallSetAssets()) ballSetAssets.put(def.getId(), def);
		external.ballSetAssets.values().forEach(def -> ballSetAssets.put(def.getId(), def));

		Map<String, DragonRadarAssetDefinition> radarAssets = new LinkedHashMap<>();
		for (DragonRadarAssetDefinition def : DragonBallDefinitions.getBootstrapRadarAssets()) radarAssets.put(def.getId(), def);
		external.radarAssets.values().forEach(def -> radarAssets.put(def.getId(), def));

		Map<String, DragonAssetDefinition> dragonAssets = new LinkedHashMap<>();
		for (DragonAssetDefinition def : DragonBallDefinitions.getBootstrapDragonAssets()) dragonAssets.put(def.getId(), def);
		external.dragonAssets.values().forEach(def -> dragonAssets.put(def.getId(), def));

		DragonBallDefinitions.setRuntimeDefinitions(new ArrayList<>(ballSets.values()), new ArrayList<>(radars.values()), new ArrayList<>(dragons.values()), new ArrayList<>(radarRecipes.values()), new ArrayList<>(ballSetAssets.values()), new ArrayList<>(radarAssets.values()), new ArrayList<>(dragonAssets.values()));
		LogUtil.info(Env.COMMON, "Loaded {} ball set(s), {} radar definition(s), {} dragon definition(s), {} radar recipe definition(s), {} ball set asset definition(s), {} radar asset definition(s), and {} dragon asset definition(s) from the dragonballs system",
			ballSets.size(), radars.size(), dragons.size(), radarRecipes.size(), ballSetAssets.size(), radarAssets.size(), dragonAssets.size());
	}
}
