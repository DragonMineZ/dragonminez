package com.dragonminez.common.compat;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.client.init.menu.screens.KikonoStationScreen;
import com.dragonminez.client.init.menu.screens.PatternStationScreen;
import com.dragonminez.common.init.MainMenus;
import com.dragonminez.common.init.menu.menutypes.PatternStationMenu;
import com.dragonminez.server.recipes.PatternRecipe;
import com.dragonminez.server.recipes.KikonoRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class JEIDragonMineZPlugin implements IModPlugin {

	@Override
	public ResourceLocation getPluginUid() {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "jei_plugin");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new KikonoStationCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new PatternStationCategory(registration.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

		List<KikonoRecipe> recipes = new ArrayList<>();

		for (Recipe<?> recipe : recipeManager.getRecipes()) {
			if (recipe instanceof KikonoRecipe) {
				recipes.add((KikonoRecipe) recipe);
			}
		}

		if (!recipes.isEmpty()) {
			registration.addRecipes(KikonoStationCategory.TYPE, recipes);
		}

		List<PatternRecipe> patternRecipes = new ArrayList<>();
		for (Recipe<?> recipe : recipeManager.getRecipes()) {
			if (recipe instanceof PatternRecipe patternRecipe) {
				patternRecipes.add(patternRecipe);
			}
		}

		if (!patternRecipes.isEmpty()) {
			registration.addRecipes(PatternStationCategory.TYPE, patternRecipes);
		}
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(MainBlocks.KIKONO_STATION.get()), KikonoStationCategory.TYPE);
		registration.addRecipeCatalyst(new ItemStack(MainBlocks.PATTERN_STATION.get()), PatternStationCategory.TYPE);
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addRecipeClickArea(KikonoStationScreen.class, 111, 35, 26, 17, KikonoStationCategory.TYPE);
		registration.addRecipeClickArea(PatternStationScreen.class, PatternStationScreen.ARROW_X, PatternStationScreen.ARROW_Y,
				PatternStationScreen.ARROW_W, PatternStationScreen.ARROW_H, PatternStationCategory.TYPE);
	}

	@Override
	public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
		registration.addRecipeTransferHandler(PatternStationMenu.class, MainMenus.PATTERN_STATION_MENU.get(), PatternStationCategory.TYPE,
				PatternStationMenu.CRAFT_SLOT_START, PatternStationMenu.CRAFT_SLOT_END - PatternStationMenu.CRAFT_SLOT_START,
				PatternStationMenu.INV_SLOT_START, PatternStationMenu.USE_ROW_SLOT_END - PatternStationMenu.INV_SLOT_START);
	}
}