package com.dragonminez.server.world.biome;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.*;

public class OtherworldBiomes {
	public static final ResourceKey<Biome> OTHERWORLD = ResourceKey.create(Registries.BIOME, new ResourceLocation(Reference.MOD_ID, "other_world"));

	public static void bootstrap(BootstapContext<Biome> context) {
		context.register(OTHERWORLD, otherworld(context));
	}

	private static Biome otherworld(BootstapContext<Biome> context) {
		MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();

		BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder(context.lookup(Registries.PLACED_FEATURE), context.lookup(Registries.CONFIGURED_CARVER));

		return new Biome.BiomeBuilder()
				.hasPrecipitation(false)
				.downfall(0.0f)
				.temperature(1.0f)
				.generationSettings(biomeBuilder.build())
				.mobSpawnSettings(spawnBuilder.build())
				.specialEffects(new BiomeSpecialEffects.Builder()
						.waterColor(4214155)
						.waterFogColor(4214120)
						.skyColor(0xBE55AA)
						.grassColorOverride(0xDCF2FF)
						.foliageColorOverride(0xDCF2FF)
						.fogColor(0xCE7EBD)
						.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
						.build())
				.build();
	}
}
