package com.dragonminez.client.gui.tooltip.dynamic;

import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.init.MainEnchants;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
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

	public static final Set<Holder<Attribute>> PERCENT_ATTRIBUTES = Set.of(
			Attributes.MOVEMENT_SPEED,
			Attributes.KNOCKBACK_RESISTANCE
	);

	public static final Comparator<AttributeModifier> ATTRIBUTE_MODIFIER_COMPARATOR =
			Comparator.comparing(AttributeModifier::operation)
					.thenComparing((AttributeModifier a) -> -Math.abs(a.amount()))
					.thenComparing(AttributeModifier::id);

	public static boolean isPercentAttribute(Holder<Attribute> attribute) {
		return PERCENT_ATTRIBUTES.contains(attribute) ||
				attribute.equals(MainAttributes.CRIT_CHANCE) ||
				attribute.equals(MainAttributes.CRIT_DAMAGE);
	}

	public static boolean isPercentAttribute(Attribute attribute) {
		if (attribute == null) return false;
		if (attribute.equals(Attributes.MOVEMENT_SPEED.value()) || attribute.equals(Attributes.KNOCKBACK_RESISTANCE.value())) {
			return true;
		}
		return attribute.equals(MainAttributes.CRIT_CHANCE.get()) || attribute.equals(MainAttributes.CRIT_DAMAGE.get());
	}

	public static boolean processVanillaAttributes(ItemStack stack, Consumer<Component> tooltip, @Nullable Player player) {
		boolean needsShiftPrompt = false;

		ItemAttributeModifiers actualModifiers = stack.getAttributeModifiers();
		ItemAttributeModifiers defaultModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		if (defaultModifiers.modifiers().isEmpty()) {
			defaultModifiers = stack.getItem().getDefaultAttributeModifiers(stack);
		}

		EquipmentSlot[] slots = {EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND};

		for (EquipmentSlot slot : slots) {
			Multimap<Holder<Attribute>, AttributeModifier> actual = collectForSlot(actualModifiers, slot);
			Multimap<Holder<Attribute>, AttributeModifier> defaults = collectForSlot(defaultModifiers, slot);

			float enchantDamage = 0;
			double critChanceBonus = 0;
			double critDamageBonus = 0;

			if (slot == EquipmentSlot.MAINHAND) {
				// MobType removed in 1.21; enchantment damage bonus is effect-driven now.
				enchantDamage = 0;

				int chanceLevel = MainEnchants.level(stack, Minecraft.getInstance().level, MainEnchants.CRIT_CHANCE);
				if (chanceLevel > 0) critChanceBonus = chanceLevel * 0.05D;

				int damageLevel = MainEnchants.level(stack, Minecraft.getInstance().level, MainEnchants.CRIT_DAMAGE);
				if (damageLevel > 0) critDamageBonus = damageLevel * 0.05D;
			}

			if (actual.isEmpty() && enchantDamage <= 0 && critChanceBonus <= 0 && critDamageBonus <= 0) continue;
			tooltip.accept(Component.translatable("item.modifiers." + slot.getName()).withStyle(ChatFormatting.GRAY));

			Set<Holder<Attribute>> allAttributes = new LinkedHashSet<>(actual.keySet());
			if (enchantDamage > 0) allAttributes.add(Attributes.ATTACK_DAMAGE);
			if (critChanceBonus > 0) allAttributes.add(MainAttributes.CRIT_CHANCE);
			if (critDamageBonus > 0) allAttributes.add(MainAttributes.CRIT_DAMAGE);

			for (Holder<Attribute> attr : allAttributes) {
				if (attr.equals(Attributes.BLOCK_INTERACTION_RANGE) || attr.equals(Attributes.ENTITY_INTERACTION_RANGE)) continue;

				List<AttributeModifier> baseMods = new ArrayList<>();
				List<AttributeModifier> extraMods = new ArrayList<>();

				for (AttributeModifier mod : actual.get(attr)) {
					if (defaults.containsEntry(attr, mod)) baseMods.add(mod);
					else extraMods.add(mod);
				}

				if (attr.equals(Attributes.ATTACK_DAMAGE) && enchantDamage > 0) {
					extraMods.add(com.dragonminez.common.util.AttributeMods.of(UUID.randomUUID(), "Enchantment Damage", enchantDamage, AttributeModifier.Operation.ADD_VALUE));
				}
				if (attr.equals(MainAttributes.CRIT_CHANCE) && critChanceBonus > 0) {
					extraMods.add(com.dragonminez.common.util.AttributeMods.of(UUID.randomUUID(), "Enchantment Crit Chance", critChanceBonus, AttributeModifier.Operation.ADD_VALUE));
				}
				if (attr.equals(MainAttributes.CRIT_DAMAGE) && critDamageBonus > 0) {
					extraMods.add(com.dragonminez.common.util.AttributeMods.of(UUID.randomUUID(), "Enchantment Crit Damage", critDamageBonus, AttributeModifier.Operation.ADD_VALUE));
				}

				double playerBase = player != null && player.getAttributes().hasAttribute(attr) ? player.getAttributeBaseValue(attr) : 0.0;
				if (attr.equals(Attributes.ATTACK_DAMAGE)) playerBase = 1.0;
				if (attr.equals(Attributes.ATTACK_SPEED)) playerBase = 4.0;
				if (attr.equals(MainAttributes.CRIT_CHANCE)) playerBase = 0.05;
				if (attr.equals(MainAttributes.CRIT_DAMAGE)) playerBase = 1.5;

				double trueBase = playerBase;
				for (AttributeModifier mod : baseMods) {
					if (mod.operation() == AttributeModifier.Operation.ADD_VALUE) trueBase += mod.amount();
					else if (mod.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) trueBase += playerBase * mod.amount();
					else if (mod.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) trueBase *= (1.0 + mod.amount());
				}

				double finalValue = trueBase;
				extraMods.sort(ATTRIBUTE_MODIFIER_COMPARATOR);
				for (AttributeModifier mod : extraMods) {
					if (mod.operation() == AttributeModifier.Operation.ADD_VALUE) finalValue += mod.amount();
					else if (mod.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) finalValue += trueBase * mod.amount();
					else if (mod.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) finalValue *= (1.0 + mod.amount());
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

	private static Multimap<Holder<Attribute>, AttributeModifier> collectForSlot(ItemAttributeModifiers modifiers, EquipmentSlot slot) {
		Multimap<Holder<Attribute>, AttributeModifier> map = com.google.common.collect.HashMultimap.<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier>create();
		if (modifiers == null) return map;
		modifiers.forEach(slot, map::put);
		return map;
	}

	public static MutableComponent createTotalComponent(Holder<Attribute> attribute, double value) {
		boolean percent = isPercentAttribute(attribute);
		String suffix = percent ? "%" : "";
		double displayValue = percent ? value * 100 : value;

		Component rawAttrDesc = Component.translatable(attribute.value().getDescriptionId());
		Component attrDescNoIcon = IconUtil.getAttributeNameWithoutIcon(rawAttrDesc);

		Component coloredStat = Component.translatable("attribute.modifier.equals.0", FORMAT.format(displayValue) + suffix, attrDescNoIcon);
		Component finalStat = IconUtil.processIcon(rawAttrDesc, coloredStat);

		return Component.empty().append(finalStat);
	}

	public static MutableComponent createTotalComponent(Attribute attribute, double value) {
		boolean percent = isPercentAttribute(attribute);
		String suffix = percent ? "%" : "";
		double displayValue = percent ? value * 100 : value;

		Component rawAttrDesc = Component.translatable(attribute.getDescriptionId());
		Component attrDescNoIcon = IconUtil.getAttributeNameWithoutIcon(rawAttrDesc);

		Component coloredStat = Component.translatable("attribute.modifier.equals.0", FORMAT.format(displayValue) + suffix, attrDescNoIcon);
		Component finalStat = IconUtil.processIcon(rawAttrDesc, coloredStat);

		return Component.empty().append(finalStat);
	}

	public static MutableComponent createModifierComponent(Holder<Attribute> attribute, AttributeModifier modifier) {
		double value = modifier.amount();
		boolean isPositive = value > 0;
		boolean percent = isPercentAttribute(attribute);

		String suffix = percent ? "%" : "";
		double displayValue = (percent || modifier.operation() != AttributeModifier.Operation.ADD_VALUE) ? value * 100 : value;

		String key = isPositive ? "attribute.modifier.plus." + modifier.operation().id() : "attribute.modifier.take." + modifier.operation().id();
		String formattedValue = FORMAT.format(Math.abs(displayValue)) + suffix;
		ChatFormatting color = isPositive ? ChatFormatting.BLUE : ChatFormatting.RED;

		Component rawAttrDesc = Component.translatable(attribute.value().getDescriptionId());
		Component attrDescNoIcon = IconUtil.getAttributeNameWithoutIcon(rawAttrDesc);

		Component coloredStat = Component.translatable(key, formattedValue, attrDescNoIcon).withStyle(color);
		Component finalStat = IconUtil.processIcon(rawAttrDesc, coloredStat);

		return Component.empty().append(finalStat);
	}

	public static MutableComponent createModifierComponent(Attribute attribute, AttributeModifier modifier) {
		double value = modifier.amount();
		boolean isPositive = value > 0;
		boolean percent = isPercentAttribute(attribute);

		String suffix = percent ? "%" : "";
		double displayValue = (percent || modifier.operation() != AttributeModifier.Operation.ADD_VALUE) ? value * 100 : value;

		String key = isPositive ? "attribute.modifier.plus." + modifier.operation().id() : "attribute.modifier.take." + modifier.operation().id();
		String formattedValue = FORMAT.format(Math.abs(displayValue)) + suffix;
		ChatFormatting color = isPositive ? ChatFormatting.BLUE : ChatFormatting.RED;

		Component rawAttrDesc = Component.translatable(attribute.getDescriptionId());
		Component attrDescNoIcon = IconUtil.getAttributeNameWithoutIcon(rawAttrDesc);

		Component coloredStat = Component.translatable(key, formattedValue, attrDescNoIcon).withStyle(color);
		Component finalStat = IconUtil.processIcon(rawAttrDesc, coloredStat);

		return Component.empty().append(finalStat);
	}

	public static MutableComponent listHeader() {
		return Component.literal(" \u2507 ").withStyle(ChatFormatting.GRAY);
	}
}
