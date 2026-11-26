package com.dragonminez.server.recipes;

import com.dragonminez.common.init.MainRecipes;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.List;

public class PatternRecipe implements Recipe<CraftingInput> {
	public static final int MAX_SIZE = 6;

	private final String group;
	private final int width;
	private final int height;
	private final NonNullList<Ingredient> ingredients;
	private final ItemStack result;
	private final boolean showNotification;

	public PatternRecipe(String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, boolean showNotification) {
		this.group = group;
		this.width = width;
		this.height = height;
		this.ingredients = ingredients;
		this.result = result;
		this.showNotification = showNotification;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		for (int x = 0; x <= input.width() - this.width; ++x) {
			for (int y = 0; y <= input.height() - this.height; ++y) {
				if (this.matches(input, x, y, true)) return true;
				if (this.matches(input, x, y, false)) return true;
			}
		}
		return false;
	}

	private boolean matches(CraftingInput input, int offsetX, int offsetY, boolean mirrored) {
		for (int i = 0; i < input.width(); ++i) {
			for (int j = 0; j < input.height(); ++j) {
				int k = i - offsetX;
				int l = j - offsetY;
				Ingredient ingredient = Ingredient.EMPTY;
				if (k >= 0 && l >= 0 && k < this.width && l < this.height) {
					ingredient = mirrored
							? this.ingredients.get(this.width - k - 1 + l * this.width)
							: this.ingredients.get(k + l * this.width);
				}
				if (!ingredient.test(input.getItem(i + j * input.width()))) return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack assemble(CraftingInput container, HolderLookup.Provider registryAccess) {
		return this.result.copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width >= this.width && height >= this.height;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registryAccess) {
		return this.result.copy();
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
		return list.isEmpty() || list.stream().filter(ing -> !ing.isEmpty()).anyMatch(Ingredient::hasNoItems);
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return MainRecipes.PATTERN_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return MainRecipes.PATTERN_TYPE.get();
	}

	private List<String> patternRows() {
		String symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		List<String> rows = new java.util.ArrayList<>(this.height);
		for (int row = 0; row < this.height; row++) {
			StringBuilder pattern = new StringBuilder(this.width);
			for (int col = 0; col < this.width; col++) {
				int index = row * this.width + col;
				pattern.append(this.ingredients.get(index).isEmpty() ? ' ' : symbols.charAt(index));
			}
			rows.add(pattern.toString());
		}
		return rows;
	}

	private Map<String, Ingredient> patternKey() {
		String symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		Map<String, Ingredient> key = Maps.newLinkedHashMap();
		for (int index = 0; index < this.ingredients.size(); index++) {
			Ingredient ingredient = this.ingredients.get(index);
			if (!ingredient.isEmpty()) key.put(Character.toString(symbols.charAt(index)), ingredient);
		}
		return key;
	}

	public static class Serializer implements RecipeSerializer<PatternRecipe> {
		public static final Serializer INSTANCE = new Serializer();
		private static final MapCodec<PatternRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.STRING.optionalFieldOf("group", "").forGetter(recipe -> recipe.group),
				Codec.STRING.listOf().fieldOf("pattern").forGetter(PatternRecipe::patternRows),
				Codec.unboundedMap(Codec.STRING, Ingredient.CODEC).fieldOf("key").forGetter(PatternRecipe::patternKey),
				ItemStack.CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
				Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(recipe -> recipe.showNotification)
		).apply(instance, Serializer::fromCodec));
		private static final StreamCodec<RegistryFriendlyByteBuf, PatternRecipe> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, recipe -> recipe.width,
				ByteBufCodecs.VAR_INT, recipe -> recipe.height,
				ByteBufCodecs.STRING_UTF8, recipe -> recipe.group,
				Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> List.copyOf(recipe.ingredients),
				ItemStack.STREAM_CODEC, recipe -> recipe.result,
				ByteBufCodecs.BOOL, recipe -> recipe.showNotification,
				Serializer::fromNetwork);

		@Override
		public MapCodec<PatternRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, PatternRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static PatternRecipe fromCodec(String group, List<String> rows, Map<String, Ingredient> key, ItemStack result, boolean showNotification) {
			if (rows.size() > MAX_SIZE || rows.isEmpty()) throw new JsonSyntaxException("Invalid pattern size");
			key = Maps.newLinkedHashMap(key);
			int rowWidth = rows.getFirst().length();
			if (rowWidth > MAX_SIZE || rows.stream().anyMatch(row -> row.length() != rowWidth)) {
				throw new JsonSyntaxException("Invalid pattern: rows must have the same width of at most " + MAX_SIZE);
			}
			for (String symbol : key.keySet()) {
				if (symbol.length() != 1 || " ".equals(symbol)) {
					throw new JsonSyntaxException("Invalid key entry: '" + symbol + "' is not a valid symbol");
				}
			}
			key.put(" ", Ingredient.EMPTY);
			String[] pattern = shrink(rows.toArray(String[]::new));
			if (pattern.length == 0) throw new JsonSyntaxException("Invalid pattern: pattern only contains empty slots");
			int width = pattern[0].length();
			int height = pattern.length;
			return new PatternRecipe(group, width, height, dissolvePattern(pattern, key, width, height), result, showNotification);
		}

		private static PatternRecipe fromNetwork(int width, int height, String group, List<Ingredient> ingredients, ItemStack result, boolean showNotification) {
			if (width < 1 || height < 1 || width > MAX_SIZE || height > MAX_SIZE || ingredients.size() != width * height) {
				throw new IllegalArgumentException("Invalid pattern recipe dimensions or ingredient count");
			}
			NonNullList<Ingredient> ingredientList = NonNullList.withSize(width * height, Ingredient.EMPTY);
			for (int index = 0; index < ingredientList.size(); index++) ingredientList.set(index, ingredients.get(index));
			return new PatternRecipe(group, width, height, ingredientList, result, showNotification);
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

	}
}
