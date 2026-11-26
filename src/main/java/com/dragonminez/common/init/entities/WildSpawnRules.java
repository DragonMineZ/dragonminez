package com.dragonminez.common.init.entities;

import com.dragonminez.common.init.MainEntities;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.Map;

public final class WildSpawnRules {

	private static final int STRUCTURE_BLOCK_LIGHT_LIMIT = 7;

	private WildSpawnRules() {}

	public static boolean canSpawn(EntityType<? extends Mob> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
		if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
		if (!hasRoom(type, level, pos)) return false;
		if (reason != MobSpawnType.NATURAL) return true;

		int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
		boolean openSky = pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
		if (openSky && blockLight <= level.dimensionType().monsterSpawnBlockLightLimit()) return true;

		return blockLight <= STRUCTURE_BLOCK_LIGHT_LIMIT && isInsideWildStructure(level, pos);
	}

	private static boolean hasRoom(EntityType<? extends Mob> type, ServerLevelAccessor level, BlockPos pos) {
		BlockState feet = level.getBlockState(pos);
		if (!feet.isAir() && !feet.canBeReplaced()) return false;

		BlockPos below = pos.below();
		if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return false;

		return level.noCollision(type.getDimensions().makeBoundingBox(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
	}

	private static boolean isInsideWildStructure(ServerLevelAccessor level, BlockPos pos) {
		StructureManager structures = level.getLevel().structureManager();
		for (Map.Entry<Structure, LongSet> entry : structures.getAllStructuresAt(pos).entrySet()) {
			Structure structure = entry.getKey();
			StructureSpawnOverride override = structure.spawnOverrides().get(MainEntities.WILD_ENEMY);
			if (override == null) continue;

			StructureStart start = override.boundingBox() == StructureSpawnOverride.BoundingBoxType.PIECE
					? structures.getStructureWithPieceAt(pos, structure)
					: structures.getStructureAt(pos, structure);
			if (start.isValid()) return true;
		}
		return false;
	}
}
