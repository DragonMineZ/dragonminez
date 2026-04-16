package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.common.combat.logic.weapon.WeaponRegistry;
import com.dragonminez.common.combat.weapon.WeaponAttributesHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class WeaponAttributeTooltip {

	@SubscribeEvent
	public static void onItemTooltip(ItemTooltipEvent event) {
		ItemStack itemStack = event.getItemStack();
		List<Component> lines = event.getToolTip();

		var attributes = WeaponRegistry.getAttributes(itemStack);
		if (attributes == null) {
			var container = WeaponAttributesHelper.getContainerFromNBT(itemStack);
			if (container == null || container.attributes() == null) return;
			attributes = container.attributes();
		}

		var lastAttributeLine = 0;
		var firstHandLine = -1;
		Integer lastGreenAttributeIndex = null;

		var attributePrefix = "attribute.modifier";
		var attributeEqualsPrefix = "attribute.modifier.equals.0";
		var handPrefix = "item.modifiers";

		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);
			var content = line.getContents();

			if (content instanceof TranslatableContents translatableText) {
				var key = translatableText.getKey();
				if (key.startsWith(attributePrefix)) lastAttributeLine = i;
				if (firstHandLine < 0 && key.startsWith(handPrefix)) firstHandLine = i;
			} else {
				for (var part : line.getSiblings()) {
					var partContent = part.getContents();
					if (partContent instanceof TranslatableContents translatableText) {
						if (translatableText.getKey().contains(attributeEqualsPrefix)) {
							lastGreenAttributeIndex = i;
						}
						if (translatableText.getKey().startsWith(attributePrefix)) {
							lastAttributeLine = i;
						}
					}
				}
			}
		}

		if (attributes.attackRange() > 0) {
			var operationId = AttributeModifier.Operation.ADDITION.toValue();
			var rangeTranslationKey = "attribute.name.generic.attack_range";
			var rangeValue = attributes.attackRange();

			MutableComponent rangeLine = Component.literal(" ")
					.append(Component.translatable("attribute.modifier.equals." + operationId,
							ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(rangeValue),
							Component.translatable(rangeTranslationKey)))
					.withStyle(ChatFormatting.DARK_GREEN);

			int index = lastGreenAttributeIndex != null ? lastGreenAttributeIndex : lastAttributeLine;
			lines.add(index + 1, rangeLine);
		}

		if (attributes.isTwoHanded() && firstHandLine >= 0) {
			var twoHandedLine = Component.translatable("tooltip.dragonminez.two_handed").withStyle(ChatFormatting.DARK_GRAY);
			lines.add(firstHandLine + 1, twoHandedLine);
		}
	}
}