package com.dragonminez.common.init;

import com.dragonminez.common.compat.WorldGuardCompat;
import net.minecraft.core.BlockPos;
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

	public static final GameRules.Key<GameRules.IntegerValue> OTHERWORLD_REVIVE_COOLDOWN =
			GameRules.register("otherworldReviveCooldown", GameRules.Category.PLAYER, GameRules.IntegerValue.create(300));

	public static boolean canKiGrief(Level level, BlockPos pos, Entity source) {
		boolean gameruleAllows;
		if (source instanceof Player || source instanceof ServerPlayer) {
			gameruleAllows = level.getGameRules().getBoolean(ALLOW_KI_GRIEFING_PLAYERS);
		} else {
			gameruleAllows = level.getGameRules().getBoolean(ALLOW_KI_GRIEFING_MOBS);
		}

		if (!gameruleAllows) {
			return false;
		}

		return WorldGuardCompat.canGrief(level, pos, source);
	}

	public static void register() {}
}
