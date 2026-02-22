package com.dragonminez.client.gui.utilitymenu.menuslots;

import com.dragonminez.client.gui.utilitymenu.AbstractMenuSlot;
import com.dragonminez.client.gui.utilitymenu.ButtonInfo;
import com.dragonminez.client.gui.utilitymenu.IUtilityMenuSlot;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class DescendFormMenuSlot extends AbstractMenuSlot implements IUtilityMenuSlot {
    @Override
    public ButtonInfo render(StatsData statsData) {
        boolean activeForm = statsData.getCharacter().getActiveForm() != null && !statsData.getCharacter().getActiveForm().isEmpty();
        boolean activeStackForm = statsData.getCharacter().getActiveStackForm() != null && !statsData.getCharacter().getActiveStackForm().isEmpty();
        boolean isAndroidBaseForm = statsData.getStatus().isAndroidUpgraded() && "androidbase".equalsIgnoreCase(statsData.getCharacter().getActiveForm());
        if (activeStackForm || (activeForm && !isAndroidBaseForm)) {
            return new ButtonInfo(
                    Component.translatable("gui.action.dragonminez.descend").withStyle(ChatFormatting.BOLD),
                    Component.translatable("gui.action.dragonminez.revert_form")
            );
        } else {
            return new ButtonInfo(
                    Component.translatable("gui.action.dragonminez.descend").withStyle(ChatFormatting.BOLD),
                    Component.translatable("gui.action.dragonminez.zero_release")
            );
        }
    }

    @Override
    public void handle(StatsData statsData, boolean rightClick) {
        NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.FORCE_DESCEND, rightClick));
        playToggleSound(false);
    }
}
