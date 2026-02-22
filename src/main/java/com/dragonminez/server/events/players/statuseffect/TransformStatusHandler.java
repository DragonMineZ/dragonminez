package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.ActionMode;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

public class TransformStatusHandler implements IStatusEffectHandler {
    @Override
    public void handleStatusEffects(ServerPlayer player, StatsData data) {
        if (data.getStatus().isActionCharging()) {
            if (data.getStatus().getSelectedAction().equals(ActionMode.FORM)) {
                if (!player.hasEffect(MainEffects.TRANSFORM.get())) {
                    player.addEffect(
                            new MobEffectInstance(
                                    MainEffects.TRANSFORM.get(),
                                    -1,
                                    0,
                                    false,
                                    false,
                                    true)
                    );
                }

            } else if (data.getStatus().getSelectedAction().equals(ActionMode.STACK)) {
                if (!player.hasEffect(MainEffects.STACK_TRANSFORM.get())) {
                    player.addEffect(
                            new MobEffectInstance(
                                    MainEffects.STACK_TRANSFORM.get(),
                                    -1,
                                    0,
                                    false,
                                    false,
                                    true)
                    );
                }

            }
        } else {
            player.removeEffect(MainEffects.TRANSFORM.get());
            player.removeEffect(MainEffects.STACK_TRANSFORM.get());
        }
    }

    @Override
    public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {
        if (data.getCharacter().getActiveForm().isEmpty() || data.getCharacter().getActiveForm().equals("base") || data.getCharacter().getActiveForm() == null) {
            serverPlayer.removeEffect(MainEffects.TRANSFORM.get());
        }
        if (data.getCharacter().getActiveStackForm().isEmpty() || data.getCharacter().getActiveStackForm().equals("base") || data.getCharacter().getActiveStackForm() == null) {
            serverPlayer.removeEffect(MainEffects.STACK_TRANSFORM.get());
        }
    }

    @Override
    public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {

    }
}
