package com.dragonminez.server.events.players.actionmode;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.events.players.IActionModeHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class KaiokenModeHandler implements IActionModeHandler {
    @Override
    public int handleActionCharge(ServerPlayer player, StatsData data) {
        if (TransformationsHelper.canStackKaioken(data)) {
            int skillLvl = data.getSkills().getSkillLevel("kaioken");
            int currentPhase = data.getStatus().getActiveKaiokenPhase();
            int maxPhase = TransformationsHelper.getMaxKaiokenPhase(skillLvl);
            if (currentPhase < maxPhase) {
                return  25;
            }
        } else {
            data.getStatus().setActionCharging(false);
        }
        return 0;
    }

    @Override
    public boolean performAction(ServerPlayer player, StatsData data) {
        int currentPhase = data.getStatus().getActiveKaiokenPhase();
        int skillLvl = data.getSkills().getSkillLevel("kaioken");
        int maxPhase = TransformationsHelper.getMaxKaiokenPhase(skillLvl);

        if (currentPhase < maxPhase) {
            data.getStatus().setActiveKaiokenPhase(currentPhase + 1);
            String name = TransformationsHelper.getKaiokenName(currentPhase + 1);
            player.displayClientMessage(Component.translatable("message.dragonminez.kaioken.activate", name), true);
        }

        if (!data.getSkills().isSkillActive("kaioken")) data.getSkills().setSkillActive("kaioken", true);
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        return true;
    }
}
