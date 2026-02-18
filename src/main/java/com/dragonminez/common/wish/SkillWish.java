
package com.dragonminez.common.wish;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;

public class SkillWish extends Wish {
    private final String skill;
    private final int level;

    public SkillWish(String name, String description, String skill, int level) {
        super(name, description, "skill");
        this.skill = skill;
        this.level = level;
    }

    @Override
    public void grant(ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!data.getStatus().hasCreatedCharacter()) return;
            data.getSkills().setSkillLevel(skill, level);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        });
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this, SkillWish.class);
    }
}
