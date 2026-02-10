package com.dragonminez.common.compat;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.server.recipes.KikonoRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;

public class KikonoStationCategory implements IRecipeCategory<KikonoRecipe> {
	public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "kikono_crafting");
	public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/screen/kikono_station_gui.png");

	public static final RecipeType<KikonoRecipe> TYPE = new RecipeType<>(UID, KikonoRecipe.class);

	private final IDrawable background;
	private final IDrawable icon;

	public KikonoStationCategory(IGuiHelper helper) {
		this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 85);
		this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(MainBlocks.KIKONO_STATION.get()));
	}

	@Override
	public RecipeType<KikonoRecipe> getRecipeType() {
		return TYPE;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("block.dragonminez.kikono_station");
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
	public void setRecipe(IRecipeLayoutBuilder builder, KikonoRecipe recipe, IFocusGroup focuses) {
		List<Ingredient> inputs = recipe.getInputs();

		int startX = 28;
		int startY = 17;

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int index = col + row * 3;
				if (index < inputs.size()) {
					builder.addSlot(RecipeIngredientRole.INPUT, startX + col * 18, startY + row * 18)
							.addIngredients(inputs.get(index));
				}
			}
		}

		builder.addSlot(RecipeIngredientRole.INPUT, 89, 17)
				.addIngredients(recipe.getPattern())
				.setSlotName("Pattern");

		builder.addSlot(RecipeIngredientRole.INPUT, 89, 53)
				.addIngredients(recipe.getTemplate())
				.setSlotName("Template");

		builder.addSlot(RecipeIngredientRole.OUTPUT, 141, 35)
				.addItemStack(recipe.getResultItem(null));
	}
}