package com.dragonminez.mixin.common;

import com.dragonminez.common.network.C2S.ReserveFirstHotbarSlotC2S;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
	@Shadow @Final public NonNullList<ItemStack> items;
	@Shadow @Final public Player player;

	@Inject(method = "getFreeSlot", at = @At("HEAD"), cancellable = true)
	private void dragonminez$skipReservedHotbarSlot(CallbackInfoReturnable<Integer> cir) {
		if (player == null || !ReserveFirstHotbarSlotC2S.isReserved(player)) return;
		for (int slot = 1; slot < items.size(); slot++) {
			if (items.get(slot).isEmpty()) {
				cir.setReturnValue(slot);
				return;
			}
		}
		cir.setReturnValue(-1);
	}
}
