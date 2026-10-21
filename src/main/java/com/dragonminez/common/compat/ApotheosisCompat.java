package com.dragonminez.common.compat;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public final class ApotheosisCompat {
	public static final String MOD_ID = "apotheosis";
	private static Boolean loaded;
	private ApotheosisCompat() {}

	public static boolean isLoaded() {
		if (loaded == null) loaded = ModList.get().isLoaded(MOD_ID);
		return loaded;
	}

	public static void copyRarity(ItemStack from, ItemStack to) {
		if (!isLoaded()) return;
		Impl.copyRarity(from, to);
	}

	public static void copyAffixes(ItemStack from, ItemStack to) {
		if (!isLoaded()) return;
		Impl.copyAffixes(from, to);
	}

	public static void copySockets(ItemStack from, ItemStack to) {
		if (!isLoaded()) return;
		Impl.copySockets(from, to);
	}

	public static void copyGems(ItemStack from, ItemStack to) {
		if (!isLoaded()) return;
		Impl.copyGems(from, to);
	}

	private static final class Impl {
		static void copyRarity(ItemStack from, ItemStack to) {
			var rarity = AffixHelper.getRarity(from);
			if (rarity.isBound()) AffixHelper.setRarity(to, rarity.get());
		}

		static void copyAffixes(ItemStack from, ItemStack to) {
			AffixHelper.setAffixes(to, AffixHelper.getAffixes(from));
		}

		static void copySockets(ItemStack from, ItemStack to) {
			SocketHelper.setSockets(to, SocketHelper.getSockets(from));
		}

		static void copyGems(ItemStack from, ItemStack to) {
			SocketHelper.setGems(to, SocketHelper.getGems(from));
		}
	}
}
