package com.dragonminez.server.events.players.actionmode;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.server.events.players.IActionModeHandler;
import com.dragonminez.server.util.RacialSkillLogic;
import net.minecraft.server.level.ServerPlayer;

public class RacialModeHandler implements IActionModeHandler {
    @Override
    public boolean canCharge(ServerPlayer player, StatsData data) {
        if (ConfigManager.getRaceCharacter(data.getCharacter().getRace()) == null) return false;
        return switch (ConfigManager.getRaceCharacter(data.getCharacter().getRace()).getRacialSkill()) {
            case "namekian" -> data.getResources().getRacialSkillCount() < ConfigManager.getServerConfig().getRacialSkills().getNamekianAssimilationAmount();
            case "majin" -> data.getResources().getRacialSkillCount() < ConfigManager.getServerConfig().getRacialSkills().getMajinAbsorptionAmount();
            case "bioandroid" -> !data.getCooldowns().hasCooldown(Cooldowns.DRAIN);
            default -> false;
        };
    }

    @Override
    public int handleActionCharge(ServerPlayer player, StatsData data) {
        String race = data.getCharacter().getRaceName();
        String racialSkill = ConfigManager.getRaceCharacter(race) == null ? "" : ConfigManager.getRaceCharacter(race).getRacialSkill();
        if ("bioandroid".equals(racialSkill)) {
            RacialSkillLogic.attemptRacialAction(player);
            data.getStatus().setActionCharging(false);
            return 0;
        }
        return 25;
    }

    @Override
    public boolean performAction(ServerPlayer player, StatsData data) {
        RacialSkillLogic.attemptRacialAction(player);
        return true;
    }
}
