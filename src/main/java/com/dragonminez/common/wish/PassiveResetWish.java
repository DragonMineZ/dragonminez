
package com.dragonminez.common.wish;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;

public class PassiveResetWish extends Wish {

    public PassiveResetWish(String name, String description) {
        super(name, description, "passivereset");
    }

    @Override
    public void grant(ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            String[] statBoosts = new String[0];
            statBoosts = switch (data.getCharacter().getRace()) {
                case "namekian" -> ConfigManager.getServerConfig().getRacialSkills().getNamekianAssimilationBoosts();
                case "majin" -> ConfigManager.getServerConfig().getRacialSkills().getMajinAbsorptionBoosts();
                case "saiyan" -> ConfigManager.getServerConfig().getRacialSkills().getSaiyanZenkaiBoosts();
                default -> statBoosts;
            };

            for (String stat : statBoosts) {
                for (int i = data.getResources().getRacialSkillCount(); i >= 0; i--) {
                    data.getBonusStats().clearBonus(stat, "Absorption_");
                    data.getBonusStats().clearBonus(stat, "Assimilation_");
                    data.getBonusStats().clearBonus(stat, "Zenkai_");
                }
            }

            data.getCooldowns().removeCooldown(Cooldowns.ZENKAI);
            data.getResources().setRacialSkillCount(0);
        });
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this, PassiveResetWish.class);
    }
}
