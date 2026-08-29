package com.dragonminez.compat.capabilities;

/**
 * Compatibility shim: old Forge Capability token type.
 * Real data is stored via NeoForge EntityCapability in {@link CapabilityManager}.
 */
public final class Capability<T> {
	final Class<T> type;
	final String name;

	Capability(Class<T> type, String name) {
		this.type = type;
		this.name = name;
	}

	public Class<T> getType() {
		return type;
	}

	public String getName() {
		return name;
	}
}
