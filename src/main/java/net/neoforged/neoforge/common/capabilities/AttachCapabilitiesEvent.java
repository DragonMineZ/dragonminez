package com.dragonminez.compat.capabilities;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Compatibility shim for Forge AttachCapabilitiesEvent.
 * Providers are stored in-memory and resolved via entity-side lookups in StatsProvider helpers.
 */
public class AttachCapabilitiesEvent<T> extends Event {
	private final T object;
	private final Map<ResourceLocation, ICapabilityProvider> caps = new HashMap<>();

	public AttachCapabilitiesEvent(T object) {
		this.object = object;
	}

	public T getObject() {
		return object;
	}

	public void addCapability(ResourceLocation key, ICapabilityProvider cap) {
		caps.put(key, cap);
	}

	public Map<ResourceLocation, ICapabilityProvider> getCapabilities() {
		return Collections.unmodifiableMap(caps);
	}
}
