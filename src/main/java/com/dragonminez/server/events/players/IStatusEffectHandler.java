package com.dragonminez.server.events.players;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;

public interface IStatusEffectHandler {
    void handleStatusEffects(ServerPlayer player, StatsData data);
    void onPlayerTick(ServerPlayer serverPlayer, StatsData data);
    void onPlayerSecond(ServerPlayer serverPlayer, StatsData data);
}
