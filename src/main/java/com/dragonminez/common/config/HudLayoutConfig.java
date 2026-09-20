package com.dragonminez.common.config;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Getter
@Setter
public class HudLayoutConfig {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;
	public static final String FILE_NAME = "hud_layout";

	private String configVersion;
	private String style = GeneralUserConfig.HUD_STYLE_DEFAULT;
	private Map<String, Map<String, HudPlacement>> layout = new LinkedHashMap<>();

	public String getStyle() {
		style = normalizeStyle(style);
		return style;
	}

	public Map<String, Map<String, HudPlacement>> getLayout() {
		if (layout == null) layout = new LinkedHashMap<>();
		return layout;
	}

	public static String normalizeStyle(String raw) {
		String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace("_", " ").replaceAll("\\s+", " ");
		return switch (normalized) {
			case "legacy1", GeneralUserConfig.HUD_STYLE_LEGACY_1 -> GeneralUserConfig.HUD_STYLE_LEGACY_1;
			case "legacy2", GeneralUserConfig.HUD_STYLE_LEGACY_2 -> GeneralUserConfig.HUD_STYLE_LEGACY_2;
			case GeneralUserConfig.HUD_STYLE_MINECRAFT -> GeneralUserConfig.HUD_STYLE_MINECRAFT;
			default -> GeneralUserConfig.HUD_STYLE_DEFAULT;
		};
	}
}
