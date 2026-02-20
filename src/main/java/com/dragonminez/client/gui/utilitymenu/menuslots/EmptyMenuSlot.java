package com.dragonminez.client.gui.utilitymenu.menuslots;

import com.dragonminez.client.gui.utilitymenu.AbstractMenuSlot;
import com.dragonminez.client.gui.utilitymenu.ButtonInfo;
import com.dragonminez.client.gui.utilitymenu.IUtilityMenuSlot;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.ActionMode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class EmptyMenuSlot extends AbstractMenuSlot implements IUtilityMenuSlot {
    @Override
    public ButtonInfo render(StatsData statsData) {
        return new ButtonInfo();
    }

    @Override
    public void handle(StatsData statsData, boolean rightClick) {
    }
}
