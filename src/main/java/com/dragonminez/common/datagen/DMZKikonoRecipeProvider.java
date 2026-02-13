package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.datagen.builder.KikonoRecipeBuilder;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;
import java.util.function.Consumer;

public class DMZKikonoRecipeProvider {
	private final Consumer<FinishedRecipe> consumer;

	public DMZKikonoRecipeProvider(Consumer<FinishedRecipe> consumer) {
		this.consumer = consumer;
	}

	protected void generate(){
		buildArmorNoHelmetSet("goku_kid", MainItems.GOKU_KID_ARMOR, MainItems.PATTERN_GOKU_KID.get());
		buildArmorNoHelmetSet("goku", MainItems.GOKU_ARMOR, MainItems.PATTERN_GOKU1.get());
		buildArmorNoHelmetSet("goku_super", MainItems.GOKU_SUPER_ARMOR, MainItems.PATTERN_GOKU_SUPER.get());
		buildArmorNoHelmetSet("goku_gt", MainItems.GOKU_GT_ARMOR, MainItems.PATTERN_GOKU_GT.get());
		buildArmorNoHelmetSet("yardrat", MainItems.YARDRAT_ARMOR, MainItems.PATTERN_YARDRAT.get());
		buildArmorNoHelmetSet("goten", MainItems.GOTEN_ARMOR, MainItems.PATTERN_GOTEN.get());
		buildArmorNoHelmetSet("goten_super", MainItems.GOTEN_SUPER_ARMOR, MainItems.PATTERN_GOTEN_SUPER.get());
		buildArmorNoHelmetSet("gohan_super", MainItems.GOHAN_SUPER_ARMOR, MainItems.PATTERN_GOHAN_SUPER.get());
		buildFullArmorSet("great_saiyaman", MainItems.GREAT_SAIYAMAN_ARMOR, MainItems.PATTERN_GREAT_SAIYAMAN.get());
		buildArmorNoHelmetSet("future_gohan", MainItems.FUTURE_GOHAN_ARMOR, MainItems.PATTERN_FUTURE_GOHAN.get());
		buildArmorNoHelmetSet("vegeta_saiyan", MainItems.VEGETA_SAIYAN_ARMOR, MainItems.PATTERN_VEGETA1.get());
		buildArmorNoHelmetSet("vegeta_namek", MainItems.VEGETA_NAMEK_ARMOR, MainItems.PATTERN_VEGETA2.get());
		buildArmorNoHelmetSet("vegeta_z", MainItems.VEGETA_Z_ARMOR, MainItems.PATTERN_VEGETA_Z.get());
		buildArmorNoHelmetSet("vegeta_buu", MainItems.VEGETA_BUU_ARMOR, MainItems.PATTERN_VEGETA_BUU.get());
		buildArmorNoHelmetSet("vegeta_super", MainItems.VEGETA_SUPER_ARMOR, MainItems.PATTERN_VEGETA_SUPER.get());
		buildArmorNoHelmetSet("vegetto", MainItems.VEGETTO_ARMOR, MainItems.PATTERN_VEGETTO.get());
		buildArmorNoHelmetSet("gogeta", MainItems.GOGETA_ARMOR, MainItems.PATTERN_GOGETA.get());
		buildFullArmorSet("piccolo", MainItems.PICCOLO_ARMOR, MainItems.PATTERN_PICCOLO.get());
		buildArmorNoHelmetSet("demon_gi_blue", MainItems.DEMON_GI_BLUE_ARMOR, MainItems.PATTERN_GOHAN1.get());
		buildArmorNoHelmetSet("bardock_dbz", MainItems.BARDOCK_DBZ_ARMOR, MainItems.PATTERN_BARDOCK1.get());
		buildArmorNoHelmetSet("bardock_super", MainItems.BARDOCK_SUPER_ARMOR, MainItems.PATTERN_BARDOCK2.get());
		buildArmorNoHelmetSet("turles", MainItems.TURLES_ARMOR, MainItems.PATTERN_TURLES.get());
		buildArmorNoHelmetSet("tien", MainItems.TIEN_ARMOR, MainItems.PATTERN_TIEN.get());
		buildArmorNoHelmetSet("trunks_z", MainItems.TRUNKS_Z_ARMOR, MainItems.PATTERN_TRUNKS_Z.get());
		buildArmorNoHelmetSet("trunks_super", MainItems.TRUNKS_SUPER_ARMOR, MainItems.PATTERN_TRUNKS_SUPER.get());
		buildArmorNoHelmetSet("trunks_kid", MainItems.TRUNKS_KID_ARMOR, MainItems.PATTERN_TRUNKS_KID.get());
		buildArmorNoHelmetSet("broly_z", MainItems.BROLY_Z_ARMOR, MainItems.PATTERN_BROLY_Z.get());
		buildArmorNoHelmetSet("broly_super", MainItems.BROLY_SUPER_ARMOR, MainItems.PATTERN_BROLY_SUPER.get());
		buildArmorNoHelmetSet("shin", MainItems.SHIN_ARMOR, MainItems.PATTERN_SHIN.get());
		buildArmorNoHelmetSet("blackgoku", MainItems.BLACKGOKU_ARMOR, MainItems.PATTERN_BLACK.get());
		buildArmorNoHelmetSet("zamasu", MainItems.ZAMASU_ARMOR, MainItems.PATTERN_ZAMASU.get());
		buildArmorNoHelmetSet("fusion_zamasu", MainItems.FUSION_ZAMASU_ARMOR, MainItems.PATTERN_FUSION_ZAMASU.get());
		buildArmorNoHelmetSet("hit", MainItems.HIT_ARMOR, MainItems.PATTERN_HIT.get());
		buildArmorNoHelmetSet("gas", MainItems.GAS_ARMOR, MainItems.PATTERN_GAS.get());
		buildArmorNoHelmetSet("majin_buu", MainItems.MAJIN_BUU_ARMOR, MainItems.PATTERN_MAJIN_BUU.get());
		buildArmorNoHelmetSet("gamma1", MainItems.GAMMA1_ARMOR, MainItems.PATTERN_GAMMA1.get());
		buildArmorNoHelmetSet("gamma2", MainItems.GAMMA2_ARMOR, MainItems.PATTERN_GAMMA2.get());
	}

