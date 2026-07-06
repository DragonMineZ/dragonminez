package com.dragonminez.common.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

public final class CuriosUtil {

	private CuriosUtil() {}

	public static ItemStack getStack(LivingEntity entity, String slotId, int index) {
		if (entity == null) return ItemStack.EMPTY;
		return CuriosApi.getCuriosInventory(entity)
				.map(inv -> inv.findCurio(slotId, index).map(SlotResult::stack).orElse(ItemStack.EMPTY))
				.orElse(ItemStack.EMPTY);
	}

	public static ItemStack getFirstStack(LivingEntity entity, String slotId) {
		return getStack(entity, slotId, 0);
	}
}
