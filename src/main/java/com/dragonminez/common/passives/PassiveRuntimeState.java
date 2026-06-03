package com.dragonminez.common.passives;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PassiveRuntimeState {

	private static final Map<UUID, PassiveRuntimeState> STATES = new ConcurrentHashMap<>();

	public int warriorStacks = 0;
	public int warriorComboProgress = 0;
	public long warriorStacksExpireTick = 0L;
	public long warriorComboExpireTick = 0L;

	public static PassiveRuntimeState get(UUID id) {
		return STATES.computeIfAbsent(id, k -> new PassiveRuntimeState());
	}

	public static PassiveRuntimeState peek(UUID id) {
		return STATES.get(id);
	}

	public static void clear(UUID id) {
		STATES.remove(id);
	}
}
