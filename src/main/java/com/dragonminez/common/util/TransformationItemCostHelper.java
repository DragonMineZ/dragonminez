package com.dragonminez.common.util;

import com.dragonminez.common.config.FormConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.core.registries.Registries;

import java.util.HashMap;
import java.util.Map;

public class TransformationItemCostHelper {
	private static final String PERSISTENT_FORM_DURATION_KEY = "dmz_form_duration_item_seconds";
	private static final String PERSISTENT_STACK_DURATION_KEY = "dmz_stack_form_duration_item_seconds";
	private static final Map<String, ResourceLocation> RESOURCE_LOCATION_CACHE = new HashMap<>();
	private static final Map<String, TagKey<Item>> ITEM_TAG_CACHE = new HashMap<>();
	private static final Map<String, CompoundTag> NBT_CACHE = new HashMap<>();

	private TransformationItemCostHelper() {}

	public static boolean canAffordAndHandleTriggerCost(Player player, FormConfig.FormData formData) {
		if (player == null || player.isCreative()) return true;
		if (formData == null || !formData.hasTriggerItemCosts()) return true;

		for (FormConfig.FormData.TriggerItemCost cost : formData.getTriggerItemCosts()) {
			if (!isValidTriggerCost(cost)) continue;
			int foundCount = countMatchingItems(player, cost.getItemId(), cost.getItemTag(), cost.getNbt());
			if (foundCount < cost.getCount()) continue;

			if (cost.isConsume()) {
				consumeMatchingItems(player, cost.getCount(), cost.getItemId(), cost.getItemTag(), cost.getNbt());
			}
			return true;
		}

		return false;
	}

	public static int consumeDurationItem(Player player, FormConfig.FormData formData) {
		if (player == null || player.isCreative()) return Integer.MAX_VALUE;
		if (formData == null || !formData.hasDurationItemCosts()) return 0;

		for (FormConfig.FormData.DurationItemCost cost : formData.getDurationItemCosts()) {
			if (!isValidDurationCost(cost)) continue;
			if (consumeMatchingItems(player, 1, cost.getItemId(), cost.getItemTag(), cost.getNbt()) > 0) {
				return cost.getDurationSeconds();
			}
		}

		return 0;
	}

	public static int getFormDurationSecondsRemaining(Player player) {
		return Math.max(0, player.getPersistentData().getInt(PERSISTENT_FORM_DURATION_KEY));
	}

	public static void setFormDurationSecondsRemaining(Player player, int seconds) {
		setPersistentDuration(player, PERSISTENT_FORM_DURATION_KEY, seconds);
	}

	public static void clearFormDurationSecondsRemaining(Player player) {
		player.getPersistentData().remove(PERSISTENT_FORM_DURATION_KEY);
	}

	public static int getStackFormDurationSecondsRemaining(Player player) {
		return Math.max(0, player.getPersistentData().getInt(PERSISTENT_STACK_DURATION_KEY));
	}

	public static void setStackFormDurationSecondsRemaining(Player player, int seconds) {
		setPersistentDuration(player, PERSISTENT_STACK_DURATION_KEY, seconds);
	}

	public static void clearStackFormDurationSecondsRemaining(Player player) {
		player.getPersistentData().remove(PERSISTENT_STACK_DURATION_KEY);
	}

	private static void setPersistentDuration(Player player, String key, int seconds) {
		if (seconds <= 0) player.getPersistentData().remove(key);
		else player.getPersistentData().putInt(key, seconds);
	}

	private static boolean isValidTriggerCost(FormConfig.FormData.TriggerItemCost cost) {
		return cost != null && (cost.hasItemId() || cost.hasItemTag());
	}

	private static boolean isValidDurationCost(FormConfig.FormData.DurationItemCost cost) {
		return cost != null && (cost.hasItemId() || cost.hasItemTag());
	}

	private static int countMatchingItems(Player player, String itemId, String itemTag, String nbtString) {
		int total = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (matches(stack, itemId, itemTag, nbtString)) total += stack.getCount();
		}
		return total;
	}

	private static int consumeMatchingItems(Player player, int amount, String itemId, String itemTag, String nbtString) {
		int remaining = Math.max(1, amount);
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!matches(stack, itemId, itemTag, nbtString)) continue;
			int extracted = Math.min(stack.getCount(), remaining);
			stack.shrink(extracted);
			remaining -= extracted;
			if (remaining <= 0) return amount;
		}
		return amount - remaining;
	}

	private static boolean matches(ItemStack stack, String itemId, String itemTag, String nbtString) {
		if (stack == null || stack.isEmpty()) return false;
		if (!matchesItemOrTag(stack, itemId, itemTag)) return false;
		return matchesNbt(stack, nbtString);
	}

	private static boolean matchesItemOrTag(ItemStack stack, String itemId, String itemTag) {
		if (itemId != null && !itemId.isBlank()) {
			ResourceLocation itemKey = getCachedResourceLocation(itemId);
			if (itemKey == null) return false;
			Item expectedItem = ForgeRegistries.ITEMS.getValue(itemKey);
			return expectedItem != null && expectedItem == stack.getItem();
		}
		if (itemTag != null && !itemTag.isBlank()) {
			TagKey<Item> tagKey = getCachedItemTag(itemTag);
			return tagKey != null && stack.is(tagKey);
		}
		return false;
	}

	private static boolean matchesNbt(ItemStack stack, String nbtString) {
		if (nbtString == null || nbtString.isBlank()) return true;
		CompoundTag requiredTag = getCachedNbt(nbtString);
		if (requiredTag == null) return false;
		CompoundTag stackTag = stack.getTag();
		if (stackTag == null) return false;
		return NbtUtils.compareNbt(requiredTag, stackTag, true);
	}

	private static ResourceLocation getCachedResourceLocation(String id) {
		if (id == null || id.isBlank()) return null;
		if (RESOURCE_LOCATION_CACHE.containsKey(id)) return RESOURCE_LOCATION_CACHE.get(id);
		ResourceLocation location = ResourceLocation.tryParse(id);
		RESOURCE_LOCATION_CACHE.put(id, location);
		return location;
	}

	private static TagKey<Item> getCachedItemTag(String tagId) {
		if (tagId == null || tagId.isBlank()) return null;
		if (ITEM_TAG_CACHE.containsKey(tagId)) return ITEM_TAG_CACHE.get(tagId);
		ResourceLocation location = getCachedResourceLocation(tagId);
		TagKey<Item> tagKey = location != null ? TagKey.create(Registries.ITEM, location) : null;
		ITEM_TAG_CACHE.put(tagId, tagKey);
		return tagKey;
	}

	private static CompoundTag getCachedNbt(String nbtString) {
		if (nbtString == null || nbtString.isBlank()) return null;
		if (NBT_CACHE.containsKey(nbtString)) return NBT_CACHE.get(nbtString);
		try {
			CompoundTag parsed = TagParser.parseTag(nbtString);
			NBT_CACHE.put(nbtString, parsed);
			return parsed;
		} catch (Exception ignored) {
			NBT_CACHE.put(nbtString, null);
			return null;
		}
	}
}
