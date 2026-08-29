package com.dragonminez.common.stats;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatsDataSerializationTest {
    @Test
    void serializationAndClonePreserveAttachmentState() throws ClassNotFoundException {
        StatsData original = new StatsData(null);
        original.getStats().setStrength(42);
        original.getStats().setVitality(17);
        original.getResources().setCurrentEnergy(12.5F);
        original.getResources().setTrainingPoints(9001.0F);
        original.getStatus().setHasCreatedCharacter(true);
        original.getStatus().markVisitedDimension("dragonminez:namek");

        CompoundTag serialized = original.save();
        StatsData restored = new StatsData(null);
        restored.load(serialized);

        assertEquals(42, restored.getStats().getStrength());
        assertEquals(17, restored.getStats().getVitality());
        assertEquals(12.5F, restored.getResources().getCurrentEnergy());
        assertEquals(9001.0F, restored.getResources().getTrainingPoints());
        assertTrue(restored.getStatus().isHasCreatedCharacter());
        assertTrue(restored.getStatus().hasVisitedDimension("dragonminez:namek"));

        StatsData clone = new StatsData(null);
        clone.copyFrom(restored);

        assertEquals(restored.save(), clone.save());
        assertNotSame(restored.getStatus().getVisitedDimensions(), clone.getStatus().getVisitedDimensions());
    }
}