	protected void buildFullArmorSet(String name, Map<ArmorItem.Type, RegistryObject<Item>> armorSet, Item pattern) {
		buildHelmetRecipes(name, armorSet.get(ArmorItem.Type.HELMET).get(), pattern);
		buildChestplateRecipes(name, armorSet.get(ArmorItem.Type.CHESTPLATE).get(), pattern);
		buildLeggingsRecipes(name, armorSet.get(ArmorItem.Type.LEGGINGS).get(), pattern);
		buildBootsRecipes(name, armorSet.get(ArmorItem.Type.BOOTS).get(), pattern);
	}

	protected void buildArmorNoHelmetSet(String name, Map<ArmorItem.Type, RegistryObject<Item>> armorSet, Item pattern) {
		buildChestplateRecipes(name, armorSet.get(ArmorItem.Type.CHESTPLATE).get(), pattern);
		buildLeggingsRecipes(name, armorSet.get(ArmorItem.Type.LEGGINGS).get(), pattern);
		buildBootsRecipes(name, armorSet.get(ArmorItem.Type.BOOTS).get(), pattern);
	}

	protected void buildFullToolSet(String name, Item pickaxe, Item axe, Item sword, Item shovel, Item hoe, Item scythe, Item pattern, Item material, Item stick) {
		buildPickaxeRecipes(name, pickaxe, pattern, material, stick);
		buildAxeRecipes(name, axe, pattern, material, stick);
		buildSwordRecipes(name, sword, pattern, material, stick);
		buildShovelRecipes(name, shovel, pattern, material, stick);
		buildHoeRecipes(name, hoe, pattern, material, stick);
		buildScytheRecipes(name, scythe, pattern, material, stick);
	}

