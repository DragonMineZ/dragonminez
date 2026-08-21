package com.dragonminez.common.util.types.items;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.Map;

/**
 * An enchanted book. Unlike a regular enchanted item these go in the
 * {@code StoredEnchantments} tag, which is what {@link EnchantedBookItem#addEnchantment}
 * writes.
 */
@Getter
@Setter
@NoArgsConstructor
public class EnchantedBookDTO extends EnchantedItemDTO {

	public static final String ITEM_TYPE = "enchanted_book";
	private static final ResourceLocation ENCHANTED_BOOK_ID = new ResourceLocation("minecraft", "enchanted_book");

	public EnchantedBookDTO(Map<ResourceLocation, Integer> enchantments) {
		super(ITEM_TYPE, ENCHANTED_BOOK_ID, 1, enchantments);
	}

	@Override
	protected void applyEnchantments(ItemStack itemStack) {
		if (this.enchantments == null || this.enchantments.isEmpty()) {
			return;
		}
		this.enchantments.forEach((id, level) -> {
			Enchantment enchantment = resolveEnchantment(id);
			if (enchantment != null) {
				EnchantedBookItem.addEnchantment(itemStack, new EnchantmentInstance(enchantment, level));
			}
		});
	}
}
