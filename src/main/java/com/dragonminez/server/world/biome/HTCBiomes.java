package com.dragonminez.server.world.biome;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.*;

public class HTCBiomes {
	public static final ResourceKey<Biome> TIME_CHAMBER = ResourceKey.create(Registries.BIOME, new ResourceLocation(Reference.MOD_ID, "hyperbolic_time_chamber"));

	public static void bootstrap(BootstapContext<Biome> context) {
		context.register(TIME_CHAMBER, timeChamber(context));
	}

	private static Biome timeChamber(BootstapContext<Biome> context) {
		MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
		BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder(context.lookup(Registries.PLACED_FEATURE), context.lookup(Registries.CONFIGURED_CARVER));

		return new Biome.BiomeBuilder()
				.hasPrecipitation(false)
				.downfall(0.0f)
				.temperature(0.7f)
				.generationSettings(biomeBuilder.build())
				.mobSpawnSettings(spawnBuilder.build())
				.specialEffects(new BiomeSpecialEffects.Builder()
						.waterColor(0xDCF2FF)
						.waterFogColor(0xDCF2FF)
						.skyColor(0xF7FCFF)
						.grassColorOverride(0xDCF2FF)
						.foliageColorOverride(0xDCF2FF)
						.fogColor(0xDCF2FF)
						.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
						.build())
				.build();
	}
}