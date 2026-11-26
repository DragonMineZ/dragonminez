// SPDX-License-Identifier: GPL-3.0-only
// Clean compatibility adapter for the removed two-argument ResourceLocation constructor.
// Delegates to Minecraft 1.21.1's verified factory and preserves its validation behavior.
package com.dragonminez.compat;

import net.minecraft.resources.ResourceLocation;

public final class LegacyResourceLocation {
    private LegacyResourceLocation() {
    }

    public static ResourceLocation of(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static ResourceLocation parse(String id) {
        return ResourceLocation.parse(id);
    }
}
