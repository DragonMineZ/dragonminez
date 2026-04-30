package com.dragonminez.client.gui.tooltip;

import com.dragonminez.client.util.TooltipUtil;
import com.dragonminez.mixin.client.BundleTooltipComponentAccessor;
import com.dragonminez.mixin.client.ClientTextTooltipAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;
import java.util.List;

public class ScrollTracker {
	private static int targetVerticalScroll = 0;
	private static int targetHorizontalScroll = 0;
	private static float currentVerticalScroll = 0f;
	private static float currentHorizontalScroll = 0f;
	private static List<ClientTooltipComponent> trackedComponents = null;
	public static boolean renderedThisFrame = false;

	private static final int VERTICAL_SENSITIVITY = 15;
	private static final int HORIZONTAL_SENSITIVITY = 15;

	public static void addVerticalScroll(int amt) {
		targetVerticalScroll += -amt * VERTICAL_SENSITIVITY;
	}

	public static void addHorizontalScroll(int amt) {
		targetHorizontalScroll += -amt * HORIZONTAL_SENSITIVITY;
	}

	public static void updateTooltip(List<ClientTooltipComponent> components) {
		if (!isEqual(components, trackedComponents)) reset();
		trackedComponents = components;
	}

	public static void applyScroll(GuiGraphics graphics, int x, int y, int width, int height, int screenWidth, int screenHeight) {
		renderedThisFrame = true;

		if (height < screenHeight) targetVerticalScroll = 0;
		if (width < screenWidth) targetHorizontalScroll = 0;

		targetVerticalScroll = Mth.clamp(targetVerticalScroll, Math.min(screenHeight - (y + height) - 4, 0), Math.max(-y + 4, 0));
		targetHorizontalScroll = Mth.clamp(targetHorizontalScroll, Math.min(screenWidth - (x + width) - 4, 0), Math.max(-x + 4, 0));

		float tickDelta = Minecraft.getInstance().getDeltaFrameTime();
		currentVerticalScroll = Mth.lerp(tickDelta * 0.5f, currentVerticalScroll, targetVerticalScroll);
		currentHorizontalScroll = Mth.lerp(tickDelta * 0.5f, currentHorizontalScroll, targetHorizontalScroll);

		graphics.pose().translate(currentHorizontalScroll, currentVerticalScroll, 0);
	}

	public static void reset() {
		targetVerticalScroll = targetHorizontalScroll = 0;
		currentVerticalScroll = currentHorizontalScroll = 0;
	}

	private static boolean isEqual(List<ClientTooltipComponent> l1, List<ClientTooltipComponent> l2) {
		if (l1 == null || l2 == null) return false;
		Iterator<ClientTooltipComponent> iter1 = l1.iterator();
		Iterator<ClientTooltipComponent> iter2 = l2.iterator();

		while (iter1.hasNext() && iter2.hasNext()) {
			ClientTooltipComponent c1 = iter1.next();
			ClientTooltipComponent c2 = iter2.next();
			if (c1 == c2) continue;

			if (c1 instanceof ClientTextTooltip ot1 && c2 instanceof ClientTextTooltip ot2) {
				if (!TooltipUtil.toText(((ClientTextTooltipAccessor) ot1).getText()).equals(TooltipUtil.toText(((ClientTextTooltipAccessor) ot2).getText()))) return false;
			} else if (c1 instanceof ClientBundleTooltip bt1 && c2 instanceof ClientBundleTooltip bt2) {
				Iterator<ItemStack> i1 = ((BundleTooltipComponentAccessor) bt1).getItems().iterator();
				Iterator<ItemStack> i2 = ((BundleTooltipComponentAccessor) bt2).getItems().iterator();
				while (i1.hasNext() && i2.hasNext()) {
					if (!ItemStack.matches(i1.next(), i2.next())) return false;
				}
				if (i1.hasNext() || i2.hasNext()) return false;
			} else {
				return false;
			}
		}
		return !(iter1.hasNext() || iter2.hasNext());
	}
}