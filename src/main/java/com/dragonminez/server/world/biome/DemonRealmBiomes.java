package com.dragonminez.server.world.biome;

import com.dragonminez.Reference;
import com.dragonminez.server.world.feature.DemonRealmFeatures;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;

public class DemonRealmBiomes {
	public static final ResourceKey<Biome> FIRST_DEMON_WORLD = key("first_demon_world");
	public static final ResourceKey<Biome> SECOND_DEMON_WORLD = key("second_demon_world");
	public static final ResourceKey<Biome> THIRD_DEMON_WORLD = key("third_demon_world");

	public static final int FIRST_SKY = 0xE2622A;
	public static final int SECOND_SKY = 0xDCF09C;
	public static final int THIRD_SKY = 0xF2B9EE;

	public static void bootstrap(BootstapContext<Biome> context) {
		context.register(FIRST_DEMON_WORLD, firstWorld(context));
		context.register(SECOND_DEMON_WORLD, secondWorld(context));
		context.register(THIRD_DEMON_WORLD, thirdWorld(context));
	}

	private static Biome firstWorld(BootstapContext<Biome> context) {
		BiomeGenerationSettings.Builder features = features(context);
		features.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, DemonRealmFeatures.ROCK_SPIRE_PLACED);
		features.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, DemonRealmFeatures.CRYSTAL_SPIRE_PLACED);
		features.addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, DemonRealmFeatures.AMBIENT_LIGHT_PLACED);

		return biome(features, 2.0f, new BiomeSpecialEffects.Builder()
				.waterColor(0x8A2A1E)
				.waterFogColor(0x3A0C08)
				.skyColor(FIRST_SKY)
				.fogColor(FIRST_SKY)
				.grassColorOverride(0x6A3A2A)
				.foliageColorOverride(0x6A3A2A)
				.ambientParticle(new AmbientParticleSettings(ParticleTypes.CRIMSON_SPORE, 0.01f)));
	}

	private static Biome secondWorld(BootstapContext<Biome> context) {
		BiomeGenerationSettings.Builder features = features(context);
		features.addFeature(GenerationStep.Decoration.LAKES, DemonRealmFeatures.SEA_PLACED);
		features.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, DemonRealmFeatures.LOWER_SHIELD_PLACED);
		features.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, DemonRealmFeatures.MAJILITE_ORE_PLACED);
		features.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, DemonRealmFeatures.AJISSA_TREE_PLACED);
		features.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, DemonRealmFeatures.NAMEK_GRASS_PLACED);
		features.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, DemonRealmFeatures.NAMEK_FLOWERS_PLACED);

		return biome(features, 0.8f, new BiomeSpecialEffects.Builder()
				.waterColor(0x5AB596)
				.waterFogColor(0x1E5A48)
				.skyColor(SECOND_SKY)
				.fogColor(SECOND_SKY)
				.grassColorOverride(0x6DBF4B)
				.foliageColorOverride(0x6DBF4B));
	}

	private static Biome thirdWorld(BootstapContext<Biome> context) {
		BiomeGenerationSettings.Builder features = features(context);
		features.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, DemonRealmFeatures.UPPER_SHIELD_PLACED);
		features.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, DemonRealmFeatures.GAS_VENT_PLACED);
		features.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, DemonRealmFeatures.MAKAI_BUSH_PLACED);
		features.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, DemonRealmFeatures.MAKAI_SHRUBS_PLACED);

		return biome(features, 1.2f, new BiomeSpecialEffects.Builder()
				.waterColor(0xC78BD0)
				.waterFogColor(0x5A2A66)
				.skyColor(THIRD_SKY)
				.fogColor(THIRD_SKY)
				.grassColorOverride(0x86AB39)
				.foliageColorOverride(0x86AB39)
				.ambientParticle(new AmbientParticleSettings(ParticleTypes.WHITE_ASH, 0.008f)));
	}

	private static BiomeGenerationSettings.Builder features(BootstapContext<Biome> context) {
		return new BiomeGenerationSettings.Builder(context.lookup(Registries.PLACED_FEATURE), context.lookup(Registries.CONFIGURED_CARVER));
	}

	private static Biome biome(BiomeGenerationSettings.Builder features, float temperature, BiomeSpecialEffects.Builder effects) {
		return new Biome.BiomeBuilder()
				.hasPrecipitation(false)
				.downfall(0.0f)
				.temperature(temperature)
				.generationSettings(features.build())
				.mobSpawnSettings(new MobSpawnSettings.Builder().build())
				.specialEffects(effects.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS).build())
				.build();
	}

	private static ResourceKey<Biome> key(String name) {
		return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}
}
