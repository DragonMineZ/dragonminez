package com.dragonminez.server.recipes;

import com.dragonminez.common.init.MainRecipes;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class PatternRecipe implements Recipe<CraftingContainer> {
	public static final int MAX_SIZE = 6;

	private final ResourceLocation id;
	private final String group;
	private final int width;
	private final int height;
	private final NonNullList<Ingredient> ingredients;
	private final ItemStack result;
	private final boolean showNotification;

	public PatternRecipe(ResourceLocation id, String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, boolean showNotification) {
		this.id = id;
		this.group = group;
		this.width = width;
		this.height = height;
		this.ingredients = ingredients;
		this.result = result;
		this.showNotification = showNotification;
	}

	@Override
	public boolean matches(CraftingContainer container, Level level) {
		for (int x = 0; x <= container.getWidth() - this.width; ++x) {
			for (int y = 0; y <= container.getHeight() - this.height; ++y) {
				if (this.matches(container, x, y, true)) return true;
				if (this.matches(container, x, y, false)) return true;
			}
		}
		return false;
	}

	private boolean matches(CraftingContainer container, int offsetX, int offsetY, boolean mirrored) {
		for (int i = 0; i < container.getWidth(); ++i) {
			for (int j = 0; j < container.getHeight(); ++j) {
				int k = i - offsetX;
				int l = j - offsetY;
				Ingredient ingredient = Ingredient.EMPTY;
				if (k >= 0 && l >= 0 && k < this.width && l < this.height) {
					ingredient = mirrored
							? this.ingredients.get(this.width - k - 1 + l * this.width)
							: this.ingredients.get(k + l * this.width);
				}
				if (!ingredient.test(container.getItem(i + j * container.getWidth()))) return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
		return this.result.copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width >= this.width && height >= this.height;
	}

	@Override
	public ItemStack getResultItem(RegistryAccess registryAccess) {
		return this.result;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return this.ingredients;
	}

	@Override
	public String getGroup() {
		return this.group;
	}

	@Override
	public boolean showNotification() {
		return this.showNotification;
	}

	@Override
	public boolean isIncomplete() {
		NonNullList<Ingredient> list = this.getIngredients();
		return list.isEmpty() || list.stream().filter(ing -> !ing.isEmpty()).anyMatch(net.minecraftforge.common.ForgeHooks::hasNoElements);
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	@Override
	public ResourceLocation getId() {
		return this.id;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return MainRecipes.PATTERN_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return MainRecipes.PATTERN_TYPE.get();
	}

	public static class Serializer implements RecipeSerializer<PatternRecipe> {
		public static final Serializer INSTANCE = new Serializer();

		@Override
		public PatternRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
			String group = GsonHelper.getAsString(json, "group", "");
			Map<String, Ingredient> key = keyFromJson(GsonHelper.getAsJsonObject(json, "key"));
			String[] pattern = shrink(patternFromJson(GsonHelper.getAsJsonArray(json, "pattern")));
			if (pattern.length == 0) throw new JsonSyntaxException("Invalid pattern: pattern only contains empty slots");
			int width = pattern[0].length();
			int height = pattern.length;
			NonNullList<Ingredient> ingredients = dissolvePattern(pattern, key, width, height);
			ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
			boolean showNotification = GsonHelper.getAsBoolean(json, "show_notification", true);
			return new PatternRecipe(recipeId, group, width, height, ingredients, result, showNotification);
		}

		@Override
		public @Nullable PatternRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
			int width = buffer.readVarInt();
			int height = buffer.readVarInt();
			String group = buffer.readUtf();
			NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
			for (int i = 0; i < ingredients.size(); ++i) {
				ingredients.set(i, Ingredient.fromNetwork(buffer));
			}
			ItemStack result = buffer.readItem();
			boolean showNotification = buffer.readBoolean();
			return new PatternRecipe(recipeId, group, width, height, ingredients, result, showNotification);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buffer, PatternRecipe recipe) {
			buffer.writeVarInt(recipe.width);
			buffer.writeVarInt(recipe.height);
			buffer.writeUtf(recipe.group);
			for (Ingredient ingredient : recipe.ingredients) {
				ingredient.toNetwork(buffer);
			}
			buffer.writeItem(recipe.result);
			buffer.writeBoolean(recipe.showNotification);
		}

		private static NonNullList<Ingredient> dissolvePattern(String[] pattern, Map<String, Ingredient> key, int width, int height) {
			NonNullList<Ingredient> list = NonNullList.withSize(width * height, Ingredient.EMPTY);
			Set<String> unused = Sets.newHashSet(key.keySet());
			unused.remove(" ");
			for (int row = 0; row < pattern.length; ++row) {
				for (int col = 0; col < pattern[row].length(); ++col) {
					String s = pattern[row].substring(col, col + 1);
					Ingredient ingredient = key.get(s);
					if (ingredient == null) {
						throw new JsonSyntaxException("Pattern references symbol '" + s + "' but it's not defined in the key");
					}
					unused.remove(s);
					list.set(col + width * row, ingredient);
				}
			}
			if (!unused.isEmpty()) {
				throw new JsonSyntaxException("Key defines symbols that aren't used in pattern: " + unused);
			}
			return list;
		}

		private static String[] shrink(String... rows) {
			int first = Integer.MAX_VALUE;
			int last = 0;
			int emptyTop = 0;
			int emptyBottom = 0;
			for (int i = 0; i < rows.length; ++i) {
				String row = rows[i];
				first = Math.min(first, firstNonSpace(row));
				int lastIdx = lastNonSpace(row);
				last = Math.max(last, lastIdx);
				if (lastIdx < 0) {
					if (emptyTop == i) ++emptyTop;
					++emptyBottom;
				} else {
					emptyBottom = 0;
				}
			}
			if (rows.length == emptyBottom) return new String[0];
			String[] result = new String[rows.length - emptyBottom - emptyTop];
			for (int i = 0; i < result.length; ++i) {
				result[i] = rows[i + emptyTop].substring(first, last + 1);
			}
			return result;
		}

		private static int firstNonSpace(String s) {
			int i = 0;
			while (i < s.length() && s.charAt(i) == ' ') ++i;
			return i;
		}

		private static int lastNonSpace(String s) {
			int i = s.length() - 1;
			while (i >= 0 && s.charAt(i) == ' ') --i;
			return i;
		}

		private static String[] patternFromJson(JsonArray array) {
			String[] rows = new String[array.size()];
			if (rows.length > MAX_SIZE) {
				throw new JsonSyntaxException("Invalid pattern: too many rows, " + MAX_SIZE + " is maximum");
			} else if (rows.length == 0) {
				throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
			}
			for (int i = 0; i < rows.length; ++i) {
				String row = GsonHelper.convertToString(array.get(i), "pattern[" + i + "]");
				if (row.length() > MAX_SIZE) {
					throw new JsonSyntaxException("Invalid pattern: too many columns, " + MAX_SIZE + " is maximum");
				}
				if (i > 0 && rows[0].length() != row.length()) {
					throw new JsonSyntaxException("Invalid pattern: each row must be the same width");
				}
				rows[i] = row;
			}
			return rows;
		}

		private static Map<String, Ingredient> keyFromJson(JsonObject json) {
			Map<String, Ingredient> map = Maps.newHashMap();
			for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
				if (entry.getKey().length() != 1) {
					throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
				}
				if (" ".equals(entry.getKey())) {
					throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
				}
				map.put(entry.getKey(), Ingredient.fromJson(entry.getValue(), false));
			}
			map.put(" ", Ingredient.EMPTY);
			return map;
		}
	}
}
