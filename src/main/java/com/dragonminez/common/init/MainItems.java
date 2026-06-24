package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.init.armor.ModArmorMaterials;
import com.dragonminez.common.init.item.*;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallSetDefinition;
import com.dragonminez.common.dragonball.DragonRadarDefinition;
import com.dragonminez.common.init.item.consumables.*;
import com.dragonminez.common.init.item.entities.BlackNimbusItem;
import com.dragonminez.common.init.item.entities.FlyingNimbusItem;
import com.dragonminez.common.init.item.entities.PunchMachineItem;
import com.dragonminez.common.init.item.entities.SaiyanShipItem;
import com.dragonminez.common.init.item.tools.ToolTiers;
import com.dragonminez.common.init.item.WeightItem;
import com.dragonminez.common.init.item.weapons.*;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("ALL")
public final class MainItems {
	public static final Item.Properties properties = new Item.Properties();
	public static final DeferredRegister<Item> ITEM_REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, Reference.MOD_ID);

	//CAPSULAS
	public static final RegistryObject<Item> RED_CAPSULE = ITEM_REGISTER.register("red_capsule", () -> new CapsuleItem(CapsuleType.STR));
	public static final RegistryObject<Item> PURPLE_CAPSULE = ITEM_REGISTER.register("purple_capsule", () -> new CapsuleItem(CapsuleType.SKP));
	public static final RegistryObject<Item> YELLOW_CAPSULE = ITEM_REGISTER.register("yellow_capsule", () -> new CapsuleItem(CapsuleType.RES));
	public static final RegistryObject<Item> GREEN_CAPSULE = ITEM_REGISTER.register("green_capsule", () -> new CapsuleItem(CapsuleType.VIT));
	public static final RegistryObject<Item> ORANGE_CAPSULE = ITEM_REGISTER.register("orange_capsule", () -> new CapsuleItem(CapsuleType.PWR));
	public static final RegistryObject<Item> BLUE_CAPSULE = ITEM_REGISTER.register("blue_capsule", () -> new CapsuleItem(CapsuleType.ENE));

	public static final RegistryObject<Item> GETE_RED_CAPSULE = ITEM_REGISTER.register("gete_red_capsule", () -> new CapsuleItem(CapsuleType.STR, ChatFormatting.GOLD, "Gete", 3, Rarity.RARE));
	public static final RegistryObject<Item> GETE_PURPLE_CAPSULE = ITEM_REGISTER.register("gete_purple_capsule", () -> new CapsuleItem(CapsuleType.SKP, ChatFormatting.GOLD, "Gete", 3, Rarity.RARE));
	public static final RegistryObject<Item> GETE_YELLOW_CAPSULE = ITEM_REGISTER.register("gete_yellow_capsule", () -> new CapsuleItem(CapsuleType.RES, ChatFormatting.GOLD, "Gete", 3, Rarity.RARE));
	public static final RegistryObject<Item> GETE_GREEN_CAPSULE = ITEM_REGISTER.register("gete_green_capsule", () -> new CapsuleItem(CapsuleType.VIT, ChatFormatting.GOLD, "Gete", 3, Rarity.RARE));
	public static final RegistryObject<Item> GETE_ORANGE_CAPSULE = ITEM_REGISTER.register("gete_orange_capsule", () -> new CapsuleItem(CapsuleType.PWR, ChatFormatting.GOLD, "Gete", 3, Rarity.RARE));
	public static final RegistryObject<Item> GETE_BLUE_CAPSULE = ITEM_REGISTER.register("gete_blue_capsule", () -> new CapsuleItem(CapsuleType.ENE, ChatFormatting.GOLD, "Gete", 3, Rarity.RARE));
	public static final RegistryObject<Item> SENZU_BEAN = ITEM_REGISTER.register("senzu_bean",
			() -> new FoodItem(20, 0.0f, 16));

	public static final RegistryObject<Item> MIGHT_TREE_FRUIT = ITEM_REGISTER.register("might_tree_fruit",
			MightTreeFruitItem::new);
	public static final RegistryObject<Item> DINO_MEAT_RAW = ITEM_REGISTER.register("raw_dino_meat",
			() -> new FoodItem(4, 3.6f, 64));
	public static final RegistryObject<Item> DINO_MEAT_COOKED = ITEM_REGISTER.register("cooked_dino_meat",
			() -> new FoodItem(8, 12.8f, 64));
	public static final RegistryObject<Item> BABY_DINO_MEAT_RAW = ITEM_REGISTER.register("raw_baby_dino_meat",
			() -> new FoodItem(2, 2.4f, 64));
	public static final RegistryObject<Item> BABY_DINO_MEAT_COOKED = ITEM_REGISTER.register("cooked_baby_dino_meat",
			() -> new FoodItem(5, 4.8f, 64));
	public static final RegistryObject<Item> HEART_MEDICINE = ITEM_REGISTER.register("heart_medicine",
			() -> new FoodItem(0, 0.0f, 16));
	public static final RegistryObject<Item> DINO_TAIL_RAW = ITEM_REGISTER.register("dino_tail_raw",
			() -> new FoodItem(6, 4.8f, 64));
	public static final RegistryObject<Item> DINO_TAIL_COOKED = ITEM_REGISTER.register("dino_tail_cooked",
			() -> new FoodItem(12, 9.6f, 64));
	public static final RegistryObject<Item> FROG_LEGS_RAW = ITEM_REGISTER.register("frog_legs_raw",
			() -> new FoodItem(2, 2.4f, 64));
	public static final RegistryObject<Item> FROG_LEGS_COOKED = ITEM_REGISTER.register("frog_legs_cooked",
			() -> new FoodItem(5, 4.8f, 64));

	//POTHALAS
	public static final RegistryObject<Item> POTHALA_LEFT =
			ITEM_REGISTER.register("pothala_left", () -> new DMZCuriosItem(new Item.Properties().fireResistant().stacksTo(1).defaultDurability(2), DMZCuriosItem.CurioType.HEAD_TECH));
	public static final RegistryObject<Item> POTHALA_RIGHT =
			ITEM_REGISTER.register("pothala_right", () -> new DMZCuriosItem(new Item.Properties().fireResistant().stacksTo(1).defaultDurability(2), DMZCuriosItem.CurioType.HEAD_TECH));
	public static final RegistryObject<Item> GREEN_POTHALA_LEFT =
			ITEM_REGISTER.register("green_pothala_left", () -> new DMZCuriosItem(new Item.Properties().fireResistant().stacksTo(1).defaultDurability(2), DMZCuriosItem.CurioType.HEAD_TECH));
	public static final RegistryObject<Item> GREEN_POTHALA_RIGHT =
			ITEM_REGISTER.register("green_pothala_right", () -> new DMZCuriosItem(new Item.Properties().fireResistant().stacksTo(1).defaultDurability(2), DMZCuriosItem.CurioType.HEAD_TECH));

	public static final RegistryObject<Item> RED_SCOUTER =
			ITEM_REGISTER.register("red_scouter", () -> new DMZCuriosItem(new Item.Properties().stacksTo(1).fireResistant().defaultDurability(15), DMZCuriosItem.CurioType.HEAD_TECH));
	public static final RegistryObject<Item> BLUE_SCOUTER =
			ITEM_REGISTER.register("blue_scouter", () -> new DMZCuriosItem(new Item.Properties().stacksTo(1).fireResistant().defaultDurability(15), DMZCuriosItem.CurioType.HEAD_TECH));
	public static final RegistryObject<Item> GREEN_SCOUTER =
			ITEM_REGISTER.register("green_scouter", () -> new DMZCuriosItem(new Item.Properties().stacksTo(1).fireResistant().defaultDurability(15), DMZCuriosItem.CurioType.HEAD_TECH));
	public static final RegistryObject<Item> PURPLE_SCOUTER =
			ITEM_REGISTER.register("purple_scouter", () -> new DMZCuriosItem(new Item.Properties().stacksTo(1).fireResistant().defaultDurability(15), DMZCuriosItem.CurioType.HEAD_TECH));

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

	//ARMAS A RANGO/PISTOLAS
	public static final RegistryObject<Item> MERUS_LASER =
			ITEM_REGISTER.register("laser_merus", () -> new MerusLaserItem());
	public static final RegistryObject<Item> BLASTER_CANNON =
			ITEM_REGISTER.register("blaster_cannon", () -> new BlasterCannonItem());

	//ARMADURAS
	public static final Map<ArmorItem.Type, RegistryObject<Item>> A13_ARMOR = fullArmorNoHelmetSet("a13_armor", "a13");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> A14_ARMOR = fullArmorNoHelmetSet("a14_armor", "a14");
	// A16
	public static final Map<ArmorItem.Type, RegistryObject<Item>> A16_ARMOR = fullArmorNoHelmetSet("a16_armor", "a16");
	// A17
	public static final Map<ArmorItem.Type, RegistryObject<Item>> A17_ARMOR = fullArmorNoHelmetSet("a17_armor", "a17");
	// ANDROID 17 SUPER
	public static final Map<ArmorItem.Type, RegistryObject<Item>> A17_SUPER_ARMOR = fullArmorNoHelmetSet("a17_super_armor", "a17super");
	// A18
	public static final Map<ArmorItem.Type, RegistryObject<Item>> A18_ARMOR = fullArmorNoHelmetSet("a18_armor", "a18");
	// ANDROID 18 CELL SAGA
	public static final Map<ArmorItem.Type, RegistryObject<Item>> A18_CELL_ARMOR = fullArmorNoHelmetSet("a18_cell_armor", "a18cell");
	// ANDROID 18 BUU SAGA (KAME HOUSE)
	public static final Map<ArmorItem.Type, RegistryObject<Item>> A18_KAME_ARMOR = fullArmorNoHelmetSet("a18_kame_armor", "a18kame");
	// ANDROID 18 BUU SAGA (WORLD TOURNAMENT)
	public static final Map<ArmorItem.Type, RegistryObject<Item>> A18_TOURNAMENT_ARMOR = fullArmorNoHelmetSet("a18_tournament_armor", "a18tournament");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> A20_ARMOR = fullArmorSet("a20_armor", "a20");
	// KEEP1000
	public static final Map<ArmorItem.Type, RegistryObject<Item>> AGE1000_ARMOR = fullArmorNoHelmetSet("age1000_armor", "age1000");
	// BARDOCK DBZ
	public static final Map<ArmorItem.Type, RegistryObject<Item>> BARDOCK_DBZ_ARMOR = fullArmorNoHelmetSet("bardock_dbz_armor", "bardock_armor");
	// BARDOCK SUPER
	public static final Map<ArmorItem.Type, RegistryObject<Item>> BARDOCK_SUPER_ARMOR = fullArmorNoHelmetSet("bardock_super_armor", "bardockdbs_armor");
	// BEERUS
	public static final Map<ArmorItem.Type, RegistryObject<Item>> BEERUS_ARMOR = fullArmorNoHelmetSet("beerus_armor", "beerus");
	// BLACK GOKU
	public static final Map<ArmorItem.Type, RegistryObject<Item>> BLACKGOKU_ARMOR = fullArmorNoHelmetSet("blackgoku_armor", "blackgoku");
	// BROLY SUPER
	public static final Map<ArmorItem.Type, RegistryObject<Item>> BROLY_SUPER_ARMOR = fullArmorNoHelmetSet("broly_super_armor", "broly_dbs");
	// BROLY Z
	public static final Map<ArmorItem.Type, RegistryObject<Item>> BROLY_Z_ARMOR = fullArmorNoHelmetSet("broly_z_armor", "broly_dbz");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> CAPSULE_CORP_ARMOR = fullArmorNoHelmetSet("capsule_corp_armor", "capsule_corp");
	// CAULIFLA
	public static final Map<ArmorItem.Type, RegistryObject<Item>> CAULIFLA_ARMOR = fullArmorNoHelmetSet("caulifla_armor", "caulifla");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> CHAOZ_ARMOR = fullArmorSet("chaoz_armor", "chaoz");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> COOLER_SOLDIER_ARMOR = fullArmorNoHelmetSet("cooler_soldier_armor", "cooler_soldier");
	// DEMON GI (AZUL)
	public static final Map<ArmorItem.Type, RegistryObject<Item>> DEMON_GI_BLUE_ARMOR = fullArmorNoHelmetSet("demon_gi_blue_armor", "demon_gi_gohan");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> EVIL_BUU_ARMOR = fullArmorNoHelmetSet("evil_buu_armor", "evil_buu");
	// FUSED ZAMASU
	public static final Map<ArmorItem.Type, RegistryObject<Item>> FUSION_ZAMASU_ARMOR = fullArmorNoHelmetSet("fusion_zamasu_armor", "fzamasu_gi");
	// FUTURE GOHAN
	public static final Map<ArmorItem.Type, RegistryObject<Item>> FUTURE_GOHAN_ARMOR = fullArmorNoHelmetSet("future_gohan_armor", "future_gohan");
	// GAMMA 1
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GAMMA1_ARMOR = fullArmorCapeNoHelmetSet("gamma1_armor", "gamma1");
	// GAMMA 2
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GAMMA2_ARMOR = fullArmorCapeNoHelmetSet("gamma2_armor", "gamma2");
	// GAS DBS
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GAS_ARMOR = fullArmorNoHelmetSet("gas_armor", "gas");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GERO_ARMOR = fullArmorNoHelmetSet("gero_armor", "gero");
	// GINE
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GINE_ARMOR = fullArmorNoHelmetSet("gine_armor", "gine");
	// GOGETA
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOGETA_ARMOR = fullArmorNoHelmetSet("gogeta_armor", "gogeta");
	// GOHAN GI (SUPER)
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOHAN_SUPER_ARMOR = fullArmorNoHelmetSet("gohan_super_armor", "gohan_dbs");
	//GOKU GI
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOKU_ARMOR = fullArmorNoHelmetSet("goku_armor", "goku_gi");
	// GOKU GT
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOKU_GT_ARMOR = fullArmorNoHelmetSet("goku_gt_armor", "goku_gt");
	// GOKU NIÑO
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOKU_KID_ARMOR = fullArmorNoHelmetSet("goku_kid_armor", "goku_kid");
	//Goku Super
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOKU_SUPER_ARMOR = fullArmorNoHelmetSet("goku_super_armor", "goku_super");
	// GOKU WHIS GI
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOKU_WHIS_ARMOR = fullArmorNoHelmetSet("goku_whis_armor", "gokuwhis");
	// GOTEN Z
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOTEN_ARMOR = fullArmorNoHelmetSet("goten_armor", "goten_gi");
	// GOTEN TEEN (SUPER)
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GOTEN_SUPER_ARMOR = fullArmorNoHelmetSet("goten_super_armor", "goten_dbs");
	// GRANOLA
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GRANOLA_ARMOR = fullArmorNoHelmetSet("granola_armor", "granola");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GREAT_SAIYAMAN_2_ARMOR = fullArmorCapeSet("great_saiyaman_2_armor", "great_saiyaman_2");
	// GOHAN GREAT SAIYAMAN
	public static final Map<ArmorItem.Type, RegistryObject<Item>> GREAT_SAIYAMAN_ARMOR = fullArmorCapeSet("great_saiyaman_armor", "saiyaman_gi");
	// HIT
	public static final Map<ArmorItem.Type, RegistryObject<Item>> HIT_ARMOR = fullArmorNoHelmetSet("hit_armor", "hit");
	//INVENCIBLE
	public static final Map<ArmorItem.Type, RegistryObject<Item>> INVENCIBLE_ARMOR = fullArmorSet("invencible_armor", "invencible");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> INVENCIBLE_BLUE_ARMOR = fullArmorSet("invencible_blue_armor", "invencible_blue");
	// KALE
	public static final Map<ArmorItem.Type, RegistryObject<Item>> KALE_ARMOR = fullArmorNoHelmetSet("kale_armor", "kale");
	// KEFLA
	public static final Map<ArmorItem.Type, RegistryObject<Item>> KEFLA_ARMOR = fullArmorNoHelmetSet("kefla_armor", "kefla");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> KIBITO_ARMOR = fullArmorNoHelmetSet("kibito_armor", "kibito");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> KING_VEGETA_ARMOR = fullArmorCapeNoHelmetSet("king_vegeta_armor", "king_vegeta");
	// MAJIN 21
	public static final Map<ArmorItem.Type, RegistryObject<Item>> MAJIN21_ARMOR = fullArmorNoHelmetSet("majin21_armor", "majin21");
	// MAJIN BUU
	public static final Map<ArmorItem.Type, RegistryObject<Item>> MAJIN_BUU_ARMOR = fullArmorCapeNoHelmetSet("majin_buu_armor", "majinbuu_gi");
	// NARUKE ARMOR
	public static final Map<ArmorItem.Type, RegistryObject<Item>> NARUKE_ARMOR = fullArmorNoHelmetSet("naruke_armor", "naruke");
	// ORANGE STAR HIGH SCHOOL UNIFORM
	public static final Map<ArmorItem.Type, RegistryObject<Item>> ORANGE_HIGH_ARMOR = fullArmorNoHelmetSet("orange_high_armor", "orange_high");
	// PICCOLO
	public static final Map<ArmorItem.Type, RegistryObject<Item>> PICCOLO_ARMOR = fullArmorCapeSet("piccolo_armor", "piccolo_gi");
	// TROPAS DEL ORGULLO
	public static final Map<ArmorItem.Type, RegistryObject<Item>> PRIDE_TROOPS_ARMOR = fullArmorNoHelmetSet("pride_troops_armor", "pride_troper");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> RADITZ_ARMOR = fullArmorNoHelmetSet("raditz_armor", "raditz_armor");
	// SHIN
	public static final Map<ArmorItem.Type, RegistryObject<Item>> SHIN_ARMOR = fullArmorNoHelmetSet("shin_armor", "kaioshin");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> SLUG_ARMOR = fullArmorSet("slug_armor", "slug");
	// THE STRONGEST
	public static final Map<ArmorItem.Type, RegistryObject<Item>> STRONGEST_ARMOR = fullArmorNoHelmetSet("strongest_armor", "strongest");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> SUBARU_NATSUKI_ARC6_ARMOR = fullArmorCapeNoHelmetSet("subaru_natsuki_arc6_armor", "subaru_natsuki_arc6");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> SUBARU_NATSUKI_ARMOR = fullArmorNoHelmetSet("subaru_natsuki_armor", "subaru_natsuki");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> SUPER_BUU_ARMOR = fullArmorNoHelmetSet("super_buu_armor", "super_buu");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> THRAGG_ARMOR = fullArmorNoHelmetSet("thragg_armor", "thragg");
	//TIEN
	public static final Map<ArmorItem.Type, RegistryObject<Item>> TIEN_ARMOR = fullArmorNoHelmetSet("tien_armor", "tenshinhan_armor");
	//TRUNKS KID DBZ
	public static final Map<ArmorItem.Type, RegistryObject<Item>> TRUNKS_KID_ARMOR = fullArmorNoHelmetSet("trunks_kid_armor", "trunks_gi");
	//TRUNKS SUPER
	public static final Map<ArmorItem.Type, RegistryObject<Item>> TRUNKS_SUPER_ARMOR = fullArmorNoHelmetSet("trunks_super_armor", "trunks_dbs");
	//TRUNKS Z
	public static final Map<ArmorItem.Type, RegistryObject<Item>> TRUNKS_Z_ARMOR = fullArmorNoHelmetSet("trunks_z_armor", "trunks_armor");
	// TURLES
	public static final Map<ArmorItem.Type, RegistryObject<Item>> TURLES_ARMOR = fullArmorNoHelmetSet("turles_armor", "turles_armor");
	// VEGETA SAGA BUU
	public static final Map<ArmorItem.Type, RegistryObject<Item>> VEGETA_BUU_ARMOR = fullArmorNoHelmetSet("vegeta_buu_armor", "vegetabuu");
	// VEGETA GT
	public static final Map<ArmorItem.Type, RegistryObject<Item>> VEGETA_GT_ARMOR = fullArmorNoHelmetSet("vegeta_gt_armor", "vegetagt");
	// VEGETA SAGA NAMEK
	public static final Map<ArmorItem.Type, RegistryObject<Item>> VEGETA_NAMEK_ARMOR = fullArmorNoHelmetSet("vegeta_namek_armor", "vegetanamek_armor");
	// VEGETA SAGA SAIYAJIN (Cambiar luego a saiyanArmor para hombreras)
	public static final Map<ArmorItem.Type, RegistryObject<Item>> VEGETA_SAIYAN_ARMOR = fullArmorNoHelmetSet("vegeta_saiyan_armor", "vegeta_saiyan_armor");
	// VEGETA ARMADURA DE SUPER
	public static final Map<ArmorItem.Type, RegistryObject<Item>> VEGETA_SUPER_ARMOR = fullArmorNoHelmetSet("vegeta_super_armor", "vegetasuper");
	// VEGETA WHIS GI
	public static final Map<ArmorItem.Type, RegistryObject<Item>> VEGETA_WHIS_ARMOR = fullArmorNoHelmetSet("vegeta_whis_armor", "vegetawhis");
	// VEGETA SAGA CELL
	public static final Map<ArmorItem.Type, RegistryObject<Item>> VEGETA_Z_ARMOR = fullArmorNoHelmetSet("vegeta_z_armor", "vegetaz_armor");
	// VEGETTO
	public static final Map<ArmorItem.Type, RegistryObject<Item>> VEGETTO_ARMOR = fullArmorNoHelmetSet("vegetto_armor", "vegetto");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> VERGIL_ARMOR = fullArmorNoHelmetSet("vergil_armor", "vergil");
	// VIDEL
	public static final Map<ArmorItem.Type, RegistryObject<Item>> VIDEL_ARMOR = fullArmorNoHelmetSet("videl_armor", "videl");
	// WHIS
	public static final Map<ArmorItem.Type, RegistryObject<Item>> WHIS_ARMOR = fullArmorNoHelmetSet("whis_armor", "whis");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> XENO_GOKU_ARMOR = fullArmorNoHelmetSet("xeno_goku_armor", "xeno_goku");
	public static final Map<ArmorItem.Type, RegistryObject<Item>> XENO_GOKU_PATREON_ARMOR = fullArmorNoHelmetSet("xeno_goku_patreon_armor", "xeno_goku_patreon");
	// YARDRAT
	public static final Map<ArmorItem.Type, RegistryObject<Item>> YARDRAT_ARMOR = fullArmorNoHelmetSet("yardrat_armor", "yardrat_gi");
	// ZAMASU
	public static final Map<ArmorItem.Type, RegistryObject<Item>> ZAMASU_ARMOR = fullArmorNoHelmetSet("zamasu_armor", "zamasu_gi");

	//LÍQUIDOS
	public static final RegistryObject<Item> HEALING_BUCKET = ITEM_REGISTER.register("healing_liquid_bucket",
			() -> new BucketItem(MainFluids.SOURCE_HEALING, properties.stacksTo(1)));

	public static final RegistryObject<Item> NAMEK_WATER_BUCKET = ITEM_REGISTER.register("namek_water_bucket",
			() -> new BucketItem(MainFluids.SOURCE_NAMEK, properties.stacksTo(1)));

	//MINERALES
	public static final RegistryObject<Item> GETE_SCRAP = regItem("gete_scrap");
	public static final RegistryObject<Item> GETE_INGOT = regItem("gete_ingot");
	// Novel wave: Ki Accumulator consumable (restores ki/energy) + Anti-Ki Cloak curios (hides BP from scouters).
	public static final RegistryObject<Item> KI_BATTERY = ITEM_REGISTER.register("ki_battery",
			() -> new KiBatteryItem());
	public static final RegistryObject<Item> ANTI_KI_CLOAK = ITEM_REGISTER.register("anti_ki_cloak",
			() -> new DMZCuriosItem(new Item.Properties().stacksTo(1).fireResistant(), DMZCuriosItem.CurioType.HEAD_TECH));
	public static final RegistryObject<Item> KIKONO_SHARD = regItem("kikono_shard");
	public static final RegistryObject<Item> KIKONO_STICK = regItem("kikono_stick");

	// WEIGHTS
	public static final RegistryObject<Item> WEIGHT_TURTLE_SHELL = ITEM_REGISTER.register("weight_turtle_shell", () -> new WeightItem(new Item.Properties().stacksTo(1), WeightItem.WeightType.TURTLE_SHELL));
	public static final RegistryObject<Item> WORKOUT_WEIGHTS = ITEM_REGISTER.register("workout_weights", () -> new WeightItem(new Item.Properties().stacksTo(1), WeightItem.WeightType.WORKOUT_WEIGHTS));
	public static final RegistryObject<Item> WEIGHT_PICCOLO_CAPE = ITEM_REGISTER.register("weight_piccolo_cape", () -> new WeightItem(new Item.Properties().stacksTo(1), WeightItem.WeightType.PICCOLO_CAPE));

	// HERRAMIENTAS
	public static final RegistryObject<Item> PATTERN_GETE = regItem("pattern_gete");
	public static final RegistryObject<Item> GETE_PICKAXE = ITEM_REGISTER.register("gete_pickaxe",
			() -> new PickaxeItem(ToolTiers.GETE_TIER, 1, -2.8F, new Item.Properties().fireResistant()));

	public static final RegistryObject<Item> GETE_AXE = ITEM_REGISTER.register("gete_axe",
			() -> new AxeItem(ToolTiers.GETE_TIER, 5.0F, -3.0F, new Item.Properties().fireResistant()));

	public static final RegistryObject<Item> GETE_SHOVEL = ITEM_REGISTER.register("gete_shovel",
			() -> new ShovelItem(ToolTiers.GETE_TIER, 1.5F, -3.0F, new Item.Properties().fireResistant()));

	public static final RegistryObject<Item> GETE_HOE = ITEM_REGISTER.register("gete_hoe",
			() -> new HoeItem(ToolTiers.GETE_TIER, -4, 0.0F, new Item.Properties().fireResistant()));

	//DRAGON BALL RADAR
	private static final Map<String, RegistryObject<Item>> DRAGON_RADAR_ITEMS = registerDragonRadarItems();
	private static final Map<String, RegistryObject<Item>> DRAGON_RADAR_CHIP_ITEMS = registerDragonRadarChipItems();
	private static final Map<String, RegistryObject<Item>> DRAGON_RADAR_CPU_ITEMS = registerDragonRadarCpuItems();

	public static final RegistryObject<Item> DBALL_RADAR_ITEM = getDragonRadarItemOrThrow("earth_radar");
	public static final RegistryObject<Item> NAMEKDBALL_RADAR_ITEM = getDragonRadarItemOrThrow("namek_radar");
	public static final RegistryObject<Item> FUSED_DBALL_RADAR_ITEM = getDragonRadarItemOrThrow("fused_radar");
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

	//MUSIC DISCS
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_1 = regMusicDisc("music_disc_menu_music_1", MainSounds.MENU_MUSIC_1, 788);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_2 = regMusicDisc("music_disc_menu_music_2", MainSounds.MENU_MUSIC_2, 1212);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_3 = regMusicDisc("music_disc_menu_music_3", MainSounds.MENU_MUSIC_3, 920);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_4 = regMusicDisc("music_disc_menu_music_4", MainSounds.MENU_MUSIC_4, 820);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_5 = regMusicDisc("music_disc_menu_music_5", MainSounds.MENU_MUSIC_5, 640);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_6 = regMusicDisc("music_disc_menu_music_6", MainSounds.MENU_MUSIC_6, 1920);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_7 = regMusicDisc("music_disc_menu_music_7", MainSounds.MENU_MUSIC_7, 800);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_8 = regMusicDisc("music_disc_menu_music_8", MainSounds.MENU_MUSIC_8, 780);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_9 = regMusicDisc("music_disc_menu_music_9", MainSounds.MENU_MUSIC_9, 1104);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_10 = regMusicDisc("music_disc_menu_music_10", MainSounds.MENU_MUSIC_10, 1320);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_11 = regMusicDisc("music_disc_menu_music_11", MainSounds.MENU_MUSIC_11, 1440);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_12 = regMusicDisc("music_disc_menu_music_12", MainSounds.MENU_MUSIC_12, 880);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_13 = regMusicDisc("music_disc_menu_music_13", MainSounds.MENU_MUSIC_13, 3240);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_14 = regMusicDisc("music_disc_menu_music_14", MainSounds.MENU_MUSIC_14, 1860);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_15 = regMusicDisc("music_disc_menu_music_15", MainSounds.MENU_MUSIC_15, 1780);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_16 = regMusicDisc("music_disc_menu_music_16", MainSounds.MENU_MUSIC_16, 880);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_17 = regMusicDisc("music_disc_menu_music_17", MainSounds.MENU_MUSIC_17, 1606);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_18 = regMusicDisc("music_disc_menu_music_18", MainSounds.MENU_MUSIC_18, 1358);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_19 = regMusicDisc("music_disc_menu_music_19", MainSounds.MENU_MUSIC_19, 1264);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_20 = regMusicDisc("music_disc_menu_music_20", MainSounds.MENU_MUSIC_20, 2020);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_21 = regMusicDisc("music_disc_menu_music_21", MainSounds.MENU_MUSIC_21, 1092);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_22 = regMusicDisc("music_disc_menu_music_22", MainSounds.MENU_MUSIC_22, 3286);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_23 = regMusicDisc("music_disc_menu_music_23", MainSounds.MENU_MUSIC_23, 1670);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_24 = regMusicDisc("music_disc_menu_music_24", MainSounds.MENU_MUSIC_24, 1936);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_25 = regMusicDisc("music_disc_menu_music_25", MainSounds.MENU_MUSIC_25, 2674);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_26 = regMusicDisc("music_disc_menu_music_26", MainSounds.MENU_MUSIC_26, 1944);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_27 = regMusicDisc("music_disc_menu_music_27", MainSounds.MENU_MUSIC_27, 2288);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_28 = regMusicDisc("music_disc_menu_music_28", MainSounds.MENU_MUSIC_28, 1316);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_29 = regMusicDisc("music_disc_menu_music_29", MainSounds.MENU_MUSIC_29, 2238);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_30 = regMusicDisc("music_disc_menu_music_30", MainSounds.MENU_MUSIC_30, 2218);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_31 = regMusicDisc("music_disc_menu_music_31", MainSounds.MENU_MUSIC_31, 5500);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_32 = regMusicDisc("music_disc_menu_music_32", MainSounds.MENU_MUSIC_32, 1500);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_33 = regMusicDisc("music_disc_menu_music_33", MainSounds.MENU_MUSIC_33, 4000);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_34 = regMusicDisc("music_disc_menu_music_34", MainSounds.MENU_MUSIC_34, 5180);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_35 = regMusicDisc("music_disc_menu_music_35", MainSounds.MENU_MUSIC_35, 4900);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_36 = regMusicDisc("music_disc_menu_music_36", MainSounds.MENU_MUSIC_36, 2720);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_37 = regMusicDisc("music_disc_menu_music_37", MainSounds.MENU_MUSIC_37, 3180);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_38 = regMusicDisc("music_disc_menu_music_38", MainSounds.MENU_MUSIC_38, 5560);
	public static final RegistryObject<Item> MUSIC_DISC_MENU_MUSIC_39 = regMusicDisc("music_disc_menu_music_39", MainSounds.MENU_MUSIC_39, 5880);

	//KIKONO STATION/ARMOR CRAFTING PATTERNS
	public static final RegistryObject<Item> ARMOR_CRAFTING_KIT = ITEM_REGISTER.register("armor_crafting_kit",
			() -> new ArmorCraftingKitItem(properties.stacksTo(1)));
	public static final RegistryObject<Item> KIKONO_STRING = regItem("kikono_string");
	public static final RegistryObject<Item> KIKONO_CLOTH = regItem("kikono_cloth");
	public static final RegistryObject<Item> BLANK_PATTERN_Z = regItem("blank_pattern_z");
	public static final RegistryObject<Item> BLANK_PATTERN_SUPER = regItem("blank_pattern_super");
	public static final RegistryObject<Item> PATTERN_A13 = regItem("pattern_a13");
	public static final RegistryObject<Item> PATTERN_A14 = regItem("pattern_a14");
	public static final RegistryObject<Item> PATTERN_A16 = regItem("pattern_a16");
	public static final RegistryObject<Item> PATTERN_A17 = regItem("pattern_a17");
	public static final RegistryObject<Item> PATTERN_A17_SUPER = regItem("pattern_a17_super");
	public static final RegistryObject<Item> PATTERN_A18 = regItem("pattern_a18");
	public static final RegistryObject<Item> PATTERN_A18_CELL = regItem("pattern_a18_cell");
	public static final RegistryObject<Item> PATTERN_A18_KAME = regItem("pattern_a18_kame");
	public static final RegistryObject<Item> PATTERN_A18_TOURNAMENT = regItem("pattern_a18_tournament");
	public static final RegistryObject<Item> PATTERN_A20 = regItem("pattern_a20");
	public static final RegistryObject<Item> PATTERN_AGE1000 = regItem("pattern_age1000");
	public static final RegistryObject<Item> PATTERN_BARDOCK1 = regItem("pattern_bardock1");
	public static final RegistryObject<Item> PATTERN_BARDOCK2 = regItem("pattern_bardock2");
	public static final RegistryObject<Item> PATTERN_BEERUS = regItem("pattern_beerus");
	public static final RegistryObject<Item> PATTERN_BLACK = regItem("pattern_black");
	public static final RegistryObject<Item> PATTERN_BROLY_SUPER = regItem("pattern_broly_super");
	public static final RegistryObject<Item> PATTERN_BROLY_Z = regItem("pattern_broly_z");
	public static final RegistryObject<Item> PATTERN_CAPSULE_CORP = regItem("pattern_capsule_corp");
	public static final RegistryObject<Item> PATTERN_CAULIFLA = regItem("pattern_caulifla");
	public static final RegistryObject<Item> PATTERN_CHAOZ = regItem("pattern_chaoz");
	public static final RegistryObject<Item> PATTERN_COOLER_SOLDIER = regItem("pattern_cooler_soldier");
	public static final RegistryObject<Item> PATTERN_EVIL_BUU = regItem("pattern_evil_buu");
	public static final RegistryObject<Item> PATTERN_FUSION_ZAMASU = regItem("pattern_fusionzamasu");
	public static final RegistryObject<Item> PATTERN_FUTURE_GOHAN = regItem("pattern_future_gohan");
	public static final RegistryObject<Item> PATTERN_GAMMA1 = regItem("pattern_gamma1");
	public static final RegistryObject<Item> PATTERN_GAMMA2 = regItem("pattern_gamma2");
	public static final RegistryObject<Item> PATTERN_GAS = regItem("pattern_gas");
	public static final RegistryObject<Item> PATTERN_GERO = regItem("pattern_gero");
	public static final RegistryObject<Item> PATTERN_GINE = regItem("pattern_gine");
	public static final RegistryObject<Item> PATTERN_GOGETA = regItem("pattern_gogeta");
	public static final RegistryObject<Item> PATTERN_GOHAN1 = regItem("pattern_gohan1");
	public static final RegistryObject<Item> PATTERN_GOHAN_SUPER = regItem("pattern_gohan_super");
	public static final RegistryObject<Item> PATTERN_GOKU1 = regItem("pattern_goku1");
	public static final RegistryObject<Item> PATTERN_GOKU_GT = regItem("pattern_goku_gt");
	public static final RegistryObject<Item> PATTERN_GOKU_KID = regItem("pattern_goku_kid");
	public static final RegistryObject<Item> PATTERN_GOKU_SUPER = regItem("pattern_goku_super");
	public static final RegistryObject<Item> PATTERN_GOKU_WHIS = regItem("pattern_goku_whis");
	public static final RegistryObject<Item> PATTERN_GOTEN = regItem("pattern_goten");
	public static final RegistryObject<Item> PATTERN_GOTEN_SUPER = regItem("pattern_goten_super");
	public static final RegistryObject<Item> PATTERN_GRANOLA = regItem("pattern_granola");
	public static final RegistryObject<Item> PATTERN_GREAT_SAIYAMAN = regItem("pattern_great_saiyaman");
	public static final RegistryObject<Item> PATTERN_GREAT_SAIYAMAN_2 = regItem("pattern_great_saiyaman_2");
	public static final RegistryObject<Item> PATTERN_HIT = regItem("pattern_hit");
	public static final RegistryObject<Item> PATTERN_KALE = regItem("pattern_kale");
	public static final RegistryObject<Item> PATTERN_KEFLA = regItem("pattern_kefla");
	public static final RegistryObject<Item> PATTERN_KIBITO = regItem("pattern_kibito");
	public static final RegistryObject<Item> PATTERN_KING_VEGETA = regItem("pattern_king_vegeta");
	public static final RegistryObject<Item> PATTERN_MAJIN21 = regItem("pattern_majin21");
	public static final RegistryObject<Item> PATTERN_MAJIN_BUU = regItem("pattern_majin_buu");
	public static final RegistryObject<Item> PATTERN_ORANGE_HIGH = regItem("pattern_orange_high");
	public static final RegistryObject<Item> PATTERN_PICCOLO = regItem("pattern_piccolo");
	public static final RegistryObject<Item> PATTERN_PRIDE_TROOPS = regItem("pattern_pride_troops");
	public static final RegistryObject<Item> PATTERN_RADITZ = regItem("pattern_raditz");
	public static final RegistryObject<Item> PATTERN_SHIN = regItem("pattern_shin");
	public static final RegistryObject<Item> PATTERN_SLUG = regItem("pattern_slug");
	public static final RegistryObject<Item> PATTERN_SUPER_BUU = regItem("pattern_super_buu");
	public static final RegistryObject<Item> PATTERN_TIEN = regItem("pattern_tien");
	public static final RegistryObject<Item> PATTERN_TRUNKS_KID = regItem("pattern_trunks_kid");
	public static final RegistryObject<Item> PATTERN_TRUNKS_SUPER = regItem("pattern_trunks_super");
	public static final RegistryObject<Item> PATTERN_TRUNKS_Z = regItem("pattern_trunks_z");
	public static final RegistryObject<Item> PATTERN_TURLES = regItem("pattern_turles");
	public static final RegistryObject<Item> PATTERN_VEGETA1 = regItem("pattern_vegeta1");
	public static final RegistryObject<Item> PATTERN_VEGETA2 = regItem("pattern_vegeta2");
	public static final RegistryObject<Item> PATTERN_VEGETA_BUU = regItem("pattern_vegeta_buu");
	public static final RegistryObject<Item> PATTERN_VEGETA_GT = regItem("pattern_vegeta_gt");
	public static final RegistryObject<Item> PATTERN_VEGETA_SUPER = regItem("pattern_vegeta_super");
	public static final RegistryObject<Item> PATTERN_VEGETA_WHIS = regItem("pattern_vegeta_whis");
	public static final RegistryObject<Item> PATTERN_VEGETA_Z = regItem("pattern_vegeta_z");
	public static final RegistryObject<Item> PATTERN_VEGETTO = regItem("pattern_vegetto");
	public static final RegistryObject<Item> PATTERN_VERGIL = regItem("pattern_vergil");
	public static final RegistryObject<Item> PATTERN_VIDEL = regItem("pattern_videl");
	public static final RegistryObject<Item> PATTERN_WHIS = regItem("pattern_whis");
	public static final RegistryObject<Item> PATTERN_XENO_GOKU = regItem("pattern_xeno_goku");
	public static final RegistryObject<Item> PATTERN_XENO_GOKU_PATREON = regItem("pattern_xeno_goku_patreon");
	public static final RegistryObject<Item> PATTERN_YARDRAT = regItem("pattern_yardrat");
	public static final RegistryObject<Item> PATTERN_ZAMASU = regItem("pattern_zamasu");

	//DRAGON BALLS
	private static final Map<String, Map<Integer, RegistryObject<Item>>> DRAGON_BALL_BLOCK_ITEMS = registerDragonBallBlockItems();

	public static final RegistryObject<Item> DBALL1_BLOCK_ITEM = getDragonBallBlockItemOrThrow("earth", 1);
	public static final RegistryObject<Item> DBALL2_BLOCK_ITEM = getDragonBallBlockItemOrThrow("earth", 2);
	public static final RegistryObject<Item> DBALL3_BLOCK_ITEM = getDragonBallBlockItemOrThrow("earth", 3);
	public static final RegistryObject<Item> DBALL4_BLOCK_ITEM = getDragonBallBlockItemOrThrow("earth", 4);
	public static final RegistryObject<Item> DBALL5_BLOCK_ITEM = getDragonBallBlockItemOrThrow("earth", 5);
	public static final RegistryObject<Item> DBALL6_BLOCK_ITEM = getDragonBallBlockItemOrThrow("earth", 6);
	public static final RegistryObject<Item> DBALL7_BLOCK_ITEM = getDragonBallBlockItemOrThrow("earth", 7);
	public static final RegistryObject<Item> DBALL1_NAMEK_BLOCK_ITEM = getDragonBallBlockItemOrThrow("namek", 1);
	public static final RegistryObject<Item> DBALL2_NAMEK_BLOCK_ITEM = getDragonBallBlockItemOrThrow("namek", 2);
	public static final RegistryObject<Item> DBALL3_NAMEK_BLOCK_ITEM = getDragonBallBlockItemOrThrow("namek", 3);
	public static final RegistryObject<Item> DBALL4_NAMEK_BLOCK_ITEM = getDragonBallBlockItemOrThrow("namek", 4);
	public static final RegistryObject<Item> DBALL5_NAMEK_BLOCK_ITEM = getDragonBallBlockItemOrThrow("namek", 5);
	public static final RegistryObject<Item> DBALL6_NAMEK_BLOCK_ITEM = getDragonBallBlockItemOrThrow("namek", 6);
	public static final RegistryObject<Item> DBALL7_NAMEK_BLOCK_ITEM = getDragonBallBlockItemOrThrow("namek", 7);

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

	private static RegistryObject<Item> regMusicDisc(String name, RegistryObject<SoundEvent> sound, int lengthInTicks) {
		return ITEM_REGISTER.register(name, () -> new RecordItem(15, sound, new Item.Properties().stacksTo(1).rarity(Rarity.RARE), lengthInTicks));
	}

	private static Map<ArmorItem.Type, RegistryObject<Item>> registerArmorSet(String name, String texture, boolean hasHelmet) {
		Map<ArmorItem.Type, RegistryObject<Item>> armorPieces = new HashMap<>();
		if (hasHelmet) armorPieces.put(ArmorItem.Type.HELMET, ITEM_REGISTER.register(name + "_helmet", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.HELMET, new Item.Properties().fireResistant().stacksTo(1), texture)));
		armorPieces.put(ArmorItem.Type.CHESTPLATE, ITEM_REGISTER.register(name + "_chestplate", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant().stacksTo(1), texture)));
		armorPieces.put(ArmorItem.Type.LEGGINGS, ITEM_REGISTER.register(name + "_leggings", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant().stacksTo(1), texture)));
		armorPieces.put(ArmorItem.Type.BOOTS, ITEM_REGISTER.register(name + "_boots", () -> new DbzArmorItem(ModArmorMaterials.KIKONO, ArmorItem.Type.BOOTS, new Item.Properties().fireResistant().stacksTo(1), texture)));
		return armorPieces;
	}

	private static Map<ArmorItem.Type, RegistryObject<Item>> registerArmorSetCape(String name, String texture, boolean hasHelmet) {
		Map<ArmorItem.Type, RegistryObject<Item>> armorPieces = new HashMap<>();
		if (hasHelmet) {
			armorPieces.put(ArmorItem.Type.HELMET, ITEM_REGISTER.register(name + "_helmet", () -> new DbzArmorCapeItem(ModArmorMaterials.KIKONO, ArmorItem.Type.HELMET, new Item.Properties().fireResistant().stacksTo(1), texture)));
		}
		armorPieces.put(ArmorItem.Type.CHESTPLATE, ITEM_REGISTER.register(name + "_chestplate", () -> new DbzArmorCapeItem(ModArmorMaterials.KIKONO, ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant().stacksTo(1), texture)));
		armorPieces.put(ArmorItem.Type.LEGGINGS, ITEM_REGISTER.register(name + "_leggings", () -> new DbzArmorCapeItem(ModArmorMaterials.KIKONO, ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant().stacksTo(1), texture)));
		armorPieces.put(ArmorItem.Type.BOOTS, ITEM_REGISTER.register(name + "_boots", () -> new DbzArmorCapeItem(ModArmorMaterials.KIKONO, ArmorItem.Type.BOOTS, new Item.Properties().fireResistant().stacksTo(1), texture)));
		return armorPieces;
	}

	public static Map<ArmorItem.Type, RegistryObject<Item>> fullArmorSet(String itemId, String textureId) {
		return registerArmorSet(itemId, textureId, true);
	}

	public static Map<ArmorItem.Type, RegistryObject<Item>> fullArmorNoHelmetSet(String itemId, String textureId) {
		return registerArmorSet(itemId, textureId, false);
	}

	public static Map<ArmorItem.Type, RegistryObject<Item>> fullArmorCapeSet(String itemId, String textureId) {
		return registerArmorSetCape(itemId, textureId, true);
	}

	public static Map<ArmorItem.Type, RegistryObject<Item>> fullArmorCapeNoHelmetSet(String itemId, String textureId) {
		return registerArmorSetCape(itemId, textureId, false);
	}

	private static Map<String, Map<Integer, RegistryObject<Item>>> registerDragonBallBlockItems() {
		Map<String, Map<Integer, RegistryObject<Item>>> registered = new HashMap<>();
		for (DragonBallSetDefinition definition : DragonBallDefinitions.getBootstrapBallSets()) {
			Map<Integer, RegistryObject<Item>> setItems = new HashMap<>();
			for (Map.Entry<Integer, String> entry : definition.getBlockRegistryNamesByStar().entrySet()) {
				int star = entry.getKey();
				String registryName = entry.getValue();
				RegistryObject<Item> item = ITEM_REGISTER.register(registryName,
						() -> new BlockItem(MainBlocks.getDragonBallBlockOrThrow(definition.getId(), star).get(), new Item.Properties().stacksTo(1).fireResistant()));
				setItems.put(star, item);
			}
			registered.put(definition.getId(), Map.copyOf(setItems));
		}
		return Map.copyOf(registered);
	}

	public static RegistryObject<Item> getDragonBallBlockItemOrThrow(String setId, int star) {
		Map<Integer, RegistryObject<Item>> setItems = DRAGON_BALL_BLOCK_ITEMS.get(setId);
		if (setItems == null || !setItems.containsKey(star)) {
			throw new IllegalArgumentException("No dragon ball block item registered for set '" + setId + "' star " + star);
		}
		return setItems.get(star);
	}

	public static Map<Integer, RegistryObject<Item>> getDragonBallBlockItems(String setId) {
		Map<Integer, RegistryObject<Item>> setItems = DRAGON_BALL_BLOCK_ITEMS.get(setId);
		return setItems == null ? Map.of() : setItems;
	}

	private static Map<String, RegistryObject<Item>> registerDragonRadarItems() {
		Map<String, RegistryObject<Item>> registered = new LinkedHashMap<>();
		for (DragonRadarDefinition definition : DragonBallDefinitions.getBootstrapRadars()) {
			RegistryObject<Item> item = ITEM_REGISTER.register(definition.getItemRegistryName(), () -> new DragonRadarItem(definition.getId()));
			registered.put(definition.getId(), item);
		}
		return Map.copyOf(registered);
	}

	private static Map<String, RegistryObject<Item>> registerDragonRadarChipItems() {
		Map<String, RegistryObject<Item>> registered = new LinkedHashMap<>();
		for (DragonRadarDefinition definition : DragonBallDefinitions.getBootstrapRadars()) {
			definition.getChipRegistryName().ifPresent(registryName -> {
				RegistryObject<Item> item = ITEM_REGISTER.register(registryName, () -> new Item(properties.stacksTo(16)));
				registered.put(definition.getId(), item);
			});
		}
		return Map.copyOf(registered);
	}

	private static Map<String, RegistryObject<Item>> registerDragonRadarCpuItems() {
		Map<String, RegistryObject<Item>> registered = new LinkedHashMap<>();
		for (DragonRadarDefinition definition : DragonBallDefinitions.getBootstrapRadars()) {
			definition.getCpuRegistryName().ifPresent(registryName -> {
				RegistryObject<Item> item = ITEM_REGISTER.register(registryName, () -> new Item(properties.stacksTo(16)));
				registered.put(definition.getId(), item);
			});
		}
		return Map.copyOf(registered);
	}

	public static RegistryObject<Item> getDragonRadarItemOrThrow(String radarId) {
		RegistryObject<Item> item = DRAGON_RADAR_ITEMS.get(radarId);
		if (item == null) {
			throw new IllegalArgumentException("No dragon radar item registered for radar '" + radarId + "'");
		}
		return item;
	}

	public static RegistryObject<Item> getDragonRadarChipItem(String radarId) {
		return DRAGON_RADAR_CHIP_ITEMS.get(radarId);
	}

	public static RegistryObject<Item> getDragonRadarCpuItem(String radarId) {
		return DRAGON_RADAR_CPU_ITEMS.get(radarId);
	}

	public static void register(IEventBus bus) {
		ITEM_REGISTER.register(bus);
	}
}
