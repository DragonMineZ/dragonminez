package com.dragonminez.server.world.region;

import com.dragonminez.Reference;
import com.dragonminez.server.world.biome.OverworldBiomes;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

public class OverworldRegion extends Region {
	public OverworldRegion(int weight) {
		super(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "overworld_region"), RegionType.OVERWORLD, weight);
	}

	@Override
	public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
		this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
			builder.replaceBiome(Biomes.DESERT, OverworldBiomes.ROCKY);
		});
	}
}

