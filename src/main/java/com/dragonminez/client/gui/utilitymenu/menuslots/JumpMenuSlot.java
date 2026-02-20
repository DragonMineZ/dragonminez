package com.dragonminez.client.gui.utilitymenu.menuslots;

import com.dragonminez.client.gui.utilitymenu.AbstractMenuSlot;
import com.dragonminez.client.gui.utilitymenu.ButtonInfo;
import com.dragonminez.client.gui.utilitymenu.IUtilityMenuSlot;
import com.dragonminez.common.network.C2S.SwitchActionC2S;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.ActionMode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class JumpMenuSlot extends AbstractMenuSlot implements IUtilityMenuSlot {
    @Override
    public ButtonInfo render(StatsData statsData) {
        if (statsData.getSkills().hasSkill("jump")) {
            return new ButtonInfo(
                    Component.translatable("skill.dragonminez.jump").withStyle(ChatFormatting.BOLD),
                    Component.translatable("gui.action.dragonminez." + (statsData.getSkills().isSkillActive("jump") ? "true" : "false")),
                    statsData.getSkills().isSkillActive("jump"));
        } else {
            return new ButtonInfo();
        }
    }

    @Override
    public void handle(StatsData statsData, boolean rightClick) {
        if (statsData.getSkills().hasSkill("jump")) {
            boolean wasActive = statsData.getSkills().isSkillActive("jump");
            NetworkHandler.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.TOGGLE, "jump", 0));
            playToggleSound(wasActive);
        }
    }
}
