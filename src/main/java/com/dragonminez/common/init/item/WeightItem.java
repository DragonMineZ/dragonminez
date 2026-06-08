package com.dragonminez.common.init.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WeightItem extends DMZCuriosItem {

	public WeightItem(Properties properties) {
		super(properties, CurioType.WEIGHTS);
	}

	public static int getWeight(ItemStack stack) {
		if (stack.isEmpty()) return 0;
		CompoundTag tag = stack.getTag();
		return tag != null ? tag.getInt("WeightValue") : 0;
	}

	public static void setWeight(ItemStack stack, int weight) {
		stack.getOrCreateTag().putInt("WeightValue", weight);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		int weight = getWeight(stack);
		if (weight > 0) {
			tooltip.add(Component.literal("Weight: " + weight + " kg").withStyle(ChatFormatting.GOLD));
		}
	}
}
