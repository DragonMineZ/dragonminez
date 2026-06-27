package com.dragonminez.common.dragonball;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.server.world.dimension.NamekDimension;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.*;

public final class DragonBallDefinitions {
	private static final Map<String, DragonBallSetDefinition> BOOTSTRAP_BALL_SETS = new LinkedHashMap<>();
	private static final Map<String, DragonRadarDefinition> BOOTSTRAP_RADARS = new LinkedHashMap<>();
	private static final Map<String, DragonDefinition> BOOTSTRAP_DRAGONS = new LinkedHashMap<>();
	private static final Map<String, DragonRadarRecipeDefinition> BOOTSTRAP_RADAR_RECIPES = new LinkedHashMap<>();
	private static final Map<String, DragonBallSetAssetDefinition> BOOTSTRAP_BALL_SET_ASSETS = new LinkedHashMap<>();
	private static final Map<String, DragonRadarAssetDefinition> BOOTSTRAP_RADAR_ASSETS = new LinkedHashMap<>();
	private static final Map<String, DragonAssetDefinition> BOOTSTRAP_DRAGON_ASSETS = new LinkedHashMap<>();

	private static Map<String, DragonBallSetDefinition> runtimeBallSets = Map.of();
	private static Map<String, DragonRadarDefinition> runtimeRadars = Map.of();
	private static Map<String, DragonDefinition> runtimeDragons = Map.of();
	private static Map<String, DragonRadarRecipeDefinition> runtimeRadarRecipes = Map.of();
	private static Map<String, DragonBallSetAssetDefinition> runtimeBallSetAssets = Map.of();
	private static Map<String, DragonRadarAssetDefinition> runtimeRadarAssets = Map.of();
	private static Map<String, DragonAssetDefinition> runtimeDragonAssets = Map.of();

