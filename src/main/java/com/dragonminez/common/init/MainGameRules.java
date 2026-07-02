package com.dragonminez.common.init;

import com.dragonminez.common.compat.WorldGuardCompat;
import com.dragonminez.common.init.MainTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class MainGameRules {

	private static final int MASTER_STRUCTURE_MARGIN = 2;

	public static final GameRules.Key<GameRules.BooleanValue> ALLOW_KI_GRIEFING_MOBS =
			GameRules.register("allowKiGriefingMobs", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));

	public static final GameRules.Key<GameRules.BooleanValue> ALLOW_KI_GRIEFING_PLAYERS =
			GameRules.register("allowKiGriefingPlayers", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));

	public static final GameRules.Key<GameRules.BooleanValue> ALLOW_KI_GRIEFING_MASTER_STRUCTURES =
			GameRules.register("allowKiGriefingMasterStructures", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));

	public static boolean canKiGrief(Level level, BlockPos pos, Entity source) {
		boolean gameruleAllows;
		if (source instanceof Player || source instanceof ServerPlayer) gameruleAllows = level.getGameRules().getBoolean(ALLOW_KI_GRIEFING_PLAYERS);
		else gameruleAllows = level.getGameRules().getBoolean(ALLOW_KI_GRIEFING_MOBS);
		if (!gameruleAllows) return false;
		if (!level.getGameRules().getBoolean(ALLOW_KI_GRIEFING_MASTER_STRUCTURES) && isInMasterStructure(level, pos)) return false;
		return WorldGuardCompat.canGrief(level, pos, source);
	}

	private static boolean isInMasterStructure(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel serverLevel)) return false;
		var registry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);
		for (Structure structure : serverLevel.structureManager().getAllStructuresAt(pos).keySet()) {
			Holder<Structure> holder = registry.wrapAsHolder(structure);
			if (!holder.is(MainTags.Structures.KI_GRIEFING_PROTECTED)) continue;
			StructureStart start = serverLevel.structureManager().getStructureAt(pos, structure);
			if (start.isValid() && start.getBoundingBox().inflatedBy(MASTER_STRUCTURE_MARGIN).isInside(pos)) {
				return true;
			}
		}
		return false;
	}

	public static void register() {}
}
