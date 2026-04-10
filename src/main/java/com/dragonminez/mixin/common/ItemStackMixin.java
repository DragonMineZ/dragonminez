package com.dragonminez.mixin.common;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@ModifyVariable(method = "hurtAndBreak", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	public int limitArmorDamage(int pAmount) {
		if (((ItemStack) (Object) this).getItem() instanceof ArmorItem) return Math.min(pAmount, 1);
		return pAmount;
	}
}