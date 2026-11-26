// SPDX-License-Identifier: GPL-3.0-only
// Adapted for XenoPixels Ragnarok from the DMZ 1.20.1 Forge compatibility model.
// NeoForge 21.1 no longer exposes Entity#getCapability(Capability); DragonMineZ 2.2.1
// provides the equivalent lookup through StatsProvider.get(Capability, Entity).
package com.dragonminez.compat.capabilities;

import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.compat.capabilities.Capability;
import com.dragonminez.compat.util.LazyOptional;
import net.minecraft.world.entity.Entity;

/** Restores the legacy entity capability lookup shape for DragonMineZ-owned capabilities. */
public final class LegacyEntityCapabilities {
    private LegacyEntityCapabilities() {
    }

    public static <T> LazyOptional<T> getCapability(Entity entity, Capability<T> capability) {
        return StatsProvider.get(capability, entity);
    }
}
