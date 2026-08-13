package com.dragonminez.client.gui.tooltip.dynamic;

import net.minecraft.core.Holder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.function.Consumer;

public class EnchantmentTooltipHandler {

	private static final String[] KEY_TYPES = {"desc", "description", "info"};
	private static final int ENCHANT_DESC_COLOR = 0x888888;

	public static void insertDescription(Enchantment enchantment, int level, Consumer<Component> lines) {
		// Enchantment is no longer a registry object with a stable BuiltInRegistries key in 1.21;
		// prefer holder-based overload when available.
		insertDescriptionById(null, level, lines);
	}

	public static void insertDescription(Holder<Enchantment> holder, int level, Consumer<Component> lines) {
		ResourceLocation id = holder.unwrapKey().map(k -> k.location()).orElse(null);
		insertDescriptionById(id, level, lines);
	}

	public static void insertDescriptionById(ResourceLocation id, int level, Consumer<Component> lines) {
		if (id == null) return;
		Component description = getDescription(id, level);
		if (description != null) {
			Style descriptionStyle = Style.EMPTY.withColor(ENCHANT_DESC_COLOR).withItalic(true);
			MutableComponent styledDescription = description.copy().withStyle(descriptionStyle);
			lines.accept(Component.literal(" ").append(styledDescription));
		}
	}

	private static Component getDescription(ResourceLocation id, int level) {
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
