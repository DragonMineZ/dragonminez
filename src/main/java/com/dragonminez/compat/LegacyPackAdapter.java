package com.dragonminez.compat;

import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.server.packs.repository.PackSource;

/** Bridges the Forge 1.20 built-in pack factory shape to Minecraft 1.21 pack metadata APIs. */
public final class LegacyPackAdapter {
    private LegacyPackAdapter() {}

    public static Pack create(String id, Component title, boolean required, Supplier<PackResources> resources,
                              PackType type, Position position, PackSource source) {
        PackLocationInfo info = new PackLocationInfo(id, title, source, Optional.empty());
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override public PackResources openPrimary(PackLocationInfo ignored) { return resources.get(); }
            @Override public PackResources openFull(PackLocationInfo ignored, Pack.Metadata metadata) { return resources.get(); }
        };
        return Pack.readMetaAndCreate(info, supplier, type,
                new PackSelectionConfig(required, position, true));
    }

    public static PackLocationInfo location(String id, Component title, PackSource source) {
        return new PackLocationInfo(id, title, source, Optional.empty());
    }
}
