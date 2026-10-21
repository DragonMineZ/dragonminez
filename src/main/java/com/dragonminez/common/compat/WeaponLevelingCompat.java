package com.dragonminez.common.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.weaponleveling.api.LevelingAPI;

public final class WeaponLevelingCompat {
	public static final String MOD_ID = "weaponleveling";
	private static Boolean loaded;

	private WeaponLevelingCompat() {}

	public static boolean isLoaded() {
		if (loaded == null) loaded = ModList.get().isLoaded(MOD_ID);
		return loaded;
	}

	public static void copyLevel(ItemStack from, ItemStack to) {
		if (!isLoaded()) return;
		Impl.copyLevel(from, to);
	}

	public static void copyLevelProgress(ItemStack from, ItemStack to) {
		if (!isLoaded()) return;
		Impl.copyLevelProgress(from, to);
	}

	private static final class Impl {
		static void copyLevel(ItemStack from, ItemStack to) {
			LevelingAPI.updateLevel(to, LevelingAPI.getLevel(from));
		}

		static void copyLevelProgress(ItemStack from, ItemStack to) {
			LevelingAPI.updateLevelProgress(to, LevelingAPI.getLevelProgress(from));
		}
	}
}
