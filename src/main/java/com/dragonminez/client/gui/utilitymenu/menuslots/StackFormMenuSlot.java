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

public class StackFormMenuSlot extends AbstractMenuSlot implements IUtilityMenuSlot {
    @Override
    public ButtonInfo render(StatsData statsData) {
        ActionMode currentMode = statsData.getStatus().getSelectedAction();

        boolean hasStackform = false;
        var skillConfig = ConfigManager.getSkillsConfig();
        for (String formSkill : skillConfig.getStackSkills()) {
            if (statsData.getSkills().getSkillLevel(formSkill) >= 1) {
                hasStackform = true;
                break;
            }
        }

        if (hasStackform) {
            return new ButtonInfo(
                    Component.translatable("race.dragonminez.stack.group." + statsData.getCharacter().getSelectedStackFormGroup()).withStyle(ChatFormatting.BOLD),
                    Component.translatable("race.dragonminez.stack.form." + statsData.getCharacter().getSelectedStackFormGroup() + "." + statsData.getCharacter().getSelectedStackForm()),
                    currentMode == ActionMode.STACK);
        } else {
            return new ButtonInfo();
        }
    }

    @Override
    public void handle(StatsData statsData, boolean rightClick) {
        boolean hasStackform = false;
        var skillConfig = ConfigManager.getSkillsConfig();
        for (String formSkill : skillConfig.getStackSkills()) {
            if (statsData.getSkills().getSkillLevel(formSkill) >= 1) {
                hasStackform = true;
                break;
            }
        }

        if (hasStackform) {
            boolean wasActive = statsData.getStatus().getSelectedAction() == ActionMode.STACK;
            if (wasActive && statsData.getCharacter().hasActiveStackForm()) {
                if (TransformationsHelper.canStackDescend(statsData)) {
                    NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.DESCEND));
                    playToggleSound(false);
                }
            } else if (!wasActive) {
                NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.STACK));
                playToggleSound(true);
            } else {
                NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.CYCLE_STACK_FORM_GROUP, rightClick));
                playToggleSound(true);
            }
        }
    }
}
