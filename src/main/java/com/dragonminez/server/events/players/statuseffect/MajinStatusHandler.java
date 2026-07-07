package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

public class MajinStatusHandler implements IStatusEffectHandler {
    @Override
    public void handleStatusEffects(ServerPlayer player, StatsData data) {
        if (data.getEffects().hasEffect("majin")) {
            if (data.getResources().getAlignment() != 0) {
                data.getResources().setAlignment(0);
                NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
            }
            if (!player.hasEffect(MainEffects.MAJIN.get())) {
                player.addEffect(
                        new MobEffectInstance(
                                MainEffects.MAJIN.get(),
                                data.getEffects().getEffectDuration("majin"),
                                0,
                                false,
                                false,
                                true
                        )
                );
            }
        } else {
            player.removeEffect(MainEffects.MAJIN.get());
        }
    }

    @Override
    public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {

    }

    @Override
    public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {

    }
}
