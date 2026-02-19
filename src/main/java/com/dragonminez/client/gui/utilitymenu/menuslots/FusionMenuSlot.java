package com.dragonminez.client.gui.utilitymenu.menuslots;

import com.dragonminez.client.gui.utilitymenu.AbstractMenuSlot;
import com.dragonminez.client.gui.utilitymenu.ButtonInfo;
import com.dragonminez.client.gui.utilitymenu.IUtilityMenuSlot;
import com.dragonminez.common.network.C2S.SwitchActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.ActionMode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class FusionMenuSlot extends AbstractMenuSlot implements IUtilityMenuSlot {
    @Override
    public ButtonInfo render(StatsData statsData) {
        ActionMode currentMode = statsData.getStatus().getSelectedAction();

        if (statsData.getSkills().hasSkill("fusion")) {
            return new ButtonInfo(
                    Component.translatable("gui.action.dragonminez.fusion").withStyle(ChatFormatting.BOLD),
                    Component.translatable("gui.action.dragonminez." + (statsData.getStatus().getSelectedAction() == ActionMode.FUSION ? "true" : "false")),
                    currentMode == ActionMode.FUSION);
        } else {
            return new ButtonInfo();
        }
    }

    @Override
    public void handle(StatsData statsData) {
        if (statsData.getSkills().hasSkill("fusion")) {
            boolean wasActive = statsData.getStatus().getSelectedAction() == ActionMode.FUSION;
            NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.FUSION));
            playToggleSound(!wasActive);
        }
    }
}