	static {
		registerBootstrapBallSetAsset(new DragonBallSetAssetDefinition("earth_ballset", "dragonminez:block/custom/dballblock", "dragonminez:item/dball%d", "dragonminez:geo/block/dball.geo.json", "dragonminez:animations/block/dball.animation.json", "dragonminez:textures/block/custom/dballblock", null, null));
		registerBootstrapBallSetAsset(new DragonBallSetAssetDefinition("namek_ballset", "dragonminez:block/custom/dballnamekblock", "dragonminez:item/dball%d_namek", "dragonminez:geo/block/dballnamek.geo.json", "dragonminez:animations/block/dball.animation.json", "dragonminez:textures/block/custom/dballnamekblock", null, null));
		registerBootstrapRadarAsset(new DragonRadarAssetDefinition("earth_radar", "dragonminez:item/dball_radar", "dragonminez:item/dball_radar", "dragonminez:textures/gui/radar.png", "dragonminez:textures/gui/radar_dot.png", null));
		registerBootstrapRadarAsset(new DragonRadarAssetDefinition("namek_radar", "dragonminez:item/dball_radar", "dragonminez:item/dball_radar", "dragonminez:textures/gui/radar.png", "dragonminez:textures/gui/radar_dot.png", null));
		registerBootstrapDragonAsset(new DragonAssetDefinition("shenron", "default", "dragonminez:geo/entity/dragon/shenron.geo.json", "dragonminez:textures/entity/dragon/shenron.png", "dragonminez:animations/entity/dragon/shenron.animation.json"));
		registerBootstrapDragonAsset(new DragonAssetDefinition("porunga", "default", "dragonminez:geo/entity/dragon/porunga.geo.json", "dragonminez:textures/entity/dragon/porunga.png", "dragonminez:animations/entity/dragon/porunga.animation.json"));

		registerBootstrapRadarRecipe(new ShapedDragonRadarRecipeDefinition("earth_radar_recipe", "dragonminez:t1_radar_chip", "dragonminez:t1_radar_cpu"));
		registerBootstrapRadarRecipe(new ShapedDragonRadarRecipeDefinition("namek_radar_recipe", "dragonminez:t2_radar_chip", "dragonminez:t2_radar_cpu"));

		registerBootstrapBallSet(new DragonBallSetDefinition("earth", Set.of(Level.OVERWORLD.location()), () -> Math.max(1, ConfigManager.getServerConfig().getWorldGen().getDragonBallSets()), () -> ConfigManager.getServerConfig().getWorldGen().getDBSpawnRange(), 5, Map.of(1, "dball1", 2, "dball2", 3, "dball3", 4, "dball4", 5, "dball5", 6, "dball6", 7, "dball7"), "earth_ballset", "Earth Dragon Ball"));
		registerBootstrapBallSet(new DragonBallSetDefinition("namek", Set.of(NamekDimension.NAMEK_KEY.location()), () -> Math.max(1, ConfigManager.getServerConfig().getWorldGen().getDragonBallSets()), () -> ConfigManager.getServerConfig().getWorldGen().getDBSpawnRange(), 5, Map.of(1, "dball1_namek", 2, "dball2_namek", 3, "dball3_namek", 4, "dball4_namek", 5, "dball5_namek", 6, "dball6_namek", 7, "dball7_namek"), "namek_ballset", "Namek Dragon Ball"));
		registerBootstrapRadar(new DragonRadarDefinition("earth_radar", "dball_radar", Set.of(Level.OVERWORLD.location()), "earth", "item.dragonminez.dball_radar.tooltip", new int[]{150, 300}, "earth_radar_recipe", "earth_radar", "Dragon Radar"));
		registerBootstrapRadar(new DragonRadarDefinition("namek_radar", "namekdball_radar", Set.of(NamekDimension.NAMEK_KEY.location()), "namek", "item.dragonminez.namekdball_radar.tooltip", new int[]{150, 300}, "namek_radar_recipe", "namek_radar", "Namek Dragon Radar"));
		// Bulma's "Bi-Dimensional Radar" — fused from an Earth + Namek radar (quest reward, not craftable).
		// Works in BOTH dimensions; RadarRenderEvent picks the matching ball set per dimension.
		registerBootstrapRadar(new DragonRadarDefinition("fused_radar", "fused_dball_radar", Set.of(Level.OVERWORLD.location(), NamekDimension.NAMEK_KEY.location()), "earth", "item.dragonminez.fused_dball_radar.tooltip", new int[]{150, 300, 600}, null, "earth_radar", "Bi-Dimensional Radar"));
		registerBootstrapDragon(new DragonDefinition("shenron", "shenron", 3.0f, 17.0f, Set.of(Level.OVERWORLD.location()), "earth", "shenron", 1, "shenron"));
		registerBootstrapDragon(new DragonDefinition("porunga", "porunga", 4.0f, 20.0f, Set.of(NamekDimension.NAMEK_KEY.location()), "namek", "porunga", 3, "porunga"));

		loadExternalBootstrapDefinitions();
		resetRuntimeDefinitions();
	}

	private DragonBallDefinitions() {}

	private static void loadExternalBootstrapDefinitions() {
		DragonBallPackManager.LoadedDefinitions loaded = DragonBallPackManager.loadAll();
		loaded.ballSetAssets.values().forEach(DragonBallDefinitions::registerBootstrapBallSetAsset);
		loaded.radarAssets.values().forEach(DragonBallDefinitions::registerBootstrapRadarAsset);
		loaded.dragonAssets.values().forEach(DragonBallDefinitions::registerBootstrapDragonAsset);
		loaded.radarRecipes.values().forEach(DragonBallDefinitions::registerBootstrapRadarRecipe);
		loaded.ballSets.values().forEach(DragonBallDefinitions::registerBootstrapBallSet);
		loaded.radars.values().forEach(DragonBallDefinitions::registerBootstrapRadar);
		loaded.dragons.values().forEach(DragonBallDefinitions::registerBootstrapDragon);
	}

