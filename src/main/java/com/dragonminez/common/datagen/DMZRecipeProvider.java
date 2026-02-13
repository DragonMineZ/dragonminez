package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class DMZRecipeProvider extends RecipeProvider implements IConditionBuilder {
	public DMZRecipeProvider(PackOutput pOutput) {
		super(pOutput);
	}

	@Override
	protected void buildRecipes(@NotNull Consumer<FinishedRecipe> pWriter) {
		oreBlasting(pWriter, Gete, RecipeCategory.MISC, MainItems.GETE_SCRAP.get(), 3.5f, 100, "gete");
		oreSmelting(pWriter, Gete, RecipeCategory.MISC, MainItems.GETE_SCRAP.get(), 3.5f, 200, "gete");
		oreBlasting(pWriter, Kikono, RecipeCategory.MISC, MainItems.KIKONO_SHARD.get(), 2.5f, 100, "kikono");
		oreSmelting(pWriter, Kikono, RecipeCategory.MISC, MainItems.KIKONO_SHARD.get(), 2.5f, 200, "kikono");
		oreBlasting(pWriter, Diamantes, RecipeCategory.MISC, Items.DIAMOND, 1.0f, 100, "diamond");
		oreSmelting(pWriter, Diamantes, RecipeCategory.MISC, Items.DIAMOND, 1.0f, 200, "diamond");
		oreBlasting(pWriter, Esmeraldas, RecipeCategory.MISC, Items.EMERALD, 1f, 100, "emerald");
		oreSmelting(pWriter, Esmeraldas, RecipeCategory.MISC, Items.EMERALD, 1f, 200, "emerald");
		oreBlasting(pWriter, Lapis, RecipeCategory.MISC, Items.LAPIS_LAZULI, 0.2f, 100, "lapis_lazuli");
		oreSmelting(pWriter, Lapis, RecipeCategory.MISC, Items.LAPIS_LAZULI, 0.2f, 200, "lapis_lazuli");
		oreBlasting(pWriter, Redstone, RecipeCategory.MISC, Items.REDSTONE, 0.7f, 100, "redstone");
		oreSmelting(pWriter, Redstone, RecipeCategory.MISC, Items.REDSTONE, 0.7f, 200, "redstone");
		oreBlasting(pWriter, Hierro, RecipeCategory.MISC, Items.IRON_INGOT, 0.7f, 100, "iron_ingot");
		oreSmelting(pWriter, Hierro, RecipeCategory.MISC, Items.IRON_INGOT, 0.7f, 200, "iron_ingot");
		oreBlasting(pWriter, Oro, RecipeCategory.MISC, Items.GOLD_INGOT, 1.0f, 100, "gold_ingot");
		oreSmelting(pWriter, Oro, RecipeCategory.MISC, Items.GOLD_INGOT, 1.0f, 200, "gold_ingot");
		oreBlasting(pWriter, Cobre, RecipeCategory.MISC, Items.COPPER_INGOT, 0.7f, 100, "copper_ingot");
		oreSmelting(pWriter, Cobre, RecipeCategory.MISC, Items.COPPER_INGOT, 0.7f, 200, "copper_ingot");
		oreBlasting(pWriter, Carbon, RecipeCategory.MISC, Items.COAL, 0.1f, 100, "coal");
		oreSmelting(pWriter, Carbon, RecipeCategory.MISC, Items.COAL, 0.1f, 200, "coal");

		SimpleCookingRecipeBuilder.smelting(Ingredient.of(MainItems.FROG_LEGS_RAW.get()),
						RecipeCategory.FOOD, MainItems.FROG_LEGS_COOKED.get(), 0.35f, 200)
				.unlockedBy(getHasName(MainItems.FROG_LEGS_RAW.get()), has(MainItems.FROG_LEGS_RAW.get())).group(Reference.MOD_ID)
				.save(pWriter, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "frog_legs_cooked"));
		SimpleCookingRecipeBuilder.smoking(Ingredient.of(MainItems.FROG_LEGS_RAW.get()),
						RecipeCategory.FOOD, MainItems.FROG_LEGS_COOKED.get(), 0.35f, 100)
				.unlockedBy(getHasName(MainItems.FROG_LEGS_RAW.get()), has(MainItems.FROG_LEGS_RAW.get())).group(Reference.MOD_ID)
				.save(pWriter, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "frog_legs_cooked_smoking"));
		SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(MainItems.FROG_LEGS_RAW.get()),
						RecipeCategory.FOOD, MainItems.FROG_LEGS_COOKED.get(), 0.35f, 600)
				.unlockedBy(getHasName(MainItems.FROG_LEGS_RAW.get()), has(MainItems.FROG_LEGS_RAW.get())).group(Reference.MOD_ID)
				.save(pWriter, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "frog_legs_cooked_campfire"));

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.NAMEK_AJISSA_PRESSURE_PLATE.get(), 1)
				.pattern("##")
				.define('#', MainBlocks.NAMEK_AJISSA_PLANKS.get())
				.unlockedBy(getHasName(MainBlocks.NAMEK_AJISSA_PLANKS.get()), has(MainBlocks.NAMEK_AJISSA_PLANKS.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.NAMEK_SACRED_PRESSURE_PLATE.get(), 1)
				.pattern("##")
				.define('#', MainBlocks.NAMEK_SACRED_PLANKS.get())
				.unlockedBy(getHasName(MainBlocks.NAMEK_SACRED_PLANKS.get()), has(MainBlocks.NAMEK_SACRED_PLANKS.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.NAMEK_STONE_SLAB.get(), 6)
				.pattern("###")
				.define('#', MainBlocks.NAMEK_STONE.get())
				.unlockedBy(getHasName(MainBlocks.NAMEK_STONE.get()), has(MainBlocks.NAMEK_STONE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.NAMEK_STONE_STAIRS.get(), 4)
				.pattern("#  ")
				.pattern("## ")
				.pattern("###")
				.define('#', MainBlocks.NAMEK_STONE.get())
				.unlockedBy(getHasName(MainBlocks.NAMEK_STONE.get()), has(MainBlocks.NAMEK_STONE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.NAMEK_STONE_WALL.get(), 6)
				.pattern("###")
				.pattern("###")
				.define('#', MainBlocks.NAMEK_STONE.get())
				.unlockedBy(getHasName(MainBlocks.NAMEK_STONE.get()), has(MainBlocks.NAMEK_STONE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.NAMEK_COBBLESTONE_SLAB.get(), 4)
				.pattern("###")
				.define('#', MainBlocks.NAMEK_COBBLESTONE.get())
				.unlockedBy(getHasName(MainBlocks.NAMEK_COBBLESTONE.get()), has(MainBlocks.NAMEK_COBBLESTONE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.NAMEK_COBBLESTONE_STAIRS.get(), 4)
				.pattern("#  ")
				.pattern("## ")
				.pattern("###")
				.define('#', MainBlocks.NAMEK_COBBLESTONE.get())
				.unlockedBy(getHasName(MainBlocks.NAMEK_COBBLESTONE.get()), has(MainBlocks.NAMEK_COBBLESTONE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.NAMEK_COBBLESTONE_WALL.get(), 6)
				.pattern("###")
				.pattern("###")
				.define('#', MainBlocks.NAMEK_COBBLESTONE.get())
				.unlockedBy(getHasName(MainBlocks.NAMEK_COBBLESTONE.get()), has(MainBlocks.NAMEK_COBBLESTONE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.NAMEK_DEEPSLATE_SLAB.get(), 6)
				.pattern("###")
				.define('#', MainBlocks.NAMEK_DEEPSLATE.get())
				.unlockedBy(getHasName(MainBlocks.NAMEK_DEEPSLATE.get()), has(MainBlocks.NAMEK_DEEPSLATE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.NAMEK_DEEPSLATE_STAIRS.get(), 4)
				.pattern("#  ")
				.pattern("## ")
				.pattern("###")
				.define('#', MainBlocks.NAMEK_DEEPSLATE.get())
				.unlockedBy(getHasName(MainBlocks.NAMEK_DEEPSLATE.get()), has(MainBlocks.NAMEK_DEEPSLATE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.NAMEK_DEEPSLATE_WALL.get(), 6)
				.pattern("###")
				.pattern("###")
				.define('#', MainBlocks.NAMEK_DEEPSLATE.get())
				.unlockedBy(getHasName(MainBlocks.NAMEK_DEEPSLATE.get()), has(MainBlocks.NAMEK_DEEPSLATE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.ROCKY_STONE_SLAB.get(), 6)
				.pattern("###")
				.define('#', MainBlocks.ROCKY_STONE.get())
				.unlockedBy(getHasName(MainBlocks.ROCKY_STONE.get()), has(MainBlocks.ROCKY_STONE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.ROCKY_STONE_STAIRS.get(), 4)
				.pattern("#  ")
				.pattern("## ")
				.pattern("###")
				.define('#', MainBlocks.ROCKY_STONE.get())
				.unlockedBy(getHasName(MainBlocks.ROCKY_STONE.get()), has(MainBlocks.ROCKY_STONE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.ROCKY_STONE_WALL.get(), 6)
				.pattern("###")
				.pattern("###")
				.define('#', MainBlocks.ROCKY_STONE.get())
				.unlockedBy(getHasName(MainBlocks.ROCKY_STONE.get()), has(MainBlocks.ROCKY_STONE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.ROCKY_COBBLESTONE_SLAB.get(), 6)
				.pattern("###")
				.define('#', MainBlocks.ROCKY_COBBLESTONE.get())
				.unlockedBy(getHasName(MainBlocks.ROCKY_COBBLESTONE.get()), has(MainBlocks.ROCKY_COBBLESTONE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.ROCKY_COBBLESTONE_STAIRS.get(), 4)
				.pattern("#  ")
				.pattern("## ")
				.pattern("###")
				.define('#', MainBlocks.ROCKY_COBBLESTONE.get())
				.unlockedBy(getHasName(MainBlocks.ROCKY_COBBLESTONE.get()), has(MainBlocks.ROCKY_COBBLESTONE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.ROCKY_COBBLESTONE_WALL.get(), 6)
				.pattern("###")
				.pattern("###")
				.define('#', MainBlocks.ROCKY_COBBLESTONE.get())
				.unlockedBy(getHasName(MainBlocks.ROCKY_COBBLESTONE.get()), has(MainBlocks.ROCKY_COBBLESTONE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, MainItems.RADAR_PIECE.get(), 1)
				.pattern("SIS")
				.pattern("IRI")
				.pattern("SIS")
				.define('S', Items.STRING)
				.define('I', Items.IRON_INGOT)
				.define('R', Items.REDSTONE)
				.unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, MainItems.T1_RADAR_CHIP.get(), 1)
				.pattern("RBR")
				.pattern("GPG")
				.pattern("RBR")
				.define('R', Items.REDSTONE)
				.define('G', Items.GREEN_CONCRETE)
				.define('P', MainItems.RADAR_PIECE.get())
				.define('B', Items.REPEATER)
				.unlockedBy(getHasName(MainItems.RADAR_PIECE.get()), has(MainItems.RADAR_PIECE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, MainItems.T1_RADAR_CPU.get(), 1)
				.pattern("RBR")
				.pattern("CPC")
				.pattern("ROR")
				.define('R', Items.COMPARATOR)
				.define('O', Items.OBSERVER)
				.define('C', MainItems.T1_RADAR_CHIP.get())
				.define('B', Items.REPEATER)
				.define('P', MainItems.RADAR_PIECE.get())
				.unlockedBy(getHasName(MainItems.T1_RADAR_CHIP.get()), has(MainItems.T1_RADAR_CHIP.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, MainItems.DBALL_RADAR_ITEM.get(), 1)
				.pattern("OPO")
				.pattern("PGP")
				.pattern("CPC")
				.define('O', Items.OBSERVER)
				.define('G', MainItems.T1_RADAR_CPU.get())
				.define('C', MainItems.T1_RADAR_CHIP.get())
				.define('P', MainItems.RADAR_PIECE.get())
				.unlockedBy(getHasName(MainItems.T1_RADAR_CPU.get()), has(MainItems.T1_RADAR_CPU.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, MainItems.T2_RADAR_CHIP.get(), 1)
				.pattern("ROR")
				.pattern("BPB")
				.pattern("TTT")
				.define('O', Items.OBSERVER)
				.define('R', Items.REDSTONE_TORCH)
				.define('B', Items.BLUE_CONCRETE)
				.define('P', MainItems.RADAR_PIECE.get())
				.define('T', MainItems.T1_RADAR_CPU.get())
				.unlockedBy(getHasName(MainItems.RADAR_PIECE.get()), has(MainItems.RADAR_PIECE.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, MainItems.NAMEKDBALL_RADAR_ITEM.get(), 1)
				.pattern("OCO")
				.pattern("PGP")
				.pattern("CPC")
				.define('O', Items.OBSERVER)
				.define('G', MainItems.T2_RADAR_CPU.get())
				.define('C', MainItems.T2_RADAR_CHIP.get())
				.define('P', MainItems.RADAR_PIECE.get())
				.unlockedBy(getHasName(MainItems.T2_RADAR_CHIP.get()), has(MainItems.T2_RADAR_CHIP.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, MainItems.KATANA_YAJIROBE.get(), 1)
				.pattern("  I")
				.pattern("GI ")
				.pattern("SG ")
				.define('I', Items.IRON_INGOT)
				.define('G', Items.GOLD_INGOT)
				.define('S', Items.STICK)
				.unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, MainItems.RED_SCOUTER.get(), 1)
				.pattern("  R")
				.pattern("PPT")
				.pattern(" B ")
				.define('R', MainItems.RADAR_PIECE.get())
				.define('P', Items.RED_STAINED_GLASS_PANE)
				.define('T', MainItems.T1_RADAR_CPU.get())
				.define('B', Items.STONE_BUTTON)
				.unlockedBy(getHasName(MainItems.T1_RADAR_CPU.get()), has(MainItems.T1_RADAR_CPU.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, MainItems.BLUE_SCOUTER.get(), 1)
				.pattern("  R")
				.pattern("PPT")
				.pattern(" B ")
				.define('R', MainItems.RADAR_PIECE.get())
				.define('P', Items.BLUE_STAINED_GLASS_PANE)
				.define('T', MainItems.T1_RADAR_CPU.get())
				.define('B', Items.STONE_BUTTON)
				.unlockedBy(getHasName(MainItems.T1_RADAR_CPU.get()), has(MainItems.T1_RADAR_CPU.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, MainItems.GREEN_SCOUTER.get(), 1)
				.pattern("  R")
				.pattern("PPT")
				.pattern(" B ")
				.define('R', MainItems.RADAR_PIECE.get())
				.define('P', Items.GREEN_STAINED_GLASS_PANE)
				.define('T', MainItems.T1_RADAR_CPU.get())
				.define('B', Items.STONE_BUTTON)
				.unlockedBy(getHasName(MainItems.T1_RADAR_CPU.get()), has(MainItems.T1_RADAR_CPU.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, MainItems.PURPLE_SCOUTER.get(), 1)
				.pattern("  R")
				.pattern("PPT")
				.pattern(" B ")
				.define('R', MainItems.RADAR_PIECE.get())
				.define('P', Items.PURPLE_STAINED_GLASS_PANE)
				.define('T', MainItems.T1_RADAR_CPU.get())
				.define('B', Items.STONE_BUTTON)
				.unlockedBy(getHasName(MainItems.T1_RADAR_CPU.get()), has(MainItems.T1_RADAR_CPU.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainBlocks.KIKONO_STATION.get(), 1)
				.pattern("ACA")
				.pattern("KTK")
				.pattern("PSP")
				.define('A', Items.ANVIL)
				.define('C', Items.DIAMOND_CHESTPLATE)
				.define('K', MainBlocks.KIKONO_BLOCK.get())
				.define('T', MainItems.T2_RADAR_CPU.get())
				.define('P', Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
				.define('S', Items.SMITHING_TABLE)
				.unlockedBy(getHasName(MainItems.T2_RADAR_CPU.get()), has(MainItems.T2_RADAR_CPU.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainBlocks.FUEL_GENERATOR.get(), 1)
				.pattern("OOO")
				.pattern("OTO")
				.pattern("OOO")
				.define('O', Items.CRYING_OBSIDIAN)
				.define('T', MainItems.T2_RADAR_CPU.get())
				.unlockedBy(getHasName(Items.CRYING_OBSIDIAN), has(Items.CRYING_OBSIDIAN))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, MainBlocks.ENERGY_CABLE.get(), 4)
				.pattern(" W ")
				.pattern("WCW")
				.pattern(" W ")
				.define('W', Items.BLACK_WOOL)
				.define('C', MainItems.T1_RADAR_CHIP.get())
				.unlockedBy(getHasName(MainItems.T1_RADAR_CHIP.get()), has(MainItems.T1_RADAR_CHIP.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, MainItems.KIKONO_SHARD.get(), 9)
				.requires(MainBlocks.KIKONO_BLOCK.get())
				.unlockedBy(getHasName(MainBlocks.KIKONO_BLOCK.get()), has(MainBlocks.KIKONO_BLOCK.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.KIKONO_BLOCK.get(), 1)
				.pattern("KKK")
				.pattern("KKK")
				.pattern("KKK")
				.define('K', MainItems.KIKONO_SHARD.get())
				.unlockedBy(getHasName(MainItems.KIKONO_SHARD.get()), has(MainItems.KIKONO_SHARD.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, MainItems.ARMOR_CRAFTING_KIT.get(), 1)
				.requires(Items.RED_WOOL).requires(Items.SHEARS)
				.unlockedBy(getHasName(Items.SHEARS), has(Items.SHEARS))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, MainItems.KIKONO_STICK.get(), 2)
				.pattern("K")
				.pattern("K")
				.define('K', MainItems.KIKONO_SHARD.get())
				.unlockedBy(getHasName(MainItems.KIKONO_SHARD.get()), has(MainItems.KIKONO_SHARD.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, MainItems.KIKONO_STRING.get(), 2)
				.requires(MainItems.KIKONO_SHARD.get())
				.requires(MainItems.ARMOR_CRAFTING_KIT.get())
				.unlockedBy(getHasName(MainItems.KIKONO_SHARD.get()), has(MainItems.KIKONO_SHARD.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, MainItems.KIKONO_CLOTH.get(), 1)
				.requires(MainItems.KIKONO_STRING.get(), 4)
				.requires(MainItems.ARMOR_CRAFTING_KIT.get())
				.unlockedBy(getHasName(MainItems.KIKONO_STRING.get()), has(MainItems.KIKONO_STRING.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.BLANK_PATTERN_Z.get(), 1)
				.pattern("RRR")
				.pattern("#W#")
				.pattern("RRR")
				.define('#', Items.PAPER)
				.define('W', Items.WHITE_WOOL)
				.define('R', Items.RED_WOOL)
				.unlockedBy(getHasName(Items.PAPER), has(Items.PAPER))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.BLANK_PATTERN_SUPER.get(), 1)
				.pattern("CPC")
				.pattern("#W#")
				.pattern("CPC")
				.define('#', Items.PAPER)
				.define('W', Items.WHITE_WOOL)
				.define('P', MainItems.BLANK_PATTERN_Z.get())
				.define('C', Items.CYAN_WOOL)
				.unlockedBy(getHasName(Items.PAPER), has(Items.PAPER))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_GOKU_KID.get(), 1)
				.pattern("R#R")
				.pattern("R R")
				.pattern("RRR")
				.define('R', Items.RED_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_GOKU1.get(), 1)
				.pattern("B#B")
				.pattern("OBO")
				.pattern("OOO")
				.define('B', Items.BLUE_DYE)
				.define('O', Items.ORANGE_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_GOKU_SUPER.get(), 1)
				.pattern("C#C")
				.pattern("OCO")
				.pattern("OWO")
				.define('C', Items.CYAN_DYE)
				.define('O', Items.ORANGE_DYE)
				.define('W', Items.WHITE_DYE)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_GOKU_GT.get(), 1)
				.pattern("L#L")
				.pattern("LLL")
				.pattern("YWY")
				.define('L', Items.LIGHT_BLUE_DYE)
				.define('Y', Items.YELLOW_DYE)
				.define('W', Items.WHITE_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_YARDRAT.get(), 1)
				.pattern("W#P")
				.pattern("WBW")
				.pattern("BYB")
				.define('W', Items.WHITE_DYE)
				.define('P', Items.PINK_DYE)
				.define('B', Items.BLUE_DYE)
				.define('Y', Items.YELLOW_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_GOTEN.get(), 1)
				.pattern("B#B")
				.pattern("OBO")
				.pattern("ONO")
				.define('B', Items.BLUE_DYE)
				.define('O', Items.ORANGE_DYE)
				.define('N', Items.BLACK_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(Items.BLUE_DYE), has(Items.BLUE_DYE))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_GOTEN_SUPER.get(), 1)
				.pattern("G#G")
				.pattern("GGG")
				.pattern("BBB")
				.define('G', Items.GREEN_DYE)
				.define('B', Items.BLUE_DYE)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_GOHAN_SUPER.get(), 1)
				.pattern("P#P")
				.pattern("PPP")
				.pattern("RRR")
				.define('P', Items.PURPLE_DYE)
				.define('R', Items.RED_DYE)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_GREAT_SAIYAMAN.get(), 1)
				.pattern("R#R")
				.pattern("GGG")
				.pattern("BYB")
				.define('R', Items.RED_DYE)
				.define('G', Items.GREEN_DYE)
				.define('B', Items.BLACK_DYE)
				.define('Y', Items.YELLOW_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_FUTURE_GOHAN.get(), 1)
				.pattern("B#B")
				.pattern("OBO")
				.pattern("ORO")
				.define('B', Items.BLUE_DYE)
				.define('O', Items.ORANGE_DYE)
				.define('R', Items.CLOCK)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_VEGETA1.get(), 1)
				.pattern("Y#Y")
				.pattern("BWB")
				.pattern("BYB")
				.define('Y', Items.YELLOW_DYE)
				.define('B', Items.BLUE_DYE)
				.define('W', Items.WHITE_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(Items.BLUE_DYE), has(Items.BLUE_DYE))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_VEGETA2.get(), 1)
				.pattern("B#B")
				.pattern("BWB")
				.pattern("BYB")
				.define('B', Items.BLUE_DYE)
				.define('W', Items.WHITE_DYE)
				.define('Y', Items.YELLOW_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(Items.BLUE_DYE), has(Items.BLUE_DYE))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_VEGETA_Z.get(), 1)
				.pattern("Y#Y")
				.pattern("BWB")
				.pattern("BYB")
				.define('B', Items.BLUE_DYE)
				.define('W', Items.WHITE_DYE)
				.define('Y', Items.YELLOW_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_VEGETA_BUU.get(), 1)
				.pattern("C#C")
				.pattern("BCB")
				.pattern("BBB")
				.define('B', Items.BLUE_DYE)
				.define('C', Items.CYAN_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_VEGETA_SUPER.get(), 1)
				.pattern("Y#Y")
				.pattern("CWC")
				.pattern("CYC")
				.define('C', Items.CYAN_DYE)
				.define('W', Items.WHITE_DYE)
				.define('Y', Items.YELLOW_DYE)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_VEGETTO.get(), 1)
				.pattern("B#B")
				.pattern("BOB")
				.pattern("WCW")
				.define('B', Items.BLUE_DYE)
				.define('W', Items.WHITE_DYE)
				.define('O', Items.ORANGE_DYE)
				.define('C', Items.CYAN_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_GOGETA.get(), 1)
				.pattern("Y#Y")
				.pattern("B B")
				.pattern("CCC")
				.define('Y', Items.YELLOW_DYE)
				.define('B', Items.BLACK_DYE)
				.define('C', Items.CYAN_DYE)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_PICCOLO.get(), 1)
				.pattern("P#P")
				.pattern("PPP")
				.pattern("RRR")
				.define('P', Items.PURPLE_DYE)
				.define('R', Items.RED_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_GOHAN1.get(), 1)
				.pattern("P#P")
				.pattern("PPP")
				.pattern("BBB")
				.define('P', Items.PURPLE_DYE)
				.define('B', Items.BLUE_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(Items.BLUE_DYE), has(Items.BLUE_DYE))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_BARDOCK1.get(), 1)
				.pattern("G#G")
				.pattern("BBB")
				.pattern("RGR")
				.define('B', Items.BLACK_DYE)
				.define('G', Items.GREEN_DYE)
				.define('R', Items.RED_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_BARDOCK2.get(), 1)
				.pattern("Y#Y")
				.pattern("BBB")
				.pattern("CYC")
				.define('Y', Items.YELLOW_DYE)
				.define('B', Items.BLACK_DYE)
				.define('C', Items.CYAN_DYE)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_TURLES.get(), 1)
				.pattern("B#B")
				.pattern("NNN")
				.pattern("BBB")
				.define('B', Items.BLUE_DYE)
				.define('N', Items.BLACK_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_TIEN.get(), 1)
				.pattern(" #G")
				.pattern("GGG")
				.pattern("RRR")
				.define('G', Items.GREEN_DYE)
				.define('R', Items.RED_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_TRUNKS_Z.get(), 1)
				.pattern("B#B")
				.pattern("NSN")
				.pattern("NYN")
				.define('B', Items.BLUE_DYE)
				.define('N', Items.BLACK_DYE)
				.define('Y', Items.YELLOW_DYE)
				.define('S', Items.IRON_SWORD)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_TRUNKS_SUPER.get(), 1)
				.pattern("S#C")
				.pattern("CRC")
				.pattern("BBB")
				.define('B', Items.BLACK_DYE)
				.define('C', Items.CYAN_DYE)
				.define('R', Items.RED_DYE)
				.define('S', Items.IRON_SWORD)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_TRUNKS_KID.get(), 1)
				.pattern("G#G")
				.pattern("GGG")
				.pattern("OOO")
				.define('G', Items.GREEN_DYE)
				.define('O', Items.ORANGE_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_BROLY_Z.get(), 1)
				.pattern("Y#Y")
				.pattern(" I ")
				.pattern("RIR")
				.define('Y', Items.GOLD_NUGGET)
				.define('I', Items.GOLD_INGOT)
				.define('R', Items.RED_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_BROLY_SUPER.get(), 1)
				.pattern("G#G")
				.pattern("BBB")
				.pattern("LPL")
				.define('G', Items.GREEN_DYE)
				.define('B', Items.BLACK_DYE)
				.define('L', Items.LIME_DYE)
				.define('P', Items.PURPLE_DYE)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_SHIN.get(), 1)
				.pattern("C#C")
				.pattern("LRL")
				.pattern("OCO")
				.define('C', Items.CYAN_DYE)
				.define('L', Items.LIGHT_BLUE_DYE)
				.define('R', Items.RED_DYE)
				.define('O', Items.ORANGE_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_BLACK.get(), 1)
				.pattern("B#B")
				.pattern("GBG")
				.pattern("RGR")
				.define('B', Items.BLACK_DYE)
				.define('G', Items.GRAY_DYE)
				.define('R', Items.RED_DYE)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_ZAMASU.get(), 1)
				.pattern("B#B")
				.pattern("PBP")
				.pattern("LLL")
				.define('B', Items.BLACK_DYE)
				.define('P', Items.PURPLE_DYE)
				.define('L', Items.LIGHT_BLUE_DYE)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_FUSION_ZAMASU.get(), 1)
				.pattern("G#G")
				.pattern("BGB")
				.pattern("RRR")
				.define('G', Items.GRAY_DYE)
				.define('B', Items.BLACK_DYE)
				.define('R', Items.RED_DYE)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_PRIDE_TROOPS.get(), 1)
				.pattern("B#B")
				.pattern("RBR")
				.pattern("RBR")
				.define('B', Items.BLACK_DYE)
				.define('R', Items.RED_DYE)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_HIT.get(), 1)
				.pattern("P#P")
				.pattern("CPC")
				.pattern("CYC")
				.define('P', Items.PURPLE_DYE)
				.define('C', Items.CYAN_DYE)
				.define('Y', Items.YELLOW_DYE)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_GAS.get(), 1)
				.pattern("Y#Y")
				.pattern("RYR")
				.pattern("RWR")
				.define('Y', Items.YELLOW_DYE)
				.define('R', Items.RED_DYE)
				.define('W', Items.WHITE_DYE)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_MAJIN_BUU.get(), 1)
				.pattern("B#B")
				.pattern("MMM")
				.pattern("BYB")
				.define('B', Items.BLACK_DYE)
				.define('M', Items.MAGENTA_DYE)
				.define('Y', Items.YELLOW_DYE)
				.define('#', MainItems.BLANK_PATTERN_Z.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_Z.get()), has(MainItems.BLANK_PATTERN_Z.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_GAMMA1.get(), 1)
				.pattern("R#R")
				.pattern("YYY")
				.pattern("BGB")
				.define('R', Items.RED_DYE)
				.define('Y', Items.YELLOW_DYE)
				.define('B', Items.BLACK_DYE)
				.define('G', Items.GRAY_DYE)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, MainItems.PATTERN_GAMMA2.get(), 1)
				.pattern("C#C")
				.pattern("YYY")
				.pattern("BGB")
				.define('C', Items.CYAN_DYE)
				.define('Y', Items.YELLOW_DYE)
				.define('B', Items.BLACK_DYE)
				.define('G', Items.GRAY_DYE)
				.define('#', MainItems.BLANK_PATTERN_SUPER.get())
				.unlockedBy(getHasName(MainItems.BLANK_PATTERN_SUPER.get()), has(MainItems.BLANK_PATTERN_SUPER.get()))
				.group(Reference.MOD_ID).save(pWriter);

//		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, MainBlocks.KIKONO_ARMOR_STATION.get(), 1)
//				.pattern("ACA")
//				.pattern("KLK")
//				.pattern("ISI")
//				.define('A', Items.ANVIL)
//				.define('C', Items.DIAMOND_CHESTPLATE)
//				.define('K', MainBlocks.KIKONO_BLOCK.get())
//				.define('L', Blocks.POLISHED_BLACKSTONE_BRICK_SLAB)
//				.define('S', Items.SMITHING_TABLE)
//				.define('I', Items.IRON_BLOCK)
//				.unlockedBy(getHasName(Items.DIAMOND_CHESTPLATE), has(Items.DIAMOND_CHESTPLATE))
//				.group(Reference.MOD_ID)
//				.save(pWriter);

				new DMZKikonoRecipeProvider(pWriter).generate();

	}

	private static final List<ItemLike> Gete = List.of(MainBlocks.GETE_ORE.get());
	private static final List<ItemLike> Kikono = List.of(MainBlocks.NAMEK_KIKONO_ORE.get());
	private static final List<ItemLike> Diamantes = List.of(MainBlocks.NAMEK_DIAMOND_ORE.get(), MainBlocks.NAMEK_DEEPSLATE_DIAMOND.get());
	private static final List<ItemLike> Esmeraldas = List.of(MainBlocks.NAMEK_EMERALD_ORE.get(), MainBlocks.NAMEK_DEEPSLATE_EMERALD.get());
	private static final List<ItemLike> Lapis = List.of(MainBlocks.NAMEK_LAPIS_ORE.get(), MainBlocks.NAMEK_DEEPSLATE_LAPIS.get());
	private static final List<ItemLike> Redstone = List.of(MainBlocks.NAMEK_REDSTONE_ORE.get(), MainBlocks.NAMEK_DEEPSLATE_REDSTONE.get());
	private static final List<ItemLike> Hierro = List.of(MainBlocks.NAMEK_IRON_ORE.get(), MainBlocks.NAMEK_DEEPSLATE_IRON.get());
	private static final List<ItemLike> Oro = List.of(MainBlocks.NAMEK_GOLD_ORE.get(), MainBlocks.NAMEK_DEEPSLATE_GOLD.get());
	private static final List<ItemLike> Cobre = List.of(MainBlocks.NAMEK_COPPER_ORE.get(), MainBlocks.NAMEK_DEEPSLATE_COPPER.get());
	private static final List<ItemLike> Carbon = List.of(MainBlocks.NAMEK_COAL_ORE.get(), MainBlocks.NAMEK_DEEPSLATE_COAL.get());

	protected static void oreSmelting(@NotNull Consumer<FinishedRecipe> pFinishedRecipeConsumer,
									  List<ItemLike> pIngredients, @NotNull RecipeCategory pCategory,
									  @NotNull ItemLike pResult, float pExperience, int pCookingTIme,
									  @NotNull String pGroup) {
		oreCooking(pFinishedRecipeConsumer, RecipeSerializer.SMELTING_RECIPE, pIngredients, pCategory, pResult,
				pExperience, pCookingTIme, pGroup, "_from_smelting");
	}

	protected static void oreBlasting(@NotNull Consumer<FinishedRecipe> pFinishedRecipeConsumer,
									  List<ItemLike> pIngredients, @NotNull RecipeCategory pCategory,
									  @NotNull ItemLike pResult, float pExperience, int pCookingTime,
									  @NotNull String pGroup) {
		oreCooking(pFinishedRecipeConsumer, RecipeSerializer.BLASTING_RECIPE, pIngredients, pCategory, pResult,
				pExperience, pCookingTime, pGroup, "_from_blasting");
	}

	protected static void oreCooking(@NotNull Consumer<FinishedRecipe> pFinishedRecipeConsumer,
									 @NotNull RecipeSerializer<? extends AbstractCookingRecipe> pCookingSerializer,
									 List<ItemLike> pIngredients, @NotNull RecipeCategory pCategory,
									 @NotNull ItemLike pResult, float pExperience, int pCookingTime,
									 @NotNull String pGroup, String pRecipeName) {
		for (ItemLike itemlike : pIngredients) {
			SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime,
							pCookingSerializer)
					.group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
					.save(pFinishedRecipeConsumer, Reference.MOD_ID + ":" + getItemName(pResult)
							+ pRecipeName + "_" + getItemName(itemlike));
		}
	}
}
