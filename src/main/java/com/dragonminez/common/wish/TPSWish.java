package com.dragonminez.common.wish;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

public class TPSWish extends Wish {
    private final int amount;

    public TPSWish(String name, String description, int amount) {
        super(name, description);
        this.amount = amount;
    }

    @Override
    public void grant(ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            data.getResources().addTrainingPoints(amount);
        });
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "tps");
        json.addProperty("name", getName());
        json.addProperty("description", getDescription());
        json.addProperty("amount", amount);
        return json;
    }
}
