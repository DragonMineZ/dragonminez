package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallSetDefinition;
import com.dragonminez.common.dragonball.DragonRadarAssetDefinition;
import com.dragonminez.common.dragonball.DragonRadarDefinition;
import com.dragonminez.common.dragonball.DragonBallSetAssetDefinition;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

public class DMZItemModelProvider extends ItemModelProvider {
	public DMZItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
		super(output, Reference.MOD_ID, existingFileHelper);
	}

	@Override
	protected void registerModels() {
		//Items (MainItems)
		for (DragonRadarDefinition radarDefinition : DragonBallDefinitions.getRadars()) {
			DragonRadarAssetDefinition assets = radarDefinition.resolveAssetDefinition();
			RegistryObject<Item> item = MainItems.getDragonRadarItemOrThrow(radarDefinition.getId());
			if (assets != null && assets.getItemTexturePath().isPresent()) {
				withExistingParent(item.getId().getPath(), mcLoc("item/generated")).texture("layer0", ResourceLocation.parse(assets.getItemTexturePath().get()));
			} else {
				simpleItem(item);
			}
		}
		simpleItem(MainItems.MIGHT_TREE_FRUIT);
		simpleItem(MainItems.NUBE_ITEM);
		simpleItem(MainItems.NUBE_NEGRA_ITEM);
		simpleItem(MainItems.NAVE_SAIYAN_ITEM);
		simpleItem(MainItems.SENZU_BEAN);
		simpleItem(MainItems.RED_CAPSULE);
		simpleItem(MainItems.YELLOW_CAPSULE);
		simpleItem(MainItems.PURPLE_CAPSULE);
		simpleItem(MainItems.GREEN_CAPSULE);
		simpleItem(MainItems.BLUE_CAPSULE);
		simpleItem(MainItems.ORANGE_CAPSULE);
		simpleItem(MainItems.POTHALA_LEFT);
		simpleItem(MainItems.POTHALA_RIGHT);
		simpleItem(MainItems.GREEN_POTHALA_LEFT);
		simpleItem(MainItems.GREEN_POTHALA_RIGHT);
		// Placeholder textures for the pair items (reuse the earring art until dedicated art exists).
		withExistingParent(MainItems.POTHALA_PAIR.getId().getPath(), mcLoc("item/generated")).texture("layer0", modLoc("item/pothala_right"));
		withExistingParent(MainItems.GREEN_POTHALA_PAIR.getId().getPath(), mcLoc("item/generated")).texture("layer0", modLoc("item/green_pothala_right"));
		simpleItem(MainItems.HEART_MEDICINE);
		simpleItem(MainItems.NAMEK_WATER_BUCKET);
		simpleItem(MainItems.HEALING_BUCKET);
		for (DragonBallSetDefinition setDefinition : DragonBallDefinitions.getBallSets()) {
			DragonBallSetAssetDefinition assets = setDefinition.resolveAssetDefinition();
			for (Map.Entry<Integer, RegistryObject<Item>> entry : MainItems.getDragonBallBlockItems(setDefinition.getId()).entrySet()) {
				int star = entry.getKey();
				RegistryObject<Item> item = entry.getValue();
				if (assets != null && assets.getInventoryTexturePathForStar(star).isPresent()) {
					withExistingParent(item.getId().getPath(), mcLoc("item/generated")).texture("layer0", ResourceLocation.parse(assets.getInventoryTexturePathForStar(star).get()));
				} else {
					simpleItem(item);
				}
			}
		}
		simpleItem(MainItems.RADAR_PIECE);
		simpleItem(MainItems.T1_RADAR_CHIP);
		simpleItem(MainItems.T2_RADAR_CHIP);
		simpleItem(MainItems.T1_RADAR_CPU);
		simpleItem(MainItems.T2_RADAR_CPU);
        simpleItem(MainItems.GREEN_SCOUTER);
        simpleItem(MainItems.RED_SCOUTER);
        simpleItem(MainItems.BLUE_SCOUTER);
        simpleItem(MainItems.PURPLE_SCOUTER);
        // Spawn Eggs
		withExistingParent(MainItems.DINO_1.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.DINO_2.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.DINO_3.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.DINO_KID.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.NAMEK_FROG_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.GINYU_FROG_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.SOLDIER01_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.SOLDIER02_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.SOLDIER03_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.NWARRIOR_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.SAIBAMAN_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.KAIWAREMAN_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.KYUKONMAN_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.COPYMAN_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.TENNENMAN_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.JINKOUMAN_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.REDRIBBONSOLDIER_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.REDRIBBONROBOT1_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.REDRIBBONROBOT2_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.REDRIBBONROBOT3_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
		withExistingParent(MainItems.BANDIT_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));

		//Comidas
		simpleItem(MainItems.DINO_MEAT_RAW);
		simpleItem(MainItems.DINO_MEAT_COOKED);
		simpleItem(MainItems.BABY_DINO_MEAT_RAW);
		simpleItem(MainItems.BABY_DINO_MEAT_COOKED);
		simpleItem(MainItems.DINO_TAIL_RAW);
		simpleItem(MainItems.DINO_TAIL_COOKED);
		simpleItem(MainItems.FROG_LEGS_RAW);
		simpleItem(MainItems.FROG_LEGS_COOKED);

		//Armaduras (DEJAR A-Z POR FAVOR Q SINO ME PIERDO XD)
		generateArmorSetModels(MainItems.A13_ARMOR);
		generateArmorSetModels(MainItems.A14_ARMOR);
		generateArmorSetModels(MainItems.A16_ARMOR);
		generateArmorSetModels(MainItems.A17_ARMOR);
		generateArmorSetModels(MainItems.A17_SUPER_ARMOR);
		generateArmorSetModels(MainItems.A18_ARMOR);
		generateArmorSetModels(MainItems.A18_CELL_ARMOR);
		generateArmorSetModels(MainItems.A18_KAME_ARMOR);
		generateArmorSetModels(MainItems.A18_TOURNAMENT_ARMOR);
		generateArmorSetModels(MainItems.A20_ARMOR);
		generateArmorSetModels(MainItems.AGE1000_ARMOR);
		generateArmorSetModels(MainItems.BARDOCK_DBZ_ARMOR);
		generateArmorSetModels(MainItems.BARDOCK_SUPER_ARMOR);
		generateArmorSetModels(MainItems.BEERUS_ARMOR);
		generateArmorSetModels(MainItems.BLACKGOKU_ARMOR);
		generateArmorSetModels(MainItems.BROLY_SUPER_ARMOR);
		generateArmorSetModels(MainItems.BROLY_Z_ARMOR);
		generateArmorSetModels(MainItems.CAULIFLA_ARMOR);
		generateArmorSetModels(MainItems.CHAOZ_ARMOR);
		generateArmorSetModels(MainItems.DEMON_GI_BLUE_ARMOR);
		generateArmorSetModels(MainItems.DRAGON_CLAN_ARMOR);
		generateArmorSetModels(MainItems.EVIL_BUU_ARMOR);
		generateArmorSetModels(MainItems.FIGHTER_ARMOR);
		generateArmorSetModels(MainItems.FUSION_ZAMASU_ARMOR);
		generateArmorSetModels(MainItems.FUTURE_GOHAN_ARMOR);
		generateArmorSetModels(MainItems.GAMMA1_ARMOR);
		generateArmorSetModels(MainItems.GAMMA2_ARMOR);
		generateArmorSetModels(MainItems.GAS_ARMOR);
		generateArmorSetModels(MainItems.GINE_ARMOR);
		generateArmorSetModels(MainItems.GOGETA_ARMOR);
		generateArmorSetModels(MainItems.GOHAN_SUPER_ARMOR);
		generateArmorSetModels(MainItems.GOKU_ARMOR);
		generateArmorSetModels(MainItems.GOKU_GT_ARMOR);
		generateArmorSetModels(MainItems.GOKU_KID_ARMOR);
		generateArmorSetModels(MainItems.GOKU_SUPER_ARMOR);
		generateArmorSetModels(MainItems.GOKU_WHIS_ARMOR);
		generateArmorSetModels(MainItems.GOTEN_ARMOR);
		generateArmorSetModels(MainItems.GOTEN_SUPER_ARMOR);
		generateArmorSetModels(MainItems.GRANOLA_ARMOR);
		generateArmorSetModels(MainItems.GREAT_SAIYAMAN_ARMOR);
		generateArmorSetModels(MainItems.HIT_ARMOR);
		generateArmorSetModels(MainItems.INVENCIBLE_ARMOR);
		generateArmorSetModels(MainItems.INVENCIBLE_BLUE_ARMOR);
		generateArmorSetModels(MainItems.KALE_ARMOR);
		generateArmorSetModels(MainItems.KEFLA_ARMOR);
		generateArmorSetModels(MainItems.KIBITO_ARMOR);
		generateArmorSetModels(MainItems.KING_VEGETA_ARMOR);
		generateArmorSetModels(MainItems.MAJIN21_ARMOR);
		generateArmorSetModels(MainItems.MAJIN_BUU_ARMOR);
		generateArmorSetModels(MainItems.MIGHTY_MAJIN_ARMOR);
		generateArmorSetModels(MainItems.MYSTIC_ARMOR);
		generateArmorSetModels(MainItems.NARUKE_ARMOR);
		generateArmorSetModels(MainItems.ORANGE_HIGH_ARMOR);
		generateArmorSetModels(MainItems.PICCOLO_ARMOR);
		generateArmorSetModels(MainItems.PRIDE_TROOPS_ARMOR);
		generateArmorSetModels(MainItems.SHIN_ARMOR);
		generateArmorSetModels(MainItems.SLUG_ARMOR);
		generateArmorSetModels(MainItems.STRONGEST_ARMOR);
		generateArmorSetModels(MainItems.SUPER_BUU_ARMOR);
		generateArmorSetModels(MainItems.TIEN_ARMOR);
		generateArmorSetModels(MainItems.TRUNKS_KID_ARMOR);
		generateArmorSetModels(MainItems.TRUNKS_SUPER_ARMOR);
		generateArmorSetModels(MainItems.TRUNKS_Z_ARMOR);
		generateArmorSetModels(MainItems.TURLES_ARMOR);
		generateArmorSetModels(MainItems.VEGETA_BUU_ARMOR);
		generateArmorSetModels(MainItems.VEGETA_GT_ARMOR);
		generateArmorSetModels(MainItems.VEGETA_NAMEK_ARMOR);
		generateArmorSetModels(MainItems.VEGETA_SAIYAN_ARMOR);
		generateArmorSetModels(MainItems.VEGETA_SUPER_ARMOR);
		generateArmorSetModels(MainItems.VEGETA_WHIS_ARMOR);
		generateArmorSetModels(MainItems.VEGETA_Z_ARMOR);
		generateArmorSetModels(MainItems.VEGETTO_ARMOR);
		generateArmorSetModels(MainItems.VIDEL_ARMOR);
		generateArmorSetModels(MainItems.WARRIOR_CLAN_ARMOR);
		generateArmorSetModels(MainItems.WHIS_ARMOR);
		generateArmorSetModels(MainItems.WONDER_MAJIN_ARMOR);
		generateArmorSetModels(MainItems.XENO_GOKU_ARMOR);
		generateArmorSetModels(MainItems.YARDRAT_ARMOR);
		generateArmorSetModels(MainItems.ZAMASU_ARMOR);
        generateArmorSetModels(MainItems.THRAGG_ARMOR);
        generateArmorSetModels(MainItems.GILGAMESH_ARMOR);
        generateArmorSetModels(MainItems.SUBARU_NATSUKI_ARMOR);
        generateArmorSetModels(MainItems.SUBARU_NATSUKI_ARC6_ARMOR);
        generateArmorSetModels(MainItems.RADITZ_ARMOR);
        generateArmorSetModels(MainItems.GERO_ARMOR);
        generateArmorSetModels(MainItems.COOLER_SOLDIER_ARMOR);
        generateArmorSetModels(MainItems.CAPSULE_CORP_ARMOR);
        generateArmorSetModels(MainItems.XENO_GOKU_PATREON_ARMOR);
        generateArmorSetModels(MainItems.VERGIL_ARMOR);
        generateArmorSetModels(MainItems.GREAT_SAIYAMAN_2_ARMOR);

        //Crafting Armaduras
		simpleItem(MainItems.KIKONO_STRING);
		simpleItem(MainItems.KIKONO_CLOTH);
		simpleItem(MainItems.KIKONO_STICK);
		simpleItem(MainItems.ARMOR_CRAFTING_KIT);
		simpleItem(MainItems.BLANK_PATTERN_Z);
		simpleItem(MainItems.BLANK_PATTERN_SUPER);
		patternItem(MainItems.PATTERN_A13);
		patternItem(MainItems.PATTERN_A16);
		patternItem(MainItems.PATTERN_A17);
		patternItem(MainItems.PATTERN_A17_SUPER);
		patternItem(MainItems.PATTERN_A18);
		patternItem(MainItems.PATTERN_A18_CELL);
		patternItem(MainItems.PATTERN_A18_KAME);
		patternItem(MainItems.PATTERN_A18_TOURNAMENT);
		patternItem(MainItems.PATTERN_AGE1000);
		patternItem(MainItems.PATTERN_BARDOCK1);
		patternItem(MainItems.PATTERN_BARDOCK2);
		patternItem(MainItems.PATTERN_BEERUS);
		patternItem(MainItems.PATTERN_BLACK);
		patternItem(MainItems.PATTERN_BROLY_SUPER);
		patternItem(MainItems.PATTERN_BROLY_Z);
		patternItem(MainItems.PATTERN_CAULIFLA);
		patternItem(MainItems.PATTERN_CHAOZ);
		patternItem(MainItems.PATTERN_DRAGON_CLAN);
		patternItem(MainItems.PATTERN_EVIL_BUU);
		patternItem(MainItems.PATTERN_FIGHTER);
		patternItem(MainItems.PATTERN_FUSION_ZAMASU);
		patternItem(MainItems.PATTERN_FUTURE_GOHAN);
		patternItem(MainItems.PATTERN_GAMMA1);
		patternItem(MainItems.PATTERN_GAMMA2);
		patternItem(MainItems.PATTERN_GAS);
		patternItem(MainItems.PATTERN_GETE);
		patternItem(MainItems.PATTERN_GINE);
		patternItem(MainItems.PATTERN_GOGETA);
		patternItem(MainItems.PATTERN_GOHAN1);
		patternItem(MainItems.PATTERN_GOHAN_SUPER);
		patternItem(MainItems.PATTERN_GOKU1);
		patternItem(MainItems.PATTERN_GOKU_GT);
		patternItem(MainItems.PATTERN_GOKU_KID);
		patternItem(MainItems.PATTERN_GOKU_SUPER);
		patternItem(MainItems.PATTERN_GOKU_WHIS);
		patternItem(MainItems.PATTERN_GOTEN);
		patternItem(MainItems.PATTERN_GOTEN_SUPER);
		patternItem(MainItems.PATTERN_GRANOLA);
		patternItem(MainItems.PATTERN_GREAT_SAIYAMAN);
		patternItem(MainItems.PATTERN_HIT);
		patternItem(MainItems.PATTERN_KALE);
		patternItem(MainItems.PATTERN_KEFLA);
		patternItem(MainItems.PATTERN_KIBITO);
		patternItem(MainItems.PATTERN_MAJIN21);
		patternItem(MainItems.PATTERN_MAJIN_BUU);
		patternItem(MainItems.PATTERN_MIGHTY_MAJIN);
		patternItem(MainItems.PATTERN_MYSTIC);
		patternItem(MainItems.PATTERN_ORANGE_HIGH);
		patternItem(MainItems.PATTERN_PICCOLO);
		patternItem(MainItems.PATTERN_PRIDE_TROOPS);
		patternItem(MainItems.PATTERN_SHIN);
		patternItem(MainItems.PATTERN_SLUG);
		patternItem(MainItems.PATTERN_SUPER_BUU);
		patternItem(MainItems.PATTERN_TIEN);
		patternItem(MainItems.PATTERN_TRUNKS_KID);
		patternItem(MainItems.PATTERN_TRUNKS_SUPER);
		patternItem(MainItems.PATTERN_TRUNKS_Z);
		patternItem(MainItems.PATTERN_TURLES);
		patternItem(MainItems.PATTERN_VEGETA1);
		patternItem(MainItems.PATTERN_VEGETA2);
		patternItem(MainItems.PATTERN_VEGETA_BUU);
		patternItem(MainItems.PATTERN_VEGETA_GT);
		patternItem(MainItems.PATTERN_VEGETA_SUPER);
		patternItem(MainItems.PATTERN_VEGETA_WHIS);
		patternItem(MainItems.PATTERN_VEGETA_Z);
		patternItem(MainItems.PATTERN_VEGETTO);
		patternItem(MainItems.PATTERN_VIDEL);
		patternItem(MainItems.PATTERN_WARRIOR_CLAN);
		patternItem(MainItems.PATTERN_WHIS);
		patternItem(MainItems.PATTERN_WONDER_MAJIN);
		patternItem(MainItems.PATTERN_XENO_GOKU);
		patternItem(MainItems.PATTERN_YARDRAT);
		patternItem(MainItems.PATTERN_ZAMASU);

		//Minerales
		simpleItem(MainItems.GETE_SCRAP);
		simpleItem(MainItems.GETE_INGOT);
		simpleItem(MainItems.KIKONO_SHARD);

		//Gete Cosas
		simpleItem(MainItems.GETE_RED_CAPSULE);
		simpleItem(MainItems.GETE_PURPLE_CAPSULE);
		simpleItem(MainItems.GETE_YELLOW_CAPSULE);
		simpleItem(MainItems.GETE_GREEN_CAPSULE);
		simpleItem(MainItems.GETE_ORANGE_CAPSULE);
		simpleItem(MainItems.GETE_BLUE_CAPSULE);

		//Bulma Cosas
		simpleItem(MainItems.KI_BATTERY);
		simpleItem(MainItems.ANTI_KI_CLOAK);

		//Pesos
		simpleItem(MainItems.WEIGHT_TURTLE_SHELL);
		simpleItem(MainItems.WORKOUT_WEIGHTS);
		simpleItem(MainItems.WEIGHT_PICCOLO_CAPE);

		//Bloques (MainBlocks)
		simpleBlockItem(MainBlocks.NAMEK_BLOCK);
		simpleBlockItem(MainBlocks.NAMEK_DIRT);
		simpleBlockItem(MainBlocks.NAMEK_STONE);
		simpleBlockItem(MainBlocks.NAMEK_COBBLESTONE);
		simpleBlockItem(MainBlocks.ROCKY_STONE);
		simpleBlockItem(MainBlocks.ROCKY_COBBLESTONE);
		simpleBlockItem(MainBlocks.NAMEK_AJISSA_PLANKS);
		simpleBlockItem(MainBlocks.NAMEK_AJISSA_LEAVES);
		simpleBlockItem(MainBlocks.NAMEK_SACRED_PLANKS);
		simpleBlockItem(MainBlocks.NAMEK_SACRED_LEAVES);
		simpleBlockItem(MainBlocks.GETE_BLOCK);
		simpleBlockItem(MainBlocks.NAMEK_KIKONO_ORE);
		simpleBlockItem(MainBlocks.KIKONO_BLOCK);
		simpleBlockItem(MainBlocks.NAMEK_DIAMOND_ORE);
		simpleBlockItem(MainBlocks.NAMEK_GOLD_ORE);
		simpleBlockItem(MainBlocks.NAMEK_IRON_ORE);
		simpleBlockItem(MainBlocks.NAMEK_LAPIS_ORE);
		simpleBlockItem(MainBlocks.NAMEK_REDSTONE_ORE);
		simpleBlockItem(MainBlocks.NAMEK_COAL_ORE);
		simpleBlockItem(MainBlocks.NAMEK_EMERALD_ORE);
		simpleBlockItem(MainBlocks.NAMEK_COPPER_ORE);
		simpleBlockItem(MainBlocks.NAMEK_DEEPSLATE_DIAMOND);
		simpleBlockItem(MainBlocks.NAMEK_DEEPSLATE_GOLD);
		simpleBlockItem(MainBlocks.NAMEK_DEEPSLATE_IRON);
		simpleBlockItem(MainBlocks.NAMEK_DEEPSLATE_LAPIS);
		simpleBlockItem(MainBlocks.NAMEK_DEEPSLATE_REDSTONE);
		simpleBlockItem(MainBlocks.NAMEK_DEEPSLATE_COAL);
		simpleBlockItem(MainBlocks.NAMEK_DEEPSLATE_EMERALD);
		simpleBlockItem(MainBlocks.NAMEK_DEEPSLATE_COPPER);
		simpleBlockItem(MainBlocks.TIME_CHAMBER_PORTAL);
		simpleBlockItem(MainBlocks.OTHERWORLD_CLOUD);
		simpleBlockItem(MainBlocks.GETE_ORE);

		//Variantes de bloques
		blockAsItem(MainBlocks.NAMEK_AJISSA_DOOR);
		blockAsItem(MainBlocks.NAMEK_SACRED_DOOR);
		fenceItem(MainBlocks.NAMEK_AJISSA_FENCE, MainBlocks.NAMEK_AJISSA_PLANKS);
		fenceItem(MainBlocks.NAMEK_SACRED_FENCE, MainBlocks.NAMEK_SACRED_PLANKS);
		buttonItem(MainBlocks.NAMEK_AJISSA_BUTTON, MainBlocks.NAMEK_AJISSA_PLANKS);
		buttonItem(MainBlocks.NAMEK_SACRED_BUTTON, MainBlocks.NAMEK_SACRED_PLANKS);
		simpleBlockItem(MainBlocks.NAMEK_STONE_STAIRS);
		simpleBlockItem(MainBlocks.NAMEK_COBBLESTONE_STAIRS);
		simpleBlockItem(MainBlocks.NAMEK_DEEPSLATE_STAIRS);
		simpleBlockItem(MainBlocks.ROCKY_STONE_STAIRS);
		simpleBlockItem(MainBlocks.ROCKY_COBBLESTONE_STAIRS);
		simpleBlockItem(MainBlocks.NAMEK_AJISSA_STAIRS);
		simpleBlockItem(MainBlocks.NAMEK_SACRED_STAIRS);
		simpleBlockItem(MainBlocks.NAMEK_STONE_SLAB);
		simpleBlockItem(MainBlocks.NAMEK_COBBLESTONE_SLAB);
		simpleBlockItem(MainBlocks.NAMEK_DEEPSLATE_SLAB);
		simpleBlockItem(MainBlocks.ROCKY_STONE_SLAB);
		simpleBlockItem(MainBlocks.ROCKY_COBBLESTONE_SLAB);
		simpleBlockItem(MainBlocks.NAMEK_AJISSA_SLAB);
		simpleBlockItem(MainBlocks.NAMEK_SACRED_SLAB);
		simpleBlockItem(MainBlocks.NAMEK_AJISSA_PRESSURE_PLATE);
		simpleBlockItem(MainBlocks.NAMEK_SACRED_PRESSURE_PLATE);
		simpleBlockItem(MainBlocks.NAMEK_AJISSA_FENCE_GATE);
		simpleBlockItem(MainBlocks.NAMEK_SACRED_FENCE_GATE);
		trapdoorItem(MainBlocks.NAMEK_AJISSA_TRAPDOOR);
		trapdoorItem(MainBlocks.NAMEK_SACRED_TRAPDOOR);
		wallItem(MainBlocks.NAMEK_STONE_WALL, MainBlocks.NAMEK_STONE);
		wallItem(MainBlocks.NAMEK_COBBLESTONE_WALL, MainBlocks.NAMEK_COBBLESTONE);
		wallItem(MainBlocks.NAMEK_DEEPSLATE_WALL, MainBlocks.NAMEK_DEEPSLATE);
		wallItem(MainBlocks.ROCKY_STONE_WALL, MainBlocks.ROCKY_STONE);
		wallItem(MainBlocks.ROCKY_COBBLESTONE_WALL, MainBlocks.ROCKY_COBBLESTONE);

		//Vegetacion
		blockAsItem(MainBlocks.CHRYSANTHEMUM_FLOWER);
		blockAsItem(MainBlocks.AMARYLLIS_FLOWER);
		blockAsItem(MainBlocks.MARIGOLD_FLOWER);
		blockAsItem(MainBlocks.CATHARANTHUS_ROSEUS_FLOWER);
		blockAsItem(MainBlocks.TRILLIUM_FLOWER);
		blockItem(MainBlocks.NAMEK_FERN);
		saplingItem(MainBlocks.NAMEK_SACRED_SAPLING);
		blockAsItem(MainBlocks.SACRED_CHRYSANTHEMUM_FLOWER);
		blockAsItem(MainBlocks.SACRED_AMARYLLIS_FLOWER);
		blockAsItem(MainBlocks.SACRED_MARIGOLD_FLOWER);
		blockAsItem(MainBlocks.SACRED_CATHARANTHUS_ROSEUS_FLOWER);
		blockAsItem(MainBlocks.SACRED_TRILLIUM_FLOWER);
		blockItem(MainBlocks.SACRED_FERN);
		saplingItem(MainBlocks.NAMEK_AJISSA_SAPLING);

		// Herramientas
		handheldItem(MainItems.GETE_PICKAXE);
		handheldItem(MainItems.GETE_AXE);
		handheldItem(MainItems.GETE_SHOVEL);
		handheldItem(MainItems.GETE_HOE);

	}

	private void simpleItem(RegistryObject<Item> item) {
		withExistingParent(item.getId().getPath(),
				ResourceLocation.parse("item/generated")).texture("layer0",
				ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "item/" + item.getId().getPath()));
	}
	private void armorItem(RegistryObject<Item> item) {
		withExistingParent(item.getId().getPath(),
				ResourceLocation.parse("item/generated")).texture("layer0",
				ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "item/armors/" + item.getId().getPath()));
	}
	private void patternItem(RegistryObject<Item> item) {
		withExistingParent(item.getId().getPath(),
				ResourceLocation.parse("item/generated")).texture("layer0",
				ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "item/patterns/" + item.getId().getPath()));
	}
	private void blockItem(RegistryObject<Block> item) {
		withExistingParent(item.getId().getPath(),
				ResourceLocation.parse("item/generated")).texture("layer0",
				ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "block/" + item.getId().getPath()));
	}
	private void blockAsItem(RegistryObject<Block> item) {
		withExistingParent(item.getId().getPath(),
				ResourceLocation.parse("item/generated")).texture("layer0",
				ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "item/" + item.getId().getPath()));
	}
	public void simpleBlockItem(RegistryObject<Block> block) {
		this.withExistingParent(Reference.MOD_ID + ":" + ForgeRegistries.BLOCKS.getKey(block.get()).getPath(),
				modLoc("block/" + ForgeRegistries.BLOCKS.getKey(block.get()).getPath()));
	}
	public void trapdoorItem(RegistryObject<Block> block) {
		this.withExistingParent(ForgeRegistries.BLOCKS.getKey(block.get()).getPath(),
				modLoc("block/" + ForgeRegistries.BLOCKS.getKey(block.get()).getPath() + "_bottom"));
	}

	public void fenceItem(RegistryObject<Block> block, RegistryObject<Block> baseBlock) {
		this.withExistingParent(ForgeRegistries.BLOCKS.getKey(block.get()).getPath(), mcLoc("block/fence_inventory"))
				.texture("texture",  ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "block/" + ForgeRegistries.BLOCKS.getKey(baseBlock.get()).getPath()));
	}

	public void buttonItem(RegistryObject<Block> block, RegistryObject<Block> baseBlock) {
		this.withExistingParent(ForgeRegistries.BLOCKS.getKey(block.get()).getPath(), mcLoc("block/button_inventory"))
				.texture("texture",  ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "block/" + ForgeRegistries.BLOCKS.getKey(baseBlock.get()).getPath()));
	}
	public void wallItem(RegistryObject<Block> block, RegistryObject<Block> baseBlock) {
		this.withExistingParent(ForgeRegistries.BLOCKS.getKey(block.get()).getPath(), mcLoc("block/wall_inventory"))
				.texture("wall",  ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "block/" + ForgeRegistries.BLOCKS.getKey(baseBlock.get()).getPath()));
	}
	private void saplingItem(RegistryObject<Block> item) {
		withExistingParent(item.getId().getPath(),
				ResourceLocation.parse("item/generated")).texture("layer0",
				ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "block/" + item.getId().getPath()));
	}
	private void generateArmorSetModels(Map<ArmorItem.Type, RegistryObject<Item>> armorSet) {
		for (RegistryObject<Item> piece : armorSet.values()) {
			armorItem(piece);
		}
	}
	private void handheldItem(RegistryObject<Item> item) {
		withExistingParent(item.getId().getPath(),
				ResourceLocation.parse("item/handheld")).texture("layer0",
				ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "item/" + item.getId().getPath()));
	}
}
