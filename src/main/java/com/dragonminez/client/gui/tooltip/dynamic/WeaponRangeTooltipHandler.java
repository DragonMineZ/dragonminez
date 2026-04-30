package com.dragonminez.client.gui.tooltip.dynamic;

import com.dragonminez.common.combat.weapon.WeaponAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.common.ForgeMod;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.Consumer;

public class WeaponRangeTooltipHandler {

	private static final DecimalFormat FORMAT = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.ROOT));

	public static boolean appendWeaponAttributes(WeaponAttributes attributes, Consumer<Component> tooltip) {
		if (attributes != null && attributes.attackRange() > 0) {
			double totalRange = attributes.attackRange();

			MutableComponent text = Component.translatable("attribute.modifier.equals.0", FORMAT.format(totalRange), Component.translatable(ForgeMod.ENTITY_REACH.get().getDescriptionId()));
			tooltip.accept(Component.literal(" ").append(text.withStyle(AttributeTooltipHandler.BASE_COLOR)));
		}

		if (attributes != null && attributes.isTwoHanded()) {
			tooltip.accept(Component.translatable("tooltip.dragonminez.two_handed").withStyle(ChatFormatting.DARK_GRAY));
		}

		return false;
	}
}