package com.dragonminez.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Reads BlockPos payloads from both the fork's 1.20 compound format and Minecraft 1.21's packed long format. */
public final class LegacyNbtData {
    private LegacyNbtData() {}

    public static BlockPos readBlockPos(Tag tag) {
        if (tag instanceof net.minecraft.nbt.LongTag packed) {
            return BlockPos.of(packed.getAsLong());
        }
        if (tag instanceof CompoundTag legacy) {
            String x = legacy.contains("X") ? "X" : "x";
            String y = legacy.contains("Y") ? "Y" : "y";
            String z = legacy.contains("Z") ? "Z" : "z";
            return new BlockPos(legacy.getInt(x), legacy.getInt(y), legacy.getInt(z));
        }
        throw new IllegalArgumentException("Unsupported saved BlockPos tag: " + (tag == null ? "null" : tag.getType().getName()));
    }
}
