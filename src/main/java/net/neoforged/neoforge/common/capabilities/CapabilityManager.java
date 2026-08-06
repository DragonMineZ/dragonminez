package com.dragonminez.compat.capabilities;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compatibility shim for Forge CapabilityManager.get(token).
 */
public final class CapabilityManager {
	private static final Map<Class<?>, Capability<?>> CAPS = new ConcurrentHashMap<>();

	private CapabilityManager() {}

	@SuppressWarnings("unchecked")
	public static <T> Capability<T> get(CapabilityToken<T> token) {
		Class<T> type = token.capture();
		return (Capability<T>) CAPS.computeIfAbsent(type, c -> new Capability<>(type, type.getName()));
	}
}
