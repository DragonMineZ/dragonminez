package com.dragonminez.server.world.gen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.server.world.biome.OtherworldBiomes;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.List;

public class OtherworldGeneration {
	public static final ResourceKey<LevelStem> OTHERWORLD_STEM = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "otherworld"));
	public static final ResourceKey<NoiseGeneratorSettings> OTHERWORLD_NOISE_SETTINGS = ResourceKey.create(Registries.NOISE_SETTINGS, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "otherworld"));

	public static final int WORLD_BOTTOM = -64;
	public static final int WORLD_TOP = 384;

	public static final int HELL_BASE_HEIGHT = -14;
	public static final int HELL_PEAK_BAND = 24;
	public static final int HELL_CREST_BAND = 8;
	public static final int HELL_VALLEY_BAND = -30;
	public static final int HELL_CEILING = 96;

	public static final int LOWER_CLOUD_DECK = 108;
	public static final int PALACE_LEVEL = 112;
	public static final int UPPER_CLOUD_DECK = 267;
	public static final int TOURNAMENT_LEVEL = 271;

	public static final ResourceKey<NormalNoise.NoiseParameters> CLOUD_BILLOW = noiseKey("otherworld_cloud_billow");
	public static final ResourceKey<NormalNoise.NoiseParameters> CLOUD_PUFF = noiseKey("otherworld_cloud_puff");

	// Alturas relativas al nivel de estructuras del manto (PALACE_LEVEL / TOURNAMENT_LEVEL).
	// El techo nunca baja de CLOUD_TOP_MIN ni supera el nivel de estructuras, y el vientre nunca sube de
	// CLOUD_BELLY_MAX ni baja de CLOUD_BAND_BOTTOM: el manto no tiene agujeros y no invade el Infierno.
	private static final int CLOUD_TOP_MIN = -9;
	private static final int CLOUD_BELLY_MAX = -11;
	private static final int CLOUD_BAND_BOTTOM = -16;
	private static final double CLOUD_BILLOW_HEIGHT = 6.0;
	private static final double CLOUD_PUFF_HEIGHT = 3.0;
	private static final double CLOUD_BELLY_BILLOW_DEPTH = 3.5;
	private static final double CLOUD_BELLY_PUFF_DEPTH = 1.5;
	private static final double CLOUD_BILLOW_GAIN = 2.0;
	private static final double CLOUD_PUFF_GAIN = 2.5;
	private static final double CLOUD_LUMP_THRESHOLD = 0.35;

	private static final double MOUNTAIN_HEIGHT = 70.0;
	private static final double NEEDLE_HEIGHT = 34.0;

	public static void bootstrap(BootstapContext<LevelStem> context) {
		HolderGetter<Biome> biomeRegistry = context.lookup(Registries.BIOME);
		HolderGetter<DimensionType> dimTypes = context.lookup(Registries.DIMENSION_TYPE);
		HolderGetter<NoiseGeneratorSettings> noiseSettings = context.lookup(Registries.NOISE_SETTINGS);

		BiomeSource biomeSource = new FixedBiomeSource(biomeRegistry.getOrThrow(OtherworldBiomes.OTHERWORLD));

		ChunkGenerator chunkGenerator = new NoiseBasedChunkGenerator(
				biomeSource,
				noiseSettings.getOrThrow(OTHERWORLD_NOISE_SETTINGS)
		);

		context.register(OTHERWORLD_STEM, new LevelStem(dimTypes.getOrThrow(OtherworldDimension.OTHERWORLD_TYPE), chunkGenerator));
	}

	public static void bootstrapNoise(BootstapContext<NoiseGeneratorSettings> context) {
		HolderGetter<NormalNoise.NoiseParameters> noiseParams = context.lookup(Registries.NOISE);

		SurfaceRules.RuleSource bedrock = SurfaceRules.ifTrue(
				SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.aboveBottom(0), VerticalAnchor.aboveBottom(3)),
				SurfaceRules.state(Blocks.BEDROCK.defaultBlockState())
		);

		SurfaceRules.RuleSource calcite = SurfaceRules.state(Blocks.CALCITE.defaultBlockState());
		SurfaceRules.RuleSource hellStone = SurfaceRules.state(MainBlocks.HELL_STONE.get().defaultBlockState());
		SurfaceRules.RuleSource hellGround = SurfaceRules.state(MainBlocks.HELL_GROUND.get().defaultBlockState());
		SurfaceRules.RuleSource hellDeepstone = SurfaceRules.state(MainBlocks.HELL_DEEPSTONE.get().defaultBlockState());
		SurfaceRules.RuleSource amethyst = SurfaceRules.state(Blocks.AMETHYST_BLOCK.defaultBlockState());
		SurfaceRules.RuleSource froglight = SurfaceRules.state(Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState());

		SurfaceRules.RuleSource peak = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE_SECONDARY, 0.62, 2.0), amethyst),
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.NETHER_STATE_SELECTOR, 0.15, 2.0), hellStone),
				hellDeepstone
		);

		SurfaceRules.RuleSource crest = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.NETHER_STATE_SELECTOR, 0.1, 2.0), calcite),
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.35, 2.0), hellGround),
				hellStone
		);

		SurfaceRules.RuleSource slope = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE_SECONDARY, 0.85, 2.0), froglight),
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.78, 2.0), calcite),
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.NETHER_STATE_SELECTOR, -0.05, 2.0), hellGround),
				hellStone
		);

		SurfaceRules.RuleSource valley = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.NETHER_STATE_SELECTOR, 0.15, 2.0), hellStone),
				hellDeepstone
		);

		SurfaceRules.RuleSource underFloor = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.NETHER_STATE_SELECTOR, 0.3, 2.0), hellDeepstone),
				hellStone
		);

		SurfaceRules.RuleSource core = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(-48), 0)), hellDeepstone),
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.25, 2.0), hellDeepstone),
				hellStone
		);

		SurfaceRules.RuleSource hell = SurfaceRules.ifTrue(
				SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(HELL_CEILING), 0)),
				SurfaceRules.sequence(
						SurfaceRules.ifTrue(
								SurfaceRules.ON_FLOOR,
								SurfaceRules.sequence(
										SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(HELL_PEAK_BAND), 0), peak),
										SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(HELL_CREST_BAND), 0), crest),
										SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(HELL_VALLEY_BAND), 0)), valley),
										slope
								)
						),
						SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, underFloor),
						core
				)
		);

		SurfaceRules.RuleSource otherWorldRules = SurfaceRules.sequence(bedrock, hell);

		NoiseSettings noiseSettings = NoiseSettings.create(WORLD_BOTTOM, WORLD_TOP - WORLD_BOTTOM, 1, 1);

		context.register(OTHERWORLD_NOISE_SETTINGS, new NoiseGeneratorSettings(
				noiseSettings,
				MainBlocks.OTHERWORLD_CLOUD.get().defaultBlockState(),
				Blocks.AIR.defaultBlockState(),
				createRouter(noiseParams),
				otherWorldRules,
				List.of(),
				0,
				false,
				false,
				false,
				false
		));
	}

	private static NoiseRouter createRouter(HolderGetter<NormalNoise.NoiseParameters> noiseParams) {
		DensityFunction constantNegative = DensityFunctions.constant(-1.0);
		DensityFunction blockY = DensityFunctions.yClampedGradient(WORLD_BOTTOM, WORLD_TOP, WORLD_BOTTOM, WORLD_TOP);

		DensityFunction wideDunes = scaled(20.0, DensityFunctions.noise(noiseParams.getOrThrow(Noises.CONTINENTALNESS), 0.4, 0.0));
		DensityFunction midDunes = scaled(8.0, DensityFunctions.noise(noiseParams.getOrThrow(Noises.EROSION), 0.9, 0.0));
		DensityFunction ripples = scaled(3.0, DensityFunctions.noise(noiseParams.getOrThrow(Noises.SURFACE), 1.0, 0.0));

		DensityFunction mountainMask = DensityFunctions.add(
				DensityFunctions.noise(noiseParams.getOrThrow(Noises.CONTINENTALNESS_LARGE), 3.0, 0.0),
				DensityFunctions.constant(0.15)
		).clamp(0.0, 1.0);
		DensityFunction mountains = scaled(MOUNTAIN_HEIGHT, DensityFunctions.mul(
				mountainMask,
				ridged(DensityFunctions.noise(noiseParams.getOrThrow(Noises.RIDGE), 1.6, 0.0)).cube()
		));

		DensityFunction needleMask = DensityFunctions.add(
				DensityFunctions.noise(noiseParams.getOrThrow(Noises.EROSION_LARGE), 5.0, 0.0),
				DensityFunctions.constant(-0.2)
		).clamp(0.0, 1.0);
		DensityFunction needles = scaled(NEEDLE_HEIGHT, DensityFunctions.mul(
				needleMask,
				ridged(DensityFunctions.noise(noiseParams.getOrThrow(Noises.SURFACE_SECONDARY), 5.0, 0.0)).cube().cube()
		));

		DensityFunction hellHeight = DensityFunctions.add(
				DensityFunctions.add(DensityFunctions.constant(HELL_BASE_HEIGHT), DensityFunctions.add(wideDunes, DensityFunctions.add(midDunes, ripples))),
				DensityFunctions.add(mountains, needles)
		).clamp(WORLD_BOTTOM + 2, HELL_CEILING - 8);

		DensityFunction hellTerrain = DensityFunctions.add(hellHeight, scaled(-1.0, blockY));

		DensityFunction decks = DensityFunctions.max(
				cloudDeck(blockY, PALACE_LEVEL, 0.0, noiseParams),
				cloudDeck(blockY, TOURNAMENT_LEVEL, 4096.0, noiseParams)
		);

		DensityFunction terrain = DensityFunctions.max(hellTerrain, decks);

		return new NoiseRouter(
				constantNegative,
				constantNegative,
				constantNegative,
				constantNegative,
				constantNegative,
				constantNegative,
				constantNegative,
				constantNegative,
				terrain,
				constantNegative,
				terrain,
				terrain,
				constantNegative,
				constantNegative,
				constantNegative
		);
	}

	private static DensityFunction ridged(DensityFunction noise) {
		return DensityFunctions.add(DensityFunctions.constant(1.0), scaled(-1.0, noise.abs()));
	}

	public static void bootstrapNoiseParameters(BootstapContext<NormalNoise.NoiseParameters> context) {
		context.register(CLOUD_BILLOW, new NormalNoise.NoiseParameters(-5, 1.0, 0.5));
		context.register(CLOUD_PUFF, new NormalNoise.NoiseParameters(-4, 1.0, 0.5));
	}

	private static DensityFunction cloudDeck(DensityFunction blockY, int surface, double offset, HolderGetter<NormalNoise.NoiseParameters> noiseParams) {
		Holder<NormalNoise.NoiseParameters> billowNoise = noiseParams.getOrThrow(CLOUD_BILLOW);
		Holder<NormalNoise.NoiseParameters> puffNoise = noiseParams.getOrThrow(CLOUD_PUFF);

		DensityFunction top = DensityFunctions.add(
				DensityFunctions.constant(surface + CLOUD_TOP_MIN),
				DensityFunctions.add(
						scaled(CLOUD_BILLOW_HEIGHT, dome(billowNoise, CLOUD_BILLOW_GAIN, offset)),
						scaled(CLOUD_PUFF_HEIGHT, dome(puffNoise, CLOUD_PUFF_GAIN, offset))
				)
		);
		DensityFunction belly = DensityFunctions.add(
				DensityFunctions.constant(surface + CLOUD_BELLY_MAX),
				DensityFunctions.add(
						scaled(-CLOUD_BELLY_BILLOW_DEPTH, dome(billowNoise, CLOUD_BILLOW_GAIN, offset + 1777.0)),
						scaled(-CLOUD_BELLY_PUFF_DEPTH, dome(puffNoise, CLOUD_PUFF_GAIN, offset + 1777.0))
				)
		);
		DensityFunction body = DensityFunctions.min(
				DensityFunctions.add(top, scaled(-1.0, blockY)),
				DensityFunctions.add(blockY, scaled(-1.0, belly))
		);

		int bandBottom = surface + CLOUD_BAND_BOTTOM;
		int bellyMax = surface + CLOUD_BELLY_MAX;
		DensityFunction lumpFade = DensityFunctions.max(
				DensityFunctions.yClampedGradient(bandBottom, bandBottom + 4, 1.0, 0.0),
				DensityFunctions.yClampedGradient(bellyMax, bellyMax + 2, 0.0, 2.0)
		);
		DensityFunction lumps = DensityFunctions.add(
				DensityFunctions.add(
						DensityFunctions.noise(puffNoise, 1.0, 4.0),
						DensityFunctions.constant(-CLOUD_LUMP_THRESHOLD)
				),
				scaled(-1.0, lumpFade)
		);

		return DensityFunctions.rangeChoice(blockY, bandBottom, surface, DensityFunctions.max(body, lumps), DensityFunctions.constant(-1.0));
	}

	// Cúpula de cúmulo: |ruido| deja pliegues finos entre masas y 1-(1-a)^2 redondea y acota la cima.
	private static DensityFunction dome(Holder<NormalNoise.NoiseParameters> noise, double gain, double offset) {
		DensityFunction billow = scaled(gain, DensityFunctions.shiftedNoise2d(DensityFunctions.constant(offset), DensityFunctions.constant(-offset), 1.0, noise).abs()).clamp(0.0, 1.0);
		DensityFunction hollow = DensityFunctions.add(DensityFunctions.constant(1.0), scaled(-1.0, billow)).square();
		return DensityFunctions.add(DensityFunctions.constant(1.0), scaled(-1.0, hollow));
	}

	private static ResourceKey<NormalNoise.NoiseParameters> noiseKey(String name) {
		return ResourceKey.create(Registries.NOISE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}

	private static DensityFunction scaled(double factor, DensityFunction function) {
		return DensityFunctions.mul(DensityFunctions.constant(factor), function);
	}
}
