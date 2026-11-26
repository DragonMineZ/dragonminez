package com.dragonminez.compat.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Adapts the old Forge overlay callback shape onto NeoForge 1.21's verified layered GUI API. */
@FunctionalInterface
public interface LegacyGuiOverlay {
    void render(LegacyForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height);

    static LayeredDraw.Layer asLayer(LegacyGuiOverlay overlay) {
        return (graphics, delta) -> overlay.render(null, graphics, delta.getGameTimeDeltaPartialTick(false),
                graphics.guiWidth(), graphics.guiHeight());
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(com.dragonminez.Reference.MOD_ID, path);
    }

    static void registerAboveAll(RegisterGuiLayersEvent event, String path, LegacyGuiOverlay overlay) {
        event.registerAboveAll(id(path), asLayer(overlay));
    }
}
