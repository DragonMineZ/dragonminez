package com.dragonminez.server.world.structure.fitted;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public record SitePlan(Rotation rotation, BlockPos origin, BlockPos center, BoundingBox templateBox, int groundLevel,
					   int skirt, double deviation, float water, int[] sampleX, int[] sampleZ,
					   int[] sampleTop) {
}
