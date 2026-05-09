package com.dragonminez.client.gui.tooltip.dynamic;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class EnchantmentColorHandler {

	private static final int ENCHANTMENT_COLOR = 0xAAAAAA;
	private static final int SUPER_LEVELED_COLOR = 0xFF55FF;
	private static final int CURSE_COLOR = 0xFF5555;

	public static void colorizeEnchantmentNames(ItemStack stack, java.util.List<Component> tooltip) {
		Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
		if (enchantments.isEmpty()) return;

		for (int i = 0; i < tooltip.size(); i++) {
			Component line = tooltip.get(i);

			if (line.getContents() instanceof TranslatableContents translatable && translatable.getKey().startsWith("enchantment.")) {
				for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
					Enchantment ench = entry.getKey();
					if (translatable.getKey().equals(ench.getDescriptionId())) {

						int color = ENCHANTMENT_COLOR;
						if (ench.isCurse()) color = CURSE_COLOR;
						else if (entry.getValue() > ench.getMaxLevel()) color = SUPER_LEVELED_COLOR;

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