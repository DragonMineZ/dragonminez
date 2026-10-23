package com.dragonminez.common.util.types.items;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import java.util.Map;

public class EnchantedBookDTO extends EnchantedItemDTO {
	public static final String ITEM_TYPE = "enchanted_book";
	private static final ResourceLocation ENCHANTED_BOOK_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "enchanted_book");

	public EnchantedBookDTO(Map<ResourceLocation, Integer> enchantments) {
		super(ITEM_TYPE, ENCHANTED_BOOK_ID, 1, enchantments);
	}

	@Override
	protected DataComponentType<ItemEnchantments> enchantmentComponent() {
		return DataComponents.STORED_ENCHANTMENTS;
	}

	public EnchantedBookDTO() {
	}
}
