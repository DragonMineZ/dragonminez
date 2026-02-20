package com.dragonminez.client.gui.utilitymenu.menuslots;

import com.dragonminez.client.gui.utilitymenu.AbstractMenuSlot;
import com.dragonminez.client.gui.utilitymenu.ButtonInfo;
import com.dragonminez.client.gui.utilitymenu.IUtilityMenuSlot;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class KiManipulationMenuSlot extends AbstractMenuSlot implements IUtilityMenuSlot {
    @Override
    public ButtonInfo render(StatsData statsData) {
        if (statsData.getSkills().hasSkill("kimanipulation") && statsData.getSkills().hasSkill("kicontrol")) {
            String weaponType = statsData.getStatus().getKiWeaponType();
            return new ButtonInfo(
                    Component.translatable("skill.dragonminez.kiweapon." + weaponType).withStyle(ChatFormatting.BOLD),
                    Component.translatable("gui.action.dragonminez." + statsData.getSkills().isSkillActive("kimanipulation")));
        } else {
            return new ButtonInfo();
        }
    }

    @Override
    public void handle(StatsData statsData, boolean rightClick) {
        if (statsData.getSkills().hasSkill("kimanipulation") && statsData.getSkills().hasSkill("kicontrol")) {
            boolean wasActive = statsData.getSkills().isSkillActive("kimanipulation");
            NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.TOGGLE_KI_WEAPON, rightClick));
            playToggleSound(!wasActive);
        }
    }
}
