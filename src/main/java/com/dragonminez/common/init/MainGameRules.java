package com.dragonminez.common.init;

import com.dragonminez.common.compat.WorldGuardCompat;
import com.dragonminez.common.init.MainTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.List;

public class MainGameRules {

	private static final int MASTER_STRUCTURE_MARGIN = 2;

	public static final GameRules.Key<GameRules.BooleanValue> ALLOW_KI_GRIEFING_MOBS =
			GameRules.register("allowKiGriefingMobs", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));

	public static final GameRules.Key<GameRules.BooleanValue> ALLOW_KI_GRIEFING_PLAYERS =
			GameRules.register("allowKiGriefingPlayers", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));

	public static final GameRules.Key<GameRules.BooleanValue> ALLOW_KI_GRIEFING_MASTER_STRUCTURES =
			GameRules.register("allowKiGriefingMasterStructures", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));

	public static final GameRules.Key<GameRules.BooleanValue> ALLOW_BUILDING_IN_ARENA_STRUCTURES =
			GameRules.register("allowBuildingInArenaStructures", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));

	public static boolean canKiGrief(Level level, BlockPos pos, Entity source) {
		boolean gameruleAllows;
		if (source instanceof Player || source instanceof ServerPlayer) gameruleAllows = level.getGameRules().getBoolean(ALLOW_KI_GRIEFING_PLAYERS);
		else gameruleAllows = level.getGameRules().getBoolean(ALLOW_KI_GRIEFING_MOBS);
		if (!gameruleAllows) return false;
		if (!level.getGameRules().getBoolean(ALLOW_KI_GRIEFING_MASTER_STRUCTURES) && isInMasterStructure(level, pos)) return false;
		return WorldGuardCompat.canGrief(level, pos, source);
	}

	private static boolean isInMasterStructure(Level level, BlockPos pos) {
		return isInsideTaggedStructure(level, pos, MainTags.Structures.KI_GRIEFING_PROTECTED);
	}

	public static boolean isInsideTaggedStructure(Level level, BlockPos pos, TagKey<Structure> tag) {
		if (!(level instanceof ServerLevel serverLevel)) return false;
		var registry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);
		for (Structure structure : serverLevel.structureManager().getAllStructuresAt(pos).keySet()) {
			Holder<Structure> holder = registry.wrapAsHolder(structure);
			if (!holder.is(tag)) continue;
			StructureStart start = serverLevel.structureManager().getStructureAt(pos, structure);
			if (start.isValid() && start.getBoundingBox().inflatedBy(MASTER_STRUCTURE_MARGIN).isInside(pos)) {
				return true;
			}
		}
		return false;
	}

	public static boolean canModifyBlock(Level level, BlockPos pos, Player player) {
		if (player != null && (player.isCreative() || player.isSpectator())) return true;
		if (level.getGameRules().getBoolean(ALLOW_BUILDING_IN_ARENA_STRUCTURES)) return true;
		return !isInsideTaggedStructure(level, pos, MainTags.Structures.BUILD_PROTECTED);
	}

	public static KiGriefGate griefGate(Level level, BoundingBox area, Entity source) {
		boolean gameruleAllows = (source instanceof Player || source instanceof ServerPlayer)
				? level.getGameRules().getBoolean(ALLOW_KI_GRIEFING_PLAYERS)
				: level.getGameRules().getBoolean(ALLOW_KI_GRIEFING_MOBS);
		if (!gameruleAllows) return KiGriefGate.DENY_ALL;

		return new KiGriefGate(level, source, collectProtectedBoxes(level, area));
	}

	private static List<BoundingBox> collectProtectedBoxes(Level level, BoundingBox area) {
		if (!(level instanceof ServerLevel serverLevel)) return List.of();
		if (level.getGameRules().getBoolean(ALLOW_KI_GRIEFING_MASTER_STRUCTURES)) return List.of();

		var registry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);
		List<BoundingBox> boxes = new ArrayList<>();

		for (int cx = SectionPos.blockToSectionCoord(area.minX()); cx <= SectionPos.blockToSectionCoord(area.maxX()); cx++) {
			for (int cz = SectionPos.blockToSectionCoord(area.minZ()); cz <= SectionPos.blockToSectionCoord(area.maxZ()); cz++) {
				for (StructureStart start : serverLevel.structureManager().startsForStructure(
						new ChunkPos(cx, cz),
						structure -> registry.wrapAsHolder(structure).is(MainTags.Structures.KI_GRIEFING_PROTECTED))) {
					if (start.isValid() && !boxes.contains(start.getBoundingBox())) boxes.add(start.getBoundingBox());
				}
			}
		}
		return boxes;
	}

	public static final class KiGriefGate {
		private static final KiGriefGate DENY_ALL = new KiGriefGate(null, null, List.of());

		private final Level level;
		private final Entity source;
		private final List<BoundingBox> protectedBoxes;

		private KiGriefGate(Level level, Entity source, List<BoundingBox> protectedBoxes) {
			this.level = level;
			this.source = source;
			this.protectedBoxes = protectedBoxes;
		}

		public boolean deniesEverything() {
			return this.level == null;
		}

		public boolean canGrief(BlockPos pos) {
			if (this.level == null) return false;
			for (int i = 0; i < this.protectedBoxes.size(); i++) {
				if (this.protectedBoxes.get(i).isInside(pos)) return false;
			}
			return WorldGuardCompat.canGrief(this.level, pos, this.source);
		}
	}

	public static void register() {}
}
