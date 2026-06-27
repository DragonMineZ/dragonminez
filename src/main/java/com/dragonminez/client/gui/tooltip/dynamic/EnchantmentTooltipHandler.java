package com.dragonminez.client.gui.tooltip.dynamic;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Consumer;

public class EnchantmentTooltipHandler {

	private static final String[] KEY_TYPES = {"desc", "description", "info"};
	private static final int ENCHANT_DESC_COLOR = 0x888888;

	public static void insertDescription(Enchantment enchantment, int level, Consumer<Component> lines) {
		Component description = getDescription(enchantment, level);
		if (description != null) {
			Style descriptionStyle = Style.EMPTY.withColor(ENCHANT_DESC_COLOR).withItalic(true);

			MutableComponent styledDescription = description.copy().withStyle(descriptionStyle);
			lines.accept(Component.literal(" ").append(styledDescription));
		}
	}

	private static Component getDescription(Enchantment enchantment, int level) {
		var id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
		if (id == null) return null;

		String baseKey = "enchantment." + id.getNamespace() + "." + id.getPath() + ".";
		Language lang = Language.getInstance();

		for (String keyType : KEY_TYPES) {
			String key = baseKey + keyType;
			if (lang.has(key)) return Component.translatable(key);
			key = key + "." + level;
			if (lang.has(key)) return Component.translatable(key);
		}
		return null;
	}
}