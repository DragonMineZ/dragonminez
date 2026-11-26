package com.dragonminez.common.util.types.items;

import com.dragonminez.common.util.gson.GsonUtils;
import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;

/**
 * A plain item stack described in JSON: an id and a count.
 *
 * <p>Subclasses add NBT on top (enchantments, potion data, armour trims) and are selected
 * by the {@code itemType} discriminator; see
 * {@code com.dragonminez.common.util.gson.GenericItemTypeAdapterFactory}.
 *
 * <p>{@link #validate()} runs once when the JSON is read and throws on anything
 * structurally wrong, so a bad file fails at load with a message naming the problem.
 * {@link #getItemStack()} never throws: it runs during gameplay, and an id that is not
 * registered on this instance — a missing addon, say — must not take a server tick with it.
 */
public class GenericItemDTO {
	public static final String ITEM_TYPE = "generic_item";
	protected String itemType;
	protected ResourceLocation itemId;
	protected int count = 1;

	public GenericItemDTO(ResourceLocation itemId, int count) {
		this(ITEM_TYPE, itemId, count);
	}

	public GenericItemDTO(String itemId, int count) {
		this(ITEM_TYPE, ResourceLocation.parse(itemId), count);
	}

	public GenericItemDTO(String itemType, String itemId, int count) {
		this(itemType, ResourceLocation.parse(itemId), count);
	}

	public GenericItemDTO(Item item, int count) {
		this(ITEM_TYPE, BuiltInRegistries.ITEM.getKey(item), count);
	}

	public GenericItemDTO(String itemType, ResourceLocation itemId, int count) {
		this.itemType = itemType;
		this.itemId = itemId;
		this.count = count;
	}

	/**
	 * Structural checks, run at load time. Throws {@link JsonSyntaxException} on malformed
	 * input. Whether the id resolves against the live registries is not checked here — see
	 * the class comment.
	 */
	public void validate() {
		if (this.itemId == null) {
			throw new JsonSyntaxException(errorPrefix() + " \'itemId\' must not be null.");
		}
		if (this.count < 1) {
			throw new JsonSyntaxException(errorPrefix() + " \'count\' must be at least 1, but was " + this.count + ".");
		}
	}

	public String toJson() {
		return GsonUtils.GSON.toJson(this, GenericItemDTO.class);
	}

	/**
	 * Builds the stack, or {@link ItemStack#EMPTY} if the id is not registered here.
	 * Never throws.
	 */
	public ItemStack getItemStack() {
		Item item = resolveItem();
		if (item == null) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(item, Math.max(1, this.count));
	}

	public ItemStack getItemStack(RegistryAccess registryAccess) {
		return getItemStack();
	}

	protected final Item resolveItem() {
		if (this.itemId == null) {
			LogUtil.warn(Env.COMMON, "{} has no item id, skipping.", this.getClass().getSimpleName());
			return null;
		}
		if (!BuiltInRegistries.ITEM.containsKey(this.itemId)) {
			LogUtil.warn(Env.COMMON, "Unknown item \'{}\', skipping.", this.itemId);
			return null;
		}
		return BuiltInRegistries.ITEM.get(this.itemId);
	}

	protected final String errorPrefix() {
		return this.getClass().getSimpleName() + " \'" + this.itemId + "\':";
	}

	public String getItemType() {
		return this.itemType;
	}

	public ResourceLocation getItemId() {
		return this.itemId;
	}

	public int getCount() {
		return this.count;
	}

	public void setItemType(final String itemType) {
		this.itemType = itemType;
	}

	public void setItemId(final ResourceLocation itemId) {
		this.itemId = itemId;
	}

	public void setCount(final int count) {
		this.count = count;
	}

	public GenericItemDTO() {
	}
}
