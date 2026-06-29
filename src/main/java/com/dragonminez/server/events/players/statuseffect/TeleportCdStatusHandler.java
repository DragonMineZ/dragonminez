package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraft.server.level.ServerPlayer;

public class TeleportCdStatusHandler implements IStatusEffectHandler {
    @Override
    public void handleStatusEffects(ServerPlayer player, StatsData data) {
        if (!data.getCooldowns().hasCooldown(Cooldowns.TELEPORT_CD)) {
            player.removeEffect(MainEffects.TELEPORT_CD.get());
        }
    }

    @Override
    public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {

    }

    @Override
    public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {

    }
}
