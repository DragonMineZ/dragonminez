package com.dragonminez.common.wish;

import net.minecraft.server.level.ServerPlayer;

public abstract class Wish {
    private final String name;
    private final String description;
    private final String type;

    public Wish(String name, String description, String type) {
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public abstract void grant(ServerPlayer player);

    public abstract String toJson();
}
