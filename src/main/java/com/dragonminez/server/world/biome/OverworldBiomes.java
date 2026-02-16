package com.dragonminez.server.world.biome;

import com.dragonminez.Reference;
import com.dragonminez.server.world.feature.OverworldPlacedFeatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;

public class OverworldBiomes {
	public static final ResourceKey<Biome> ROCKY = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "rocky"));

	public static void bootstrap(BootstapContext<Biome> context) {
		context.register(ROCKY, rockyBiome(context));
	}

	public static Biome rockyBiome(BootstapContext<Biome> context) {
		MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.commonSpawns(spawnBuilder);
		spawnBuilder.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.SKELETON, 100, 4, 4));

		BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder(context.lookup(Registries.PLACED_FEATURE), context.lookup(Registries.CONFIGURED_CARVER));

		BiomeDefaultFeatures.addDefaultCarversAndLakes(biomeBuilder);
		BiomeDefaultFeatures.addDefaultCrystalFormations(biomeBuilder);
		BiomeDefaultFeatures.addDefaultOres(biomeBuilder);
		BiomeDefaultFeatures.addDefaultSoftDisks(biomeBuilder);
		BiomeDefaultFeatures.addDesertVegetation(biomeBuilder);
		BiomeDefaultFeatures.addDesertExtraVegetation(biomeBuilder);

		biomeBuilder.addFeature(GenerationStep.Decoration.RAW_GENERATION,
				context.lookup(Registries.PLACED_FEATURE).getOrThrow(OverworldPlacedFeatures.STONE_SPIKE_PLACED_KEY));

        biomeBuilder.addFeature(GenerationStep.Decoration.RAW_GENERATION,
                context.lookup(Registries.PLACED_FEATURE).getOrThrow(OverworldPlacedFeatures.ROCKY_PEAK_PLACED_KEY));

		BiomeSpecialEffects.Builder effectsBuilder = new BiomeSpecialEffects.Builder()
				.waterColor(0x3F76E4)
				.waterFogColor(0x050533)
				.skyColor(0x77A8FF)
				.fogColor(0xC0D8FF)
				.grassColorOverride(0x948666)
				.foliageColorOverride(0x948666);

		return new Biome.BiomeBuilder()
				.hasPrecipitation(false)
				.temperature(2.0F)
				.downfall(0.0F)
				.specialEffects(effectsBuilder.build())
				.mobSpawnSettings(spawnBuilder.build())
				.generationSettings(biomeBuilder.build())
				.build();
	}
}
