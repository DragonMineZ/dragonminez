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
	public static final String STYLE_XENOVERSE = "xenoverse";
	public static final String STYLE_COMPACT = "compact";
	public static final String STYLE_MODERN = "modern";
	public static final String STYLE_VANILLA = "vanilla";

	private static final Map<String, String> LEGACY_GROUPS = Map.of(
			"legacy 1", STYLE_XENOVERSE,
			"legacy 2", STYLE_COMPACT,
			"default", STYLE_MODERN,
			"minecraft", STYLE_VANILLA);
	private static final Map<String, String> LEGACY_ELEMENTS = Map.of(
			"legacy2_health", "compact_health",
			"legacy2_ki", "compact_ki",
			"legacy2_stamina", "compact_stamina");

	private String configVersion;
	private String style = STYLE_XENOVERSE;
	private Map<String, Map<String, HudPlacement>> layout = new LinkedHashMap<>();

	public String getStyle() {
		style = normalizeStyle(style);
		return style;
	}

	public Map<String, Map<String, HudPlacement>> getLayout() {
		if (layout == null) layout = new LinkedHashMap<>();
		return layout;
	}

	public boolean migrateLegacyNames() {
		boolean changed = false;
		String normalized = normalizeStyle(style);
		if (!normalized.equals(style)) {
			style = normalized;
			changed = true;
		}

		Map<String, Map<String, HudPlacement>> renamed = new LinkedHashMap<>();
		for (Map.Entry<String, Map<String, HudPlacement>> group : getLayout().entrySet()) {
			String groupName = LEGACY_GROUPS.getOrDefault(group.getKey(), group.getKey());
			Map<String, HudPlacement> placements = renamed.computeIfAbsent(groupName, key -> new LinkedHashMap<>());
			if (!groupName.equals(group.getKey())) changed = true;
			if (group.getValue() == null) continue;
			for (Map.Entry<String, HudPlacement> placement : group.getValue().entrySet()) {
				String elementName = LEGACY_ELEMENTS.getOrDefault(placement.getKey(), placement.getKey());
				if (!elementName.equals(placement.getKey())) changed = true;
				placements.putIfAbsent(elementName, placement.getValue());
			}
		}
		if (changed) layout = renamed;
		return changed;
	}

	public static String normalizeStyle(String raw) {
		String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace("_", " ").replaceAll("\\s+", " ");
		return switch (normalized) {
			case STYLE_COMPACT, "legacy 2", "legacy2" -> STYLE_COMPACT;
			case STYLE_MODERN, "default" -> STYLE_MODERN;
			case STYLE_VANILLA, "minecraft" -> STYLE_VANILLA;
			default -> STYLE_XENOVERSE;
		};
	}
}
