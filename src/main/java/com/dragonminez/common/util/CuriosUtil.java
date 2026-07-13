package com.dragonminez.common.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.List;
import java.util.Optional;

public final class CuriosUtil {

	private CuriosUtil() {}

	public static ItemStack getFirstStackForItem(LivingEntity entity, String slotId, String item) {
        LazyOptional<ICuriosItemHandler> curiosInventory = CuriosApi.getCuriosInventory(entity);
		if (!curiosInventory.isPresent()) return ItemStack.EMPTY;

		Optional<ICuriosItemHandler> optionalICuriosItemHandler = curiosInventory.resolve();
		if (optionalICuriosItemHandler.isEmpty()) return ItemStack.EMPTY;

		ICuriosItemHandler curiosItemHandler = optionalICuriosItemHandler.get();
		List<SlotResult> slotResultList = curiosItemHandler.findCurios(slotId);

		Optional<SlotResult> first = slotResultList.stream().filter(slotResult -> slotResult.stack().getDescriptionId().contains(item)).findFirst();
		if (first.isEmpty()) return ItemStack.EMPTY;

		return first.get().stack();
	}
}
