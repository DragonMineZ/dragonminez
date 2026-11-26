package com.dragonminez.compat;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Forge 1.20 event-bus return-value bridge for NeoForge 1.21's event-returning post method. */
public final class LegacyEventAdapter {
    private LegacyEventAdapter() {}

    public static boolean postCancellable(Event event) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        return event instanceof ICancellableEvent cancellable && cancellable.isCanceled();
    }
}
