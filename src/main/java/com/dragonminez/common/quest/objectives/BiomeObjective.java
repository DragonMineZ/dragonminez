package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

public class BiomeObjective extends QuestObjective {
    private final String biomeId;

    public BiomeObjective(String description, String biomeId) {
        super(ObjectiveType.BIOME, description, 1);
        this.biomeId = biomeId;
    }

    public String getBiomeId() {
        return biomeId;
    }

    @Override
    public boolean checkProgress(Object... params) {
        if (params.length > 0 && params[0] instanceof Biome biome) {
            String currentBiome = biome.toString();
            if (currentBiome.contains(biomeId)) {
                setProgress(1);
                return true;
            }
        }
        return false;
    }
}

