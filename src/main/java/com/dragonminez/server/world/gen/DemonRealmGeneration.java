package com.dragonminez.server.world.gen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.block.custom.DemonRealmBlocks;
import com.dragonminez.server.world.biome.DemonRealmBiomes;
import com.dragonminez.server.world.dimension.DemonRealmDimension;
import com.dragonminez.server.world.feature.DemonRealmFeatures;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CubicSpline;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.ArrayList;
import java.util.List;

public class DemonRealmGeneration {
	public static final ResourceKey<LevelStem> DEMON_REALM_STEM = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "demon_realm"));
	public static final ResourceKey<NoiseGeneratorSettings> DEMON_REALM_NOISE_SETTINGS = ResourceKey.create(Registries.NOISE_SETTINGS, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "demon_realm"));

	public static final int WORLD_BOTTOM = -64;
	public static final int WORLD_TOP = 768;

	public static final int FIRST_BASE = 64;
	public static final int FIRST_CLOUDS = 200;

	public static final int CRUST_A_BOTTOM = 252;
	public static final int CRUST_A_CORE = 262;
	public static final int CRUST_A_TOP = 300;

	public static final int SECOND_SEA = 318;
	public static final int SECOND_BAND_TOP = 452;
	public static final int SECOND_CLOUDS = 436;

	public static final int CRUST_B_BOTTOM = 482;
	public static final int CRUST_B_CORE = 492;
	public static final int CRUST_B_TOP = 530;

	public static final int THIRD_DARK_SEA = 540;
	public static final int THIRD_BASE = 600;
	public static final int THIRD_ROCKY_FLOOR = 628;
	public static final int THIRD_ROCK_CENTER = 700;
	public static final int THIRD_CLOUDS = 748;

	public static final int CORE_THICKNESS = 4;
	public static final int STALACTITE_DEPTH = 24;

	public static final int TUNNEL_SPACING = 2048;
	public static final int TUNNEL_RADIUS = 90;
	public static final int TUNNEL_EDGE_NOISE = 10;
	public static final int UPPER_TUNNEL_X = 0;
	public static final int UPPER_TUNNEL_Z = 0;
	public static final int LOWER_TUNNEL_X = 700;
	public static final int LOWER_TUNNEL_Z = 450;

	public static final ResourceKey<NormalNoise.NoiseParameters> ABYSS = noiseKey("demon_realm_abyss");
	public static final ResourceKey<NormalNoise.NoiseParameters> MESA = noiseKey("demon_realm_mesa");
	public static final ResourceKey<NormalNoise.NoiseParameters> HILLS = noiseKey("demon_realm_hills");
	public static final ResourceKey<NormalNoise.NoiseParameters> ROCKS = noiseKey("demon_realm_rocks");
	public static final ResourceKey<NormalNoise.NoiseParameters> ROCK_HEIGHT = noiseKey("demon_realm_rock_height");
	public static final ResourceKey<NormalNoise.NoiseParameters> ISLANDS = noiseKey("demon_realm_islands");
	public static final ResourceKey<NormalNoise.NoiseParameters> PILLARS = noiseKey("demon_realm_pillars");
	public static final ResourceKey<NormalNoise.NoiseParameters> STALACTITES = noiseKey("demon_realm_stalactites");
	public static final ResourceKey<NormalNoise.NoiseParameters> CRAGS = noiseKey("demon_realm_crags");

	public static float depthAt(int blockY) {
		return (float) (blockY - (WORLD_BOTTOM + WORLD_TOP) / 2) / ((WORLD_TOP - WORLD_BOTTOM) / 2.0F);
	}

	public static void bootstrap(BootstapContext<LevelStem> context) {
		HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
		HolderGetter<DimensionType> dimTypes = context.lookup(Registries.DIMENSION_TYPE);
		HolderGetter<NoiseGeneratorSettings> noiseSettings = context.lookup(Registries.NOISE_SETTINGS);

		Climate.Parameter zero = Climate.Parameter.point(0.0F);
		float lowerSplit = depthAt(CRUST_A_CORE);
		float upperSplit = depthAt(CRUST_B_CORE);
		MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(List.of(
				Pair.of(Climate.parameters(zero, zero, zero, zero, Climate.Parameter.span(-1.0F, lowerSplit), zero, 0.0F), biomes.getOrThrow(DemonRealmBiomes.FIRST_DEMON_WORLD)),
				Pair.of(Climate.parameters(zero, zero, zero, zero, Climate.Parameter.span(lowerSplit, upperSplit), zero, 0.0F), biomes.getOrThrow(DemonRealmBiomes.SECOND_DEMON_WORLD)),
				Pair.of(Climate.parameters(zero, zero, zero, zero, Climate.Parameter.span(upperSplit, 1.0F), zero, 0.0F), biomes.getOrThrow(DemonRealmBiomes.THIRD_DEMON_WORLD))
		)));

		ChunkGenerator chunkGenerator = new NoiseBasedChunkGenerator(biomeSource, noiseSettings.getOrThrow(DEMON_REALM_NOISE_SETTINGS));
		context.register(DEMON_REALM_STEM, new LevelStem(dimTypes.getOrThrow(DemonRealmDimension.DEMON_REALM_TYPE), chunkGenerator));
	}

	public static void bootstrapNoiseParameters(BootstapContext<NormalNoise.NoiseParameters> context) {
		context.register(ABYSS, new NormalNoise.NoiseParameters(-9, 1.0, 1.0, 0.5));
		context.register(MESA, new NormalNoise.NoiseParameters(-7, 1.0, 0.5, 0.25));
		context.register(HILLS, new NormalNoise.NoiseParameters(-6, 1.0, 0.5, 0.25));
		context.register(ROCKS, new NormalNoise.NoiseParameters(-5, 1.0, 0.6));
		context.register(ROCK_HEIGHT, new NormalNoise.NoiseParameters(-7, 1.0));
		context.register(ISLANDS, new NormalNoise.NoiseParameters(-8, 1.0, 1.0, 0.5, 0.25));
		context.register(PILLARS, new NormalNoise.NoiseParameters(-4, 1.0, 0.5));
		context.register(STALACTITES, new NormalNoise.NoiseParameters(-3, 1.0, 0.5));
		context.register(CRAGS, new NormalNoise.NoiseParameters(-6, 1.0, 0.6, 0.3));
	}

	public static void bootstrapNoise(BootstapContext<NoiseGeneratorSettings> context) {
		HolderGetter<NormalNoise.NoiseParameters> noiseParams = context.lookup(Registries.NOISE);

		context.register(DEMON_REALM_NOISE_SETTINGS, new NoiseGeneratorSettings(
				NoiseSettings.create(WORLD_BOTTOM, WORLD_TOP - WORLD_BOTTOM, 1, 1),
				MainBlocks.DEMON_ROCK.get().defaultBlockState(),
				Blocks.AIR.defaultBlockState(),
				createRouter(noiseParams),
				surfaceRules(),
				List.of(),
				WORLD_BOTTOM,
				false,
				false,
				false,
				false
		));
	}

	private static SurfaceRules.RuleSource surfaceRules() {
		SurfaceRules.RuleSource makaiStone = SurfaceRules.state(DemonRealmBlocks.natural(MainBlocks.MAKAI_STONE.get()));
		SurfaceRules.RuleSource strata = SurfaceRules.state(DemonRealmBlocks.natural(MainBlocks.MAKAI_STRATA_STONE.get()));
		SurfaceRules.RuleSource makaiGrass = SurfaceRules.state(DemonRealmBlocks.natural(MainBlocks.MAKAI_GRASS_BLOCK.get()));
		SurfaceRules.RuleSource makaiDirt = SurfaceRules.state(DemonRealmBlocks.natural(MainBlocks.MAKAI_DIRT.get()));
		SurfaceRules.RuleSource makaiSand = SurfaceRules.state(DemonRealmBlocks.natural(MainBlocks.MAKAI_SAND.get()));
		SurfaceRules.RuleSource lilacSand = SurfaceRules.state(DemonRealmBlocks.natural(MainBlocks.LILAC_SAND.get()));
		SurfaceRules.RuleSource darkCloud = state(MainBlocks.DARK_SEA_CLOUD.get());
		SurfaceRules.RuleSource crust = SurfaceRules.state(DemonRealmBlocks.natural(MainBlocks.DEMON_REALM_CRUST.get()));
		SurfaceRules.RuleSource turquoise = state(MainBlocks.TURQUOISE_ROCK.get());
		SurfaceRules.RuleSource namekGrass = state(MainBlocks.NAMEK_GRASS_BLOCK.get());
		SurfaceRules.RuleSource namekDirt = state(MainBlocks.NAMEK_DIRT.get());
		SurfaceRules.RuleSource namekStone = state(MainBlocks.NAMEK_STONE.get());
		SurfaceRules.RuleSource demonRock = state(MainBlocks.DEMON_ROCK.get());
		SurfaceRules.RuleSource redAsh = state(MainBlocks.RED_ASH.get());
		SurfaceRules.RuleSource magmaRock = state(MainBlocks.DEMON_MAGMA_ROCK.get());

		SurfaceRules.RuleSource bedrock = SurfaceRules.ifTrue(
				SurfaceRules.verticalGradient("demon_realm_bedrock", VerticalAnchor.aboveBottom(0), VerticalAnchor.aboveBottom(4)),
				SurfaceRules.state(Blocks.BEDROCK.defaultBlockState())
		);

		SurfaceRules.RuleSource firstWorld = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.NETHER_STATE_SELECTOR, 0.55), magmaRock),
						redAsh
				)),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, redAsh),
				demonRock
		);

		SurfaceRules.RuleSource secondWorld = SurfaceRules.sequence(
				SurfaceRules.ifTrue(atOrAbove(SECOND_SEA), SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, namekGrass),
						SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, namekDirt)
				)),
				SurfaceRules.ifTrue(atOrAbove(CRUST_A_TOP + 6), turquoise),
				namekStone
		);

		SurfaceRules.ConditionSource lilacPatch = SurfaceRules.noiseCondition(Noises.SURFACE, 0.45);
		SurfaceRules.ConditionSource grassy = SurfaceRules.noiseCondition(Noises.SURFACE_SECONDARY, 0.15);
		SurfaceRules.ConditionSource nearDeckSurface = SurfaceRules.stoneDepthCheck(10, false, CaveSurface.FLOOR);

		SurfaceRules.RuleSource thirdWorld = SurfaceRules.sequence(
				SurfaceRules.ifTrue(below(THIRD_DARK_SEA + 6), SurfaceRules.ifTrue(nearDeckSurface, darkCloud)),
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(lilacPatch, lilacSand),
						SurfaceRules.ifTrue(atOrAbove(THIRD_ROCKY_FLOOR), makaiStone),
						SurfaceRules.ifTrue(grassy, makaiGrass),
						makaiSand
				)),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(lilacPatch, lilacSand),
						SurfaceRules.ifTrue(atOrAbove(THIRD_ROCKY_FLOOR), makaiStone),
						SurfaceRules.ifTrue(grassy, makaiDirt),
						makaiSand
				)),
				strataBands(THIRD_BASE - 20, WORLD_TOP, strata, makaiStone)
		);

		return SurfaceRules.sequence(
				bedrock,
				SurfaceRules.ifTrue(below(CRUST_A_CORE), firstWorld),
				SurfaceRules.ifTrue(below(CRUST_A_CORE + CORE_THICKNESS), crust),
				SurfaceRules.ifTrue(below(SECOND_BAND_TOP), secondWorld),
				SurfaceRules.ifTrue(below(CRUST_B_CORE), strataBands(SECOND_BAND_TOP, CRUST_B_CORE, strata, makaiStone)),
				SurfaceRules.ifTrue(below(CRUST_B_CORE + CORE_THICKNESS), crust),
				thirdWorld
		);
	}

	private static SurfaceRules.RuleSource strataBands(int from, int to, SurfaceRules.RuleSource strata, SurfaceRules.RuleSource stone) {
		List<SurfaceRules.RuleSource> bands = new ArrayList<>();
		for (int y = from; y < to; y += 7) {
			bands.add(SurfaceRules.ifTrue(below(y + 2), SurfaceRules.ifTrue(atOrAbove(y), strata)));
		}
		bands.add(stone);
		return SurfaceRules.sequence(bands.toArray(new SurfaceRules.RuleSource[0]));
	}

	private static NoiseRouter createRouter(HolderGetter<NormalNoise.NoiseParameters> noiseParams) {
		DensityFunction zero = DensityFunctions.constant(0.0);
		DensityFunction blockY = DensityFunctions.yClampedGradient(WORLD_BOTTOM, WORLD_TOP, WORLD_BOTTOM, WORLD_TOP);

		Holder<NormalNoise.NoiseParameters> abyss = noiseParams.getOrThrow(ABYSS);
		Holder<NormalNoise.NoiseParameters> mesa = noiseParams.getOrThrow(MESA);
		Holder<NormalNoise.NoiseParameters> hills = noiseParams.getOrThrow(HILLS);
		Holder<NormalNoise.NoiseParameters> rocks = noiseParams.getOrThrow(ROCKS);
		Holder<NormalNoise.NoiseParameters> rockHeight = noiseParams.getOrThrow(ROCK_HEIGHT);
		Holder<NormalNoise.NoiseParameters> islands = noiseParams.getOrThrow(ISLANDS);
		Holder<NormalNoise.NoiseParameters> pillars = noiseParams.getOrThrow(PILLARS);
		Holder<NormalNoise.NoiseParameters> stalactites = noiseParams.getOrThrow(STALACTITES);
		Holder<NormalNoise.NoiseParameters> crags = noiseParams.getOrThrow(CRAGS);
		Holder<NormalNoise.NoiseParameters> billow = noiseParams.getOrThrow(OtherworldGeneration.CLOUD_BILLOW);

		DensityFunction firstWorld = firstWorld(blockY, hills, mesa, crags, pillars);
		DensityFunction secondWorld = secondWorld(blockY, islands, hills, pillars, rockHeight, stalactites);
		DensityFunction thirdWorld = thirdWorld(blockY, abyss, mesa, hills, rocks, rockHeight, stalactites, billow);

		DensityFunction terrain = DensityFunctions.interpolated(DensityFunctions.max(firstWorld, DensityFunctions.max(secondWorld, thirdWorld)));
		DensityFunction depth = DensityFunctions.yClampedGradient(WORLD_BOTTOM, WORLD_TOP, -1.0, 1.0);

		return new NoiseRouter(
				zero, zero, zero, zero,
				zero, zero, zero, zero,
				depth,
				zero,
				terrain,
				terrain,
				zero, zero, zero
		);
	}

	private static DensityFunction firstWorld(DensityFunction blockY, Holder<NormalNoise.NoiseParameters> hills, Holder<NormalNoise.NoiseParameters> mesa,
											  Holder<NormalNoise.NoiseParameters> crags, Holder<NormalNoise.NoiseParameters> pillars) {
		DensityFunction dunes = scaled(6.0, noise2d(hills, 5000.0));
		DensityFunction cragMask = DensityFunctions.add(scaled(2.0, noise2d(mesa, 7000.0)), DensityFunctions.constant(0.1)).clamp(0.0, 1.0);
		DensityFunction ridges = DensityFunctions.add(DensityFunctions.constant(1.0), scaled(-1.0, noise2d(crags, 0.0).abs())).cube();
		DensityFunction knolls = scaled(4.0, DensityFunctions.add(noise2d(pillars, 9000.0), DensityFunctions.constant(-0.45))).clamp(0.0, 1.0).square();
		DensityFunction groundTop = DensityFunctions.add(
				DensityFunctions.add(DensityFunctions.constant(FIRST_BASE), dunes),
				DensityFunctions.add(scaled(30.0, DensityFunctions.mul(cragMask, ridges)), scaled(7.0, knolls))
		);
		return slab(blockY, groundTop, DensityFunctions.constant(WORLD_BOTTOM - 8));
	}

	private static DensityFunction secondWorld(DensityFunction blockY, Holder<NormalNoise.NoiseParameters> islands, Holder<NormalNoise.NoiseParameters> hills,
											   Holder<NormalNoise.NoiseParameters> pillars, Holder<NormalNoise.NoiseParameters> rockHeight,
											   Holder<NormalNoise.NoiseParameters> stalactites) {
		DensityFunction islandShape = DensityFunctions.spline(CubicSpline.builder(coordinate(noise2d(islands, 0.0)))
				.addPoint(-1.0F, 2.0F, 0.0F)
				.addPoint(-0.2F, 6.0F, 0.0F)
				.addPoint(0.0F, 12.0F, 0.0F)
				.addPoint(0.12F, 16.0F, 0.0F)
				.addPoint(0.2F, 20.0F, 0.0F)
				.addPoint(0.35F, 26.0F, 0.0F)
				.addPoint(0.55F, 40.0F, 0.0F)
				.addPoint(0.8F, 58.0F, 0.0F)
				.addPoint(1.0F, 66.0F, 0.0F)
				.build());
		DensityFunction pillarMask = DensityFunctions.mul(
				scaled(6.0, DensityFunctions.add(noise2d(pillars, 0.0), DensityFunctions.constant(-0.6))).clamp(0.0, 1.0),
				scaled(2.0, noise2d(rockHeight, 1200.0)).clamp(0.0, 1.0)
		);
		DensityFunction islandTop = DensityFunctions.add(
				DensityFunctions.add(DensityFunctions.constant(CRUST_A_TOP), islandShape),
				DensityFunctions.add(scaled(3.0, noise2d(hills, 2600.0)), scaled(90.0, pillarMask))
		);

		DensityFunction tunnel = new DemonRealmFeatures.TunnelDistance(TUNNEL_SPACING, LOWER_TUNNEL_X, LOWER_TUNNEL_Z);
		DensityFunction craterRing = DensityFunctions.add(
				scaled(30.0, scaled(1.0 / 12.0, DensityFunctions.add(DensityFunctions.constant(TUNNEL_RADIUS + 40), scaled(-1.0, tunnel))).clamp(0.0, 1.0)),
				scaled(5.0, noise2d(hills, 8800.0))
		);
		DensityFunction top = DensityFunctions.max(islandTop, DensityFunctions.add(DensityFunctions.constant(CRUST_A_TOP), craterRing));

		DensityFunction underside = DensityFunctions.add(DensityFunctions.constant(CRUST_A_BOTTOM), scaled(-1.0, stalactiteDepth(stalactites, 0.0)));
		DensityFunction body = slab(blockY, top, underside);
		return DensityFunctions.min(body, holeWall(tunnel, hills, 5100.0));
	}

	private static DensityFunction thirdWorld(DensityFunction blockY, Holder<NormalNoise.NoiseParameters> abyss, Holder<NormalNoise.NoiseParameters> mesa,
											  Holder<NormalNoise.NoiseParameters> hills, Holder<NormalNoise.NoiseParameters> rocks,
											  Holder<NormalNoise.NoiseParameters> rockHeight, Holder<NormalNoise.NoiseParameters> stalactites,
											  Holder<NormalNoise.NoiseParameters> billow) {
		DensityFunction tunnel = new DemonRealmFeatures.TunnelDistance(TUNNEL_SPACING, UPPER_TUNNEL_X, UPPER_TUNNEL_Z);

		DensityFunction abyssNoise = noise2d(abyss, 0.0);
		DensityFunction terraces = DensityFunctions.spline(CubicSpline.builder(coordinate(noise2d(mesa, 0.0)))
				.addPoint(-1.0F, 0.0F, 0.0F)
				.addPoint(0.05F, 0.0F, 0.0F)
				.addPoint(0.18F, 24.0F, 0.0F)
				.addPoint(0.3F, 24.0F, 0.0F)
				.addPoint(0.42F, 46.0F, 0.0F)
				.addPoint(0.54F, 46.0F, 0.0F)
				.addPoint(0.66F, 72.0F, 0.0F)
				.addPoint(0.8F, 74.0F, 0.0F)
				.addPoint(1.0F, 90.0F, 0.0F)
				.build());
		DensityFunction edgeDrop = scaled(90.0, DensityFunctions.add(abyssNoise, DensityFunctions.constant(-0.28))).clamp(0.0, 22.0);
		DensityFunction craterLip = scaled(10.0, scaled(1.0 / 10.0, DensityFunctions.add(DensityFunctions.constant(TUNNEL_RADIUS + 30), scaled(-1.0, tunnel))).clamp(0.0, 1.0));
		DensityFunction landTop = DensityFunctions.add(
				DensityFunctions.add(DensityFunctions.constant(THIRD_BASE), scaled(9.0, noise2d(hills, 0.0))),
				DensityFunctions.add(DensityFunctions.add(terraces, craterLip), scaled(-1.0, edgeDrop))
		);
		DensityFunction chasm = scaled(4000.0, DensityFunctions.add(abyssNoise, DensityFunctions.constant(-0.42))).clamp(0.0, 2000.0);
		DensityFunction land = DensityFunctions.add(slab(blockY, landTop, DensityFunctions.constant(CRUST_B_TOP)), scaled(-1.0, chasm));

		DensityFunction deckTop = DensityFunctions.add(DensityFunctions.constant(THIRD_DARK_SEA), scaled(3.0, noise2d(billow, 0.0)));
		DensityFunction underside = DensityFunctions.add(DensityFunctions.constant(CRUST_B_BOTTOM), scaled(-1.0, stalactiteDepth(stalactites, 4200.0)));
		DensityFunction crustAndDeck = slab(blockY, deckTop, underside);

		DensityFunction body = DensityFunctions.min(DensityFunctions.max(land, crustAndDeck), holeWall(tunnel, hills, 3300.0));
		DensityFunction floatingRocks = floatingRocks(blockY, noise2d(rocks, 0.0), noise2d(rockHeight, 0.0), 0.52, 110.0, THIRD_ROCK_CENTER, 25.0);
		return DensityFunctions.max(body, floatingRocks);
	}

	private static DensityFunction holeWall(DensityFunction tunnel, Holder<NormalNoise.NoiseParameters> edgeNoise, double offset) {
		DensityFunction edge = DensityFunctions.add(DensityFunctions.constant(TUNNEL_RADIUS), scaled(TUNNEL_EDGE_NOISE, noise2d(edgeNoise, offset)));
		return DensityFunctions.add(tunnel, scaled(-1.0, edge));
	}

	private static DensityFunction floatingRocks(DensityFunction blockY, DensityFunction mask, DensityFunction heightNoise, double threshold, double thicknessScale, int center, double centerSpread) {
		DensityFunction thickness = scaled(thicknessScale, DensityFunctions.add(mask, DensityFunctions.constant(-threshold)));
		DensityFunction middle = DensityFunctions.add(DensityFunctions.constant(center), scaled(centerSpread, heightNoise));
		DensityFunction top = DensityFunctions.add(middle, scaled(0.22, thickness));
		DensityFunction bottom = DensityFunctions.add(middle, scaled(-1.0, thickness));
		return slab(blockY, top, bottom);
	}

	private static DensityFunction stalactiteDepth(Holder<NormalNoise.NoiseParameters> stalactites, double offset) {
		return scaled(STALACTITE_DEPTH, scaled(1.6, DensityFunctions.add(noise2d(stalactites, offset), DensityFunctions.constant(-0.1))).clamp(0.0, 1.0).square());
	}

	private static DensityFunction slab(DensityFunction blockY, DensityFunction top, DensityFunction bottom) {
		return DensityFunctions.min(
				DensityFunctions.add(top, scaled(-1.0, blockY)),
				DensityFunctions.add(blockY, scaled(-1.0, bottom))
		);
	}

	private static DensityFunction noise2d(Holder<NormalNoise.NoiseParameters> noise, double offset) {
		if (offset == 0.0) return DensityFunctions.noise(noise, 1.0, 0.0);
		return DensityFunctions.shiftedNoise2d(DensityFunctions.constant(offset), DensityFunctions.constant(-offset), 1.0, noise);
	}

	private static DensityFunctions.Spline.Coordinate coordinate(DensityFunction function) {
		return new DensityFunctions.Spline.Coordinate(Holder.direct(function));
	}

	private static SurfaceRules.ConditionSource below(int y) {
		return SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(y), 0));
	}

	private static SurfaceRules.ConditionSource atOrAbove(int y) {
		return SurfaceRules.yBlockCheck(VerticalAnchor.absolute(y), 0);
	}

	private static SurfaceRules.RuleSource state(Block block) {
		return SurfaceRules.state(block.defaultBlockState());
	}

	private static ResourceKey<NormalNoise.NoiseParameters> noiseKey(String name) {
		return ResourceKey.create(Registries.NOISE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}

	private static DensityFunction scaled(double factor, DensityFunction function) {
		return DensityFunctions.mul(DensityFunctions.constant(factor), function);
	}
}
