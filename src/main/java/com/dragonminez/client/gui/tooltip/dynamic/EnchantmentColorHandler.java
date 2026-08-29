package com.dragonminez.client.gui.tooltip.dynamic;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class EnchantmentColorHandler {

	private static final int ENCHANTMENT_COLOR = 0xAAAAAA;
	private static final int SUPER_LEVELED_COLOR = 0xFF55FF;
	private static final int CURSE_COLOR = 0xFF5555;

	public static void colorizeEnchantmentNames(ItemStack stack, java.util.List<Component> tooltip) {
		ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		if (enchantments.isEmpty()) {
			enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		}
		if (enchantments.isEmpty()) return;

		for (int i = 0; i < tooltip.size(); i++) {
			Component line = tooltip.get(i);

			if (line.getContents() instanceof TranslatableContents translatable && translatable.getKey().startsWith("enchantment.")) {
				for (Holder<Enchantment> holder : enchantments.keySet()) {
					int level = enchantments.getLevel(holder);
					String descId = holder.unwrapKey()
							.map(key -> net.minecraft.Util.makeDescriptionId("enchantment", key.location()))
							.orElse("");
					if (translatable.getKey().equals(descId)) {
						int color = ENCHANTMENT_COLOR;
						if (descId.contains("curse") || descId.contains("vanishing") || descId.contains("binding")) {
							color = CURSE_COLOR;
						} else if (level > 5) {
							color = SUPER_LEVELED_COLOR;
						}

						int finalColor = color;
						MutableComponent newComponent = line.copy().withStyle(style -> style.withColor(finalColor));
						tooltip.set(i, newComponent);
						break;
					}
				}
			}
		}
	}
}
