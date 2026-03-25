package com.dragonminez.common.quest;

import com.dragonminez.common.quest.objectives.BiomeObjective;
import com.dragonminez.common.quest.objectives.CoordsObjective;
import com.dragonminez.common.quest.objectives.StructureObjective;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Shared helpers for quest objectives that depend on player world position.
 */
public final class QuestLocationHelper {

	private QuestLocationHelper() {
	}

	public static boolean isLocationObjective(QuestObjective objective) {
		return objective instanceof BiomeObjective || objective instanceof StructureObjective || objective instanceof CoordsObjective;
	}

	public static boolean isQuestStartLocationSatisfied(ServerPlayer player, Quest quest) {
		if (player == null || quest == null) return false;
		for (QuestObjective objective : quest.getObjectives()) {
			if (!isLocationObjective(objective)) {
				return true;
			}
			return isLocationConditionMet(player, objective);
		}
		return true;
	}

	public static boolean isQuestStartLocationSatisfied(Player player, Quest quest) {
		if (player == null || quest == null) return false;
		for (QuestObjective objective : quest.getObjectives()) {
			if (!isLocationObjective(objective)) {
				return true;
			}
			return isLocationConditionMet(player, objective, true);
		}
		return true;
	}

	public static boolean isLocationConditionMet(ServerPlayer player, QuestObjective objective) {
		if (player == null) return false;
		return isLocationConditionMet(player, objective, false);
	}

	private static boolean isLocationConditionMet(Player player, QuestObjective objective, boolean allowUnknownStructureOnClient) {
		if (player == null || objective == null) return false;
		return isLocationConditionMet(player.level(), player.blockPosition(), objective, allowUnknownStructureOnClient);
	}

	private static boolean isLocationConditionMet(Level level, BlockPos pos, QuestObjective objective, boolean allowUnknownStructureOnClient) {
		if (objective instanceof BiomeObjective biomeObj) {
			return matchesBiome(level, pos, biomeObj.getBiomeId());
		}

		if (objective instanceof StructureObjective structObj) {
			if (!(level instanceof ServerLevel serverLevel)) {
				return allowUnknownStructureOnClient;
			}
			return isInStructure(serverLevel, pos, structObj.getStructureId());
		}

		if (objective instanceof CoordsObjective coordsObj) {
			double distSq = pos.distSqr(coordsObj.getTargetPos());
			double radiusSq = (double) coordsObj.getRadius() * coordsObj.getRadius();
			return distSq <= radiusSq;
		}

		return false;
	}

	private static boolean matchesBiome(Level level, BlockPos pos, String targetBiome) {
		try {
			Holder<Biome> biomeHolder = level.getBiome(pos);
			if (targetBiome.startsWith("#")) {
				ResourceLocation tagRL = ResourceLocation.parse(targetBiome.substring(1));
				TagKey<Biome> tagKey = TagKey.create(Registries.BIOME, tagRL);
				return biomeHolder.is(tagKey);
			}

			ResourceLocation biomeRL = ResourceLocation.parse(targetBiome.contains(":") ? targetBiome : "minecraft:" + targetBiome);
			return biomeHolder.is(biomeRL);
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean isInStructure(ServerLevel level, BlockPos pos, String targetStructure) {
		try {
			ResourceLocation structRL = ResourceLocation.parse(targetStructure.contains(":") ? targetStructure : "minecraft:" + targetStructure);
			ResourceKey<Structure> structKey = ResourceKey.create(Registries.STRUCTURE, structRL);
			return level.structureManager().getStructureWithPieceAt(pos, structKey).isValid();
		} catch (Exception e) {
			return false;
		}
	}
}

