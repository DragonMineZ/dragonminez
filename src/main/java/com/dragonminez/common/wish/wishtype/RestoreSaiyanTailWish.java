package com.dragonminez.common.wish.wishtype;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.wish.Wish;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;

public class RestoreSaiyanTailWish extends Wish {
    public RestoreSaiyanTailWish(String name, String description, String... commands) {
        super(name, description, "restoresaiyantail");
    }

    @Override
    public void grant(ServerPlayer player) {
        player.getCapability(StatsCapability.INSTANCE).ifPresent(stats -> {
            stats.getCharacter().setSaiyanTail(true);
        });
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this, RestoreSaiyanTailWish.class);
    }
}
