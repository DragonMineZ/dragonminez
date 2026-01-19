package com.dragonminez.common.init;

import net.minecraft.world.level.GameRules;

public class MainGameRules {

	public static final GameRules.Key<GameRules.BooleanValue> ALLOW_KI_GRIEFING_MOBS =
			GameRules.register("allowKiGriefingMobs", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));

	public static final GameRules.Key<GameRules.BooleanValue> ALLOW_KI_GRIEFING_PLAYERS =
			GameRules.register("allowKiGriefingPlayers", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));

	public static final GameRules.Key<GameRules.IntegerValue> OTHERWORLD_REVIVE_COOLDOWN =
			GameRules.register("otherworldReviveCooldown", GameRules.Category.PLAYER, GameRules.IntegerValue.create(300));

	public static void register() {}
}