	protected void buildHelmetRecipes(String name, Item output, Item pattern) {
		KikonoRecipeBuilder.kikonize(output)
				.pattern(pattern)
				.template(Items.IRON_HELMET)
				.input(MainItems.KIKONO_CLOTH.get())
				.input(MainItems.KIKONO_STRING.get())
				.input(MainItems.KIKONO_CLOTH.get())
				.input(MainItems.KIKONO_STRING.get())
				.input(Items.AIR)
				.input(MainItems.KIKONO_STRING.get())
				.input(Items.AIR)
				.input(Items.AIR)
				.input(Items.AIR)
				.time(200)
				.energy(1000)
				.save(this.consumer, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name + "_armor_helmet"));
	}

	protected void buildCapeRecipes(String name, Item output, Item pattern) {
		KikonoRecipeBuilder.kikonize(output)
				.pattern(pattern)
				.template(Items.IRON_CHESTPLATE)
				.input(MainItems.KIKONO_CLOTH.get())
				.input(MainItems.KIKONO_CLOTH.get())
				.input(MainItems.KIKONO_CLOTH.get())
				.input(MainItems.KIKONO_STRING.get())
				.input(MainItems.KIKONO_STRING.get())
				.input(MainItems.KIKONO_STRING.get())
				.input(MainItems.KIKONO_CLOTH.get())
				.input(MainItems.KIKONO_CLOTH.get())
				.input(MainItems.KIKONO_CLOTH.get())
				.time(200)
				.energy(1000)
				.save(this.consumer, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name + "_armor_cape"));
	}

	protected void buildChestplateRecipes(String name, Item output, Item pattern) {
		KikonoRecipeBuilder.kikonize(output)
				.pattern(pattern)
				.template(Items.IRON_CHESTPLATE)
				.input(MainItems.KIKONO_CLOTH.get())
				.input(Items.AIR)
				.input(MainItems.KIKONO_CLOTH.get())
				.input(MainItems.KIKONO_STRING.get())
				.input(MainItems.KIKONO_STRING.get())
				.input(MainItems.KIKONO_STRING.get())
				.input(MainItems.KIKONO_CLOTH.get())
				.input(MainItems.KIKONO_CLOTH.get())
				.input(MainItems.KIKONO_CLOTH.get())
				.time(200)
				.energy(1000)
				.save(this.consumer, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name + "_armor_chestplate"));
	}

	protected void buildLeggingsRecipes(String name, Item output, Item pattern) {
		KikonoRecipeBuilder.kikonize(output)
				.pattern(pattern)
				.template(Items.IRON_LEGGINGS)
				.input(MainItems.KIKONO_CLOTH.get())
				.input(MainItems.KIKONO_CLOTH.get())
				.input(MainItems.KIKONO_CLOTH.get())
				.input(MainItems.KIKONO_CLOTH.get())
				.input(Items.AIR)
				.input(MainItems.KIKONO_CLOTH.get())
				.input(MainItems.KIKONO_STRING.get())
				.input(Items.AIR)
				.input(MainItems.KIKONO_STRING.get())
				.time(200)
				.energy(1000)
				.save(this.consumer, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name + "_armor_leggings"));
	}

	protected void buildBootsRecipes(String name, Item output, Item pattern) {
		KikonoRecipeBuilder.kikonize(output)
				.pattern(pattern)
				.template(Items.IRON_BOOTS)
				.input(Items.AIR)
				.input(Items.AIR)
				.input(Items.AIR)
				.input(MainItems.KIKONO_CLOTH.get())
				.input(Items.AIR)
				.input(MainItems.KIKONO_CLOTH.get())
				.input(MainItems.KIKONO_STRING.get())
				.input(Items.AIR)
				.input(MainItems.KIKONO_STRING.get())
				.time(200)
				.energy(1000)
				.save(this.consumer, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name + "_armor_boots"));
	}

	protected void buildPickaxeRecipes(String name, Item output, Item pattern, Item material, Item stick) {
		KikonoRecipeBuilder.kikonize(output)
				.pattern(pattern)
				.template(Items.IRON_PICKAXE)
				.input(material)
				.input(material)
				.input(material)
				.input(Items.AIR)
				.input(stick)
				.input(Items.AIR)
				.input(Items.AIR)
				.input(stick)
				.input(Items.AIR)
				.time(100)
				.energy(1000)
				.save(this.consumer, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name + "_pickaxe"));
	}

	protected void buildAxeRecipes(String name, Item output, Item pattern, Item material, Item stick) {
		KikonoRecipeBuilder.kikonize(output)
				.pattern(pattern)
				.template(Items.IRON_AXE)
				.input(material)
				.input(material)
				.input(Items.AIR)
				.input(material)
				.input(stick)
				.input(Items.AIR)
				.input(Items.AIR)
				.input(stick)
				.input(Items.AIR)
				.time(100)
				.energy(1000)
				.save(this.consumer, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name + "_axe"));
	}

	protected void buildSwordRecipes(String name, Item output, Item pattern, Item material, Item stick) {
		KikonoRecipeBuilder.kikonize(output)
				.pattern(pattern)
				.template(Items.IRON_SWORD)
				.input(Items.AIR)
				.input(material)
				.input(Items.AIR)
				.input(Items.AIR)
				.input(material)
				.input(Items.AIR)
				.input(Items.AIR)
				.input(stick)
				.input(Items.AIR)
				.time(100)
				.energy(1000)
				.save(this.consumer, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name + "_sword"));
	}

	protected void buildShovelRecipes(String name, Item output, Item pattern, Item material, Item stick) {
		KikonoRecipeBuilder.kikonize(output)
				.pattern(pattern)
				.template(Items.IRON_SHOVEL)
				.input(Items.AIR)
				.input(material)
				.input(Items.AIR)
				.input(Items.AIR)
				.input(stick)
				.input(Items.AIR)
				.input(Items.AIR)
				.input(stick)
				.input(Items.AIR)
				.time(100)
				.energy(1000)
				.save(this.consumer, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name + "_shovel"));
	}

	protected void buildHoeRecipes(String name, Item output, Item pattern, Item material, Item stick) {
		KikonoRecipeBuilder.kikonize(output)
				.pattern(pattern)
				.template(Items.IRON_HOE)
				.input(material)
				.input(material)
				.input(Items.AIR)
				.input(Items.AIR)
				.input(stick)
				.input(Items.AIR)
				.input(Items.AIR)
				.input(stick)
				.input(Items.AIR)
				.time(100)
				.energy(1000)
				.save(this.consumer, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name + "_hoe"));
	}

	protected void buildScytheRecipes(String name, Item output, Item pattern, Item material, Item stick) {
		KikonoRecipeBuilder.kikonize(output)
				.pattern(pattern)
				.template(Items.IRON_SWORD)
				.input(Items.AIR)
				.input(material)
				.input(material)
				.input(Items.AIR)
				.input(stick)
				.input(material)
				.input(stick)
				.input(Items.AIR)
				.input(Items.AIR)
				.time(100)
				.energy(1000)
				.save(this.consumer, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name + "_scythe"));
	}
}
