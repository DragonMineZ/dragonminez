package com.dragonminez.common.util.types.items;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import java.util.HashMap;
import java.util.Map;

public class EnchantedItemDTO extends GenericItemDTO {
	public static final String ITEM_TYPE = "enchanted_item";
	protected Map<ResourceLocation, Integer> enchantments = new HashMap<>();

	public EnchantedItemDTO(ResourceLocation itemId, int count) {
		super(ITEM_TYPE, itemId, count);
	}

	public EnchantedItemDTO(ResourceLocation itemId, int count, Map<ResourceLocation, Integer> enchantments) {
		super(ITEM_TYPE, itemId, count);
		this.enchantments = enchantments;
	}

	protected EnchantedItemDTO(String itemType, ResourceLocation itemId, int count) {
		super(itemType, itemId, count);
	}

	protected EnchantedItemDTO(String itemType, ResourceLocation itemId, int count, Map<ResourceLocation, Integer> enchantments) {
		super(itemType, itemId, count);
		this.enchantments = enchantments;
	}

	@Override
	public void validate() {
		super.validate();
		if (this.enchantments == null) return;
		this.enchantments.forEach((id, level) -> {
			if (id == null) {
				throw new JsonSyntaxException(errorPrefix() + " an enchantment id must not be null.");
			}
			if (level == null || level < 1) {
				throw new JsonSyntaxException(errorPrefix() + " enchantment \'" + id + "\' has an invalid level: " + level + ".");
			}
		});
	}

	@Override
	public ItemStack getItemStack(RegistryAccess registryAccess) {
		ItemStack itemStack = super.getItemStack(registryAccess);
		if (itemStack.isEmpty() || registryAccess == null || this.enchantments == null) return itemStack;
		ItemEnchantments.Mutable resolved = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		this.enchantments.forEach((id, level) -> {
			Holder<Enchantment> enchantment = resolveEnchantment(registryAccess, id);
			if (enchantment != null) resolved.set(enchantment, level);
		});
		ItemEnchantments enchantments = resolved.toImmutable();
		if (!enchantments.isEmpty()) itemStack.set(enchantmentComponent(), enchantments);
		return itemStack;
	}

	protected DataComponentType<ItemEnchantments> enchantmentComponent() {
		return DataComponents.ENCHANTMENTS;
	}

	protected final Holder<Enchantment> resolveEnchantment(RegistryAccess registryAccess, ResourceLocation id) {
		if (id == null) return null;
		Holder.Reference<Enchantment> enchantment = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT, id)).orElse(null);
		if (enchantment == null) {
			LogUtil.warn(Env.COMMON, "Unknown enchantment \'{}\' on item \'{}\', skipping.", id, this.itemId);
		}
		return enchantment;
	}

	public Map<ResourceLocation, Integer> getEnchantments() {
		return this.enchantments;
	}

	public void setEnchantments(final Map<ResourceLocation, Integer> enchantments) {
		this.enchantments = enchantments;
	}

	public EnchantedItemDTO() {
	}
}