	public static void registerBootstrapBallSet(DragonBallSetDefinition definition) { BOOTSTRAP_BALL_SETS.put(definition.getId(), definition); }
	public static void registerBootstrapRadar(DragonRadarDefinition definition) { BOOTSTRAP_RADARS.put(definition.getId(), definition); }
	public static void registerBootstrapDragon(DragonDefinition definition) { BOOTSTRAP_DRAGONS.put(definition.getId(), definition); }
	public static void registerBootstrapRadarRecipe(DragonRadarRecipeDefinition definition) { BOOTSTRAP_RADAR_RECIPES.put(definition.getId(), definition); }
	public static void registerBootstrapBallSetAsset(DragonBallSetAssetDefinition definition) { BOOTSTRAP_BALL_SET_ASSETS.put(definition.getId(), definition); }
	public static void registerBootstrapRadarAsset(DragonRadarAssetDefinition definition) { BOOTSTRAP_RADAR_ASSETS.put(definition.getId(), definition); }
	public static void registerBootstrapDragonAsset(DragonAssetDefinition definition) { BOOTSTRAP_DRAGON_ASSETS.put(definition.getId(), definition); }

	public static void setRuntimeDefinitions(Collection<DragonBallSetDefinition> ballSets, Collection<DragonRadarDefinition> radars, Collection<DragonDefinition> dragons, Collection<DragonRadarRecipeDefinition> radarRecipes, Collection<DragonBallSetAssetDefinition> ballSetAssets, Collection<DragonRadarAssetDefinition> radarAssets, Collection<DragonAssetDefinition> dragonAssets) {
		Map<String, DragonBallSetDefinition> preparedBallSets = new LinkedHashMap<>();
		for (DragonBallSetDefinition definition : ballSets) {
			syncRegisteredBlocks(definition);
			preparedBallSets.put(definition.getId(), definition);
		}
		runtimeBallSets = Map.copyOf(preparedBallSets);
		runtimeRadars = copyById(radars, DragonRadarDefinition::getId);
		runtimeDragons = copyById(dragons, DragonDefinition::getId);
		runtimeRadarRecipes = copyById(radarRecipes, DragonRadarRecipeDefinition::getId);
		runtimeBallSetAssets = copyById(ballSetAssets, DragonBallSetAssetDefinition::getId);
		runtimeRadarAssets = copyById(radarAssets, DragonRadarAssetDefinition::getId);
		runtimeDragonAssets = copyById(dragonAssets, DragonAssetDefinition::getId);
	}

	private static void syncRegisteredBlocks(DragonBallSetDefinition target) {
		DragonBallSetDefinition bootstrap = BOOTSTRAP_BALL_SETS.get(target.getId());
		if (bootstrap != null) {
			for (int star : target.getStars()) {
				var registered = bootstrap.getRegisteredBlockObjectForStar(star);
				if (registered != null) target.setRegisteredBlock(star, registered);
			}
		}
		DragonBallSetDefinition current = runtimeBallSets.get(target.getId());
		if (current != null) {
			for (int star : target.getStars()) {
				if (target.getRegisteredBlockObjectForStar(star) == null) {
					var registered = current.getRegisteredBlockObjectForStar(star);
					if (registered != null) target.setRegisteredBlock(star, registered);
				}
			}
		}
	}

	private static <T> Map<String, T> copyById(Collection<T> definitions, java.util.function.Function<T, String> idGetter) {
		Map<String, T> map = new LinkedHashMap<>();
		for (T definition : definitions) map.put(idGetter.apply(definition), definition);
		return Map.copyOf(map);
	}

