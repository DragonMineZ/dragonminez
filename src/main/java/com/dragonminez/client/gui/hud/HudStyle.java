package com.dragonminez.client.gui.hud;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralUserConfig;

public enum HudStyle {
	LEGACY_1(GeneralUserConfig.HUD_STYLE_LEGACY_1),
	LEGACY_2(GeneralUserConfig.HUD_STYLE_LEGACY_2),
	DEFAULT(GeneralUserConfig.HUD_STYLE_DEFAULT),
	MINECRAFT(GeneralUserConfig.HUD_STYLE_MINECRAFT);

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
		return DEFAULT;
	}

	public static HudStyle current() {
		return fromConfigName(ConfigManager.getHudLayoutConfig().getStyle());
	}
}
