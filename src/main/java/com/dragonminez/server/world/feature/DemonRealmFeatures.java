package com.dragonminez.server.world.feature;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.block.custom.DemonRealmBlocks;
import com.dragonminez.server.world.gen.DemonRealmGeneration;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.stream.Stream;

public class DemonRealmFeatures {
	public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Reference.MOD_ID);
	public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, Reference.MOD_ID);
	public static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY_FUNCTIONS = DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, Reference.MOD_ID);

	public static final RegistryObject<Feature<NoneFeatureConfiguration>> SEA = FEATURES.register("demon_realm_sea",
			() -> new SeaFeature(NoneFeatureConfiguration.CODEC));
	public static final RegistryObject<Feature<NoneFeatureConfiguration>> TUNNEL_SHIELD = FEATURES.register("demon_realm_tunnel_shield",
			() -> new TunnelShieldFeature(NoneFeatureConfiguration.CODEC));
	public static final RegistryObject<Feature<NoneFeatureConfiguration>> GAS_VENT = FEATURES.register("demon_realm_gas_vent",
			() -> new GasVentFeature(NoneFeatureConfiguration.CODEC));
	public static final RegistryObject<Feature<NoneFeatureConfiguration>> MAKAI_BUSH = FEATURES.register("demon_realm_makai_bush",
			() -> new MakaiBushFeature(NoneFeatureConfiguration.CODEC));
	public static final RegistryObject<Feature<NoneFeatureConfiguration>> ROCK_SPIRE = FEATURES.register("demon_realm_rock_spire",
			() -> new RockSpireFeature(NoneFeatureConfiguration.CODEC));
	public static final RegistryObject<Feature<NoneFeatureConfiguration>> AMBIENT_LIGHT = FEATURES.register("demon_realm_ambient_light",
			() -> new AmbientLightFeature(NoneFeatureConfiguration.CODEC));
	public static final RegistryObject<Feature<NoneFeatureConfiguration>> CRYSTAL_SPIRE = FEATURES.register("demon_realm_crystal_spire",
			() -> new CrystalSpireFeature(NoneFeatureConfiguration.CODEC));

	public static final RegistryObject<PlacementModifierType<SurfaceInBand>> SURFACE_IN_BAND = PLACEMENT_MODIFIERS.register("surface_in_band",
			() -> () -> SurfaceInBand.CODEC);

	public static final RegistryObject<Codec<TunnelDistance>> TUNNEL_DISTANCE = DENSITY_FUNCTIONS.register("tunnel_distance",
			TunnelDistance.CODEC::codec);

	public static final ResourceKey<ConfiguredFeature<?, ?>> SEA_CONFIGURED = configuredKey("demon_realm_sea");
	public static final ResourceKey<ConfiguredFeature<?, ?>> TUNNEL_SHIELD_CONFIGURED = configuredKey("demon_realm_tunnel_shield");
	public static final ResourceKey<ConfiguredFeature<?, ?>> GAS_VENT_CONFIGURED = configuredKey("demon_realm_gas_vent");
	public static final ResourceKey<ConfiguredFeature<?, ?>> MAKAI_BUSH_CONFIGURED = configuredKey("demon_realm_makai_bush");
	public static final ResourceKey<ConfiguredFeature<?, ?>> MAKAI_SHRUBS_CONFIGURED = configuredKey("demon_realm_makai_shrubs");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ROCK_SPIRE_CONFIGURED = configuredKey("demon_realm_rock_spire");
	public static final ResourceKey<ConfiguredFeature<?, ?>> AMBIENT_LIGHT_CONFIGURED = configuredKey("demon_realm_ambient_light");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CRYSTAL_SPIRE_CONFIGURED = configuredKey("demon_realm_crystal_spire");
	public static final ResourceKey<ConfiguredFeature<?, ?>> MAJILITE_ORE_CONFIGURED = configuredKey("demon_realm_majilite_ore");

	public static final ResourceKey<PlacedFeature> SEA_PLACED = placedKey("demon_realm_sea_placed");
	public static final ResourceKey<PlacedFeature> LOWER_SHIELD_PLACED = placedKey("demon_realm_lower_shield_placed");
	public static final ResourceKey<PlacedFeature> UPPER_SHIELD_PLACED = placedKey("demon_realm_upper_shield_placed");
	public static final ResourceKey<PlacedFeature> GAS_VENT_PLACED = placedKey("demon_realm_gas_vent_placed");
	public static final ResourceKey<PlacedFeature> MAKAI_BUSH_PLACED = placedKey("demon_realm_makai_bush_placed");
	public static final ResourceKey<PlacedFeature> MAKAI_SHRUBS_PLACED = placedKey("demon_realm_makai_shrubs_placed");
	public static final ResourceKey<PlacedFeature> ROCK_SPIRE_PLACED = placedKey("demon_realm_rock_spire_placed");
	public static final ResourceKey<PlacedFeature> AMBIENT_LIGHT_PLACED = placedKey("demon_realm_ambient_light_placed");
	public static final ResourceKey<PlacedFeature> CRYSTAL_SPIRE_PLACED = placedKey("demon_realm_crystal_spire_placed");
	public static final ResourceKey<PlacedFeature> MAJILITE_ORE_PLACED = placedKey("demon_realm_majilite_ore_placed");
	public static final ResourceKey<PlacedFeature> AJISSA_TREE_PLACED = placedKey("demon_realm_ajissa_tree_placed");
	public static final ResourceKey<PlacedFeature> NAMEK_GRASS_PLACED = placedKey("demon_realm_namek_grass_placed");
	public static final ResourceKey<PlacedFeature> NAMEK_FLOWERS_PLACED = placedKey("demon_realm_namek_flowers_placed");

	private static final int FIRST_SKY_LIMIT = DemonRealmGeneration.CRUST_A_BOTTOM - DemonRealmGeneration.STALACTITE_DEPTH - 2;
	private static final int MAX_SUPPORT_DEPTH = 3;
	private static final int AMBIENT_LIGHT_SPACING = 5;
	private static final int AMBIENT_LIGHT_HEIGHT = 2;

	public static void register(IEventBus eventBus) {
		FEATURES.register(eventBus);
		PLACEMENT_MODIFIERS.register(eventBus);
		DENSITY_FUNCTIONS.register(eventBus);
	}

	public static void bootstrapConfigured(BootstapContext<ConfiguredFeature<?, ?>> context) {
		context.register(SEA_CONFIGURED, new ConfiguredFeature<>(SEA.get(), NoneFeatureConfiguration.INSTANCE));
		context.register(TUNNEL_SHIELD_CONFIGURED, new ConfiguredFeature<>(TUNNEL_SHIELD.get(), NoneFeatureConfiguration.INSTANCE));
		context.register(GAS_VENT_CONFIGURED, new ConfiguredFeature<>(GAS_VENT.get(), NoneFeatureConfiguration.INSTANCE));
		context.register(MAKAI_BUSH_CONFIGURED, new ConfiguredFeature<>(MAKAI_BUSH.get(), NoneFeatureConfiguration.INSTANCE));
		context.register(ROCK_SPIRE_CONFIGURED, new ConfiguredFeature<>(ROCK_SPIRE.get(), NoneFeatureConfiguration.INSTANCE));
		context.register(AMBIENT_LIGHT_CONFIGURED, new ConfiguredFeature<>(AMBIENT_LIGHT.get(), NoneFeatureConfiguration.INSTANCE));
		context.register(CRYSTAL_SPIRE_CONFIGURED, new ConfiguredFeature<>(CRYSTAL_SPIRE.get(), NoneFeatureConfiguration.INSTANCE));

		context.register(MAKAI_SHRUBS_CONFIGURED, new ConfiguredFeature<>(Feature.RANDOM_PATCH, new RandomPatchConfiguration(24, 6, 2,
				PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(MainBlocks.MAKAI_SHRUB.get()))))));

		BlockState majilite = MainBlocks.MAJILITE_ORE.get().defaultBlockState();
		context.register(MAJILITE_ORE_CONFIGURED, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(List.of(
				OreConfiguration.target(new BlockMatchTest(MainBlocks.TURQUOISE_ROCK.get()), majilite),
				OreConfiguration.target(new BlockMatchTest(MainBlocks.NAMEK_STONE.get()), majilite)
		), 7)));
	}

	public static void bootstrapPlaced(BootstapContext<PlacedFeature> context) {
		HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);

		SurfaceInBand firstSurface = new SurfaceInBand(FIRST_SKY_LIMIT, DemonRealmGeneration.WORLD_BOTTOM + 4);
		SurfaceInBand secondSurface = new SurfaceInBand(DemonRealmGeneration.SECOND_BAND_TOP, DemonRealmGeneration.CRUST_A_TOP);
		SurfaceInBand thirdSurface = new SurfaceInBand(DemonRealmGeneration.WORLD_TOP - 2, DemonRealmGeneration.CRUST_B_TOP);

		register(context, ROCK_SPIRE_PLACED, configured.getOrThrow(ROCK_SPIRE_CONFIGURED), List.of(
				NoiseThresholdCountPlacement.of(0.35D, 0, 5), InSquarePlacement.spread(), firstSurface, BiomeFilter.biome()));
		register(context, AMBIENT_LIGHT_PLACED, configured.getOrThrow(AMBIENT_LIGHT_CONFIGURED), List.of(
				fixedHeight(DemonRealmGeneration.FIRST_BASE), BiomeFilter.biome()));
		register(context, CRYSTAL_SPIRE_PLACED, configured.getOrThrow(CRYSTAL_SPIRE_CONFIGURED), List.of(
				RarityFilter.onAverageOnceEvery(10), InSquarePlacement.spread(), firstSurface, BiomeFilter.biome()));

		register(context, SEA_PLACED, configured.getOrThrow(SEA_CONFIGURED), List.of(
				fixedHeight(DemonRealmGeneration.SECOND_SEA), BiomeFilter.biome()));
		register(context, LOWER_SHIELD_PLACED, configured.getOrThrow(TUNNEL_SHIELD_CONFIGURED), List.of(
				fixedHeight(DemonRealmGeneration.CRUST_A_CORE + 1), BiomeFilter.biome()));
		register(context, MAJILITE_ORE_PLACED, configured.getOrThrow(MAJILITE_ORE_CONFIGURED), List.of(
				CountPlacement.of(9), InSquarePlacement.spread(),
				HeightRangePlacement.uniform(VerticalAnchor.absolute(DemonRealmGeneration.CRUST_A_TOP - 12), VerticalAnchor.absolute(DemonRealmGeneration.SECOND_SEA + 30)),
				BiomeFilter.biome()));
		register(context, AJISSA_TREE_PLACED, configured.getOrThrow(NamekConfiguredFeatures.AJISSA_TREE), List.of(
				PlacementUtils.countExtra(1, 0.25f, 2), InSquarePlacement.spread(), secondSurface,
				PlacementUtils.filteredByBlockSurvival(MainBlocks.NAMEK_AJISSA_SAPLING.get()), BiomeFilter.biome()));
		register(context, NAMEK_GRASS_PLACED, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_PATCH_GRASS_KEY), List.of(
				CountPlacement.of(5), InSquarePlacement.spread(), secondSurface, BiomeFilter.biome()));
		register(context, NAMEK_FLOWERS_PLACED, configured.getOrThrow(NamekConfiguredFeatures.NAMEK_FLOWERS_KEY), List.of(
				RarityFilter.onAverageOnceEvery(3), InSquarePlacement.spread(), secondSurface, BiomeFilter.biome()));

		register(context, UPPER_SHIELD_PLACED, configured.getOrThrow(TUNNEL_SHIELD_CONFIGURED), List.of(
				fixedHeight(DemonRealmGeneration.CRUST_B_CORE + 1), BiomeFilter.biome()));
		register(context, GAS_VENT_PLACED, configured.getOrThrow(GAS_VENT_CONFIGURED), List.of(
				RarityFilter.onAverageOnceEvery(3), InSquarePlacement.spread(), thirdSurface, BiomeFilter.biome()));
		register(context, MAKAI_SHRUBS_PLACED, configured.getOrThrow(MAKAI_SHRUBS_CONFIGURED), List.of(
				CountPlacement.of(3), InSquarePlacement.spread(), thirdSurface, BiomeFilter.biome()));
		register(context, MAKAI_BUSH_PLACED, configured.getOrThrow(MAKAI_BUSH_CONFIGURED), List.of(
				CountPlacement.of(UniformInt.of(0, 3)), InSquarePlacement.spread(), thirdSurface, BiomeFilter.biome()));
	}

	private static PlacementModifier fixedHeight(int y) {
		return HeightRangePlacement.uniform(VerticalAnchor.absolute(y), VerticalAnchor.absolute(y));
	}

	private static void register(BootstapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> feature, List<PlacementModifier> modifiers) {
		context.register(key, new PlacedFeature(feature, List.copyOf(modifiers)));
	}

	private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String name) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}

	private static ResourceKey<PlacedFeature> placedKey(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}

	private static boolean isEmpty(BlockState state) {
		return state.isAir() || (state.canBeReplaced() && state.getFluidState().isEmpty());
	}

	private static boolean isFirstWorldGround(BlockState state) {
		return state.is(MainBlocks.RED_ASH.get())
				|| state.is(MainBlocks.DEMON_ROCK.get())
				|| state.is(MainBlocks.DEMON_MAGMA_ROCK.get());
	}

	private static boolean isThirdWorldGround(BlockState state) {
		return state.is(MainBlocks.MAKAI_GRASS_BLOCK.get())
				|| state.is(MainBlocks.MAKAI_DIRT.get())
				|| state.is(MainBlocks.MAKAI_SAND.get())
				|| state.is(MainBlocks.MAKAI_STONE.get())
				|| state.is(MainBlocks.MAKAI_STRATA_STONE.get())
				|| state.is(MainBlocks.LILAC_SAND.get());
	}

	private static boolean supported(WorldGenLevel level, BlockPos pos, BlockState fill) {
		BlockPos.MutableBlockPos cursor = pos.mutable();
		for (int depth = 1; depth <= MAX_SUPPORT_DEPTH; depth++) {
			cursor.move(0, -1, 0);
			if (isEmpty(level.getBlockState(cursor))) continue;
			for (int y = pos.getY() - 1; y > cursor.getY(); y--) {
				level.setBlock(new BlockPos(pos.getX(), y, pos.getZ()), fill, 2);
			}
			return true;
		}
		return false;
	}

	private static void cone(WorldGenLevel level, BlockPos base, int height, float baseRadius, float exponent, float leanX, float leanZ,
							 BlockState body, BlockState foot, int maxY) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int i = -2; i < height; i++) {
			int y = base.getY() + i;
			if (y >= maxY) break;
			float t = Math.max(0, i) / (float) height;
			float radius = baseRadius * (float) Math.pow(1.0F - t, exponent) + 0.35F;
			int r = Mth.ceil(radius);
			int cx = base.getX() + Math.round(leanX * t);
			int cz = base.getZ() + Math.round(leanZ * t);
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dz * dz > radius * radius + 0.3F) continue;
					cursor.set(cx + dx, y, cz + dz);
					BlockState current = level.getBlockState(cursor);
					if (!isEmpty(current) && !isFirstWorldGround(current)) continue;
					level.setBlock(cursor, i <= 0 ? foot : body, 2);
				}
			}
		}
	}

	public record TunnelDistance(int spacing, int offsetX, int offsetZ) implements DensityFunction.SimpleFunction {
		public static final KeyDispatchDataCodec<TunnelDistance> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(instance -> instance.group(
				ExtraCodecs.POSITIVE_INT.fieldOf("spacing").forGetter(TunnelDistance::spacing),
				Codec.INT.fieldOf("offset_x").forGetter(TunnelDistance::offsetX),
				Codec.INT.fieldOf("offset_z").forGetter(TunnelDistance::offsetZ)
		).apply(instance, TunnelDistance::new)));

		public static double distance(int x, int z, int spacing, int offsetX, int offsetZ) {
			int dx = Math.floorMod(x - offsetX, spacing);
			if (dx > spacing / 2) dx -= spacing;
			int dz = Math.floorMod(z - offsetZ, spacing);
			if (dz > spacing / 2) dz -= spacing;
			return Math.sqrt((double) dx * dx + (double) dz * dz);
		}

		@Override
		public double compute(FunctionContext context) {
			return distance(context.blockX(), context.blockZ(), spacing, offsetX, offsetZ);
		}

		@Override
		public double minValue() {
			return 0.0;
		}

		@Override
		public double maxValue() {
			return spacing;
		}

		@Override
		public KeyDispatchDataCodec<? extends DensityFunction> codec() {
			return CODEC;
		}
	}

	public static class SurfaceInBand extends PlacementModifier {
		public static final Codec<SurfaceInBand> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("top").forGetter(placement -> placement.top),
				Codec.INT.fieldOf("bottom").forGetter(placement -> placement.bottom)
		).apply(instance, SurfaceInBand::new));

		private final int top;
		private final int bottom;

		public SurfaceInBand(int top, int bottom) {
			this.top = top;
			this.bottom = bottom;
		}

		@Override
		public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
			BlockPos surface = findSurface(context.getLevel(), pos.getX(), pos.getZ(), top, bottom);
			return surface == null ? Stream.empty() : Stream.of(surface);
		}

		public static BlockPos findSurface(WorldGenLevel level, int x, int z, int top, int bottom) {
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, top, z);
			boolean openAbove = false;
			for (int y = top; y >= bottom; y--) {
				cursor.setY(y);
				BlockState state = level.getBlockState(cursor);
				if (isEmpty(state)) {
					openAbove = true;
					continue;
				}
				if (!state.getFluidState().isEmpty()) {
					if (openAbove) return null;
					continue;
				}
				if (openAbove) return cursor.above().immutable();
			}
			return null;
		}

		@Override
		public PlacementModifierType<?> type() {
			return SURFACE_IN_BAND.get();
		}
	}

	public static class SeaFeature extends Feature<NoneFeatureConfiguration> {
		public SeaFeature(Codec<NoneFeatureConfiguration> codec) {
			super(codec);
		}

		@Override
		public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
			WorldGenLevel level = context.level();
			int minX = context.origin().getX() & ~15;
			int minZ = context.origin().getZ() & ~15;
			int sea = DemonRealmGeneration.SECOND_SEA;
			int floorLimit = DemonRealmGeneration.CRUST_A_TOP - 12;
			BlockState water = Blocks.WATER.defaultBlockState();
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			boolean placed = false;

			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					int floor = Integer.MIN_VALUE;
					for (int y = sea; y >= floorLimit; y--) {
						cursor.set(minX + x, y, minZ + z);
						if (!level.isEmptyBlock(cursor)) {
							floor = y;
							break;
						}
					}
					if (floor == Integer.MIN_VALUE || floor >= sea) continue;
					for (int y = floor + 1; y <= sea; y++) {
						cursor.set(minX + x, y, minZ + z);
						level.setBlock(cursor, water, 2);
					}
					placed = true;
				}
			}
			return placed;
		}
	}

	public static class TunnelShieldFeature extends Feature<NoneFeatureConfiguration> {
		public TunnelShieldFeature(Codec<NoneFeatureConfiguration> codec) {
			super(codec);
		}

		@Override
		public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
			WorldGenLevel level = context.level();
			BlockPos origin = context.origin();
			boolean upper = origin.getY() >= DemonRealmGeneration.CRUST_B_CORE;
			int offsetX = upper ? DemonRealmGeneration.UPPER_TUNNEL_X : DemonRealmGeneration.LOWER_TUNNEL_X;
			int offsetZ = upper ? DemonRealmGeneration.UPPER_TUNNEL_Z : DemonRealmGeneration.LOWER_TUNNEL_Z;
			int reach = DemonRealmGeneration.TUNNEL_RADIUS + DemonRealmGeneration.TUNNEL_EDGE_NOISE + 4;
			int minX = origin.getX() & ~15;
			int minZ = origin.getZ() & ~15;
			BlockState shield = MainBlocks.LIGHT_SHIELD.get().defaultBlockState();
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			boolean placed = false;

			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					double distance = TunnelDistance.distance(minX + x, minZ + z, DemonRealmGeneration.TUNNEL_SPACING, offsetX, offsetZ);
					if (distance > reach) continue;
					cursor.set(minX + x, origin.getY(), minZ + z);
					if (!level.isEmptyBlock(cursor)) continue;
					level.setBlock(cursor, shield, 2);
					placed = true;
				}
			}
			return placed;
		}
	}

	public static class GasVentFeature extends Feature<NoneFeatureConfiguration> {
		public GasVentFeature(Codec<NoneFeatureConfiguration> codec) {
			super(codec);
		}

		@Override
		public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
			WorldGenLevel level = context.level();
			RandomSource random = context.random();
			BlockPos origin = context.origin();
			if (!isThirdWorldGround(level.getBlockState(origin.below()))) return false;
			if (FeatureUtil.isInsideDmzStructure(level, origin)) return false;

			buildVent(level, random, origin, 1 + random.nextInt(3), 2.0F + random.nextFloat() * 1.5F);
			if (random.nextInt(3) == 0) {
				BlockPos side = origin.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
				if (level.isEmptyBlock(side) && isThirdWorldGround(level.getBlockState(side.below()))) {
					buildVent(level, random, side, 1, 1.2F);
				}
			}
			return true;
		}

		private void buildVent(WorldGenLevel level, RandomSource random, BlockPos origin, int height, float radius) {
			BlockState stone = DemonRealmBlocks.natural(MainBlocks.MAKAI_STONE.get());
			BlockState strata = DemonRealmBlocks.natural(MainBlocks.MAKAI_STRATA_STONE.get());
			BlockState sand = DemonRealmBlocks.natural(MainBlocks.LILAC_SAND.get());
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

			for (int dy = 0; dy < height; dy++) {
				float layerRadius = radius * (1.0F - (float) dy / (height + 1));
				int r = Mth.ceil(layerRadius);
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (dx * dx + dz * dz > layerRadius * layerRadius + 0.5F) continue;
						cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
						if (!isEmpty(level.getBlockState(cursor))) continue;
						if (dy == 0 && !supported(level, cursor, stone)) continue;
						this.setBlock(level, cursor, random.nextInt(3) == 0 ? strata : stone);
					}
				}
			}
			this.setBlock(level, origin.above(height - 1), MainBlocks.GAS_VENT.get().defaultBlockState());

			int patches = 4 + random.nextInt(5);
			for (int i = 0; i < patches; i++) {
				BlockPos ground = origin.offset(random.nextInt(9) - 4, -1, random.nextInt(9) - 4);
				if (isThirdWorldGround(level.getBlockState(ground)) && isEmpty(level.getBlockState(ground.above()))) {
					this.setBlock(level, ground, sand);
				}
			}
		}
	}

	public static class MakaiBushFeature extends Feature<NoneFeatureConfiguration> {
		public MakaiBushFeature(Codec<NoneFeatureConfiguration> codec) {
			super(codec);
		}

		@Override
		public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
			WorldGenLevel level = context.level();
			RandomSource random = context.random();
			BlockPos origin = context.origin();
			BlockState ground = level.getBlockState(origin.below());
			if (!ground.is(MainBlocks.MAKAI_GRASS_BLOCK.get()) && !ground.is(MainBlocks.MAKAI_SAND.get()) && !ground.is(MainBlocks.MAKAI_DIRT.get())) return false;

			BlockState bush = MainBlocks.MAKAI_BUSH.get().defaultBlockState();
			float radius = random.nextInt(3) == 0 ? 1.8F : 1.2F;
			int r = Mth.ceil(radius);
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			for (int dx = -r; dx <= r; dx++) {
				for (int dy = 0; dy <= r; dy++) {
					for (int dz = -r; dz <= r; dz++) {
						float distance = dx * dx + dy * dy * 1.6F + dz * dz;
						if (distance > radius * radius + random.nextFloat() * 0.6F) continue;
						cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
						if (!isEmpty(level.getBlockState(cursor))) continue;
						this.setBlock(level, cursor, bush);
					}
				}
			}
			return true;
		}
	}

	public static class RockSpireFeature extends Feature<NoneFeatureConfiguration> {
		public RockSpireFeature(Codec<NoneFeatureConfiguration> codec) {
			super(codec);
		}

		@Override
		public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
			WorldGenLevel level = context.level();
			RandomSource random = context.random();
			BlockPos origin = context.origin();
			if (!isFirstWorldGround(level.getBlockState(origin.below()))) return false;
			if (FeatureUtil.isInsideDmzStructure(level, origin)) return false;

			int height = 5 + random.nextInt(random.nextInt(5) == 0 ? 20 : 11);
			float radius = 2.0F + random.nextFloat() * 1.8F;
			float leanX = (random.nextFloat() - 0.5F) * height * 0.2F;
			float leanZ = (random.nextFloat() - 0.5F) * height * 0.2F;
			BlockState rock = MainBlocks.DEMON_ROCK.get().defaultBlockState();
			cone(level, origin, height, radius, 0.7F, leanX, leanZ, rock, rock, FIRST_SKY_LIMIT);
			return true;
		}
	}

	public static class AmbientLightFeature extends Feature<NoneFeatureConfiguration> {
		public AmbientLightFeature(Codec<NoneFeatureConfiguration> codec) {
			super(codec);
		}

		@Override
		public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
			WorldGenLevel level = context.level();
			int minX = context.origin().getX() & ~15;
			int minZ = context.origin().getZ() & ~15;
			BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
			boolean placed = false;

			for (int x = Math.floorMod(-minX, AMBIENT_LIGHT_SPACING); x < 16; x += AMBIENT_LIGHT_SPACING) {
				for (int z = Math.floorMod(-minZ, AMBIENT_LIGHT_SPACING); z < 16; z += AMBIENT_LIGHT_SPACING) {
					BlockPos ground = SurfaceInBand.findSurface(level, minX + x, minZ + z, FIRST_SKY_LIMIT, DemonRealmGeneration.WORLD_BOTTOM + 4);
					if (ground == null) continue;
					BlockPos lamp = ground.above(AMBIENT_LIGHT_HEIGHT);
					if (!level.isEmptyBlock(lamp)) continue;
					level.setBlock(lamp, light, 2);
					placed = true;
				}
			}
			return placed;
		}
	}

	public static class CrystalSpireFeature extends Feature<NoneFeatureConfiguration> {
		public CrystalSpireFeature(Codec<NoneFeatureConfiguration> codec) {
			super(codec);
		}

		@Override
		public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
			WorldGenLevel level = context.level();
			RandomSource random = context.random();
			BlockPos origin = context.origin();
			if (!isFirstWorldGround(level.getBlockState(origin.below()))) return false;
			if (FeatureUtil.isInsideDmzStructure(level, origin)) return false;

			BlockState crystal = MainBlocks.CRIMSON_CRYSTAL.get().defaultBlockState();
			BlockState rock = MainBlocks.DEMON_ROCK.get().defaultBlockState();
			int height = 5 + random.nextInt(7);
			cone(level, origin, height, 1.2F + random.nextFloat() * 0.8F, 0.7F, (random.nextFloat() - 0.5F) * 3.0F, (random.nextFloat() - 0.5F) * 3.0F, crystal, rock, FIRST_SKY_LIMIT);
			int satellites = 1 + random.nextInt(3);
			for (int i = 0; i < satellites; i++) {
				BlockPos ground = SurfaceInBand.findSurface(level, origin.getX() + random.nextInt(7) - 3, origin.getZ() + random.nextInt(7) - 3, origin.getY() + 4, origin.getY() - 8);
				if (ground == null || !isFirstWorldGround(level.getBlockState(ground.below()))) continue;
				cone(level, ground, 2 + random.nextInt(4), 0.8F, 0.7F, 0.0F, 0.0F, crystal, rock, FIRST_SKY_LIMIT);
			}
			return true;
		}
	}
}
