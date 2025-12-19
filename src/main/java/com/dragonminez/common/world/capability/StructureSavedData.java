package com.dragonminez.common.world.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class StructureSavedData extends SavedData {
    private static final String DATA_NAME = "dragonminez_structures";
    private final StructurePositions positions = new StructurePositions();

    public StructureSavedData() {
    }

    public StructureSavedData(CompoundTag nbt) {
        positions.load(nbt);
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag nbt) {
        nbt.merge(positions.save());
        return nbt;
    }

    public StructurePositions getPositions() {
        return positions;
    }

    public static StructureSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                StructureSavedData::new,
                StructureSavedData::new,
                DATA_NAME
        );
    }
}

