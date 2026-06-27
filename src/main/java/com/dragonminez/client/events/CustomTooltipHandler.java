package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.tooltip.CustomTooltipNodes;
import com.dragonminez.client.gui.tooltip.TooltipDecor;
import com.dragonminez.client.util.ColorUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.FormattedText;

import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CustomTooltipHandler {

	private static ItemStack lastTooltipItem = ItemStack.EMPTY;

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.isPaused()) return;

		float deltaTime = mc.getDeltaFrameTime() / 50.0f;
		TooltipDecor.updateTimer(deltaTime);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onGatherComponents(RenderTooltipEvent.GatherComponents event) {
		ItemStack stack = event.getItemStack();
		if (stack.isEmpty()) return;

		List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();

		if (!elements.isEmpty()) {
			Either<FormattedText, TooltipComponent> first = elements.get(0);
			if (first.left().isPresent()) {
				elements.set(0, Either.right(new CustomTooltipNodes.HeaderNode(stack, first.left().get())));
			}

			elements.add(1, Either.right(new CustomTooltipNodes.PaddingNode(6)));
			elements.add(2, Either.right(new CustomTooltipNodes.SeparatorNode()));
		}
	}

	@SubscribeEvent
	public static void onTooltipColor(RenderTooltipEvent.Color event) {
		int baseColor = ChatFormatting.WHITE.getColor();

		if (TooltipDecor.forceCustomBorder) {
			baseColor = TooltipDecor.forcedColor;
			TooltipDecor.hasSpecialBorder = true;
		} else {
			ItemStack stack = event.getItemStack();
			if (stack.isEmpty()) {
				TooltipDecor.hasSpecialBorder = false;
				return;
			}

			if (!ItemStack.isSameItemSameTags(stack, lastTooltipItem)) {
				TooltipDecor.resetTimer();
				lastTooltipItem = stack.copy();
			}

			TextColor nameColor = stack.getHoverName().getStyle().getColor();
			Integer legacyColor = getLegacyFormattingColor(stack.getHoverName());

			if (nameColor != null) {
				baseColor = nameColor.getValue();
			} else if (legacyColor != null) {
				baseColor = legacyColor;
			} else if (stack.getRarity() != null && stack.getRarity().color != null) {
				Integer rColor = stack.getRarity().color.getColor();
				if (rColor != null) baseColor = rColor;
			}

			if (baseColor == ChatFormatting.WHITE.getColor() || baseColor == ChatFormatting.GRAY.getColor()) {
				baseColor = 0x99AABB;
			}

			TooltipDecor.hasSpecialBorder = true;
		}

		float[] hsv = ColorUtils.rgbToHsv((baseColor >> 16) & 0xFF, (baseColor >> 8) & 0xFF, baseColor & 0xFF);
		float hue = hsv[0];
		boolean addHue = (hue >= 62 && hue <= 240);

		float startHue = (addHue ? hue - 4 : hue + 4 + 360) % 360;
		float endHue = (addHue ? hue + 18 : hue - 18 + 360) % 360;
		float startBGHue = (addHue ? hue - 3 : hue + 3 + 360) % 360;
		float endBGHue = (addHue ? hue + 13 : hue - 13 + 360) % 360;

		int[] startColorRGB = ColorUtils.hsvToRgb(startHue, hsv[1], hsv[2]);
		int[] endColorRGB = ColorUtils.hsvToRgb(endHue, hsv[1], hsv[2] * 0.95f);
		int[] startBgRGB = ColorUtils.hsvToRgb(startBGHue, hsv[1] * 0.9f, 14f);
		int[] endBgRGB = ColorUtils.hsvToRgb(endBGHue, hsv[1] * 0.8f, 18f);

		int borderStart = TooltipDecor.combineARGB(0xFF, startColorRGB[0], startColorRGB[1], startColorRGB[2]);
		int borderEnd = TooltipDecor.combineARGB(0xFF, endColorRGB[0], endColorRGB[1], endColorRGB[2]);
		int bgStart = TooltipDecor.combineARGB(0xE4, startBgRGB[0], startBgRGB[1], startBgRGB[2]);
		int bgEnd = TooltipDecor.combineARGB(0xFD, endBgRGB[0], endBgRGB[1], endBgRGB[2]);

		TooltipDecor.currentBorderStart = borderStart;
		TooltipDecor.currentBorderEnd = borderEnd;
		TooltipDecor.currentBackgroundStart = bgStart;
		TooltipDecor.currentBackgroundEnd = bgEnd;

		event.setBorderStart(borderStart);
		event.setBorderEnd(borderEnd);
		event.setBackgroundStart(bgStart);
		event.setBackgroundEnd(bgEnd);
	}

	private static Integer getLegacyFormattingColor(Component component) {
		String rawText = component.getString();
		for (int i = 0; i < rawText.length() - 1; i++) {
			if (rawText.charAt(i) == '\u00a7') {
				ChatFormatting format = ChatFormatting.getByCode(rawText.charAt(i + 1));
				if (format != null && format.isColor() && format.getColor() != null) {
					return format.getColor();
				}
			}
		}
		return null;
	}
}