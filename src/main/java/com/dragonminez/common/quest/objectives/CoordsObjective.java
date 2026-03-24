package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import lombok.Getter;
import net.minecraft.core.BlockPos;

@Getter
public class CoordsObjective extends QuestObjective {
    private final BlockPos targetPos;
    private final int radius;

    public CoordsObjective(String description, BlockPos targetPos, int radius) {
        super(ObjectiveType.COORDS, description, 1);
        this.targetPos = targetPos;
        this.radius = radius;
    }

	@Override
    public boolean checkProgress(Object... params) {
        if (params.length > 0 && params[0] instanceof BlockPos playerPos) {
            double distance = Math.sqrt(playerPos.distSqr(targetPos));
            if (distance <= radius) {
                setProgress(1);
                return true;
            }
        }
        return false;
    }
}

