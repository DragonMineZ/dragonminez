// SPDX-License-Identifier: GPL-3.0-only
// Clean-room adapter for Forge 1.20.1 tick phases backed by NeoForge 21.1.248 Pre/Post event subclasses.
package com.dragonminez.compat.event;

import net.neoforged.bus.api.Event;

/** Phase query used by migrated listeners that accept NeoForge's tick-event base classes. */
public final class LegacyTickEvents {
    private LegacyTickEvents() {}

    public enum Phase { START, END }

    public static Phase phase(Event event) {
        String eventPhase = event.getClass().getSimpleName();
        return switch (eventPhase) {
            case "Pre" -> Phase.START;
            case "Post" -> Phase.END;
            default -> throw new IllegalArgumentException("Not a NeoForge Pre/Post tick event: " + event.getClass().getName());
        };
    }
}
