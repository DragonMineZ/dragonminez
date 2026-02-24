package com.dragonminez.common.wish;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.level.ServerPlayer;

@Setter
@Getter
@AllArgsConstructor
public abstract class Wish {
    private final String name;
    private final String description;
    private final String type;

    public abstract void grant(ServerPlayer player);

    public abstract String toJson();
}
