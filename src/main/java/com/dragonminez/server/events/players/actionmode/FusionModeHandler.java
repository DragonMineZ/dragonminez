package com.dragonminez.server.events.players.actionmode;

import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.*;
import com.dragonminez.server.events.players.IActionModeHandler;
import com.dragonminez.server.util.FusionLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.List;

public class FusionModeHandler implements IActionModeHandler {
    @Override
    public int handleActionCharge(ServerPlayer player, StatsData data) {
        if (data.getSkills().hasSkill("fusion") && !data.getCooldowns().hasCooldown(Cooldowns.COMBAT) && !data.getCooldowns().hasCooldown(Cooldowns.FUSION_CD)) {
            return 10;
        }
        return 0;
    }

    @Override
    public boolean performAction(ServerPlayer player, StatsData data) {
        return attemptFusion(player, data);
    }

    private static boolean attemptFusion(ServerPlayer player, StatsData data) {
        List<ServerPlayer> nearby = player.level().getEntitiesOfClass(ServerPlayer.class,
                player.getBoundingBox().inflate(5.0), p -> p != player);

        for (ServerPlayer target : nearby) {
            StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(targetData -> {
                if (targetData.getStatus().getSelectedAction() == ActionMode.FUSION && targetData.getResources().getActionCharge() >= 50 && targetData.getStatus().isActionCharging()) {
                    if (data.getResources().getActionCharge() >= 100) {
                        if (FusionLogic.executeMetamoru(player, target, data, targetData)) {
                            data.getResources().setActionCharge(0);
                            targetData.getResources().setActionCharge(0);

                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.FUSION.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                        }
                    }
                }
            });
            if (data.getStatus().isFused()) return true;
        }
        return false;
    }
}
