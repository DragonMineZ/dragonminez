package com.dragonminez.common.quest;

import com.dragonminez.Reference;
import com.dragonminez.server.world.dimension.HTCDimension;
import com.dragonminez.server.world.dimension.NamekDimension;
import com.dragonminez.server.world.structure.helper.DMZStructures;
import com.dragonminez.server.world.structure.helper.StructureLocator;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Map;

/**
 * Resolves optional quest-tree hints for DMZ-owned structures.
 */
public final class QuestStructureHintHelper {

	private static final Map<String, Pair<ResourceKey<Structure>, ResourceKey<Level>>> DMZ_STRUCTURE_INFO = Map.of(
			Reference.MOD_ID + ":goku_house", Pair.of(DMZStructures.GOKU_HOUSE, Level.OVERWORLD),
			Reference.MOD_ID + ":roshi_house", Pair.of(DMZStructures.ROSHI_HOUSE, Level.OVERWORLD),
			Reference.MOD_ID + ":elder_guru", Pair.of(DMZStructures.ELDER_GURU, NamekDimension.NAMEK_KEY),
			Reference.MOD_ID + ":timechamber", Pair.of(DMZStructures.TIMECHAMBER, HTCDimension.HTC_KEY),
			Reference.MOD_ID + ":kamilookout", Pair.of(DMZStructures.KAMILOOKOUT, Level.OVERWORLD),
			Reference.MOD_ID + ":gero_lab", Pair.of(DMZStructures.GERO_LAB, Level.OVERWORLD)
	);

	private QuestStructureHintHelper() {
	}

	public static QuestPrerequisites.StructureHint resolveHint(String structureId) {
		if (structureId == null || structureId.isBlank() || !structureId.contains(":")) {
			return null;
		}

		String normalized;
		try {
			normalized = ResourceLocation.parse(structureId).toString();
		} catch (Exception e) {
			return null;
		}

		Pair<ResourceKey<Structure>, ResourceKey<Level>> info = DMZ_STRUCTURE_INFO.get(normalized);
		if (info == null) {
			return null;
		}

		String dimensionId = info.getSecond().location().toString();
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) {
			return new QuestPrerequisites.StructureHint(dimensionId, null, null, null);
		}

		ServerLevel targetLevel = server.getLevel(info.getSecond());
		if (targetLevel == null) {
			return new QuestPrerequisites.StructureHint(dimensionId, null, null, null);
		}

		BlockPos structurePos = StructureLocator.locateStructure(targetLevel, info.getFirst(), targetLevel.getSharedSpawnPos());
		if (structurePos == null) {
			return new QuestPrerequisites.StructureHint(dimensionId, null, null, null);
		}

		return new QuestPrerequisites.StructureHint(
				dimensionId,
				structurePos.getX(),
				structurePos.getY(),
				structurePos.getZ()
		);
	}
}
