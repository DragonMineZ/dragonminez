package com.dragonminez.mixin.common;

import com.dragonminez.common.network.C2S.ReserveFirstHotbarSlotC2S;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
	@Redirect(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
	private boolean dragonminez$reserveFirstHotbarSlot(Inventory inventory, ItemStack stack, Player player) {
		if (!player.getPersistentData().getBoolean(ReserveFirstHotbarSlotC2S.PLAYER_DATA_KEY)) return inventory.add(stack);

		boolean addedAny = false;
		for (int slot = 1; slot < inventory.items.size() && !stack.isEmpty(); slot++) {
			ItemStack existing = inventory.getItem(slot);
			if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, stack)) continue;

			int capacity = Math.min(existing.getMaxStackSize(), stack.getMaxStackSize()) - existing.getCount();
			int moved = Math.min(stack.getCount(), capacity);
			if (moved <= 0) continue;
			existing.grow(moved);
			stack.shrink(moved);
			inventory.setChanged();
			addedAny = true;
		}

		for (int slot = 1; slot < inventory.items.size() && !stack.isEmpty(); slot++) {
			if (!inventory.getItem(slot).isEmpty()) continue;

			int moved = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize()));
			inventory.setItem(slot, stack.split(moved));
			addedAny = true;
		}

		return addedAny;
	}
}
