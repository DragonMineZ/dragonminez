package com.dragonminez.server.events.players.actionmode;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IActionModeHandler;
import com.dragonminez.server.util.RacialSkillLogic;
import net.minecraft.server.level.ServerPlayer;

public class RacialModeHandler implements IActionModeHandler {
    @Override
    public int handleActionCharge(ServerPlayer player, StatsData data) {
        String race = data.getCharacter().getRaceName();
        if ("bioandroid".equals(race)) {
            RacialSkillLogic.attemptRacialAction(player);
            data.getStatus().setActionCharging(false);
            return 0;
        }
        return 25;
    }

    @Override
    public boolean performAction(ServerPlayer player, StatsData data) {
        RacialSkillLogic.attemptRacialAction(player);
        return true;
    }
}
