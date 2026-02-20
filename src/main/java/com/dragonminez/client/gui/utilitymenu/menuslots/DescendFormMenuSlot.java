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

public class DescendFormMenuSlot extends AbstractMenuSlot implements IUtilityMenuSlot {
    @Override
    public ButtonInfo render(StatsData statsData) {
        ActionMode currentMode = statsData.getStatus().getSelectedAction();
        String race = statsData.getCharacter().getRaceName();

        if ("frostdemon".equals(race) || "majin".equals(race) || "bioandroid".equals(race)) {
            return new ButtonInfo(
                    Component.translatable("gui.action.dragonminez.descend").withStyle(ChatFormatting.BOLD),
                    Component.translatable("gui.action.dragonminez.revert_form")
            );
        } else {
            return new ButtonInfo();
        }
    }

    @Override
    public void handle(StatsData statsData, boolean rightClick) {
        String race = statsData.getCharacter().getRaceName();
        if ("frostdemon".equals(race) || "majin".equals(race) || "bioandroid".equals(race)) {
            NetworkHandler.sendToServer(new ExecuteActionC2S("force_descend", rightClick));
            playToggleSound(false);
        }
    }
}
