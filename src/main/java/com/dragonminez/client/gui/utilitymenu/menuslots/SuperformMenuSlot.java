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
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class SuperformMenuSlot extends AbstractMenuSlot implements IUtilityMenuSlot {
    @Override
    public ButtonInfo render(StatsData statsData) {
        ActionMode currentMode = statsData.getStatus().getSelectedAction();
        String race = statsData.getCharacter().getRaceName();
        boolean hasSuperform = false;
        var skillConfig = ConfigManager.getSkillsConfig();
        for (String formSkill : skillConfig.getFormSkills()) {
            if (statsData.getSkills().getSkillLevel(formSkill) >= 1) {
                hasSuperform = true;
                break;
            }
        }

        if (hasSuperform) {
            return new ButtonInfo(
                    Component.translatable("race.dragonminez." + race + ".group." + statsData.getCharacter().getSelectedFormGroup()).withStyle(ChatFormatting.BOLD),
                    Component.translatable("race.dragonminez." + race + ".form." + statsData.getCharacter().getSelectedFormGroup() + "." + TransformationsHelper.getFirstFormGroup(statsData.getCharacter().getSelectedFormGroup(), race)),
                    currentMode == ActionMode.FORM);
        } else {
            return new ButtonInfo();
        }
    }

    @Override
    public void handle(StatsData statsData, boolean rightClick) {
        boolean hasSuperform = false;
        var skillConfig = ConfigManager.getSkillsConfig();
        for (String formSkill : skillConfig.getFormSkills()) {
            if (statsData.getSkills().getSkillLevel(formSkill) >= 1) {
                hasSuperform = true;
                break;
            }
        }

        if (hasSuperform) {
            boolean wasActive = statsData.getStatus().getSelectedAction() == ActionMode.FORM;
            if (wasActive && statsData.getCharacter().hasActiveForm()) {
                if (TransformationsHelper.canDescend(statsData)) {
                    NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.DESCEND));
                    playToggleSound(false);
                }
            } else if (!wasActive) {
                NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.FORM));
                playToggleSound(true);
            } else {
                NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.CYCLE_FORM_GROUP, rightClick));
                playToggleSound(true);
            }
        }
    }
}
