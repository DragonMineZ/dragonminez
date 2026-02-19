package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

public class KaiokenStatusHandler implements IStatusEffectHandler {
    @Override
    public void handleStatusEffects(ServerPlayer player, StatsData data) {
        if (data.getSkills().isSkillActive("kaioken")) {
            if (!player.hasEffect(MainEffects.KAIOKEN.get())) {
                player.addEffect(new MobEffectInstance(MainEffects.KAIOKEN.get(), -1, 0, false, false, true));
            }
        } else {
            player.removeEffect(MainEffects.KAIOKEN.get());
        }
    }

    @Override
    public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {

    }

    @Override
    public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {
        handleKaiokenEffects(serverPlayer, data);
    }

    private static void handleKaiokenEffects(ServerPlayer player, StatsData data) {
        if (!data.getSkills().isSkillActive("kaioken")) return;
        if (player.isCreative() || player.isSpectator()) return;

        float drain = Math.max(2, TransformationsHelper.getKaiokenHealthDrain(data));

        if (player.getHealth() - drain <= 1.0f) {
            data.getSkills().setSkillActive("kaioken", false);
            data.getStatus().setActiveKaiokenPhase(0);
            data.getResources().setPowerRelease(0);
            data.getResources().setActionCharge(0);
        } else {
            player.setHealth(player.getHealth() - drain);
        }
    }
}
