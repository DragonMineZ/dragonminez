package com.dragonminez.server.world.structure.helper;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainTags;
import com.dragonminez.server.world.biome.DemonRealmBiomes;
import com.dragonminez.server.world.structure.DemonVillageStructure;
import com.dragonminez.server.world.structure.TallJigsawStructure;
import com.dragonminez.server.world.structure.BossStructures.SaiyanCraterStructure;
import com.dragonminez.server.world.structure.BossStructures.GeteStarStructure;
import com.dragonminez.server.world.structure.BossStructures.GomahCampStructure;
import com.dragonminez.server.world.structure.BossStructures.GomahCradleStructure;
import com.dragonminez.server.world.structure.BossStructures.NamekRuinsStructure;
import com.dragonminez.server.world.structure.BossStructures.TreeOfMightStructure;
import com.dragonminez.server.world.structure.fitted.FittedTemplateStructure;
import com.dragonminez.server.world.structure.fitted.IncrementalFittedTemplateStructure;
import com.dragonminez.server.world.structure.fitted.TerrainFit;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.Map;
import java.util.Optional;

public class DMZStructures {
	private static final float RED_RIBBON_BASE_MAX_DEVIATION = 2.5F;

	public static final ResourceKey<Structure> GOKU_HOUSE = createKey("goku_house"),
			ROSHI_HOUSE = createKey("roshi_house"), TIMECHAMBER = createKey("timechamber"),
			ELDER_GURU = createKey("elder_guru"), KAMILOOKOUT = createKey("kamilookout"),
			GERO_LAB = createKey("gero_lab"), BABIDI = createKey("babidi"),
			CELL_ARENA = createKey("cell_arena"), FRIEZA_SHIP = createKey("frieza_ship"),
			PICCOLO_HOUSE = createKey("piccolo_house"), OLDKAI_PILLAR = createKey("oldkai_pillar"),
			YAMCHA_HOUSE = createKey("yamcha_house"), TRUNKS_SHIP = createKey("trunks_ship"),
			VEGETA_POD = createKey("vegeta_pod"),
			BABA_PALACE = createKey("baba_palace"), TREE_OF_MIGHT = createKey("tree_of_might"),
			SAIYAN_CRATER = createKey("saiyan_crater"), NAMEK_RUINS = createKey("namek_ruins"),
			GETE_STAR = createKey("gete_star"), DEMON_VILLAGE = createKey("demon_village"),
			GOMAH_CAMP = createKey("gomah_camp"), BUU_HOUSE = createKey("buu_house"),
			GOMAH_CRADLE = createKey("gomah_cradle"), RED_RIBBON_BASE = createKey("red_ribbon_base"),
			BUDOKAI_TENKAICHI = createKey("budokai_tenkaichi");

	private static final TerrainFit BUDOKAI_TENKAICHI_FIT = new TerrainFit(23, 2, 16, 1.0F, 64, 24, true);
	private static final float BUDOKAI_TENKAICHI_MIN_WATER = 0.98F;
	private static final int BUDOKAI_TENKAICHI_SAMPLE_STEP = 16;

	public static void bootstrap(BootstrapContext<Structure> context) {
		HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
		HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);

		HolderSet<Biome> overworld = biomes.getOrThrow(BiomeTags.IS_OVERWORLD);
		HolderSet<Biome> land = biomes.getOrThrow(MainTags.Biomes.IS_LAND);
		HolderSet<Biome> namek = biomes.getOrThrow(MainTags.Biomes.IS_NAMEK);

		fitted(context, GOKU_HOUSE, overworld, "goku_house", TerrainFit.terrain(3, 24, 1.0F, 64, 64));
		fitted(context, ROSHI_HOUSE, overworld, "roshi_house", TerrainFit.terrain(1, 24, 0.75F, 64, 32));
		fitted(context, BABA_PALACE, overworld, "baba_palace", TerrainFit.terrain(1, 16, 0.75F, 64, 32));
		fitted(context, TIMECHAMBER, biomes.getOrThrow(MainTags.Biomes.IS_HTC), "timechamber", TerrainFit.flush(0), Optional.of(Rotation.NONE));
		fitted(context, ELDER_GURU, namek, "elder_guru", TerrainFit.terrain(0, 20, 1.0F, 64, 64));
		fitted(context, KAMILOOKOUT, overworld, "kamilookout", TerrainFit.pillar(0, 0, 64, 0));
		fitted(context, CELL_ARENA, overworld, "cell_arena", TerrainFit.terrain(4, 20, 1.0F, 64, 64));
		fitted(context, FRIEZA_SHIP, namek, "frieza_ship", TerrainFit.pillar(0, 12, 48, 48));
		fitted(context, PICCOLO_HOUSE, land, "piccolo_house", TerrainFit.terrain(1, 16, 1.0F, 64, 64));
		fitted(context, OLDKAI_PILLAR, biomes.getOrThrow(MainTags.Biomes.IS_SACREDKAI), "oldkai_pillar", TerrainFit.pillar(0, 6, 64, 32));
		fitted(context, YAMCHA_HOUSE, overworld, "yamcha_house", TerrainFit.pillar(0, 8, 32, 32));
		fitted(context, BUU_HOUSE, overworld, "buu_house", TerrainFit.terrain(1, 20, 1.0F, 64, 64));
		fitted(context, TRUNKS_SHIP, land, "trunks_ship", TerrainFit.pillar(0, 4, 16, 24));
		fitted(context, VEGETA_POD, land, "vegeta_pod", TerrainFit.terrain(0, 12, 1.0F, 48, 48));
		fitted(context, RED_RIBBON_BASE, HolderSet.direct(biomes.getOrThrow(Biomes.PLAINS), biomes.getOrThrow(Biomes.SUNFLOWER_PLAINS),
						biomes.getOrThrow(Biomes.SAVANNA)), "red_ribbon_base", TerrainFit.terrain(0, 12, 1.0F, 64, 48), Optional.empty(),
				RED_RIBBON_BASE_MAX_DEVIATION);

