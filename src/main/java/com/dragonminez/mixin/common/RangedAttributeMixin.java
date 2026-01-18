package com.dragonminez.mixin.common;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RangedAttribute.class)
public interface RangedAttributeMixin {

	@Accessor("minValue")
	@Mutable
	void setMinValue(double minValue);

	@Accessor("maxValue")
	@Mutable
	void setMaxValue(double maxValue);
}