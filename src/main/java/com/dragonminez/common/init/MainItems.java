package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.init.armor.ModArmorMaterials;
import com.dragonminez.common.init.item.*;
import com.dragonminez.common.init.item.weapons.BraveSwordItem;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

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
			() -> new FoodItem(20, 0.0f, 8));

	public static final RegistryObject<Item> MIGHT_TREE_FRUIT = ITEM_REGISTER.register("might_tree_fruit",
			MightTreeFruitItem::new);
	public static final RegistryObject<Item> COMIDA_DINO_RAW = ITEM_REGISTER.register("raw_dino_meat",
			() -> new FoodItem(4, 3.6f, 32));
	public static final RegistryObject<Item> COMIDA_DINO_COOKED = ITEM_REGISTER.register("cooked_dino_meat",
			() -> new FoodItem(8, 12.8f, 32));
	public static final RegistryObject<Item> HEART_MEDICINE = ITEM_REGISTER.register("heart_medicine",
			() -> new FoodItem(0, 0.0f, 8));
	public static final RegistryObject<Item> DINO_TAIL_RAW = ITEM_REGISTER.register("dino_tail_raw",
			() -> new FoodItem(6, 4.8f, 16));
	public static final RegistryObject<Item> DINO_TAIL_COOKED = ITEM_REGISTER.register("dino_tail_cooked",
			() -> new FoodItem(12, 9.6f, 16));
	public static final RegistryObject<Item> FROG_LEGS_RAW = ITEM_REGISTER.register("frog_legs_raw",
			() -> new FoodItem(2, 2.4f, 64));
	public static final RegistryObject<Item> FROG_LEGS_COOKED = ITEM_REGISTER.register("frog_legs_cooked",
			() -> new FoodItem(5, 4.8f, 64));

	//POTHALAS
	public static final RegistryObject<Item> POTHALA_LEFT =
			ITEM_REGISTER.register("pothala_left", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.HELMET, new Item.Properties().fireResistant().stacksTo(1), "pothala_left", false));
	public static final RegistryObject<Item> POTHALA_RIGHT =
			ITEM_REGISTER.register("pothala_right", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.HELMET, new Item.Properties().fireResistant().stacksTo(1), "pothala_right", false));
	public static final RegistryObject<Item> GREEN_POTHALA_LEFT =
			ITEM_REGISTER.register("green_pothala_left", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.HELMET, new Item.Properties().fireResistant().stacksTo(1), "green_pothala_left", false));
	public static final RegistryObject<Item> GREEN_POTHALA_RIGHT =
			ITEM_REGISTER.register("green_pothala_right", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.HELMET, new Item.Properties().fireResistant().stacksTo(1), "green_pothala_right", false));

    public static final RegistryObject<Item> RED_SCOUTER =
            ITEM_REGISTER.register("red_scouter", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1).fireResistant(), "red_scouter", false));
    public static final RegistryObject<Item> BLUE_SCOUTER =
            ITEM_REGISTER.register("blue_scouter", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1).fireResistant(), "blue_scouter", false));
    public static final RegistryObject<Item> GREEN_SCOUTER =
            ITEM_REGISTER.register("green_scouter", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1).fireResistant(), "green_scouter", false));
    public static final RegistryObject<Item> PURPLE_SCOUTER =
            ITEM_REGISTER.register("purple_scouter", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1).fireResistant(), "purple_scouter", false));
    public static final RegistryObject<Item> MERUS_LASER =
            ITEM_REGISTER.register("laser_merus", () -> new MerusLaserItem());
    public static final RegistryObject<Item> BLASTER_CANNON =
            ITEM_REGISTER.register("blaster_cannon", () -> new BlasterCannonItem());
    //ARMAS
	// 0 + X = Daño | 4 +/- X = Velocidad de ataque | 0 + X = Durabilidad (0 = Irrompible)
	public static final RegistryObject<SwordItem> KATANA_YAJIROBE =
			ITEM_REGISTER.register("yajirobe_katana", () -> new YajirobeKatanaItem());
    public static final RegistryObject<SwordItem> Z_SWORD =
            ITEM_REGISTER.register("z_sword", () -> new ZSwordItem());
    public static final RegistryObject<SwordItem> BRAVE_SWORD =
            ITEM_REGISTER.register("brave_sword", () -> new BraveSwordItem());
    public static final RegistryObject<SwordItem> POWER_POLE =
            ITEM_REGISTER.register("power_pole", () -> new PowerPoleItem());

	//ARMADURAS
    // GOKU NIÑO
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOKU_KID_ARMOR = fullArmorNoHelmetSet("goku_kid_armor", "goku_kid", false);
    //GOKU GI
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOKU_ARMOR = fullArmorNoHelmetSet("goku_armor", "goku_gi", false);
    //Goku Super
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOKU_SUPER_ARMOR = fullArmorNoHelmetSet("goku_super_armor", "goku_super", false);
    // GOKU GT
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOKU_GT_ARMOR = fullArmorNoHelmetSet("goku_gt_armor", "goku_gt", false);
    // YARDRAT
	public static final Map<ArmorItem.Type, RegistryObject<Item>> YARDRAT_ARMOR = fullArmorNoHelmetSet("yardrat_armor", "yardrat_gi", false);
    // GOTEN Z
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOTEN_ARMOR = fullArmorNoHelmetSet("goten_armor", "goten_gi", false);
    // GOTEN TEEN (SUPER)
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOTEN_SUPER_ARMOR = fullArmorNoHelmetSet("goten_super_armor", "goten_dbs", false);
    // GOHAN GI (SUPER)
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOHAN_SUPER_ARMOR = fullArmorNoHelmetSet("gohan_super_armor", "gohan_dbs", false);
    // GOHAN GREAT SAIYAMAN
	public static final Map<ArmorItem.Type, RegistryObject<Item>>  GREAT_SAIYAMAN_ARMOR = fullArmorCapeSet("great_saiyaman_armor", "saiyaman_gi", false);
    // FUTURE GOHAN
	public static final Map<ArmorItem.Type, RegistryObject<Item>> FUTURE_GOHAN_ARMOR = fullArmorNoHelmetSet("future_gohan_armor", "future_gohan", false);
    // VEGETA SAGA SAIYAJIN (Cambiar luego a saiyanArmor para hombreras)
    public static final Map<ArmorItem.Type, RegistryObject<Item>> VEGETA_SAIYAN_ARMOR = fullArmorNoHelmetSet("vegeta_saiyan_armor", "vegeta_saiyan_armor", false);
    // VEGETA SAGA NAMEK
	public static final Map<ArmorItem.Type, RegistryObject<Item>> VEGETA_NAMEK_ARMOR = fullArmorNoHelmetSet("vegeta_namek_armor", "vegetanamek_armor", false);
    // VEGETA SAGA CELL
	public static final Map<ArmorItem.Type, RegistryObject<Item>> VEGETA_Z_ARMOR = fullArmorNoHelmetSet("vegeta_z_armor", "vegetaz_armor", false);
    // VEGETA SAGA BUU
	public static final Map<ArmorItem.Type, RegistryObject<Item>> VEGETA_BUU_ARMOR = fullArmorNoHelmetSet("vegeta_buu_armor", "vegetabuu", false);
    // VEGETA ARMADURA DE SUPER
	public static final Map<ArmorItem.Type, RegistryObject<Item>> VEGETA_SUPER_ARMOR = fullArmorNoHelmetSet("vegeta_super_armor", "vegetasuper", false);
    // VEGETTO
	public static final Map<ArmorItem.Type, RegistryObject<Item>> VEGETTO_ARMOR = fullArmorNoHelmetSet("vegetto_armor", "vegetto", false);
    // GOGETA
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOGETA_ARMOR = fullArmorNoHelmetSet("gogeta_armor", "gogeta", false);
    // PICCOLO
	public static final Map<ArmorItem.Type, RegistryObject<Item>> PICCOLO_ARMOR = fullArmorCapeSet("piccolo_armor", "piccolo_gi", false);
    // DEMON GI (AZUL)
	public static final Map<ArmorItem.Type, RegistryObject<Item>> DEMON_GI_BLUE_ARMOR = fullArmorNoHelmetSet("demon_gi_blue_armor", "demon_gi_gohan", false);
    // BARDOCK DBZ
	public static final Map<ArmorItem.Type, RegistryObject<Item>> BARDOCK_DBZ_ARMOR = fullArmorNoHelmetSet("bardock_dbz_armor", "bardock_armor", false);
    // BARDOCK SUPER
	public static final Map<ArmorItem.Type, RegistryObject<Item>> BARDOCK_SUPER_ARMOR = fullArmorNoHelmetSet("bardock_super_armor", "bardockdbs_armor", false);
    // TURLES
	public static final Map<ArmorItem.Type, RegistryObject<Item>> TURLES_ARMOR = fullArmorNoHelmetSet("turles_armor", "turles_armor", false);
    //TIEN
	public static final Map<ArmorItem.Type, RegistryObject<Item>> TIEN_ARMOR = fullArmorNoHelmetSet("tien_armor", "tenshinhan_armor", false);
    //TRUNKS Z
	public static final Map<ArmorItem.Type, RegistryObject<Item>> TRUNKS_Z_ARMOR = fullArmorNoHelmetSet("trunks_z_armor", "trunks_armor", false);
    //TRUNKS SUPER
	public static final Map<ArmorItem.Type, RegistryObject<Item>> TRUNKS_SUPER_ARMOR = fullArmorNoHelmetSet("trunks_super_armor", "trunks_dbs", false);
    //TRUNKS KID DBZ
	public static final Map<ArmorItem.Type, RegistryObject<Item>> TRUNKS_KID_ARMOR = fullArmorNoHelmetSet("trunks_kid_armor", "trunks_gi", false);
    // BROLY Z
	public static final Map<ArmorItem.Type, RegistryObject<Item>> BROLY_Z_ARMOR = fullArmorNoHelmetSet("broly_z_armor", "broly_dbz", false);
    // BROLY SUPER
	public static final Map<ArmorItem.Type, RegistryObject<Item>> BROLY_SUPER_ARMOR = fullArmorNoHelmetSet("broly_super_armor", "broly_dbs", false);
    // SHIN
	public static final Map<ArmorItem.Type, RegistryObject<Item>> SHIN_ARMOR = fullArmorNoHelmetSet("shin_armor", "kaioshin", false);
    // BLACK GOKU
	public static final Map<ArmorItem.Type, RegistryObject<Item>> BLACKGOKU_ARMOR = fullArmorNoHelmetSet("blackgoku_armor", "blackgoku", false);
    // ZAMASU
	public static final Map<ArmorItem.Type, RegistryObject<Item>> ZAMASU_ARMOR = fullArmorNoHelmetSet("zamasu_armor", "zamasu_gi", false);
    // FUSED ZAMASU
	public static final Map<ArmorItem.Type, RegistryObject<Item>> FUSION_ZAMASU_ARMOR = fullArmorNoHelmetSet("fusion_zamasu_armor", "fzamasu_gi", false);
    // TROPAS DEL ORGULLO
	public static final Map<ArmorItem.Type, RegistryObject<Item>> PRIDE_TROOPS_ARMOR = fullArmorNoHelmetSet("pride_troops_armor", "pride_troper", false);
    // HIT
	public static final Map<ArmorItem.Type, RegistryObject<Item>> HIT_ARMOR = fullArmorNoHelmetSet("hit_armor", "hit", false);
    // GAS DBS
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GAS_ARMOR = fullArmorNoHelmetSet("gas_armor", "gas", false);
    // MAJIN BUU
    public static final Map<ArmorItem.Type, RegistryObject<Item>>  MAJIN_BUU_ARMOR = fullArmorCapeNoHelmetSet("majin_buu_armor", "majinbuu_gi", false);
    // GAMMA 1
    public static final Map<ArmorItem.Type, RegistryObject<Item>>  GAMMA1_ARMOR = fullArmorCapeNoHelmetSet("gamma1_armor", "gamma1", false);
    // GAMMA 2
    public static final Map<ArmorItem.Type, RegistryObject<Item>>  GAMMA2_ARMOR = fullArmorCapeNoHelmetSet("gamma2_armor", "gamma2", false);
    // NARUKE ARMOR
    public static final Map<ArmorItem.Type, RegistryObject<Item>> NARUKE_ARMOR = fullArmorNoHelmetSet("naruke_armor", "naruke", false);
    // THE STRONGEST
    public static final Map<ArmorItem.Type, RegistryObject<Item>> STRONGEST_ARMOR = fullArmorNoHelmetSet("strongest_armor", "strongest", false);
    // A17
    public static final Map<ArmorItem.Type, RegistryObject<Item>> A17_ARMOR = fullArmorNoHelmetSet("a17_armor", "a17", false);
    // A18
    public static final Map<ArmorItem.Type, RegistryObject<Item>> A18_ARMOR = fullArmorNoHelmetSet("a18_armor", "a18", false);
    // A16
    public static final Map<ArmorItem.Type, RegistryObject<Item>> A16_ARMOR = fullArmorNoHelmetSet("a16_armor", "a16", false);
    // ORANGE STAR HIGH SCHOOL UNIFORM
    public static final Map<ArmorItem.Type, RegistryObject<Item>> ORANGE_HIGH_ARMOR = fullArmorNoHelmetSet("orange_high_armor", "orange_high", false);
    // GRANOLA
    public static final Map<ArmorItem.Type, RegistryObject<Item>> GRANOLA_ARMOR = fullArmorNoHelmetSet("granola_armor", "granola", false);


    //INVENCIBLE
	public static final Map<ArmorItem.Type, RegistryObject<Item>> INVENCIBLE_ARMOR = fullArmorSet("invencible_armor", "invencible", false);
    public static final Map<ArmorItem.Type, RegistryObject<Item>> INVENCIBLE_BLUE_ARMOR = fullArmorSet("invencible_blue_armor", "invencible_blue", false);

	//LÍQUIDOS
	public static final RegistryObject<Item> HEALING_BUCKET = ITEM_REGISTER.register("healing_liquid_bucket",
			() -> new BucketItem(MainFluids.SOURCE_HEALING, properties.stacksTo(1)));

	public static final RegistryObject<Item> NAMEK_WATER_BUCKET = ITEM_REGISTER.register("namek_water_bucket",
			() -> new BucketItem(MainFluids.SOURCE_NAMEK, properties.stacksTo(1)));

	//MINERALES
	public static final RegistryObject<Item> GETE_SCRAP = regItem("gete_scrap");
	public static final RegistryObject<Item> GETE_INGOT = regItem("gete_ingot");
	public static final RegistryObject<Item> KIKONO_SHARD = regItem("kikono_shard");
	public static final RegistryObject<Item> KIKONO_STICK = regItem("kikono_stick");

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
    public static final RegistryObject<Item> PUNCH_MACHINE_ITEM = ITEM_REGISTER.register("punch_machine_item", PunchMachineItem::new);

	//KIKONO STATION/ARMOR CRAFTING PATTERNS
	public static final RegistryObject<Item> ARMOR_CRAFTING_KIT = ITEM_REGISTER.register("armor_crafting_kit",
			() -> new ArmorCraftingKitItem(properties.stacksTo(1)));
	public static final RegistryObject<Item> KIKONO_STRING = regItem("kikono_string");
	public static final RegistryObject<Item> KIKONO_CLOTH = regItem("kikono_cloth");
	public static final RegistryObject<Item> BLANK_PATTERN_Z = regItem("blank_pattern_z");
	public static final RegistryObject<Item> BLANK_PATTERN_SUPER = regItem("blank_pattern_super");
	public static final RegistryObject<Item> PATTERN_GOKU_KID = regItem("pattern_goku_kid");
	public static final RegistryObject<Item> PATTERN_GOKU1 = regItem("pattern_goku1");
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
	public static final RegistryObject<Item> DINO_1 = ITEM_REGISTER.register("dino1_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.DINOSAUR1, 0xED5B18, 0x6ED610, new Item.Properties()));
	public static final RegistryObject<Item> DINO_2 = ITEM_REGISTER.register("dino2_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.DINOSAUR2, 0xED5B18, 0x6ED610, new Item.Properties()));
	public static final RegistryObject<Item> DINO_3 = ITEM_REGISTER.register("dino3_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.DINOSAUR3, 0xED5B18, 0x6ED610, new Item.Properties()));
	public static final RegistryObject<Item> DINO_KID = ITEM_REGISTER.register("dinokid_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.DINO_KID, 0xED5B18, 0x6ED610, new Item.Properties()));
	public static final RegistryObject<Item> NAMEK_FROG_SE = ITEM_REGISTER.register("namek_frog_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.NAMEK_FROG, 0x22C96B, 0xD62B52, new Item.Properties()));
	public static final RegistryObject<Item> GINYU_FROG_SE = ITEM_REGISTER.register("ginyu_frog_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.NAMEK_FROG_GINYU, 0x22C96B, 0x6D0480, new Item.Properties()));
	public static final RegistryObject<Item> SOLDIER01_SE = ITEM_REGISTER.register("soldier01_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.SAGA_FRIEZA_SOLDIER, 0x010714, 0xE6E7EB, new Item.Properties()));
	public static final RegistryObject<Item> SOLDIER02_SE = ITEM_REGISTER.register("soldier02_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.SAGA_FRIEZA_SOLDIER2, 0X5D1066, 0xA18B33, new Item.Properties()));
	public static final RegistryObject<Item> SOLDIER03_SE = ITEM_REGISTER.register("soldier03_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.SAGA_FRIEZA_SOLDIER3, 0x95F0CB, 0xDABAE6, new Item.Properties()));
	public static final RegistryObject<Item> NWARRIOR_SE = ITEM_REGISTER.register("nwarrior_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.NAMEK_WARRIOR, 0x246E18, 0x12848A, new Item.Properties()));
	public static final RegistryObject<Item> SAIBAMAN_SE = ITEM_REGISTER.register("saibaman_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.SAGA_SAIBAMAN, 0x6ED610, 0x2A6E18, new Item.Properties()));
	public static final RegistryObject<Item> KAIWAREMAN_SE = ITEM_REGISTER.register("kaiwareman_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.SAGA_SAIBAMAN2, 0x54e8b2, 0x298ba3, new Item.Properties()));
	public static final RegistryObject<Item> KYUKONMAN_SE = ITEM_REGISTER.register("kyukonman_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.SAGA_SAIBAMAN3, 0xe6d575, 0x6b5e12, new Item.Properties()));
	public static final RegistryObject<Item> COPYMAN_SE = ITEM_REGISTER.register("copyman_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.SAGA_SAIBAMAN4, 0x47463d, 0x242321, new Item.Properties()));
	public static final RegistryObject<Item> TENNENMAN_SE = ITEM_REGISTER.register("tennenman_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.SAGA_SAIBAMAN5, 0xb971d1, 0x298ba3, new Item.Properties()));
	public static final RegistryObject<Item> JINKOUMAN_SE = ITEM_REGISTER.register("jinkouman_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.SAGA_SAIBAMAN6, 0xb3acb5, 0x242321, new Item.Properties()));
	public static final RegistryObject<Item> REDRIBBONSOLDIER_SE = ITEM_REGISTER.register("redribbon_soldier_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.RED_RIBBON_SOLDIER, 0xe6975e, 0xe63c29, new Item.Properties()));
	public static final RegistryObject<Item> REDRIBBONROBOT1_SE = ITEM_REGISTER.register("redribbon_robot1_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.RED_RIBBON_ROBOT1, 0xe6975e, 0xe63c29, new Item.Properties()));
	public static final RegistryObject<Item> REDRIBBONROBOT2_SE = ITEM_REGISTER.register("redribbon_robot2_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.RED_RIBBON_ROBOT2, 0xe6975e, 0xe63c29, new Item.Properties()));
	public static final RegistryObject<Item> REDRIBBONROBOT3_SE = ITEM_REGISTER.register("redribbon_robot3_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.RED_RIBBON_ROBOT3, 0xe6975e, 0xe63c29, new Item.Properties()));
	public static final RegistryObject<Item> BANDIT_SE = ITEM_REGISTER.register("bandit_spawn_egg", () ->
			new ForgeSpawnEggItem(MainEntities.BANDIT, 0x8B4513, 0xFFFF00, new Item.Properties()));


	public static RegistryObject<Item> regItem(String name) {
		return ITEM_REGISTER.register(name, () -> new Item(properties.stacksTo(64)));
	}

	private static Map<ArmorItem.Type, RegistryObject<Item>> registerArmorSet(String name, String texture, boolean hasHelmet, boolean isDamageOn) {
		Map<ArmorItem.Type, RegistryObject<Item>> armorPieces = new HashMap<>();
		if (hasHelmet) {
			armorPieces.put(ArmorItem.Type.HELMET, ITEM_REGISTER.register(name + "_helmet", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.HELMET, new Item.Properties().fireResistant().stacksTo(1), texture, isDamageOn)));
		}
		armorPieces.put(ArmorItem.Type.CHESTPLATE, ITEM_REGISTER.register(name + "_chestplate", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant().stacksTo(1), texture, isDamageOn)));
		armorPieces.put(ArmorItem.Type.LEGGINGS, ITEM_REGISTER.register(name + "_leggings", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant().stacksTo(1), texture, isDamageOn)));
		armorPieces.put(ArmorItem.Type.BOOTS, ITEM_REGISTER.register(name + "_boots", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.BOOTS, new Item.Properties().fireResistant().stacksTo(1), texture, isDamageOn)));
		return armorPieces;
	}
    private static Map<ArmorItem.Type, RegistryObject<Item>> registerArmorSetCape(String name, String texture, boolean hasHelmet, boolean isDamageOn) {
        Map<ArmorItem.Type, RegistryObject<Item>> armorPieces = new HashMap<>();
        if (hasHelmet) {
            armorPieces.put(ArmorItem.Type.HELMET, ITEM_REGISTER.register(name + "_helmet", () -> new DbzArmorCapeItem(ModArmorMaterials.KIKONO, ArmorItem.Type.HELMET, new Item.Properties().fireResistant().stacksTo(1), texture, isDamageOn)));
        }
        armorPieces.put(ArmorItem.Type.CHESTPLATE, ITEM_REGISTER.register(name + "_chestplate", () -> new DbzArmorCapeItem(ModArmorMaterials.KIKONO, ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant().stacksTo(1), texture, isDamageOn)));
        armorPieces.put(ArmorItem.Type.LEGGINGS, ITEM_REGISTER.register(name + "_leggings", () -> new DbzArmorCapeItem(ModArmorMaterials.KIKONO, ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant().stacksTo(1), texture, isDamageOn)));
        armorPieces.put(ArmorItem.Type.BOOTS, ITEM_REGISTER.register(name + "_boots", () -> new DbzArmorCapeItem(ModArmorMaterials.KIKONO, ArmorItem.Type.BOOTS, new Item.Properties().fireResistant().stacksTo(1), texture, isDamageOn)));
        return armorPieces;
    }

	public static Map<ArmorItem.Type, RegistryObject<Item>> fullArmorSet(String itemId, String textureId, boolean isDamageOn) {
		return registerArmorSet(itemId, textureId, true, isDamageOn);
	}

	public static Map<ArmorItem.Type, RegistryObject<Item>> fullArmorNoHelmetSet(String itemId, String textureId, boolean isDamageOn) {
		return registerArmorSet(itemId, textureId, false, isDamageOn);
	}

    public static Map<ArmorItem.Type, RegistryObject<Item>> fullArmorCapeSet(String itemId, String textureId, boolean isDamageOn) {
        return registerArmorSetCape(itemId, textureId, true, isDamageOn);
    }

    public static Map<ArmorItem.Type, RegistryObject<Item>> fullArmorCapeNoHelmetSet(String itemId, String textureId, boolean isDamageOn) {
        return registerArmorSetCape(itemId, textureId, false, isDamageOn);
    }

	public static void register(IEventBus bus) {
		ITEM_REGISTER.register(bus);
	}
}
