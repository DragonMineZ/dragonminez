package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.server.recipes.KikonoRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MainRecipes {
	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
			DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Reference.MOD_ID);
	public static final DeferredRegister<RecipeType<?>> TYPES =
			DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, Reference.MOD_ID);

	public static final RegistryObject<RecipeSerializer<KikonoRecipe>> KIKONO_SERIALIZER =
			SERIALIZERS.register("kikono_crafting", () -> KikonoRecipe.Serializer.INSTANCE);

	public static final RegistryObject<RecipeType<KikonoRecipe>> KIKONO_TYPE =
			TYPES.register("kikono_crafting", () -> new RecipeType<KikonoRecipe>() {
				@Override
				public String toString() {
					return "kikono_crafting";
				}
			});

	public static void register(IEventBus eventBus) {
		SERIALIZERS.register(eventBus);
		TYPES.register(eventBus);
	}
}
