package com.dragonminez.client.gui.utilitymenu;

import com.dragonminez.common.stats.StatsData;

public interface IUtilityMenuSlot {
    ButtonInfo render(StatsData statsData);
    void handle(StatsData statsData, boolean rightClick);
}
