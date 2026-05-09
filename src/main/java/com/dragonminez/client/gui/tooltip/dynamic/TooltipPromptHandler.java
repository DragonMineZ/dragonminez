package com.dragonminez.client.gui.tooltip.dynamic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.List;

public class TooltipPromptHandler {

	public static Component getExpandPrompt() {
		return Component.translatable("tooltip.dragonminez.shift_expand").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true));
	}

	public static int findEnchantSectionEnd(List<Component> tooltip) {
		for (int i = 0; i < tooltip.size(); i++) {
			Component line = tooltip.get(i);

			// Verificamos si el componente tiene una clave de traducción base
			if (line.getContents() instanceof TranslatableContents t && t.getKey().startsWith("item.modifiers.")) {
				// Si la línea anterior está vacía, insertamos justo ahí
				if (i > 0 && tooltip.get(i - 1).getString().isEmpty()) {
					return i - 1;
				}
				return i;
			}

			// Si el componente base no es translatable, buscamos en sus "hermanos" (siblings)
			for (Component sibling : line.getSiblings()) {
				if (sibling.getContents() instanceof TranslatableContents tSibling && tSibling.getKey().startsWith("item.modifiers.")) {
					if (i > 0 && tooltip.get(i - 1).getString().isEmpty()) {
						return i - 1;
					}
					return i;
				}
			}
		}
		return -1;
	}
}