		context.register(BUDOKAI_TENKAICHI, new IncrementalFittedTemplateStructure(
				new Structure.StructureSettings(budokaiOceans(biomes), Map.of(), GenerationStep.Decoration.TOP_LAYER_MODIFICATION, TerrainAdjustment.NONE),
				ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "budokai_tenkaichi"), BUDOKAI_TENKAICHI_FIT,
				Optional.of(Rotation.NONE), BUDOKAI_TENKAICHI_MIN_WATER, BUDOKAI_TENKAICHI_SAMPLE_STEP));

		context.register(GERO_LAB, new JigsawStructure(
				new Structure.StructureSettings(
						land,
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.GERO_LAB),
				3,
				ConstantHeight.of(VerticalAnchor.absolute(1)),
				false,
				Heightmap.Types.WORLD_SURFACE_WG
		));

		context.register(BABIDI, new TallJigsawStructure(
				new Structure.StructureSettings(
						land,
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				),
				pools.getOrThrow(DMZPools.BABIDI),
				Optional.empty(),
				3,
				ConstantHeight.of(VerticalAnchor.absolute(1)),
				false,
				Optional.of(Heightmap.Types.WORLD_SURFACE_WG),
				160,
				63
		));

		context.register(TREE_OF_MIGHT, new TreeOfMightStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				)
		));

		context.register(SAIYAN_CRATER, new SaiyanCraterStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
						Map.of(),
						GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
						TerrainAdjustment.NONE
				)
		));

		context.register(NAMEK_RUINS, new NamekRuinsStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(MainTags.Biomes.IS_NAMEK),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.BEARD_THIN
				)
		));

		context.register(GETE_STAR, new GeteStarStructure(
				new Structure.StructureSettings(
						biomes.getOrThrow(MainTags.Biomes.IS_NAMEK),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				)
		));

		context.register(DEMON_VILLAGE, new DemonVillageStructure(
				new Structure.StructureSettings(
						HolderSet.direct(biomes.getOrThrow(DemonRealmBiomes.THIRD_DEMON_WORLD)),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				)
		));

		context.register(GOMAH_CAMP, new GomahCampStructure(
				new Structure.StructureSettings(
						HolderSet.direct(biomes.getOrThrow(DemonRealmBiomes.FIRST_DEMON_WORLD),
								biomes.getOrThrow(DemonRealmBiomes.SECOND_DEMON_WORLD),
								biomes.getOrThrow(DemonRealmBiomes.THIRD_DEMON_WORLD)),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.BEARD_THIN
				)
		));

		context.register(GOMAH_CRADLE, new GomahCradleStructure(
				new Structure.StructureSettings(
						HolderSet.direct(biomes.getOrThrow(DemonRealmBiomes.FIRST_DEMON_WORLD),
								biomes.getOrThrow(DemonRealmBiomes.SECOND_DEMON_WORLD),
								biomes.getOrThrow(DemonRealmBiomes.THIRD_DEMON_WORLD)),
						Map.of(),
						GenerationStep.Decoration.SURFACE_STRUCTURES,
						TerrainAdjustment.NONE
				)
		));
	}

	private static void fitted(BootstrapContext<Structure> context, ResourceKey<Structure> key, HolderSet<Biome> biomes,
							   String template, TerrainFit fit) {
		fitted(context, key, biomes, template, fit, Optional.empty(), 0.0F);
	}

	private static void fitted(BootstrapContext<Structure> context, ResourceKey<Structure> key, HolderSet<Biome> biomes,
							   String template, TerrainFit fit, Optional<Rotation> rotation) {
		fitted(context, key, biomes, template, fit, rotation, 0.0F);
	}

	private static void fitted(BootstrapContext<Structure> context, ResourceKey<Structure> key, HolderSet<Biome> biomes,
							   String template, TerrainFit fit, Optional<Rotation> rotation, float maxDeviation) {
		context.register(key, new FittedTemplateStructure(
				new Structure.StructureSettings(biomes, Map.of(), GenerationStep.Decoration.TOP_LAYER_MODIFICATION, TerrainAdjustment.NONE),
				ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, template), fit, rotation, maxDeviation));
	}

	public static HolderSet<Biome> budokaiOceans(HolderGetter<Biome> biomes) {
		return HolderSet.direct(biomes.getOrThrow(Biomes.OCEAN), biomes.getOrThrow(Biomes.DEEP_OCEAN),
				biomes.getOrThrow(Biomes.LUKEWARM_OCEAN), biomes.getOrThrow(Biomes.DEEP_LUKEWARM_OCEAN),
				biomes.getOrThrow(Biomes.WARM_OCEAN), biomes.getOrThrow(Biomes.COLD_OCEAN), biomes.getOrThrow(Biomes.DEEP_COLD_OCEAN));
	}

	private static ResourceKey<Structure> createKey(String name) {
		return ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}
}
