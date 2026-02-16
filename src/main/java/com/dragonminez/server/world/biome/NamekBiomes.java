package com.dragonminez.server.world.biome;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.server.world.feature.NamekPlacedFeatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;

public class NamekBiomes {
	public static final ResourceKey<Biome> AJISSA_PLAINS = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "ajissa_plains"));
	public static final ResourceKey<Biome> SACRED_LAND = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "sacred_land"));
	public static final ResourceKey<Biome> NAMEKIAN_RIVERS = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "namekian_rivers"));

	public static void bootstrap(BootstapContext<Biome> context) {
		context.register(AJISSA_PLAINS, ajissaPlains(context));
		context.register(SACRED_LAND, sacredLand(context));
		context.register(NAMEKIAN_RIVERS, namekRiver(context));
	}

    private static void addWaterMobs(MobSpawnSettings.Builder builder) {
        builder.addSpawn(MobCategory.WATER_CREATURE, new MobSpawnSettings.SpawnerData(EntityType.SQUID, 10, 1, 4));

        builder.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.SALMON, 15, 1, 5));
        builder.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.COD, 10, 1, 5));
        builder.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.PUFFERFISH, 5, 1, 3));
        builder.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.TROPICAL_FISH, 8, 1, 4));

        builder.addSpawn(MobCategory.AXOLOTLS, new MobSpawnSettings.SpawnerData(EntityType.AXOLOTL, 5, 1, 2));

        builder.addMobCharge(EntityType.SQUID, 0.7D, 0.15D);
        builder.addMobCharge(EntityType.AXOLOTL, 1.0D, 0.12D);
    }

	private static Biome ajissaPlains(BootstapContext<Biome> context) {
		var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
		var carvers = context.lookup(Registries.CONFIGURED_CARVER);

		MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
		BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder(placedFeatures, carvers);

		//addWaterMobs(spawnBuilder);
        spawnBuilder.addMobCharge(MainEntities.SAGA_FRIEZA_SOLDIER.get(), 0.9, 0.1D);
        spawnBuilder.addMobCharge(MainEntities.SAGA_FRIEZA_SOLDIER2.get(), 0.9, 0.1D);
        spawnBuilder.addMobCharge(MainEntities.SAGA_FRIEZA_SOLDIER3.get(), 0.9, 0.1D);

        biomeBuilder.addFeature(GenerationStep.Decoration.LAKES, NamekPlacedFeatures.NAMEK_LAKE_LAVA_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.FLUID_SPRINGS, NamekPlacedFeatures.NAMEK_SPRING_LAVA_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.FLUID_SPRINGS, NamekPlacedFeatures.NAMEK_SPRING_WATER_PLACED);

		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_COAL_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_COPPER_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_IRON_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_GOLD_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_REDSTONE_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_EMERALD_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_LAPIS_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_DIAMOND_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_KIKONO_ORE_PLACED);

		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, NamekPlacedFeatures.NAMEK_PATCH_GRASS_PLAIN);
		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, NamekPlacedFeatures.NAMEK_PLAINS_FLOWERS);

		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, NamekPlacedFeatures.AJISSA_TREE_PLACED);

		return biome(spawnBuilder, biomeBuilder, 6530427);
	}

	private static Biome sacredLand(BootstapContext<Biome> context) {
		var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
		var carvers = context.lookup(Registries.CONFIGURED_CARVER);

		MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
		BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder(placedFeatures, carvers);

		//addWaterMobs(spawnBuilder);
        spawnBuilder.addMobCharge(MainEntities.SAGA_FRIEZA_SOLDIER.get(), 0.9, 0.1D);
        spawnBuilder.addMobCharge(MainEntities.SAGA_FRIEZA_SOLDIER2.get(), 0.9, 0.1D);
        spawnBuilder.addMobCharge(MainEntities.SAGA_FRIEZA_SOLDIER3.get(), 0.9, 0.1D);

		biomeBuilder.addFeature(GenerationStep.Decoration.LAKES, NamekPlacedFeatures.NAMEK_LAKE_LAVA_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.FLUID_SPRINGS, NamekPlacedFeatures.NAMEK_SPRING_LAVA_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.FLUID_SPRINGS, NamekPlacedFeatures.NAMEK_SPRING_WATER_PLACED);

		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_COAL_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_COPPER_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_IRON_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_GOLD_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_REDSTONE_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_EMERALD_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_LAPIS_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_DIAMOND_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_KIKONO_ORE_PLACED);

		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, NamekPlacedFeatures.NAMEK_PATCH_SACRED_GRASS_PLAIN);
		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, NamekPlacedFeatures.NAMEK_SACRED_FLOWERS);

		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, NamekPlacedFeatures.SACRED_TREE_PLACED);

		return biome(spawnBuilder, biomeBuilder, 6530427);
	}

	private static Biome namekRiver(BootstapContext<Biome> context) {
		var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
		var carvers = context.lookup(Registries.CONFIGURED_CARVER);

		MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
		BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder(placedFeatures, carvers);

		addWaterMobs(spawnBuilder);

		biomeBuilder.addFeature(GenerationStep.Decoration.FLUID_SPRINGS, NamekPlacedFeatures.NAMEK_SPRING_WATER_PLACED);

		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_COAL_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_COPPER_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_IRON_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_GOLD_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_REDSTONE_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_EMERALD_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_LAPIS_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_DIAMOND_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_KIKONO_ORE_PLACED);

		return biome(spawnBuilder, biomeBuilder, 6530427);
	}

	private static Biome biome(MobSpawnSettings.Builder spawnBuilder, BiomeGenerationSettings.Builder biomeBuilder, int skyColor) {
		return new Biome.BiomeBuilder()
				.hasPrecipitation(false)
				.temperature(2.0f)
				.downfall(0.0f)
				.specialEffects(new BiomeSpecialEffects.Builder()
						.waterColor(4159204)
						.waterFogColor(329011)
						.skyColor(skyColor)
						.fogColor(12638463)
						.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
						.build())
				.mobSpawnSettings(spawnBuilder.build())
				.generationSettings(biomeBuilder.build())
				.build();
	}
}