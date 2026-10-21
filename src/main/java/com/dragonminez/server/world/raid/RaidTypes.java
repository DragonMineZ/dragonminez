package com.dragonminez.server.world.raid;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.RaidDefaults;
import com.dragonminez.common.config.RaidDefinition;

import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class RaidTypes {

	private static final Map<String, RaidType> REGISTRY = new LinkedHashMap<>();
	private static List<RaidType> TRIGGERABLE = List.of();
	public static final String DEFAULT_ID = RaidDefaults.SAIYAN_ASSAULT;

	private RaidTypes() {}

	public static void reload(Map<String, RaidDefinition> definitions) {
		REGISTRY.clear();

		for (Map.Entry<String, RaidDefinition> entry : definitions.entrySet()) {
			RaidType type = new RaidType(entry.getKey(), entry.getValue());

			if (!type.isEnabled()) continue;
			if (!type.isUsable()) {
				LogUtil.warn(Env.SERVER, "Raid '{}' defines no waves and was skipped", entry.getKey());
				continue;
			}
			REGISTRY.put(entry.getKey(), type);
		}

		TRIGGERABLE = REGISTRY.values().stream().filter(RaidType::hasTrigger).toList();
		LogUtil.info(Env.SERVER, "Loaded {} raid type(s): {}", REGISTRY.size(), REGISTRY.keySet());
	}

	public static RaidType get(String id) {
		return REGISTRY.get(id);
	}

	public static RaidType getOrDefault(String id) {
		RaidType type = REGISTRY.get(id);
		return type != null ? type : REGISTRY.get(DEFAULT_ID);
	}

	public static boolean contains(String id) {
		return REGISTRY.containsKey(id);
	}

	public static Set<String> ids() {
		return Collections.unmodifiableSet(REGISTRY.keySet());
	}

	public static List<RaidType> triggerable() {
		return TRIGGERABLE;
	}
}
