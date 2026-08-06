package com.dragonminez.compat.capabilities;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Compatibility shim. Registration is a no-op; providers attach at AttachCapabilitiesEvent time.
 */
public class RegisterCapabilitiesEvent extends Event implements IModBusEvent {
	public void register(Class<?> type) {
		// no-op under NeoForge 1.21 capability model
	}
}
