package com.dragonminez.client.systems.kisense;

import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.skills.Skill;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class KiSenseState {

	public enum Mode {
		NONE, COMBAT, SEARCH
	}

	private static Mode mode = Mode.NONE;

	private KiSenseState() {}

	public static Mode getMode() {
		return mode;
	}

	public static boolean isActive() {
		return mode != Mode.NONE;
	}

	public static boolean isCombat() {
		return mode == Mode.COMBAT;
	}

	public static boolean isSearch() {
		return mode == Mode.SEARCH;
	}

	public static void cycle() {
		Mode next = switch (mode) {
			case NONE -> Mode.COMBAT;
			case COMBAT -> Mode.SEARCH;
			case SEARCH -> Mode.NONE;
		};
		set(next);
	}

	public static void set(Mode newMode) {
		mode = newMode;
		KiSenseScan.forceRescan();
		syncActiveFlag();
	}

	public static void reset() {
		mode = Mode.NONE;
	}

	private static void syncActiveFlag() {
		Player player = Minecraft.getInstance().player;
		if (player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			Skill kiSense = data.getSkills().getSkill("kisense");
			if (kiSense == null || kiSense.getLevel() <= 0) return;

			boolean desired = mode != Mode.NONE;
			if (desired != kiSense.isActive()) {
				data.getSkills().setSkillActive("kisense", desired);
				NetworkHandler.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.TOGGLE, "kisense", 0));
			}
		});
	}
}
