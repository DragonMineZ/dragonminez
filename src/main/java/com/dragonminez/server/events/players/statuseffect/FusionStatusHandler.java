package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import com.dragonminez.server.util.FusionLogic;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class FusionStatusHandler implements IStatusEffectHandler {
    @Override
    public void handleStatusEffects(ServerPlayer player, StatsData data) {

    }

    @Override
    public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {
        fusionTickHandling(serverPlayer, data);
    }

    @Override
    public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {

    }

    private static void fusionTickHandling(ServerPlayer serverPlayer, StatsData data) {
        if (data.getStatus().isFused()) {
            if (data.getStatus().isFusionLeader()) {
                int timer = data.getStatus().getFusionTimer();
                if (timer > 0) {
                    data.getStatus().setFusionTimer(timer - 1);
                    if (timer - 1 <= 0) FusionLogic.endFusion(serverPlayer, data, false);
                }
                UUID partnerUUID = data.getStatus().getFusionPartnerUUID();
                ServerPlayer partner = serverPlayer.getServer().getPlayerList().getPlayer(partnerUUID);
                if (partner == null || partner.hasDisconnected()) {
                    FusionLogic.endFusion(serverPlayer, data, true);
                } else if (partner.isDeadOrDying()) {
                    FusionLogic.endFusion(serverPlayer, data, true);
                } else if (partner.distanceTo(serverPlayer) > 5.0) {
                    partner.teleportTo(serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ());
                }
            } else {
                UUID leaderUUID = data.getStatus().getFusionPartnerUUID();
                ServerPlayer leader = serverPlayer.getServer().getPlayerList().getPlayer(leaderUUID);
                if (leader == null || leader.hasDisconnected() || leader.isDeadOrDying()) FusionLogic.endFusion(serverPlayer, data, true);
            }
        }
    }
}
