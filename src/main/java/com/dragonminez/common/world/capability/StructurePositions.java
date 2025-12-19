package com.dragonminez.common.world.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

import java.util.HashMap;
import java.util.Map;

public class StructurePositions {
    private final Map<String, BlockPos> structures = new HashMap<>();

    public void setStructurePosition(String structureName, BlockPos position) {
        structures.put(structureName, position);
    }

    public BlockPos getStructurePosition(String structureName) {
        return structures.get(structureName);
    }

    public boolean hasStructure(String structureName) {
        return structures.containsKey(structureName);
    }

    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        for (Map.Entry<String, BlockPos> entry : structures.entrySet()) {
            nbt.put(entry.getKey(), NbtUtils.writeBlockPos(entry.getValue()));
        }
        return nbt;
    }

    public void load(CompoundTag nbt) {
        structures.clear();
        for (String key : nbt.getAllKeys()) {
            structures.put(key, NbtUtils.readBlockPos(nbt.getCompound(key)));
        }
    }
}


