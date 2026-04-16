package com.dragonminez.mixin.common;

import com.dragonminez.common.combat.logic.weapon.ItemStackNBTWeaponAttributes;
import com.dragonminez.common.combat.weapon.WeaponAttributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ItemStackNBTWeaponAttributes {
	@Unique
	private boolean hasInvalidAttributes = false;

	@Unique
	private WeaponAttributes weaponAttributes;

	@Override
	public boolean hasInvalidAttributes() {
		return this.hasInvalidAttributes;
	}

	@Override
	public void setInvalidAttributes(boolean invalid) {
		this.hasInvalidAttributes = invalid;
	}

	@Override
	public WeaponAttributes getWeaponAttributes() {
		return this.weaponAttributes;
	}

	@Override
	public void setWeaponAttributes(WeaponAttributes weaponAttributes) {
		this.weaponAttributes = weaponAttributes;
	}

	@ModifyVariable(method = "hurtAndBreak", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	public int limitArmorDamage(int pAmount) {
		if (((ItemStack) (Object) this).getItem() instanceof ArmorItem) return Math.min(pAmount, 1);
		return pAmount;
	}
}