package com.dragonminez.server.events.players.actionmode;

import com.dragonminez.common.racial.RacialAbility;
import com.dragonminez.common.racial.RacialContext;
import com.dragonminez.common.racial.RacialRegistry;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IActionModeHandler;
import net.minecraft.server.level.ServerPlayer;

public class RacialModeHandler implements IActionModeHandler {

    @Override
    public boolean canCharge(ServerPlayer player, StatsData data) {
        return RacialRegistry.forPlayer(data)
                .map(ability -> ability.canActivate(new RacialContext(player, data)))
                .orElse(false);
    }

    @Override
    public int handleActionCharge(ServerPlayer player, StatsData data) {
        return RacialRegistry.forPlayer(data).map(ability -> {
            RacialContext ctx = new RacialContext(player, data);
            int chargeSeconds = ability.chargeSeconds(ctx);
            if (chargeSeconds <= 0) {
                ability.onActivate(ctx);
                data.getStatus().setActionCharging(false);
                return 0;
            }
            return Math.max(1, 100 / chargeSeconds);
        }).orElse(0);
    }

    @Override
    public boolean performAction(ServerPlayer player, StatsData data) {
        return RacialRegistry.forPlayer(data)
                .map(ability -> ability.onActivate(new RacialContext(player, data)))
                .orElse(false);
    }
}
