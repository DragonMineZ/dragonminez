package com.dragonminez.common.wish;

import com.dragonminez.common.stats.StatsData;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

@Setter
@Getter
public abstract class Wish {
    private final String name;
    private final String description;
    private final String type;
    private Boolean repeatable;

    protected Wish(String name, String description, String type) {
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public abstract void grant(ServerPlayer player);

    public void grant(ServerPlayer wisher, List<ServerPlayer> targets) {
        grant(wisher);
    }

    public boolean isRepeatable() {
        return repeatable == null || repeatable;
    }

    public int getMaxTargets() {
        return 0;
    }

    public List<Component> getTooltipExtras(StatsData data) {
        return List.of();
    }

}
