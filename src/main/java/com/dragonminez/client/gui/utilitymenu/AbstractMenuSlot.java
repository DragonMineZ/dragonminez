package com.dragonminez.client.gui.utilitymenu;

import com.dragonminez.common.init.MainSounds;
import net.minecraft.client.Minecraft;

public abstract class AbstractMenuSlot {
    protected void playToggleSound(boolean turnedOn) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (turnedOn) {
                mc.player.playSound(MainSounds.SWITCH_ON.get(), 1.0F, 1.0F);
            } else {
                mc.player.playSound(MainSounds.SWITCH_OFF.get(), 1.0F, 1.0F);
            }
        }
    }
}
