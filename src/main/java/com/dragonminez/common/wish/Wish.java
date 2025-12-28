package com.dragonminez.common.wish;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

public abstract class Wish {
    private final String name;
    private final String description;

    public Wish(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public abstract void grant(ServerPlayer player);

    public abstract JsonObject toJson();
}
