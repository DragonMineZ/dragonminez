package com.dragonminez.common.wish.wishes;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.util.ItemIdAliases;
import com.dragonminez.common.wish.Wish;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class MultiItemWish extends Wish {
	private final List<Tuple<String, Integer>> items;

	public MultiItemWish(String name, String description, List<Tuple<String, Integer>> items) {
		super(name, description, "multi_wish");
		this.items = items;
	}

	/**
	 * Reads the pre-GenericItemDTO format used by existing custom packs. The pair fields are
	 * called {@code a}/{@code b} in named mappings, while runtime-generated files can expose
	 * their mapped names. Accept the newer itemId/count spelling too.
	 */
	public static MultiItemWish fromJson(JsonObject root) {
		String name = requiredString(root, "name");
		String description = requiredString(root, "description");
		JsonElement rawItems = root.get("items");
		if (rawItems == null || !rawItems.isJsonArray()) {
			throw new JsonParseException("multi_wish requires an items array");
		}

		List<Tuple<String, Integer>> items = new ArrayList<>();
		for (JsonElement rawItem : rawItems.getAsJsonArray()) {
			if (!rawItem.isJsonObject()) {
				throw new JsonParseException("multi_wish items must be objects");
			}
			JsonObject item = rawItem.getAsJsonObject();
			String itemId = firstString(item, "itemId", "item_id", "a", "f_14413_");
			Integer count = firstInt(item, "count", "amount", "b", "f_14414_");
			if (itemId == null || count == null || count <= 0) {
				throw new JsonParseException("multi_wish item requires an item ID and a positive count");
			}
			items.add(new Tuple<>(itemId, count));
		}
		return new MultiItemWish(name, description, items);
	}

	private static String requiredString(JsonObject root, String key) {
		String value = firstString(root, key);
		if (value == null) throw new JsonParseException("multi_wish requires '" + key + "'");
		return value;
	}

	private static String firstString(JsonObject object, String... keys) {
		for (String key : keys) {
			JsonElement value = object.get(key);
			if (value != null && !value.isJsonNull() && value.isJsonPrimitive()) return value.getAsString();
		}
		return null;
	}

	private static Integer firstInt(JsonObject object, String... keys) {
		for (String key : keys) {
			JsonElement value = object.get(key);
			if (value != null && !value.isJsonNull() && value.isJsonPrimitive()) return value.getAsInt();
		}
		return null;
	}

	@Override
	public void grant(ServerPlayer player) {
		for (Tuple<String, Integer> itemInfo : items) {
			String itemId = ItemIdAliases.normalize(itemInfo.getA());
			Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemId));
			if (item != null) {
				giveOrDrop(player, new ItemStack(item, itemInfo.getB()));
			} else {
				LogUtil.warn(Env.COMMON, "Item with id " + itemId + " not found.");
			}
		}
	}

	private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
		player.getInventory().add(stack);
		if (!stack.isEmpty()) {
			ItemEntity drop = player.drop(stack, false);
			if (drop != null) drop.setNoPickUpDelay();
		}
	}

	@Override
	public String toJson() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(this, MultiItemWish.class);
	}
}
