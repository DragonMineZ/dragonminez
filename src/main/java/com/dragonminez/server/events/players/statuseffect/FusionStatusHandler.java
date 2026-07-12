package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import com.dragonminez.server.util.FusionLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
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
                    if (timer - 1 <= 0) {
                        // Break the pothala earrings when a potara fusion ends by time, so the two don't
                        // instantly re-fuse by proximity. Capture the partner before the state is cleared.
                        boolean isPothala = "POTHALA".equals(data.getStatus().getFusionType());
                        UUID timedPartnerUUID = data.getStatus().getFusionPartnerUUID();
                        ServerPlayer timedPartner = timedPartnerUUID != null ? serverPlayer.getServer().getPlayerList().getPlayer(timedPartnerUUID) : null;
                        FusionLogic.endFusion(serverPlayer, data, false);
                        if (isPothala) {
                            FusionLogic.breakPothala(serverPlayer);
                            if (timedPartner != null) FusionLogic.breakPothala(timedPartner);
                        }
                        return;
                    }
                }
                UUID partnerUUID = data.getStatus().getFusionPartnerUUID();
                ServerPlayer partner = serverPlayer.getServer().getPlayerList().getPlayer(partnerUUID);
                if (partner == null || partner.hasDisconnected()) {
                    FusionLogic.endFusion(serverPlayer, data, true);
                } else if (partner.isDeadOrDying()) {
                    FusionLogic.endFusion(serverPlayer, data, true);
                } else if (partner.level() != serverPlayer.level()) {
                    partner.stopRiding();
                    partner.teleportTo((ServerLevel) serverPlayer.level(), serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            Collections.emptySet(), serverPlayer.getYRot(), serverPlayer.getXRot());
                    partner.startRiding(serverPlayer, true);
                } else if (partner.distanceTo(serverPlayer) > 5.0) {
                    if (!partner.isPassenger()) partner.startRiding(serverPlayer, true);
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
