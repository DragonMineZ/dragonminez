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

<<<<<<< feat/dynamicFormAndStackFormSkills
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
                    Component.translatable("race.dragonminez.stack.form." + statsData.getCharacter().getSelectedStackFormGroup() + "." + TransformationsHelper.getFirstStackFormGroup(statsData.getCharacter().getSelectedStackFormGroup())),
                    currentMode == ActionMode.STACK);
=======
        // FIXME: The first time a Stack Form Group is unlocked, no Stack Form Group is selected
        //  so the button will always be empty unless players click it after.
        if (statsData.getSkills().getSkillLevel("kaioken") >= 1
                || statsData.getSkills().getSkillLevel("ultrainstinct") >= 1
                || statsData.getSkills().getSkillLevel("ultraego") >= 1) {
            String selectedStackFormGroup = statsData.getCharacter().getSelectedStackFormGroup();
            String firstStackFormGroup = TransformationsHelper.getFirstStackFormGroup(selectedStackFormGroup);
            if (selectedStackFormGroup != null && !selectedStackFormGroup.isEmpty()
                    && firstStackFormGroup != null && !firstStackFormGroup.isEmpty()) {
                return new ButtonInfo(
                        Component.translatable("race.dragonminez.stack.group." + selectedStackFormGroup).withStyle(ChatFormatting.BOLD),
                        Component.translatable("race.dragonminez.stack.form." + selectedStackFormGroup + "." + firstStackFormGroup),
                        currentMode == ActionMode.STACK);
            } else {
                return new ButtonInfo();
            }
>>>>>>> main
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
                if (TransformationsHelper.canDescend(statsData)) {
                    NetworkHandler.sendToServer(new ExecuteActionC2S("descend"));
                    playToggleSound(false);
                }
            } else if (!wasActive) {
                NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.STACK));
                playToggleSound(true);
            } else {
                NetworkHandler.sendToServer(new ExecuteActionC2S("cycle_stack_form_group", rightClick));
                playToggleSound(true);
            }
        }
    }
}
