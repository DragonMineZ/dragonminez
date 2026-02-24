package com.dragonminez.client.gui.utilitymenu.menuslots;

import com.dragonminez.client.gui.utilitymenu.AbstractMenuSlot;
import com.dragonminez.client.gui.utilitymenu.ButtonInfo;
import com.dragonminez.client.gui.utilitymenu.IUtilityMenuSlot;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.C2S.SwitchActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.ActionMode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class RacialActionMenuSlot extends AbstractMenuSlot implements IUtilityMenuSlot {
    @Override
    public ButtonInfo render(StatsData statsData) {
        ActionMode currentMode = statsData.getStatus().getSelectedAction();
        String race = statsData.getCharacter().getRaceName();
        String form = statsData.getCharacter().getActiveForm();
        String racialSkill = ConfigManager.getRaceCharacter(race) == null ? "" : ConfigManager.getRaceCharacter(race).getRacialSkill();

        if ("saiyan".equals(race) && (statsData.getCharacter().isHasSaiyanTail() || (form != null && form.contains("oozaru")))) {
            return new ButtonInfo(
                    Component.translatable("gui.action.dragonminez.tail").withStyle(ChatFormatting.BOLD),
                    Component.translatable("gui.action.dragonminez." + (statsData.getStatus().isTailVisible())),
                    statsData.getStatus().isTailVisible()
            );
        } else if ("namekian".equals(racialSkill) || "bioandroid".equals(racialSkill) || "majin".equals(racialSkill)) {
            return new ButtonInfo(
                    Component.translatable("gui.action.dragonminez.racial." + racialSkill).withStyle(ChatFormatting.BOLD),
                    Component.translatable("gui.action.dragonminez." + (statsData.getStatus().getSelectedAction() == ActionMode.RACIAL ? "true" : "false")),
                    currentMode == ActionMode.RACIAL
            );
        } else {
            return new ButtonInfo();
        }
    }

    @Override
    public void handle(StatsData statsData, boolean rightClick) {
        String race = statsData.getCharacter().getRaceName();
        String racialSkill = ConfigManager.getRaceCharacter(race) == null ? "" : ConfigManager.getRaceCharacter(race).getRacialSkill();
        if ("saiyan".equals(race)) {
            boolean wasActive = statsData.getStatus().isTailVisible();
            NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.TOGGLE_TAIL));
            playToggleSound(!wasActive);
        }
        else if ("namekian".equals(racialSkill) || "bioandroid".equals(racialSkill) || "majin".equals(racialSkill)) {
            boolean wasActive = statsData.getStatus().getSelectedAction() == ActionMode.RACIAL;
            NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.RACIAL));
            playToggleSound(!wasActive);
        }
    }
}
