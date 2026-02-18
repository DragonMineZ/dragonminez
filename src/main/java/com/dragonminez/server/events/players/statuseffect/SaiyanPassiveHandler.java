package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SaiyanPassiveHandler implements IStatusEffectHandler {
    private static int saiyanZenkaiSeconds = 0;

    @Override
    public void handleStatusEffects(ServerPlayer player, StatsData data) {

    }

    @Override
    public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {

    }

    @Override
    public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {
        if (ConfigManager.getServerConfig().getRacialSkills().isEnableRacialSkills() && ConfigManager.getServerConfig().getRacialSkills().isSaiyanRacialSkill()) {
            if (data.getCharacter().getRaceName().equals("saiyan")) {
                handleSaiyanPassive(serverPlayer, data);
            }
        }
    }

    private static void handleSaiyanPassive(ServerPlayer player, StatsData data) {
        GeneralServerConfig.RacialSkillsConfig config = ConfigManager.getServerConfig().getRacialSkills();

        if (data.getResources().getRacialSkillCount() >= config.getSaiyanZenkaiAmount()) return;
        if (data.getCooldowns().hasCooldown(Cooldowns.ZENKAI)) return;

        float maxHealth = player.getMaxHealth();
        if (player.getHealth() <= maxHealth * 0.15) {
            saiyanZenkaiSeconds = saiyanZenkaiSeconds + 1;
        } else {
            saiyanZenkaiSeconds = 0;
        }

        if (saiyanZenkaiSeconds >= 8) {
            player.heal((float) (maxHealth * config.getSaiyanZenkaiHealthRegen()));

            double boostMult = config.getSaiyanZenkaiStatBoost();
            String[] statsToBoost = config.getSaiyanZenkaiBoosts();

            for (String statKey : statsToBoost) {
                int currentStat = getStatValue(data, statKey);
                int bonus = (int) Math.max(1, currentStat * boostMult);
                data.getBonusStats().addBonus(statKey, "Zenkai_" + (data.getResources().getRacialSkillCount() + 1), "+", bonus);
            }

            player.displayClientMessage(Component.translatable("message.dragonminez.racial.zenkai.used"), true);

            data.getResources().addRacialSkillCount(1);
            data.getCooldowns().setCooldown(Cooldowns.ZENKAI, config.getSaiyanZenkaiCooldownSeconds());
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            saiyanZenkaiSeconds = 0;
        }
    }

    private static int getStatValue(StatsData data, String statName) {
        return switch (statName) {
            case "STR" -> data.getStats().getStrength();
            case "SKP" -> data.getStats().getStrikePower();
            case "RES" -> data.getStats().getResistance();
            case "VIT" -> data.getStats().getVitality();
            case "PWR" -> data.getStats().getKiPower();
            case "ENE" -> data.getStats().getEnergy();
            default -> 0;
        };
    }
}
