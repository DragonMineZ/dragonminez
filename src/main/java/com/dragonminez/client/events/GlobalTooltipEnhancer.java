package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.tooltip.dynamic.*;
import com.dragonminez.common.combat.logic.weapon.WeaponRegistry;
import com.dragonminez.common.combat.weapon.WeaponAttributesHelper;
import com.dragonminez.common.combat.weapon.WeaponAttributes;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GlobalTooltipEnhancer {

	@SubscribeEvent
	public static void onItemTooltip(ItemTooltipEvent event) {
		ItemStack itemStack = event.getItemStack();
		List<Component> lines = event.getToolTip();

		boolean needsShiftPrompt = false;

		EnchantmentColorHandler.colorizeEnchantmentNames(itemStack, lines);
		Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(itemStack);

		if (!enchantments.isEmpty()) {
			boolean isEnchantmentBook = itemStack.getItem() == Items.ENCHANTED_BOOK;
			boolean shouldShowDescriptions = Screen.hasShiftDown() || isEnchantmentBook;

			if (!isEnchantmentBook) needsShiftPrompt = true;

			if (shouldShowDescriptions) {
				for (int i = 0; i < lines.size(); i++) {
					Component line = lines.get(i);
					if (line.getContents() instanceof TranslatableContents t && t.getKey().startsWith("enchantment.")) {
						for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
							if (t.getKey().equals(entry.getKey().getDescriptionId())) {
								List<Component> descList = new ArrayList<>();
								EnchantmentTooltipHandler.insertDescription(entry.getKey(), entry.getValue(), descList::add);
								if (!descList.isEmpty()) {
									lines.addAll(i + 1, descList);
									i += descList.size();
								}
								break;
							}
						}
					}
				}
			}
		}

		int attributeInsertIndex = -1;
		List<Integer> linesToRemove = new ArrayList<>();

		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);
			if (isVanillaAttributeLine(line)) {
				if (attributeInsertIndex == -1) attributeInsertIndex = i;
				linesToRemove.add(i);
			}
		}

		if (attributeInsertIndex == -1) attributeInsertIndex = findAdvancedTooltipStart(lines, itemStack);
		for (int i = linesToRemove.size() - 1; i >= 0; i--) lines.remove((int) linesToRemove.get(i));
		List<Component> attributeLines = new ArrayList<>();

		boolean attrPrompt = AttributeTooltipHandler.processVanillaAttributes(itemStack, attributeLines::add, event.getEntity());
		if (attrPrompt) needsShiftPrompt = true;

		WeaponAttributes weaponAttrs = WeaponRegistry.getAttributes(itemStack);
		if (weaponAttrs == null) {
			var container = WeaponAttributesHelper.getContainerFromNBT(itemStack);
			if (container != null) weaponAttrs = container.attributes();
		}

		boolean customAttrPrompt = WeaponRangeTooltipHandler.appendWeaponAttributes(weaponAttrs, attributeLines::add);
		if (customAttrPrompt) needsShiftPrompt = true;

		cleanRedundantHeaders(attributeLines);

		if (needsShiftPrompt && !Screen.hasShiftDown()) attributeLines.add(0, TooltipPromptHandler.getExpandPrompt());

		lines.addAll(attributeInsertIndex, attributeLines);
	}

	private static boolean isVanillaAttributeLine(Component line) {
		if (line.getContents() instanceof TranslatableContents t) {
			String key = t.getKey();
			if (key.startsWith("item.modifiers.") || key.startsWith("attribute.modifier.")) return true;
		}
		for (Component sibling : line.getSiblings()) {
			if (sibling.getContents() instanceof TranslatableContents tSibling) {
				String key = tSibling.getKey();
				if (key.startsWith("item.modifiers.") || key.startsWith("attribute.modifier.")) return true;
			}
		}
		return false;
	}

	private static int findAdvancedTooltipStart(List<Component> lines, ItemStack stack) {
		String registryName = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();

		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);
			if (line.getContents() instanceof TranslatableContents t) {
				if (t.getKey().equals("item.durability") || t.getKey().equals("item.nbt_tags")) return i;
			}

			if (line.getString().contains(registryName)) return i;
		}
		return lines.size();
	}

	private static void cleanRedundantHeaders(List<Component> lines) {
		int firstHeaderIndex = -1;
		List<Integer> headersToRemove = new ArrayList<>();

		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).getContents() instanceof TranslatableContents translatable) {
				if (translatable.getKey().startsWith("item.modifiers.")) {
					if (firstHeaderIndex == -1) firstHeaderIndex = i;
					else headersToRemove.add(i);
				}
			}
		}

		for (int i = headersToRemove.size() - 1; i >= 0; i--) lines.remove((int) headersToRemove.get(i));
	}
}