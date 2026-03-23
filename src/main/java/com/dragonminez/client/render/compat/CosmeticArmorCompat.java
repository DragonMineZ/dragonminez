package com.dragonminez.client.render.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.UUID;

public final class CosmeticArmorCompat {

	private static final boolean LOADED;
	private static Method getCAStacksClient;
	private static Method getStackInSlot;
	private static Method isSkinArmor;

	static {
		boolean loaded = false;
		try {
			Class<?> apiClass = Class.forName("lain.mods.cos.api.CosArmorAPI");
			getCAStacksClient = apiClass.getMethod("getCAStacksClient", UUID.class);

			Class<?> caStacksClass = Class.forName("lain.mods.cos.api.inventory.CAStacksBase");
			getStackInSlot = caStacksClass.getMethod("getStackInSlot", int.class);
			isSkinArmor = caStacksClass.getMethod("isSkinArmor", int.class);

			loaded = true;
		} catch (Exception ignored) {
		}
		LOADED = loaded;
	}

	private CosmeticArmorCompat() {
	}

	public static boolean isLoaded() {
		return LOADED;
	}

	public static ItemStack getCosmeticStack(Player player, EquipmentSlot slot) {
		if (!LOADED) return null;
		try {
			int cosSlot = getCosSlotIndex(slot);
			if (cosSlot < 0) return null;

			Object cosInv = getCAStacksClient.invoke(null, player.getUUID());

			boolean active = (boolean) isSkinArmor.invoke(cosInv, cosSlot);
			if (!active) return null;

			return (ItemStack) getStackInSlot.invoke(cosInv, cosSlot);
		} catch (Exception e) {
			return null;
		}
	}

	private static int getCosSlotIndex(EquipmentSlot slot) {
		return switch (slot) {
			case FEET -> 0;
			case LEGS -> 1;
			case CHEST -> 2;
			case HEAD -> 3;
			default -> -1;
		};
	}
}



