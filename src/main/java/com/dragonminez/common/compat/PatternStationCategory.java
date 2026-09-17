package com.dragonminez.common.compat;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.server.recipes.PatternRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class PatternStationCategory implements IRecipeCategory<PatternRecipe> {
	public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "pattern_crafting");
	public static final RecipeType<PatternRecipe> TYPE = new RecipeType<>(UID, PatternRecipe.class);

	private static final int GRID = PatternRecipe.MAX_SIZE * 18;
	private static final int ARROW_X = GRID + 8;
	private static final int OUTPUT_X = ARROW_X + 30;

	private final IDrawable background;
	private final IDrawable icon;
	private final IDrawableStatic slot;
	private final IDrawableStatic arrow;

	public PatternStationCategory(IGuiHelper helper) {
		this.background = helper.createBlankDrawable(OUTPUT_X + 26, GRID);
		this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(MainBlocks.PATTERN_STATION.get()));
		this.slot = helper.getSlotDrawable();
		this.arrow = helper.getRecipeArrow();
	}

	@Override
	public RecipeType<PatternRecipe> getRecipeType() {
		return TYPE;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("block.dragonminez.pattern_station");
	}

	@Override
	public IDrawable getBackground() {
		return this.background;
	}

	@Override
	public IDrawable getIcon() {
		return this.icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, PatternRecipe recipe, IFocusGroup focuses) {
		int offsetX = (PatternRecipe.MAX_SIZE - recipe.getWidth()) / 2;
		int offsetY = (PatternRecipe.MAX_SIZE - recipe.getHeight()) / 2;

		for (int row = 0; row < PatternRecipe.MAX_SIZE; row++) {
			for (int col = 0; col < PatternRecipe.MAX_SIZE; col++) {
				int recipeCol = col - offsetX;
				int recipeRow = row - offsetY;
				var slotBuilder = builder.addSlot(RecipeIngredientRole.INPUT, col * 18 + 1, row * 18 + 1).setBackground(this.slot, -1, -1);
				if (recipeCol >= 0 && recipeRow >= 0 && recipeCol < recipe.getWidth() && recipeRow < recipe.getHeight()) {
					Ingredient ingredient = recipe.getIngredients().get(recipeCol + recipeRow * recipe.getWidth());
					slotBuilder.addIngredients(ingredient);
				}
			}
		}

		builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 5, GRID / 2 - 8)
				.setBackground(this.slot, -1, -1)
				.addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
	}

	@Override
	public void draw(PatternRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
		this.arrow.draw(guiGraphics, ARROW_X, GRID / 2 - 8);
	}
}
