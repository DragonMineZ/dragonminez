package com.dragonminez.client.gui.tooltip.dynamic;

import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.init.MainEnchants;
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

	public static boolean isPercentAttribute(Attribute attribute) {
		return PERCENT_ATTRIBUTES.contains(attribute) ||
				attribute.equals(MainAttributes.CRIT_CHANCE.get()) ||
				attribute.equals(MainAttributes.CRIT_DAMAGE.get());
	}

	public static boolean processVanillaAttributes(ItemStack stack, Consumer<Component> tooltip, @Nullable Player player) {
		boolean needsShiftPrompt = false;

		EquipmentSlot[] slots = {EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND};

		for (EquipmentSlot slot : slots) {
			Multimap<Attribute, AttributeModifier> actualModifiers = stack.getAttributeModifiers(slot);
			Multimap<Attribute, AttributeModifier> defaultModifiers = stack.getItem().getDefaultAttributeModifiers(slot);

			float enchantDamage = 0;
			double critChanceBonus = 0;
			double critDamageBonus = 0;

			if (slot == EquipmentSlot.MAINHAND) {
				enchantDamage = EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED);

				int chanceLevel = stack.getEnchantmentLevel(MainEnchants.CRIT_CHANCE.get());
				if (chanceLevel > 0) critChanceBonus = chanceLevel * 0.05D;

				int damageLevel = stack.getEnchantmentLevel(MainEnchants.CRIT_DAMAGE.get());
				if (damageLevel > 0) critDamageBonus = damageLevel * 0.05D;
			}

			if (actualModifiers.isEmpty() && enchantDamage <= 0 && critChanceBonus <= 0 && critDamageBonus <= 0) continue;
			tooltip.accept(Component.translatable("item.modifiers." + slot.getName()).withStyle(ChatFormatting.GRAY));

			Set<Attribute> allAttributes = new LinkedHashSet<>(actualModifiers.keySet());
			if (enchantDamage > 0) allAttributes.add(Attributes.ATTACK_DAMAGE);
			if (critChanceBonus > 0) allAttributes.add(MainAttributes.CRIT_CHANCE.get());
			if (critDamageBonus > 0) allAttributes.add(MainAttributes.CRIT_DAMAGE.get());

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
				if (attr.equals(MainAttributes.CRIT_CHANCE.get()) && critChanceBonus > 0) {
					extraMods.add(new AttributeModifier(UUID.randomUUID(), "Enchantment Crit Chance", critChanceBonus, AttributeModifier.Operation.ADDITION));
				}
				if (attr.equals(MainAttributes.CRIT_DAMAGE.get()) && critDamageBonus > 0) {
					extraMods.add(new AttributeModifier(UUID.randomUUID(), "Enchantment Crit Damage", critDamageBonus, AttributeModifier.Operation.ADDITION));
				}

				double playerBase = player != null ? player.getAttributeBaseValue(attr) : 0.0;
				if (attr.equals(Attributes.ATTACK_DAMAGE)) playerBase = 1.0;
				if (attr.equals(Attributes.ATTACK_SPEED)) playerBase = 4.0;
				if (attr.equals(MainAttributes.CRIT_CHANCE.get())) playerBase = 0.05;
				if (attr.equals(MainAttributes.CRIT_DAMAGE.get())) playerBase = 1.5;

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
		boolean percent = isPercentAttribute(attribute);
		String suffix = percent ? "%" : "";
		double displayValue = percent ? value * 100 : value;

		Component rawAttrDesc = Component.translatable(attribute.getDescriptionId());
		Component attrDescNoIcon = IconUtil.getAttributeNameWithoutIcon(rawAttrDesc);

		Component coloredStat = Component.translatable("attribute.modifier.equals.0", FORMAT.format(displayValue) + suffix, attrDescNoIcon);
		Component finalStat = IconUtil.processIcon(rawAttrDesc, coloredStat);

		return Component.empty().append(finalStat);
	}

	public static MutableComponent createModifierComponent(Attribute attribute, AttributeModifier modifier) {
		double value = modifier.getAmount();
		boolean isPositive = value > 0;
		boolean percent = isPercentAttribute(attribute);

		String suffix = percent ? "%" : "";
		double displayValue = (percent || modifier.getOperation() != AttributeModifier.Operation.ADDITION) ? value * 100 : value;

		String key = isPositive ? "attribute.modifier.plus." + modifier.getOperation().toValue() : "attribute.modifier.take." + modifier.getOperation().toValue();
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