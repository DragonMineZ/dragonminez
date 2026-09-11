package com.dragonminez.client.render.effects;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.world.entity.player.Player;

public final class AuraFxState {

	private AuraFxState() {}

	public static StatsData stats(Player player) {
		return StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
	}

	public static boolean isSurging(StatsData stats) {
		if (stats == null) return false;
		return stats.getStatus().isSurgeActive() || stats.getStatus().isKiBurstArmed();
	}

	public static boolean isSurging(Player player) {
		return isSurging(stats(player));
	}

	public static double auraScaleMultiplier(StatsData stats) {
		if (!isSurging(stats)) return 1.0;
		CombatConfig config = ConfigManager.getCombatConfig();
		return config == null ? 1.0 : config.getSurgeAuraScaleMultiplier();
	}

	public static double auraSpeedMultiplier(Player player) {
		StatsData stats = stats(player);
		if (!isSurging(stats)) return 1.0;
		CombatConfig config = ConfigManager.getCombatConfig();
		return config == null ? 1.0 : config.getSurgeAuraSpeedMultiplier();
	}

	public static boolean hasLightning(StatsData stats) {
		if (stats == null) return false;
		return formLightning(stats) != null || isSurging(stats);
	}

	public static boolean hasLightning(Player player) {
		return hasLightning(stats(player));
	}

	private static final String LIGHTNING_CYAN = "#5CD6FF";
	private static final String LIGHTNING_DEEP_BLUE = "#12277A";
	private static final String LIGHTNING_BLACK = "#111014";

	public static String lightningColor(StatsData stats) {
		if (stats == null) return LIGHTNING_CYAN;

		FormConfig.FormData form = formLightning(stats);
		if (form != null) {
			String color = form.getLightningColor();
			if (color != null && !color.isEmpty()) return color;
		}
		return complementaryLightningColor(stats.getCharacter().getActiveAuraColor());
	}

	public static String complementaryLightningColor(String auraColorHex) {
		float[] hsv = ColorUtils.hexToHsv(auraColorHex != null && !auraColorHex.isEmpty() ? auraColorHex : "#FFFFFF");
		float hue = hsv[0];
		float saturation = hsv[1];

		if (saturation < 15.0f) return LIGHTNING_DEEP_BLUE;
		if (hue >= 45.0f && hue < 170.0f) return LIGHTNING_CYAN;
		if (hue >= 170.0f && hue < 260.0f) return LIGHTNING_DEEP_BLUE;
		return LIGHTNING_BLACK;
	}

	public static float lightningSpeedMultiplier(StatsData stats) {
		if (!isSurging(stats)) return 1.0f;
		CombatConfig config = ConfigManager.getCombatConfig();
		return config == null ? 1.0f : config.getSurgeAuraSpeedMultiplier().floatValue();
	}

	private static FormConfig.FormData formLightning(StatsData stats) {
		Character character = stats.getCharacter();
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null
				&& character.getActiveStackFormData().getHasLightnings()) {
			return character.getActiveStackFormData();
		}
		if (character.hasActiveForm() && character.getActiveFormData() != null
				&& character.getActiveFormData().getHasLightnings()) {
			return character.getActiveFormData();
		}
		return null;
	}
}
