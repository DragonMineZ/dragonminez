package com.dragonminez.server.world.biome;

import com.dragonminez.Reference;
import com.dragonminez.server.world.feature.SacredKaiPlacedFeatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;

public class SacredKaiBiomes {
	public static final ResourceKey<Biome> SACREDKAI_PLAINS = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "sacredkai_plains"));
	public static final ResourceKey<Biome> SACREDKAI_HILLS = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "sacredkai_hills"));
	public static final ResourceKey<Biome> SACREDKAI_RIVERS = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "sacredkai_rivers"));

	public static void bootstrap(BootstapContext<Biome> context) {
		context.register(SACREDKAI_PLAINS, plains(context));
		context.register(SACREDKAI_HILLS, hills(context));
		context.register(SACREDKAI_RIVERS, rivers(context));
	}

	private static void addNormalCaves(BiomeGenerationSettings.Builder builder, BootstapContext<Biome> context) {
		var carvers = context.lookup(Registries.CONFIGURED_CARVER);
		builder.addCarver(GenerationStep.Carving.AIR, carvers.getOrThrow(Carvers.CAVE));
		builder.addCarver(GenerationStep.Carving.AIR, carvers.getOrThrow(Carvers.CAVE_EXTRA_UNDERGROUND));
		builder.addCarver(GenerationStep.Carving.AIR, carvers.getOrThrow(Carvers.CANYON));
	}

	private static Biome plains(BootstapContext<Biome> context) {
		var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
		var carvers = context.lookup(Registries.CONFIGURED_CARVER);

		MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();

		BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
		addNormalCaves(biomeBuilder, context);

		biomeBuilder.addFeature(GenerationStep.Decoration.LAKES, SacredKaiPlacedFeatures.WATER_LAKE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, SacredKaiPlacedFeatures.SAND_PATCH_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, SacredKaiPlacedFeatures.ROCK_CLUSTER_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, SacredKaiPlacedFeatures.OAK_TREE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, SacredKaiPlacedFeatures.GRASS_PATCH_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, SacredKaiPlacedFeatures.FLOWERS_PLACED);

		return biome(spawnBuilder, biomeBuilder);
	}

	private static Biome hills(BootstapContext<Biome> context) {
		var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
		var carvers = context.lookup(Registries.CONFIGURED_CARVER);

		MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();

		BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
		addNormalCaves(biomeBuilder, context);

		biomeBuilder.addFeature(GenerationStep.Decoration.RAW_GENERATION, SacredKaiPlacedFeatures.GRASSY_PEAK_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.RAW_GENERATION, SacredKaiPlacedFeatures.GRASSY_CLIFF_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.RAW_GENERATION, SacredKaiPlacedFeatures.KARST_PILLAR_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.RAW_GENERATION, SacredKaiPlacedFeatures.STONE_SPIKE_PLACED);

		biomeBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, SacredKaiPlacedFeatures.SAND_PATCH_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, SacredKaiPlacedFeatures.ROCK_CLUSTER_SPARSE_PLACED);

		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, SacredKaiPlacedFeatures.OAK_TREE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, SacredKaiPlacedFeatures.GRASS_PATCH_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, SacredKaiPlacedFeatures.FLOWERS_PLACED);

		return biome(spawnBuilder, biomeBuilder);
	}

	private static Biome rivers(BootstapContext<Biome> context) {
		var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
		var carvers = context.lookup(Registries.CONFIGURED_CARVER);

		MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();

		BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
		addNormalCaves(biomeBuilder, context);

		biomeBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, SacredKaiPlacedFeatures.SAND_PATCH_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, SacredKaiPlacedFeatures.GRASS_PATCH_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, SacredKaiPlacedFeatures.FLOWERS_PLACED);

		return biome(spawnBuilder, biomeBuilder);
	}

	private static Biome biome(MobSpawnSettings.Builder spawnBuilder, BiomeGenerationSettings.Builder biomeBuilder) {
		return new Biome.BiomeBuilder()
				.hasPrecipitation(false)
				.temperature(2.0f)
				.downfall(0.0f)
				.specialEffects(new BiomeSpecialEffects.Builder()
						.waterColor(0x9C82E8)
						.waterFogColor(0x6A55A0)
						.skyColor(0x9A6BE3)
						.fogColor(0xD7A9E6)
						.grassColorOverride(0x83E03B)
						.foliageColorOverride(0x7BD636)
						.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
						.build())
				.mobSpawnSettings(spawnBuilder.build())
				.generationSettings(biomeBuilder.build())
				.build();
	}
}
