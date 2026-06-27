package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import lombok.Getter;
import net.minecraft.world.level.Level;

@Getter
public class DimensionObjective extends QuestObjective {
    private final String dimensionId;

    public DimensionObjective(String dimensionId) {
        super(ObjectiveType.DIMENSION, 1);
        this.dimensionId = dimensionId;
    }

    @Override
    public boolean checkProgress(Object... params) {
        if (params.length > 0 && params[0] instanceof Level level) {
            if (level.dimension().location().toString().equals(dimensionId)) {
                setProgress(1);
                return true;
            }
        }
        return false;
    }
}
