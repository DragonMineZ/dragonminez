package com.dragonminez.server.world.gen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.server.world.biome.NamekBiomes;
import com.dragonminez.server.world.dimension.NamekDimension;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.List;

public class NamekGeneration {
	public static final ResourceKey<LevelStem> NAMEK_STEM = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "namek"));
	public static final ResourceKey<NoiseGeneratorSettings> NAMEK_NOISE_SETTINGS = ResourceKey.create(Registries.NOISE_SETTINGS, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "namek"));

	public static void bootstrap(BootstapContext<LevelStem> context) {
		HolderGetter<Biome> biomeRegistry = context.lookup(Registries.BIOME);
		HolderGetter<DimensionType> dimTypes = context.lookup(Registries.DIMENSION_TYPE);
		HolderGetter<NoiseGeneratorSettings> noiseSettings = context.lookup(Registries.NOISE_SETTINGS);

		BiomeSource biomeSource = MultiNoiseBiomeSource.createFromList(
				new Climate.ParameterList<>(List.of(
						Pair.of(Climate.parameters(0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.0F, 0.0F), biomeRegistry.getOrThrow(NamekBiomes.AJISSA_PLAINS)),
						Pair.of(Climate.parameters(0.0F, 0.0F, 0.6F, 0.0F, 0.0F, 0.0F, 0.0F), biomeRegistry.getOrThrow(NamekBiomes.SACRED_LAND)),
						Pair.of(Climate.parameters(0.0F, 0.0F, -0.45F, 0.0F, 0.0F, 0.0F, 0.0F), biomeRegistry.getOrThrow(NamekBiomes.NAMEKIAN_RIVERS))
				))
		);

		ChunkGenerator chunkGenerator = new NoiseBasedChunkGenerator(biomeSource, noiseSettings.getOrThrow(NAMEK_NOISE_SETTINGS));
		context.register(NAMEK_STEM, new LevelStem(dimTypes.getOrThrow(NamekDimension.NAMEK_TYPE), chunkGenerator));
	}

	public static void bootstrapNoise(BootstapContext<NoiseGeneratorSettings> context) {
		SurfaceRules.RuleSource bedrockRule = SurfaceRules.ifTrue(
				SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.aboveBottom(0), VerticalAnchor.aboveBottom(5)),
				SurfaceRules.state(Blocks.BEDROCK.defaultBlockState())
		);

		SurfaceRules.RuleSource namekSurfaceRule = SurfaceRules.sequence(
				SurfaceRules.ifTrue(
						SurfaceRules.isBiome(NamekBiomes.AJISSA_PLAINS, NamekBiomes.NAMEKIAN_RIVERS),
						SurfaceRules.sequence(
								SurfaceRules.ifTrue(
										SurfaceRules.abovePreliminarySurface(),
										SurfaceRules.ifTrue(
												SurfaceRules.stoneDepthCheck(0, false, 0, CaveSurface.FLOOR),
												SurfaceRules.sequence(
														SurfaceRules.ifTrue(
																SurfaceRules.waterBlockCheck(-1, 0),
																SurfaceRules.state(MainBlocks.NAMEK_GRASS_BLOCK.get().defaultBlockState())
														),
														SurfaceRules.state(MainBlocks.NAMEK_DIRT.get().defaultBlockState())
												)
										)
								),
								SurfaceRules.ifTrue(
										SurfaceRules.yStartCheck(VerticalAnchor.absolute(50), 4),
										SurfaceRules.sequence(
												SurfaceRules.ifTrue(
														SurfaceRules.stoneDepthCheck(0, true, 5, CaveSurface.FLOOR),
														SurfaceRules.state(MainBlocks.NAMEK_DIRT.get().defaultBlockState())
												)
										)
								)
						)
				)
		);

		SurfaceRules.RuleSource sacredLandSurfaceRule = SurfaceRules.sequence(
				SurfaceRules.ifTrue(
						SurfaceRules.isBiome(NamekBiomes.SACRED_LAND),
						SurfaceRules.sequence(
								SurfaceRules.ifTrue(
										SurfaceRules.abovePreliminarySurface(),
										SurfaceRules.ifTrue(
												SurfaceRules.stoneDepthCheck(0, false, 0, CaveSurface.FLOOR),
												SurfaceRules.sequence(
														SurfaceRules.ifTrue(
																SurfaceRules.waterBlockCheck(0, 0),
																SurfaceRules.state(MainBlocks.NAMEK_SACRED_GRASS_BLOCK.get().defaultBlockState())
														),
														SurfaceRules.state(MainBlocks.NAMEK_DIRT.get().defaultBlockState())
												)
										)
								),
								SurfaceRules.ifTrue(
										SurfaceRules.yStartCheck(VerticalAnchor.absolute(50), 4),
										SurfaceRules.sequence(
												SurfaceRules.ifTrue(
														SurfaceRules.stoneDepthCheck(0, true, 5, CaveSurface.FLOOR),
														SurfaceRules.state(MainBlocks.NAMEK_DIRT.get().defaultBlockState())
												)
										)
								)
						)
				)
		);

		SurfaceRules.RuleSource deepslateRule = SurfaceRules.ifTrue(
				SurfaceRules.verticalGradient("deepslate", VerticalAnchor.absolute(0), VerticalAnchor.absolute(8)),
				SurfaceRules.state(MainBlocks.NAMEK_DEEPSLATE.get().defaultBlockState())
		);

		SurfaceRules.RuleSource finalRules = SurfaceRules.sequence(
				bedrockRule,
				namekSurfaceRule,
				sacredLandSurfaceRule,
				deepslateRule,
				SurfaceRules.state(MainBlocks.NAMEK_STONE.get().defaultBlockState())
		);

		NoiseSettings noiseSettings = NoiseSettings.create(-64, 384, 1, 2);

		HolderGetter<DensityFunction> densityFunctions = context.lookup(Registries.DENSITY_FUNCTION);
		HolderGetter<NormalNoise.NoiseParameters> noiseParams = context.lookup(Registries.NOISE);
		NoiseRouter router = NamekNoiseRouterData.createNamekRouter(densityFunctions, noiseParams);

		context.register(NAMEK_NOISE_SETTINGS, new NoiseGeneratorSettings(
				noiseSettings,
				MainBlocks.NAMEK_STONE.get().defaultBlockState(),
				MainBlocks.NAMEK_WATER_LIQUID.get().defaultBlockState(),
				router,
				finalRules,
				List.of(),
				64,
				false,
				true,
				false,
				false
		));
	}
}