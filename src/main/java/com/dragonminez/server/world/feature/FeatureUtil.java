package com.dragonminez.server.world.feature;

import com.dragonminez.Reference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.Map;

final class FeatureUtil {
    private static final int MAX_GROUND_DEPTH = 64;
    private static final int STRUCTURE_PROTECT_MARGIN = 6;

    private FeatureUtil() {}

    static void groundColumn(WorldGenLevel level, BlockPos basePos, BlockState fill) {
        BlockPos.MutableBlockPos cursor = basePos.mutable().move(0, -1, 0);
        for (int depth = 0; depth < MAX_GROUND_DEPTH; depth++) {
            BlockState state = level.getBlockState(cursor);
            if (!(state.isAir() || state.canBeReplaced() || state.liquid())) break;
            level.setBlock(cursor, fill, 2);
            cursor.move(0, -1, 0);
        }
    }

    static boolean isInsideDmzStructure(WorldGenLevel level, BlockPos pos) {
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkAccess chunk = level.getChunk(chunkX + dx, chunkZ + dz, ChunkStatus.STRUCTURE_STARTS, false);
                if (chunk == null) continue;

                for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
                    StructureStart start = entry.getValue();
                    if (start == null || !start.isValid()) continue;

                    ResourceLocation id = structures.getKey(entry.getKey());
                    if (id == null || !Reference.MOD_ID.equals(id.getNamespace())) continue;

                    BoundingBox box = start.getBoundingBox();
                    if (pos.getX() >= box.minX() - STRUCTURE_PROTECT_MARGIN
                            && pos.getX() <= box.maxX() + STRUCTURE_PROTECT_MARGIN
                            && pos.getZ() >= box.minZ() - STRUCTURE_PROTECT_MARGIN
                            && pos.getZ() <= box.maxZ() + STRUCTURE_PROTECT_MARGIN) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
