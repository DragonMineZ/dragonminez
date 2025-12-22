package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.init.armor.ModArmorMaterials;
import com.dragonminez.common.init.item.*;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("ALL")
public final class MainItems {
	public static final Item.Properties properties = new Item.Properties();
	public static final DeferredRegister<Item> ITEM_REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, Reference.MOD_ID);

	//CAPSULAS
	public static final RegistryObject<Item> RED_CAPSULE = ITEM_REGISTER.register("red_capsule", () -> new CapsuleItem(CapsuleType.RED));
	public static final RegistryObject<Item> PURPLE_CAPSULE = ITEM_REGISTER.register("purple_capsule", () -> new CapsuleItem(CapsuleType.PURPLE));
	public static final RegistryObject<Item> YELLOW_CAPSULE = ITEM_REGISTER.register("yellow_capsule", () -> new CapsuleItem(CapsuleType.YELLOW));
	public static final RegistryObject<Item> GREEN_CAPSULE = ITEM_REGISTER.register("green_capsule", () -> new CapsuleItem(CapsuleType.GREEN));
	public static final RegistryObject<Item> ORANGE_CAPSULE = ITEM_REGISTER.register("orange_capsule", () -> new CapsuleItem(CapsuleType.ORANGE));
	public static final RegistryObject<Item> BLUE_CAPSULE = ITEM_REGISTER.register("blue_capsule", () -> new CapsuleItem(CapsuleType.BLUE));
	public static final RegistryObject<Item> SENZU_BEAN = ITEM_REGISTER.register("senzu_bean",
			() -> new FoodItem(20, 20.0f, false, true));

	public static final RegistryObject<Item> MIGHT_TREE_FRUIT = ITEM_REGISTER.register("might_tree_fruit",
			MightTreeFruitItem::new);
	public static final RegistryObject<Item> COMIDA_DINO_RAW = ITEM_REGISTER.register("raw_dino_meat",
			() -> new FoodItem(4, 4.8f, true));
	public static final RegistryObject<Item> COMIDA_DINO_COOKED = ITEM_REGISTER.register("cooked_dino_meat",
			() -> new FoodItem(8, 12.8f, true));
	public static final RegistryObject<Item> HEART_MEDICINE = ITEM_REGISTER.register("heart_medicine",
			() -> new FoodItem(20, 20.0f, false, false));
	public static final RegistryObject<Item> DINO_TAIL_RAW = ITEM_REGISTER.register("dino_tail_raw",
			() -> new FoodItem(3, 3.6f, true));
	public static final RegistryObject<Item> DINO_TAIL_COOKED = ITEM_REGISTER.register("dino_tail_cooked",
			() -> new FoodItem(6, 9.6f, true));
	public static final RegistryObject<Item> FROG_LEGS_RAW = ITEM_REGISTER.register("frog_legs_raw",
			() -> new FoodItem(2, 2.4f, true));
	public static final RegistryObject<Item> FROG_LEGS_COOKED = ITEM_REGISTER.register("frog_legs_cooked",
			() -> new FoodItem(5, 7.2f, true));

	//POTHALAS
	public static final RegistryObject<Item> POTHALA_LEFT =
			ITEM_REGISTER.register("pothala_left", () -> new Item(properties.stacksTo(1).fireResistant()));
	public static final RegistryObject<Item> POTHALA_RIGHT =
			ITEM_REGISTER.register("pothala_right", () -> new Item(properties.stacksTo(1).fireResistant()));
	public static final RegistryObject<Item> GREEN_POTHALA_LEFT =
			ITEM_REGISTER.register("green_pothala_left", () -> new Item(properties.stacksTo(1).fireResistant()));
	public static final RegistryObject<Item> GREEN_POTHALA_RIGHT =
			ITEM_REGISTER.register("green_pothala_right", () -> new Item(properties.stacksTo(1).fireResistant()));

    public static final RegistryObject<Item> RED_SCOUTER =
            ITEM_REGISTER.register("red_scouter", () -> new Item(properties.stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> BLUE_SCOUTER =
            ITEM_REGISTER.register("blue_scouter", () -> new Item(properties.stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> GREEN_SCOUTER =
            ITEM_REGISTER.register("green_scouter", () -> new Item(properties.stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> YELLOW_SCOUTER =
            ITEM_REGISTER.register("yellow_scouter", () -> new Item(properties.stacksTo(1).fireResistant()));

    //ARMAS
	// 0 + X = Daño | 4 +/- X = Velocidad de ataque | 0 + X = Durabilidad (0 = Irrompible)
//	public static final RegistryObject<Item> BACULO_SAGRADO =
//			ITEM_REGISTER.register("baculo_item", () -> new DMZWeaponItem(8, -1.6F, 0, "baculo_item"));
//	public static final RegistryObject<Item> TRUNKS_SWORD =
//			ITEM_REGISTER.register("trunks_sword", () -> new TrunksSword(new Item.Properties().fireResistant()));
//	public static final RegistryObject<SwordItem> Z_SWORD =
//			ITEM_REGISTER.register("z_sword", () -> new ZSword(new Item.Properties().fireResistant()));
//	public static final RegistryObject<SwordItem> KATANA_YAJIROBE =
//			ITEM_REGISTER.register("yajirobe_katana", () -> new YajirobeKatana(new Item.Properties().fireResistant()));

	//ARMADURAS
	// GOKU NIÑO
//	public static final RegistryObject<Item> GOKU_KID_ARMOR_CHESTPLATE = armorItem("goku_kid_armor_chestplate", ArmorItem.Type.CHESTPLATE, "goku_kid");
//	public static final RegistryObject<Item> GOKU_KID_ARMOR_LEGGINGS = armorItem("goku_kid_armor_leggings", ArmorItem.Type.LEGGINGS, "goku_kid");
//	public static final RegistryObject<Item> GOKU_KID_ARMOR_BOOTS = armorItem("goku_kid_armor_boots", ArmorItem.Type.BOOTS, "goku_kid");
//	//GOKU GI
//	public static final RegistryObject<Item> GOKU_ARMOR_CHESTPLATE = armorItem("goku_armor_chestplate", ArmorItem.Type.CHESTPLATE, "goku_gi", true);
//	public static final RegistryObject<Item> GOKU_ARMOR_LEGGINGS = armorItem("goku_armor_leggings", ArmorItem.Type.LEGGINGS, "goku_gi", true);
//	public static final RegistryObject<Item> GOKU_ARMOR_BOOTS = armorItem("goku_armor_boots", ArmorItem.Type.BOOTS, "goku_gi", true);
//	//Goku Boku no hero (osea mid xdxdxddx)
//	public static final RegistryObject<Item> GOKU_KAITO_ARMOR_CHESTPLATE = armorItem("goku_kaito_armor_chestplate", ArmorItem.Type.CHESTPLATE, "goku_gi_kaito", true);
//	public static final RegistryObject<Item> GOKU_KAITO_ARMOR_LEGGINGS = armorItem("goku_kaito_armor_leggings", ArmorItem.Type.LEGGINGS, "goku_gi_kaito", true);
//	public static final RegistryObject<Item> GOKU_KAITO_ARMOR_BOOTS = armorItem("goku_kaito_armor_boots", ArmorItem.Type.BOOTS, "goku_gi_kaito", true);
//	//Goku Super
//	public static final RegistryObject<Item> GOKU_SUPER_ARMOR_CHESTPLATE = armorItem("goku_super_armor_chestplate", ArmorItem.Type.CHESTPLATE, "goku_super");
//	public static final RegistryObject<Item> GOKU_SUPER_ARMOR_LEGGINGS = armorItem("goku_super_armor_leggings", ArmorItem.Type.LEGGINGS, "goku_super");
//	public static final RegistryObject<Item> GOKU_SUPER_ARMOR_BOOTS = armorItem("goku_super_armor_boots", ArmorItem.Type.BOOTS, "goku_super");
//	// GOKU GT
//	public static final RegistryObject<Item> GOKU_GT_ARMOR_CHESTPLATE = armorItem("goku_gt_armor_chestplate", ArmorItem.Type.CHESTPLATE, "goku_gt");
//	public static final RegistryObject<Item> GOKU_GT_ARMOR_LEGGINGS = armorItem("goku_gt_armor_leggings", ArmorItem.Type.LEGGINGS, "goku_gt");
//	public static final RegistryObject<Item> GOKU_GT_ARMOR_BOOTS = armorItem("goku_gt_armor_boots", ArmorItem.Type.BOOTS, "goku_gt");
//	// YARDRAT
//	public static final RegistryObject<Item> YARDRAT_ARMOR_CHESTPLATE = armorItem("yardrat_armor_chestplate", ArmorItem.Type.CHESTPLATE, "yardrat_gi");
//	public static final RegistryObject<Item> YARDRAT_ARMOR_LEGGINGS = armorItem("yardrat_armor_leggings", ArmorItem.Type.LEGGINGS, "yardrat_gi");
//	public static final RegistryObject<Item> YARDRAT_ARMOR_BOOTS = armorItem("yardrat_armor_boots", ArmorItem.Type.BOOTS, "yardrat_gi");
//	// GOTEN Z
//	public static final RegistryObject<Item> GOTEN_ARMOR_CHESTPLATE = armorItem("goten_armor_chestplate", ArmorItem.Type.CHESTPLATE, "goten_gi");
//	public static final RegistryObject<Item> GOTEN_ARMOR_LEGGINGS = armorItem("goten_armor_leggings", ArmorItem.Type.LEGGINGS, "goten_gi");
//	public static final RegistryObject<Item> GOTEN_ARMOR_BOOTS = armorItem("goten_armor_boots", ArmorItem.Type.BOOTS, "goten_gi");
//	// GOTEN TEEN (SUPER)
//	public static final RegistryObject<Item> GOTEN_SUPER_ARMOR_CHESTPLATE = armorItem("goten_super_armor_chestplate", ArmorItem.Type.CHESTPLATE, "goten_dbs");
//	public static final RegistryObject<Item> GOTEN_SUPER_ARMOR_LEGGINGS = armorItem("goten_super_armor_leggings", ArmorItem.Type.LEGGINGS, "goten_dbs");
//	public static final RegistryObject<Item> GOTEN_SUPER_ARMOR_BOOTS = armorItem("goten_super_armor_boots", ArmorItem.Type.BOOTS, "goten_dbs");
//	// GOHAN GI (SUPER)
//	public static final RegistryObject<Item> GOHAN_SUPER_ARMOR_CHESTPLATE = armorItem("gohan_super_armor_chestplate", ArmorItem.Type.CHESTPLATE, "gohan_dbs");
//	public static final RegistryObject<Item> GOHAN_SUPER_ARMOR_LEGGINGS = armorItem("gohan_super_armor_leggings", ArmorItem.Type.LEGGINGS, "gohan_dbs");
//	public static final RegistryObject<Item> GOHAN_SUPER_ARMOR_BOOTS = armorItem("gohan_super_armor_boots", ArmorItem.Type.BOOTS, "gohan_dbs");
//	// GOHAN GREAT SAIYAMAN
//	public static final RegistryObject<Item> GREAT_SAIYAMAN_ARMOR_HELMET = capeArItem("great_saiyaman_armor_helmet", ArmorItem.Type.HELMET, "saiyaman_gi");
//	public static final RegistryObject<Item> GREAT_SAIYAMAN_ARMOR_CHESTPLATE = capeArItem("great_saiyaman_armor_chestplate", ArmorItem.Type.CHESTPLATE, "saiyaman_gi");
//	public static final RegistryObject<Item> GREAT_SAIYAMAN_ARMOR_LEGGINGS = capeArItem("great_saiyaman_armor_leggings", ArmorItem.Type.LEGGINGS, "saiyaman_gi");
//	public static final RegistryObject<Item> GREAT_SAIYAMAN_ARMOR_BOOTS = capeArItem("great_saiyaman_armor_boots", ArmorItem.Type.BOOTS, "saiyaman_gi");
//	// FUTURE GOHAN (DESCOMENTAR CUANDO SE IMPLEMENTE LA ROPA)
//	public static final RegistryObject<Item> FUTURE_GOHAN_ARMOR_CHESTPLATE = armorItem("future_gohan_armor_chestplate", ArmorItem.Type.CHESTPLATE, "future_gohan");
//	public static final RegistryObject<Item> FUTURE_GOHAN_ARMOR_LEGGINGS = armorItem("future_gohan_armor_leggings", ArmorItem.Type.LEGGINGS, "future_gohan");
//	public static final RegistryObject<Item> FUTURE_GOHAN_ARMOR_BOOTS = armorItem("future_gohan_armor_boots", ArmorItem.Type.BOOTS, "future_gohan");
//	//VEGETA SAGA SAIYAJIN ARMADURA
//	public static final RegistryObject<Item> VEGETA_SAIYAN_ARMOR_CHESTPLATE = saiyArItem("vegeta_saiyan_armor_chestplate", ArmorItem.Type.CHESTPLATE, "vegeta_saiyan_armor", true);
//	public static final RegistryObject<Item> VEGETA_SAIYAN_ARMOR_LEGGINGS = saiyArItem("vegeta_saiyan_armor_leggings", ArmorItem.Type.LEGGINGS, "vegeta_saiyan_armor", true);
//	public static final RegistryObject<Item> VEGETA_SAIYAN_ARMOR_BOOTS = saiyArItem("vegeta_saiyan_armor_boots", ArmorItem.Type.BOOTS, "vegeta_saiyan_armor", true);
//	//VEGETA SAGA NAMEK ARMOR
//	public static final RegistryObject<Item> VEGETA_NAMEK_ARMOR_CHESTPLATE = armorItem("vegeta_namek_armor_chestplate", ArmorItem.Type.CHESTPLATE, "vegetanamek_armor");
//	public static final RegistryObject<Item> VEGETA_NAMEK_ARMOR_LEGGINGS = armorItem("vegeta_namek_armor_leggings", ArmorItem.Type.LEGGINGS, "vegetanamek_armor");
//	public static final RegistryObject<Item> VEGETA_NAMEK_ARMOR_BOOTS = armorItem("vegeta_namek_armor_boots", ArmorItem.Type.BOOTS, "vegetanamek_armor");
//	//VEGETA ARMADURA DE Z
//	public static final RegistryObject<Item> VEGETA_Z_ARMOR_CHESTPLATE = armorItem("vegeta_z_armor_chestplate", ArmorItem.Type.CHESTPLATE, "vegetaz_armor");
//	public static final RegistryObject<Item> VEGETA_Z_ARMOR_LEGGINGS = armorItem("vegeta_z_armor_leggings", ArmorItem.Type.LEGGINGS, "vegetaz_armor");
//	public static final RegistryObject<Item> VEGETA_Z_ARMOR_BOOTS = armorItem("vegeta_z_armor_boots", ArmorItem.Type.BOOTS, "vegetaz_armor");
//	//VEGETA ROPA ENTRENAMIENTO - SAGA BUU
//	public static final RegistryObject<Item> VEGETA_BUU_ARMOR_CHESTPLATE = armorItem("vegeta_buu_armor_chestplate", ArmorItem.Type.CHESTPLATE, "vegetabuu");
//	public static final RegistryObject<Item> VEGETA_BUU_ARMOR_LEGGINGS = armorItem("vegeta_buu_armor_leggings", ArmorItem.Type.LEGGINGS, "vegetabuu");
//	public static final RegistryObject<Item> VEGETA_BUU_ARMOR_BOOTS = armorItem("vegeta_buu_armor_boots", ArmorItem.Type.BOOTS, "vegetabuu");
//	//VEGETA ARMADURA DE SUPER
//	public static final RegistryObject<Item> VEGETA_SUPER_ARMOR_CHESTPLATE = armorItem("vegeta_super_armor_chestplate", ArmorItem.Type.CHESTPLATE, "vegetasuper");
//	public static final RegistryObject<Item> VEGETA_SUPER_ARMOR_LEGGINGS = armorItem("vegeta_super_armor_leggings", ArmorItem.Type.LEGGINGS, "vegetasuper");
//	public static final RegistryObject<Item> VEGETA_SUPER_ARMOR_BOOTS = armorItem("vegeta_super_armor_boots", ArmorItem.Type.BOOTS, "vegetasuper");
//	//VEGETTO
//	public static final RegistryObject<Item> VEGETTO_ARMOR_CHESTPLATE = armorItem("vegetto_armor_chestplate", ArmorItem.Type.CHESTPLATE, "vegetto");
//	public static final RegistryObject<Item> VEGETTO_ARMOR_LEGGINGS = armorItem("vegetto_armor_leggings", ArmorItem.Type.LEGGINGS, "vegetto");
//	public static final RegistryObject<Item> VEGETTO_ARMOR_BOOTS = armorItem("vegetto_armor_boots", ArmorItem.Type.BOOTS, "vegetto");
//	//GOGETA
//	public static final RegistryObject<Item> GOGETA_ARMOR_CHESTPLATE = armorItem("gogeta_armor_chestplate", ArmorItem.Type.CHESTPLATE, "gogeta");
//	public static final RegistryObject<Item> GOGETA_ARMOR_LEGGINGS = armorItem("gogeta_armor_leggings", ArmorItem.Type.LEGGINGS, "gogeta");
//	public static final RegistryObject<Item> GOGETA_ARMOR_BOOTS = armorItem("gogeta_armor_boots", ArmorItem.Type.BOOTS, "gogeta");
//	//PICCOLO
//	public static final RegistryObject<Item> PICCOLO_ARMOR_HELMET = armorItem("piccolo_armor_helmet", ArmorItem.Type.HELMET, "piccolo_gi");
//	public static final RegistryObject<Item> PICCOLO_ARMOR_CHESTPLATE_CAPE = capeHombArItem("piccolo_armor_chestplate_cape", ArmorItem.Type.CHESTPLATE, "piccolo_gi");
//	public static final RegistryObject<Item> PICCOLO_ARMOR_CHESTPLATE = armorItem("piccolo_armor_chestplate", ArmorItem.Type.CHESTPLATE, "piccolo_gi");
//	public static final RegistryObject<Item> PICCOLO_ARMOR_LEGGINGS = armorItem("piccolo_armor_leggings", ArmorItem.Type.LEGGINGS, "piccolo_gi");
//	public static final RegistryObject<Item> PICCOLO_ARMOR_BOOTS = armorItem("piccolo_armor_boots", ArmorItem.Type.BOOTS, "piccolo_gi");
//	//DEMON GI (AZUL)
//	public static final RegistryObject<Item> DEMON_GI_BLUE_ARMOR_CHESTPLATE = armorItem("demon_gi_blue_armor_chestplate", ArmorItem.Type.CHESTPLATE, "demon_gi_gohan");
//	public static final RegistryObject<Item> DEMON_GI_BLUE_ARMOR_LEGGINGS = armorItem("demon_gi_blue_armor_leggings", ArmorItem.Type.LEGGINGS, "demon_gi_gohan");
//	public static final RegistryObject<Item> DEMON_GI_BLUE_ARMOR_BOOTS = armorItem("demon_gi_blue_armor_boots", ArmorItem.Type.BOOTS, "demon_gi_gohan");
//	//BARDOCK DBZ
//	public static final RegistryObject<Item> BARDOCK_DBZ_ARMOR_CHESTPLATE = saiyArItem("bardock_dbz_armor_chestplate", ArmorItem.Type.CHESTPLATE, "bardock_armor");
//	public static final RegistryObject<Item> BARDOCK_DBZ_ARMOR_LEGGINGS = saiyArItem("bardock_dbz_armor_leggings", ArmorItem.Type.LEGGINGS, "bardock_armor");
//	public static final RegistryObject<Item> BARDOCK_DBZ_ARMOR_BOOTS = saiyArItem("bardock_dbz_armor_boots", ArmorItem.Type.BOOTS, "bardock_armor");
//	//BARDOCK SUPER
//	public static final RegistryObject<Item> BARDOCK_SUPER_ARMOR_CHESTPLATE = saiyArItem("bardock_super_armor_chestplate", ArmorItem.Type.CHESTPLATE, "bardockdbs_armor");
//	public static final RegistryObject<Item> BARDOCK_SUPER_ARMOR_LEGGINGS = saiyArItem("bardock_super_armor_leggings", ArmorItem.Type.LEGGINGS, "bardockdbs_armor");
//	public static final RegistryObject<Item> BARDOCK_SUPER_ARMOR_BOOTS = saiyArItem("bardock_super_armor_boots", ArmorItem.Type.BOOTS, "bardockdbs_armor");
//	//TURLES
//	public static final RegistryObject<Item> TURLES_ARMOR_CHESTPLATE = saiyArItem("turles_armor_chestplate", ArmorItem.Type.CHESTPLATE, "turles_armor");
//	public static final RegistryObject<Item> TURLES_ARMOR_LEGGINGS = saiyArItem("turles_armor_leggings", ArmorItem.Type.LEGGINGS, "turles_armor");
//	public static final RegistryObject<Item> TURLES_ARMOR_BOOTS = saiyArItem("turles_armor_boots", ArmorItem.Type.BOOTS, "turles_armor");
//	//TIEN
//	public static final RegistryObject<Item> TIEN_ARMOR_CHESTPLATE = armorItem("tien_armor_chestplate", ArmorItem.Type.CHESTPLATE, "tenshinhan_armor");
//	public static final RegistryObject<Item> TIEN_ARMOR_LEGGINGS = armorItem("tien_armor_leggings", ArmorItem.Type.LEGGINGS, "tenshinhan_armor");
//	public static final RegistryObject<Item> TIEN_ARMOR_BOOTS = armorItem("tien_armor_boots", ArmorItem.Type.BOOTS, "tenshinhan_armor");
//	//TRUNKS Z
//	public static final RegistryObject<Item> TRUNKS_Z_ARMOR_CHESTPLATE = armorItem("trunks_z_armor_chestplate", ArmorItem.Type.CHESTPLATE, "trunks_armor");
//	public static final RegistryObject<Item> TRUNKS_Z_ARMOR_LEGGINGS = armorItem("trunks_z_armor_leggings", ArmorItem.Type.LEGGINGS, "trunks_armor");
//	public static final RegistryObject<Item> TRUNKS_Z_ARMOR_BOOTS = armorItem("trunks_z_armor_boots", ArmorItem.Type.BOOTS, "trunks_armor");
//	//TRUNKS SUPER
//	public static final RegistryObject<Item> TRUNKS_SUPER_ARMOR_CHESTPLATE = armorItem("trunks_super_armor_chestplate", ArmorItem.Type.CHESTPLATE, "trunks_dbs");
//	public static final RegistryObject<Item> TRUNKS_SUPER_ARMOR_LEGGINGS = armorItem("trunks_super_armor_leggings", ArmorItem.Type.LEGGINGS, "trunks_dbs");
//	public static final RegistryObject<Item> TRUNKS_SUPER_ARMOR_BOOTS = armorItem("trunks_super_armor_boots", ArmorItem.Type.BOOTS, "trunks_dbs");
//	//TRUNKS KID DBZ
//	public static final RegistryObject<Item> TRUNKS_KID_ARMOR_CHESTPLATE = armorItem("trunks_kid_armor_chestplate", ArmorItem.Type.CHESTPLATE, "trunks_gi");
//	public static final RegistryObject<Item> TRUNKS_KID_ARMOR_LEGGINGS = armorItem("trunks_kid_armor_leggings", ArmorItem.Type.LEGGINGS, "trunks_gi");
//	public static final RegistryObject<Item> TRUNKS_KID_ARMOR_BOOTS = armorItem("trunks_kid_armor_boots", ArmorItem.Type.BOOTS, "trunks_gi");
//	// BROLY Z
//	public static final RegistryObject<Item> BROLY_Z_ARMOR_CHESTPLATE = armorItem("broly_z_armor_chestplate", ArmorItem.Type.CHESTPLATE, "broly_dbz");
//	public static final RegistryObject<Item> BROLY_Z_ARMOR_LEGGINGS = armorItem("broly_z_armor_leggings", ArmorItem.Type.LEGGINGS, "broly_dbz");
//	public static final RegistryObject<Item> BROLY_Z_ARMOR_BOOTS = armorItem("broly_z_armor_boots", ArmorItem.Type.BOOTS, "broly_dbz");
//	// BROLY SUPER
//	public static final RegistryObject<Item> BROLY_SUPER_ARMOR_CHESTPLATE = saiyArItem("broly_super_armor_chestplate", ArmorItem.Type.CHESTPLATE, "broly_dbs");
//	public static final RegistryObject<Item> BROLY_SUPER_ARMOR_LEGGINGS = saiyArItem("broly_super_armor_leggings", ArmorItem.Type.LEGGINGS, "broly_dbs");
//	public static final RegistryObject<Item> BROLY_SUPER_ARMOR_BOOTS = saiyArItem("broly_super_armor_boots", ArmorItem.Type.BOOTS, "broly_dbs");
//	// SHIN
//	public static final RegistryObject<Item> SHIN_ARMOR_CHESTPLATE = armorItem("shin_armor_chestplate", ArmorItem.Type.CHESTPLATE, "kaioshin");
//	public static final RegistryObject<Item> SHIN_ARMOR_LEGGINGS = armorItem("shin_armor_leggings", ArmorItem.Type.LEGGINGS, "kaioshin");
//	public static final RegistryObject<Item> SHIN_ARMOR_BOOTS = armorItem("shin_armor_boots", ArmorItem.Type.BOOTS, "kaioshin");
//	// BLACK GOKU
	public static final RegistryObject<Item> BLACK_ARMOR_CHESTPLATE = armorItem("black_armor_chestplate", ArmorItem.Type.CHESTPLATE, "blackgoku");
	public static final RegistryObject<Item> BLACK_ARMOR_LEGGINGS = armorItem("black_armor_leggings", ArmorItem.Type.LEGGINGS, "blackgoku");
	public static final RegistryObject<Item> BLACK_ARMOR_BOOTS = armorItem("black_armor_boots", ArmorItem.Type.BOOTS, "blackgoku");
//	// ZAMASU
//	public static final RegistryObject<Item> ZAMASU_ARMOR_CHESTPLATE = armorItem("zamasu_armor_chestplate", ArmorItem.Type.CHESTPLATE, "zamasu_gi");
//	public static final RegistryObject<Item> ZAMASU_ARMOR_LEGGINGS = armorItem("zamasu_armor_leggings", ArmorItem.Type.LEGGINGS, "zamasu_gi");
//	public static final RegistryObject<Item> ZAMASU_ARMOR_BOOTS = armorItem("zamasu_armor_boots", ArmorItem.Type.BOOTS, "zamasu_gi");
//	// FUSION ZAMASU
//	public static final RegistryObject<Item> FUSION_ZAMASU_ARMOR_CHESTPLATE = armorItem("fusion_zamasu_armor_chestplate", ArmorItem.Type.CHESTPLATE, "fzamasu_gi");
//	public static final RegistryObject<Item> FUSION_ZAMASU_ARMOR_LEGGINGS = armorItem("fusion_zamasu_armor_leggings", ArmorItem.Type.LEGGINGS, "fzamasu_gi");
//	public static final RegistryObject<Item> FUSION_ZAMASU_ARMOR_BOOTS = armorItem("fusion_zamasu_armor_boots", ArmorItem.Type.BOOTS, "fzamasu_gi");
//	// TROPAS DEL ORGULLO
//	public static final RegistryObject<Item> PRIDE_TROOPS_ARMOR_CHESTPLATE = armorItem("pride_troops_armor_chestplate", ArmorItem.Type.CHESTPLATE, "pride_troper");
//	public static final RegistryObject<Item> PRIDE_TROOPS_ARMOR_LEGGINGS = armorItem("pride_troops_armor_leggings", ArmorItem.Type.LEGGINGS, "pride_troper");
//	public static final RegistryObject<Item> PRIDE_TROOPS_ARMOR_BOOTS = armorItem("pride_troops_armor_boots", ArmorItem.Type.BOOTS, "pride_troper");
//	// HIT
//	public static final RegistryObject<Item> HIT_ARMOR_CHESTPLATE = armorItem("hit_armor_chestplate", ArmorItem.Type.CHESTPLATE, "hit");
//	public static final RegistryObject<Item> HIT_ARMOR_LEGGINGS = armorItem("hit_armor_leggings", ArmorItem.Type.LEGGINGS, "hit");
//	public static final RegistryObject<Item> HIT_ARMOR_BOOTS = armorItem("hit_armor_boots", ArmorItem.Type.BOOTS, "hit");
//	// GAS DBS
//	public static final RegistryObject<Item> GAS_ARMOR_CHESTPLATE = armorItem("gas_armor_chestplate", ArmorItem.Type.CHESTPLATE, "gas");
//	public static final RegistryObject<Item> GAS_ARMOR_LEGGINGS = armorItem("gas_armor_leggings", ArmorItem.Type.LEGGINGS, "gas");
//	public static final RegistryObject<Item> GAS_ARMOR_BOOTS = armorItem("gas_armor_boots", ArmorItem.Type.BOOTS, "gas");
//	// MAJIN BUU
//	public static final RegistryObject<Item> MAJIN_BUU_ARMOR_CHESTPLATE = capeArItem("majin_buu_armor_chestplate", ArmorItem.Type.CHESTPLATE, "majinbuu_gi");
//	public static final RegistryObject<Item> MAJIN_BUU_ARMOR_LEGGINGS = capeArItem("majin_buu_armor_leggings", ArmorItem.Type.LEGGINGS, "majinbuu_gi");
//	public static final RegistryObject<Item> MAJIN_BUU_ARMOR_BOOTS = capeArItem("majin_buu_armor_boots", ArmorItem.Type.BOOTS, "majinbuu_gi");
//	// GAMMA 1
//	public static final RegistryObject<Item> GAMMA1_ARMOR_CHESTPLATE = capeArItem("gamma1_armor_chestplate", ArmorItem.Type.CHESTPLATE, "gamma1");
//	public static final RegistryObject<Item> GAMMA1_ARMOR_LEGGINGS = capeArItem("gamma1_armor_leggings", ArmorItem.Type.LEGGINGS, "gamma1");
//	public static final RegistryObject<Item> GAMMA1_ARMOR_BOOTS = capeArItem("gamma1_armor_boots", ArmorItem.Type.BOOTS, "gamma1");
//	// GAMMA 2
//	public static final RegistryObject<Item> GAMMA2_ARMOR_CHESTPLATE = capeArItem("gamma2_armor_chestplate", ArmorItem.Type.CHESTPLATE, "gamma2");
//	public static final RegistryObject<Item> GAMMA2_ARMOR_LEGGINGS = capeArItem("gamma2_armor_leggings", ArmorItem.Type.LEGGINGS, "gamma2");
//	public static final RegistryObject<Item> GAMMA2_ARMOR_BOOTS = capeArItem("gamma2_armor_boots", ArmorItem.Type.BOOTS, "gamma2");
//	//INVENCIBLE
//	public static final RegistryObject<Item> INVENCIBLE_ARMOR_HELMET = armorItem("invencible_armor_helmet", ArmorItem.Type.HELMET, "invencible");
//	public static final RegistryObject<Item> INVENCIBLE_ARMOR_CHESTPLATE = armorItem("invencible_armor_chestplate", ArmorItem.Type.CHESTPLATE, "invencible");
//	public static final RegistryObject<Item> INVENCIBLE_ARMOR_LEGGINGS = armorItem("invencible_armor_leggings", ArmorItem.Type.LEGGINGS, "invencible");
//	public static final RegistryObject<Item> INVENCIBLE_ARMOR_BOOTS = armorItem("invencible_armor_boots", ArmorItem.Type.BOOTS, "invencible");

	//LÍQUIDOS
	public static final RegistryObject<Item> HEALING_BUCKET = ITEM_REGISTER.register("healing_liquid_bucket",
			() -> new BucketItem(MainFluids.SOURCE_HEALING, properties.stacksTo(1)));

	public static final RegistryObject<Item> NAMEK_WATER_BUCKET = ITEM_REGISTER.register("namek_water_bucket",
			() -> new BucketItem(MainFluids.SOURCE_NAMEK, properties.stacksTo(1)));

	//MINERALES
	public static final RegistryObject<Item> GETE_SCRAP = regItem("gete_scrap");
	public static final RegistryObject<Item> GETE_INGOT = regItem("gete_ingot");
	public static final RegistryObject<Item> KIKONO_SHARD = regItem("kikono_shard");

	//DRAGON BALL RADAR
	public static final RegistryObject<Item> DBALL_RADAR_ITEM = ITEM_REGISTER.register("dball_radar", RadarItem::new);
	public static final RegistryObject<Item> NAMEKDBALL_RADAR_ITEM = ITEM_REGISTER.register("namekdball_radar", NamekRadarItem::new);
	public static final RegistryObject<Item> RADAR_PIECE = ITEM_REGISTER.register("radar_piece",
			() -> new Item(properties.stacksTo(16)));
	public static final RegistryObject<Item> T1_RADAR_CHIP = ITEM_REGISTER.register("t1_radar_chip",
			() -> new Item(properties.stacksTo(16)));
	public static final RegistryObject<Item> T1_RADAR_CPU = ITEM_REGISTER.register("t1_radar_cpu",
			() -> new Item(properties.stacksTo(16)));
	public static final RegistryObject<Item> T2_RADAR_CHIP = ITEM_REGISTER.register("t2_radar_chip",
			() -> new Item(properties.stacksTo(16)));
	public static final RegistryObject<Item> T2_RADAR_CPU = ITEM_REGISTER.register("t2_radar_cpu",
			() -> new Item(properties.stacksTo(16)));

	//ENTIDADES (VEHÍCULOS)
	public static final RegistryObject<Item> NUBE_ITEM = ITEM_REGISTER.register("flying_nimbus", FlyingNimbusItem::new);
	public static final RegistryObject<Item> NUBE_NEGRA_ITEM = ITEM_REGISTER.register("black_nimbus", BlackNimbusItem::new);
	public static final RegistryObject<Item> NAVE_SAIYAN_ITEM = ITEM_REGISTER.register("saiyan_ship", SaiyanShipItem::new);

	//ARMOR STATION/ARMOR CRAFTING PATTERNS
	public static final RegistryObject<Item> ARMOR_CRAFTING_KIT = ITEM_REGISTER.register("armor_crafting_kit",
			() -> new ArmorCraftingKitItem(properties.stacksTo(1)));
	public static final RegistryObject<Item> KIKONO_STRING = regItem("kikono_string");
	public static final RegistryObject<Item> KIKONO_CLOTH = regItem("kikono_cloth");
	public static final RegistryObject<Item> BLANK_PATTERN_Z = regItem("blank_pattern_z");
	public static final RegistryObject<Item> BLANK_PATTERN_SUPER = regItem("blank_pattern_super");
	public static final RegistryObject<Item> PATTERN_GOKU_KID = regItem("pattern_goku_kid");
	public static final RegistryObject<Item> PATTERN_GOKU1 = regItem("pattern_goku1");
	public static final RegistryObject<Item> PATTERN_GOKU2 = regItem("pattern_goku2");
	public static final RegistryObject<Item> PATTERN_GOKU_SUPER = regItem("pattern_goku_super");
	public static final RegistryObject<Item> PATTERN_GOKU_GT = regItem("pattern_goku_gt");
	public static final RegistryObject<Item> PATTERN_YARDRAT = regItem("pattern_yardrat");
	public static final RegistryObject<Item> PATTERN_GOTEN = regItem("pattern_goten");
	public static final RegistryObject<Item> PATTERN_GOTEN_SUPER = regItem("pattern_goten_super");
	public static final RegistryObject<Item> PATTERN_GOHAN_SUPER = regItem("pattern_gohan_super");
	public static final RegistryObject<Item> PATTERN_GREAT_SAIYAMAN = regItem("pattern_great_saiyaman");
	public static final RegistryObject<Item> PATTERN_FUTURE_GOHAN = regItem("pattern_future_gohan");
	public static final RegistryObject<Item> PATTERN_VEGETA1 = regItem("pattern_vegeta1");
	public static final RegistryObject<Item> PATTERN_VEGETA2 = regItem("pattern_vegeta2");
	public static final RegistryObject<Item> PATTERN_VEGETA_Z = regItem("pattern_vegeta_z");
	public static final RegistryObject<Item> PATTERN_VEGETA_BUU = regItem("pattern_vegeta_buu");
	public static final RegistryObject<Item> PATTERN_VEGETA_SUPER = regItem("pattern_vegeta_super");
	public static final RegistryObject<Item> PATTERN_VEGETTO = regItem("pattern_vegetto");
	public static final RegistryObject<Item> PATTERN_GOGETA = regItem("pattern_gogeta");
	public static final RegistryObject<Item> PATTERN_PICCOLO = regItem("pattern_piccolo");
	public static final RegistryObject<Item> PATTERN_GOHAN1 = regItem("pattern_gohan1");
	public static final RegistryObject<Item> PATTERN_BARDOCK1 = regItem("pattern_bardock1");
	public static final RegistryObject<Item> PATTERN_BARDOCK2 = regItem("pattern_bardock2");
	public static final RegistryObject<Item> PATTERN_TURLES = regItem("pattern_turles");
	public static final RegistryObject<Item> PATTERN_TIEN = regItem("pattern_tien");
	public static final RegistryObject<Item> PATTERN_TRUNKS_Z = regItem("pattern_trunks_z");
	public static final RegistryObject<Item> PATTERN_TRUNKS_SUPER = regItem("pattern_trunks_super");
	public static final RegistryObject<Item> PATTERN_TRUNKS_KID = regItem("pattern_trunks_kid");
	public static final RegistryObject<Item> PATTERN_BROLY_Z = regItem("pattern_broly_z");
	public static final RegistryObject<Item> PATTERN_BROLY_SUPER = regItem("pattern_broly_super");
	public static final RegistryObject<Item> PATTERN_SHIN = regItem("pattern_shin");
	public static final RegistryObject<Item> PATTERN_BLACK = regItem("pattern_black");
	public static final RegistryObject<Item> PATTERN_ZAMASU = regItem("pattern_zamasu");
	public static final RegistryObject<Item> PATTERN_FUSION_ZAMASU = regItem("pattern_fusionzamasu");
	public static final RegistryObject<Item> PATTERN_PRIDE_TROOPS = regItem("pattern_pride_troops");
	public static final RegistryObject<Item> PATTERN_HIT = regItem("pattern_hit");
	public static final RegistryObject<Item> PATTERN_GAS = regItem("pattern_gas");
	public static final RegistryObject<Item> PATTERN_MAJIN_BUU = regItem("pattern_majin_buu");
	public static final RegistryObject<Item> PATTERN_GAMMA1 = regItem("pattern_gamma1");
	public static final RegistryObject<Item> PATTERN_GAMMA2 = regItem("pattern_gamma2");

	//DRAGON BALLS
	public static final RegistryObject<Item> DBALL1_BLOCK_ITEM = ITEM_REGISTER.register("dball1",
			() -> new BlockItem(MainBlocks.DBALL1_BLOCK.get(), properties
					.stacksTo(1)
					.fireResistant()
			));
	public static final RegistryObject<Item> DBALL2_BLOCK_ITEM = ITEM_REGISTER.register("dball2",
			() -> new BlockItem(MainBlocks.DBALL2_BLOCK.get(), properties.stacksTo(1).fireResistant()));
	public static final RegistryObject<Item> DBALL3_BLOCK_ITEM = ITEM_REGISTER.register("dball3",
			() -> new BlockItem(MainBlocks.DBALL3_BLOCK.get(), properties.stacksTo(1).fireResistant()));
	public static final RegistryObject<Item> DBALL4_BLOCK_ITEM = ITEM_REGISTER.register("dball4",
			() -> new BlockItem(MainBlocks.DBALL4_BLOCK.get(), properties.stacksTo(1).fireResistant()));
	public static final RegistryObject<Item> DBALL5_BLOCK_ITEM = ITEM_REGISTER.register("dball5",
			() -> new BlockItem(MainBlocks.DBALL5_BLOCK.get(), properties.stacksTo(1).fireResistant()));
	public static final RegistryObject<Item> DBALL6_BLOCK_ITEM = ITEM_REGISTER.register("dball6",
			() -> new BlockItem(MainBlocks.DBALL6_BLOCK.get(), properties.stacksTo(1).fireResistant()));
	public static final RegistryObject<Item> DBALL7_BLOCK_ITEM = ITEM_REGISTER.register("dball7",
			() -> new BlockItem(MainBlocks.DBALL7_BLOCK.get(), properties.stacksTo(1).fireResistant()));
	public static final RegistryObject<Item> DBALL1_NAMEK_BLOCK_ITEM = ITEM_REGISTER.register("dball1_namek",
			() -> new BlockItem(MainBlocks.DBALL1_NAMEK_BLOCK.get(), properties.stacksTo(1).fireResistant()));
	public static final RegistryObject<Item> DBALL2_NAMEK_BLOCK_ITEM = ITEM_REGISTER.register("dball2_namek",
			() -> new BlockItem(MainBlocks.DBALL2_NAMEK_BLOCK.get(), properties.stacksTo(1).fireResistant()));
	public static final RegistryObject<Item> DBALL3_NAMEK_BLOCK_ITEM = ITEM_REGISTER.register("dball3_namek",
			() -> new BlockItem(MainBlocks.DBALL3_NAMEK_BLOCK.get(), properties.stacksTo(1).fireResistant()));
	public static final RegistryObject<Item> DBALL4_NAMEK_BLOCK_ITEM = ITEM_REGISTER.register("dball4_namek",
			() -> new BlockItem(MainBlocks.DBALL4_NAMEK_BLOCK.get(), properties.stacksTo(1).fireResistant()));
	public static final RegistryObject<Item> DBALL5_NAMEK_BLOCK_ITEM = ITEM_REGISTER.register("dball5_namek",
			() -> new BlockItem(MainBlocks.DBALL5_NAMEK_BLOCK.get(), properties.stacksTo(1).fireResistant()));
	public static final RegistryObject<Item> DBALL6_NAMEK_BLOCK_ITEM = ITEM_REGISTER.register("dball6_namek",
			() -> new BlockItem(MainBlocks.DBALL6_NAMEK_BLOCK.get(), properties.stacksTo(1).fireResistant()));
	public static final RegistryObject<Item> DBALL7_NAMEK_BLOCK_ITEM = ITEM_REGISTER.register("dball7_namek",
			() -> new BlockItem(MainBlocks.DBALL7_NAMEK_BLOCK.get(), properties.stacksTo(1).fireResistant()));

	// SPAWN EGGS
	//public static final RegistryObject<Item> DINO_SE = ITEM_REGISTER.register("dino_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.DINO1, 0xED5B18, 0x6ED610, new Item.Properties()));
	//public static final RegistryObject<Item> NAMEK_FROG_SE = ITEM_REGISTER.register("namek_frog_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.NAMEK_FROG, 0x22C96B, 0xD62B52, new Item.Properties()));
	//public static final RegistryObject<Item> GINYU_FROG_SE = ITEM_REGISTER.register("ginyu_frog_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.GINYU_FROG, 0x22C96B, 0x6D0480, new Item.Properties()));
	//public static final RegistryObject<Item> PINK_FROG_SE = ITEM_REGISTER.register("pink_frog_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.PINK_FROG, 0xF7B1C1, 0xD62B52, new Item.Properties()));
	//public static final RegistryObject<Item> YELLOW_FROG_SE = ITEM_REGISTER.register("yellow_frog_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.YELLOW_FROG, 0xF7F71C, 0xD62B52, new Item.Properties()));
	//public static final RegistryObject<Item> SOLDIER01_SE = ITEM_REGISTER.register("soldier01_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.FRIEZA_SOLDIER01, 0x010714, 0xE6E7EB, new Item.Properties()));
	//public static final RegistryObject<Item> SOLDIER02_SE = ITEM_REGISTER.register("soldier02_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.FRIEZA_SOLDIER02, 0X5D1066, 0xA18B33, new Item.Properties()));
	//public static final RegistryObject<Item> SOLDIER03_SE = ITEM_REGISTER.register("soldier03_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.FRIEZA_SOLDIER03, 0x95F0CB, 0xDABAE6, new Item.Properties()));
	//public static final RegistryObject<Item> MORO_SOLDIER_SE = ITEM_REGISTER.register("moro_soldier_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.MORO_SOLDIER, 0x051942, 0xE6E7EB, new Item.Properties()));
	//public static final RegistryObject<Item> NWARRIOR1_SE = ITEM_REGISTER.register("nwarrior1_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.NAMEKNPC_WARRIOR1, 0x246E18, 0x12848A, new Item.Properties()));
	//public static final RegistryObject<Item> NWARRIOR2_SE = ITEM_REGISTER.register("nwarrior2_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.NAMEKNPC_WARRIOR2, 0x246E18, 0x6ED610, new Item.Properties()));
	//public static final RegistryObject<Item> SAIBAMAN_SE = ITEM_REGISTER.register("saibaman_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.SAIBAMAN, 0x6ED610, 0x2A6E18, new Item.Properties()));
	//public static final RegistryObject<Item> KAIWAREMAN_SE = ITEM_REGISTER.register("kaiwareman_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.KAIWAREMAN, 0x54e8b2, 0x298ba3, new Item.Properties()));
	//public static final RegistryObject<Item> KYUKONMAN_SE = ITEM_REGISTER.register("kyukonman_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.KYUKONMAN, 0xe6d575, 0x6b5e12, new Item.Properties()));
	//public static final RegistryObject<Item> COPYMAN_SE = ITEM_REGISTER.register("copyman_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.COPYMAN, 0x47463d, 0x242321, new Item.Properties()));
	//public static final RegistryObject<Item> TENNENMAN_SE = ITEM_REGISTER.register("tennenman_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.TENNENMAN, 0xb971d1, 0x298ba3, new Item.Properties()));
	//public static final RegistryObject<Item> JINKOUMAN_SE = ITEM_REGISTER.register("jinkouman_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.JINKOUMAN, 0xb3acb5, 0x242321, new Item.Properties()));
	//public static final RegistryObject<Item> REDRIBBONSOLDIER_SE = ITEM_REGISTER.register("redribbon_soldier_spawn_egg", () ->
	//		new ForgeSpawnEggItem(MainEntity.REDRIBBON_SOLDIER, 0xe6975e, 0xe63c29, new Item.Properties()));

	public static RegistryObject<Item> regItem(String name) {
		return ITEM_REGISTER.register(name, () -> new Item(properties.stacksTo(64)));
	}

	//public static RegistryObject<Item> armorItem(String name, ArmorItem.Type armorType, String itemId, boolean isDamageOn) {
	//	return ITEM_REGISTER.register(name, () ->
	//			new DbzArmorItem(ModArmorMaterials.KIKONO, armorType,
	//					new Item.Properties().fireResistant().stacksTo(1), itemId, isDamageOn, false));
	//}
//
	//public static RegistryObject<Item> saiyArItem(String name, ArmorItem.Type armorType, String itemId, boolean isDamageOn) {
	//	return ITEM_REGISTER.register(name, () ->
	//			new SaiyanArmorItem(ModArmorMaterials.KIKONO, armorType,
	//					new Item.Properties().fireResistant().stacksTo(1), itemId, isDamageOn, false));
	//}
	//public static RegistryObject<Item> capeArItem(String name, ArmorItem.Type armorType, String itemId, boolean isDamageOn) {
	//	// Armaduras con capa
	//	return ITEM_REGISTER.register(name, () ->
	//			new DbzArmorItem(ModArmorMaterials.KIKONO, armorType,
	//					new Item.Properties().fireResistant().stacksTo(1), itemId, isDamageOn, true));
	//}
	//public static RegistryObject<Item> capeHombArItem(String name, ArmorItem.Type armorType, String itemId, boolean isDamageOn) {
	//	// Armaduras con capa y hombreras
	//	return ITEM_REGISTER.register(name, () ->
	//			new SaiyanArmorItem(ModArmorMaterials.KIKONO, armorType,
	//					new Item.Properties().fireResistant().stacksTo(1), itemId, isDamageOn, true));
	//}
//
	//// Mismos métodos, pero con isDamageOn en false por defecto, evitando así poner "false" en cada registro de items.
	public static RegistryObject<Item> armorItem(String name, ArmorItem.Type armorType, String itemId) {
		return ITEM_REGISTER.register(name, () ->
				new DbzArmorItem(ModArmorMaterials.KIKONO, armorType,
						new Item.Properties().fireResistant().stacksTo(1), itemId, false, false));
	}
//
	//public static RegistryObject<Item> saiyArItem(String name, ArmorItem.Type armorType, String itemId) {
	//	return ITEM_REGISTER.register(name, () ->
	//			new SaiyanArmorItem(ModArmorMaterials.KIKONO, armorType,
	//					new Item.Properties().fireResistant().stacksTo(1), itemId, false, false));
	//}
	//public static RegistryObject<Item> capeArItem(String name, ArmorItem.Type armorType, String itemId) {
	//	// Armaduras con capa
	//	return ITEM_REGISTER.register(name, () ->
	//			new DbzArmorItem(ModArmorMaterials.KIKONO, armorType,
	//					new Item.Properties().fireResistant().stacksTo(1), itemId, false, true));
	//}
	//public static RegistryObject<Item> capeHombArItem(String name, ArmorItem.Type armorType, String itemId) {
	//	// Armaduras con capa y hombreras
	//	return ITEM_REGISTER.register(name, () ->
	//			new SaiyanArmorItem(ModArmorMaterials.KIKONO, armorType,
	//					new Item.Properties().fireResistant().stacksTo(1), itemId, false, true));
	//}
	public static void register(IEventBus bus) {
		ITEM_REGISTER.register(bus);
	}
}
