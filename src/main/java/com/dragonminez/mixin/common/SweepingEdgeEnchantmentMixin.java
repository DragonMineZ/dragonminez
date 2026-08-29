package com.dragonminez.mixin.common;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21: SweepingEdgeEnchantment class no longer exists (data-driven enchantments).
 * Disable sweeping edge damage ratio so DMZ combat stays non-sweeping.
 */
@Mixin(EnchantmentHelper.class)
public class SweepingEdgeEnchantmentMixin {
	@Inject(method = "getSweepingDamageRatio", at = @At("HEAD"), cancellable = true, require = 0)
	private static void dragonminez$disableSweeping(Player player, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(0.0F);
	}
}
