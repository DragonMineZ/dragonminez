package com.dragonminez.mixin.common;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements IBattlePower {
	@Shadow public abstract float getMaxHealth();
	@Shadow public abstract AttributeMap getAttributes();

	@Unique
	public int battlePower = 0;

	@Override
	public int getBattlePower() {
		if (this.battlePower == 0) {
			this.battlePower = (int) (this.getMaxHealth() + this.getAttributes().getBaseValue(Attributes.ATTACK_DAMAGE) * 5);
		}
		return this.battlePower;
	}

	@Override
	public void setBattlePower(int power) {
		this.battlePower = power;
	}
}
