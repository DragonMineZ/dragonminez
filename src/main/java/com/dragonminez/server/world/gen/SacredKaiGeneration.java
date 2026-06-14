package com.dragonminez.server.world.gen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.server.world.biome.SacredKaiBiomes;
import com.dragonminez.server.world.dimension.SacredKaiDimension;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.List;

public class SacredKaiGeneration {
	public static final ResourceKey<LevelStem> SACREDKAI_STEM = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "sacredkaiplanet"));
	public static final ResourceKey<NoiseGeneratorSettings> SACREDKAI_NOISE_SETTINGS = ResourceKey.create(Registries.NOISE_SETTINGS, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "sacredkaiplanet"));

	public static void bootstrap(BootstapContext<LevelStem> context) {
		HolderGetter<Biome> biomeRegistry = context.lookup(Registries.BIOME);
		HolderGetter<DimensionType> dimTypes = context.lookup(Registries.DIMENSION_TYPE);
		HolderGetter<NoiseGeneratorSettings> noiseSettings = context.lookup(Registries.NOISE_SETTINGS);

		MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.createFromList(createSacredKaiBiomeParameters(biomeRegistry));

		ChunkGenerator chunkGenerator = new NoiseBasedChunkGenerator(
				biomeSource,
				noiseSettings.getOrThrow(SACREDKAI_NOISE_SETTINGS)
		);

		context.register(SACREDKAI_STEM, new LevelStem(
				dimTypes.getOrThrow(SacredKaiDimension.SACREDKAI_TYPE),
				chunkGenerator
		));
	}

	private static Climate.ParameterList<Holder<Biome>> createSacredKaiBiomeParameters(HolderGetter<Biome> biomeRegistry) {
		return new Climate.ParameterList<>(List.of(
				Pair.of(
						Climate.parameters(0.0F, 0.0F, 0.15F, 0.55F, 0.0F, 0.0F, 0.0F),
						biomeRegistry.getOrThrow(SacredKaiBiomes.SACREDKAI_PLAINS)
				),
				Pair.of(
						Climate.parameters(0.0F, 0.0F, 0.55F, -0.15F, 0.0F, 0.0F, 0.0F),
						biomeRegistry.getOrThrow(SacredKaiBiomes.SACREDKAI_HILLS)
				),
				Pair.of(
						Climate.parameters(0.0F, 0.0F, -0.45F, 0.0F, 0.0F, 0.0F, 0.0F),
						biomeRegistry.getOrThrow(SacredKaiBiomes.SACREDKAI_RIVERS)
				)
		));
	}

	public static void bootstrapNoise(BootstapContext<NoiseGeneratorSettings> context) {
		SurfaceRules.RuleSource bedrockRule = SurfaceRules.ifTrue(
				SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.aboveBottom(0), VerticalAnchor.aboveBottom(5)),
				SurfaceRules.state(Blocks.BEDROCK.defaultBlockState())
		);

		SurfaceRules.RuleSource grass = SurfaceRules.state(MainBlocks.SACRED_PLANET_GRASS_BLOCK.get().defaultBlockState());
		SurfaceRules.RuleSource sand = SurfaceRules.state(Blocks.SAND.defaultBlockState());
		SurfaceRules.RuleSource rockyDirt = SurfaceRules.state(MainBlocks.ROCKY_DIRT.get().defaultBlockState());
		SurfaceRules.RuleSource rockyStone = SurfaceRules.state(MainBlocks.ROCKY_STONE.get().defaultBlockState());

		SurfaceRules.RuleSource sacredKaiSurface = SurfaceRules.ifTrue(
				SurfaceRules.isBiome(SacredKaiBiomes.SACREDKAI_PLAINS, SacredKaiBiomes.SACREDKAI_HILLS, SacredKaiBiomes.SACREDKAI_RIVERS),
				SurfaceRules.ifTrue(
						SurfaceRules.abovePreliminarySurface(),
						SurfaceRules.sequence(
								SurfaceRules.ifTrue(
										SurfaceRules.ON_FLOOR,
										SurfaceRules.sequence(
												SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), grass),
												sand
										)
								),
								SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, rockyDirt)
						)
				)
		);

		SurfaceRules.RuleSource finalRules = SurfaceRules.sequence(
				bedrockRule,
				sacredKaiSurface,
				rockyStone
		);

		NoiseSettings noiseSettings = NoiseSettings.create(-64, 384, 1, 2);

		HolderGetter<DensityFunction> densityFunctions = context.lookup(Registries.DENSITY_FUNCTION);
		HolderGetter<NormalNoise.NoiseParameters> noiseParams = context.lookup(Registries.NOISE);
		NoiseRouter router = SacredKaiNoiseRouterData.createSacredKaiRouter(densityFunctions, noiseParams);

		context.register(SACREDKAI_NOISE_SETTINGS, new NoiseGeneratorSettings(
				noiseSettings,
				MainBlocks.ROCKY_STONE.get().defaultBlockState(),
				Blocks.WATER.defaultBlockState(),
				router,
				finalRules,
				List.of(),
				64,
				false,
				false,
				false,
				false
		));
	}
}
