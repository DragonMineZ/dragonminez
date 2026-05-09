package com.dragonminez.client.gui.tooltip;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class CustomTooltipNodes {
	public record HeaderNode(ItemStack stack, FormattedText title) implements TooltipComponent { }
	public record SeparatorNode() implements TooltipComponent { }
	public record PaddingNode(int height) implements TooltipComponent { }
}