	public static void resetRuntimeDefinitions() { setRuntimeDefinitions(BOOTSTRAP_BALL_SETS.values(), BOOTSTRAP_RADARS.values(), BOOTSTRAP_DRAGONS.values(), BOOTSTRAP_RADAR_RECIPES.values(), BOOTSTRAP_BALL_SET_ASSETS.values(), BOOTSTRAP_RADAR_ASSETS.values(), BOOTSTRAP_DRAGON_ASSETS.values()); }
	public static Collection<DragonBallSetDefinition> getBootstrapBallSets() { return Collections.unmodifiableCollection(BOOTSTRAP_BALL_SETS.values()); }
	public static Collection<DragonRadarDefinition> getBootstrapRadars() { return Collections.unmodifiableCollection(BOOTSTRAP_RADARS.values()); }
	public static Collection<DragonDefinition> getBootstrapDragons() { return Collections.unmodifiableCollection(BOOTSTRAP_DRAGONS.values()); }
	public static Collection<DragonRadarRecipeDefinition> getBootstrapRadarRecipes() { return Collections.unmodifiableCollection(BOOTSTRAP_RADAR_RECIPES.values()); }
	public static Collection<DragonBallSetAssetDefinition> getBootstrapBallSetAssets() { return Collections.unmodifiableCollection(BOOTSTRAP_BALL_SET_ASSETS.values()); }
	public static Collection<DragonRadarAssetDefinition> getBootstrapRadarAssets() { return Collections.unmodifiableCollection(BOOTSTRAP_RADAR_ASSETS.values()); }
	public static Collection<DragonAssetDefinition> getBootstrapDragonAssets() { return Collections.unmodifiableCollection(BOOTSTRAP_DRAGON_ASSETS.values()); }
	public static Collection<DragonBallSetDefinition> getBallSets() { return Collections.unmodifiableCollection(runtimeBallSets.values()); }
	public static Collection<DragonRadarDefinition> getRadars() { return Collections.unmodifiableCollection(runtimeRadars.values()); }
	public static Collection<DragonDefinition> getDragons() { return Collections.unmodifiableCollection(runtimeDragons.values()); }
	public static Collection<DragonRadarRecipeDefinition> getRadarRecipes() { return Collections.unmodifiableCollection(runtimeRadarRecipes.values()); }
	public static Collection<DragonBallSetAssetDefinition> getBallSetAssets() { return Collections.unmodifiableCollection(runtimeBallSetAssets.values()); }
	public static Collection<DragonRadarAssetDefinition> getRadarAssets() { return Collections.unmodifiableCollection(runtimeRadarAssets.values()); }
	public static Collection<DragonAssetDefinition> getDragonAssets() { return Collections.unmodifiableCollection(runtimeDragonAssets.values()); }
	public static DragonBallSetDefinition getBallSet(String id) { return runtimeBallSets.get(id); }
	public static DragonRadarDefinition getRadar(String id) { return runtimeRadars.get(id); }
	public static DragonDefinition getDragon(String id) { return runtimeDragons.get(id); }
	public static DragonRadarRecipeDefinition getRadarRecipe(String id) { return runtimeRadarRecipes.get(id); }
	public static DragonBallSetAssetDefinition getBallSetAsset(String id) { return runtimeBallSetAssets.get(id); }
	public static DragonRadarAssetDefinition getRadarAsset(String id) { return runtimeRadarAssets.get(id); }
	public static DragonAssetDefinition getDragonAsset(String id) { return runtimeDragonAssets.get(id); }

	public static DragonBallSetDefinition getBallSetForBlock(Block block) {
		for (DragonBallSetDefinition definition : BOOTSTRAP_BALL_SETS.values()) {
			if (definition.getStarForBlock(block) != null) return runtimeBallSets.getOrDefault(definition.getId(), definition);
		}
		return null;
	}
	public static DragonDefinition getDragonForSetAndDimension(String ballSetId, ResourceKey<Level> dimension) {
		for (DragonDefinition definition : runtimeDragons.values()) if (definition.supportsBallSet(ballSetId) && definition.supportsDimension(dimension)) return definition;
		return null;
	}
	public static Collection<DragonBallSetDefinition> getBallSetsForDimension(ResourceKey<Level> dimension) {
		List<DragonBallSetDefinition> matches = new ArrayList<>();
		for (DragonBallSetDefinition definition : runtimeBallSets.values()) if (definition.supportsDimension(dimension)) matches.add(definition);
		return matches;
	}
	public static Collection<DragonBallSetDefinition> getBallSetsForRadar(String radarId, ResourceKey<Level> dimension) {
		DragonRadarDefinition radar = getRadar(radarId);
		if (radar == null || !radar.supportsDimension(dimension) || radar.getBallSetId() == null) return List.of();
		DragonBallSetDefinition definition = getBallSet(radar.getBallSetId());
		if (definition == null || !definition.supportsDimension(dimension)) return List.of();
		return List.of(definition);
	}
}
