package com.dragonminez.client.gui.hud;

import net.minecraft.client.Minecraft;

public class HUDManager {
    private static int hudGuiScale = -1;

    public static void setHudGuiScale() {
        if (hudGuiScale == -1) {
            hudGuiScale = Minecraft.getInstance().options.guiScale().get();
        }
    }

    public static int getHudGuiScale() {
        if (hudGuiScale == -1) {
            setHudGuiScale();
        }
        return hudGuiScale;
    }

    public static float getScaleFactor() {
        int currentGuiScale = Minecraft.getInstance().options.guiScale().get();
        int originalHudScale = getHudGuiScale();
        if (originalHudScale == 0 || currentGuiScale == 0) return 1.0f;
        return (float) originalHudScale / currentGuiScale;
    }
}
