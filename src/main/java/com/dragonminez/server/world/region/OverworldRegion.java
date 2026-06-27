package com.dragonminez.server.world.region;

import com.dragonminez.Reference;
import com.dragonminez.server.world.biome.OverworldBiomes;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

/**
 * TerraBlender region that ADDS the rocky biome to the overworld without replacing any vanilla biome.
 *
 * We first emit the untouched vanilla overworld layout ({@code addModifiedVanillaOverworldBiomes} with no
 * edits) and then append extra parameter points for {@link OverworldBiomes#ROCKY}. Because both go through
 * the same {@code mapper}, the rocky biome simply joins the vanilla list and wins only where its climate
 * points are nearest — i.e. the hot, dry, inland, eroded niche — while deserts, savannas and badlands keep
 * generating everywhere they did before.
 */
public class OverworldRegion extends Region {

	// Warm + hot temperatures and the three driest humidity bands: a wide niche so rocky is easy to find.
	private static final Climate.Parameter TEMPERATURE = Climate.Parameter.span(0.2F, 1.0F);
	private static final Climate.Parameter HUMIDITY = Climate.Parameter.span(-1.0F, -0.1F);
	// Near + mid + far inland, so it forms plateaus rather than coasts.
	private static final Climate.Parameter CONTINENTALNESS = Climate.Parameter.span(-0.11F, 1.0F);
	// Low-to-mid erosion = high, rugged, eroded terrain (the badlands/mesa slots).
	private static final Climate.Parameter[] EROSIONS = new Climate.Parameter[]{
			Climate.Parameter.span(-1.0F, -0.78F),
			Climate.Parameter.span(-0.78F, -0.375F),
			Climate.Parameter.span(-0.375F, -0.2225F)
	};
	private static final Climate.Parameter WEIRDNESS = Climate.Parameter.span(-1.0F, 1.0F);
	// Surface only: depth 0.0 (and no deep point), so rocky never gets assigned underground/in caves.
	private static final Climate.Parameter DEPTH_SURFACE = Climate.Parameter.point(0.0F);

	public OverworldRegion(int weight) {
		super(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "overworld_region"), RegionType.OVERWORLD, weight);
	}

	@Override
	public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
		// Keep the full vanilla overworld layout untouched (no replacements/removals).
		this.addModifiedVanillaOverworldBiomes(mapper, builder -> {});

		// Add rocky on top of it, surface only, across the rugged hot/dry erosion bands.
		for (Climate.Parameter erosion : EROSIONS) {
			this.addBiome(mapper, Climate.parameters(
					TEMPERATURE, HUMIDITY, CONTINENTALNESS, erosion,
					DEPTH_SURFACE, WEIRDNESS, 0.0F), OverworldBiomes.ROCKY);
		}
	}
}
