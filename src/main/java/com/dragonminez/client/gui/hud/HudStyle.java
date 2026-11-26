package com.dragonminez.client.gui.hud;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.HudLayoutConfig;

public enum HudStyle {
	XENOVERSE(HudLayoutConfig.STYLE_XENOVERSE),
	COMPACT(HudLayoutConfig.STYLE_COMPACT),
	MODERN(HudLayoutConfig.STYLE_MODERN),
	VANILLA(HudLayoutConfig.STYLE_VANILLA);

	private final String configName;

	HudStyle(String configName) {
		this.configName = configName;
	}

	public String configName() {
		return configName;
	}

	public String translationKey() {
		return "gui.dragonminez.hud_editor.style." + configName;
	}

	public static HudStyle fromConfigName(String name) {
		for (HudStyle style : values()) {
			if (style.configName.equals(name)) return style;
		}
		return XENOVERSE;
	}

	public static HudStyle current() {
		return fromConfigName(ConfigManager.getHudLayoutConfig().getStyle());
	}
}
