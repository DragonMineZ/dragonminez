package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Getter
public class StructureObjective extends QuestObjective {
    private final String structureId;

    public StructureObjective(String description, String structureId) {
        super(ObjectiveType.STRUCTURE, description, 1);
        this.structureId = structureId;
    }

	@Override
    public boolean checkProgress(Object... params) {
        if (params.length >= 2 && params[0] instanceof Level level && params[1] instanceof BlockPos pos) {
            setProgress(1);
            return true;
        }
        return false;
    }
}

