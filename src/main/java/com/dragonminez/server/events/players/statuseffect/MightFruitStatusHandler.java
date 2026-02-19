package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

public class MightFruitStatusHandler implements IStatusEffectHandler {
    @Override
    public void handleStatusEffects(ServerPlayer player, StatsData data) {
        if (data.getEffects().hasEffect("mightfruit")) {
            if (!player.hasEffect(MainEffects.MIGHTFRUIT.get())) {
                player.addEffect(new MobEffectInstance(MainEffects.MIGHTFRUIT.get(), -1, 0, false, false, true));
            }
        } else {
            player.removeEffect(MainEffects.MIGHTFRUIT.get());
        }
    }

    @Override
    public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {

    }

    @Override
    public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {

    }
}
