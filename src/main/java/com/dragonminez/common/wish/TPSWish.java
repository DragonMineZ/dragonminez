package com.dragonminez.common.wish;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;

public class TPSWish extends Wish {
    private final int amount;

    public TPSWish(String name, String description, int amount) {
        super(name, description, "tps");
        this.amount = amount;
    }

    @Override
    public void grant(ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> data.getResources().addTrainingPoints(amount));
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this, TPSWish.class);
    }
}
