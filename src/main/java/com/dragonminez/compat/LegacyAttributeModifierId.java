package com.dragonminez.compat;

import java.util.UUID;

import net.minecraft.resources.ResourceLocation;

/** Stable resource-key ids for UUID based modifiers from the Forge 1.20 API. */
public final class LegacyAttributeModifierId {
    private LegacyAttributeModifierId() {}

    public static ResourceLocation from(UUID id) {
        return com.dragonminez.common.util.AttributeMods.id(id);
    }
}
