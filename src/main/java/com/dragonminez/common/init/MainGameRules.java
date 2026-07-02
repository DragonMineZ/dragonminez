package com.dragonminez.common.init;

import com.dragonminez.common.compat.WorldGuardCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public class MainGameRules {

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

	/** True when {@code pos} lies within a structure tagged as {@link MainTags.Structures#KI_GRIEFING_PROTECTED}. */
	private static boolean isInMasterStructure(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel serverLevel)) return false;
		return serverLevel.structureManager()
				.getStructureWithPieceAt(pos, MainTags.Structures.KI_GRIEFING_PROTECTED)
				.isValid();
	}

	public static void register() {}
}
