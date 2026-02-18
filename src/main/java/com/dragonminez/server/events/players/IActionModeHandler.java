package com.dragonminez.server.events.players;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;

public interface IActionModeHandler {
    int handleActionCharge(ServerPlayer player, StatsData data);
    boolean performAction(ServerPlayer player, StatsData data);
}
