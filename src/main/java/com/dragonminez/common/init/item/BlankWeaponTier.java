package com.dragonminez.common.init.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public class BlankWeaponTier implements Tier {
	private final int uses;
	private final float speed;
	private final float attackDamageBonus;
	private final int level;
	private final int enchantmentValue;
	private final Ingredient repairIngredient;

	public BlankWeaponTier(int uses, float speed, float attackDamageBonus, int level, int enchantmentValue, Ingredient repairIngredient) {
		this.uses = uses;
		this.speed = speed;
		this.attackDamageBonus = attackDamageBonus;
		this.level = level;
		this.enchantmentValue = enchantmentValue;
		this.repairIngredient = repairIngredient;
	}

	@Override
	public int getUses() {
		return this.uses;
	}

	@Override
	public float getSpeed() {
		return this.speed;
	}

	@Override
	public float getAttackDamageBonus() {
		return this.attackDamageBonus;
	}

	@Override
	public int getLevel() {
		return this.level; // Nivel de minería
	}

	@Override
	public int getEnchantmentValue() {
		return this.enchantmentValue;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return this.repairIngredient;
	}
}
