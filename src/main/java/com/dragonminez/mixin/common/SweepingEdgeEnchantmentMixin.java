package com.dragonminez.mixin.common;

import net.minecraft.world.item.enchantment.SweepingEdgeEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SweepingEdgeEnchantment.class)
public class SweepingEdgeEnchantmentMixin {
	@Inject(method = "getMaxLevel", at = @At("HEAD"), cancellable = true)
	public void dragonminez$getMaxLevel_DisableSweeping(CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(0);
		cir.cancel();
	}
}