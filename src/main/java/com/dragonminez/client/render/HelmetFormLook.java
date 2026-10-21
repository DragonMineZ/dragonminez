package com.dragonminez.client.render;

import com.dragonminez.client.render.compat.CosmeticArmorCompat;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.util.lists.SaiyanForms;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public final class HelmetFormLook {
	private static final float[] BROLY_Z_CYAN = ColorUtils.hexToRgb("#25C0CB");
	private static final Look BROLY_Z_IKARI = new Look(BROLY_Z_CYAN, "#FFD700", "#8C7600");
	private static final Look BROLY_Z_HYBRID = new Look(null, "#FFD700", "#8C7600");
	private static final Look BROLY_Z_FULL_POWER = new Look(null, "#B5FF20", "#415C0C");

	private record Look(float[] hair, String aura, float[] auraRgb, String auraNoise) {
		Look(float[] hair, String aura, String auraNoise) {
			this(hair, aura, ColorUtils.hexToRgb(aura), auraNoise);
		}
	}

	private HelmetFormLook() {}

	public static Function<FormConfig.FormData, float[]> hair(StatsData stats) {
		return hair(stats != null ? stats.getPlayer() : null);
	}

	public static Function<FormConfig.FormData, float[]> hair(Player player) {
		return wearsBrolyZHelmet(player) ? HelmetFormLook::brolyZHair : FormConfig.FormData::getRgbHairColor;
	}

	public static String auraColor(Player player, FormConfig.FormData form) {
		Look look = look(player, form);
		if (look != null) return look.aura();
		return form != null ? form.getAuraColor() : null;
	}

	public static float[] rgbAuraColor(Player player, FormConfig.FormData form) {
		Look look = look(player, form);
		if (look != null) return look.auraRgb();
		return form != null ? form.getRgbAuraColor() : null;
	}

	public static String activeAuraColor(StatsData stats) {
		Character character = stats.getCharacter();
		FormConfig.FormData stack = character.hasActiveStackForm() ? character.getActiveStackFormData() : null;
		if (stack == null || stack.getAuraColor() == null || stack.getAuraColor().isEmpty()) {
			String form = character.hasActiveForm() ? auraColor(stats.getPlayer(), character.getActiveFormData()) : null;
			if (form != null && !form.isEmpty()) return form;
		}
		return character.getActiveAuraColor();
	}

	public static FormConfig.Aura3DStyle aura3DStyle(Player player, FormConfig.FormData form) {
		if (form == null) return null;
		Look look = look(player, form);
		return look != null ? withNoise(form.getAura3DStyle(), look.auraNoise()) : form.getAura3DStyle();
	}

	private static float[] brolyZHair(FormConfig.FormData form) {
		Look look = brolyZ(form);
		return look != null && look.hair() != null ? look.hair() : form.getRgbHairColor();
	}

	private static Look look(Player player, FormConfig.FormData form) {
		return form != null && wearsBrolyZHelmet(player) ? brolyZ(form) : null;
	}

	private static Look brolyZ(FormConfig.FormData form) {
		String name = form.getName();
		if (SaiyanForms.IKARI.equalsIgnoreCase(name)) return BROLY_Z_IKARI;
		if (SaiyanForms.SSJ_HYBRID.equalsIgnoreCase(name)) return BROLY_Z_HYBRID;
		if (SaiyanForms.SSJ_FULL_POWER.equalsIgnoreCase(name)) return BROLY_Z_FULL_POWER;
		return null;
	}

	private static FormConfig.Aura3DStyle withNoise(FormConfig.Aura3DStyle style, String noise) {
		return new FormConfig.Aura3DStyle()
				.size(style.getSizeX(), style.getSizeY(), style.getSizeZ())
				.peaks(style.getFlamePeaks())
				.waves(style.getWaveFrequency(), style.getWaveSpeed(), style.getWaveAmplitude())
				.turbulence(style.getNoiseDetail(), style.getUpwardBias())
				.rim(style.getCoreAlpha(), style.getRimAlpha(), style.getRimPower(), style.getRimThreshold())
				.colors(style.getCoreColor(), style.getRimColor(), noise)
				.bloom(style.getBloomIntensity())
				.spikes(style.getSpikeDensity(), style.getSpikeBurstRate(), style.getSpikeRarity(), style.getBandStart())
				.secondBorder(style.getSecondBorderColor());
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
