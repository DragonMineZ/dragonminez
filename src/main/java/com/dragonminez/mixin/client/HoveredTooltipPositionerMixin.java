package com.dragonminez.mixin.client;

import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DefaultTooltipPositioner.class)
public class HoveredTooltipPositionerMixin {
	@ModifyArg(method = "positionTooltip(IILorg/joml/Vector2i;II)V", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"), index = 1)
	private int dragonminez$preventVanillaClamping(int max) {
		return Integer.MIN_VALUE;
	}
}