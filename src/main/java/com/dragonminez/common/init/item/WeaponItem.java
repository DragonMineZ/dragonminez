package com.dragonminez.common.init.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WeaponItem extends SwordItem {
	private final String tag;

	public WeaponItem(int damageBase, float attackSpeed, int durability, String tag) {
		super(ToolTiers.BLANK_WEAPON_TIER, damageBase, attackSpeed, new Properties().durability(durability));
		this.tag = tag;
	}

	@Override
	public @NotNull Component getName(@NotNull ItemStack pStack) {
		return Component.translatable("dmz.weapons." + this.tag);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
		pTooltipComponents.add(Component.translatable("dmz.weapons." + this.tag + ".tooltip").withStyle(ChatFormatting.GRAY));
	}
}
