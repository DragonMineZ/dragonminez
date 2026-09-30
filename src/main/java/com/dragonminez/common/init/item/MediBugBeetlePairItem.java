package com.dragonminez.common.init.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class MediBugBeetlePairItem extends PothalaPairItem {

	public MediBugBeetlePairItem(Properties properties, Supplier<Item> beetle) {
		super(properties, beetle, beetle);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.dragonminez.medi_bug_beetle_pair.tooltip.split").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.dragonminez.medi_bug_beetle_pair.tooltip.fuse").withStyle(ChatFormatting.GRAY));
		appendPairIdTooltip(stack, tooltip);
	}
}
