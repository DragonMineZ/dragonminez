package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class DMZItemModelProvider extends net.minecraftforge.client.model.generators.ItemModelProvider {
	public DMZItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
		super(output, Reference.MOD_ID, existingFileHelper);
	}

	@Override
	protected void registerModels() {
		//Items (MainItems)
		simpleItem(MainItems.DBALL_RADAR_ITEM);
		simpleItem(MainItems.NAMEKDBALL_RADAR_ITEM);
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
		simpleItem(MainItems.HEART_MEDICINE);
		simpleItem(MainItems.NAMEK_WATER_BUCKET);
		simpleItem(MainItems.HEALING_BUCKET);
		simpleItem(MainItems.DBALL1_BLOCK_ITEM);
		simpleItem(MainItems.DBALL2_BLOCK_ITEM);
		simpleItem(MainItems.DBALL3_BLOCK_ITEM);
		simpleItem(MainItems.DBALL4_BLOCK_ITEM);
		simpleItem(MainItems.DBALL5_BLOCK_ITEM);
		simpleItem(MainItems.DBALL6_BLOCK_ITEM);
		simpleItem(MainItems.DBALL7_BLOCK_ITEM);
		simpleItem(MainItems.DBALL1_NAMEK_BLOCK_ITEM);
		simpleItem(MainItems.DBALL2_NAMEK_BLOCK_ITEM);
		simpleItem(MainItems.DBALL3_NAMEK_BLOCK_ITEM);
		simpleItem(MainItems.DBALL4_NAMEK_BLOCK_ITEM);
		simpleItem(MainItems.DBALL5_NAMEK_BLOCK_ITEM);
		simpleItem(MainItems.DBALL6_NAMEK_BLOCK_ITEM);
		simpleItem(MainItems.DBALL7_NAMEK_BLOCK_ITEM);
		simpleItem(MainItems.RADAR_PIECE);
		simpleItem(MainItems.T1_RADAR_CHIP);
		simpleItem(MainItems.T2_RADAR_CHIP);
		simpleItem(MainItems.T1_RADAR_CPU);
		simpleItem(MainItems.T2_RADAR_CPU);
        simpleItem(MainItems.GREEN_SCOUTER);
        simpleItem(MainItems.RED_SCOUTER);
        simpleItem(MainItems.BLUE_SCOUTER);
        simpleItem(MainItems.YELLOW_SCOUTER);
		// Spawn Eggs
//		withExistingParent(MainItems.DINO_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.NAMEK_FROG_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.GINYU_FROG_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.PINK_FROG_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.YELLOW_FROG_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.SOLDIER01_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.SOLDIER02_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.SOLDIER03_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.MORO_SOLDIER_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.NWARRIOR1_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.NWARRIOR2_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.SAIBAMAN_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.KAIWAREMAN_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.KYUKONMAN_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.COPYMAN_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.TENNENMAN_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.JINKOUMAN_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));
//		withExistingParent(MainItems.REDRIBBONSOLDIER_SE.getId().getPath(), mcLoc("item/template_spawn_egg"));

		//Comidas
		simpleItem(MainItems.COMIDA_DINO_RAW);
		simpleItem(MainItems.COMIDA_DINO_COOKED);
		simpleItem(MainItems.DINO_TAIL_RAW);
		simpleItem(MainItems.DINO_TAIL_COOKED);
		simpleItem(MainItems.FROG_LEGS_RAW);
		simpleItem(MainItems.FROG_LEGS_COOKED);

		//Armaduras
//		armorItem(MainItems.GOKU_KID_ARMOR_CHESTPLATE);
//		armorItem(MainItems.GOKU_KID_ARMOR_LEGGINGS);
//		armorItem(MainItems.GOKU_KID_ARMOR_BOOTS);
//		armorItem(MainItems.GOKU_ARMOR_CHESTPLATE);
//		armorItem(MainItems.GOKU_ARMOR_LEGGINGS);
//		armorItem(MainItems.GOKU_ARMOR_BOOTS);
//		armorItem(MainItems.GOKU_KAITO_ARMOR_CHESTPLATE);
//		armorItem(MainItems.GOKU_KAITO_ARMOR_LEGGINGS);
//		armorItem(MainItems.GOKU_KAITO_ARMOR_BOOTS);
//		armorItem(MainItems.GOKU_SUPER_ARMOR_CHESTPLATE);
//		armorItem(MainItems.GOKU_SUPER_ARMOR_LEGGINGS);
//		armorItem(MainItems.GOKU_SUPER_ARMOR_BOOTS);
//		armorItem(MainItems.GOKU_GT_ARMOR_CHESTPLATE);
//		armorItem(MainItems.GOKU_GT_ARMOR_LEGGINGS);
//		armorItem(MainItems.GOKU_GT_ARMOR_BOOTS);
//		armorItem(MainItems.YARDRAT_ARMOR_CHESTPLATE);
//		armorItem(MainItems.YARDRAT_ARMOR_LEGGINGS);
//		armorItem(MainItems.YARDRAT_ARMOR_BOOTS);
//		armorItem(MainItems.GOTEN_ARMOR_CHESTPLATE);
//		armorItem(MainItems.GOTEN_ARMOR_LEGGINGS);
//		armorItem(MainItems.GOTEN_ARMOR_BOOTS);
//		armorItem(MainItems.GOTEN_SUPER_ARMOR_CHESTPLATE);
//		armorItem(MainItems.GOTEN_SUPER_ARMOR_LEGGINGS);
//		armorItem(MainItems.GOTEN_SUPER_ARMOR_BOOTS);
//		armorItem(MainItems.GOHAN_SUPER_ARMOR_CHESTPLATE);
//		armorItem(MainItems.GOHAN_SUPER_ARMOR_LEGGINGS);
//		armorItem(MainItems.GOHAN_SUPER_ARMOR_BOOTS);
//		armorItem(MainItems.GREAT_SAIYAMAN_ARMOR_HELMET);
//		armorItem(MainItems.GREAT_SAIYAMAN_ARMOR_CHESTPLATE);
//		armorItem(MainItems.GREAT_SAIYAMAN_ARMOR_LEGGINGS);
//		armorItem(MainItems.GREAT_SAIYAMAN_ARMOR_BOOTS);
//		armorItem(MainItems.FUTURE_GOHAN_ARMOR_CHESTPLATE);
//		armorItem(MainItems.FUTURE_GOHAN_ARMOR_LEGGINGS);
//		armorItem(MainItems.FUTURE_GOHAN_ARMOR_BOOTS);
//		armorItem(MainItems.VEGETA_SAIYAN_ARMOR_CHESTPLATE);
//		armorItem(MainItems.VEGETA_SAIYAN_ARMOR_LEGGINGS);
//		armorItem(MainItems.VEGETA_SAIYAN_ARMOR_BOOTS);
//		armorItem(MainItems.VEGETA_NAMEK_ARMOR_CHESTPLATE);
//		armorItem(MainItems.VEGETA_NAMEK_ARMOR_LEGGINGS);
//		armorItem(MainItems.VEGETA_NAMEK_ARMOR_BOOTS);
//		armorItem(MainItems.VEGETA_Z_ARMOR_CHESTPLATE);
//		armorItem(MainItems.VEGETA_Z_ARMOR_LEGGINGS);
//		armorItem(MainItems.VEGETA_Z_ARMOR_BOOTS);
//		armorItem(MainItems.VEGETA_BUU_ARMOR_CHESTPLATE);
//		armorItem(MainItems.VEGETA_BUU_ARMOR_LEGGINGS);
//		armorItem(MainItems.VEGETA_BUU_ARMOR_BOOTS);
//		armorItem(MainItems.VEGETA_SUPER_ARMOR_CHESTPLATE);
//		armorItem(MainItems.VEGETA_SUPER_ARMOR_LEGGINGS);
//		armorItem(MainItems.VEGETA_SUPER_ARMOR_BOOTS);
//		armorItem(MainItems.VEGETTO_ARMOR_CHESTPLATE);
//		armorItem(MainItems.VEGETTO_ARMOR_LEGGINGS);
//		armorItem(MainItems.VEGETTO_ARMOR_BOOTS);
//		armorItem(MainItems.GOGETA_ARMOR_CHESTPLATE);
//		armorItem(MainItems.GOGETA_ARMOR_LEGGINGS);
//		armorItem(MainItems.GOGETA_ARMOR_BOOTS);
//		armorItem(MainItems.PICCOLO_ARMOR_HELMET);
//		armorItem(MainItems.PICCOLO_ARMOR_CHESTPLATE_CAPE);
//		armorItem(MainItems.PICCOLO_ARMOR_CHESTPLATE);
//		armorItem(MainItems.PICCOLO_ARMOR_LEGGINGS);
//		armorItem(MainItems.PICCOLO_ARMOR_BOOTS);
//		armorItem(MainItems.DEMON_GI_BLUE_ARMOR_CHESTPLATE);
//		armorItem(MainItems.DEMON_GI_BLUE_ARMOR_LEGGINGS);
//		armorItem(MainItems.DEMON_GI_BLUE_ARMOR_BOOTS);
//		armorItem(MainItems.BARDOCK_DBZ_ARMOR_CHESTPLATE);
//		armorItem(MainItems.BARDOCK_DBZ_ARMOR_LEGGINGS);
//		armorItem(MainItems.BARDOCK_DBZ_ARMOR_BOOTS);
//		armorItem(MainItems.BARDOCK_SUPER_ARMOR_CHESTPLATE);
//		armorItem(MainItems.BARDOCK_SUPER_ARMOR_LEGGINGS);
//		armorItem(MainItems.BARDOCK_SUPER_ARMOR_BOOTS);
//		armorItem(MainItems.TURLES_ARMOR_CHESTPLATE);
//		armorItem(MainItems.TURLES_ARMOR_LEGGINGS);
//		armorItem(MainItems.TURLES_ARMOR_BOOTS);
//		armorItem(MainItems.TIEN_ARMOR_CHESTPLATE);
//		armorItem(MainItems.TIEN_ARMOR_LEGGINGS);
//		armorItem(MainItems.TIEN_ARMOR_BOOTS);
//		armorItem(MainItems.TRUNKS_Z_ARMOR_CHESTPLATE);
//		armorItem(MainItems.TRUNKS_Z_ARMOR_LEGGINGS);
//		armorItem(MainItems.TRUNKS_Z_ARMOR_BOOTS);
//		armorItem(MainItems.TRUNKS_SUPER_ARMOR_CHESTPLATE);
//		armorItem(MainItems.TRUNKS_SUPER_ARMOR_LEGGINGS);
//		armorItem(MainItems.TRUNKS_SUPER_ARMOR_BOOTS);
//		armorItem(MainItems.TRUNKS_KID_ARMOR_CHESTPLATE);
//		armorItem(MainItems.TRUNKS_KID_ARMOR_LEGGINGS);
//		armorItem(MainItems.TRUNKS_KID_ARMOR_BOOTS);
//		armorItem(MainItems.BROLY_Z_ARMOR_CHESTPLATE);
//		armorItem(MainItems.BROLY_Z_ARMOR_LEGGINGS);
//		armorItem(MainItems.BROLY_Z_ARMOR_BOOTS);
//		armorItem(MainItems.BROLY_SUPER_ARMOR_CHESTPLATE);
//		armorItem(MainItems.BROLY_SUPER_ARMOR_LEGGINGS);
//		armorItem(MainItems.BROLY_SUPER_ARMOR_BOOTS);
//		armorItem(MainItems.SHIN_ARMOR_CHESTPLATE);
//		armorItem(MainItems.SHIN_ARMOR_LEGGINGS);
//		armorItem(MainItems.SHIN_ARMOR_BOOTS);
//		armorItem(MainItems.BLACK_ARMOR_CHESTPLATE);
//		armorItem(MainItems.BLACK_ARMOR_LEGGINGS);
//		armorItem(MainItems.BLACK_ARMOR_BOOTS);
//		armorItem(MainItems.ZAMASU_ARMOR_CHESTPLATE);
//		armorItem(MainItems.ZAMASU_ARMOR_LEGGINGS);
//		armorItem(MainItems.ZAMASU_ARMOR_BOOTS);
//		armorItem(MainItems.FUSION_ZAMASU_ARMOR_CHESTPLATE);
//		armorItem(MainItems.FUSION_ZAMASU_ARMOR_LEGGINGS);
//		armorItem(MainItems.FUSION_ZAMASU_ARMOR_BOOTS);
//		armorItem(MainItems.PRIDE_TROOPS_ARMOR_CHESTPLATE);
//		armorItem(MainItems.PRIDE_TROOPS_ARMOR_LEGGINGS);
//		armorItem(MainItems.PRIDE_TROOPS_ARMOR_BOOTS);
//		armorItem(MainItems.HIT_ARMOR_CHESTPLATE);
//		armorItem(MainItems.HIT_ARMOR_LEGGINGS);
//		armorItem(MainItems.HIT_ARMOR_BOOTS);
//		armorItem(MainItems.GAS_ARMOR_CHESTPLATE);
//		armorItem(MainItems.GAS_ARMOR_LEGGINGS);
//		armorItem(MainItems.GAS_ARMOR_BOOTS);
//		armorItem(MainItems.MAJIN_BUU_ARMOR_CHESTPLATE);
//		armorItem(MainItems.MAJIN_BUU_ARMOR_LEGGINGS);
//		armorItem(MainItems.MAJIN_BUU_ARMOR_BOOTS);
//		armorItem(MainItems.GAMMA1_ARMOR_CHESTPLATE);
//		armorItem(MainItems.GAMMA1_ARMOR_LEGGINGS);
//		armorItem(MainItems.GAMMA1_ARMOR_BOOTS);
//		armorItem(MainItems.GAMMA2_ARMOR_CHESTPLATE);
//		armorItem(MainItems.GAMMA2_ARMOR_LEGGINGS);
//		armorItem(MainItems.GAMMA2_ARMOR_BOOTS);
//		armorItem(MainItems.INVENCIBLE_ARMOR_HELMET);
//		armorItem(MainItems.INVENCIBLE_ARMOR_CHESTPLATE);
//		armorItem(MainItems.INVENCIBLE_ARMOR_LEGGINGS);
//		armorItem(MainItems.INVENCIBLE_ARMOR_BOOTS);

		//Crafting Armaduras
		simpleItem(MainItems.KIKONO_STRING);
		simpleItem(MainItems.KIKONO_CLOTH);
		simpleItem(MainItems.ARMOR_CRAFTING_KIT);
		simpleItem(MainItems.BLANK_PATTERN_Z);
		simpleItem(MainItems.BLANK_PATTERN_SUPER);
		patternItem(MainItems.PATTERN_GOKU_KID);
		patternItem(MainItems.PATTERN_GOKU1);
		patternItem(MainItems.PATTERN_GOKU2);
		patternItem(MainItems.PATTERN_GOKU_SUPER);
		patternItem(MainItems.PATTERN_GOKU_GT);
		patternItem(MainItems.PATTERN_YARDRAT);
		patternItem(MainItems.PATTERN_GOTEN);
		patternItem(MainItems.PATTERN_GOTEN_SUPER);
		patternItem(MainItems.PATTERN_GOHAN_SUPER);
		patternItem(MainItems.PATTERN_GREAT_SAIYAMAN);
		patternItem(MainItems.PATTERN_FUTURE_GOHAN);
		patternItem(MainItems.PATTERN_VEGETA1);
		patternItem(MainItems.PATTERN_VEGETA2);
		patternItem(MainItems.PATTERN_VEGETA_Z);
		patternItem(MainItems.PATTERN_VEGETA_BUU);
		patternItem(MainItems.PATTERN_VEGETA_SUPER);
		patternItem(MainItems.PATTERN_VEGETTO);
		patternItem(MainItems.PATTERN_GOGETA);
		patternItem(MainItems.PATTERN_PICCOLO);
		patternItem(MainItems.PATTERN_GOHAN1);
		patternItem(MainItems.PATTERN_BARDOCK1);
		patternItem(MainItems.PATTERN_BARDOCK2);
		patternItem(MainItems.PATTERN_TURLES);
		patternItem(MainItems.PATTERN_TIEN);
		patternItem(MainItems.PATTERN_TRUNKS_Z);
		patternItem(MainItems.PATTERN_TRUNKS_SUPER);
		patternItem(MainItems.PATTERN_TRUNKS_KID);
		patternItem(MainItems.PATTERN_BROLY_Z);
		patternItem(MainItems.PATTERN_BROLY_SUPER);
		patternItem(MainItems.PATTERN_SHIN);
		patternItem(MainItems.PATTERN_BLACK);
		patternItem(MainItems.PATTERN_ZAMASU);
		patternItem(MainItems.PATTERN_FUSION_ZAMASU);
		patternItem(MainItems.PATTERN_PRIDE_TROOPS);
		patternItem(MainItems.PATTERN_HIT);
		patternItem(MainItems.PATTERN_GAS);
		patternItem(MainItems.PATTERN_MAJIN_BUU);
		patternItem(MainItems.PATTERN_GAMMA1);
		patternItem(MainItems.PATTERN_GAMMA2);

		//Minerales
		simpleItem(MainItems.GETE_SCRAP);
		simpleItem(MainItems.GETE_INGOT);
		simpleItem(MainItems.KIKONO_SHARD);

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
		//simpleBlockItem(MainBlocks.GETE_FURNACE);
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

	}

	private ItemModelBuilder simpleItem(RegistryObject<Item> item) {
		return withExistingParent(item.getId().getPath(),
				new ResourceLocation("item/generated")).texture("layer0",
				new ResourceLocation(Reference.MOD_ID, "item/" + item.getId().getPath()));
	}
	private ItemModelBuilder armorItem(RegistryObject<Item> item) {
		return withExistingParent(item.getId().getPath(),
				new ResourceLocation("item/generated")).texture("layer0",
				new ResourceLocation(Reference.MOD_ID, "item/armors/" + item.getId().getPath()));
	}
	private ItemModelBuilder patternItem(RegistryObject<Item> item) {
		return withExistingParent(item.getId().getPath(),
				new ResourceLocation("item/generated")).texture("layer0",
				new ResourceLocation(Reference.MOD_ID, "item/patterns/" + item.getId().getPath()));
	}
	private ItemModelBuilder blockItem(RegistryObject<Block> item) {
		return withExistingParent(item.getId().getPath(),
				new ResourceLocation("item/generated")).texture("layer0",
				new ResourceLocation(Reference.MOD_ID, "block/" + item.getId().getPath()));
	}
	private ItemModelBuilder blockAsItem(RegistryObject<Block> item) {
		return withExistingParent(item.getId().getPath(),
				new ResourceLocation("item/generated")).texture("layer0",
				new ResourceLocation(Reference.MOD_ID, "item/" + item.getId().getPath()));
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
				.texture("texture",  new ResourceLocation(Reference.MOD_ID, "block/" + ForgeRegistries.BLOCKS.getKey(baseBlock.get()).getPath()));
	}

	public void buttonItem(RegistryObject<Block> block, RegistryObject<Block> baseBlock) {
		this.withExistingParent(ForgeRegistries.BLOCKS.getKey(block.get()).getPath(), mcLoc("block/button_inventory"))
				.texture("texture",  new ResourceLocation(Reference.MOD_ID, "block/" + ForgeRegistries.BLOCKS.getKey(baseBlock.get()).getPath()));
	}
	public void wallItem(RegistryObject<Block> block, RegistryObject<Block> baseBlock) {
		this.withExistingParent(ForgeRegistries.BLOCKS.getKey(block.get()).getPath(), mcLoc("block/wall_inventory"))
				.texture("wall",  new ResourceLocation(Reference.MOD_ID, "block/" + ForgeRegistries.BLOCKS.getKey(baseBlock.get()).getPath()));
	}
	private ItemModelBuilder saplingItem(RegistryObject<Block> item) {
		return withExistingParent(item.getId().getPath(),
				new ResourceLocation("item/generated")).texture("layer0",
				new ResourceLocation(Reference.MOD_ID,"block/" + item.getId().getPath()));
	}
}
