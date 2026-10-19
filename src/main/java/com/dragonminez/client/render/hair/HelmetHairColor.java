package com.dragonminez.client.render.hair;

import com.dragonminez.client.render.compat.CosmeticArmorCompat;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.lists.SaiyanForms;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Function;

public final class HelmetHairColor {
	private static final float[] BROLY_Z_CYAN = ColorUtils.hexToRgb("#25C0CB");

	private HelmetHairColor() {}

	public static Function<FormConfig.FormData, float[]> hair(StatsData stats) {
		return hair(stats != null ? stats.getPlayer() : null);
	}

	public static Function<FormConfig.FormData, float[]> hair(Player player) {
		return wearsBrolyZHelmet(player) ? HelmetHairColor::brolyZHair : FormConfig.FormData::getRgbHairColor;
	}

	public static Function<FormConfig.FormData, float[]> bodyColor2(StatsData stats) {
		return wearsBrolyZHelmet(stats != null ? stats.getPlayer() : null) ? HelmetHairColor::brolyZBodyColor2 : FormConfig.FormData::getRgbBodyColor2;
	}

	private static float[] brolyZHair(FormConfig.FormData form) {
		return isHybrid(form) ? BROLY_Z_CYAN : form.getRgbHairColor();
	}

	private static float[] brolyZBodyColor2(FormConfig.FormData form) {
		String bodyColor2 = form.getBodyColor2();
		boolean hairColored = bodyColor2 != null && !bodyColor2.isEmpty() && bodyColor2.equalsIgnoreCase(Objects.toString(form.getHairColor(), ""));
		return isHybrid(form) && hairColored ? BROLY_Z_CYAN : form.getRgbBodyColor2();
	}

	private static boolean isHybrid(FormConfig.FormData form) {
		return SaiyanForms.SSJ_HYBRID.equalsIgnoreCase(form.getName());
	}

	private static boolean wearsBrolyZHelmet(Player player) {
		if (player == null) return false;
		ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
		if (CosmeticArmorCompat.isLoaded()) {
			ItemStack cosmetic = CosmeticArmorCompat.getCosmeticStack(player, EquipmentSlot.HEAD);
			if (cosmetic != null) head = cosmetic;
		}
		return !head.isEmpty() && head.is(MainItems.BROLY_Z_ARMOR.get(ArmorItem.Type.HELMET).get());
	}
}
