package com.dragonminez.client.gui.tooltip.dynamic;

import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.ForgeMod;

import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Consumer;

public class AttributeTooltipHandler {

	private static final DecimalFormat FORMAT = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.ROOT));

	public static final ChatFormatting BASE_COLOR = ChatFormatting.DARK_GREEN;
	public static final int MERGE_BASE_MODIFIER_COLOR = 16758784;
	public static final int MODIFIER_BLUE = 0x5555FF;
	public static final int MODIFIER_RED = 0xFF5555;

	public static final Set<Attribute> PERCENT_ATTRIBUTES = Set.of(
			Attributes.MOVEMENT_SPEED,
			Attributes.KNOCKBACK_RESISTANCE
	);

	public static final Comparator<AttributeModifier> ATTRIBUTE_MODIFIER_COMPARATOR =
			Comparator.comparing(AttributeModifier::getOperation).thenComparing((AttributeModifier a) -> -Math.abs(a.getAmount())).thenComparing(AttributeModifier::getId);

	public static boolean processVanillaAttributes(ItemStack stack, Consumer<Component> tooltip, @Nullable Player player) {
		boolean needsShiftPrompt = false;

		EquipmentSlot[] slots = {EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND};

		for (EquipmentSlot slot : slots) {
			Multimap<Attribute, AttributeModifier> actualModifiers = stack.getAttributeModifiers(slot);
			Multimap<Attribute, AttributeModifier> defaultModifiers = stack.getItem().getDefaultAttributeModifiers(slot);

			float enchantDamage = 0;
			if (slot == EquipmentSlot.MAINHAND) enchantDamage = EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED);
			if (actualModifiers.isEmpty() && enchantDamage <= 0) continue;
			tooltip.accept(Component.translatable("item.modifiers." + slot.getName()).withStyle(ChatFormatting.GRAY));

			Set<Attribute> allAttributes = new LinkedHashSet<>(actualModifiers.keySet());
			if (enchantDamage > 0) allAttributes.add(Attributes.ATTACK_DAMAGE);

			for (Attribute attr : allAttributes) {
				if (attr.equals(ForgeMod.BLOCK_REACH.get()) || attr.equals(ForgeMod.ENTITY_REACH.get())) continue;

				List<AttributeModifier> baseMods = new ArrayList<>();
				List<AttributeModifier> extraMods = new ArrayList<>();

				for (AttributeModifier mod : actualModifiers.get(attr)) {
					if (defaultModifiers.containsEntry(attr, mod)) baseMods.add(mod);
					else extraMods.add(mod);
				}

				if (attr.equals(Attributes.ATTACK_DAMAGE) && enchantDamage > 0) {
					extraMods.add(new AttributeModifier(UUID.randomUUID(), "Enchantment Damage", enchantDamage, AttributeModifier.Operation.ADDITION));
				}

				double playerBase = player != null ? player.getAttributeBaseValue(attr) : 0.0;
				if (attr.equals(Attributes.ATTACK_DAMAGE)) playerBase = 1.0;
				if (attr.equals(Attributes.ATTACK_SPEED)) playerBase = 4.0;

				double trueBase = playerBase;
				for (AttributeModifier mod : baseMods) {
					if (mod.getOperation() == AttributeModifier.Operation.ADDITION) trueBase += mod.getAmount();
					else if (mod.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) trueBase += playerBase * mod.getAmount();
					else if (mod.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) trueBase *= (1.0 + mod.getAmount());
				}

				double finalValue = trueBase;
				extraMods.sort(ATTRIBUTE_MODIFIER_COMPARATOR);
				for (AttributeModifier mod : extraMods) {
					if (mod.getOperation() == AttributeModifier.Operation.ADDITION) finalValue += mod.getAmount();
					else if (mod.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) finalValue += trueBase * mod.getAmount();
					else if (mod.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) finalValue *= (1.0 + mod.getAmount());
				}

				boolean hasExtras = !extraMods.isEmpty();
				if (hasExtras) needsShiftPrompt = true;

				if (Screen.hasShiftDown() && hasExtras) {
					tooltip.accept(createTotalComponent(attr, finalValue).withStyle(style -> style.withColor(MERGE_BASE_MODIFIER_COLOR)));
					tooltip.accept(listHeader().append(createTotalComponent(attr, trueBase).withStyle(BASE_COLOR)));

					for (AttributeModifier mod : extraMods) tooltip.accept(listHeader().append(createModifierComponent(attr, mod)));
				} else {
					ChatFormatting color = hasExtras ? null : BASE_COLOR;
					Integer intColor = hasExtras ? MERGE_BASE_MODIFIER_COLOR : null;
					tooltip.accept(createTotalComponent(attr, finalValue).withStyle(style -> {
						if (intColor != null) return style.withColor(intColor);
						if (color != null) return style.applyFormat(color);
						return style;
					}));
				}
			}
		}
		return needsShiftPrompt;
	}

	public static MutableComponent createTotalComponent(Attribute attribute, double value) {
		String suffix = PERCENT_ATTRIBUTES.contains(attribute) ? "%" : "";
		double displayValue = PERCENT_ATTRIBUTES.contains(attribute) ? value * 100 : value;
		return Component.literal(" ").append(Component.translatable("attribute.modifier.equals.0", FORMAT.format(displayValue) + suffix, Component.translatable(attribute.getDescriptionId())));
	}

	public static MutableComponent createModifierComponent(Attribute attribute, AttributeModifier modifier) {
		double value = modifier.getAmount();
		boolean isPositive = value > 0;

		String suffix = PERCENT_ATTRIBUTES.contains(attribute) ? "%" : "";
		double displayValue = (PERCENT_ATTRIBUTES.contains(attribute) || modifier.getOperation() != AttributeModifier.Operation.ADDITION) ? value * 100 : value;

		String key = isPositive ? "attribute.modifier.plus." + modifier.getOperation().toValue() : "attribute.modifier.take." + modifier.getOperation().toValue();
		String formattedValue = FORMAT.format(Math.abs(displayValue)) + suffix;
		ChatFormatting color = isPositive ? ChatFormatting.BLUE : ChatFormatting.RED;

		return Component.translatable(key, formattedValue, Component.translatable(attribute.getDescriptionId())).withStyle(color);
	}

	public static MutableComponent listHeader() {
		return Component.literal(" \u2507 ").withStyle(ChatFormatting.GRAY);
	}
}